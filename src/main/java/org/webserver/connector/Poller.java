package org.webserver.connector;

import org.webserver.constant.ServerConfig;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * 轮询客户端连接
 */
public class Poller implements Runnable {
    private final long expiryTime = Long.parseLong(System.getProperty(ServerConfig.CONNECTION_EXPIRY_TIME));
    private final static Logger logger = Logger.getLogger(Poller.class.getPackageName());

    private Selector selector;
    private String pollerName;
    private Server server;
    private Map<SocketChannel, SocketWrapper> clients; // 保存客户端socket，SocketWrapper 在处理后的连接重新注册时使用
    /**
     * 事件队列：每当接收到新的连接时，将获得的 SocketChannel 对象
     * 封装后注册到 Poller 的事件队列。
     * 使用并发安全的对象，涉及到两个线程：轮询线程，Connector线程注册。
     */
    private Queue<PollerEvent> pollerEventQueue;


    Poller(String pollerName, Server server) throws IOException {
        this.selector = Selector.open();
        this.pollerName = pollerName;
        this.server = server;
        this.pollerEventQueue = new ConcurrentLinkedQueue<>();
        this.clients = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        logger.info(String.format("%s 开始轮询客户端连接", this.pollerName));
        while (server.isRunning()) {
            try {
                // 监听事件队列中的连接
                handleEvents();

                if (selector.select() <= 0) {
                    continue;
                }
                logger.info(String.format("%s 开始读取监听的事件", this.pollerName));
                // 获取就绪事件
                for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); iter.hasNext(); iter.remove()) {
                    SelectionKey key = iter.next();
                    if (key.isReadable()) {
                        /*
                        【注意！！】：
                            当浏览器加载一半时突然关闭连接（X按钮），那么就会发送连接断开信号，服务端就会
                            处于可读状态，但 read 总是返回 -1，无法读到ByteBuffer，如果 while 循环中仅
                            判断是否大于0的话，就不不断地处于可读状态，然后导致死循环，所以应该在客户端断
                            开后，关闭服务端的监听。
                         */
                        logger.info(String.format("[%s] 读就绪，开始读取", ((SocketChannel)key.channel()).getRemoteAddress()));
                        SocketWrapper socketWrapper = (SocketWrapper) key.attachment();
                        socketWrapper.setLastCommutationTime(System.currentTimeMillis()); // 更新状态

                        cancelReadListening(socketWrapper); // 取消读事件的监听
                        server.processClient(socketWrapper);
                    }
                }
            } catch (IOException e) {
                logger.warning(String.format("%s 的 selector 异常[%s]", this.pollerName, e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    /**
     * 向轮询线程注册一个连接
     * @param isNew 用于判断是初次向Poller注册，还是处理完后又注册了监听事件
     */
    void register(SocketChannel client, boolean isNew) {
        SocketWrapper socketWrapper;
        if (isNew) {
            logger.info(String.format("新的连接被注册到 %s 的 PollerEvent 事件队列中", this.pollerName));
            socketWrapper = new SocketWrapper(client, this);
            clients.put(client, socketWrapper);
        } else {
            socketWrapper = clients.get(client);
        }
        pollerEventQueue.offer(new PollerEvent(socketWrapper));
        // 唤醒轮询线程（调用select()阻塞的话），更新监听状态
        selector.wakeup();
    }

    /**
     * 取消某个Socket的读事件在selector上的注册，因为将请求处理器放入线程池中后，
     * 会导致当前socket被多个线程读取数据
     */
    void cancelReadListening(SocketWrapper socketWrapper) {
        socketWrapper.getClient().keyFor(selector).cancel();
    }


    /**
     * 将Socket注册到Selector上，监听读事件
     */
    private void enableReadListening(SocketWrapper socketWrapper) {
        try {
            socketWrapper.getClient().register(selector, SelectionKey.OP_READ);
        } catch (ClosedChannelException ignore) {
        }
    }


    /**
     * 将事件队列中的事件注册到 selector 中
     */
    private void handleEvents() {
        for (int size = pollerEventQueue.size(); size > 0; size--) {
            if (pollerEventQueue.peek() != null) {
                pollerEventQueue.poll().start();
            }
        }
    }


    /**
     * 清理过期（长时间未通信）的 HTTP 长连接
     */
    void clearExpiredConnection() {
        for (Iterator<Map.Entry<SocketChannel, SocketWrapper>> iter = clients.entrySet().iterator(); iter.hasNext(); ) {
            SocketWrapper client = iter.next().getValue();
            // 断开连接
            if (!client.getClient().isConnected()) {
                logger.info(String.format("%s 轮询的客户端[%s]已断开连接，取消监听", this.pollerName, client.getClient().toString()));
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                selector.wakeup();
                iter.remove();
            } else if (System.currentTimeMillis() - client.getLastCommutationTime() > expiryTime) {
                logger.info(String.format("%s 轮询的客户端[%s]的连接过期，断开连接", getPollerName(), client));
                // 连接过期
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                selector.wakeup();
                iter.remove();
            }
        }
    }

    /**
     * 关闭轮询线程，并关闭所有连接
     */
    void close() throws IOException {
        for (SocketWrapper client : clients.values()) {
            client.close();
        }
        selector.close();
    }

    Selector getSelector() {
        return this.selector;
    }

    String getPollerName() {
        return this.pollerName;
    }

    Map<SocketChannel, SocketWrapper> getClients() {
        return clients;
    }

    /**
     * 事件对象
     */
    private class PollerEvent {
        private SocketWrapper socketWrapper;

        PollerEvent(SocketWrapper socketWrapper) {
            this.socketWrapper = socketWrapper;
        }

        void start() {
            // 将客户端连接注册到 selector 上
            try {
                if (socketWrapper.getClient().isOpen()) {
                    socketWrapper.getClient().register(Poller.this.getSelector(), SelectionKey.OP_READ, socketWrapper);
                    logger.info(String.format("%s 开始监听 %s", Poller.this.pollerName, socketWrapper.getClient().getRemoteAddress()));
                }
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                logger.info(String.format("%s 监听连接失败[%s]", Poller.this.pollerName, e.getMessage()));
            } catch (IOException ignore) {
            }
        }
    }
}

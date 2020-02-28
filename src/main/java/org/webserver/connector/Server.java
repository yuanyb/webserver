package org.webserver.connector;

import org.webserver.constant.ServerConfig;
import org.webserver.container.Container;
import org.webserver.exception.InternalServerException;
import org.webserver.http.session.ExpiredSessionCleaner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 开启socket并监听客户端的请求
 */
public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getPackageName());
    private final int pollerThreadCount = Integer.parseInt(System.getProperty(ServerConfig.POLLER_THREAD_COUNT)); // Tomcat: Math.min(2, Runtime.getRuntime().availableProcessors());;

    private Container container;
    private RequestProcessor requestProcessor;
    private ServerSocketChannel server;
    private Acceptor acceptor; // 接收客户端连接
    private volatile int port;
    private volatile boolean isRunning = true;

    private List<Poller> pollers;
    private final AtomicInteger nextPollerIndex = new AtomicInteger(0);
    private ExpiredConnectionCleaner connectionCleaner;

    public void start(int port) {
        this.port = port;

        try {
            // 初始化ServerSocketChannel
            initServerSocket(port);
            // 初始化Acceptor并异步监听
            initAcceptor();
            // 初始化轮询线程
            initPollers();
            // 初始化过期连接清理器
            initExpiredConnectionCleaner();
            // 初始化过期Session清理器
            initContainer();
            // 初始化请求处理器
            initRequestProcessor();
            logger.info("服务启动成功");
        } catch (IOException | InternalServerException e) {
            logger.info("服务器启动失败：" + e.getMessage());
            this.isRunning = false;
            e.printStackTrace();
        }
    }


    private void initServerSocket(int port) throws IOException {
        logger.info(String.format("监听 %s 端口", System.getProperty(ServerConfig.PORT)));
        this.server = ServerSocketChannel.open();
        this.server.bind(new InetSocketAddress(port));
        this.server.configureBlocking(true); // 阻塞监听客户端连接
    }

    private void initAcceptor() {
        logger.info("启动连接监听器");
        this.acceptor = new Acceptor(this);
        Thread thread = new Thread(this.acceptor, "Acceptor");
        thread.setDaemon(true); // 设置为Connector线程的守护线程
        thread.start();
    }

    private void initPollers() throws IOException {
        logger.info(String.format("启动轮询线程，个数（%s）", pollerThreadCount));
        this.pollers = new ArrayList<>();
        for (int i = 0; i < pollerThreadCount; i++) {
            String pollerName = "Poller-" + i;
            Poller poller = new Poller(pollerName, this);
            Thread pollerThread = new Thread(poller, pollerName);
            pollerThread.setDaemon(true);
            pollerThread.start();
            pollers.add(poller);
        }
    }

    private void initExpiredConnectionCleaner() {
        logger.info(String.format("启动过期连接清理器，周期（%s）", System.getProperty(ServerConfig.CONNECTION_CLEANING_CYCLE)));
        this.connectionCleaner = new ExpiredConnectionCleaner(pollers);
        this.connectionCleaner.start();
    }

    private void initContainer() throws InternalServerException {
        logger.info("启动容器");
        this.container = new Container();
        this.container.init();
    }

    private void initRequestProcessor() {
        logger.info("初始化请求处理器");
        this.requestProcessor = new RequestProcessor(this.container);
    }

    boolean isRunning() {
        return this.isRunning;
    }

    SocketChannel accept() throws IOException {
        return this.server.accept();
    }

    int getPort() {
        return port;
    }

    /**
     * 将客户端连接注册到轮询线程，轮询多个Poller线程，负载均衡
     * @param client
     */
    void registerToPoller(SocketChannel client) {
        // nextPollerIndex 加到最大值溢出
        this.pollers.get(Math.abs(nextPollerIndex.getAndIncrement()) % pollerThreadCount).register(client, true);
    }

    /**
     * 处理读就绪的客户端连接
     */
    void processClient(SocketWrapper socketWrapper) {
        this.requestProcessor.process(socketWrapper);
    }
}

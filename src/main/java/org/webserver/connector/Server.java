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
    /** 轮询线程数量（Tomcat: Math.min(2, Runtime.getRuntime().availableProcessors())）*/
    private final int pollerThreadCount = Integer.parseInt(System.getProperty(ServerConfig.POLLER_THREAD_COUNT));
    /** 容器类，保存 Session 和 控制器方法 */
    private Container container;
    /** 请求处理器，用于处理读就绪的连接 */
    private RequestProcessor requestProcessor;
    /** 服务器Socket */
    private ServerSocketChannel server;
    /** 监听器，监听客户端连接，守护线程*/
    private Acceptor acceptor; // 接收客户端连接
    /** 监听的端口 */
    private volatile int port;
    /** 服务器是否还在运行 */
    private volatile boolean isRunning = true;
    /** 轮询线程，监听客户端发来的数据 */
    private List<Poller> pollers;
    /** 索引，循环将新连接分配给轮询线程 */
    private final AtomicInteger nextPollerIndex = new AtomicInteger(0);
    /** 守护线程，清理过期的连接（HTTP长连接） */
    private ExpiredConnectionCleaner connectionCleaner;

    /**
     * 启动服务器
     * @param port 监听端口
     */
    public void start(int port) {
        this.port = port;

        try {
            // 初始化ServerSocketChannel
            initServerSocket(port);
            // 初始化轮询线程
            initPollers();
            // 初始化Acceptor并异步监听
            initAcceptor();
            // 初始化过期连接清理器
            initExpiredConnectionCleaner();
            // 初始化容器
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


// ============== 初始化方法开始 ==============
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
// ============== 初始化方法结束 ==============

    /**
     * 服务器是否还在运行，Acceptor 和 Poller使用
     */
    boolean isRunning() {
        return this.isRunning;
    }

    /**
     * 获取端口
     */
    int getPort() {
        return port;
    }

    /**
     * Acceptor 使用，监听新连接
     */
    SocketChannel accept() throws IOException {
        return server.accept();
    }

    /**
     * 将客户端连接注册到轮询线程，轮询多个 Poller 线程，负载均衡
     */
    void registerToPoller(SocketChannel client) {
        // nextPollerIndex 加到最大值溢出
        this.pollers.get(Math.abs(nextPollerIndex.getAndIncrement()) % pollerThreadCount).register(client, true);
    }

    /**
     * 处理读就绪的客户端连接，交给请求处理器
     */
    void processClient(SocketWrapper socketWrapper) {
        this.requestProcessor.process(socketWrapper);
    }
}

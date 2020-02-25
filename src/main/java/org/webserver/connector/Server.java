package org.webserver.connector;

import org.webserver.constant.ServerConfig;
import org.webserver.http.request.Cookie;
import org.webserver.http.response.HttpResponse;
import org.webserver.http.response.HttpStatus;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
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
    private ServerSocketChannel server;
    private Acceptor acceptor; // 接收客户端连接
    private volatile boolean isRunning = true;
    private volatile int port;
    private List<Poller> pollers;
    private final AtomicInteger nextPollerIndex = new AtomicInteger(0);
    private ExpiredConnectionCleaner cleaner;

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

        } catch (IOException e) {
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
        this.cleaner = new ExpiredConnectionCleaner(pollers);
        this.cleaner.start();
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
        this.pollers.get(Math.abs(nextPollerIndex.getAndIncrement()) % pollerThreadCount).register(client);;
    }

    /**
     * 处理读就绪的客户端连接
     * @param socketWrapper
     */
    public void processClient(SocketWrapper socketWrapper) {
//        ByteBuffer buffer = ByteBuffer.allocate(1024);
//        SocketChannel client = socketWrapper.getClient();
//        try {
//            int len;
//            while (true) {
//                len = client.read(buffer);
//                if (len == 0)
//                    break;
//                if (len == -1) { // 当客户端主动关闭连接时，必须取消它的监听，否则会死循环
//                    socketWrapper.close();
//                    return;
//                }
//                buffer.flip();
//                System.out.println(Charset.defaultCharset().decode(buffer));
//                buffer.clear();
//            }
//            HttpResponse response = new HttpResponse();
//            response.setContent("hello!!!".getBytes());
//            response.addHeader("Server", "XXX");
//            response.setContentType("text/html");
//            Cookie cookie = new Cookie("ccc", "bbb");
//            cookie.setMaxAge(3600 * 24 * 10);
//            response.addCookie(cookie);
//            response.setStatus(HttpStatus.SC_200);
//            ByteBuffer[] bf = response.getResponseData();
//            client.write(bf);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.setProperty(ServerConfig.POLLER_THREAD_COUNT, "2");
        System.setProperty(ServerConfig.CONNECTION_EXPIRY_TIME, "1000");
        System.setProperty(ServerConfig.CONNECTION_CLEANING_CYCLE, "10000");
        System.setProperty(ServerConfig.PORT, "80");
        new Server().start(Integer.parseInt(System.getProperty(ServerConfig.PORT)));
        Thread.sleep(500000);
    }

}

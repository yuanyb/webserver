package org.webserver.connector;

import org.webserver.constant.ServerConfig;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.request.HttpRequestParser;
import org.webserver.http.response.HttpResponse;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * 请求处理器：处理传递过来的请求
 */
public class RequestProcessor {
    private final static Logger logger = Logger.getLogger(RequestProcessor.class.getPackageName());
    private final static int REQUEST_PROCESSOR_THREAD_COUNT = Integer.parseInt(System.getProperty(ServerConfig.REQUEST_PROCESSOR_THREAD_COUNT));
    private final static ExecutorService threadPool = Executors.newFixedThreadPool(REQUEST_PROCESSOR_THREAD_COUNT);

    static void process(SocketWrapper socketWrapper) {
        threadPool.submit(new RequestProcessTask(socketWrapper));
    }

    private static class RequestProcessTask implements Runnable {
        SocketWrapper socketWrapper;
        RequestProcessTask(SocketWrapper socketWrapper) {
            this.socketWrapper = socketWrapper;
        }

        @Override
        public void run() {
            HttpRequest httpRequest = HttpRequestParser.parseRequest(socketWrapper);
            HttpResponse response;
            // 根据请求地址找控制器执行或返回静态资源
            // 返回response
            // 根据response响应
        }
    }
}

package org.webserver.connector;

import org.webserver.constant.HttpConstant;
import org.webserver.constant.ServerConfig;
import org.webserver.container.Container;
import org.webserver.exception.HttpMethodNotSupportedException;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.request.HttpRequestParser;
import org.webserver.http.response.HttpResponse;
import org.webserver.http.response.HttpStatus;
import org.webserver.util.ErrorResponseUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * 请求处理器：处理传递过来的请求
 */
public class RequestProcessor {
    private final static Logger logger =
            Logger.getLogger(RequestProcessor.class.getPackageName());

    /** 线程池大小 */
    private final static int REQUEST_PROCESSOR_THREAD_COUNT =
            Integer.parseInt(System.getProperty(ServerConfig.REQUEST_PROCESSOR_THREAD_COUNT));

    /** 控制器根路径 */
    private final String CONTROLLER_ROOT_PATH =
            RequestProcessor.class.getResource("/org/webserver/controller/")
                    .toString().substring(6); //substring(6)去掉开头的file:/

    /** MIME TYPE */
    private final Properties mime;

    /** 处理响应的线程池 */
    private final ExecutorService threadPool =
            Executors.newFixedThreadPool(REQUEST_PROCESSOR_THREAD_COUNT);

    /** 容器 */
    private final Container container;

    RequestProcessor(Container container) {
        this.container = container;
        this.mime = new Properties();
        InputStream mimeInput = ClassLoader.getSystemResourceAsStream("mime.properties");
        if (mimeInput != null) {
            try {
                this.mime.load(mimeInput);
            } catch (IOException e) {
                logger.warning("加载配置文件 mime.properties 失败");
                e.printStackTrace();
            }
        } else {
            logger.warning("缺少配置文件 mime.properties");
        }
    }


    /** 将HTTP请求添加到线程池中处理 */
    void process(SocketWrapper socketWrapper) {
        threadPool.submit(new RequestProcessTask(socketWrapper));
    }




    /** 请求处理任务类 */
    private class RequestProcessTask implements Runnable {
        private SocketWrapper socketWrapper;
        RequestProcessTask(SocketWrapper socketWrapper) {
            this.socketWrapper = socketWrapper;
        }

        @Override
        public void run() {
            // 解析请求
            HttpRequest request = HttpRequestParser.parseRequest(socketWrapper, RequestProcessor.this.container);

            // 构建响应
            HttpResponse response = buildResponse(request);

            // 写回并处理连接（持久连接or非持久连接）
            writeResponse(request, response);
        }


        /**
         * 构建响应
         */
        private HttpResponse buildResponse(HttpRequest request) {
            HttpResponse response;
            try {
                response = RequestProcessor.this.container.handle(request);
            } catch (HttpMethodNotSupportedException e) { // 不支持的方法
                response = new HttpResponse();
                ErrorResponseUtil.renderErrorResponse(response, HttpStatus.SC_405, "不支持 " + request.getMethod() + "方法");
            }

            if (response == null) { // 静态资源
                response = processStaticResource(request);
            }
            return response;
        }

        /**
         * 处理静态资源请求
         */
        private HttpResponse processStaticResource(HttpRequest request) {
            HttpResponse response = new HttpResponse();
            // 获得请求的资源名
            String URI = request.getRequestURI();
            int idx = URI.indexOf('?');
            String filename = URI.substring(1, idx == -1 ? URI.length() : idx);
            filename = RequestProcessor.this.CONTROLLER_ROOT_PATH + filename;

            try {
                response.setStatus(HttpStatus.SC_200);
                String extName = filename.substring(filename.lastIndexOf('.') + 1);
                response.setContentType(mime.getProperty(extName));
                response.setContentLength(Files.size(Path.of(filename)));
                FileInputStream fin = new FileInputStream(filename);
                byte[] buffer = new byte[512];
                int len;
                while ((len = fin.read(buffer)) > 0) {
                    response.getOutputStream().write(buffer, 0, len);
                }
            }
            // 404
            catch (FileNotFoundException e) {
                ErrorResponseUtil.renderErrorResponse(response, HttpStatus.SC_400, request.getRequestURI());
                e.printStackTrace();
                logger.warning(String.format("未找到文件：%s", filename));
            }
            // 500
            catch (IOException e) {
                ErrorResponseUtil.renderErrorResponse(response, HttpStatus.SC_500, e.getMessage());
                e.printStackTrace();
                logger.warning(String.format("读取静态文件失败（%s）：%s", filename, e.getMessage()));
                response.setStatus(HttpStatus.SC_500);
            }

            return response;
        }

        public void writeResponse(HttpRequest request, HttpResponse response) {
            // 写回数据
            try {
                socketWrapper.getClient().write(response.getResponseData());
            } catch (IOException e) {
                logger.warning(String.format("向客户端[%s]写数据失败：%s", socketWrapper.getClient(), e.getMessage()));
                e.printStackTrace();
            }

            // 处理连接
            String conn = request.getHeader(HttpConstant.CONNECTION);
            if (conn != null && conn.contains("close")) { // 非持久连接
                try {
                    logger.info(String.format("非持久连接：关闭 %s", socketWrapper.getClient()));
                    socketWrapper.close();
                } catch (IOException e) {
                    logger.warning("关闭连接失败：" + e.getMessage());
                    e.printStackTrace();
                }
            } else { // 持久连接
                socketWrapper.getPoller().register(socketWrapper.getClient(), false);
                logger.info(String.format("持久连接：%s 被重新注册到了Poller", socketWrapper.getClient()));
            }
        }
    }
}

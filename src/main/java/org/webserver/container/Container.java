package org.webserver.container;

import org.webserver.constant.ServerConfig;
import org.webserver.container.annotation.Controller;
import org.webserver.exception.HttpMethodNotSupportedException;
import org.webserver.exception.InternalServerException;
import org.webserver.http.HttpMethod;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.response.HttpResponse;
import org.webserver.http.session.ExpiredSessionCleaner;
import org.webserver.http.session.HttpSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 保存加载的控制器映射等信息，Session
 */
public class Container {
    private final long SESSION_EXPIRY_TIME = Long.parseLong(System.getProperty(ServerConfig.SESSION_EXPIRY_TIME));
    private final static Logger logger = Logger.getLogger(Container.class.getPackageName());
    private final Map<String, HttpSession> sessions = new ConcurrentHashMap<>(); // 线程安全
    private Map<String, TargetMethod> methodMap;
    private ExpiredSessionCleaner sessionCleaner;

    /**
     * 初始化容器
     * @throws InternalServerException
     */
    public void init() throws InternalServerException {
        logger.info("初始化容器类");
        this.methodMap = ControllerScanner.scan();
    }

    /**
     * 处理请求
     */
    public HttpResponse handle(HttpRequest request) throws HttpMethodNotSupportedException{
        String target = request.getRequestURI().substring(0, request.getRequestURI().indexOf('?'));
        if (methodMap.get(target) == null) { // 请求静态资源
            return null;
        }
        if(!methodMap.get(target).getHttpMethodType().equals(HttpMethod.ANY) && !request.getMethod().equals(methodMap.get(target).getHttpMethodType())) {
            throw new HttpMethodNotSupportedException();
        }
        return methodMap.get(target).invoke(request);
    }

    private void initExpiredSessionCleaner() {
        this.sessionCleaner = new ExpiredSessionCleaner(this);
    }

    /**
     * 重建一个新Session
     * @return
     */
    public HttpSession createNewSession() {
        HttpSession session = new HttpSession(this);
        sessions.put(session.getID(), session);
        return session;
    }

    public HttpSession getSession(String id) {
        return sessions.get(id);
    }

    public void clearExpiredSession() {
        logger.info("开始清除过期Session");
        Iterator<Map.Entry<String, HttpSession>> iter = sessions.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, HttpSession> entry = iter.next();
            if (System.currentTimeMillis() - entry.getValue().getLastAccessedTime() > SESSION_EXPIRY_TIME) {
                logger.info(String.format("Session[%s]已过期", entry.getValue().getID()));
                iter.remove();
            }
        }
    }

    /**
     * 销毁指定Session
     * @param httpSession
     */
    public void invalidateSession(HttpSession httpSession) {
        logger.info(String.format("Session[%s]被销毁", httpSession.getID()));
        this.sessions.remove(httpSession.getID());
    }
}

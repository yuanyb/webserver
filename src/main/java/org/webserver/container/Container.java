package org.webserver.container;

import org.webserver.constant.ServerConfig;
import org.webserver.exception.HttpMethodNotSupportedException;
import org.webserver.exception.InternalServerException;
import org.webserver.http.HttpMethod;
import org.webserver.http.request.HttpRequest;
import org.webserver.http.response.HttpResponse;
import org.webserver.http.session.ExpiredSessionCleaner;
import org.webserver.http.session.HttpSession;

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
     */
    public void init() throws InternalServerException {
        logger.info("初始化容器类");
        this.methodMap = ControllerScanner.scan();
        initExpiredSessionCleaner();
    }

    /**
     * 处理请求
     */
    public HttpResponse handle(HttpRequest request) throws HttpMethodNotSupportedException{
        int idx = request.getRequestURI().indexOf('?');
        String target = request.getRequestURI().substring(0, idx == -1 ? request.getRequestURI().length() : idx);
        // 请求的可能是静态资源
        if (methodMap.get(target) == null) {
            return null;
        }
        // 方法不支持
        if(!methodMap.get(target).getHttpMethodType().equals(HttpMethod.ANY) && !request.getMethod().equals(methodMap.get(target).getHttpMethodType())) {
            throw new HttpMethodNotSupportedException();
        }
        return methodMap.get(target).invoke(request);
    }

    private void initExpiredSessionCleaner() {
        this.sessionCleaner = new ExpiredSessionCleaner(this);
        this.sessionCleaner.start();
    }

    /**
     * 重建一个新Session
     */
    public HttpSession createNewSession() {
        HttpSession session = new HttpSession(this);
        sessions.put(session.getID(), session);
        session.setIsNew(true);
        return session;
    }

    /**
     * 获取指定 Session
     */
    public HttpSession getSession(String id) {
        HttpSession session = sessions.get(id);
        if (session != null && session.isNew())
            session.setIsNew(false);
        return session;
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
     */
    public void invalidateSession(HttpSession httpSession) {
        logger.info(String.format("Session[%s]被销毁", httpSession.getID()));
        this.sessions.remove(httpSession.getID());
    }
}

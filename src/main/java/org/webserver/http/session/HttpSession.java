package org.webserver.http.session;

import org.webserver.container.Container;

import java.util.Map;
import java.util.UUID;

public class HttpSession {
    private String sessionID;
    private Map<String, Object> attributes;
    private long creationTime;
    private long lastAccessedTime;
    private Container container;
    /** 是否是本次请求创建的新的Session，用于判断是否向客户端返回 Set-Cookie:JSESSIONID */
    private boolean isNew;

    public HttpSession(Container container) {
        this.sessionID = UUID.randomUUID().toString().replaceAll("-", "");
        this.lastAccessedTime = this.creationTime = System.currentTimeMillis();
        this.container = container;
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        this.attributes.remove(key);
    }

    public void invalidate() {
        this.attributes = null;
        container.invalidateSession(this); // 从容器中删除自己
    }

    public String getID() {
        return sessionID;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(long time) {
        this.lastAccessedTime = time;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isNew() {
        return this.isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }
}

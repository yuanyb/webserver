package org.webserver.http.session;

import org.webserver.constant.HttpConstant;
import org.webserver.container.Container;

import java.util.Map;
import java.util.UUID;

public class HttpSession {
    private String sessionID;
    private Map<String, Object> attributes;
    private long creationTime;
    private long lastAccessedTime;
    private Container container;

    public HttpSession(Container container) {
        this.sessionID = UUID.randomUUID().toString();
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

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isNew() {
        return true;
    }
}

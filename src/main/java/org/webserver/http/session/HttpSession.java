package org.webserver.http.session;

import java.util.Map;

public class HttpSession {
    private String sessionID;
    private Map<String, Object> attributes;
    private long creationTime;
    private long lastAccessedTime;

    public HttpSession() {
        lastAccessedTime = this.creationTime = System.currentTimeMillis();
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
        // todo 清除session
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

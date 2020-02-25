package org.webserver.http.request;

import org.webserver.connector.SocketWrapper;
import org.webserver.constant.HttpConstant;
import org.webserver.http.HttpMethod;
import org.webserver.http.session.HttpSession;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequest {
    private String URI;
    private HttpMethod method;
    private Map<String, String> headers;
    private Map<String, List<String>> params;
//    private byte[] content;
    private Map<String, Cookie> cookies;
    private HttpSession session;
    private Map<String, Object> attributes;

    public HttpRequest() {
    }

    public HttpSession getSession() {
        return null;
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public Object setAttributes(String key, Object value) {
        return this.attributes.put(key, value);
    }

    public String getRequestURI() {
        return this.URI;
    }

    void setRequestURI(String URI) {
        this.URI = URI;
    }

    public HttpMethod getMethod() {
        return method;
    }

    void setMethod(HttpMethod method) {
        this.method = method;
    }


    public Cookie getCookie(String key) {
        return this.cookies.get(key);
    }

    public String getParameter(String key) {
        return this.params.get(key) == null ? null : this.params.get(key).get(0);
    }

    public List<String> getParameterValues(String key) {
        return this.params.get(key);
    }

    public String getHeader(String name) {
        return this.headers.get(name);
    }

    public String getContentType() {
        return getHeader(HttpConstant.CONTENT_TYPE);
    }

    public String getRemoteAddr() {
        return "";
    }


    // HttpRequestParser 使用，包私有
    void setParams(Map<String, List<String>> params) {
        this.params = params;
    }

    void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    void setCookies(Map<String, Cookie> cookies) {
        this.cookies = cookies;
    }

    void addParam(String k, String v) {
        params.putIfAbsent(k, new ArrayList<>(1));
        params.get(k).add(v);
    }

//    public static void main(String[] args) {
//        String req = "POST /s?ie=utf-8&word=123%E5%93%88%E5%93%88 HTTP/1.1\r\n" +
//                "Host: www.baidu.com\r\n" +
//                "Connection: keep-alive\r\n" +
//                "Upgrade-Insecure-Requests: 1\r\n" +
//                "Cookie: a=b; c=3; fuck=123\r\n" +
//                "Content-Type: application/x-www-form-urlencoded\r\n" +
//                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36\r\n" +
//                "\r\n" +
//                "post=ppp&ok=ok";
////        HttpRequest request = new HttpRequest(req.getBytes());
//    }
}

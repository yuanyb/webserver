package org.webserver.http.response;

import org.webserver.constant.HttpConstant;
import org.webserver.http.Cookie;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpResponse {
    private HttpStatus status;
    private Map<String, String> headers;
    private List<Cookie> cookies;
    private ByteArrayOutputStream content;

    public HttpResponse() {
        this.headers = new HashMap<>();
        this.cookies = new ArrayList<>();
        this.content = new ByteArrayOutputStream();
        this.status = HttpStatus.SC_200;
    }

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setContentType(String contentType) {
        this.headers.put(HttpConstant.CONTENT_TYPE, contentType);
    }

    public void setContentLength(long length) {
        this.headers.put(HttpConstant.CONTENT_LENGTH, length + "");
    }

    public void setCharacterEncoding(String charset) {
        this.headers.put(HttpConstant.CONTENT_ENCODING, charset);
    }

    public void sendError(int sc, String msg) {
        switch (sc) {
            case 400:
                this.status = HttpStatus.SC_400;
                break;
            case 403:
                this.status = HttpStatus.SC_403;
                break;
            case 404:
                this.status = HttpStatus.SC_404;
                break;
            case 500:
                this.status = HttpStatus.SC_500;
                break;
        }
        this.content.reset();
        try {
            this.headers.put(HttpConstant.CONTENT_ENCODING, "UTF-8");
            this.content.write(msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignore) {
        }
    }

    public void sendRedirect(String location) {
        addHeader(HttpConstant.LOCATION, location);
        this.status = HttpStatus.SC_302;
    }

    /**
     * ByteBuffer - Scatter/Gather
     */
    public ByteBuffer[] getResponseData() {
        byte[] bytes = content.toByteArray();
        this.headers.put(HttpConstant.CONTENT_LENGTH, bytes.length + "");
        return new ByteBuffer[] {buildHeader(), ByteBuffer.wrap(bytes)};
    }

    private ByteBuffer buildHeader() {
        StringBuilder sb = new StringBuilder();
        // 响应行
        sb.append(HttpConstant.PROTOCOL)
                .append(" ")
                .append(status.getCode())
                .append(" ")
                .append(status.getReason())
                .append(HttpConstant.CRLF);
        // 头部信息
        //GMT
        sb.append(HttpConstant.DATE).append(": ")
                .append(ZonedDateTime.now(ZoneOffset.of("Z")).format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .append(HttpConstant.CRLF);
        headers.forEach((k, v) -> {
            sb.append(k).append(": ").append(v).append(HttpConstant.CRLF);
        });
        // Cookie
        cookies.forEach(cookie -> {
            sb.append(HttpConstant.SET_COOKIE)
                    .append(": ")
                    .append(cookie.getName())
                    .append("=")
                    .append(cookie.getValue())
                    .append("; ");
            if (cookie.getMaxAge() != -1)
                sb.append("max-age=").append(cookie.getMaxAge());
            sb.append(HttpConstant.CRLF);
        });

        sb.append(HttpConstant.CRLF);
        return ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.US_ASCII));
    }

    public Writer getWriter() {
        return new OutputStreamWriter(content);
    }

    public OutputStream getOutputStream() {
        return content;
    }
}

package org.webserver.http.response;

import org.webserver.constant.HttpConstant;
import org.webserver.http.request.Cookie;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpResponse {
    private HttpStatus status;
    private Map<String, String> headers;
    private List<Cookie> cookies;
    private ByteArrayOutputStream content;
    private String responsePath; // 要去哪个路径渲染

    public HttpResponse() {
        this.headers = new HashMap<>();
        this.cookies = new ArrayList<>();
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

    public String getResponsePath() {
        return this.responsePath;
    }

    public void setResponsePath(String path) {
        this.responsePath = path;
    }

    /**
     * ByteBuffer - Scatter/Gather
     */
    public ByteBuffer[] getResponseData() {
        return new ByteBuffer[] {buildHeader(), ByteBuffer.wrap(content.toByteArray())};
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
            sb.append("Max-Age=").append(cookie.getMaxAge()).append(HttpConstant.CRLF);
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

    public static void main(String[] args) throws IOException {
        HttpResponse response = new HttpResponse();
//        response.setContent("hello!!!".getBytes());
        response.addHeader("Server", "XXX");
        response.setContentType("text/html");
        response.addCookie(new Cookie("ca", "cv"));
        response.addCookie(new Cookie("cb", "cv"));
        response.setStatus(HttpStatus.SC_200);
        ByteBuffer[] buffer = response.getResponseData();
    }
}

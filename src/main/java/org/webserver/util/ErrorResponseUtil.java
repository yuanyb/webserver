package org.webserver.util;

import org.webserver.http.response.HttpResponse;
import org.webserver.http.response.HttpStatus;

import java.io.IOException;

public class ErrorResponseUtil {
    // todo error page
    public static void renderErrorResponse(HttpResponse response, HttpStatus status, String msg) {
        response.setStatus(status);
        response.setContentType("text/html; charset=utf-8");
        switch (status) {
            case SC_400:
                msg = "<h3 style='color:red;'>400请求头错误 " + msg + "</h3>";
                break;
            case SC_403:
                msg = "<h3 style='color:red;'>403禁止访问 " + msg + "</h3>";
                break;
            case SC_404:
                msg = "<h3 style='color:red;'>404资源不存在 " + msg + "</h3>";
                break;
            case SC_500:
                msg = "<h3 style='color:red;'>500服务器内部错误 " + msg + "</h3>";
                break;
        }
        try {
            response.getWriter().write(msg);
        } catch (IOException ignore) {
        }
    }
}

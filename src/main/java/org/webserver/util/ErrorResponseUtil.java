package org.webserver.util;

import org.webserver.http.response.HttpResponse;
import org.webserver.http.response.HttpStatus;

import java.io.IOException;

public class ErrorResponseUtil {
    public static void renderErrorResponse(HttpResponse response, HttpStatus status, String msg) {
        response.setStatus(status);
        try {
            response.getWriter().write(msg);
        } catch (IOException ignore) {
        }
    }
}

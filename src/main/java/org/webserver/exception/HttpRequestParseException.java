package org.webserver.exception;

import org.webserver.http.request.HttpRequest;

public class HttpRequestParseException extends Exception {
    public HttpRequestParseException() {
    }

    public HttpRequestParseException(String msg) {
        super(msg);
    }
}

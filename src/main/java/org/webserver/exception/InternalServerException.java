package org.webserver.exception;

/**
 * 服务器异常
 */
public class InternalServerException extends Exception {
    public InternalServerException() {
    }

    public InternalServerException(String msg) {
        super(msg);
    }
}

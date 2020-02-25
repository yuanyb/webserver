package org.webserver.http.response;

import org.webserver.constant.HttpConstant;

public enum HttpStatus {
    SC_200(200, "OK"),
    SC_302(302, "Move Temporarily"),
    SC_400(400, "Bad Request"),
    SC_403(403, "Forbidden"),
    SC_404(404, "File Not Found"),
    SC_500(500, "Internal Server Error");

    private int code;
    private String reason;

    HttpStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return this.reason;
    }

    public String toResponseLine() {
        return "HTTP/1.1 " + this.code + " " + this.reason + HttpConstant.CRLF;
    }
}

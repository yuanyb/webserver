package org.webserver.http;

public enum HttpMethod {
    HEAD("HEAD"),
    GET("GET"),
    POST("POST");

    private String name;

    HttpMethod(String name) {
        this.name = name;
    }

    public static HttpMethod get(String name) {
        switch (name) {
            case "HEAD":
                return HEAD;
            case "POST":
                return POST;
            default:
                return GET;
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}

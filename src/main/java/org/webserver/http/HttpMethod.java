package org.webserver.http;

public enum HttpMethod {
    HEAD("HEAD"),
    GET("GET"),
    POST("POST"),
    ANY("ANY");

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
            case "GET":
                return GET;
            default:
                return ANY;
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}

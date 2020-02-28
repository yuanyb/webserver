package org.webserver.http;

public class Cookie {
    private final String name;
    private String value;
    private int maxAge = -1;


    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public void setMaxAge(int expiry) {
        this.maxAge = expiry;
    }

    public int getMaxAge() {
        return this.maxAge;
    }

    public String getName() {
        return this.name;
    }

    public void setValue(String newValue) {
        this.value = newValue;
    }

    public String getValue() {
        return this.value;
    }
}

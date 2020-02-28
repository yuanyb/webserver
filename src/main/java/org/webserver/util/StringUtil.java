package org.webserver.util;

public class StringUtil {
    /**
     * 传入域名，返回对应的 getter 方法名
     */
    public static String getterName(String filed) {
        return "get" + Character.toUpperCase(filed.charAt(0)) + filed.substring(1);
    }

    /**
     * 传入域名，返回对应的 setter 方法名
     */
    public static String setterName(String filed) {
        return "set" + Character.toUpperCase(filed.charAt(0)) + filed.substring(1);
    }
}

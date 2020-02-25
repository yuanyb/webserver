package org.webserver.container;

import org.webserver.constant.ServerConfig;

import java.util.Map;

public class ControllerScanner {
    private final static String WEBAPP_ROOT_PATH = System.getProperty(ServerConfig.WEBAPP_ROOT_PATH);

    static Map<String, TargetMethod> scan() {
        return null;
    }
}

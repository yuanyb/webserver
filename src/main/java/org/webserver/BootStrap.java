package org.webserver;


import org.webserver.connector.Server;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class BootStrap {
    public static void main(String[] args) throws IOException {
        initConfig();
        Server server = new Server();
        server.start(Integer.parseInt(System.getProperty("PORT")));
    }

    private static void initConfig() throws IOException {
        Properties configs = new Properties();
        configs.load(BootStrap.class.getResourceAsStream("/server-config.properties"));
        configs.forEach((k, v) -> {
            System.setProperty(k.toString(), v.toString());
        });
    }
}

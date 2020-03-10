package org.webserver;


import org.webserver.connector.Server;
import org.webserver.constant.ServerConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.*;

public class BootStrap {
    public static void run() {
        try {
            initConfig();
            initLogger();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("初始化配置文件或日志器失败");
            return;
        }
        Server server = new Server();
        server.start(Integer.parseInt(System.getProperty(ServerConfig.PORT)));
        System.out.println("输入exit结束服务器");
        Scanner in = new Scanner(System.in);
        while (!in.nextLine().equals("exit")) {}
        server.close();
    }

    private static void initConfig() throws IOException {
        Properties configs = new Properties();
        if (Files.exists(Path.of(BootStrap.class.getResource("/").toString().substring(6) + "/server-config.properties"))) {
            configs.load(BootStrap.class.getResourceAsStream("/server-config.properties"));
        } else {
            configs.load(BootStrap.class.getResourceAsStream("/default-server-config.properties"));
        }
        configs.forEach((k, v) -> {
            System.setProperty(k.toString(), v.toString());
        });
    }

    private static void initLogger() throws IOException {
        String loggerPath = System.getProperty(ServerConfig.LOG_FILE_STORAGE_PATH);
        if (loggerPath.indexOf(loggerPath.length()-1) != File.separator.charAt(0)) {
            loggerPath += File.separator;
        }
        FileHandler accessLoggerHandler = new FileHandler(loggerPath + "access-%u.log");
        FileHandler serverLoggerHandler = new FileHandler(loggerPath + "server-%u.log");
        accessLoggerHandler.setFormatter(new AccessLoggerFormatter());
        serverLoggerHandler.setFormatter(new ServerLoggerFormatter());
        Logger.getLogger("http_access").addHandler(accessLoggerHandler);
        Logger.getLogger("").addHandler(serverLoggerHandler); // 全局
    }


    private static class AccessLoggerFormatter extends Formatter {
        /** Logger 线程安全 */
        private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private static Date date = new Date();
        /**
         * record.getMessage(): IP:Port|RequestURI|ResponseCode|ResponseLength
         */
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            date.setTime(record.getMillis());
            sb.append(dateFormat.format(date)).append("|")
                    .append(record.getMessage()).append("\n");
            return sb.toString();
        }
    }

    private static class ServerLoggerFormatter extends Formatter {
        /** Logger 线程安全 */
        private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private static Date date = new Date();
        /**
         * Level|Location|DateTime|Message
         */
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            date.setTime(record.getMillis());
            sb.append(record.getLevel()).append("|")
                    .append(record.getSourceClassName()).append("#")
                    .append(record.getSourceMethodName()).append("|")
                    .append(dateFormat.format(date)).append("|")
                    .append(record.getMessage()).append("\n");
            return sb.toString();
        }
    }
}

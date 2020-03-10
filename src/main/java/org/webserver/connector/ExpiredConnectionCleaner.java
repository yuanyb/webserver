package org.webserver.connector;

import org.webserver.constant.ServerConfig;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 清除长时间没有数据交换的HTTP连接，用于支持HTTP默认的长连接
 */
public class ExpiredConnectionCleaner {
    private static final Logger logger = Logger.getLogger(ExpiredConnectionCleaner.class.getPackageName());
    /** 清理过期连接线程的周期 */
    private static final long CLEANING_CYCLE = Long.parseLong(System.getProperty(ServerConfig.CONNECTION_CLEANING_CYCLE));
    private List<Poller> pollers;

    private ScheduledExecutorService cleaner;

    ExpiredConnectionCleaner(List<Poller> pollers) {
        this.pollers = pollers;
    }

    void start() {
        this.cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleAtFixedRate(() -> {
            for (Poller poller : pollers) {
                logger.info(String.format("开始清理 %s 的过期连接", poller.getPollerName()));
                poller.clearExpiredConnection();
            }
        }, 0, CLEANING_CYCLE, TimeUnit.MILLISECONDS);
    }

    void shutDown() {
        logger.info("终止过期连接清理器");
        this.cleaner.shutdown();
    }

    public static void main(String[] args) {
    }
}

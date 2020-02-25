package org.webserver.http.session;

import org.webserver.connector.ExpiredConnectionCleaner;
import org.webserver.constant.ServerConfig;
import org.webserver.container.Container;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 定时清理过期的Session
 */
public class ExpiredSessionCleaner {
    private final static Logger logger = Logger.getLogger(ExpiredConnectionCleaner.class.getPackageName());
    private final static long SESSION_CLEANING_CYCLE = Long.parseLong(System.getProperty(ServerConfig.SESSION_CLEANING_CYCLE));
    private final static ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
    private Container container;

    public ExpiredSessionCleaner(Container container) {
        this.container = container;
    }

    public void start() {
        logger.info("启动过期Session清理器");
        schedule.scheduleAtFixedRate(container::clearExpiredSession, 0, SESSION_CLEANING_CYCLE, TimeUnit.MILLISECONDS);
    }

    public void shutDown() {
        logger.info("终止过期Session清理器");
        schedule.shutdown();
    }
}

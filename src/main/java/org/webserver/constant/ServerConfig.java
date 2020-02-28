package org.webserver.constant;

/**
 * 配置项名
 */
public class ServerConfig {
    /** 监听端口 */
    public static final String PORT = "PORT";
    /** 日志文件保存目录 */
    public static final String LOG_FILE_STORAGE_PATH = "LOG_FILE_STORAGE_PATH";
    /** 毫秒 */
    public static final String CONNECTION_EXPIRY_TIME = "CONNECTION_EXPIRY_TIME";
    /** 毫秒 */
    public static final String CONNECTION_CLEANING_CYCLE = "CONNECTION_CLEANING_CYCLE";
    /** 毫秒 */
    public static final String SESSION_CLEANING_CYCLE = "SESSION_CLEANING_CYCLE";
    /** 毫秒 */
    public static final String SESSION_EXPIRY_TIME = "SESSION_EXPIRY_TIME";
    /** 轮询线程池大小 */
    public static final String POLLER_THREAD_COUNT = "POLLER_THREAD_COUNT";
    /** 请求处理器线程池大小 */
    public static final String REQUEST_PROCESSOR_THREAD_COUNT = "REQUEST_PROCESSOR_THREAD_COUNT";

}

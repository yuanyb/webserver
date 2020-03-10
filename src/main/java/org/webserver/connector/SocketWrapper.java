package org.webserver.connector;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 客户端socket的包装器
 */
public class SocketWrapper {
    private SocketChannel client;
    private Poller poller;
    private long lastCommutationTime;

    SocketWrapper(SocketChannel client, Poller poller) {
        this.client = client;
        this.poller = poller;
        this.lastCommutationTime = System.currentTimeMillis();
    }

    /**
     * 关闭Socket
     */
    void close() throws IOException {
        poller.cancelReadListening(this); // 取消监听该连接
        poller.getClients().remove(this.getClient());
        client.close(); // 关闭连接
    }

    /**
     * 返回上次通信的时间戳
     */
    long getLastCommutationTime() {
        return lastCommutationTime;
    }

    /**
     * 设置上次通信的时间戳
     */
    void setLastCommutationTime(long lastCommutationTime) {
        this.lastCommutationTime = lastCommutationTime;
    }

    /**
     * 获取Socket
     * @return
     */
    public SocketChannel getClient() {
        return client;
    }

    /**
     * 获取所属的Poller对象
     * @return
     */
    public Poller getPoller() {
        return poller;
    }

    @Override
    public String toString() {
        return "SocketWrapper[" + client + "]";
    }
}

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

    void close() throws IOException {
        poller.cancelReadListening(this); // 取消监听该连接
        poller.getClients().remove(this);
        client.close(); // 关闭连接
    }

    long getLastCommutationTime() {
        return lastCommutationTime;
    }

    void setLastCommutationTime(long lastCommutationTime) {
        this.lastCommutationTime = lastCommutationTime;
    }

    public SocketChannel getClient() {
        return client;
    }

    @Override
    public String toString() {
        return "SocketWrapper[" + client + "]";
    }
}

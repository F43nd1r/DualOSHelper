package com.faendir.clipboardshare.net;

import com.faendir.clipboardshare.threading.TaskManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lukas
 * @since 26.04.18
 */
public class Server extends SocketLifeCycleManager{
    public static final int PORT = 6028;
    private final InetAddress inetAddress;
    private ServerSocket socket;

    public Server(InetAddress inetAddress, TaskManager taskManager) {
        super(taskManager);
        this.inetAddress = inetAddress;
    }

    @Override
    protected void prepare() {
        try {
            socket = new ServerSocket(PORT, 0, inetAddress);
            socket.setSoTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void release() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Socket acquireSocket() {
        try {
            return socket.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

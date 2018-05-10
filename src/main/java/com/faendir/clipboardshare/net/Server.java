package com.faendir.clipboardshare.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lukas
 * @since 26.04.18
 */
public class Server extends SocketLifeCycleManager<ServerSocket>{
    public static final int PORT = 6028;
    private final InetAddress inetAddress;

    public Server(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    @Override
    protected ServerSocket prepare() throws IOException {
        ServerSocket socket = new ServerSocket(PORT, 0, inetAddress);
        socket.setSoTimeout(1000);
        return socket;
    }

    @Override
    protected void release(ServerSocket serverSocket) throws IOException {
        serverSocket.close();
    }

    @Override
    protected Socket acquireSocket(ServerSocket serverSocket) throws IOException {
        return serverSocket.accept();
    }
}

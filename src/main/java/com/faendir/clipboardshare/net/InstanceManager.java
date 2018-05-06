package com.faendir.clipboardshare.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.function.Consumer;

/**
 * @author lukas
 * @since 06.05.18
 */
public class InstanceManager {
    private static final int PORT = 6029;
    private Thread thread;
    private volatile boolean stop = false;

    public boolean start(String[] args, Consumer<String[]> futureArgs) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
        stop = false;
        try {
            ServerSocket serverSocket = new ServerSocket(PORT, 0, inetAddress);
            serverSocket.setSoTimeout(1000);
            thread = new Thread(() -> {
                while (!stop) {
                    if (serverSocket.isClosed()) {
                        stop = true;
                    }
                    try (Socket socket = serverSocket.accept();
                         DataInputStream in = new DataInputStream(socket.getInputStream())) {
                        int count = in.readInt();
                        String[] newArgs = new String[count];
                        for (int i = 0; i < count; i++) {
                            newArgs[i] = in.readUTF();
                        }
                        futureArgs.accept(newArgs);
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (!serverSocket.isClosed()) {
                    try {
                        serverSocket.close();
                    } catch (IOException ignored) {
                    }
                }
            });
            thread.start();
            return true;
        } catch (IOException e) {
            try (Socket socket = new Socket(inetAddress, PORT);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                out.writeInt(args.length);
                for (String arg : args) {
                    out.writeUTF(arg);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return false;
        }
    }

    public void stop() throws InterruptedException {
        if (!stop) {
            stop = true;
            thread.interrupt();
            thread.join();
        }
    }
}

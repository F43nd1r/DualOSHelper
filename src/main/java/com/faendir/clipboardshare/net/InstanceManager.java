package com.faendir.clipboardshare.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lukas
 * @since 06.05.18
 */
public class InstanceManager {
    private static final int PORT = 6029;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<Listener> listeners;
    private Thread thread;
    private volatile boolean stop = false;

    public InstanceManager() {
        listeners = new ArrayList<>();
    }

    public boolean start(String[] args) {
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
            logger.debug("Successfully opened port " + PORT + ". This is the first instance.");
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
                        logger.debug("Received args from another instance: " + Arrays.toString(newArgs));
                        listeners.forEach(listener -> listener.onNewArgs(newArgs));
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
            logger.debug("Failed to open port " + PORT + ". Usually this means another instance is already running. Sending args to that instance...");
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

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void onNewArgs(String[] args);
    }
}

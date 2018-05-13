package com.faendir.clipboardshare.net;

import dorkbox.systemTray.SystemTray;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

/**
 * @author lukas
 * @since 10.05.18
 */
public abstract class SocketLifeCycleManager<T> {
    private volatile Connector connector = null;
    private volatile boolean stop = false;

    public void start() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            SystemTray systemTray = SystemTray.get();
            Image logo = ImageIO.read(getClass().getResource("mylogo.png"));
            systemTray.setImage(logo);
            systemTray.setTooltip("Waiting for connection");
            systemTray.getMenu().add(new dorkbox.systemTray.MenuItem("Stop", e -> {
                synchronized (this) {
                    stop = true;
                    notifyAll();
                }
            }));
            T t = prepare();
            Thread mainLoopThread = new Thread(() -> {
                while (!stop) {
                    try {
                        Socket client = acquireSocket(t);
                        systemTray.setTooltip("Connected");
                        Connector connector = new Connector(client, clipboard);
                        this.connector = connector;
                        connector.run();
                        systemTray.setTooltip("Disconnected");
                        this.connector = null;
                    } catch (IOException | InterruptedException ignored) {
                    }
                }
            });
            try {
                mainLoopThread.start();
                while (!stop) {
                    synchronized (this) {
                        wait();
                    }
                }
                if (connector != null) {
                    connector.stop();
                }
            } finally {
                systemTray.shutdown();
                release(t);
                if (mainLoopThread.isAlive()) {
                    mainLoopThread.join();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract T prepare() throws IOException;

    protected abstract void release(T t) throws IOException;

    protected abstract Socket acquireSocket(T t) throws IOException;

    protected Optional<Connector> getConnector() {
        return Optional.ofNullable(connector);
    }
}

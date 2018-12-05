package com.faendir.clipboardshare.net;

import dorkbox.systemTray.SystemTray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private volatile Connector connector = null;
    private volatile boolean stop = false;

    public void start() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            SystemTray systemTray = SystemTray.get();
            Image logo = ImageIO.read(getClass().getResource("mylogo.png"));
            systemTray.setImage(logo);
            setStatus(systemTray, "Waiting for connection");
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
                        logger.debug("Acquiring socket...");
                        Socket socket = acquireSocket(t);
                        logger.debug("Acquired socket");
                        setStatus(systemTray, "Connected");
                        Connector connector = new Connector(socket, clipboard);
                        this.connector = connector;
                        logger.debug("Running connector...");
                        connector.run();
                        logger.debug("Connector returned");
                    } catch (IOException ignored) {
                    } finally {
                        setStatus(systemTray, "Disconnected");
                        this.connector = null;
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

    private void setStatus(SystemTray systemTray, String status) {
        systemTray.setStatus(status);
        systemTray.setTooltip(status);
    }
}

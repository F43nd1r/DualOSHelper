package com.faendir.clipboardshare.net;

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
            PopupMenu menu = new PopupMenu();
            menu.add(new MenuItem("Stop")).addActionListener(e -> {
                synchronized (this) {
                    stop = true;
                    notifyAll();
                }
            });
            TrayIcon trayIcon = new TrayIcon(
                    ImageIO.read(getClass().getResource("mylogo.png")).getScaledInstance(SystemTray.getSystemTray().getTrayIconSize().width, -1, Image.SCALE_SMOOTH));
            trayIcon.setPopupMenu(menu);
            T t = prepare();
            Thread mainLoopThread = new Thread(() -> {
                while (!stop) {
                    try {
                        Socket client = acquireSocket(t);
                        Connector connector = new Connector(client, clipboard);
                        this.connector = connector;
                        connector.run();
                        this.connector = null;
                    } catch (IOException | InterruptedException ignored) {
                    }
                }
            });
            try {
                SystemTray.getSystemTray().add(trayIcon);
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
                SystemTray.getSystemTray().remove(trayIcon);
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

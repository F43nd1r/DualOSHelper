package com.faendir.clipboardshare.net;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lukas
 * @since 26.04.18
 */
public class Server {
    public static final int PORT = 6028;
    private final InetAddress inetAddress;
    private volatile Connector connector = null;
    private volatile boolean stop = false;

    public Server(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public void start() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try (ServerSocket serverSocket = new ServerSocket(PORT, 0, inetAddress)) {
            Image logo = ImageIO.read(getClass().getResource("mylogo.png"));
            TrayIcon trayIcon = new TrayIcon(logo.getScaledInstance(SystemTray.getSystemTray().getTrayIconSize().width, -1, Image.SCALE_SMOOTH));
            PopupMenu menu = new PopupMenu();
            menu.add(new MenuItem("Stop")).addActionListener(e -> {
                synchronized (this) {
                    stop = true;
                    notifyAll();
                }
            });
            trayIcon.setPopupMenu(menu);
            SystemTray.getSystemTray().add(trayIcon);
            Thread mainLoopThread = new Thread(() -> {
                while (!stop) {
                    try {
                        Socket client = serverSocket.accept();
                        Connector connector = new Connector(client, clipboard);
                        this.connector = connector;
                        connector.run();
                        this.connector = null;
                    } catch (IOException | InterruptedException ignored) {
                    }
                }
            });
            mainLoopThread.start();
            while (!stop) {
                synchronized (this) {
                        wait();
                }
            }
            if(connector != null) {
                connector.stop();
            }
            serverSocket.close();
            mainLoopThread.join();
            SystemTray.getSystemTray().remove(trayIcon);
        } catch (IOException | AWTException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

package com.faendir.clipboardshare.net;

import com.faendir.clipboardshare.message.Command;
import com.faendir.clipboardshare.message.StringMessage;
import com.tulskiy.keymaster.common.Provider;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

/**
 * @author lukas
 * @since 27.04.18
 */
public class Client {
    private final InetAddress host;
    private final Map<KeyStroke, String> hotkeys;
    private Connector connector;

    public Client(InetAddress host, Map<KeyStroke, String> hotkeys) {
        this.host = host;
        this.hotkeys = hotkeys;
    }

    public void start() {
        Provider hotKeyProvider = Provider.getCurrentProvider(false);
        TrayIcon trayIcon = null;
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Socket socket = new Socket(host, Server.PORT);
            connector = new Connector(socket, clipboard);
            hotkeys.forEach((keyStroke, command) -> hotKeyProvider.register(keyStroke, e -> connector.send(new StringMessage(Command.KEY_STROKE, command))));
            Image logo = ImageIO.read(getClass().getResource("mylogo.png"));
            trayIcon = new TrayIcon(logo.getScaledInstance(SystemTray.getSystemTray().getTrayIconSize().width, -1, Image.SCALE_SMOOTH));
            PopupMenu menu = new PopupMenu();
            menu.add(new MenuItem("Stop")).addActionListener(e -> connector.stop());
            trayIcon.setPopupMenu(menu);
            SystemTray.getSystemTray().add(trayIcon);
            connector.run();
        } catch (IOException | AWTException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
            }
            hotKeyProvider.reset();
            hotKeyProvider.stop();
        }
    }

    public void handleUrl(String url) {
        connector.send(new StringMessage(Command.URL_CONTENT, url));
    }
}

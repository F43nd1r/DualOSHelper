package com.faendir.clipboardshare.net;

import com.faendir.clipboardshare.message.Command;
import com.faendir.clipboardshare.message.StringMessage;
import com.faendir.clipboardshare.threading.TaskManager;
import com.tulskiy.keymaster.common.Provider;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author lukas
 * @since 27.04.18
 */
public class Client extends SocketLifeCycleManager {
    private final InetAddress host;
    private final Map<KeyStroke, String> hotkeys;
    private Provider provider;

    public Client(InetAddress host, Map<KeyStroke, String> hotkeys, TaskManager taskManager) {
        super(taskManager);
        this.host = host;
        this.hotkeys = hotkeys;
    }

    @Override
    protected void prepare() {
        provider = Provider.getCurrentProvider(false);
        hotkeys.forEach((keyStroke, command) -> provider.register(keyStroke, e -> getConnector().ifPresent(connector -> connector.send(new StringMessage(Command.KEY_STROKE, command)))));
    }

    @Override
    protected void release() {
        provider.reset();
        provider.stop();
    }

    @Override
    protected Socket acquireSocket() {
        try {
            return new Socket(host, Server.PORT);
        } catch (IOException e) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignored) {
            }
            throw new RuntimeException(e);
        }
    }

    public void handleUrl(String url) {
        Optional<Connector> connector = getConnector();
        if (connector.isPresent()) {
            connector.get().send(new StringMessage(Command.URL_CONTENT, url));
        } else {
            try {
                Runtime.getRuntime().exec("\"C:\\Program Files\\Mozilla Firefox\\firefox.exe\" -url " + url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

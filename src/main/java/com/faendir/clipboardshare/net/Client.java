package com.faendir.clipboardshare.net;

import com.faendir.clipboardshare.message.Command;
import com.faendir.clipboardshare.message.StringMessage;
import com.tulskiy.keymaster.common.Provider;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

/**
 * @author lukas
 * @since 27.04.18
 */
public class Client extends SocketLifeCycleManager<Provider> {
    private final InetAddress host;
    private final Map<KeyStroke, String> hotkeys;

    public Client(InetAddress host, Map<KeyStroke, String> hotkeys) {
        this.host = host;
        this.hotkeys = hotkeys;
    }

    @Override
    protected Provider prepare() {
        Provider provider = Provider.getCurrentProvider(false);
        hotkeys.forEach(
                (keyStroke, command) -> provider.register(keyStroke, e -> getConnector().ifPresent(connector -> connector.send(new StringMessage(Command.KEY_STROKE, command)))));
        return provider;
    }

    @Override
    protected void release(Provider provider) {
        provider.reset();
        provider.stop();
    }

    @Override
    protected Socket acquireSocket(Provider provider) throws IOException {
        return new Socket(host, Server.PORT);
    }

    public void handleUrl(String url) {
        getConnector().ifPresent(connector -> connector.send(new StringMessage(Command.URL_CONTENT, url)));
    }
}

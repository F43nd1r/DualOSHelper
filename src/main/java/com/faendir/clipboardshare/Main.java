package com.faendir.clipboardshare;

import com.faendir.clipboardshare.net.Client;
import com.faendir.clipboardshare.net.InstanceManager;
import com.faendir.clipboardshare.net.Server;
import com.faendir.clipboardshare.threading.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.swing.*;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author lukas
 * @since 26.04.18
 */
public class Main {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class);
        TaskManager taskManager = new TaskManager();
        InstanceManager instanceManager = new InstanceManager(taskManager);
        if (instanceManager.start(args)) {
            try {
                CommandLine commandLine = new CommandLine(new MainCommand(instanceManager, taskManager));
                commandLine.registerConverter(InetAddress.class, InetAddress::getByName);
                commandLine.registerConverter(KeyStroke.class, KeyStroke::getKeyStroke);
                commandLine.execute(args);
            } finally {
                instanceManager.stop();
            }
        } else {
            logger.info("Another instance is already running. Start parameters have been passed to that instance.");
        }
        System.exit(0);
    }

    @CommandLine.Command(name = "", mixinStandardHelpOptions = true, version = "1.0")
    public static class MainCommand implements Callable<Integer> {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final InstanceManager instanceManager;
        private final TaskManager taskManager;
        @CommandLine.Option(names = {"-s", "--server"}, arity = "0..1", description = "start in server mode, optionally restricted to listening on an address")
        InetAddress server;
        @CommandLine.Option(names = {"-c", "--client"}, description = "start in client mode, connecting to the server at the given address")
        InetAddress client;
        @CommandLine.Option(names = {"-u", "--url"}, description = "send url to server browser")
        String url;
        @CommandLine.Option(names = {"-k", "--hotkey"}, description = "run the command on the host when keystroke is pressed")
        Map<KeyStroke, String> hotkeys = new LinkedHashMap<>();

        MainCommand(InstanceManager instanceManager, TaskManager taskManager) {
            this.instanceManager = instanceManager;
            this.taskManager = taskManager;
        }


        @Override
        public Integer call() {
            if (server != null) {
                Server server = new Server(this.server, taskManager);
                try {
                    server.start();
                } finally {
                    logger.info("Server stopped");
                }
            } else if (client != null) {
                Client client = new Client(this.client, this.hotkeys, taskManager);
                InstanceManager.Listener listener = a -> {
                    if (url != null) {
                        client.handleUrl(url);
                    }
                };
                instanceManager.addListener(listener);
                logger.info("Starting client...");
                try {
                    client.start();
                } finally {
                    logger.info("Client stopped");
                }
                instanceManager.removeListener(listener);
            }
            return 0;
        }
    }
}

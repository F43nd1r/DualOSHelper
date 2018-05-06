package com.faendir.clipboardshare;

import com.faendir.clipboardshare.net.Client;
import com.faendir.clipboardshare.net.InstanceManager;
import com.faendir.clipboardshare.net.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.function.Consumer;

/**
 * @author lukas
 * @since 26.04.18
 */
public class Main {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class);
        InstanceManager instanceManager = new InstanceManager();
        final NewArgHandler newArgHandler = new NewArgHandler();
        if(instanceManager.start(args, newArgHandler)) {
            try {
                Arguments arguments = Arguments.parse(args);
                if (arguments.printHelp) {
                    logger.info("Supported arguments:");
                    logger.info("-h|--help print this message");
                    logger.info("-s|--server [<InetAddress>] start in server mode, optionally restricted to listening on InetAddress");
                    logger.info("-c|--client <InetAddress> start in client mode, connecting to the server at InetAddress");
                } else if (arguments.server) {
                    Server server = new Server(arguments.address);
                    logger.info("Starting server...");
                    try {
                        server.start();
                    } finally {
                        logger.info("Server stopped");
                    }
                } else if (arguments.client) {
                    Client client = new Client(arguments.address);
                    newArgHandler.urlHandler = client::handleUrl;
                    logger.info("Starting client...");
                    try {
                        client.start();
                    } finally {
                        logger.info("Client stopped");
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.error("Failed to parse parameters: " + e.getMessage());
            } finally {
                try {
                    instanceManager.stop();
                } catch (InterruptedException ignored) {
                }
            }
        }else {
            logger.info("Another instance is already running. Start parameters have been passed to that instance.");
        }
        System.exit(0);
    }

    public static class Arguments {
        public boolean server = false;
        public boolean client = false;
        public InetAddress address = null;
        public boolean printHelp = false;
        public String url = null;

        public static Arguments parse(String[] args) throws IllegalArgumentException {
            if (args.length == 0) throw new IllegalArgumentException("Expected at least one parameter");
            ListIterator<String> iterator = Arrays.asList(args).listIterator();
            Arguments arguments = new Arguments();
            while (iterator.hasNext()) {
                String arg;
                String arg2;
                switch (arg = iterator.next()) {
                    case "-h":
                    case "--help":
                        arguments.printHelp = true;
                        break;
                    case "-s":
                    case "--server":
                        arguments.server = true;
                        if (iterator.hasNext()) {
                            arg2 = iterator.next();
                            if (arg2.charAt(0) != '-') {
                                try {
                                    arguments.address = InetAddress.getByName(arg2);
                                } catch (UnknownHostException e) {
                                    throw new IllegalArgumentException(arg2 + " is not a valid InetAddress", e);
                                }
                            } else {
                                iterator.previous();
                            }
                        }
                        break;
                    case "-c":
                    case "--client":
                        arguments.client = true;
                        if (iterator.hasNext() && (arg2 = iterator.next()).charAt(0) != '-') {
                            try {
                                arguments.address = InetAddress.getByName(arg2);
                            } catch (UnknownHostException e) {
                                throw new IllegalArgumentException(arg2 + " is not a valid InetAddress", e);
                            }
                        } else {
                            throw new IllegalArgumentException("Client requires an InetAddress parameter");
                        }
                        break;
                    case "-u":
                    case "--url":
                        if (iterator.hasNext() && (arg2 = iterator.next()).charAt(0) != '-' && (arg2.startsWith("http://") || arg2.startsWith("https://"))) {
                            arguments.url = arg2;
                        } else {
                            throw new IllegalArgumentException("--url requires an url parameter");
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown parameter " + arg);
                }
            }
            return arguments;
        }
    }

    private static class NewArgHandler implements Consumer<String[]> {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private Consumer<String> urlHandler = null;

        @Override
        public void accept(String[] newArgs) {
            logger.debug("Received args from another instance: "+ Arrays.toString(newArgs));
            Arguments arguments = Arguments.parse(newArgs);
            if (urlHandler != null && arguments.url != null) {
                urlHandler.accept(arguments.url);
            }
        }
    }
}

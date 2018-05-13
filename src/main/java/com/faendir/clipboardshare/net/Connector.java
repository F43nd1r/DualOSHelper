package com.faendir.clipboardshare.net;

import com.faendir.clipboardshare.io.InputHandler;
import com.faendir.clipboardshare.io.OutputHandler;
import com.faendir.clipboardshare.message.Command;
import com.faendir.clipboardshare.message.Message;
import com.faendir.clipboardshare.message.StringMessage;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author lukas
 * @since 27.04.18
 */
public class Connector implements ClipboardOwner {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Socket socket;
    private final Clipboard clipboard;
    private final InputHandler in;
    private final OutputHandler out;
    private long lastHeartbeat;

    public Connector(Socket socket, Clipboard clipboard) {
        this.socket = socket;
        this.clipboard = clipboard;
        in = new InputHandler(socket);
        out = new OutputHandler(socket);
    }

    public void run() throws InterruptedException {
        gainOwnership();
        in.start();
        out.start();
        Thread hook = new Thread(this::stop);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        try {
            Runtime.getRuntime().addShutdownHook(hook);
            out.put(new Message(Command.SOCKET_READY));
            //noinspection StatementWithEmptyBody
            while (in.take().getCommand() != Command.SOCKET_READY) ;
            lastHeartbeat = System.currentTimeMillis();
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (TimeUnit.MILLISECONDS.toMinutes(lastHeartbeat) + 5 < TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis())) {
                    try {
                        logger.info("No heartbeat for 5 Minutes. Stopping...");
                        stopNow();
                    } catch (IOException | InterruptedException ignored) {
                    }
                } else {
                    out.put(new Message(Command.HEARTBEAT));
                }
            }, 3, 3, TimeUnit.MINUTES);
            logger.info("Socket connected");
            loop:
            while (!socket.isClosed() && !in.isStopped() && !out.isStopped()) {
                try {
                    Message message = in.take();
                    switch (message.getCommand()) {
                        case CB_STRING_CONTENT:
                            StringMessage cbMessage = (StringMessage) message;
                            logger.debug("Received remote clipboard content \"" + cbMessage.getMsg() + "\"");
                            clipboard.setContents(new StringSelection(cbMessage.getMsg()), this);
                            break;
                        case KEY_STROKE:
                            StringMessage keyStrokeMessage = (StringMessage) message;
                            logger.debug("Received keystroke command" + keyStrokeMessage.getMsg());
                            Runtime.getRuntime().exec(keyStrokeMessage.getMsg());
                            break;
                        case HEARTBEAT:
                            logger.debug("Received Heartbeat");
                            lastHeartbeat = System.currentTimeMillis();
                            break;
                        case URL_CONTENT:
                            StringMessage urlMessage = (StringMessage) message;
                            logger.debug("Received remote url "+ urlMessage.getMsg());
                            new BrowserLauncher().openURLinBrowser(urlMessage.getMsg());
                        case SOCKET_EXIT:
                            logger.debug("Received disconnect message");
                            stopNow();
                            break loop;
                        default:
                            logger.error("Invalid message " + message.getCommand());
                            break;
                    }
                } catch (InterruptedException ignored) {
                } catch (IOException | BrowserLaunchingInitializingException | UnsupportedOperatingSystemException e) {
                    e.printStackTrace();
                }
            }
            logger.info("Socket disconnected");
        } finally {
            scheduledExecutorService.shutdownNow();
            Runtime.getRuntime().removeShutdownHook(hook);
        }
    }

    public void send(Message message) {
        out.put(message);
    }

    public void stop() {
        try {
            out.put(new Message(Command.SOCKET_EXIT));
            TimeUnit.MILLISECONDS.sleep(100);
            stopNow();
        } catch (IOException | InterruptedException ignored) {
        }
    }

    private void stopNow() throws IOException, InterruptedException {
        out.stop();
        in.stop();
        socket.close();
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        if (!socket.isClosed()) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.debug("Local ClipBoard Changed");
            if (Arrays.asList(clipboard.getAvailableDataFlavors()).contains(DataFlavor.stringFlavor)) {
                try {
                    out.put(new StringMessage(Command.CB_STRING_CONTENT, (String) clipboard.getData(DataFlavor.stringFlavor)));
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                logger.debug("ClipBoard content not supported, not sending");
            }
            gainOwnership();
        }
    }

    private void gainOwnership() {
        clipboard.setContents(clipboard.getContents(null), this);
    }
}

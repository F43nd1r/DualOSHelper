package com.faendir.clipboardshare.net;

import com.faendir.clipboardshare.io.InputHandler;
import com.faendir.clipboardshare.io.OutputHandler;
import com.faendir.clipboardshare.message.Command;
import com.faendir.clipboardshare.message.KeyStrokeMessage;
import com.faendir.clipboardshare.message.Message;
import com.faendir.clipboardshare.message.StringMessage;

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
    private final Socket socket;
    private final Clipboard clipboard;
    private InputHandler in;
    private OutputHandler out;
    private long lastHeartbeat;

    public Connector(Socket socket, Clipboard clipboard) {
        this.socket = socket;
        this.clipboard = clipboard;
    }

    public void run() throws InterruptedException {
        gainOwnership();
        in = new InputHandler(socket);
        out = new OutputHandler(socket);
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
                        System.out.println("No heartbeat for 5 Minutes. Stopping...");
                        stopNow();
                    } catch (IOException | InterruptedException ignored) {
                    }
                } else {
                    out.put(new Message(Command.HEARTBEAT));
                }
            }, 3, 3, TimeUnit.MINUTES);
            System.out.println("Socket connected");
            loop:
            while (!socket.isClosed() && !in.isStopped() && !out.isStopped()) {
                try {
                    Message message = in.take();
                    switch (message.getCommand()) {
                        case CB_STRING_CONTENT:
                            StringMessage cbMessage = (StringMessage) message;
                            System.out.println("Received remote clipboard content \"" + cbMessage.getMsg() + "\"");
                            clipboard.setContents(new StringSelection(cbMessage.getMsg()), this);
                            break;
                        case KEY_STROKE:
                            KeyStrokeMessage keyStrokeMessage = (KeyStrokeMessage) message;
                            System.out.println("Received keystroke " + keyStrokeMessage.getSequence().name());
                            keyStrokeMessage.getSequence().perform();
                            break;
                        case HEARTBEAT:
                            lastHeartbeat = System.currentTimeMillis();
                            break;
                        case URL_CONTENT:
                            StringMessage urlMessage = (StringMessage) message;
                            Runtime.getRuntime().exec("xdg-open " + urlMessage.getMsg());
                        case SOCKET_EXIT:
                            System.out.println("Received disconnect message");
                            stopNow();
                            break loop;
                        default:
                            System.err.println("Invalid message " + message.getCommand());
                            break;
                    }
                } catch (InterruptedException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Socket disconnected");
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
            System.out.println("Local ClipBoard Changed");
            if (Arrays.asList(clipboard.getAvailableDataFlavors()).contains(DataFlavor.stringFlavor)) {
                try {
                    out.put(new StringMessage(Command.CB_STRING_CONTENT, (String) clipboard.getData(DataFlavor.stringFlavor)));
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("ClipBoard content not supported, not sending");
            }
            gainOwnership();
        }
    }

    private void gainOwnership() {
        clipboard.setContents(clipboard.getContents(null), this);
    }
}

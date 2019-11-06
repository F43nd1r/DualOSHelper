package com.faendir.clipboardshare.net;

import com.faendir.clipboardshare.threading.TaskManager;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author lukas
 * @since 10.05.18
 */
public abstract class SocketLifeCycleManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TaskManager taskManager;
    private volatile Connector connector = null;
    private volatile Action action = Action.CONTINUE;

    public SocketLifeCycleManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void start() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            SystemTray systemTray = SystemTray.get();
            Image logo = ImageIO.read(getClass().getResource("mylogo.png"));
            systemTray.setImage(logo);
            setStatus(systemTray, "Waiting for connection");
            systemTray.getMenu().add(new MenuItem("Restart", e -> {
                synchronized (this) {
                    action = Action.RESTART;
                    notifyAll();
                }
            }));
            systemTray.getMenu().add(new MenuItem("Stop", e -> {
                synchronized (this) {
                    action = Action.STOP;
                    notifyAll();
                }
            }));
            try {
                prepare();
                do {
                    action = Action.CONTINUE;
                    Future<?> future = taskManager.startTask(() -> {
                        while (!Thread.interrupted() && action == Action.CONTINUE) {
                            try {
                                logger.debug("Acquiring socket...");
                                Socket socket = acquireSocket();
                                logger.debug("Acquired socket");
                                setStatus(systemTray, "Connected");
                                Connector connector = new Connector(socket, clipboard, taskManager);
                                this.connector = connector;
                                logger.debug("Running connector...");
                                connector.run();
                                logger.debug("Connector returned");
                            } catch (InterruptedException e) {
                                break;
                            } finally {
                                setStatus(systemTray, "Disconnected");
                                if (connector != null) {
                                    connector.stop();
                                    connector = null;
                                }
                            }
                        }
                    });
                    while (action == Action.CONTINUE) {
                        synchronized (this) {
                            wait();
                        }
                    }
                    future.cancel(true);
                    if (!future.isDone()) {
                        try {
                            future.get();
                        } catch (InterruptedException | ExecutionException | CancellationException ignored) {
                        }
                    }
                } while (action == Action.RESTART);
            } finally {
                systemTray.shutdown();
                release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void prepare();

    protected abstract void release();

    protected abstract Socket acquireSocket();

    protected Optional<Connector> getConnector() {
        return Optional.ofNullable(connector);
    }

    private void setStatus(SystemTray systemTray, String status) {
        systemTray.setStatus(status);
        systemTray.setTooltip(status);
    }

    private enum Action {
        CONTINUE,
        STOP,
        RESTART
    }
}

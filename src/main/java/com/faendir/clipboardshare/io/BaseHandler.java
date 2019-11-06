package com.faendir.clipboardshare.io;

import com.faendir.clipboardshare.message.Message;
import com.faendir.clipboardshare.threading.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author lukas
 * @since 27.04.18
 */
public abstract class BaseHandler<T extends Closeable> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final BlockingDeque<Message> queue;
    private Future<?> future;
    private final Socket socket;
    private final TaskManager taskManager;
    private volatile boolean stop = false;

    public BaseHandler(Socket socket, TaskManager taskManager) {
        this.socket = socket;
        this.taskManager = taskManager;
        queue = new LinkedBlockingDeque<>();
    }

    protected abstract T openResource(Socket socket) throws IOException;

    protected abstract boolean isResourceOpen(Socket socket);

    protected abstract void closeResource(Socket socket) throws IOException;

    public void start() {
        future = taskManager.startTask(() -> {
            try (T resource = openResource(socket)) {
                logger.debug("Aquired resource of type " + resource.getClass().getSimpleName());
                while (!stop && !socket.isClosed() && isResourceOpen(socket)) {
                    try {
                        run(resource);
                    } catch (EOFException | SocketException e) {
                        break;
                    } catch (InterruptedException | SocketTimeoutException ignored) {
                    } catch (IOException | IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
                logger.debug("Dropping resource of type " + resource.getClass().getSimpleName());
                try {
                    stop();
                } catch (IOException ignored) {
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void stop() throws IOException {
        if(!stop) {
            stop = true;
            if(!socket.isClosed()) {
                logger.debug("Closing resource");
                closeResource(socket);
            }
            if(future != null && !future.isDone()) {
                future.cancel(true);
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException| CancellationException ignored) {
                }
            }
        }
    }

    protected abstract void run(T resource) throws IOException, InterruptedException;

    public boolean isStopped() {
        return stop;
    }

    protected BlockingDeque<Message> getQueue() {
        return queue;
    }
}

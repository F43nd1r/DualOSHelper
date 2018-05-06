package com.faendir.clipboardshare.io;

import com.faendir.clipboardshare.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author lukas
 * @since 27.04.18
 */
public abstract class BaseHandler<T extends Closeable> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final BlockingDeque<Message> queue;
    private final Thread thread;
    private final Socket socket;
    private volatile boolean stop = false;

    public BaseHandler(Socket socket) {
        this.socket = socket;
        queue = new LinkedBlockingDeque<>();
        thread = new Thread(() -> {
            try (T resource = openResource(socket)) {
                logger.debug("Aquired resource of type " + resource.getClass().getSimpleName());
                while (!stop && !socket.isClosed() && isResourceOpen(socket)) {
                    try {
                        run(resource);
                    } catch (EOFException | SocketException e) {
                        try {
                            stop();
                        } catch (InterruptedException|IOException ignored) {
                        }
                    } catch (InterruptedException | SocketTimeoutException ignored) {
                    } catch (IOException | IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
                logger.debug("Dropping resource of type " + resource.getClass().getSimpleName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    protected abstract T openResource(Socket socket) throws IOException;

    protected abstract boolean isResourceOpen(Socket socket);

    protected abstract void closeResource(Socket socket) throws IOException;

    public void start() {
        thread.start();
    }

    public void stop() throws InterruptedException, IOException {
        if(!stop) {
            stop = true;
            if(!socket.isClosed()) {
                logger.debug("Closing resource");
                closeResource(socket);
            }
            thread.interrupt();
            thread.join();
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

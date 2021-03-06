package com.faendir.clipboardshare.io;

import com.faendir.clipboardshare.message.Command;
import com.faendir.clipboardshare.message.Message;
import com.faendir.clipboardshare.threading.TaskManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * @author lukas
 * @since 27.04.18
 */
public class InputHandler extends BaseHandler<DataInputStream> {
    public InputHandler(Socket socket, TaskManager taskManager) {
        super(socket, taskManager);
    }

    @Override
    protected void run(DataInputStream in) throws IOException {
        Command command = Command.valueOf(in.readUTF());
        getQueue().offerLast(command.readMessage(in));
        synchronized (getQueue()) {
            getQueue().notifyAll();
        }
    }

    @Override
    protected DataInputStream openResource(Socket socket) throws IOException {
        socket.setSoTimeout(1000);
        return new DataInputStream(socket.getInputStream());
    }

    @Override
    protected boolean isResourceOpen(Socket socket) {
        return !socket.isInputShutdown();
    }

    @Override
    protected void closeResource(Socket socket) throws IOException {
        if(!socket.isClosed()) {
            socket.shutdownInput();
        }
    }

    @Override
    public void stop() throws IOException {
        super.stop();
        synchronized (getQueue()) {
            getQueue().notifyAll();
        }
    }

    private Message poll() {
        while (true) {
            try {
                return getQueue().pollFirst(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public Message take() throws InterruptedException {
        Message message;
        while ((message = poll()) == null) {
            if (isStopped()) {
                throw new InterruptedException();
            }
            synchronized (getQueue()) {
                getQueue().wait();
            }
        }
        return message;
    }
}

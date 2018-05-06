package com.faendir.clipboardshare.io;

import com.faendir.clipboardshare.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author lukas
 * @since 27.04.18
 */
public class OutputHandler extends BaseHandler<DataOutputStream> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public OutputHandler(Socket socket) {
        super(socket);
    }

    @Override
    protected void run(DataOutputStream out) throws IOException, InterruptedException {
        Message message = getQueue().takeFirst();
        message.writeTo(out);
    }

    @Override
    protected DataOutputStream openResource(Socket socket) throws IOException {
        return new DataOutputStream(socket.getOutputStream());
    }

    @Override
    protected boolean isResourceOpen(Socket socket) {
        return !socket.isOutputShutdown();
    }

    @Override
    protected void closeResource(Socket socket) throws IOException {
        socket.shutdownOutput();
    }

    public void put(Message message) {
        logger.debug("Adding msg " + message.getCommand().name());
        getQueue().addLast(message);
    }
}

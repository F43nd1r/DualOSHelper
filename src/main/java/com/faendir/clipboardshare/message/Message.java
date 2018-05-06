package com.faendir.clipboardshare.message;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author lukas
 * @since 27.04.18
 */
public class Message {
    private final Command command;

    public Message(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(command.name());
    }
}

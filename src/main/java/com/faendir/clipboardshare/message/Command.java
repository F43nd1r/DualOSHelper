package com.faendir.clipboardshare.message;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author lukas
 * @since 27.04.18
 */
public enum Command {
    SOCKET_READY,
    HEARTBEAT,
    SOCKET_EXIT,
    CB_STRING_CONTENT {
        @Override
        public Message readMessage(DataInputStream in) throws IOException {
            return StringMessage.readFrom(this, in);
        }
    },
    URL_CONTENT {
        @Override
        public Message readMessage(DataInputStream in) throws IOException {
            return StringMessage.readFrom(this, in);
        }
    },
    KEY_STROKE {
        @Override
        public Message readMessage(DataInputStream in) throws IOException {
            return StringMessage.readFrom(this, in);
        }
    };

    public Message readMessage(DataInputStream in) throws IOException {
        return new Message(this);
    }
}

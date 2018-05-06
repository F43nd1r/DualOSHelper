package com.faendir.clipboardshare.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author lukas
 * @since 03.05.18
 */
public class StringMessage extends Message{
    private final String msg;

    public StringMessage(Command command, String msg) {
        super(command);
        this.msg = msg;
    }

    public static StringMessage readFrom(Command command, DataInputStream in) throws IOException {
        return new StringMessage(command, in.readUTF());
    }

    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        super.writeTo(out);
        out.writeUTF(msg);
    }

    public String getMsg() {
        return msg;
    }
}

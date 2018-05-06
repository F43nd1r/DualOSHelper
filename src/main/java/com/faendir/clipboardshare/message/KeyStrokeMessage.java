package com.faendir.clipboardshare.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author lukas
 * @since 04.05.18
 */
public class KeyStrokeMessage extends Message {
    private final Sequence sequence;

    public KeyStrokeMessage(Sequence sequence) {
        super(Command.KEY_STROKE);
        this.sequence = sequence;
    }

    public static KeyStrokeMessage readFrom(DataInputStream in) throws IOException {
        return new KeyStrokeMessage(Sequence.valueOf(in.readUTF()));
    }

    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        super.writeTo(out);
        out.writeUTF(sequence.name());
    }

    public enum Sequence {
        WIN_DEL{
            @Override
            public void perform() throws IOException {
                Runtime.getRuntime().exec("irsend send_once teufel power");
            }
        },
        WIN_END{
            @Override
            public void perform() throws IOException {
                Runtime.getRuntime().exec("sudo switch_monitor_source");
            }
        };

        public abstract void perform() throws IOException;
    }
}

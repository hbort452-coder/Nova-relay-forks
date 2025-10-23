package org.cloudburstmc.protocol.common.util.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.cloudburstmc.protocol.common.util.Preconditions.checkArgument;

public class LittleEndianByteBufInputStream extends ByteBufInputStream {
    private final ByteBuf buffer;

    public LittleEndianByteBufInputStream(ByteBuf buffer) {
        super(buffer);
        this.buffer = buffer;
    }

    @Override
    public char readChar() throws IOException {
        return Character.reverseBytes(buffer.readChar());
    }

    @Override
    public double readDouble() throws IOException {
        return buffer.readDoubleLE();
    }

    @Override
    public float readFloat() throws IOException {
        return buffer.readFloatLE();
    }

    @Override
    public short readShort() throws IOException {
        return buffer.readShortLE();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return buffer.readUnsignedShortLE();
    }

    @Override
    public long readLong() throws IOException {
        return buffer.readLongLE();
    }

    @Override
    public int readInt() throws IOException {
        return buffer.readIntLE();
    }

    @Override
    public String readUTF() throws IOException {
        int length = this.readUnsignedShort();
        return (String) buffer.readCharSequence(length, StandardCharsets.UTF_8);
    }

    public String readUTFMaxLen(int maxLength) throws IOException {
        int length = this.readUnsignedShort();
        checkArgument(maxLength <= 0 || length <= maxLength,
                "Tried to read %s bytes but maximum is %s", length, maxLength);
        return (String) buffer.readCharSequence(length, StandardCharsets.UTF_8);
    }
}

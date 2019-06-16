package es.gencom.integration.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class BitOutputArray implements BitOutputStream, Closeable, AutoCloseable {

    private long value;
    private byte bits_pushed;

    private int size;
    private byte[] array;

    public BitOutputArray() {
        size = 0;
        array = new byte[1024];
    }

    public final byte[] getArray() throws IOException {
        flush();
        return Arrays.copyOf(array, size);
    }

    @Override
    public void writeBits(long bits, int nbits) throws IOException {
        value |= bits << bits_pushed;
        bits_pushed += nbits;
        if (bits_pushed >= 64) {
            put(value);
            bits_pushed -= 64;
            value = bits >> (nbits - bits_pushed);
        }
    }

    @Override
    public void align() throws IOException {
        if (bits_pushed > 0) {
            for (int i = 0; i < bits_pushed; i += 8) {
                write(value >> i);
            }
            bits_pushed = 0;
        }
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(array, 0, size);
    }

    protected void put(long value) throws IOException {
        for (int i = 0; i < 64; i += 8) {
            write(value >> i);
        }
    }

    private void write(final long value) {
        if (array.length == size) {
            array = Arrays.copyOf(array, size << 1);
        }
        array[size++] = (byte)(value & 0xFF);
    }

    public void flush() throws IOException {
        align();
    }
    
    @Override
    public void close() throws IOException {
        flush();
    }
}

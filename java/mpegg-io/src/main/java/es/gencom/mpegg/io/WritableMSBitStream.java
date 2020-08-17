package es.gencom.mpegg.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.WritableByteChannel;

public class WritableMSBitStream implements MPEGWriter {
    private final WritableMSBitChannel channel;
    private boolean open = true;

    public WritableMSBitStream(OutputStream outputStream) {
        this.channel = new WritableMSBitChannel(
                new WritableByteChannel() {
                    @Override
                    public int write(ByteBuffer byteBuffer) throws IOException {
                        byte[] bufferToWrite = new byte[1024];
                        int totalSizeToWrite = byteBuffer.remaining();
                        while(byteBuffer.hasRemaining()){
                            int toWrite = Integer.min(byteBuffer.remaining(), 1024);
                            byteBuffer.get(bufferToWrite, 0, toWrite);
                            outputStream.write(bufferToWrite);
                        }
                        return totalSizeToWrite;
                    }

                    @Override
                    public boolean isOpen() {
                        return open;
                    }

                    @Override
                    public void close() throws IOException {
                        outputStream.close();
                    }
                }
        );
    }

    @Override
    public void writeByteBuffer(ByteBuffer buf) throws IOException {
        channel.writeByteBuffer(buf);
    }

    @Override
    public void align() throws IOException {
        channel.align();
    }

    @Override
    public void writeBits(long bits, int nbits) throws IOException {
        channel.writeBits(bits, nbits);
    }

    @Override
    public void flush() throws IOException {
        channel.flush();
    }
}

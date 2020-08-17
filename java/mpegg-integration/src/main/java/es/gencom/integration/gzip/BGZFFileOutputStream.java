package es.gencom.integration.gzip;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * <p>
 * File based GZip OutputStream with a BGZF support.
 * </p>
 * 
 * @author Dmitry Repchevsky
 */

public class BGZFFileOutputStream extends OutputStream 
        implements AutoCloseable {
    
    private final static byte[] EOF = 
        {31, -117, 8, 4, 0, 0, 0, 0, 0, -1, 6, 0, 66, 67,
         2, 0, 27, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        
    private int size;
    private long pos;
    private final FileChannel channel;
    private final OutputStream out;
    private final BGZFOutputStream gzip;
    
    private final GZipHeader header;

    public BGZFFileOutputStream(final Path file) throws IOException {
        
        header = new GZipHeader(0, null, null, 0);
        
        channel = FileChannel.open(file,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        out = new BufferedOutputStream(Channels.newOutputStream(channel));
        gzip = new BGZFOutputStream(header, out);
    }

    @Override
    public void write(final int b) throws IOException {
        if (size == 65536) {
            gzip.close();
            
            fix_bsize();
            
            pos = channel.position();

            // write next header;
            header.write(out);
            
            size = 0;
        }
        gzip.write(b);
        size++;
    }

    @Override
    public void write(final byte b[], int off, int len) throws IOException {
        if (len > 0) {
            if (size == 65536) {
                gzip.close();
                fix_bsize();
                pos = channel.position();
                header.write(out);
                size = 0;
            }
            while(true) {
                if (size + len <= 65536) {
                    gzip.write(b, off, len);
                    size += len;
                    break;
                }
                gzip.write(b, off, 65536 - size);
                gzip.close();
                fix_bsize();
                pos = channel.position();
                header.write(out);
                len = len + size - 65536;
                off = off - size + 65536;
                size = 0;
            }                
            
        }
    }

    /**
     * <p>
     * Flushes deflate data and ends the BGZF chunk.
     * </p>
     * 
     * @throws IOException 
     */
    @Override
    public void flush() throws IOException {
        if (size > 0) {
            gzip.close();
            fix_bsize();
            pos = channel.position();
            header.write(out);
            size = 0;
        }
    }

    /**
     * <p>
     * Closes the stream and the underlying file.
     * </p>
     * 
     * @throws IOException 
     */
    @Override
    public void close() throws IOException {
        if (size == 0) {
            channel.write(ByteBuffer.wrap(EOF), pos);
        } else {
            gzip.close();
            fix_bsize();
            out.write(EOF);
            out.flush();
        }
        channel.close();
    }
    
    private void fix_bsize() throws IOException {
        final short bsize = (short)(channel.position() - pos - 1);
        final ByteBuffer buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort(0, bsize);
        while (buf.hasRemaining()) {
            channel.write(buf, buf.position() + pos + 16);
        }
    }
}

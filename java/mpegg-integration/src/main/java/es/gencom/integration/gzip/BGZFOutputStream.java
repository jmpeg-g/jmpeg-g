package es.gencom.integration.gzip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * GZip OutputStream which supports improved GZip Header.
 * 
 * @author Dmitry Repchevsky
 */

public class BGZFOutputStream extends DeflaterOutputStream {

    private long size;
    private final CRC32 crc;

    public BGZFOutputStream(final OutputStream out) throws IOException {
        this(new GZipHeader(), out);
    }
    
    public BGZFOutputStream(final GZipHeader header, 
                            final OutputStream out) throws IOException {
        super(out, new Deflater(Deflater.BEST_COMPRESSION, true), true);
        
        crc = new java.util.zip.CRC32();
        header.write(out);
    }

    @Override
    public void write(final int b) throws IOException {
        super.write(b);
    }

    @Override
    public void write(final byte b[]) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(final byte b[], int off, int len) throws IOException {
        super.write(b, off, len);
        crc.update(b, off, len);
        size += len;
    }
    
    /**
     * Closes gzip stream writing the last deflate block and 
     * gzip footer (CRC32 + ISIZE) without closing underlying output stream.
     * 
     * @throws IOException 
     */
    @Override
    public void close() throws IOException {
        super.finish();

        // write crc
        final long crc32 = crc.getValue();
        out.write((int)(crc32 & 0xFF));
        out.write((int)((crc32 >>> 8) & 0xFF));
        out.write((int)((crc32 >>> 16) & 0xFF));
        out.write((int)((crc32 >>> 24) & 0xFF));

        out.write((int)(size & 0xFF));
        out.write((int)((size >>> 8) & 0xFF));
        out.write((int)((size >>> 16) & 0xFF));
        out.write((int)((size >>> 24) & 0xFF));

        out.flush();
        
        size = 0;
        crc.reset();
        def.reset();
    }
}

/**
 * *****************************************************************************
 * Copyright (C) 2019 Spanish National Bioinformatics Institute (INB) and
 * Barcelona Supercomputing Center
 *
 * Modifications to the initial code base are copyright of their respective
 * authors, or their employers as appropriate.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *****************************************************************************
 */

package es.gencom.mpegg.format;

import es.gencom.mpegg.io.MSBitOutputArray;
import es.gencom.mpegg.io.MSBitBuffer;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.WritableMSBitChannel;
import es.gencom.mpegg.io.MPEGReader;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * <p>
 * An abstract class that holds MPEG-G container KEY (see 6.3) and 
 * which all MPEG-G containers must extend.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 * 
 * @param <T>
 */

public abstract class GenInfo<T> {
    
    public static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    
    public final String key;
    
    public GenInfo(final String key) {
        this.key = key;
    }

    /**
     * The method to write the structure content into the bitstream.
     * 
     * @param writer bitstream writer to write the structure.
     * 
     * @throws IOException 
     */
    public abstract void write(MPEGWriter writer) throws IOException, InvalidMPEGStructureException;

    /**
     * Generic method to write structure with its header into the bitstream.
     * If size() method is overwritten it writes directly into the bitstream.
     * Otherwise, the structure is written to a buffer first (to calculate the size).
     *
     * @param writer
     * @throws IOException
     */
    public final void writeWithHeader(final MPEGWriter writer) throws IOException, InvalidMPEGStructureException {

        final long size = size();

        if (size > 0 /*&& size <= Integer.MAX_VALUE*/) {
            new Header(key, Header.SIZE + size).write(writer);
            write(writer);
            writer.flush();
        } else {
            final MSBitOutputArray tmpWriter = new MSBitOutputArray();

            write(tmpWriter);
            tmpWriter.flush();

            ByteBuffer buf = tmpWriter.toByteBuffer();

            new Header(key, Header.SIZE + buf.remaining()).write(writer);

            while (buf.hasRemaining()) {
                writer.writeByteBuffer(buf);
            }
        }
    }

    /**
     * The method to read the structure content from the bitstream.
     * 
     * @param reader bitstream reader to read the structure from.
     * @param size the size of the read structure.
     * 
     * @return the read structure
     * 
     * @throws IOException 
     * @throws InvalidMPEGStructureException 
     * @throws ParsedSizeMismatchException 
     * @throws InvalidMPEGGFileException
     */
    public abstract T read(MPEGReader reader, long size) throws IOException, InvalidMPEGStructureException, ParsedSizeMismatchException, InvalidMPEGGFileException;

    /**
     * The method to read the structure content from the readable byte channel.
     *
     * @param reader ReadableByteChannel to read the structure from
     *
     * @return the read structure
     * @throws java.io.IOException
     * @throws InvalidMPEGGFileException
     *
     * @throws IOException, IllegalArgumentException
     * @throws InvalidMPEGStructureException
     * @throws ParsedSizeMismatchException
     */
    public T read(ReadableByteChannel reader) 
            throws IOException, IllegalArgumentException, InvalidMPEGStructureException, 
                   ParsedSizeMismatchException, InvalidMPEGGFileException {

        Header header = Header.read(reader);
        if(header == null){
            throw new EOFException();
        }

        if(!header.key.equals(key)) {
            throw new IllegalArgumentException(
                    String.format("Invalid Header KEY: %s (must be %s)", header.key, key));

        }

        final ByteBuffer buf = ByteBuffer.allocate((int)header.length).order(BYTE_ORDER);
        while (buf.hasRemaining() && reader.read(buf) >= 0){}

        buf.rewind();

        return this.read(new MSBitBuffer(buf), header.getContentSize()); // read(dataset_header, new MSBitBuffer(buf));
    }

    /**
     * The method to read the structure content from the readable byte channel.
     *
     * @param reader ReadableByteChannel to read the structure from
     *
     * @return the read structure
     *
     */
    public T read(final FileChannel reader) 
            throws IOException, IllegalArgumentException, InvalidMPEGStructureException, 
                   ParsedSizeMismatchException, InvalidMPEGGFileException {

        Header header = Header.read(reader);
        if(header == null){
            throw new EOFException();
        }
        
        if(!header.key.equals(key)){
            throw new IllegalArgumentException();
        }

        final long content_size = header.length - Header.SIZE;
        final ByteBuffer buf = reader.map(FileChannel.MapMode.READ_ONLY, reader.position(), content_size);
        
        reader.position(reader.position() + content_size);
        
        return this.read(new MSBitBuffer(buf), content_size);
    }

    public long sizeWithHeader() {
        return size() + Header.SIZE;
    }

    /**
     * The data content size. If size is unknown (zero), write() method writes 
     * the content to a buffer first.
     * 
     * @return the size of the data content.
     */
    protected long size() {
        return 0;
    }
    
    /**
     * Generic method to write structure into the channel.
     * If size() method is overwritten it writes directly into the channel.
     * Otherwise, the structure is written to the buffer first (to calculate the size).
     * 
     * @param channel
     * @throws IOException 
     */
    public final void write(final WritableByteChannel channel) throws IOException, InvalidMPEGStructureException {
        
        final long size = size();

        if (size > 0 && size <= Integer.MAX_VALUE) {
            new Header(key, Header.SIZE + size).write(channel);
            
            WritableMSBitChannel writer = new WritableMSBitChannel(channel);
            write(writer);
            writer.flush();
        } else {
            final MSBitOutputArray writer = new MSBitOutputArray();
            
            write(writer);
            writer.flush();
            
            ByteBuffer buf = writer.toByteBuffer();
            
            new Header(key, Header.SIZE + buf.remaining()).write(channel);
        
            while (buf.hasRemaining() && channel.write(buf) >= 0) {}
        }
    }
    
    /**
     * Generic method to write structure into the seekable channel.
     * There is no need to know the size as the header is written after the content.
     * 
     * @param channel
     * @throws IOException 
     */
    public final void write(final SeekableByteChannel channel) throws IOException, InvalidMPEGStructureException {
        final long start_position = channel.position();
        channel.position(start_position + Header.SIZE);
        WritableMSBitChannel writer = new WritableMSBitChannel(channel);
        write(writer);
        writer.flush();
        final long end_position = channel.position();
        channel.position(start_position);
        new Header(key, end_position - start_position).write(channel);
        channel.position(end_position);
    }


    public final static class Header {

        public static final int SIZE = 12;
        
        public final String key;
        public final long length;
        
        public Header(final String key, final long length) {
            this.key = key;
            this.length = length;
        }

        public void write(final MPEGWriter writer) throws IOException {
            writer.writeString(key);
            writer.writeLong(length);
        }

        public void write(final WritableByteChannel channel) throws UnsupportedEncodingException, IOException {
            final ByteBuffer buf = ByteBuffer.allocate(SIZE);
            write(buf);
            buf.rewind();
            while (buf.hasRemaining() && channel.write(buf) >= 0) {}
        }

        public void write(final ByteBuffer buf) throws UnsupportedEncodingException, IOException {
            buf.put(key.getBytes("UTF-8"));
            buf.putLong(length);
        }

        public long getContentSize() {
            return length - Header.SIZE;
        }

        /**
         * Reads KEY / LENGTH (ISO/IEC 23092-1 6.3 Syntax for representation) info from a 
         * channel.
         * 
         * @param channel the channel to read GenInfo from.
         * @return either GenInfo or null if no data left in the channel.
         * 
         * @throws IOException 
         */
        public static Header read(final ReadableByteChannel channel) throws IOException {
            final byte[] arr = new byte[12];
            final ByteBuffer buf = ByteBuffer.wrap(arr).order(BYTE_ORDER);
            while (buf.hasRemaining() && channel.read(buf) >= 0) {}
            if (buf.position() == 0) {
                return null;
            }
            if (buf.hasRemaining()) {
                throw new EOFException();
            }
            buf.position(4);
            return new Header(new String(arr, 0, 4), buf.getLong());
        }
        
        public static Header read(final MPEGReader reader) throws IOException {
            final String key = String.valueOf(reader.readChars(4));
            final long length = reader.readBits(64);
         
            return new Header(key, length);
        }
    }
}

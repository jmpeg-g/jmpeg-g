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

package es.gencom.mpegg.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>
 * Big Endian MPEG-G BitBuffer based on a ByteBuffer.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MSBitBuffer implements MPEGReader, MPEGWriter {
    
    private long value;
    private byte bits_left;
    private final ByteBuffer buf;

    public MSBitBuffer(final ByteBuffer buf) {
        this.buf = buf.slice().order(ByteOrder.BIG_ENDIAN);
    }
    
    @Override
    public void align() {
        buf.position(buf.position() - (bits_left >>> 3));
        bits_left = 0;
    }

    @Override
    public Payload readPayload(final long size) throws IOException {
        final ByteBuffer bb = readByteBuffer((int)size);
        return new Payload(new ByteBuffer[] {bb});
    }
    
    @Override
    public ByteBuffer readByteBuffer(final int size) throws IOException {
        align();
        
        if (size > buf.remaining()) {
            throw new EOFException();
        }
        final ByteBuffer bb = buf.slice();
        bb.limit(size);
        buf.position(buf.position() + size);
        return bb;
    }

    @Override
    public long readBits(int nbits) throws EOFException {
        final long mask = -1L >>> (64 - nbits);
        
        if (bits_left == 0) {
            value = get();
        } else if (bits_left < nbits) {
            nbits -= bits_left;
            long bits = value << nbits;
            value = get();
            bits_left -= nbits;
            return (value >>> bits_left | bits) & mask;
        }
        bits_left -= nbits;
        return (value >>> bits_left) & mask;
    }

    @Override
    public void writeByteBuffer(final ByteBuffer buf) throws IOException {
        if(bits_left == 0) {
            this.buf.put(buf);
        } else {
            while(buf.remaining() > 0) {
                writeByte(buf.get());
            }
        }
    }
    
    @Override
    public void writeBits(long bits, int nbits) {        

        bits &= -1L >>> (64 - nbits);

        if (bits_left > nbits) {
            bits_left -= nbits;
            value |= bits << bits_left;
        } else if (bits_left == 0) {
            bits_left = (byte)(64 - nbits);
            value = bits << bits_left;
            if(bits_left == 0){
                put(value);
            }
        } else if (bits_left < nbits) {
            nbits -= bits_left;
            put(value | (bits >> nbits));
            bits_left = (byte)(64 - nbits);
            value = bits << bits_left;
        } else {
            put(value | bits);
            bits_left = 0;
        }
    }

    private long get() throws EOFException {
        if (buf.remaining() >= Long.BYTES) {
            bits_left = 64;
            return buf.getLong();
        }
        byte bytesToRead = (byte) buf.remaining();
        if(bytesToRead == 0){
            throw new EOFException();
        }
        bits_left = (byte) (bytesToRead * 8);
        // can't read entire Long, so read byte by byte
        final byte[] tail = new byte[8];
        buf.get(tail, 8-bytesToRead, bytesToRead);
        return ByteBuffer.wrap(tail).order(ByteOrder.BIG_ENDIAN).getLong();
    }
    
    protected void put(final long value) {
        if (buf.remaining() >= Long.BYTES) {
            buf.putLong(value);
        } else {
            final byte[] tail = new byte[8];
            ByteBuffer.wrap(tail).order(ByteOrder.BIG_ENDIAN).putLong(value);
            buf.put(tail, 0, buf.remaining());
        }
    }

    protected void putPartially(long value, int length){
        if(length < 0 || length > 8){
            throw new IllegalArgumentException();
        }
        final byte[] tail = new byte[8];
        ByteBuffer.wrap(tail).order(ByteOrder.BIG_ENDIAN).putLong(value);
        buf.put(tail, 0, length);
    }

    @Override
    public void flush() throws IOException {
        if (bits_left > 0) {
            final int pos = buf.position();
            final int len = (71 - bits_left) >>> 3;
            putPartially(value, len);
            buf.position(pos + len);
            bits_left = 0;
        }
    }

    @Override
    public long getPosition(){
        int currentPosition = buf.position();
        buf.rewind();
        int startPosition = buf.position();
        buf.position(currentPosition);
        return currentPosition - startPosition;
    }
}

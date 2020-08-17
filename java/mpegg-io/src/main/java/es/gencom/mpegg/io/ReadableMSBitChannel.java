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
import java.nio.channels.ReadableByteChannel;

/**
 * <p>
 * MPEGReader implementation based on the ReadableByteChannel
 * </p>
 * 
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class ReadableMSBitChannel<T extends ReadableByteChannel> implements MPEGReader {
    
    private byte bits_left;
    
    private final byte[] arr;
    private final ByteBuffer buf;
    final T channel;
    private long position = 0;
    
    public ReadableMSBitChannel(final T channel) {
        this.channel = channel;
        
        arr = new byte[1];
        buf = ByteBuffer.wrap(arr);
    }
    
    @Override
    public void align() {
        bits_left = 0;
    }

    @Override
    public Payload readPayload(long size) throws IOException {
        final int n = (int) (size / Integer.MAX_VALUE) + 1;
        final ByteBuffer[] bb = new ByteBuffer[n];
        for (int i = 0; i < n; i++, size -= Integer.MAX_VALUE) {
            bb[i] = ByteBuffer.allocate(Math.min((int)size, Integer.MAX_VALUE));
            while (bb[i].hasRemaining() && channel.read(bb[i]) >= 0) {}
            bb[i].position(0);
            position += bb[i].capacity();
        }
        return new Payload(bb);
    }
    
    @Override
    public ByteBuffer readByteBuffer(final int size) throws IOException {
        align();
        
        final ByteBuffer bb = ByteBuffer.allocate(size);
        while (bb.hasRemaining() && channel.read(bb) >= 0) {}
        position += size;
        return bb;
    }

    @Override
    public long getPosition() throws IOException {
        return position;
    }

    @Override
    public long readBits(int nbits) throws IOException {
        long bits = 0;
        do {
            final int read = Math.min(nbits, 8);
            bits = (bits << read) | read8Bits(read);
        } while ((nbits -= 8) > 0);
        return bits;
    }

    private long read8Bits(int nbits) throws IOException {
        if (bits_left == 0) {
            read8Bits();
            bits_left = (byte)(8 - nbits);
        } else if (bits_left < nbits) {
            nbits -= bits_left;
            byte mask = (byte)((1 << bits_left)-1);
            bits_left = (byte)(8 - nbits);
            long bits = (arr[0] & mask) << nbits;
            read8Bits();
            mask = (byte) ((1 << nbits)-1);
            return (arr[0] >>> bits_left) & mask | bits;
        } else {
            bits_left -= nbits;
        }
        return (arr[0] >>> bits_left) & ((1  << nbits)-1);
    }

    private void read8Bits() throws IOException {
        buf.position(0);
        if (!buf.hasRemaining()) {
            throw new EOFException();
        }
        int result;
        while ((result = channel.read(buf)) == 0) {}
        position++;
        if(result == -1){
            throw new EOFException();
        }
    }
}

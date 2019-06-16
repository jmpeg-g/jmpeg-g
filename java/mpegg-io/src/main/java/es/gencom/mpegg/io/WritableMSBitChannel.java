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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * <p>
 * Channel based MPEGWriter implementation.
 * </p>
 * 
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class WritableMSBitChannel implements MPEGWriter {

    private long value;
    private byte bits_left;

    private final byte[] arr;
    private final ByteBuffer buf;
    private final WritableByteChannel channel;

    public WritableMSBitChannel(final WritableByteChannel channel) {
        this.channel = channel;

        arr = new byte[8];
        buf = ByteBuffer.wrap(arr);
    }

    @Override
    public void writeByteBuffer(final ByteBuffer buf) throws IOException {
        
        if(bits_left == 0) {
            while (buf.hasRemaining() && channel.write(buf) >= 0) { }
        } else {
            while(buf.remaining() > 0) {
                writeByte(buf.get());
            }
        }
    }

    @Override
    public void writeBits(long bits, int nbits) throws IOException {
        long upper_mask;
        if(nbits == 64){
            upper_mask = -1L;
        }else {
            upper_mask = (1L << nbits) - 1L;
        }
        bits &= upper_mask;
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
            put(value | (bits >>> nbits));
            bits_left = (byte)(64 - nbits);
            value = bits << bits_left;
        } else {
            put(value | bits);
            bits_left = 0;
        }
    }

    @Override
    public void align() throws IOException {
        flush();
    }

    @Override
    public void flush() throws IOException {
        if (bits_left > 0) {
            buf.putLong(0, value);
            buf.limit((71 - bits_left) >>> 3);
            channel.write(buf);
            buf.rewind();
            buf.limit(8);
            bits_left = 0;
        }
    }

    private void put(final long value) throws IOException {
        buf.putLong(0, value);
        channel.write(buf);
        buf.rewind();
    }
}

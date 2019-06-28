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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * <p>
 * Array based {@link MPEGWriter} implementation.
 * </p>
 * 
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MSBitOutputArray implements MPEGWriter, Closeable, AutoCloseable {

    private byte bits_left;

    private int size;
    private byte[] array;

    public MSBitOutputArray() {
        size = 0;
        array = new byte[4096];
    }

    /**
     * The number of bits written so far.
     * 
     * @return number of stored bits.
     */
    public final long getLength() {
        return bits_left == 0 ? (size << 3) : ((size + 1) << 3) - bits_left;
    }
    
    /**
     * The number of written bytes.
     * 
     * @return stored data size in bytes.
     */
    public final int size() {
        return bits_left == 0 ? size : size + 1;
    }

    /**
     * @return a copy of internal storage.
     */
    public final byte[] getArray() {
        return Arrays.copyOf(array, size());
    }

    /**
     * @return {@link ByteBuffer} wrapping the internal storage.
     */
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(array, 0, size());
    }

    @Override
    public void writeByteBuffer(ByteBuffer buf) throws IOException {
        final int len = buf.remaining();
        if (array.length - size < len) {
            array = Arrays.copyOf(array, size << 1);
        }

        if (bits_left % 8 == 0){
            buf.get(array, size, len);
            size += len;
        } else {
            byte tempArray[] = new byte[len];
            buf.get(tempArray, 0, len);
            for (byte byteValue : tempArray) {
                writeByte(byteValue);
            }
        }

    }

    @Override
    public void writeBits(long bits, int nbits) throws IOException {

        bits &= -1L >>> (64 - nbits);
        
        if (nbits < bits_left) {
            array[size] |= (bits << (bits_left - nbits)) & 0xFF;
            bits_left -= nbits;
        } else if (nbits > bits_left) {
            if (bits_left > 0) {
                nbits -= bits_left;
                array[size] |= bits >>> nbits;
                size++;
            }
            while (nbits >= 8) {
                nbits -= 8;
                write((int)(bits >>> nbits));
                size++;
            }
            if (nbits == 0) {
                bits_left = 0;
            } else {
                bits_left = (byte)(8 - nbits);
                write((int)(bits << bits_left));
            }
        } else { // nbits == bits_left
            array[size] |= bits & 0xFF; //ok
            bits_left = 0;
            size++;
        }
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
    
    private void write(final int value) {
        if (array.length == size) {
            array = Arrays.copyOf(array, size << 1);
        }
        array[size] = (byte)(value & 0xFF);
    }

    @Override
    public void align() throws IOException {
        writeBits(0, bits_left);
    }
}

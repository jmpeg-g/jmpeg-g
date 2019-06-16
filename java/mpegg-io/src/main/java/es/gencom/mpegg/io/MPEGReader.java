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
import java.util.Arrays;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public interface MPEGReader extends BitReader {

    void align();

    /**
     * Reads MPEG-G u7(v) variable length unsigned integer from the stream.
     * 
     * @return the unsigned integer
     * 
     * @throws IOException 
     */
    default long readVarSizedUnsignedInt() throws IOException {
        long v = 0;
        long c;
        do {
            c = readBits(8);
            v = (v << 7) | (c & 0x7f);
        } while ((c & 0x80) != 0);
        return v;
    }
    
    /**
     * Reads null (0) terminated String from the bitstream.
     * 
     * @return the string
     * 
     * @throws IOException 
     */
    default String readNTString() throws IOException {
        int size = 0;
        byte[] array = new byte[2];

        byte ch;
        while((ch = (byte)readBits(8)) != 0) {
            if (array.length == size) {
                array = Arrays.copyOf(array, size << 1);
            }
            array[size++] = ch;
        }
        return new String(array, 0, size, "UTF-8");
    }
    
    /**
     * Reads characters from the bitstream.
     * 
     * @param nchars number of characters to read,
     * @return read characters.
     * 
     * @throws IOException 
     */
    default char[] readChars(final int nchars) throws IOException {
        final char[] ch = new char[nchars];
        for (int i = 0; i < nchars; i++) {
            ch[i] = (char)readBits(8);
        }
        return ch;
    }

    /**
     * Reads size number of bytes from the bit stream to a new payload.
     * Before reading the stream is aligned to bytes.
     *
     * @param size the number of bytes to be read.
     * @return payload with read data.
     *
     * @throws IOException
     */
    Payload readPayload(long size) throws IOException;

    /**
     * Reads byte buffer from the bit stream. 
     * Before reading the stream is aligned to bytes.
     * 
     * @param size the number of bytes to be read.
     * @return byte buffer with read data.
     * 
     * @throws IOException 
     */
    ByteBuffer readByteBuffer(int size) throws IOException;

    /**
     * Returns position in the MPEGReader.
     * Returns the position in number of bytes since the initial position.
     * If not byte aligned, the position corresponds to the next aligned position.
     * 
     * @return current reader position
     * 
     * @throws java.io.IOException 
     */
    long getPosition() throws IOException;
}

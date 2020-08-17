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

/**
 * <p>
 * MPEG-G bit writer that implements encoding of basic MPEG-G types.
 * </p>
 * 
 * See 6.3 Specification of syntax functions and data types.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public interface MPEGWriter extends BitWriter {

    /**
     * <p>
     * Write a byte buffer into this writer.
     * </p>
     * 
     * @param buf the buffer to be written
     * 
     * @throws IOException 
     */
    void writeByteBuffer(ByteBuffer buf) throws IOException;
    
    /**
     * <p>
     * Align the writer to a byte boundary.
     * </p>
     * 
     * @throws IOException 
     */
    void align() throws IOException;
    
    default void writeBytes(final byte[] bytes) throws IOException {
        for (int i = 0, n = bytes.length; i < n; i++) {
            writeByte(bytes[i]);
        }
    }
    
    default void writeString(final String s) throws IOException {
        writeBytes(s.getBytes("UTF-8"));
    }

    default void writeNTString(final String s) throws IOException {
        writeString(s);
        writeByte((byte) 0);
    }

    default void writePayload(final Payload payload) throws IOException {
        for(ByteBuffer byteBuffer : payload.getByteBuffers()){
            writeByteBuffer(byteBuffer);
        }
    }

    default void writeU7(long value) throws IOException{
        int lengthNumber = 64 - Long.numberOfLeadingZeros(value);
        long mask = 0x7f;
        short[] bytes = new short[10];
        for(int i=0; i<10; i++){
            bytes[10-i-1] = (short)((value & (mask << (i*7))) >> (i*7));
        }
        int bytesToWrite = (int) Math.ceil(lengthNumber/(double)7);
        for(int byte_index = 10-bytesToWrite; byte_index <9; byte_index++){
            writeUnsignedByte((short) (bytes[byte_index] | 0x80));
        }
        writeUnsignedByte(bytes[9]);
    }

    default void writeUnsignedBytes(short[] shorts) throws IOException {
        for(int val_i=0; val_i < shorts.length; val_i++){
            writeUnsignedByte(shorts[val_i]);
        }
    }
}

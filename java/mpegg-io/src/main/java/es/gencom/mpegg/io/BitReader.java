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

/**
 * <p>
 * Bit Reader interface with default implementation for basic Java types.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public interface BitReader {

    /**
     * <p>
     * Read bits from this reader.
     * </p>
     * 
     * @param nbits number of bits to read
     * @return the value with read bits
     * 
     * @throws IOException 
     */
    long readBits(int nbits) throws IOException;

    default byte readByte() throws IOException{
        return (byte)readBits(8);
    }
    
    default short readUnsignedByte() throws IOException{
        return (short) (readByte() & 0xFF);
    }
    
    default short readShort() throws IOException {
        return (short)readBits(16);
    }

    default int readUnsignedShort() throws IOException{
        return readShort() & 0xFFFF;
    }

    default int readInt() throws IOException {
        return (int)readBits(32);
    }

    default long readUnsignedInt() throws IOException{
        return readInt() & 0xFFFFFFFFL;
    }

    default long readLong() throws IOException {
        return readBits(64);
    }

    default double readDouble() throws IOException{
        return (double)readBits(64);
    }

    default boolean readBoolean() throws IOException{
        return readBits(1)!=0x00;
    }
}

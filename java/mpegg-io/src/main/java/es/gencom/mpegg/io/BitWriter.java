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

import java.io.Flushable;
import java.io.IOException;

/**
 * <p>
 * Bit Writer interface with default implementation for basic Java types.
 * </p>
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public interface BitWriter extends Flushable {
    
    /**
     * <p>
     * Write bits into this writer.
     * </p>
     * 
     * @param bits  value to get bist from
     * @param nbits number of bits to write
     * 
     * @throws IOException 
     */
    void writeBits(long bits, int nbits) throws IOException;

    default void writeBits(final byte bits, final int nbits) throws IOException {
        writeBits(bits & 0xFFL, nbits);
    }

    default void writeBits(final short bits, final int nbits) throws IOException {
        writeBits(bits & 0xFFFFL, nbits);
    }
    
    default void writeBits(final int bits, final int nbits) throws IOException {
        writeBits(bits & 0xFFFFFFFFL, nbits);
    }

    default void writeByte(final byte byte_value) throws IOException {
        writeBits(byte_value, 8);
    }

    default void writeUnsignedByte(final short value) throws IOException {
        writeByte((byte)value);
    }

    default void writeShort(final short short_value) throws IOException {
        writeBits(short_value & 0xFFFFL, 16);
    }

    default void writeUnsignedShort(final int value) throws IOException {
        writeShort((short)value);
    }

    default void writeInt(final int int_value) throws IOException {
        writeBits(int_value, 32);
    }

    default void writeUnsignedInt(final long value) throws IOException {
        writeInt((int)value);
    }

    default void writeLong(final long long_value) throws IOException {
        writeBits(long_value, 64);
    }

    default void writeBoolean(final boolean b) throws IOException {
        writeBits(b ? 1 : 0, 1);
    }
}

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

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * Hash functions used to verify the integrity of the reference sequences 
 * (ISO/IEC DIS 23092-1 6.5.1.3.7 Checksum).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum ChecksumAlgorithm {
    MD5((byte)0, 128), 
    SHA256((byte)1, 256);
    
    public final byte value;
    public final int size;
    
    private ChecksumAlgorithm(final byte value, final int size) {
        this.value = value;
        this.size = size;
    }
    
    public static ChecksumAlgorithm getChecksumAlgorithm(final byte value) {
        switch (value) {
            case 0: return MD5;
            case 1: return SHA256;
            default: throw new IllegalArgumentException();
        }
    }
    
    public static ChecksumAlgorithm read(final MPEGReader reader) throws IOException {
        return getChecksumAlgorithm(reader.readByte());
    }

    public void write(final MPEGWriter writer) throws IOException {
        writer.writeByte(value);
    }
}

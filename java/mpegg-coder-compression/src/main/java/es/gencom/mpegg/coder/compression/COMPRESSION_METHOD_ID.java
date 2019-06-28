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

package es.gencom.mpegg.coder.compression;

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * Enumeration of compression methods for the tokentype as defined in Table 78.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum COMPRESSION_METHOD_ID {
    
    COP((byte)0),
    CAT((byte)1),
    RLE((byte)2),
    CABAC_ORDER_0((byte)3),
    CABAC_ORDER_1((byte)4),
    X4((byte)5);
    
    public final byte ID; // u(4)
    
    COMPRESSION_METHOD_ID(final byte id) {
        this.ID = id;
    }
    
    public static COMPRESSION_METHOD_ID getCompressionMethodId(final byte id) {
        switch (id) {
           case 0: return COP;
           case 1: return CAT;
           case 2: return RLE;
           case 3: return CABAC_ORDER_0;
           case 4: return CABAC_ORDER_1;
           case 5: return X4;
           default: throw new IllegalArgumentException();
        }
    }

    public static COMPRESSION_METHOD_ID read(final MPEGReader reader) throws IOException {
        return getCompressionMethodId((byte)reader.readBits(4));
    }
    
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(ID, 4);
    }
}

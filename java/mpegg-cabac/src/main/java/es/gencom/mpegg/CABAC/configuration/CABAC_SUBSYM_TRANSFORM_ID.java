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

package es.gencom.mpegg.CABAC.configuration;

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * MPEG-G IEC 23092-2 Subsymbol Transformations.
 * </p>
 * 
 * 'transform_ID_subsym' subsymbol transformations according to Table 100.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum CABAC_SUBSYM_TRANSFORM_ID {
    NO_TRANSFORM((byte)0),
    LUT_TRANSFORM((byte)1),
    DIFF_CODING((byte)2);

    public static final byte SIZE_IN_BITS = 3;
    public final byte ID;
    
    CABAC_SUBSYM_TRANSFORM_ID(final byte id) {
        this.ID = id;
    }
    
    public static CABAC_SUBSYM_TRANSFORM_ID getSubseqTransformId(final byte id) {
        switch (id) {
           case 0: return NO_TRANSFORM;
           case 1: return LUT_TRANSFORM;
           case 2: return DIFF_CODING;
           default: throw new IllegalArgumentException();
        }
    }
    
    public static CABAC_SUBSYM_TRANSFORM_ID read(final MPEGReader reader) throws IOException {
        return getSubseqTransformId((byte)reader.readBits(SIZE_IN_BITS));
    }
    
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(ID, SIZE_IN_BITS); // u(3)
    }
}

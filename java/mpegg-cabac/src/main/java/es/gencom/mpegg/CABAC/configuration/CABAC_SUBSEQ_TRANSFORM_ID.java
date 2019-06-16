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
 * MPEG-G IEC 23092-2 Subsequence Transformations.
 * </p>
 * 
 * 'transform_ID_subseq' subsequence transformations according to Table 100.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum CABAC_SUBSEQ_TRANSFORM_ID {
    NO_TRANSFORM((short)0),     // no transform
    EQUALITY_CODING((short)1),  // equality transformation
    MATCH_CODING((short)2),     // match transformation
    RLE_CODING((short)3),       // rle transformation
    MERGE_CODING((short)4),     // merge transformation
    RLE_QV_CODING((short)77),
    BWT_TRANSFORM((short)99);
    
    public final short ID;
    
    CABAC_SUBSEQ_TRANSFORM_ID(final short id) {
        this.ID = id;
    }
    
    public static CABAC_SUBSEQ_TRANSFORM_ID getSubseqTransformId(final short id) {
        switch (id) {
           case 0: return NO_TRANSFORM;
           case 1: return EQUALITY_CODING;
           case 2: return MATCH_CODING;
           case 3: return RLE_CODING;
           case 4: return MERGE_CODING;
           case 77: return RLE_QV_CODING;
           case 99: return BWT_TRANSFORM;
           default: throw new IllegalArgumentException();
        }
    }
    
    public static CABAC_SUBSEQ_TRANSFORM_ID read(MPEGReader reader) throws IOException {
        return getSubseqTransformId(reader.readUnsignedByte());
    }
    
    public void write(MPEGWriter writer) throws IOException {
        writer.writeByte((byte)ID);
    }
}

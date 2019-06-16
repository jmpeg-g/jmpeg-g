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
 * Specifies the type of data encoded in the Dataset.
 * </p>
 * 
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum DATASET_TYPE {
    
    NON_ALIGNED_CONTENT((byte)0),
    ALIGNED_CONTENT((byte)1),
    REFERENCE((byte)2);

    public final byte ID; // u(4)
    
    DATASET_TYPE(final byte id) {
        this.ID = id;
    }
    
    public static DATASET_TYPE getDatasetType(final byte id) {
        switch (id) {
            case 0: return NON_ALIGNED_CONTENT;
            case 1: return ALIGNED_CONTENT;
            case 2: return REFERENCE;
            default: throw new IllegalArgumentException();
        }
    }

    public static DATASET_TYPE read(MPEGReader reader) throws IOException {
        return getDatasetType((byte)reader.readBits(4));
    }
    
    public void write(MPEGWriter writer) throws IOException {
        writer.writeBits(ID, 4);
    }
}

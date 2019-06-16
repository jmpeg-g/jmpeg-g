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
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum DatasetType {
    
    NON_ALIGNED((byte)0),
    ALIGNED((byte)1),
    REFERENCE((byte)2);
    
    public final byte ID;
    
    DatasetType(final byte id) {
        this.ID = id;
    }
    
    public static DatasetType getDatasetType(final byte id) {
        switch (id) {
            case 0: return NON_ALIGNED;
            case 1: return ALIGNED;
            case 2: return REFERENCE;
            default: throw new IllegalArgumentException();
        }
    }

    public static DatasetType read(final MPEGReader reader) throws IOException {
        return getDatasetType((byte)reader.readBits(4));
    }

    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(ID, 4);
    }
}

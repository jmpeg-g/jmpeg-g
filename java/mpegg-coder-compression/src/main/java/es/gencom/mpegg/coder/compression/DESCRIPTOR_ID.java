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
 * Enumeration of Genomic Descriptors Identifiers (ISO/IEC 23092-2 Table 24).
 * </p>
 * 
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum DESCRIPTOR_ID {
    
    POS((byte)0),
    RCOMP((byte)1),
    FLAGS((byte)2),
    MMPOS((byte)3),
    MMTYPE((byte)4),
    CLIPS((byte)5),
    UREADS((byte)6),
    RLEN((byte)7),
    PAIR((byte)8),
    MSCORE((byte)9),
    MMAP((byte)10),
    MSAR((byte)11),
    RTYPE((byte)12),
    RGROUP((byte)13),
    QV((byte)14),
    RNAME((byte)15),
    RFTP((byte)16),
    RFTT((byte)17);
    
    public final byte ID; // u(7)
    
    DESCRIPTOR_ID(final byte id) {
        this.ID = id;
    }
    
    public static DESCRIPTOR_ID getDescriptorId(final byte id) {
        switch (id) {
           case 0: return POS;
           case 1: return RCOMP;
           case 2: return FLAGS;
           case 3: return MMPOS;
           case 4: return MMTYPE;
           case 5: return CLIPS;
           case 6: return UREADS;
           case 7: return RLEN;
           case 8: return PAIR;
           case 9: return MSCORE;
           case 10: return MMAP;
           case 11: return MSAR;
           case 12: return RTYPE;
           case 13: return RGROUP;
           case 14: return QV;
           case 15: return RNAME;
           case 16: return RFTP;
           case 17: return RFTT;
           default: throw new IllegalArgumentException();
        }
    }
    
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(ID, 7);
    }

    public static DESCRIPTOR_ID read(final MPEGReader reader) throws IOException {
        return getDescriptorId((byte)reader.readBits(7));
    }
}

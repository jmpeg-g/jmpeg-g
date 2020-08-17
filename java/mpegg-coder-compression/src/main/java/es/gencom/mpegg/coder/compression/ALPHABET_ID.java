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
 * Identifiers of alphabets supported for sequencing reads representation (table 34).
 * </p>
 * 
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum ALPHABET_ID {
    
    DNA((byte)0, (byte)5),
    IUPAC((byte)1, (byte)16);

    public static final byte ALPHABETS[][] = {
        {'A', 'C', 'G', 'T', 'N'},
        {'A', 'C', 'G', 'T', 'R', 'Y', 'S', 'W', 'K', 'M', 'B', 'D', 'H', 'V', 'N', '-'}};

    public static final byte ENCODING_SIZE_IN_BITS = 8;

    public final byte ID; // u(8)
    public final byte SIZE;
    
    ALPHABET_ID(final byte id, final byte size) {
        this.ID = id;
        this.SIZE = size;
    }
    
    public static ALPHABET_ID getAlphabetId(final byte id) {
        switch (id) {
           case 0: return DNA;
           case 1: return IUPAC;
           default: throw new IllegalArgumentException();
        }
    }
    
    public static ALPHABET_ID read(final MPEGReader reader) throws IOException {
        return getAlphabetId((byte)reader.readBits(ENCODING_SIZE_IN_BITS));
    }
    
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(ID, ENCODING_SIZE_IN_BITS);
    }
    
    public static byte charToId(final ALPHABET_ID alphabet_id, final char charToTranslate){
        return charToId(ALPHABETS[alphabet_id.ID], Character.toUpperCase(charToTranslate));
    }

    public static byte charToId(final byte[] alphabet, final char charToTranslate){
        for(int value = 0; value < alphabet.length; value++) {
            if(alphabet[value] == charToTranslate) {
                return (byte)value;
            }
        }
        throw new IllegalArgumentException("char "+charToTranslate+" not present in alphabet");
    }
}

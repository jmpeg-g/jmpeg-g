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

package es.gencom.mpegg.decoder.descriptors;

import es.gencom.mpegg.coder.compression.ALPHABET_ID;

public class S_alphabets {
    public static byte alphabets[][] = {
        {'A', 'C', 'G', 'T', 'N'},
        {'A', 'C', 'G', 'T', 'R', 'Y', 'S', 'W', 'K', 'M', 'B', 'D', 'H', 'V', 'N', '-'},
        {'A', 'C', 'G', 'U', 'N'},
        {'A', 'C', 'G', 'U', 'R', 'Y', 'S', 'W', 'K', 'M', 'B', 'D', 'H', 'V', 'N', '-'}
    };

    public static byte charToId(ALPHABET_ID alphabet_id, char charToTranslate){
        return charToId(alphabets[alphabet_id.ID], Character.toUpperCase(charToTranslate));
    }

    public static byte charToId(byte[] alphabet, char charToTranslate){
        for(byte value = 0; value < alphabet.length; value++){
            if(alphabet[value] == charToTranslate){
                return value;
            }
        }
        throw new IllegalArgumentException("char "+charToTranslate+" not present in alphabet");
    }
}

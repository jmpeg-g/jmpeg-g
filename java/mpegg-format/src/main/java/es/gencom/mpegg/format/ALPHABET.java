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

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum ALPHABET {
    
    DNA_NON_IUPAC((byte)0, (byte) 3),
    DNA_IUPAC((byte)1, (byte) 5);
    
    public final byte id;
    public final byte bits;
    
    ALPHABET(final byte id, final byte bits) {
        this.id = id;
        this.bits = bits;
    }

    public static ALPHABET getAlphabet(final byte id){
        switch (id){
            case 0: return DNA_NON_IUPAC;
            case 1: return DNA_IUPAC;
            default:
                throw new IllegalArgumentException();
        }
    }
    
    public static byte getBaseIdentifier(ALPHABET alphabet, char base){
        switch (alphabet){
            case DNA_NON_IUPAC:
                switch (Character.toUpperCase(base)){
                    case 'A':
                        return 0;
                    case 'C':
                        return 1;
                    case 'G':
                        return 2;
                    case 'T':
                        return 3;
                    case 'N':
                        return 4;
                    default:
                        throw new IllegalArgumentException();
                }
            default:
                throw new UnsupportedOperationException();
        }
    }
}

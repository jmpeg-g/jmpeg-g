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

package es.gencom.mpegg.coder.tokens;

public enum TokenType {
    //**********The unused entries in the enumeration are there to be aligned with the ref software********//
    ErrorToken((byte)-1),
    TypeToken((byte)0),
    AlphaToken((byte)1),
    CharToken((byte)2),
    DZLENToken((byte)3),
    Digits0Token((byte)4),
    DupToken((byte)5),
    DiffToken((byte)6),
    DigitsToken((byte) 7),
    D1Token((byte)8),
    D2Token((byte)9),
    D3Token((byte)10),
    DeltaDigitsToken((byte)11),
    DeltaDigitsPaddedToken((byte) 12),
    MatchToken((byte) 13),
    EndToken((byte)14);

    public final byte id;

    TokenType(final byte id) {
        this.id = id;
    }
}

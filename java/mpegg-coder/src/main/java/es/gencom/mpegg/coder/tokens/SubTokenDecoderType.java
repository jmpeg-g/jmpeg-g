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

public enum SubTokenDecoderType {
    COP((byte)0),
    CAT((byte)1),
    RLE((byte)2),
    CABAC_METHOD_0((byte)3),
    CABAC_METHOD_1((byte)4),
    X4((byte)5);

    public final byte ID;

    SubTokenDecoderType(final byte id) {
        this.ID = id;
    }

    public static SubTokenDecoderType getTokenType(final byte id) {
        switch (id) {
            case 0: return COP;
            case 1: return CAT;
            case 2: return RLE;
            case 3: return CABAC_METHOD_0;
            case 4: return CABAC_METHOD_1;
            case 5: return X4;
            default: throw new IllegalArgumentException();
        }
    }
}

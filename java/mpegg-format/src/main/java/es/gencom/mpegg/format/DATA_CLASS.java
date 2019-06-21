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
 * <p>
 * Classes of the Genomic Records according their mappings 
 * (ISO/IEC DIS 23092-1 5.2 Data Classes).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum DATA_CLASS {
    CLASS_P((byte)1),
    CLASS_N((byte)2),
    CLASS_M((byte)3),
    CLASS_I((byte)4),
    CLASS_HM((byte)5),
    CLASS_U((byte)6);
    
    public final byte ID;
    
    DATA_CLASS(final byte id) {
        this.ID = id;
    }
    
    public static DATA_CLASS getDataClass(final byte id) {
        switch (id){
            case 1: return CLASS_P;
            case 2: return CLASS_N;
            case 3: return CLASS_M;
            case 4: return CLASS_I;
            case 5: return CLASS_HM;
            case 6: return CLASS_U;
            default:
                throw new IllegalArgumentException(Byte.toString(id));
        }
    }
}

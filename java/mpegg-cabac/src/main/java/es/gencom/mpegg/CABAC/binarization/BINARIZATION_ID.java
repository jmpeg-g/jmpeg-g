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

package es.gencom.mpegg.CABAC.binarization;

import es.gencom.mpegg.io.BitReader;
import es.gencom.mpegg.io.BitWriter;
import java.io.IOException;

/**
 * <p>
 * MPEG-G IEC 23092-2 12.3.3 CABAC binarizations.
 * </p>
 * 
 * Table 97: Values of binarization_ID and associated binarizations
 *
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum BINARIZATION_ID {

    BI((byte)0),    // Binary Coding
    TU((byte)1),    // Truncated Unary
    EG((byte)2),    // Exponential Golomb
    SEG((byte)3),   // Signed Exponential Golomb
    TEG((byte)4),   // Truncated Exponential Golomb
    STEG((byte)5),  // Signed Truncated Exponential Golomb
    SUTU((byte)6),  // Split Unit-wise Truncated Unary
    SSUTU((byte)7), // Signed Split Unit-wise Truncated Unary
    DTU((byte)8),   // Double Truncated Unary
    SDTU((byte)9);  // Signed Double Truncated Unary

    public static final byte SIZE_IN_BITS = 5;
    public final byte ID;

    BINARIZATION_ID(final byte binarization_id) {
        this.ID = binarization_id;
    }

    /**
     * <p>
     * Get the BINARIZATION_ID enum by its binarization_id code.
     * </p>
     * 
     * @param binarization_id the binarization_id code
     * @return the BINARIZATION_ID enum which corresponds the the binarization_id code
     */
    public static BINARIZATION_ID getBinarizationId(final byte binarization_id) {
        switch (binarization_id) {
            case 0: return BI;
            case 1: return TU;
            case 2: return EG;
            case 3: return SEG;
            case 4: return TEG;
            case 5: return STEG;
            case 6: return SUTU;
            case 7: return SSUTU;
            case 8: return DTU;
            case 9: return SDTU;
            default: throw new IllegalArgumentException();
        }
    }

    /**
     * <p>
     * Reads the binarization_id enumeration from the bit stream.
     * </p>
     * 
     * @param reader the reader to read the binarization_id from
     * @return the BINARUZATION_ID enum
     * 
     * @throws IOException 
     */
    public static BINARIZATION_ID read(final BitReader reader) throws IOException {
        return getBinarizationId((byte)reader.readBits(SIZE_IN_BITS));
    }

    /**
     * <p>
     * Writes this binarization_id (u5) into the bit stream.
     * </p>
     * 
     * @param writer the stream to write this binarization_id
     * @throws IOException 
     */
    public void write(final BitWriter writer) throws IOException {
        writer.writeBits(ID, SIZE_IN_BITS);
    }
}

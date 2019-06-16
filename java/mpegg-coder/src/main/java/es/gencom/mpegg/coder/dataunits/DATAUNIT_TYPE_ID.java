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

package es.gencom.mpegg.coder.dataunits;

import es.gencom.mpegg.io.BitReader;
import es.gencom.mpegg.io.BitWriter;
import java.io.IOException;

/**
 * <p>
 * MPEG-G IEC 23092-2 7.1 Data unit types.
 * </p>
 * 
 * Values of data_unit_type (Table 4).
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public enum DATAUNIT_TYPE_ID {

    RAW_REF((short)0), // Binary Coding
    PARAMS((short)1),  // Truncated Unary
    AU((short)2);      // Exponential Golomb

    public final short ID;

    DATAUNIT_TYPE_ID(final short binarization_id) {
        this.ID = binarization_id;
    }

    /**
     * <p>
     * Writes this dataunit_type_id (u8) into the bit stream.
     * </p>
     * 
     * @param writer the stream to write this dataunit_type_id
     * @throws IOException 
     */
    public void write(final BitWriter writer) throws IOException {
        writer.writeUnsignedByte(ID);
    }

    /**
     * <p>
     * Reads the dataunit_type_id enumeration from the bit stream.
     * </p>
     * 
     * @param reader the reader to read the dataunit_type_id from
     * @return the DATAUNIT_TYPE_ID enum
     * @throws IOException 
     */
    public static DATAUNIT_TYPE_ID read(final BitReader reader) throws IOException {
        return getDataUnitTypeId((byte)reader.readUnsignedByte());
    }

    /**
     * <p>
     * Get the DATAUNIT_TYPE_ID enum by its dataunit_type_id code.
     * </p>
     * 
     * @param dataunit_type_id the dataunit_type_id code
     * @return the DATAUNIT_TYPE_ID enum which corresponds the the dataunit_type_id code
     */
    public static DATAUNIT_TYPE_ID getDataUnitTypeId(final short dataunit_type_id) {
        switch (dataunit_type_id) {
            case 0: return RAW_REF;
            case 1: return PARAMS;
            case 2: return AU;
            default: throw new IllegalArgumentException();
        }
    }
}

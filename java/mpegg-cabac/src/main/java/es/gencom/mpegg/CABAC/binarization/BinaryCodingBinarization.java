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

import es.gencom.mpegg.io.BitWriter;
import java.io.IOException;

/**
 * <p>
 * Binary coding (BI) Binarization implementation (12.2.1).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class BinaryCodingBinarization extends AbstractBinarization {
    
    public final int cLength;

    public BinaryCodingBinarization(final int cLength) {
        super(BINARIZATION_ID.BI);

        this.cLength = cLength;
    }

    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return cLength;
    }
    
    @Override
    public void encode(final MCoderBitWriter writer, final long sym_val) throws IOException {
        writer.writeBits(sym_val, cLength);
    }
    
    @Override
    public long decode(final MCoderBitReader reader) throws IOException {
        return reader.readBits(cLength);
    }
    
    @Override
    public void write(final BitWriter writer) throws IOException {
        // do nothing
    }

    @Override
    public long sizeInBits() {
        return 0;
    }
}

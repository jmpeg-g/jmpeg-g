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
 * Signed Double Truncated Unary (SDTU) Binarization implementation (12.2.9).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class SignedDoubleTruncatedUnaryBinarization extends DoubleTruncatedUnaryBinarization {
    
    public SignedDoubleTruncatedUnaryBinarization(
            final int cMaxDTU, final byte splitUnitSize, final byte outputSymSize) {

        super(BINARIZATION_ID.SDTU, cMaxDTU, splitUnitSize, outputSymSize);
    }

    /**
     * @param numAlphaSubsym number of alphabet symbols
     * @return cMaxDTU + ceil(outputSymSize / splitUnitSize) * ((1 &lt;&lt; splitUnitSize) - 1) + 1
     */
    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return super.getNumContextsSymbol(numAlphaSubsym) + 1;
    }

    @Override
    public void encode(final MCoderBitWriter writer, final long sym_val) throws IOException {
        super.encode(writer, Math.abs(sym_val));
        writer.writeBits(sym_val >>> 63, 1);
    }
    
    @Override
    public long decode(final MCoderBitReader reader) throws IOException {
        final long sym_val = super.decode(reader);
        if (sym_val != 0) {
            return reader.readBits(1) == 0 ? sym_val : -sym_val;
        }
        return 0;
    }
    
    @Override
    public void write(final BitWriter writer) throws IOException {
        writer.writeBits(tu == null ? 0 : tu.cMax, 8);
        writer.writeBits(splitUnitSize, 4);
    }

    @Override
    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += 8;
        sizeInBits += 4;
        return sizeInBits;
    }

    public static SignedDoubleTruncatedUnaryBinarization read(
            final BitReader reader, final byte outputSymSize) throws IOException {

        final int cMaxDTU = reader.readUnsignedByte();
        final byte splitUnitSize = (byte)reader.readBits(4);

        return new SignedDoubleTruncatedUnaryBinarization(cMaxDTU, splitUnitSize, outputSymSize);
    }
}

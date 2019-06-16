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
 * Signed Split Unit-wise Truncated Unary (SSUTU) Binarization implementation (12.2.7).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class SignedSplitUnitWiseTruncatedUnaryBinarization 
        extends SplitUnitWiseTruncatedUnaryBinarization {
    
    public SignedSplitUnitWiseTruncatedUnaryBinarization(
            final byte splitUnitSize, final byte outputSymSize) {
        super(BINARIZATION_ID.SSUTU, splitUnitSize, outputSymSize);
    }

    /**
     * @return ceil(outputSymSize / splitUnitSize) * ((1 &lt;&lt; splitUnitSize) - 1) + 1
     */
    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return (outputSymSize / splitUnitSize) * ((1 << splitUnitSize) - 1) + ((1<<(outputSymSize % splitUnitSize)) - 1) + 1;
    }
    
    @Override
    public void encode(final MCoderBitWriter writer, final long sym_val) throws IOException {
        super.encode(writer, Math.abs(sym_val));
        if (sym_val != 0) {
            writer.writeBits(sym_val >>> 63, 1);
        }
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
        writer.writeBits(splitUnitSize, 4);
    }

    @Override
    public long sizeInBits() {
        return 4;
    }

    public static SignedSplitUnitWiseTruncatedUnaryBinarization read(
            final BitReader reader, final byte outputSymSize) throws IOException {
        final byte splitUnitSize = (byte)reader.readBits(4);
        return new SignedSplitUnitWiseTruncatedUnaryBinarization(splitUnitSize, outputSymSize);
    }
}

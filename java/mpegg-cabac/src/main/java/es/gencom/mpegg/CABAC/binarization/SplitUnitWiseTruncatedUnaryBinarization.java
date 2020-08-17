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
 * Split Unit-wise Truncated Unary (SUTU) Binarization implementation (12.2.6).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class SplitUnitWiseTruncatedUnaryBinarization extends TruncatedUnaryBinarization {
    
    public final byte splitUnitSize;
    public final byte outputSymSize;

    private final int c_max; // last fraction cMax or 0 if all fractions are equal
    private final long mask;

    public SplitUnitWiseTruncatedUnaryBinarization(final byte splitUnitSize, final byte outputSymSize) {
        this(BINARIZATION_ID.SUTU, splitUnitSize, outputSymSize);
    }

    SplitUnitWiseTruncatedUnaryBinarization(
            final BINARIZATION_ID binarization, 
            final byte splitUnitSize, 
            final byte outputSymSize) {
        super(binarization, (1 << splitUnitSize) - 1);
        
        this.splitUnitSize = splitUnitSize;
        this.outputSymSize = outputSymSize;

        c_max = ((1 << (outputSymSize % splitUnitSize)) - 1);
        mask = ~(0xFFFFFFFFFFFFFFFFL << splitUnitSize);
        
    }

    /**
     * @return ceil(outputSymSize / splitUnitSize) * ((1 &lt;&lt; splitUnitSize) - 1)
     */
    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return (outputSymSize / splitUnitSize) * ((1 << splitUnitSize) - 1) + 
               ((1 <<(outputSymSize % splitUnitSize)) - 1);
    }
    
    @Override
    public void encode(final MCoderBitWriter writer, long sym_val) throws IOException {
        for (int i = 0, n = outputSymSize - splitUnitSize; i <= n; i += splitUnitSize, sym_val >>= splitUnitSize) {
            super.encode(writer, sym_val & mask);
            writer.ctxIdx += cMax;
        }
        if (c_max > 0) {
            // last fraction != splitUnitSize
            TruncatedUnaryBinarization.encodeSymbolValue(writer, c_max, sym_val);
            writer.ctxIdx += c_max;
        }
    }
    
    @Override
    public long decode(final MCoderBitReader reader) throws IOException {
        long sym_val = 0;

        int i = 0;
        for (int n = outputSymSize - splitUnitSize; i <= n; i += splitUnitSize) {
            sym_val |= super.decode(reader) << i;
            reader.ctxIdx += cMax;
        }
        if (c_max > 0) {
            // last fraction != splitUnitSize
            sym_val |= TruncatedUnaryBinarization.decodeSymbolValue(reader, c_max) << i;
            reader.ctxIdx += c_max;
        }
        
        return sym_val;
    }

    @Override
    public void write(final BitWriter writer) throws IOException {
        writer.writeBits(splitUnitSize, 4);
    }
    
    public static SplitUnitWiseTruncatedUnaryBinarization read(
            final BitReader reader, final byte outputSymSize) throws IOException {
        
        final byte splitUnitSize = (byte)reader.readBits(4);
        return new SplitUnitWiseTruncatedUnaryBinarization(splitUnitSize, outputSymSize);
    }

    @Override
    public long sizeInBits() {
        return 4; //splitUnitSize
    }
}

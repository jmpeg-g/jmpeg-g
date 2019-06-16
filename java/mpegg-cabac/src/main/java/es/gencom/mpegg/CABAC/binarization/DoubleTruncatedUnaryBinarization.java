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
 * Double Truncated Unary (DTU) Binarization implementation (12.2.8).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DoubleTruncatedUnaryBinarization extends SplitUnitWiseTruncatedUnaryBinarization {
    
    public final TruncatedUnaryBinarization tu;

    public DoubleTruncatedUnaryBinarization(
            final int cMaxDTU, 
            final byte splitUnitSize, 
            final byte outputSymSize) {

        this(BINARIZATION_ID.DTU, cMaxDTU, splitUnitSize, outputSymSize);
    }

    DoubleTruncatedUnaryBinarization(BINARIZATION_ID binarization, int cMaxDTU, byte splitUnitSize, byte outputSymSize) {
        super(binarization, splitUnitSize, outputSymSize);
        tu = cMaxDTU > 0 ? new TruncatedUnaryBinarization(cMaxDTU) : null;
    }

    /**
     * @return cMaxDTU + ceil(outputSymSize / splitUnitSize) * ((1 &lt;&lt; splitUnitSize) - 1)
     */
    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        final int numCtxSubsym = (outputSymSize / splitUnitSize) * ((1 << splitUnitSize) - 1) + ((1 <<(outputSymSize % splitUnitSize)) - 1);
        
        return tu == null ? numCtxSubsym : tu.cMax + numCtxSubsym;
    }

    @Override
    public void encode(final MCoderBitWriter writer, long sym_val) throws IOException {
        if (tu == null) {
            super.encode(writer, sym_val);
        } else {
            if (sym_val >= tu.cMax) {
                tu.encode(writer, tu.cMax);
                writer.ctxIdx += tu.cMax;
                sym_val -= tu.cMax;
                super.encode(writer, sym_val);
            } else {
                tu.encode(writer, sym_val);
            }
        }
    }
    
    @Override
    public long decode(final MCoderBitReader reader) throws IOException {
        if (tu == null) {
            return super.decode(reader);
        }

        long sym_val = tu.decode(reader);
        if (sym_val >= tu.cMax) {
            sym_val += super.decode(reader);
        }

        return sym_val;
    }
    
    @Override
    public void write(final BitWriter writer) throws IOException {
        writer.writeBits(tu == null ? 0 : tu.cMax, 8);
        writer.writeBits(splitUnitSize, 4);
    }
    
    public static DoubleTruncatedUnaryBinarization read(
            final BitReader reader, final byte outputSymSize) throws IOException {

        final int cMaxDTU = reader.readUnsignedByte();
        final byte splitUnitSize = (byte)reader.readBits(4);

        return new DoubleTruncatedUnaryBinarization(cMaxDTU, splitUnitSize, outputSymSize);
    }

    @Override
    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += 8;
        sizeInBits += 4;
        return sizeInBits; //cMax
    }
}

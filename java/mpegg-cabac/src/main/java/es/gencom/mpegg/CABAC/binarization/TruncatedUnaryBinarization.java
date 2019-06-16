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
 * Truncated Unary (TU) Binarization implementation (12.2.2).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class TruncatedUnaryBinarization extends AbstractBinarization {
    
    public final int cMax;
    
    public TruncatedUnaryBinarization(final int cMax) {
        super(BINARIZATION_ID.TU);
        this.cMax = cMax;
    }

    TruncatedUnaryBinarization(final BINARIZATION_ID binarization, final int cMax) {
        super(binarization);
        this.cMax = cMax;
    }

    /**
     * @return (numAlphabetSymbols - 1)
     */
    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return (int)(numAlphaSubsym - 1);
    }

    @Override
    public void encode(final MCoderBitWriter writer, final long sym_val) throws IOException {
        encodeSymbolValue(writer, cMax, sym_val);
    }

    @Override
    public long decode(final MCoderBitReader reader) throws IOException {
        return decodeSymbolValue(reader, cMax);
    }
    
    @Override
    public void write(final BitWriter writer) throws IOException {
        writer.writeBits(cMax, 8);
    }

    @Override
    public long sizeInBits() {
        return 8; //cMax
    }

    public static void encodeSymbolValue(final MCoderBitWriter writer, final int cMax, long sym_val) throws IOException {
        int ctxIdx = writer.ctxIdx;
        for (int i = 0; i < sym_val; i++, ctxIdx++) {
            writer.writeBit(ctxIdx, (short)1);
        }
        if (sym_val < cMax) {
            writer.writeBit(ctxIdx, (short)0);
        }
    }
    
    public static long decodeSymbolValue(final MCoderBitReader reader, final int cMax) throws IOException {
        int sym_val = 0;
        
        while (sym_val < cMax && reader.readBits(reader.ctxIdx + sym_val, 1) != 0) {
            sym_val++;
        }

        return sym_val;
    }
    
    public static TruncatedUnaryBinarization read(final BitReader reader) throws IOException {

        final int cmax = reader.readUnsignedByte();   // u(8) Table 97
        return new TruncatedUnaryBinarization(cmax);
    }
}

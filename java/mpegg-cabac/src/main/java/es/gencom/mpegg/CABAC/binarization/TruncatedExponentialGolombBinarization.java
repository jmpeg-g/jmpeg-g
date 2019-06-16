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
 * Truncated Exponential Golomb (TEG) Binarization implementation (12.2.4).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class TruncatedExponentialGolombBinarization extends TruncatedUnaryBinarization {
    
    public TruncatedExponentialGolombBinarization(final byte cTruncExpGolParam) {
        super(BINARIZATION_ID.TEG, cTruncExpGolParam);
    }

    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return cMax + 64 - Long.numberOfLeadingZeros(numAlphaSubsym + 1);
    }
    
    @Override
    public void encode(MCoderBitWriter writer, long sym_val) throws IOException {
        if (sym_val < cMax) {
            super.encode(writer, sym_val);
        } else {
            super.encode(writer, (byte)cMax);
            ExponentialGolombBinarization.encodeSymbolValue(writer, sym_val - cMax);
        }
    }

    @Override
    public long decode(MCoderBitReader reader) throws IOException {
        long sym = super.decode(reader);
        if (sym == cMax) {
            sym += ExponentialGolombBinarization.decodeSymbolValue(reader);
        }

        return sym;
    }
    
    @Override
    public void write(final BitWriter writer) throws IOException {
        writer.writeBits(cMax, 6);
    }

    @Override
    public long sizeInBits() {
        return 6;
    }

    public static TruncatedExponentialGolombBinarization read(final BitReader reader) throws IOException {
        final byte cTruncExpGolParam = (byte)reader.readBits(6);
        return new TruncatedExponentialGolombBinarization(cTruncExpGolParam);
    }
}

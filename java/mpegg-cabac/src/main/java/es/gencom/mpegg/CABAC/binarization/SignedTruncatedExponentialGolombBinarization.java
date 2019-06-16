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
 * Signed Truncated Exponential Golomb (STEG) Binarization implementation (12.2.5).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class SignedTruncatedExponentialGolombBinarization extends TruncatedUnaryBinarization {
    
    public SignedTruncatedExponentialGolombBinarization(byte signedTruncExpGolParam) {
        super(BINARIZATION_ID.STEG, signedTruncExpGolParam);
    }

    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return cMax + 65 - Long.numberOfLeadingZeros(numAlphaSubsym + 1);
    }
    
    @Override
    public void encode(MCoderBitWriter writer, long sym_val) throws IOException {
        if (sym_val == 0) {
            writer.writeBits(0, 1);
        } else {
            if (Math.abs(sym_val) < cMax) {
                super.encode(writer, Math.abs(sym_val));
            } else {
                super.encode(writer, cMax);
                ExponentialGolombBinarization.encodeSymbolValue(writer, Math.abs(sym_val) - cMax);
            }
            writer.writeBits(sym_val >>> 31, 1);
        }
    }
    
    @Override
    public long decode(MCoderBitReader reader) throws IOException {
        long sym = super.decode(reader);
        if (sym > 0) {
            if (sym == cMax) {
                sym += ExponentialGolombBinarization.decodeSymbolValue(reader);
            }
            if (reader.readBits(1) == 1) {
                sym = -sym;
            }
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

    public static SignedTruncatedExponentialGolombBinarization read(final BitReader reader) throws IOException {
        final byte cTruncExpGolParam = (byte)reader.readBits(6);
        return new SignedTruncatedExponentialGolombBinarization(cTruncExpGolParam);
    }
}

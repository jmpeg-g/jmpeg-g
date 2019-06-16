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
 * Signed Exponential Golomb (SEG) Binarization implementation (12.2.3.2).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class SignedExponentialGolombBinarization extends ExponentialGolombBinarization {
    
    public SignedExponentialGolombBinarization() {
        super(BINARIZATION_ID.SEG);
    }
    
    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return 63 - Long.numberOfLeadingZeros(numAlphaSubsym + 1) + 2;
    }
    
    @Override
    public void encode(MCoderBitWriter writer, long sym_val) throws IOException {
        SignedExponentialGolombBinarization.encodeSymbolValue(writer, sym_val);
    }
    
    @Override
    public long decode(MCoderBitReader reader) throws IOException {
        return SignedExponentialGolombBinarization.decodeSymbolValue(reader);
    }
    
    @Override
    public void write(final BitWriter writer) throws IOException {
        // no config params
    }

    @Override
    public long sizeInBits() {
        return 0;
    }

    public static SignedExponentialGolombBinarization read(final BitReader reader) throws IOException {
        return new SignedExponentialGolombBinarization();
    }
    
    static void encodeSymbolValue(final MCoderBitWriter writer, final long sym_val) throws IOException {
        final long val = sym_val <= 0 ? -sym_val << 1: (sym_val << 1) - 1;
        ExponentialGolombBinarization.encodeSymbolValue(writer, val);        
    }
    
    static long decodeSymbolValue(final MCoderBitReader reader) throws IOException {
        final long val = ExponentialGolombBinarization.decodeSymbolValue(reader);
        return (val & 1) == 0 ? (val + 1) >> 1 : -val >> 1;
    }
}

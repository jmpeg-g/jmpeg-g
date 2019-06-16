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
 * Exponential Golomb (EG) Binarization implementation (12.2.3).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class ExponentialGolombBinarization extends AbstractBinarization {

    public ExponentialGolombBinarization() {
        this(BINARIZATION_ID.EG);
    }
    
    ExponentialGolombBinarization(final BINARIZATION_ID binarization) {
        super(binarization);
    }

    /**
     * @return (floor(log2(numAlphaSubsym + 1)) + 1)
     */
    @Override
    public int getNumContextsSymbol(final long numAlphaSubsym) {
        return 63 - Long.numberOfLeadingZeros(numAlphaSubsym + 1) + 1;
    }
    
    @Override
    public void encode(MCoderBitWriter writer, long sym_val) throws IOException {
        ExponentialGolombBinarization.encodeSymbolValue(writer, sym_val);
    }

    @Override
    public long decode(MCoderBitReader reader) throws IOException {
        return ExponentialGolombBinarization.decodeSymbolValue(reader);
    }
    
    @Override
    public void write(final BitWriter writer) throws IOException {
        // no config params
    }

    @Override
    public long sizeInBits() {
        return 0;
    }

    public static ExponentialGolombBinarization read(final BitReader reader) throws IOException {
        return new ExponentialGolombBinarization();
    }

    static void encodeSymbolValue(MCoderBitWriter writer, long sym_val) throws IOException {
        if (sym_val == 0) {
            writer.writeBits(1, 1);
        } else {
            long val = sym_val + 1;
            int zeros = 63 - Long.numberOfLeadingZeros(val);
            writer.writeBits(0, zeros);
            writer.writeBits(1, 1);
            writer.writeBits(val - (1 << zeros), zeros);
        }        
    }

    static long decodeSymbolValue(BitReader reader) throws IOException {
        int zeros = 0;
        while (reader.readBits(1) == 0) {
            zeros++;
        }
        return zeros == 0 ? 0 : (1 << zeros) + reader.readBits(zeros) - 1;
    }

    /**
     * Decode EG syntax element using bypass mode for the suffix part.
     * 
     * @param reader the MCoderBitReader
     * @return the decoded syntax element
     * 
     * @throws IOException 
     */
    static long decodeSymbolValue(MCoderBitReader reader) throws IOException {
        int zeros = 0;
        while (reader.readBits(1, false) == 0) {
            zeros++;
        }
        return zeros == 0 ? 0 : (1 << zeros) + reader.readBits(zeros, true) - 1;
    }


}

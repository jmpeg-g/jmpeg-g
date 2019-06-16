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

package es.gencom.mpegg.CABAC.decoder;

import es.gencom.mpegg.CABAC.binarization.AbstractBinarization;
import es.gencom.mpegg.CABAC.binarization.BINARIZATION_ID;
import es.gencom.mpegg.CABAC.binarization.MCoderBitReader;
import es.gencom.mpegg.CABAC.binarization.SplitUnitWiseTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.TruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.configuration.CodecConfiguration;
import es.gencom.mpegg.CABAC.configuration.DefaultCodecConfigurations;
import es.gencom.mpegg.CABAC.mcoder.MDecoder;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.io.BitReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * CABAC syntax element (i.e. descriptor) decoder.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class SyntaxElementDecoder {

    private final CABACBitReader reader;

    public final AbstractBinarization binarization;
    public final CodecConfiguration configuration;
    private final InputStream ref_source;
    
    public SyntaxElementDecoder(
                final BitReader reader,
                final DESCRIPTOR_ID descriptor_id,
                final int descriptor_subsequence_id,
                final ALPHABET_ID alphabet_id,
                final boolean primary_alignments_only,
                final InputStream ref_source) throws IOException {

        this(reader, 
             DefaultCodecConfigurations.getDefaultCodecConfiguration(
                    descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only),
             DefaultCodecConfigurations.getDefaultBinarization(
                    descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only),
             true, /* adaptive_mode_flag */
             ref_source);
    }
    
    public SyntaxElementDecoder(
                final BitReader reader,
                final COMPRESSION_METHOD_ID method_id,
                final InputStream ref_source) throws IOException {
        
        this(reader,
            DefaultCodecConfigurations.getDefaultCodecConfiguration(method_id),
            DefaultCodecConfigurations.getDefaultBinarization(method_id),
            true, /* adaptive_mode_flag */
            ref_source);
    }
    
    public SyntaxElementDecoder(
                final BitReader reader,
                final CodecConfiguration configuration,
                final AbstractBinarization binarization,
                final boolean adaptive_mode_flag,
                final InputStream ref_source) throws IOException {
        
        this.binarization = binarization;
        this.configuration = configuration;

        this.reader = new CABACBitReader(reader, configuration.ctxTable, adaptive_mode_flag);
        this.ref_source = ref_source;
        
        if (configuration.coding_order > 0 && configuration.numCtxLuts > 0) {
            decode_luts();
        }
    }

    private void decode_luts() throws IOException {

        final SplitUnitWiseTruncatedUnaryBinarization sutu =  
                new SplitUnitWiseTruncatedUnaryBinarization((byte)2, configuration.coding_subsym_size);
                
        for (int i = 0; i < configuration.lutValues.length; i++) {
            for (int j = 0; j < configuration.lutValues[i].length; j++) {
                for (int k = 0; k < configuration.num_alphabet_symbols; k++) {
                    reader.ctxIdx = 0;
                    final int numMaxElems = (int)sutu.decode(reader);
                    configuration.lutValues[i][j][k] = new int[numMaxElems];
                    for (int l = 0; l < numMaxElems; l++) {
                        reader.ctxIdx = 0;
                        configuration.lutValues[i][j][k][l] = (short)sutu.decode(reader);
                    }
                }
            }
        }
    }
    
    public long decode() throws IOException {
        
        long symVal = 0;

        for (int subSymIdx = 0, symBits = 0; symBits < configuration.output_symbol_size; symBits += configuration.coding_subsym_size, subSymIdx++) {

            final int lutValsSubSymIdx = configuration.share_subsym_lut_flag ? 0 : subSymIdx;
            final int prvValsSubSymIdx = configuration.share_subsym_prv_flag ? 0: subSymIdx;

            if (ref_source != null && configuration.coding_order > 0) {
                final int refSym = ref_source.read();
                if (refSym < 0) {
                    throw new EOFException("no reference data left.");
                }
                configuration.updateStateValues(prvValsSubSymIdx, refSym);
            }

            reader.ctxIdx = configuration.context_selection(subSymIdx, prvValsSubSymIdx);

            int subSymVal;
            if (binarization.binarization_id == BINARIZATION_ID.TU) {
                int cMax;
                if (configuration.lutValues != null) {
                    if(configuration.coding_order == 1) {
                        cMax = Math.min(configuration.numCtxSubsym, configuration.lutValues[lutValsSubSymIdx][0][configuration.prvValues[prvValsSubSymIdx][0]].length - 1);
                    } else { // coding_order == 2
                        cMax = Math.min(configuration.numCtxSubsym, configuration.lutValues[lutValsSubSymIdx][configuration.prvValues[prvValsSubSymIdx][1]][configuration.prvValues[prvValsSubSymIdx][0]].length - 1);
                    }
                } else {
                    cMax = configuration.numCtxSubsym;
                }

                subSymVal = (int)TruncatedUnaryBinarization.decodeSymbolValue(reader, cMax);
            } else {
                subSymVal = (int)binarization.decode(reader);
            }
            
            switch(configuration.subsym_transform_id) {
                case LUT_TRANSFORM:
                    subSymVal = configuration.lutValues[lutValsSubSymIdx][configuration.coding_order == 2 ? configuration.prvValues[prvValsSubSymIdx][1] : 0][configuration.prvValues[prvValsSubSymIdx][0]][subSymVal];
                    break;
                case DIFF_CODING:
                    subSymVal += configuration.prvValues[prvValsSubSymIdx][0];
                    break;
            }
            
            if (configuration.coding_order > 0) {
                configuration.updateStateValues(prvValsSubSymIdx, subSymVal);
            }

            symVal |= subSymVal << symBits;
        }

        return symVal;
    }
    
    public static class CABACBitReader extends MCoderBitReader {

        private final MDecoder decoder;
        
        public CABACBitReader(final BitReader reader,
                              final short[][] ctxTable,
                              final boolean adaptive_mode_flag) throws IOException {
            this.decoder = new MDecoder(reader, ctxTable, adaptive_mode_flag);
        }
        
        @Override
        public long readBits(int ctxIdx, int nbits, final boolean bypass) throws IOException {
            long val = 0;

            if (bypass) {
                while (nbits-- > 0) {
                    val = (val << 1) | decoder.bypass();
                }
            } else {
                while (nbits-- > 0) {
                    val = (val << 1) | decoder.decode(ctxIdx++);
                }
            }
            return val;
        }
    }
}

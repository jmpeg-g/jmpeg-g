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

package es.gencom.mpegg.CABAC.encoder;

import es.gencom.mpegg.CABAC.binarization.AbstractBinarization;
import es.gencom.mpegg.CABAC.binarization.BINARIZATION_ID;
import es.gencom.mpegg.CABAC.binarization.MCoderBitWriter;
import es.gencom.mpegg.CABAC.binarization.SplitUnitWiseTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.TruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.configuration.CodecConfiguration;
import es.gencom.mpegg.CABAC.configuration.DefaultCodecConfigurations;
import es.gencom.mpegg.CABAC.mcoder.MEncoder;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.io.BitWriter;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MSBitOutputArray;
import es.gencom.mpegg.io.Payload;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class SyntaxElementEncoder implements Closeable {

    private final CABACBitWriter writer;

    public final AbstractBinarization binarization;
    public final CodecConfiguration configuration;

    private final MSBitOutputArray data_buf;
    
    private final InputStream ref_source;
    private final ByteArrayOutputStream ref_source_buf;

    public SyntaxElementEncoder(
                final BitWriter writer,
                final DESCRIPTOR_ID descriptor_id,
                final int descriptor_subsequence_id,
                final ALPHABET_ID alphabet_id,
                final boolean primary_alignments_only,
                final InputStream ref_source) throws IOException {

        this(writer, 
             DefaultCodecConfigurations.getDefaultCodecConfiguration(
                    descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only),
             DefaultCodecConfigurations.getDefaultBinarization(
                    descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only),
             true, /* adaptive_mode_flag */
             ref_source);
    }
    
    public SyntaxElementEncoder(
                final BitWriter writer,
                final COMPRESSION_METHOD_ID method_id,
                final InputStream ref_source) throws IOException {
        
        this(writer,
            DefaultCodecConfigurations.getDefaultCodecConfiguration(method_id),
            DefaultCodecConfigurations.getDefaultBinarization(method_id),
            true, /* adaptive_mode_flag */
            ref_source);
    }

    public SyntaxElementEncoder(
                final BitWriter writer,
                final CodecConfiguration configuration,
                final AbstractBinarization binarization,
                final boolean adaptive_mode_flag,
                final InputStream ref_source) throws IOException {
        
        this.binarization = binarization;
        this.configuration = configuration;

        this.writer = new CABACBitWriter(writer, configuration.ctxTable, adaptive_mode_flag);
        
        this.ref_source = ref_source;
        if (ref_source != null && ref_source.markSupported()) {
            this.ref_source_buf = null;
            ref_source.mark(Integer.MAX_VALUE);
        } else {
            this.ref_source_buf = new ByteArrayOutputStream();
        }
        
        if (configuration.numCtxLuts > 0 &&
            configuration.lutValues[0][0][0] == null) {
            
            data_buf = new MSBitOutputArray();
            
            for (int i = 0; i < configuration.lutValues.length; i++) {
                for (int j = 0; j < configuration.lutValues[i].length; j++) {
                    for (int k = 0; k < configuration.lutValues[i][j].length; k++) {
                        configuration.lutValues[i][j][k] = new int[configuration.num_alphabet_symbols];
                    }
                }
            }
        } else {
            data_buf = null;
        }
    }

    private void encode_luts() throws IOException {
        final SplitUnitWiseTruncatedUnaryBinarization sutu = 
                new SplitUnitWiseTruncatedUnaryBinarization((byte)2, configuration.coding_subsym_size);
        
        long[] freq = new long[configuration.num_alphabet_symbols];
        
        for (int i = 0; i < configuration.lutValues.length; i++) {
            for (int j = 0; j < configuration.lutValues[i].length; j++) {
                for (int k = 0; k < configuration.num_alphabet_symbols; k++) {
                    int numMaxElems = 0;
                    for (int l = 0; l < freq.length; l++) {
                        if (configuration.lutValues[i][j][k][l] > 0) {
                            freq[numMaxElems++] = ((long)-configuration.lutValues[i][j][k][l] << 32) | l;
                        }
                    }
                    configuration.lutValues[i][j][k] = new int[numMaxElems];
                    writer.ctxIdx = 0;
                    if (numMaxElems == 0) {
                        writer.ctxIdx = 0;
                        sutu.encode(writer, 0);
                    } else {
                        sutu.encode(writer, numMaxElems);
                        Arrays.sort(freq, 0, numMaxElems);

                        for (int l = 0; l < numMaxElems; l++) {
                            final short lutValue = (short)(freq[l] & 0xFFFF);
                            configuration.lutValues[i][j][k][l] = lutValue;

                            writer.ctxIdx = 0;
                            sutu.encode(writer, lutValue);
                        }
                    }
                }
            }
        }

    }
    
    public void encode(final long symVal) throws IOException {
        if (data_buf == null) {
            encode_symbol(symVal, ref_source);
        } else {
            data_buf.writeBits(symVal, configuration.output_symbol_size);
            
            for (int subSymIdx = 0, symBits = 0; symBits < configuration.output_symbol_size; subSymIdx++) {
                
                final int lutValsSubSymIdx = configuration.share_subsym_lut_flag ? 0 : subSymIdx;
                final int prvValsSubSymIdx = configuration.share_subsym_prv_flag ? 0 : subSymIdx;

                final long mask = 0xFFFFFFFFFFFFFFFFL >>> (64 - configuration.coding_subsym_size);

                int subSymVal = (int)((symVal >> symBits) & mask);
                
                symBits += configuration.coding_subsym_size;
            
                if (ref_source != null && configuration.coding_order > 0) {
                    final int refSym = ref_source.read();
                    if (refSym < 0) {
                        throw new EOFException("no reference data left.");
                    }
                    
                    if (ref_source_buf != null) {
                        ref_source_buf.write(refSym);
                    }
                    configuration.updateStateValues(prvValsSubSymIdx, refSym);
                }

                // use luts table to count in this step
                configuration.lutValues[lutValsSubSymIdx][configuration.coding_order == 2 ? configuration.prvValues[prvValsSubSymIdx][1] : 0][configuration.prvValues[prvValsSubSymIdx][0]][subSymVal]++;

                configuration.updateStateValues(prvValsSubSymIdx, subSymVal);
            }
        }
    }
    
    private void encode_symbol(final long symVal, final InputStream ref) throws IOException {
        
        for (int subSymIdx = 0, symBits = 0; symBits < configuration.output_symbol_size; subSymIdx++) {

            final int lutValsSubSymIdx = configuration.share_subsym_lut_flag ? 0 : subSymIdx;
            final int prvValsSubSymIdx = configuration.share_subsym_prv_flag ? 0 : subSymIdx;

            final long mask = 0xFFFFFFFFFFFFFFFFL >>> (64 - configuration.coding_subsym_size);
            
            int subSymVal = (int)((symVal >> symBits) & mask);

            symBits += configuration.coding_subsym_size;

            if (ref != null && configuration.coding_order > 0) {
                final int refSym = ref.read();
                if (refSym < 0) {
                    throw new EOFException("no reference data left.");
                }
                configuration.updateStateValues(prvValsSubSymIdx, refSym);
            }

            writer.ctxIdx = configuration.context_selection(subSymIdx, prvValsSubSymIdx);
            
            switch(configuration.subsym_transform_id) {
                case LUT_TRANSFORM:
                    lut_transform_encode(lutValsSubSymIdx, prvValsSubSymIdx, subSymVal);
                    break;
                case DIFF_CODING:
                    subSymVal -= configuration.prvValues[prvValsSubSymIdx][0];
                default:
                    binarization.encode(writer, subSymVal);
            }
            
            if (configuration.coding_order > 0) {
                configuration.updateStateValues(prvValsSubSymIdx, subSymVal);
            }
        }
    }

    private void lut_transform_encode(
            final int lutValsSubSymIdx,
            final int prvValsSubSymIdx,
            final int subSymVal) throws IOException {

        int lutVal = 0;
        final int[] luts = configuration.lutValues[lutValsSubSymIdx][configuration.coding_order == 2 ? configuration.prvValues[prvValsSubSymIdx][1] : 0][configuration.prvValues[prvValsSubSymIdx][0]];
        for (int j = 0; j < luts.length; j++) {
            if (luts[j] == subSymVal) {
                lutVal = j;
                break;
            }
        }

        if (binarization.binarization_id == BINARIZATION_ID.TU) {
            int cMax;
            if(configuration.coding_order == 1) {
                cMax = Math.min(configuration.numCtxSubsym, configuration.lutValues[lutValsSubSymIdx][0][configuration.prvValues[prvValsSubSymIdx][0]].length - 1);
            } else { // coding_order == 2
                cMax = Math.min(configuration.numCtxSubsym, configuration.lutValues[lutValsSubSymIdx][configuration.prvValues[prvValsSubSymIdx][1]][configuration.prvValues[prvValsSubSymIdx][0]].length - 1);
            }
            TruncatedUnaryBinarization.encodeSymbolValue(writer, cMax, lutVal);
        } else {
            binarization.encode(writer, lutVal);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (data_buf != null) {
                encode_luts();

                configuration.resetStateValues();

                final InputStream ref;
                if (ref_source == null) {
                    ref = null;
                } else if (ref_source.markSupported()) {
                    ref_source.reset();
                    ref = ref_source;
                } else {
                    ref = new ByteArrayInputStream(ref_source_buf.toByteArray());
                }

                final ByteBuffer buf = data_buf.toByteBuffer();
                final MPEGReader reader = new Payload(buf);
                for (int i = 0, n = (int)(data_buf.getLength() / configuration.output_symbol_size); i < n; i++) {
                    final long symVal = reader.readBits(configuration.output_symbol_size);
                    encode_symbol(symVal, ref);
                }
            }
        } finally {
            writer.encoder.terminate((short)1);
        }
    }

    public static class CABACBitWriter extends MCoderBitWriter {

        private final MEncoder encoder;
        
        public CABACBitWriter(final BitWriter writer,
                              final short[][] ctxTable,
                              final boolean adaptive_mode_flag) throws IOException {
            this.encoder = new MEncoder(writer, ctxTable, adaptive_mode_flag);
        }

        @Override
        public void bypass(final short bit) throws IOException {
            encoder.bypass(bit);
        }
        
        @Override
        public void writeBit(final int ctxIdx, final short bit) throws IOException{
            encoder.encode(ctxIdx, bit);
        }
    }

}

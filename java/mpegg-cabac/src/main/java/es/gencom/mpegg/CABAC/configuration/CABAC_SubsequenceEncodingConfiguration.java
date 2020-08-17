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

package es.gencom.mpegg.CABAC.configuration;

import es.gencom.mpegg.CABAC.binarization.AbstractBinarization;
import es.gencom.mpegg.CABAC.binarization.BINARIZATION_ID;
import es.gencom.mpegg.CABAC.binarization.BinaryCodingBinarization;
import es.gencom.mpegg.CABAC.binarization.DoubleTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.ExponentialGolombBinarization;
import es.gencom.mpegg.CABAC.binarization.SignedDoubleTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.SignedExponentialGolombBinarization;
import es.gencom.mpegg.CABAC.binarization.SignedSplitUnitWiseTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.SignedTruncatedExponentialGolombBinarization;
import es.gencom.mpegg.CABAC.binarization.SplitUnitWiseTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.TruncatedExponentialGolombBinarization;
import es.gencom.mpegg.CABAC.binarization.TruncatedUnaryBinarization;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * <p>
 * Subsequence Decoding Configuration.
 * </p>
 * 
 * The class encapsulates CABAC subsequence encoding/decoding configuration parameters
 * found in 12.3.1:
 * 
 * <br><br>
 * 
 * <font color="lightgray">
 * for (j = 0; j &lt; transformSubseqCounter ; j++) {
 * </font>
 * <b>
 * <br>&nbsp;&nbsp;&nbsp; transform_ID_subsym
 * <br>&nbsp;&nbsp;&nbsp; support_values()
 * <br>&nbsp;&nbsp;&nbsp; cabac_binarization()
 * </b>
 * 
 * <br>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_SubsequenceEncodingConfiguration {
    
    public final CABAC_SymbolEncodingConfiguration symbol_encoding_params;
    public final AbstractBinarization binarization;
    public final CABAC_ContextParameters context_parameters;

    /**
     * <p>
     * Constructor for default tokentype subsequence configuration.
     * </p>
     * 
     * @param method_id should be either CABAC_ORDER_0 or CABAC_ORDER_1
     */
    public CABAC_SubsequenceEncodingConfiguration(final COMPRESSION_METHOD_ID method_id) {
        
        this(DefaultCodecConfigurations.getDefaultBinarization(method_id),
             DefaultCodecConfigurations.getDefaultCodecConfiguration(method_id));
    }

    
    /**
     * <p>
     * Constructor for default descriptor subsequence configuration.
     * </p>
     * 
     * @param descriptor_id
     * @param descriptor_subsequence_id
     * @param alphabet_id
     * @param primary_alignments_only 
     */
    public CABAC_SubsequenceEncodingConfiguration(
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final ALPHABET_ID alphabet_id,
            final boolean primary_alignments_only) {

        this(DefaultCodecConfigurations.getDefaultBinarization(
                        descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only),
             DefaultCodecConfigurations.getDefaultCodecConfiguration(
                        descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only));
    }

    private CABAC_SubsequenceEncodingConfiguration(
            final AbstractBinarization binarization,
            final CodecConfiguration configuration) {
        
        this.binarization = binarization;

        symbol_encoding_params = new CABAC_SymbolEncodingConfiguration(
                configuration.subsym_transform_id,
                configuration.output_symbol_size,
                configuration.coding_subsym_size,
                configuration.coding_order,
                configuration.share_subsym_lut_flag,
                configuration.share_subsym_prv_flag);

        final byte[] context_initialization_values = new byte[configuration.ctxTable[0].length];
        Arrays.fill(context_initialization_values, (byte)64);

        context_parameters = new CABAC_ContextParameters(true, context_initialization_values, configuration.share_subsym_ctx_flag);
    }

    public CABAC_SubsequenceEncodingConfiguration(
            final CABAC_SymbolEncodingConfiguration sym_encoding_params,
            final AbstractBinarization binarization,
            final CABAC_ContextParameters context_parameters) {
        
        switch(sym_encoding_params.transform_ID_subsym) {
            case NO_TRANSFORM:
            case LUT_TRANSFORM:
            case DIFF_CODING: break;
            default: throw new IllegalArgumentException(
                    String.format("invalid transform id: %s", sym_encoding_params.transform_ID_subsym.ID));
        }

        this.symbol_encoding_params = sym_encoding_params;
        this.binarization = binarization;
        this.context_parameters = context_parameters;
    }

    public void write(final MPEGWriter writer) throws IOException {
        symbol_encoding_params.write(writer);

        binarization.binarization_id.write(writer);
        
        writer.writeBits(context_parameters == null ? 1 : 0, 1); // bypass_flag
        
        binarization.write(writer);
        
        if (context_parameters != null) {
            final boolean subsymbols = symbol_encoding_params.coding_subsym_size < symbol_encoding_params.output_symbol_size;
            context_parameters.write(writer, subsymbols);
        }
    }
    
    public static CABAC_SubsequenceEncodingConfiguration read(
            final MPEGReader reader) throws IOException {
        
        final CABAC_SymbolEncodingConfiguration symbol_encoding_params = CABAC_SymbolEncodingConfiguration.read(reader);
        
        final BINARIZATION_ID binarization_id = BINARIZATION_ID.read(reader);

        final boolean bypass_flag = reader.readBits(1) != 0;

        AbstractBinarization binarization;
        
        switch(binarization_id) {
            case BI:    binarization = new BinaryCodingBinarization(symbol_encoding_params.coding_subsym_size); break;
            case TU:    binarization = TruncatedUnaryBinarization.read(reader); break;
            case EG:    binarization = ExponentialGolombBinarization.read(reader); break;
            case SEG:   binarization = SignedExponentialGolombBinarization.read(reader); break;
            case TEG:   binarization = TruncatedExponentialGolombBinarization.read(reader); break;
            case STEG:  binarization = SignedTruncatedExponentialGolombBinarization.read(reader); break;
            case SUTU:  binarization = SplitUnitWiseTruncatedUnaryBinarization.read(reader, symbol_encoding_params.output_symbol_size); break;
            case SSUTU: binarization = SignedSplitUnitWiseTruncatedUnaryBinarization.read(reader, symbol_encoding_params.output_symbol_size); break;
            case DTU:   binarization = DoubleTruncatedUnaryBinarization.read(reader, symbol_encoding_params.output_symbol_size); break;
            case SDTU:  binarization = SignedDoubleTruncatedUnaryBinarization.read(reader, symbol_encoding_params.output_symbol_size); break;
            default: throw new IOException(String.format("illegal binarization: %s", binarization_id.ID));
        }

        final boolean subsymbols = symbol_encoding_params.coding_subsym_size < symbol_encoding_params.output_symbol_size;
        final CABAC_ContextParameters context_parameters = bypass_flag ? null : 
        CABAC_ContextParameters.read(reader, subsymbols);

        return new CABAC_SubsequenceEncodingConfiguration(symbol_encoding_params, 
                binarization, context_parameters);
    }
    
    public CodecConfiguration getSubsequenceCodecConfiguration() {
        
        int numAlphabetSymbols = 1 << symbol_encoding_params.coding_subsym_size;
                
        final AbstractBinarization binarization;
        if (numAlphabetSymbols == 1 << symbol_encoding_params.coding_subsym_size) {
            binarization = this.binarization;
        } else {
            // fix binarization for the alphabet_id
            switch(this.binarization.binarization_id) {
                case TU: binarization = new TruncatedUnaryBinarization(numAlphabetSymbols - 1);
                         break;
                default: binarization = this.binarization;
                         break;
            }
        }
        
        final int numCtxSubsym = binarization.getNumContextsSymbol(numAlphabetSymbols);
                
        final int codingSizeCtxOffset;
        if(symbol_encoding_params.coding_order == 0) {
            codingSizeCtxOffset = numCtxSubsym;
        } else if (symbol_encoding_params.coding_order == 1) {
            codingSizeCtxOffset = numCtxSubsym * numAlphabetSymbols;
        } else { // coding_order == 2
            codingSizeCtxOffset = numCtxSubsym * numAlphabetSymbols * numAlphabetSymbols;
        }

        final short[][] ctxTable = context_parameters.getContextTable();

        final int numCtxLuts = symbol_encoding_params.coding_order > 0 && 
                               symbol_encoding_params.transform_ID_subsym == CABAC_SUBSYM_TRANSFORM_ID.LUT_TRANSFORM ?
                                    ((symbol_encoding_params.coding_subsym_size >> 1) + (symbol_encoding_params.coding_subsym_size & 1)) * 3
                                    : 0;

        return new CodecConfiguration(
                        symbol_encoding_params.coding_order,
                        symbol_encoding_params.output_symbol_size,
                        symbol_encoding_params.coding_subsym_size,
                        symbol_encoding_params.transform_ID_subsym,
                        numAlphabetSymbols,
                        context_parameters.share_subsym_ctx_flag,
                        symbol_encoding_params.share_subsym_lut_flag,
                        symbol_encoding_params.share_subsym_prv_flag,
                        numCtxLuts,
                        codingSizeCtxOffset,
                        (byte)0 /* prvValue */,
                        numCtxSubsym,
                        ctxTable,
                        null);

    }
    
    public CodecConfiguration getSubsequenceCodecConfiguration(
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id) {
        
        int numAlphabetSymbols = 1 << symbol_encoding_params.coding_subsym_size;
        
        // Table 69 fix
        switch(descriptor_id) {
            case MMTYPE:
                switch(descriptor_subsequence_id) {
                    case 0: numAlphabetSymbols = 3; break;
                    case 1:
                    case 2: numAlphabetSymbols = alphabet_id.SIZE; break;
                }
                break;
            case CLIPS:
                switch(descriptor_subsequence_id) {
                    case 1: numAlphabetSymbols = 9; break;
                    case 2: numAlphabetSymbols = alphabet_id.SIZE + 1; break;
                }
                break;
            case UREADS: numAlphabetSymbols = alphabet_id.SIZE; break;
            case RTYPE: numAlphabetSymbols = 6; break;
        }
        
        final AbstractBinarization binarization;
        if (numAlphabetSymbols == 1 << symbol_encoding_params.coding_subsym_size) {
            binarization = this.binarization;
        } else {
            // fix binarization for the alphabet_id
            switch(this.binarization.binarization_id) {
                case TU: binarization = new TruncatedUnaryBinarization(numAlphabetSymbols - 1);
                         break;
                default: binarization = this.binarization;
                         break;
            }
        }
        
        final int numCtxSubsym = binarization.getNumContextsSymbol(numAlphabetSymbols);
                
        final int codingSizeCtxOffset;
        if(symbol_encoding_params.coding_order == 0) {
            codingSizeCtxOffset = numCtxSubsym;
        } else if (symbol_encoding_params.coding_order == 1) {
            codingSizeCtxOffset = numCtxSubsym * numAlphabetSymbols;
        } else { // coding_order == 2
            codingSizeCtxOffset = numCtxSubsym * numAlphabetSymbols * numAlphabetSymbols;
        }

        final short[][] ctxTable = context_parameters.getContextTable();

        final int numCtxLuts = symbol_encoding_params.coding_order > 0 && 
                               symbol_encoding_params.transform_ID_subsym == CABAC_SUBSYM_TRANSFORM_ID.LUT_TRANSFORM ?
                                    ((symbol_encoding_params.coding_subsym_size >> 1) + (symbol_encoding_params.coding_subsym_size & 1)) * 3
                                    : 0;

        return new CodecConfiguration(
                        symbol_encoding_params.coding_order,
                        symbol_encoding_params.output_symbol_size,
                        symbol_encoding_params.coding_subsym_size,
                        symbol_encoding_params.transform_ID_subsym,
                        numAlphabetSymbols,
                        context_parameters.share_subsym_ctx_flag,
                        symbol_encoding_params.share_subsym_lut_flag,
                        symbol_encoding_params.share_subsym_prv_flag,
                        numCtxLuts,
                        codingSizeCtxOffset,
                        (byte)0 /* prvValue */,
                        numCtxSubsym,
                        ctxTable,
                        null);
    }

    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += symbol_encoding_params.sizeInBits();
        sizeInBits += BINARIZATION_ID.SIZE_IN_BITS;
        sizeInBits += 1; //bypass_flag

        sizeInBits += binarization.sizeInBits();

        if (context_parameters != null) {
            final boolean subsymbols = symbol_encoding_params.coding_subsym_size < symbol_encoding_params.output_symbol_size;
            sizeInBits += context_parameters.sizeInBits(subsymbols);
        }

        return sizeInBits;
    }
}

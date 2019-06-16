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

import java.util.Arrays;

/**
 * CABAC codec configuration holder.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CodecConfiguration {
    
    public final byte coding_order;
    public final byte output_symbol_size;
    public final byte coding_subsym_size;

    public final CABAC_SUBSYM_TRANSFORM_ID subsym_transform_id;

    public final int num_alphabet_symbols;

    public final int numCtxSubsym;
    
    public final boolean share_subsym_ctx_flag;
    public final boolean share_subsym_lut_flag;
    public final boolean share_subsym_prv_flag;

    public final int numCtxLuts;
    public final int codingSizeCtxOffset;

    public final short[][] ctxTable;
    public final int[][] prvValues;
    public final int[][][][] lutValues;
    
    private final int[] codingOrderCtxOffset;
    
    public CodecConfiguration(
            final byte coding_order,
            final byte output_symbol_size,
            final byte coding_subsym_size,
            final CABAC_SUBSYM_TRANSFORM_ID subsym_transform_id,
            final int num_alphabet_symbols,
            final boolean share_subsym_ctx_flag,
            final boolean share_subsym_lut_flag,
            final boolean share_subsym_prv_flag,
            final int numCtxLuts,
            final int codingSizeCtxOffset,
            final int prvValue,
            final int numCtxSubsym,
            final short[][] ctxTable,
            final int[][][][] luts) {
        
        this.coding_order = coding_order;
        this.output_symbol_size = output_symbol_size;
        this.coding_subsym_size = coding_subsym_size;
        
        this.subsym_transform_id = subsym_transform_id;

        this.num_alphabet_symbols = num_alphabet_symbols;
        
        this.share_subsym_ctx_flag = share_subsym_ctx_flag;
        this.share_subsym_lut_flag = share_subsym_lut_flag;
        this.share_subsym_prv_flag = share_subsym_prv_flag;
        
        this.numCtxLuts = numCtxLuts;
        this.codingSizeCtxOffset = codingSizeCtxOffset;
        
        this.numCtxSubsym = numCtxSubsym;
        
        if (ctxTable != null) {
            this.ctxTable = ctxTable;
        } else {
            final int numSubsyms = output_symbol_size / coding_subsym_size;
            final int numCtxTotal = numCtxLuts + 
                     (share_subsym_ctx_flag ? 1 : numSubsyms) * codingSizeCtxOffset;
            this.ctxTable = new short[2][numCtxTotal];
            Arrays.fill(this.ctxTable[0], (short)0); // pStateIdx
            Arrays.fill(this.ctxTable[1], (short)1); // valMps
        }

        if (coding_order > 0 && !share_subsym_prv_flag) {
            prvValues = new int[output_symbol_size / coding_subsym_size][2];
        } else {
            prvValues = new int[1][2];
        }
        
        if (luts != null) {
            this.lutValues = luts;
        } else if (coding_order > 0 && numCtxLuts > 0) {
            final int numLuts = share_subsym_lut_flag ? 1 : output_symbol_size / coding_subsym_size;
            final int dim = coding_order == 1 ? 1 : num_alphabet_symbols;
            this.lutValues = new int[numLuts][dim][num_alphabet_symbols][];            
        } else {
            this.lutValues = null;
        }
        
        // Table 105. Calculation of codingOrderCtxOffset[]
        codingOrderCtxOffset = new int[3];
        codingOrderCtxOffset[1] = numCtxSubsym;
        codingOrderCtxOffset[2] = numCtxSubsym * num_alphabet_symbols;
    }
    
    public void resetStateValues() {
        for (int i = 0; i < prvValues.length; i++) {
            for (int j = 0; j < prvValues[i].length; j++) {
                prvValues[i][j] = 0;
            }
        }
    }

    /**
     * <p>
     * 12.6.2.8	Internal state update.
     * </p>
     * 
     * @param subSymIdx subsymbol index
     * @param symVal symbol value to update states
     */
    public void updateStateValues(final int subSymIdx, final int symVal) {
//        for (int i = coding_order - 1; i > 0; i--) {
//           prvValues[subSymIdx][i] = prvValues[subSymIdx][i - 1];
//        }

        if(coding_order == 2) {
            prvValues[subSymIdx][1] = prvValues[subSymIdx][0];
        }
            
        prvValues[subSymIdx][0] = symVal;
    }
    
    /**
     * <p>
     * 12.6.2.5	Context selection.
     * </p>
     *
     * 
     * @param subSymIdx        subsymbol index
     * @param prvValsSubSymIdx subsymbol index or zero (if 
     * 
     * @return 
     */
    public int context_selection(final int subSymIdx, final int prvValsSubSymIdx) {        
        int ctxIdx = 0;
        if(coding_order > 0) {
            ctxIdx += numCtxLuts;
            for (int i = 1; i <= coding_order; i++) {
                ctxIdx += prvValues[prvValsSubSymIdx][i-1] * codingOrderCtxOffset[i];
            }
        }
        return ctxIdx + subSymIdx * codingSizeCtxOffset;
    }    
}

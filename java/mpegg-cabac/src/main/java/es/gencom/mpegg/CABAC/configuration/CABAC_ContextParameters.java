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

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * 12.3.3.2 CABAC context parameters.
 * </p>
 * 
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_ContextParameters {
    
    public final boolean adaptive_mode_flag;            // u(1)
    private final byte context_initialization_values[]; // u(7)

    public final boolean share_subsym_ctx_flag;         // u(1)
    
    public CABAC_ContextParameters(
            final boolean adaptive_mode_flag,
            final byte[] context_initialization_values) {

        this(adaptive_mode_flag, 
             context_initialization_values, 
             false);
    }

    public CABAC_ContextParameters(
            final boolean adaptive_mode_flag,
            final byte[] context_initialization_values,
            final boolean share_subsym_ctx_flag) {

        this.adaptive_mode_flag = adaptive_mode_flag;
        this.context_initialization_values = context_initialization_values;
        
        this.share_subsym_ctx_flag = share_subsym_ctx_flag;
    }

    /**
     * Initializes ctxTable.
     * 
     * @return ctxTable or null (if context_initialization_values is empty).
     */
    public short[][] getContextTable() {
                
        if (context_initialization_values.length == 0) {
            return null;
        }

        short ctxTable[][] = new short[2][context_initialization_values.length];

        for (int ctxIdx = 0; ctxIdx < context_initialization_values.length; ctxIdx++) {
            final byte initState = context_initialization_values[ctxIdx];

            if (initState <= 63) {
                ctxTable[0][ctxIdx] = (short)(63 - initState); // pStateIdx
                ctxTable[1][ctxIdx] = 0; // valMps
            } else {
                ctxTable[0][ctxIdx] = (short)(initState - 64); // pStateIdx
                ctxTable[1][ctxIdx] = 1; // valMps
            }
        }

        return ctxTable;
    }
    
    public void write(final MPEGWriter writer, 
                      final boolean subsymbols) throws IOException {
        
        writer.writeBoolean(adaptive_mode_flag);
        
        writer.writeShort((short)0);
//        writer.writeShort((short)context_initialization_values.length);
//        for (int i = 0, n = context_initialization_values.length; i < n; i++) {
//            writer.writeBits(context_initialization_values[i], 7);
//        }
        
        //if (coding_subsym_size < output_symbol_size) {
        if (subsymbols) {
            writer.writeBoolean(share_subsym_ctx_flag);
        }
    }
    
    public static CABAC_ContextParameters read(
            final MPEGReader reader, final boolean subsymbols) throws IOException {
        
        final boolean adaptive_mode_flag = reader.readBoolean();
        
        final int num_contexts = reader.readUnsignedShort(); // u(16)
        final byte[] context_initialization_values = new byte[num_contexts];
        for (int i = 0; i < num_contexts; i++) {
            context_initialization_values[i] = (byte)reader.readBits(7);
        }

        //if (coding_subsym_size < output_symbol_size)
        final boolean share_subsym_ctx_flag = subsymbols ? reader.readBoolean() : false;
        
        return new CABAC_ContextParameters(
            adaptive_mode_flag,
            context_initialization_values,
            share_subsym_ctx_flag);
    }

    public long sizeInBits(final boolean subsymbols) {
        long sizeInBits = 0;
        sizeInBits += 1; //adaptive_mode_flag;
        sizeInBits += 16; //num_contexts
        //if (coding_subsym_size < output_symbol_size)
        if(subsymbols){
            sizeInBits += 1; //share_subsym_ctx_flag
        }

        return sizeInBits;
    }
}

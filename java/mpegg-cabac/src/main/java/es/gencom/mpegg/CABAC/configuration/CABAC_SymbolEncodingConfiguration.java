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
 * This class contains CABAC symbol/subsymbol encoding parameters basically 
 * enclosing support_values parameters as in 12.3.2
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_SymbolEncodingConfiguration {
    
    public final CABAC_SUBSYM_TRANSFORM_ID transform_ID_subsym;

    public final byte output_symbol_size;       // u(6)
    public final byte coding_subsym_size;       // u(6)
    public final byte coding_order;             // u(2)
    public final boolean share_subsym_lut_flag; // u(1)
    public final boolean share_subsym_prv_flag; // u(1)


    public CABAC_SymbolEncodingConfiguration(
            final CABAC_SUBSYM_TRANSFORM_ID transform_ID_subsym,
            final byte output_symbol_size,
            final byte coding_subsym_size,
            final byte coding_order,
            final boolean share_subsym_lut_flag,
            final boolean share_subsym_prv_flag) {
        
        this.output_symbol_size = output_symbol_size;
        this.coding_subsym_size = coding_subsym_size;
        this.coding_order = coding_order;
        this.transform_ID_subsym = transform_ID_subsym;
        this.share_subsym_lut_flag = share_subsym_lut_flag;
        this.share_subsym_prv_flag = share_subsym_prv_flag;
    }
    
    public static CABAC_SymbolEncodingConfiguration read(MPEGReader reader) throws IOException {
        
        final CABAC_SUBSYM_TRANSFORM_ID transform_ID_subsym = CABAC_SUBSYM_TRANSFORM_ID.read(reader);
                
        final byte output_symbol_size = (byte)reader.readBits(6);
        final byte coding_subsym_size = (byte)reader.readBits(6);
        final byte coding_order = (byte)reader.readBits(2);

        final boolean share_subsym_lut_flag;
        final boolean share_subsym_prv_flag;
        
        if(coding_subsym_size == output_symbol_size || coding_order == 0) {
            share_subsym_lut_flag = false;
            share_subsym_prv_flag = false;
        } else if (transform_ID_subsym == CABAC_SUBSYM_TRANSFORM_ID.LUT_TRANSFORM) {
            share_subsym_lut_flag = reader.readBoolean(); // u(1)
            share_subsym_prv_flag = reader.readBoolean(); // u(1)
        } else {
            share_subsym_lut_flag = false;
            share_subsym_prv_flag = reader.readBoolean(); // u(1)
        }

        return new CABAC_SymbolEncodingConfiguration(
                transform_ID_subsym,
                output_symbol_size,
                coding_subsym_size,
                coding_order,
                share_subsym_lut_flag,
                share_subsym_prv_flag);
    }
    
    public void write(MPEGWriter writer) throws IOException {
        
        transform_ID_subsym.write(writer);
                
        writer.writeBits(output_symbol_size, 6);
        writer.writeBits(coding_subsym_size, 6);
        writer.writeBits(coding_order, 2);
        
        if(coding_subsym_size < output_symbol_size && coding_order > 0) {
            if(transform_ID_subsym == CABAC_SUBSYM_TRANSFORM_ID.LUT_TRANSFORM) {
                writer.writeBoolean(share_subsym_lut_flag);
            }
            writer.writeBoolean(share_subsym_prv_flag);
        }
    }

    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += CABAC_SUBSYM_TRANSFORM_ID.SIZE_IN_BITS;
        sizeInBits += 6; //output_symbol_size
        sizeInBits += 6; //coding_subsym_size;
        sizeInBits += 2; //coding_order


        if(coding_subsym_size < output_symbol_size && coding_order > 0) {
            if(transform_ID_subsym == CABAC_SUBSYM_TRANSFORM_ID.LUT_TRANSFORM) {
                sizeInBits += 1;
            }
            sizeInBits += 1;
        }

        return sizeInBits;
    }
}

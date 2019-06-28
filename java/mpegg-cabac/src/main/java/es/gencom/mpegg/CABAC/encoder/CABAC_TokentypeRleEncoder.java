/**
 * *****************************************************************************
 * Copyright (C) 2017 ELIXIR ES, Spanish National Bioinformatics Institute (INB)
 * and Barcelona Supercomputing Center (BSC)
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

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorEncoder;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * Get over the specification quirk where 'rle_guard_tokentype' parameter is 
 * stored in CABAC configuration while its usage is out of CABAC context.
 * </p>
 * The class implements IEC 23092-2 10.4.19.3.3 RLE
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_TokentypeRleEncoder extends DescriptorEncoder {

    private final MPEGWriter writer;
    private final short rle_guard_tokentype;
    
    private long sym_val = 0;
    private long tot_run = -1;
    
    public CABAC_TokentypeRleEncoder(
            final MPEGWriter writer,
            final DESCRIPTOR_ID descriptor_id,
            final short rle_guard_tokentype) {
        
        super(descriptor_id);
        
        this.writer = writer;
        this.rle_guard_tokentype = rle_guard_tokentype;
    }
    
    @Override
    public void write(final long value) throws IOException {
        if (++tot_run > 0 && sym_val != value) {
            if (sym_val == rle_guard_tokentype) {
                writer.writeUnsignedByte(rle_guard_tokentype);
                if (tot_run > 1) {
                    writer.writeU7(tot_run);
                    writer.writeUnsignedByte((short)sym_val);
                } else {
                    writer.writeU7(0);
                }
            } else if (tot_run < 3) {
                writer.writeUnsignedByte((short)sym_val);
                if (tot_run == 2) {
                    writer.writeUnsignedByte((short)sym_val);
                }
            } else {
                writer.writeUnsignedByte(rle_guard_tokentype);
                writer.writeU7(tot_run);
                writer.writeUnsignedByte((short)sym_val);
            }
            tot_run = 0;
        }
        sym_val = value;
    }

    @Override
    public void close() throws IOException {
        write(sym_val ^ 1); // write differnt dummy symbol
    }
}

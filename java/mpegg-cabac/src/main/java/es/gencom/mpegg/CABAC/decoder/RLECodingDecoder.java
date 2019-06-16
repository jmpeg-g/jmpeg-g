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

import java.io.IOException;

/**
 * <p>
 * An abstract RLE decoder that implements 12.6.2.9.3 RLE transformation.
 * <`p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class RLECodingDecoder implements SubsequenceTransformDecoder {
    
    public final short rle_coding_guard;

    private long sym_val;
    private long tot_run;
    
    public RLECodingDecoder(
            final short rle_coding_guard) {

        this.rle_coding_guard = rle_coding_guard;
    }

    @Override
    public long read() throws IOException {
        
        if (tot_run < 0) {
            tot_run = 0;
            long sym_run;
            while ((sym_run = decode_length()) == rle_coding_guard) {
                tot_run += sym_run;
            }
            tot_run += sym_run;   
        }
        
        if (--tot_run < 0) {
            sym_val = decode();
        }

        return sym_val;
    }
    
    protected long decode_length() throws IOException {
        return decode();
    }
    
    protected abstract long decode() throws IOException;
}

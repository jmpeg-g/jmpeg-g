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
 * An abstract MATCH decoder that implements 12.6.2.9.2 Match transformation.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class MatchCodingDecoder implements SubsequenceTransformDecoder {
    
    private int idx;
    private int len;
    private int ptr;
    private final long[] buf;
    
    public MatchCodingDecoder(final int match_coding_buffer_size) {
        
        idx = match_coding_buffer_size;
        buf = new long[idx];
    }

    @Override
    public long read() throws IOException {
        if (len > 0) {
            len--;
            return buf[idx = inc(idx)] = buf[ptr = inc(ptr)];
        }
        
        len = (int)decode_length();
        if (len-- == 0) {
            return buf[idx = inc(idx)] = decode_symbol();
        }
        
        ptr = idx - (int)decode_pointer() + 1;
        if (ptr < 0) {
            ptr += buf.length;
        }
        
        return buf[idx = inc(idx)] = buf[ptr];
    }
    
    private int inc(int index) {
        return ++index >= buf.length ? 0 : index;
    }
    
    protected abstract long decode_length() throws IOException;
    protected abstract long decode_pointer() throws IOException;
    protected abstract long decode_symbol() throws IOException;
}

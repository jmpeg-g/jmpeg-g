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

import es.gencom.mpegg.coder.compression.transform.DC3;
import java.io.Closeable;
import java.io.IOException;
import java.nio.LongBuffer;
import java.util.Arrays;

/**
 * <p>
 * An abstract MATCH encoder that implements 12.6.2.9.2 Match transformation.
 * </p>
 * 
 * The encoder is based on Suffix Array and compresses entire block of data.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class MatchCodingEncoder implements Closeable, AutoCloseable {
    
    public final static int DEFAULT_MIN_PATTERN_LENGTH = 4;
    
    public final int match_coding_buffer_size;
    public final int min_pattern_length;

    private int size;
    private long[] data;
    
    public MatchCodingEncoder() {
        this(0x10000); // default size = 64K
    }
    
    public MatchCodingEncoder(final int match_coding_buffer_size) {
        this(match_coding_buffer_size, DEFAULT_MIN_PATTERN_LENGTH);
    }

    public MatchCodingEncoder(
            final int match_coding_buffer_size,
            final int min_pattern_length) {
        
        this.match_coding_buffer_size = match_coding_buffer_size;
        this.min_pattern_length = min_pattern_length;

        data = new long[match_coding_buffer_size];
    }
    
    public void write(final long value) throws IOException {
        if (size == data.length) {
            lz();
            if (size == match_coding_buffer_size) {
                data = Arrays.copyOf(data, size << 1);
            } else {
                // shift buffer match_coding_buffer_size left
                System.arraycopy(data, match_coding_buffer_size, data, 0, match_coding_buffer_size);
                size = match_coding_buffer_size;
            }
        }
        data[size++] = value;
    }

    @Override
    public void close() throws IOException {
        lz();
    }
    
    protected abstract void encode_length(long value) throws IOException;
    protected abstract void encode_pointer(long value) throws IOException;
    protected abstract void encode_symbol(long value) throws IOException;
    
    /**
     * Suffix Array based LZ algorithm.
     * 
     * @throws IOException 
     */
    private void lz() throws IOException {
        final LongBuffer buf = LongBuffer.wrap(data, 0, size);
        
        final int[] sa = new int[size];
        DC3.dc3(buf, sa);
        
        final int[] lcp = new int[sa.length];
        final int[] inv = new int[sa.length]; // inverse SA
        
        lcp(buf, sa, lcp, inv);
        
        int pos = size > match_coding_buffer_size ? match_coding_buffer_size : 0;
        
        do {
            int length = 1;
            int distance = match_coding_buffer_size; // limit max distance
            
            /* simple pattern search with no heuristic */
            for (int p = inv[pos] + 1, l = Integer.MAX_VALUE; p < lcp.length && (l = Math.min(l, lcp[p])) >= length; p++) {
                final int d = pos - sa[p];
                if (d > 0 && d < distance) {
                    distance = d;
                    length = l;
                    break;
                }
            }

            for (int p = inv[pos] - 1, l = lcp[inv[pos]]; l >= length; l = Math.min(l, lcp[p]), p--) {
                final int d = pos - sa[p];
                if (d > 0 && (d < distance || (l > length && d < match_coding_buffer_size))) {
                    distance = d;
                    length = l;
                    break;
                }
            }
            
            length = distance > 1 && distance < length ? distance : Math.min(match_coding_buffer_size - 1, length);
            if (length < min_pattern_length) {
                encode_length(0);
                encode_symbol(buf.get(pos));
            } else {
                encode_pointer(distance--);
                encode_length(length--);
                
                pos += length;
            }
        } while (++pos < sa.length);
    }
    
    /**
     * Construct Longest Common Prefix (lcp) array using Kasai's algorithm
     * (doi:10.1007/3-540-48194-X_17).
     * 
     * @param buf original data
     * @param sa  suffix array
     * @param lcp resulted lcp array
     * @param inv resulted inverted suffix array
     * 
     * @return value position where (sa[x] == match_coding_buffer_size)
     */
    private void lcp(final LongBuffer buf, 
                     final int[] sa, 
                     final int[] lcp, 
                     final int[] inv) {
        
        for (int i = 0, n = inv.length; i < n; i++) {
            inv[sa[i]] = i;
        }

        for (int i = 0, k = 0, n = inv.length; i < n; i++) {
            if (inv[i] == n - 1) {
                k = 0;
                continue;
            }

            final int j = sa[inv[i] + 1];
            while (i + k < n && j + k < n &&
                   buf.get(i + k) == buf.get(j + k)) {
                k++;
            }

            lcp[inv[i] + 1] = k;

            if (k > 0) {
                k--;
            }
        }
    }
}

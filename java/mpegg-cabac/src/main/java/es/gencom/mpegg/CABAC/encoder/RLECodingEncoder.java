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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * <p>
 * An abstract RLE encoder that implements 12.6.2.9.3 RLE transformation.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class RLECodingEncoder implements Flushable, Closeable, AutoCloseable {
    
    public final short rle_coding_guard;
    
    private long sym_val = 0;
    private long tot_run = -1;
    
    public RLECodingEncoder(final short rle_coding_guard) {
        this.rle_coding_guard = rle_coding_guard;
    }

    public void write(final long value) throws IOException {
        if (++tot_run > 0 && sym_val != value) {
            encode(sym_val);
            while (tot_run > rle_coding_guard) {
                encode_length(rle_coding_guard);
                tot_run -= rle_coding_guard;
            }
            encode_length(tot_run - 1);
            tot_run = 0;
        }
        sym_val = value;
    }
    
    @Override
    public void flush() throws IOException {
        write(sym_val ^ 1);
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    /**
     * Method is called to write RLE length value.
     * Default implementation writes the length into the encoded stream.
     * 
     * @param value the length value
     * 
     * @throws IOException 
     */
    protected void encode_length(final long value) throws IOException {
        encode(value);
    }

    /**
     * Method to write RLE encoded stream
     * 
     * @param value encoded symbol
     * 
     * @throws IOException 
     */
    protected abstract void encode(final long value) throws IOException;
}

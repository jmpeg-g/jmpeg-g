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

package es.gencom.mpegg.CABAC.binarization;

import java.io.IOException;

/**
 * <p>
 * BitWriter with 'bypass' mode.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class MCoderBitWriter {
    
    public int ctxIdx;
    
    public final void writeBits(final long bits, final int nbits) throws IOException {
        writeBits(ctxIdx, bits, nbits, false);
    }
    
    public final void writeBits(final int ctxIdx, final long bits, final int nbits) throws IOException {
        writeBits(ctxIdx, bits, nbits, false);
    }
    
    public final void writeBits(final long bits, final int nbits, final boolean bypass) throws IOException {
        writeBits(ctxIdx, bits, nbits, bypass);
    }

    public final void writeBits(int ctxIdx, long bits, int nbits, boolean bypass) throws IOException {
        if (bypass) {
            while (nbits-- > 0) {
                bypass((short) ((bits >> nbits) & 1));
            }
        } else {
            while (nbits-- > 0) {
                writeBit(ctxIdx++, (short) ((bits >> nbits) & 1));
            }
        }
    }
        
    public abstract void bypass(short bit) throws IOException;
    
    public abstract void writeBit(int ctxIdx, short bit) throws IOException;
}

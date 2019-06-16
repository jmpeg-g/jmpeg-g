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
 * BitReader with 'bypass' mode.
 * </p>
 * 
 * Bypass mode allows binarizations to switch the mode on subsymbol level.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class MCoderBitReader {

    public int ctxIdx;

    public final long readBits(final int nbits) throws IOException {
        return readBits(ctxIdx, nbits, false);
    }
    
    public final long readBits(final int ctxIdx, final int nbits) throws IOException {
        return readBits(ctxIdx, nbits, false);
    }

    public final long readBits(final int nbits, final boolean bypass) throws IOException {
        return readBits(ctxIdx, nbits, bypass);
    }
    /**
     * 
     * @param ctxIdx
     * @param nbits
     * @param bypass
     * @return
     * @throws IOException 
     */
    public abstract long readBits(int ctxIdx, int nbits, boolean bypass) throws IOException;
}

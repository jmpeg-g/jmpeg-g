/**
 * *****************************************************************************
 * Copyright (C) 2015 Spanish National Bioinformatics Institute (INB) and
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

package es.gencom.integration.deflate;

import es.gencom.integration.io.BitInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Deflate Block Header class (RFC1951 3.2.3).
 * 
 * @author Dmitry Repchevsky
 */

public class BlockHeader {

    public final static int NO_COMPRESSION = 0x00;
    public final static int HUFF_FIXED = 0x01;
    public final static int HUFF_DYNAMIC = 0x02;

    public final boolean bfinal;
    public final int btype;
    public int len;
    
    public BlockHeader(final int btype, final boolean bfinal) {
        this.btype = btype;
        this.bfinal = bfinal;
    }

    public BlockHeader(final BitInputStream in) throws IOException {
        final int block_header = (int) ((in.readBits(3)) & 0b111);
        
        btype = block_header >>> 1;
        bfinal = (block_header & 0x01) != 0;
    }

    public static final byte[] getDefaultLitLen() {
        final byte[] cl = new byte[288];
        Arrays.fill(cl, 0, 144, (byte)8);
        Arrays.fill(cl, 144, 256, (byte)9);
        Arrays.fill(cl, 256, 280, (byte)7);
        Arrays.fill(cl, 280, 288, (byte)8);
        return cl;
    }
    
    public static final byte[] getDefaultDist() {
        final byte[] cl = new byte[32];
        Arrays.fill(cl, 0, 32, (byte)5);
        return cl;
    }
}

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

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.io.MPEGReader;
import java.io.EOFException;
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

public class CABAC_TokentypeRleDecoder extends DescriptorDecoder {
    
    private final MPEGReader reader;
    private final short rle_guard_tokentype;

    private long numOutputSymbols;
    private short sym_val;
    private long rle_len;

    public CABAC_TokentypeRleDecoder(
            final MPEGReader reader,
            final DESCRIPTOR_ID descriptor_id,
            final long numOutputSymbols,
            final short rle_guard_tokentype) throws IOException {

        super(descriptor_id);
        
        this.reader = reader;
        this.numOutputSymbols = numOutputSymbols;
        this.rle_guard_tokentype = rle_guard_tokentype;
    }

    @Override
    public boolean hasNext() throws IOException {
        return numOutputSymbols > 0;
    }

    @Override
    public long read() throws IOException {
        if(--numOutputSymbols < 0) {
            throw new EOFException();
        }
        
        if (--rle_len < 0) {
            sym_val = reader.readUnsignedByte();
            if (sym_val == rle_guard_tokentype) {
                rle_len = reader.readVarSizedUnsignedInt();
                if (rle_len-- > 0) {
                    sym_val = reader.readUnsignedByte();
                }
            }
        }
        
        return sym_val;
    }
}

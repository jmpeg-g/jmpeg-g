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

import es.gencom.mpegg.CABAC.binarization.AbstractBinarization;
import es.gencom.mpegg.CABAC.configuration.CodecConfiguration;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_RLEQVCodingEncoder extends RLECodingEncoder implements SubsequenceTransformEncoder {
    
    private final SyntaxElementEncoder encoder;
        
    public CABAC_RLEQVCodingEncoder(
            final MPEGWriter writer,
            final short rle_coding_guard,
            final CodecConfiguration configuration,
            final AbstractBinarization binarization,
            final boolean adaptive_mode_flag,
            final InputStream ref_source) 
                            throws IOException {
        super(rle_coding_guard);
        
        encoder = new SyntaxElementEncoder(
                        writer, configuration, binarization, adaptive_mode_flag, ref_source);
    }
    
    @Override
    public void write(final long value) throws IOException {
        super.write(value);
    }

    @Override
    public void close() throws IOException {
        super.flush();
        encoder.close();
    }

    @Override
    protected void encode(long value) throws IOException {
        encoder.encode(value);
    }
}

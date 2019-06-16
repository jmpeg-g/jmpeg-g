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

import es.gencom.mpegg.CABAC.configuration.CABAC_SubsequenceEncodingConfiguration;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_EqualityCodingEncoder extends EqualityCodingEncoder 
                                         implements SubsequenceTransformEncoder {
    
    private final SyntaxElementEncoder flags_encoder;
    private final SyntaxElementEncoder symbols_encoder;
    
    public CABAC_EqualityCodingEncoder(
            final MPEGWriter writer,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final CABAC_SubsequenceEncodingConfiguration flags_conf,
            final CABAC_SubsequenceEncodingConfiguration symbols_conf,
            final InputStream ref_source) 
                            throws IOException {
        
        flags_encoder = new SyntaxElementEncoder(
                writer, 
                flags_conf.getSubsequenceCodecConfiguration(alphabet_id, descriptor_id, descriptor_subsequence_id),
                flags_conf.binarization, 
                flags_conf.context_parameters.adaptive_mode_flag, 
                ref_source);

        symbols_encoder = new SyntaxElementEncoder(
                writer, 
                symbols_conf.getSubsequenceCodecConfiguration(alphabet_id, descriptor_id, descriptor_subsequence_id),
                symbols_conf.binarization, 
                symbols_conf.context_parameters.adaptive_mode_flag, 
                ref_source);
    }
    
    @Override
    public void write(final long value) throws IOException {
        super.write(value);
    }

    @Override
    public void close() throws IOException {
        flags_encoder.close();
        symbols_encoder.close();
    }

    @Override
    protected void encode_flag(long value) throws IOException {
        flags_encoder.encode(value);
    }

    @Override
    protected void encode_symbol(long value) throws IOException {
        symbols_encoder.encode(value);
    }
}

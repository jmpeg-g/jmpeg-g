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

public class CABAC_MergeCodingEncoder implements SubsequenceTransformEncoder {
    
    /**
     * ·The sum of the sizes of transformed symbols for all transformed subsequences.
     */
    public final byte output_symbol_size;
    
    private final SyntaxElementEncoder[] encoders;

    public CABAC_MergeCodingEncoder(
            final MPEGWriter writer,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            CABAC_SubsequenceEncodingConfiguration[] encoding_configurations,
            final InputStream ref_source) throws IOException {
        
        byte size = 0;

        encoders = new SyntaxElementEncoder[encoding_configurations.length];
        for (int i = 0; i < encoders.length; i++) {
            size += encoding_configurations[i].symbol_encoding_params.output_symbol_size;

            encoders[i] = new SyntaxElementEncoder(
                            writer, 
                            encoding_configurations[i].getSubsequenceCodecConfiguration(
                                    alphabet_id, descriptor_id, descriptor_subsequence_id),
                            encoding_configurations[i].binarization, 
                            encoding_configurations[i].context_parameters.adaptive_mode_flag, 
                            ref_source);
        }
        
        output_symbol_size = size;
    }

    @Override
    public void write(long value) throws IOException {
        
        // since we push from 'high' to 'low' values, we need to calculate the bits shift.
        int shift = output_symbol_size - encoders[0].configuration.output_symbol_size;
        
        for (int i = 0; i < encoders.length; i++, shift -= encoders[i].configuration.output_symbol_size) {
            encoders[i].encode(value >> shift);
        }
    }

    @Override
    public void close() throws IOException {
        for (int i = 0; i < encoders.length; i++) {
            encoders[i].close();
        }
    }
}

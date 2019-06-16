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

package es.gencom.mpegg.CABAC.configuration.subseq;

import es.gencom.mpegg.CABAC.configuration.CABAC_SubsequenceEncodingConfiguration;
import es.gencom.mpegg.CABAC.configuration.CABAC_SUBSEQ_TRANSFORM_ID;
import es.gencom.mpegg.CABAC.decoder.CABAC_NoTransformDecoder;
import es.gencom.mpegg.CABAC.encoder.CABAC_NoTransformEncoder;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration for the subsequence coding with no transformations
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_SubsequenceCoderConfiguration 
        extends SubsequenceCoderConfiguration {

    public final CABAC_SubsequenceEncodingConfiguration configuration;
    
    public CABAC_SubsequenceCoderConfiguration(
            final CABAC_SubsequenceEncodingConfiguration configuration) {

        super(CABAC_SUBSEQ_TRANSFORM_ID.NO_TRANSFORM);

        this.configuration = configuration;
    }
    
    @Override
    public CABAC_NoTransformDecoder getDecoder(
            final MPEGReader reader,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {
        
        return new CABAC_NoTransformDecoder(
                reader, 
                configuration.getSubsequenceCodecConfiguration(alphabet_id, descriptor_id, descriptor_subsequence_id),
                configuration.binarization,
                configuration.context_parameters.adaptive_mode_flag,
                ref_source);
    }

    @Override
    public CABAC_NoTransformEncoder getEncoder(
            final MPEGWriter writer,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {
        
        return new CABAC_NoTransformEncoder(
                writer, 
                configuration.getSubsequenceCodecConfiguration(alphabet_id, descriptor_id, descriptor_subsequence_id),
                configuration.binarization,
                configuration.context_parameters.adaptive_mode_flag,
                ref_source);
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        configuration.write(writer);
    }
    
    public static CABAC_SubsequenceCoderConfiguration read(
            final MPEGReader reader) throws IOException {
        
        final CABAC_SubsequenceEncodingConfiguration configuration 
                = CABAC_SubsequenceEncodingConfiguration.read(reader);
        
        return new CABAC_SubsequenceCoderConfiguration(configuration);
    }

    @Override
    public long sizeInBits() {
        return configuration.sizeInBits();
    }
}

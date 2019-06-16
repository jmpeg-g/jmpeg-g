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

package es.gencom.mpegg.CABAC.configuration;

import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_DescriptorSubsequenceConfiguration;
import es.gencom.mpegg.CABAC.decoder.CABAC_DescriptorDecoder;
import es.gencom.mpegg.CABAC.encoder.CABAC_DescriptorEncoder;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * CABAC Descritor Decoder Configuration implementation.
 * </p>
 * 
 * While CABAC Decoder Configuration provides configuration read/write, this class 
 * provides Descriptor encoders/decoders for the configuration.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_DescriptorDecoderConfiguration 
        extends CABAC_DecoderConfiguration implements DescriptorDecoderConfiguration<CABAC_DescriptorDecoder, CABAC_DescriptorEncoder> {

    public CABAC_DescriptorDecoderConfiguration() {}
        
    public CABAC_DescriptorDecoderConfiguration(
            final CABAC_DescriptorSubsequenceConfiguration[] subsequenceConfigurations) {
        
        super(subsequenceConfigurations);
    }
    
    @Override
    public CABAC_DescriptorDecoder getDescriptorDecoder(
            final MPEGReader reader,
            final DESCRIPTOR_ID descriptor_id,
            final int descriptor_subsequence_id,
            final ALPHABET_ID alphabet_id,
            final boolean primary_alignments_only,
            final InputStream ref_source) {
        
        if (subsequenceConfigurations != null) {
            for (int i = 0; i < subsequenceConfigurations.length; i++) {
                if (subsequenceConfigurations[i].descriptor_subsequence_id == descriptor_subsequence_id) {
                    return new CABAC_DescriptorDecoder(reader, descriptor_id, alphabet_id, subsequenceConfigurations[i], ref_source);
                }
            }
        }
        
        // no explicit configuration found
        return new CABAC_DescriptorDecoder(reader, descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only, ref_source);        
    }
    
    @Override
    public CABAC_DescriptorEncoder getDescriptorEncoder(
            final MPEGWriter writer,
            final DESCRIPTOR_ID descriptor_id,
            final int descriptor_subsequence_id,
            final ALPHABET_ID alphabet_id,
            final boolean primary_alignments_only,
            final InputStream ref_source) {
        
        if (subsequenceConfigurations != null) {
            for (int i = 0; i < subsequenceConfigurations.length; i++) {
                if (subsequenceConfigurations[i].descriptor_subsequence_id == descriptor_subsequence_id) {
                    return new CABAC_DescriptorEncoder(writer, descriptor_id, alphabet_id, subsequenceConfigurations[i], ref_source);
                }
            }
        }
        
        // no explicit configuration found
        return new CABAC_DescriptorEncoder(writer, descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only, ref_source);
    } 
    
    public static CABAC_DescriptorDecoderConfiguration read(
            final MPEGReader reader) throws IOException {
        
        return new CABAC_DescriptorDecoderConfiguration(
                CABAC_DecoderConfiguration.read(reader).subsequenceConfigurations);
    }
}

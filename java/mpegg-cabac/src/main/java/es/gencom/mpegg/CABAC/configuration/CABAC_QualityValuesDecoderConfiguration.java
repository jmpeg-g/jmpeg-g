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
import es.gencom.mpegg.CABAC.decoder.CABAC_QualityValuesDecoder;
import es.gencom.mpegg.CABAC.encoder.CABAC_QualityValuesEncoder;
import es.gencom.mpegg.coder.compression.QV_CODING_MODE;
import es.gencom.mpegg.coder.compression.QualityValuesDecoderConfiguration;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * CABAC QV Decoder Configuration implementation.
 * </p>
 * 
 * While CABAC Decoder Configuration provides configuration read/write, this class 
 * provides Quality Values encoders/decoders for the configuration.
 *
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_QualityValuesDecoderConfiguration 
        extends CABAC_DecoderConfiguration implements QualityValuesDecoderConfiguration {

    public CABAC_QualityValuesDecoderConfiguration() {}
    
    private CABAC_QualityValuesDecoderConfiguration(
            final CABAC_DescriptorSubsequenceConfiguration[] subsequenceConfigurations) {
        
        super(subsequenceConfigurations);
    }

    @Override
    public CABAC_QualityValuesDecoder getQualityValuesDecoder(
            final MPEGReader reader, 
            final QV_CODING_MODE qv_coding_mode,
            final int descriptor_subsequence_id) throws IOException {
        
        return new CABAC_QualityValuesDecoder(reader, qv_coding_mode, descriptor_subsequence_id, true);
    }

    @Override
    public CABAC_QualityValuesEncoder getQualityValuesEncoder(
            final MPEGWriter writer, 
            final QV_CODING_MODE qv_coding_mode, 
            final int descriptor_subsequence_id) {
        
        if (subsequenceConfigurations != null) {
            for (int i = 0; i < subsequenceConfigurations.length; i++) {
                if (subsequenceConfigurations[i].descriptor_subsequence_id == descriptor_subsequence_id) {
                    return new CABAC_QualityValuesEncoder(writer, subsequenceConfigurations[i]);
                }
            }
        }
        
        return new CABAC_QualityValuesEncoder(writer, descriptor_subsequence_id, qv_coding_mode);
    }
    
    public static CABAC_QualityValuesDecoderConfiguration read(
            final MPEGReader reader) throws IOException {
        
        return new CABAC_QualityValuesDecoderConfiguration(
                CABAC_DecoderConfiguration.read(reader).subsequenceConfigurations);
    }
}

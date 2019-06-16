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
import es.gencom.mpegg.CABAC.configuration.DefaultCodecConfigurations;
import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_DescriptorSubsequenceConfiguration;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorEncoder;
import es.gencom.mpegg.coder.compression.QV_CODING_MODE;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_QualityValuesEncoder extends DescriptorEncoder {
    
    private final MPEGWriter writer;
    private int descriptor_subsequence_id;
    private QV_CODING_MODE qv_coding_mode;
    private CABAC_DescriptorSubsequenceConfiguration subsequenceConfiguration;
    private SubsequenceTransformEncoder encoder;
    
    public CABAC_QualityValuesEncoder(
            final MPEGWriter writer,
            final int descriptor_subsequence_id,
            final QV_CODING_MODE qv_coding_mode) {
        
        super(DESCRIPTOR_ID.QV);
        
        this.writer = writer;
        this.descriptor_subsequence_id = descriptor_subsequence_id;
        this.qv_coding_mode = qv_coding_mode;
    }

    public CABAC_QualityValuesEncoder(
            final MPEGWriter writer,
            final CABAC_DescriptorSubsequenceConfiguration subsequenceConfiguration) {
        
        super(DESCRIPTOR_ID.QV);

        this.writer = writer;
        this.subsequenceConfiguration = subsequenceConfiguration;
    }

    @Override
    public void write(long value) throws IOException {
        if (encoder == null) {
            if (subsequenceConfiguration != null) {
                encoder = subsequenceConfiguration.getEncoder(writer, null, DESCRIPTOR_ID.QV, null);                
            } else {
                AbstractBinarization binarization = DefaultCodecConfigurations.getDefaultBinarization(
                        qv_coding_mode, descriptor_subsequence_id);
                CodecConfiguration configuration = DefaultCodecConfigurations.getDefaultCodecConfiguration(
                        qv_coding_mode, descriptor_subsequence_id);
                  
                //encoder = new CABAC_NoTransformEncoder(writer, configuration, binarization, true, null);
                encoder = new CABAC_RLEQVCodingEncoder(writer, (short)10, configuration, binarization, true, null);
            }
        }

        encoder.write(value);
    }

    @Override
    public void close() throws IOException {
        if (encoder != null) {
            encoder.close();
        }
    }
}

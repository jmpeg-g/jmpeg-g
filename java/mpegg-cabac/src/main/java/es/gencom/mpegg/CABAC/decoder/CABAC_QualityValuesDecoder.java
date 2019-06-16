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

import es.gencom.mpegg.CABAC.binarization.AbstractBinarization;
import es.gencom.mpegg.CABAC.configuration.CodecConfiguration;
import es.gencom.mpegg.CABAC.configuration.DefaultCodecConfigurations;
import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_DescriptorSubsequenceConfiguration;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.QV_CODING_MODE;
import es.gencom.mpegg.io.MPEGReader;
import java.io.EOFException;
import java.io.IOException;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_QualityValuesDecoder extends DescriptorDecoder {

    private final MPEGReader reader;
    private final QV_CODING_MODE qv_coding_mode;
    private final int descriptor_subsequence_id;
    
    private CABAC_DescriptorSubsequenceConfiguration subsequenceConfiguration;
    private SubsequenceTransformDecoder decoder;
    
    private int count;
    private int num_output_symbols;

    public CABAC_QualityValuesDecoder(
            final MPEGReader reader,
            final QV_CODING_MODE qv_coding_mode,
            final int descriptor_subsequence_id,
            final boolean adaptive_mode_flag) {
        
        super(DESCRIPTOR_ID.QV);
        
        this.reader = reader;
        this.qv_coding_mode = qv_coding_mode;
        this.descriptor_subsequence_id = descriptor_subsequence_id;
    }
    
    @Override
    public boolean hasNext() throws IOException {
        if (decoder == null) {
            num_output_symbols = Integer.reverseBytes(reader.readInt());
            
            if (subsequenceConfiguration != null) {
                decoder = subsequenceConfiguration.getDecoder(
                        reader, null, descriptor_id, null);                
            } else {
                final CodecConfiguration configuration = DefaultCodecConfigurations.getDefaultCodecConfiguration(qv_coding_mode, descriptor_subsequence_id);
                final AbstractBinarization binarization = DefaultCodecConfigurations.getDefaultBinarization(qv_coding_mode, descriptor_subsequence_id);

                decoder = new CABAC_RLEQVCodingDecoder(reader, (short)10, configuration, binarization, true, null);
            }
        }

        return count < num_output_symbols;
    }
    
    @Override
    public long read() throws IOException {
        if (hasNext()) {
            count++;
            return decoder.read();
        }
        throw new EOFException();
    }
}

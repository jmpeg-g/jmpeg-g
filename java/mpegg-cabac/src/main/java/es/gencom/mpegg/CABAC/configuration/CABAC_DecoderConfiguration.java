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
import es.gencom.mpegg.coder.compression.AbstractDecoderConfiguration;
import es.gencom.mpegg.coder.compression.ENCODING_MODE_ID;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * CABAC Decoder Configuration (12.3) implementation.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_DecoderConfiguration extends AbstractDecoderConfiguration {
    
    protected final CABAC_DescriptorSubsequenceConfiguration[] subsequenceConfigurations;
    
    public CABAC_DecoderConfiguration() {
        this(null);
    }
    
    public CABAC_DecoderConfiguration(final CABAC_DescriptorSubsequenceConfiguration[] subsequenceConfigurations) {
        super(ENCODING_MODE_ID.CABAC);
        
        this.subsequenceConfigurations = subsequenceConfigurations;
    }
    
    /**
     * Write CABAC Decoder Configuration.
     * 
     * @param writer the writer to write CABAC Decoder Configuration into
     * 
     * @throws IOException 
     */
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        if (subsequenceConfigurations != null && subsequenceConfigurations.length > 0) {
            writer.writeByte((byte)(subsequenceConfigurations.length - 1));
            for (int i = 0; i < subsequenceConfigurations.length; i++) {
                subsequenceConfigurations[i].write(writer);
            }
        }
    }

    @Override
    public long sizeInBits() {
        long sizeInBits = 0;
        if (subsequenceConfigurations != null && subsequenceConfigurations.length > 0) {
            sizeInBits += 8; //numSubsequences
            for (int i = 0; i < subsequenceConfigurations.length; i++) {
                sizeInBits += subsequenceConfigurations[i].sizeInBits();
            }
        }
        return sizeInBits;
    }

    /**
     * <p>
     * Read CABAC Decoder Configuration from MPEG reader.
     * </p>
     * 
     * @param reader the reader to read CABAC Decoder Configuration from
     * @return CABAC Decoder Configuration
     * 
     * @throws IOException 
     */
    public static CABAC_DecoderConfiguration read(
            final MPEGReader reader) throws IOException {
        
        final int num_descriptor_subsequences = reader.readUnsignedByte() + 1;
        final CABAC_DescriptorSubsequenceConfiguration[] decoderSubsequenceConfigurations = 
                new CABAC_DescriptorSubsequenceConfiguration[num_descriptor_subsequences];
        
        for (int i = 0; i < num_descriptor_subsequences; i++) {
            decoderSubsequenceConfigurations[i] = CABAC_DescriptorSubsequenceConfiguration.read(reader);
        }
        
        return new CABAC_DecoderConfiguration(decoderSubsequenceConfigurations);
    }
}

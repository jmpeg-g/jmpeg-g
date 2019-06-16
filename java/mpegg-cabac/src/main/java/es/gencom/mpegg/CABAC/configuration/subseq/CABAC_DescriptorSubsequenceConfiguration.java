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
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_DescriptorSubsequenceConfiguration extends CABAC_SubsequenceConfiguration {
    
    public CABAC_DescriptorSubsequenceConfiguration(
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final ALPHABET_ID alphabet_id,
            final boolean primary_alignments_only) {

        super(descriptor_subsequence_id, new CABAC_SubsequenceCoderConfiguration(
                new CABAC_SubsequenceEncodingConfiguration(
                        descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only)));
    }
            
    public CABAC_DescriptorSubsequenceConfiguration(
            final short descriptor_subsequence_id,
            final SubsequenceCoderConfiguration subsequence_coder_configuration) {
        
        super(descriptor_subsequence_id, subsequence_coder_configuration);
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(descriptor_subsequence_id, 10);
        super.write(writer);
    }

    public static CABAC_DescriptorSubsequenceConfiguration read(
            final MPEGReader reader) throws IOException {
        
        final short descriptor_subsequence_id = (short)reader.readBits(10);
        
        return new CABAC_DescriptorSubsequenceConfiguration(
                descriptor_subsequence_id, 
                CABAC_SubsequenceConfiguration.readSubsequenceCoderConfiguration(reader));
    }

    @Override
    public long sizeInBits() {
        return 10 + super.sizeInBits();
    }
}

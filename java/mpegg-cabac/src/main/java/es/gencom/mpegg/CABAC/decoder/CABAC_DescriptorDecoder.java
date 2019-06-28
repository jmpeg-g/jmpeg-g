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
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.io.MPEGReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * CABAC Descriptor Decoder implementation.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_DescriptorDecoder extends DescriptorDecoder {
    
    private final MPEGReader reader;
    private final ALPHABET_ID alphabet_id;
    private final int descriptor_subsequence_id;
    private final boolean primary_alignments_only;
    private CABAC_DescriptorSubsequenceConfiguration subsequenceConfiguration;
    
    private final InputStream ref_source;
    private SubsequenceTransformDecoder decoder;
    
    private int count;
    private int num_output_symbols;
    
    public CABAC_DescriptorDecoder(
            final MPEGReader reader,
            final DESCRIPTOR_ID descriptor_id,
            final int descriptor_subsequence_id,
            final ALPHABET_ID alphabet_id,
            final boolean primary_alignments_only,
            final InputStream ref_source) {
        
        super(descriptor_id);
        
        this.reader = reader;
        this.alphabet_id = alphabet_id;
        this.descriptor_subsequence_id = descriptor_subsequence_id;
        this.primary_alignments_only = primary_alignments_only;
        this.ref_source = ref_source;
    }
    
    public CABAC_DescriptorDecoder(
            final MPEGReader reader,
            final DESCRIPTOR_ID descriptor_id,
            final ALPHABET_ID alphabet_id,
            final CABAC_DescriptorSubsequenceConfiguration subsequenceConfiguration,
            final InputStream ref_source) {

        super(descriptor_id);
        
        this.reader = reader;
        this.alphabet_id = alphabet_id;
        this.descriptor_subsequence_id = subsequenceConfiguration.descriptor_subsequence_id;
        this.primary_alignments_only = false;
        this.subsequenceConfiguration = subsequenceConfiguration;
        this.ref_source = ref_source;
    }
    
    @Override
    public boolean hasNext() throws IOException {
        if (decoder == null) {
            num_output_symbols = Integer.reverseBytes(reader.readInt());
            
            if (subsequenceConfiguration != null) {
                decoder = subsequenceConfiguration.getDecoder(
                        reader, alphabet_id, descriptor_id, ref_source);                
            } else {
                AbstractBinarization binarization = DefaultCodecConfigurations.getDefaultBinarization(
                        descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only);
                CodecConfiguration configuration = DefaultCodecConfigurations.getDefaultCodecConfiguration(
                        descriptor_id, descriptor_subsequence_id, alphabet_id, primary_alignments_only);
                     
                decoder = new CABAC_NoTransformDecoder(reader, configuration, binarization, true, ref_source);
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

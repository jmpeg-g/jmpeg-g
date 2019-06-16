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

import es.gencom.mpegg.CABAC.configuration.DefaultCodecConfigurations;
import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_DescriptorSubsequenceConfiguration;
import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_SubsequenceConfiguration;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorEncoder;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_TokentypeEncoder extends DescriptorEncoder {

    private final MPEGWriter writer;

    private final COMPRESSION_METHOD_ID compression_method_id;
    private CABAC_SubsequenceConfiguration configuration;
    private CABAC_DescriptorSubsequenceConfiguration subsequenceConfiguration;
    
    private SubsequenceTransformEncoder encoder;

    public CABAC_TokentypeEncoder(
            final MPEGWriter writer,
            final DESCRIPTOR_ID descriptor_id,
            final COMPRESSION_METHOD_ID compression_method_id) {
        
        super(descriptor_id);
        
        this.writer = writer;
        this.compression_method_id = compression_method_id;
    }

    public CABAC_TokentypeEncoder(
            final MPEGWriter writer,
            final DESCRIPTOR_ID descriptor_id,
            final COMPRESSION_METHOD_ID compression_method_id,
            final CABAC_SubsequenceConfiguration configuration) {

        super(descriptor_id);
        
        this.writer = writer;
        this.configuration = configuration;
        this.compression_method_id = compression_method_id;
    }

    @Override
    public void write(final long value) throws IOException {
        if (encoder == null) {
            if (subsequenceConfiguration != null) {
                encoder = configuration.getEncoder(writer, ALPHABET_ID.IUPAC, descriptor_id, null);
            } else {
                encoder = new CABAC_NoTransformEncoder(
                        writer,
                        DefaultCodecConfigurations.getDefaultCodecConfiguration(compression_method_id),
                        DefaultCodecConfigurations.getDefaultBinarization(compression_method_id),
                        true, // adaptive_mode_flag,
                        null /* ref_source */);
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

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

import es.gencom.mpegg.CABAC.configuration.DefaultCodecConfigurations;
import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_SubsequenceConfiguration;
import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.io.MPEGReader;
import java.io.EOFException;
import java.io.IOException;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_TokentypeDecoder extends DescriptorDecoder {

    private final MPEGReader reader;
    private final SubsequenceTransformDecoder decoder;

    private int count;
    private final long numOutputSymbols;
            
    public CABAC_TokentypeDecoder(
            final MPEGReader reader,
            final long numOutputSymbols,
            final DESCRIPTOR_ID descriptor_id,
            final COMPRESSION_METHOD_ID compression_method_id) throws IOException {

        this(reader, numOutputSymbols, descriptor_id, new CABAC_NoTransformDecoder(
                reader,
                DefaultCodecConfigurations.getDefaultCodecConfiguration(compression_method_id),
                DefaultCodecConfigurations.getDefaultBinarization(compression_method_id),
                true, // adaptive_mode_flag
                null /* ref_source */));
    }
    
    public CABAC_TokentypeDecoder(
            final MPEGReader reader,
            final long numOutputSymbols,
            final DESCRIPTOR_ID descriptor_id,
            CABAC_SubsequenceConfiguration configuration) 
            throws IOException {

        this(reader, numOutputSymbols, descriptor_id, 
                configuration.getDecoder(reader, null, descriptor_id, null));
    }

    private CABAC_TokentypeDecoder(
            final MPEGReader reader,
            final long numOutputSymbols,
            final DESCRIPTOR_ID descriptor_id,
            final SubsequenceTransformDecoder decoder) {
        
        super(descriptor_id);

        this.reader = reader;
        this.numOutputSymbols = numOutputSymbols;
        this.decoder = decoder;
    }

    @Override
    public boolean hasNext() throws IOException {
        return count < numOutputSymbols;
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

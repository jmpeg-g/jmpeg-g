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

package es.gencom.mpegg.decoder.descriptors.streams;

import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;

public class UReadsStream {
    private final DescriptorDecoder decoders[];
    private int j6_0 = 0;

    public UReadsStream(
            final AccessUnitBlock block,
            final DATA_CLASS dataClass,
            final EncodingParameters encodingParameters) {
        
        if(block == null){
            decoders = null;
            return;
        }

        DescriptorDecoderConfiguration conf = 
                encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.UREADS, dataClass);

        Payload[] sub_streams = block.getPayloads();

        decoders = new DescriptorDecoder[sub_streams.length];
        for(int substream_index = 0; substream_index < sub_streams.length; substream_index++){
            decoders[substream_index] = conf.getDescriptorDecoder(
                    sub_streams[substream_index],
                    DESCRIPTOR_ID.UREADS,
                    substream_index,
                    encodingParameters.getAlphabetId()
            );
        }
    }

    public byte[] read(int rlen, byte[] S_alphabet_ID) throws IOException {
        byte decodedURead[] = new byte[rlen];

        for(int i=0; i<rlen; i++){
            decodedURead[i] = S_alphabet_ID[(byte)decoders[0].read()];
        }
        return decodedURead;
    }
}

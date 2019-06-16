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

import es.gencom.mpegg.CABAC.configuration.CABAC_DescriptorDecoderConfiguration;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;

public class FlagsStream {
    private Payload readers[];
    private DescriptorDecoder decoders[];
    private byte flags;
    private long j2_0 = 0;
    private long j2_1 = 0;
    private long j2_2 = 0;

    public FlagsStream(
            final DataUnitAccessUnit.Block block, 
            final DATA_CLASS dataClass, 
            final EncodingParameters encodingParameters) {

        CABAC_DescriptorDecoderConfiguration conf = encodingParameters.getDecoderConfiguration(
                DESCRIPTOR_ID.FLAGS,
                dataClass);

        if(block == null) {
            readers = null;
        }else {
            readers = block.getPayloads();
            decoders = new DescriptorDecoder[readers.length];
            for(int subsequence_index = 0; subsequence_index<readers.length; subsequence_index++){
                decoders[subsequence_index] = conf.getDescriptorDecoder(
                        readers[subsequence_index],
                        DESCRIPTOR_ID.FLAGS,
                        subsequence_index,
                        encodingParameters.getAlphabetId()
                );
            }
        }
    }

    public void read() throws IOException {
        flags = 0;

        int val0 = (int)decoders[0].read();
        int val1 = (int)decoders[1].read();
        int val2 = (int)decoders[2].read();

        if(!(
            (val0 == 0 || val0 == 16777216)
            && (val1 == 0 || val1 == 16777216)
            && (val2 == 0 || val2 == 16777216)
        )){
            System.err.println("Strange value.");
        }


        flags |= (byte)(val0 != 0 ? 1:0);
        flags |= (byte)((val1 != 0 ? 1:0) << 1);
        flags |= (byte)((val2 != 0 ? 1:0) << 2);

        j2_0++;
        j2_1++;
        j2_2++;
    }

    public byte getFlags(){
        return flags;
    }

}
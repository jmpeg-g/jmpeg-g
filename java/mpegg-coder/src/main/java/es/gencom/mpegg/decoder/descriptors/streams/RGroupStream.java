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
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;

public class RGroupStream {
    private final DescriptorDecoder decoder;
    private final int numberReadGroups;

    public RGroupStream(
            final DataUnitAccessUnit.Block block,
            final int numberReadGroups,
            final DATA_CLASS dataClass,
            final EncodingParameters encodingParameters) throws IOException {
        
        this.numberReadGroups = numberReadGroups;
        DescriptorDecoderConfiguration conf = 
                encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.RGROUP, dataClass);

        if(block == null) {
            decoder = null;
        } else {
            Payload stream = block.getDescriptorSpecificData();
            decoder = conf.getDescriptorDecoder(
                    stream,
                    DESCRIPTOR_ID.RCOMP,
                    0,
                    encodingParameters.getAlphabetId());
        }
    }

    public int read() throws IOException {
        long readReadGroup = decoder.read();
        if(readReadGroup >= numberReadGroups || readReadGroup < 0){
            throw new IllegalArgumentException();
        }
        return (int)readReadGroup;
    }


}

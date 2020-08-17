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
import es.gencom.mpegg.decoder.GenomicPosition;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;

public class PosStream {
    private final DescriptorDecoder[] decoder;
    private final int numberTemplateSegments;
    private GenomicPosition previousPosition;

    public PosStream(
            AccessUnitBlock block,
            GenomicPosition auStartPosition,
            DATA_CLASS dataClass,
            EncodingParameters encodingParameters
    ){
        this.previousPosition = auStartPosition;
        this.numberTemplateSegments = encodingParameters.getNumberOfTemplateSegments();

        Payload stream;
        if(block == null){
            decoder = null;
        }else {
            stream = block.getSubstream(0);

            //CABAC_DescriptorDecoderConfiguration conf = new CABAC_DescriptorDecoderConfiguration();
            DescriptorDecoderConfiguration conf = 
                    encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.POS, dataClass);

            decoder = new DescriptorDecoder[block.getPayloads().length];
            for(int subsequence_i=0; subsequence_i < block.getPayloads().length; subsequence_i++) {
                decoder[subsequence_i] = conf.getDescriptorDecoder(
                        stream,
                        DESCRIPTOR_ID.POS,
                        0,
                        encodingParameters.getAlphabetId()
                );
            }
        }
    }

    public long[][][] read(int[] numberOfSegmentAlignments, int maxNumberOfSegmentAlignments) throws IOException {
        long[][][] genomicPosition = new long[maxNumberOfSegmentAlignments][numberTemplateSegments][1];

        previousPosition = previousPosition.advance(decoder[0].read());
        genomicPosition[0][0][0] = previousPosition.getPosition();

        for(int i=1; i < numberOfSegmentAlignments[0]; i++){
            genomicPosition[i][0][0] = genomicPosition[i-1][0][0] + decoder[1].read();
        }
        return genomicPosition;
    }

    public boolean hasNext() throws IOException {
        return decoder[0].hasNext();
    }
}

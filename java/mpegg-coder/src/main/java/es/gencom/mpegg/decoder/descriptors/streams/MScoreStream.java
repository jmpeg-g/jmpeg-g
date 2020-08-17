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

//import es.gencom.mpeg.io.ByteReader;
//import java.io.IOException;

import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.format.DATA_CLASS;

import java.io.IOException;

public class MScoreStream {
    private final DescriptorDecoder decoder;
    private final byte as_depth;

    public MScoreStream(
            DATA_CLASS dataClass,
            AccessUnitBlock block,
            EncodingParameters encodingParameters) {
        as_depth = encodingParameters.getASDepth();


        DescriptorDecoderConfiguration conf =
                encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.MSCORE, dataClass);

        if(block == null) {
            decoder = null;
            return;
        }

        decoder = conf.getDescriptorDecoder(
                block.getPayloads()[0],
                DESCRIPTOR_ID.MSCORE,
                0,
                encodingParameters.getAlphabetId()
        );


    }


    public long[][][] read(
            int numberOfAlignedRecordSegments,
            int[] numberOfSegmentAlignments,
            SplitType[][] splitTypes
    ) throws IOException {
        int maxNumAlignedSegments = 0;

        for(int segment_i=0; segment_i < numberOfAlignedRecordSegments; segment_i++){
            maxNumAlignedSegments = Integer.max(maxNumAlignedSegments, numberOfSegmentAlignments[segment_i]);
        }

        long[][][] mappingScores = new long[maxNumAlignedSegments][numberOfAlignedRecordSegments][as_depth];

        for(int i=0; i < as_depth; i++){
            for(int segment_i=0; segment_i<numberOfAlignedRecordSegments; segment_i++){
                for(int alignment_i=0; alignment_i < numberOfSegmentAlignments[segment_i]; alignment_i++){
                    if(splitTypes[alignment_i][segment_i] == SplitType.MappedSameRecord) {
                        mappingScores[alignment_i][segment_i][i] = decoder.read();
                    }
                }
            }
        }

        return mappingScores;
    }
}

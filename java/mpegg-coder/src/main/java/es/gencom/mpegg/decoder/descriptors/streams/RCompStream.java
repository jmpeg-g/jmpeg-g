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

public class RCompStream {
    private final DescriptorDecoder decoder;

    public RCompStream(
            DataUnitAccessUnit.Block block,
            DATA_CLASS dataClass,
            EncodingParameters encodingParameters) throws IOException {

        CABAC_DescriptorDecoderConfiguration conf = encodingParameters.getDecoderConfiguration(
                DESCRIPTOR_ID.RCOMP,
                dataClass
        );

        if(block == null) {
            decoder = null;
        } else {
            Payload stream = block.getDescriptorSpecificData();
            decoder = conf.getDescriptorDecoder(
                stream,
                DESCRIPTOR_ID.RCOMP,
                0,
                encodingParameters.getAlphabetId()
            );
        }
    }

    /**
     *
     * @param numberOfSegmentAlignments Number of alignments for each segments
     * @param splicedSegLength Length of splices for the aligned segments
     * @return for each spliced segment for each alignment for each aligned record segments true if on reverse,
     * false otherise
     * @throws IOException
     */
    public boolean[][][] read(
            int[] numberOfSegmentAlignments,
            long[][][] splicedSegLength
    ) throws IOException {
        boolean[][][] reverseComp = new boolean[Math.toIntExact(numberOfSegmentAlignments.length)][][];

        for(
                int segment_i=0;
                segment_i < numberOfSegmentAlignments.length;
                segment_i++
        ){
            reverseComp[segment_i] = new boolean[numberOfSegmentAlignments[segment_i]][];
            for(
                int segmentAlignment_i = 0;
                segmentAlignment_i < numberOfSegmentAlignments[segment_i];
                segmentAlignment_i++
            ){
                int numberSplices = 1;
                if(splicedSegLength[segment_i].length != 0){
                    numberSplices = splicedSegLength[segment_i][segmentAlignment_i].length;
                }
                reverseComp[segment_i][segmentAlignment_i] = new boolean[numberSplices];

                for(
                        long splice_i = 0;
                        splice_i < splicedSegLength
                                [Math.toIntExact(segment_i)]
                                [Math.toIntExact(segmentAlignment_i)].length;
                        splice_i++
                ) {
                    reverseComp
                            [Math.toIntExact(segment_i)]
                            [Math.toIntExact(segmentAlignment_i)]
                            [Math.toIntExact(splice_i)]
                        = decoder.read() != 0;
                }
            }
        }
        return reverseComp;
    }
}

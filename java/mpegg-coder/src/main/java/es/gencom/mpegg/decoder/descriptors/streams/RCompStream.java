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

import es.gencom.mpegg.ReverseCompType;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;

public class RCompStream {
    private final DescriptorDecoder decoder;

    public RCompStream(
            AccessUnitBlock block,
            DATA_CLASS dataClass,
            EncodingParameters encodingParameters) throws IOException {

        DescriptorDecoderConfiguration conf = 
                encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.RCOMP, dataClass);

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
     * @param numberOfAlignedRecordSegments The number of aligned segments in the record
     * @param numberOfSegmentAlignments Number of alignments for each segments
     * @param splicedSegLength Length of splices for the aligned segments
     * @return for each spliced segment for each alignment for each aligned record segments true if on reverse,
     * false otherise
     * @throws IOException
     */
    public ReverseCompType[][][] read(
            int numberOfAlignedRecordSegments,
            int[] numberOfSegmentAlignments,
            int[] numberOfSplicedSeg,
            int numMaxNumberAlignments,
            int numSegments,
            SplitType[][] splitMate
    ) throws IOException {
        ReverseCompType[][][] reverseComp = new ReverseCompType[numMaxNumberAlignments][numSegments][];

        for(int segment_i = 0; segment_i < numberOfAlignedRecordSegments; segment_i++){
            for(int alignment_j = 0; alignment_j < numberOfSegmentAlignments[segment_i]; alignment_j++){
                reverseComp[alignment_j][segment_i] = new ReverseCompType[numberOfSplicedSeg[segment_i]];
                if(splitMate[alignment_j][segment_i] == SplitType.MappedSameRecord){
                    for(int splice_k = 0; splice_k < numberOfSplicedSeg[segment_i]; splice_k++){
                        reverseComp[alignment_j][segment_i][splice_k] =
                                ReverseCompType.getReverseComp((byte)decoder.read());
                    }
                }
            }
        }
        return reverseComp;
    }
}

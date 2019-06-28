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
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

import static es.gencom.mpegg.decoder.descriptors.streams.StreamsConstantsParams.MAX_NUM_TEMPLATE_SEGMENTS;

public class MMapStream {
    private final DATA_CLASS dataClass;
    private final DescriptorDecoder decoders[];
    private final boolean multiple_alignment_flag;

    public MMapStream(
            final DATA_CLASS dataClass, 
            final DataUnitAccessUnit.Block block,
            final boolean multiple_alignment_flag,
            final EncodingParameters encodingParameters) {

        this.dataClass = dataClass;

        if(block == null){
            decoders = null;
            this.multiple_alignment_flag = false;
            return;
        }

        this.multiple_alignment_flag = multiple_alignment_flag;

        DescriptorDecoderConfiguration conf = 
                encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.MMAP, dataClass);

        Payload[] sub_streams = block.getPayloads();
        decoders = new DescriptorDecoder[sub_streams.length];

        for(int substream_index=0; substream_index < sub_streams.length; substream_index++){
            decoders[substream_index] = conf.getDescriptorDecoder(
                    sub_streams[substream_index],
                    DESCRIPTOR_ID.MMAP,
                    substream_index,
                    encodingParameters.getAlphabetId()
            );
        }
    }

    public MMapStreamSymbol readSymbol(
            boolean unpairedRead,
            long numberOfRecordSegments
    ) throws IOException {
        int[] numberOfSegmentAlignments = new int[MAX_NUM_TEMPLATE_SEGMENTS];
        int numberOfAlignments = 0;
        boolean moreAlignments = false;
        SequenceIdentifier moreAlignmentsNextSeqId = null;
        long moreAlignmentsNextPos = 0;
        long[] numberOfAlignmentsPairs = null;
        int[][] alignPtr = null;

        if(dataClass != DATA_CLASS.CLASS_U){
            if(!multiple_alignment_flag){
                numberOfSegmentAlignments[0] = 1;
            } else {
                numberOfSegmentAlignments[0] = Math.toIntExact(decoders[0].read());
            }
        } else {
            numberOfSegmentAlignments[0] = 0;
        }


        if(unpairedRead || dataClass == DATA_CLASS.CLASS_HM){
            numberOfAlignments = Math.toIntExact(numberOfSegmentAlignments[0]);
            alignPtr = new int[numberOfAlignments][2];

            for(int alignment_i = 0; alignment_i < numberOfAlignments; alignment_i++){
                alignPtr[alignment_i][0] = alignment_i;
            }
        } else if (dataClass == DATA_CLASS.CLASS_U){
            if(numberOfRecordSegments > 1){
                numberOfSegmentAlignments[1] = 0;
            }
            numberOfAlignments = 0;
        } else {
            numberOfAlignmentsPairs = new long[Math.toIntExact(numberOfSegmentAlignments[0])];
            numberOfSegmentAlignments[1] = 0;
            alignPtr = new int[1024][];
            int numberAlignPtr = 0;

            for(long segmentAlignment_i=0; segmentAlignment_i < numberOfSegmentAlignments[0]; segmentAlignment_i++){
                if(!multiple_alignment_flag){
                    numberOfAlignmentsPairs[Math.toIntExact(segmentAlignment_i)] = 1;
                } else {
                    numberOfAlignmentsPairs[Math.toIntExact(segmentAlignment_i)] = decoders[0].read();
                }

                for(
                        long alignmentPair_i=0;
                        alignmentPair_i < numberOfAlignmentsPairs[Math.toIntExact(segmentAlignment_i)];
                        alignmentPair_i++
                ){
                    int ptr;
                    if(segmentAlignment_i != 0){
                        ptr = Math.toIntExact(decoders[1].read());
                    } else {
                        ptr = 0;
                    }
                    alignPtr[numberAlignPtr] = new int[2];
                    alignPtr[numberAlignPtr][0] = Math.toIntExact(segmentAlignment_i);
                    alignPtr[numberAlignPtr][1] = numberOfSegmentAlignments[1] - ptr;

                    if (ptr == 0){
                        numberOfSegmentAlignments[1]++;
                    }
                    numberAlignPtr++;
                    if(alignPtr.length == numberAlignPtr){
                        alignPtr = Arrays.copyOf(alignPtr, alignPtr.length*2);
                    }
                }
            }
            numberOfAlignments = numberAlignPtr;
            alignPtr = Arrays.copyOf(alignPtr, numberOfAlignments);
        }

        if(multiple_alignment_flag && dataClass != DATA_CLASS.CLASS_U){
            if(decoders[2].read() != 0){
                moreAlignments = true;
                moreAlignmentsNextSeqId = new SequenceIdentifier((int) decoders[3].read());
                moreAlignmentsNextPos = decoders[4].read();
            }
        }



        return new MMapStreamSymbol(
            numberOfAlignments,
            numberOfSegmentAlignments,
            numberOfAlignmentsPairs,
            alignPtr,
            moreAlignments,
            moreAlignmentsNextSeqId,
            moreAlignmentsNextPos
        );
    }
}

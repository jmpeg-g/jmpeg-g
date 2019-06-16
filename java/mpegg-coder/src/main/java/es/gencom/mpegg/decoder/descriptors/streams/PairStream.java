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

import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;

import static es.gencom.mpegg.decoder.descriptors.streams.StreamsConstantsParams.MAX_NUM_TEMPLATE_SEGMENTS;

public class PairStream {
    private final Payload sub_streams[];
    private final DescriptorDecoder decoders[];
    private final DATA_CLASS dataClass;
    private final byte numberOfTemplateSegments;

    private int segment_flag;
    private boolean read_1_first;
    private long abs_pos;
    private long delta;
    private long record_index;
    private SequenceIdentifier seqId;
    private long au_ID;
    private boolean paired;
    private long value_subsequence0;
    private boolean same_sequence;


    public PairStream(
            DATA_CLASS dataClass,
            DataUnitAccessUnit.Block block,
            byte numberOfTemplateSegments,
            EncodingParameters encodingParameters
    ) {
        this.dataClass = dataClass;
        this.numberOfTemplateSegments = numberOfTemplateSegments;

        DescriptorDecoderConfiguration configuration = encodingParameters.getDecoderConfiguration(
                DESCRIPTOR_ID.PAIR,
                dataClass
        );

        if(block != null) {
            sub_streams = block.getPayloads();
            decoders = new DescriptorDecoder[sub_streams.length];

            for (int substream_index = 0; substream_index < sub_streams.length; substream_index++) {
                decoders[substream_index] = configuration.getDescriptorDecoder(
                        sub_streams[substream_index],
                        DESCRIPTOR_ID.PAIR,
                        substream_index,
                        encodingParameters.getAlphabetId()
                );
            }
        }else{
            sub_streams = null;
            decoders = null;
        }
    }

    public PairStreamFirstSymbol readFirst() throws IOException {
        if(numberOfTemplateSegments != 1 && dataClass != DATA_CLASS.CLASS_HM){
            value_subsequence0 = decoders[0].read();
        }
        byte numberOfRecordSegments;
        if(numberOfTemplateSegments == 1 || dataClass == DATA_CLASS.CLASS_HM) {
            /*The trivial case where the result can only be equal to numberOfTemplateSegments*/
            numberOfRecordSegments = numberOfTemplateSegments;
        } else if (value_subsequence0 == 0){
            /*We know that the other segment is stored in the same record*/
            numberOfRecordSegments = 2;
        } else {
            /*We know that the other segment is stored in another record*/
            numberOfRecordSegments = 1;
        }

        byte numberOfAlignedRecordSegments;
        if(dataClass == DATA_CLASS.CLASS_HM){
            /*Trivial case as in Class_HM there can only be one aligned record*/
            numberOfAlignedRecordSegments = 1;
        } else if (dataClass == DATA_CLASS.CLASS_U){
            /*Trivial case as in Class_U there cannot be any aligned records*/
            numberOfAlignedRecordSegments = 0;
        } else {
            /*For the general case we know that all records of the template are aligned, but there is an upper bound
            based on the number of record segments*/
            numberOfAlignedRecordSegments = numberOfRecordSegments;
        }


        boolean unpairedRead;
        if (dataClass == DATA_CLASS.CLASS_HM){
            /*Trivial case as we know that the read must be paired*/
            unpairedRead = false;
        } else if (numberOfTemplateSegments == 1){
            /*Trivial case as we know that the cannot be paired*/
            unpairedRead = true;

        } else /*General case where we expect it to be paired, except if explicitely told the contrary*/
            unpairedRead = value_subsequence0 == 5 || value_subsequence0 == 6;

        return new PairStreamFirstSymbol(
            numberOfRecordSegments,
            numberOfAlignedRecordSegments,
            unpairedRead
        );
    }

    public PairStreamSymbol readUnknown(
            int[] numberOfSegmentAlignments,
            long[][] mappingPos,
            SequenceIdentifier sequenceIdentifier,
            boolean unpairedRead,
            int numberOfAlignments,
            int[][] alignPtr,
            long numberOfAlignedRecordSegments,
            long[][] splicedSegLength
    ) throws IOException {
        if(unpairedRead){
            return new PairStreamSymbol(
                    unpairedRead,
                    null,
                    null,
                    null,
                    null,
                    new SplitType[]{SplitType.Unpaired},
                    true
            );
        }


        SequenceIdentifier[] mateSeqId = new SequenceIdentifier[Math.toIntExact(numberOfSegmentAlignments[1])];
        long[][] matePosition = new long[1][1];
        SplitType[] splitMate = new SplitType[numberOfSegmentAlignments[1]];
        long[] mateAuId = new long[1];
        long[] mateRecordIndex = new long[1];

        splitMate[0] = SplitType.SameRecord;
        boolean read_1_first = true;

        if(dataClass == DATA_CLASS.CLASS_HM) {
            read_1_first = (decoders[1].read() & 1) != 0;
            splitMate[0] = SplitType.SameRecord;
        } else {
            for(int templateSegment_i=1; templateSegment_i < numberOfTemplateSegments; templateSegment_i++){
                if(value_subsequence0 == 0){
                    splitMate[0] = SplitType.SameRecord;
                    if( dataClass != DATA_CLASS.CLASS_U){
                        long value_subsequence1 = decoders[1].read();
                        read_1_first = (value_subsequence1 & 0x0001) == 0;
                        delta = value_subsequence1 >> 1;
                        matePosition[0][0] = mappingPos[0][0] + delta;
                        mateSeqId[0] = sequenceIdentifier;
                        segment_flag = 0;
                        same_sequence = true;
                    }else{
                        read_1_first = true;
                        mateAuId[templateSegment_i] = -1;
                        mateRecordIndex[templateSegment_i] = -1;
                    }
                } else if (value_subsequence0 == 1){
                    splitMate[0] = SplitType.DifferentRecord;
                    read_1_first = false;
                    if(dataClass != DATA_CLASS.CLASS_U){
                        matePosition[0][0] = decoders[2].read();
                        mateSeqId[0] = sequenceIdentifier;
                    } else {
                        mateAuId[templateSegment_i] = -1;
                        mateRecordIndex[templateSegment_i] = decoders[2].read();
                    }
                } else if (value_subsequence0 == 2){
                    splitMate[0] = SplitType.DifferentRecord;
                    read_1_first = true;
                    if(dataClass != DATA_CLASS.CLASS_U) {
                        matePosition[0][0] =decoders[3].read();
                        mateSeqId[0] = sequenceIdentifier;
                    }else {
                        mateAuId[templateSegment_i] = -1;
                        mateRecordIndex[templateSegment_i] = decoders[3].read();
                    }
                } else if (value_subsequence0 == 3){
                    splitMate[0] = SplitType.DifferentRecord;
                    read_1_first = false;
                    if(dataClass != DATA_CLASS.CLASS_U){
                        mateSeqId[0] = new SequenceIdentifier(Math.toIntExact(decoders[4].read()));
                        matePosition[0][0] = decoders[6].read();
                    }else{
                        mateAuId[templateSegment_i] = decoders[4].read();
                        mateRecordIndex[templateSegment_i] = decoders[6].read();
                    }
                } else if (value_subsequence0 == 4){
                    splitMate[0] = SplitType.DifferentRecord;
                    read_1_first = true;
                    if(dataClass != DATA_CLASS.CLASS_U){
                        mateSeqId[0] = new SequenceIdentifier(Math.toIntExact(decoders[5].read()));
                        matePosition[0][0] = decoders[7].read();
                    }else{
                        mateAuId[templateSegment_i] = decoders[5].read();
                        mateRecordIndex[templateSegment_i] = decoders[7].read();
                    }
                } else if (value_subsequence0 == 5){
                    splitMate[0] = SplitType.UnmappedOtherRecord;
                    read_1_first = true;
                } else if (value_subsequence0 == 6){
                    splitMate[0] = SplitType.UnmappedOtherRecord;
                    read_1_first = false;
                }
            }
        }


        if(
            (
                dataClass == DATA_CLASS.CLASS_P
                || dataClass == DATA_CLASS.CLASS_N
                || dataClass == DATA_CLASS.CLASS_M
                || dataClass == DATA_CLASS.CLASS_I
            ) && !unpairedRead
        ){
            for(long j = 1; j < numberOfTemplateSegments; j++){
                long currAlignIdx = 0;
                for(int i = 1; i < numberOfAlignments; i++){
                    long alignIdx = alignPtr[i][Math.toIntExact(j)];
                    if(alignIdx > currAlignIdx) {
                        currAlignIdx = alignIdx;
                        value_subsequence0 = decoders[0].read();
                        if(value_subsequence0 == 0){
                            splitMate[Math.toIntExact(alignIdx)] = SplitType.SameRecord;
                            long value_subsequence1 = decoders[1].read();
                            long delta = value_subsequence1 >> 1;
                            if((value_subsequence1 & 0x01) != 0){
                                delta = -delta;
                            }

                            matePosition[Math.toIntExact(alignIdx)][0] =
                                mappingPos[Math.toIntExact(alignPtr[i][0])][0] + delta;
                            mateSeqId[Math.toIntExact(alignIdx)] = seqId;
                        } else if (value_subsequence0 == 2){
                            splitMate[Math.toIntExact(alignIdx)] = SplitType.SameRecord;
                            matePosition[Math.toIntExact(alignIdx)][0] = decoders[3].read();
                            mateSeqId[Math.toIntExact(alignIdx)] = seqId;
                        } else if (value_subsequence0 == 4){
                            splitMate[Math.toIntExact(alignIdx)] = SplitType.DifferentRecord;
                            mateSeqId[Math.toIntExact(alignIdx)] =
                                    new SequenceIdentifier(Math.toIntExact(decoders[5].read())
                            );
                            matePosition[Math.toIntExact(alignIdx)][0] = decoders[7].read();
                        }
                    }
                }
            }
        }

        long[][] splicedSegMappingPos = new long
                [Math.toIntExact(numberOfAlignedRecordSegments)]
                [MAX_NUM_TEMPLATE_SEGMENTS];
        for(long i=0; i < numberOfAlignedRecordSegments; i++){
            splicedSegMappingPos[Math.toIntExact(i)][0] = matePosition[0][0];
        }

        long prevSpliceMappingEnd;
        if(dataClass == DATA_CLASS.CLASS_I || dataClass == DATA_CLASS.CLASS_HM) {
            for(
                    long alignedRecordSegment_i=0;
                    alignedRecordSegment_i < numberOfAlignedRecordSegments;
                    alignedRecordSegment_i++
            ){
                for(
                        long splicedSegment_i = 1;
                        splicedSegment_i < splicedSegLength
                                [Math.toIntExact(alignedRecordSegment_i)].length;
                        splicedSegment_i++
                ){
                    prevSpliceMappingEnd =
                            splicedSegMappingPos
                                    [Math.toIntExact(alignedRecordSegment_i)]
                                    [Math.toIntExact(splicedSegment_i - 1)] +
                                        splicedSegLength
                                                [Math.toIntExact(alignedRecordSegment_i)]
                                                [Math.toIntExact(splicedSegment_i - 1)]
                                    ;
                    value_subsequence0 = decoders[0].read();
                    if(value_subsequence0 == 0){
                        long value_subsequence1 = decoders[1].read();
                        long delta = value_subsequence1 >> 1;
                        if((value_subsequence1 & 1) != 0){
                            delta = -delta;
                        }
                        splicedSegMappingPos
                                [Math.toIntExact(alignedRecordSegment_i)]
                                [Math.toIntExact(splicedSegment_i - 1)] = prevSpliceMappingEnd + delta;
                    } else {
                        splicedSegMappingPos
                            [Math.toIntExact(alignedRecordSegment_i)]
                            [Math.toIntExact(splicedSegment_i - 1)] = decoders[3].read();
                    }
                }
            }
        }


        return new PairStreamSymbol(
                unpairedRead,
                mateSeqId,
                matePosition,
                mateAuId,
                mateRecordIndex,
                splitMate,
                read_1_first
        );
    }


}


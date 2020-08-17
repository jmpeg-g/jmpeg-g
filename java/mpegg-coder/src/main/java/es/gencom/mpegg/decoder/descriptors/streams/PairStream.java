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

import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

public class PairStream {
    private final DescriptorDecoder[] decoders;
    private final DATA_CLASS dataClass;
    private final byte numberOfTemplateSegments;
    private final SequenceIdentifier seqId;

    private long value_subsequence0 = 0;



    public PairStream(
            DATA_CLASS dataClass,
            AccessUnitBlock block,
            byte numberOfTemplateSegments,
            SequenceIdentifier seqId,
            EncodingParameters encodingParameters
    ) {
        this.dataClass = dataClass;
        this.seqId = seqId;
        this.numberOfTemplateSegments = numberOfTemplateSegments;

        DescriptorDecoderConfiguration configuration = encodingParameters.getDecoderConfiguration(
                DESCRIPTOR_ID.PAIR,
                dataClass
        );

        Payload[] sub_streams;
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

    public boolean readFirstAlignment(
            SequenceIdentifier[][] mappingSequenceIdentifiers,
            long[][][] mappingPosition,
            long[] accessUnitRecord,
            long[] recordIndex,
            SplitType[][] splitType
    ) throws IOException {
        boolean read1First = true;

        if(dataClass == DATA_CLASS.CLASS_HM){
            read1First = (decoders[1].read() & 0x0001) == 0;
            splitType[0][0] = SplitType.MappedSameRecord;
            splitType[0][1] = SplitType.UnmappedSameRecord;
        } else {
            if(numberOfTemplateSegments > 2){
                throw new UnsupportedOperationException();
            }
            for(int segment_i = 1; segment_i < numberOfTemplateSegments; segment_i++){
                if(value_subsequence0 == 0){
                    splitType[0][segment_i] = SplitType.MappedSameRecord;
                    if(dataClass != DATA_CLASS.CLASS_U){
                        long value_subsequence1 = decoders[1].read();
                        read1First = (value_subsequence1 & 0x1) == 0;
                        long delta = value_subsequence1 >> 1;
                        mappingPosition[0][segment_i][0] = mappingPosition[0][0][0] + delta;
                        mappingSequenceIdentifiers[0][segment_i] = seqId;
                    } else {
                        read1First = true;
                        accessUnitRecord[segment_i] = -1;
                        recordIndex[segment_i] = -1;
                    }
                } else if (value_subsequence0 == 1){
                    read1First = false;
                    if(dataClass != DATA_CLASS.CLASS_U){
                        mappingPosition[0][segment_i][0] = decoders[2].read();
                        mappingSequenceIdentifiers[0][segment_i] = seqId;
                        splitType[0][segment_i] = SplitType.MappedDifferentRecordSameSequence;
                    } else {
                        accessUnitRecord[segment_i] = -1;
                        recordIndex[segment_i] = decoders[2].read();
                        splitType[0][segment_i] = SplitType.UnmappedDifferentRecordSameAU;
                    }
                } else if (value_subsequence0 == 2){
                    read1First = true;
                    if(dataClass != DATA_CLASS.CLASS_U) {
                        mappingPosition[0][segment_i][0] = decoders[3].read();
                        mappingSequenceIdentifiers[0][segment_i] = seqId;
                        splitType[0][segment_i] = SplitType.MappedDifferentRecordSameSequence;
                    } else {
                        accessUnitRecord[segment_i] = -1;
                        recordIndex[segment_i] = decoders[3].read();
                        splitType[0][segment_i] = SplitType.UnmappedDifferentRecordSameAU;
                    }
                } else if (value_subsequence0 == 3){
                    read1First = false;
                    if(dataClass != DATA_CLASS.CLASS_U){
                        mappingSequenceIdentifiers[0][segment_i] = new SequenceIdentifier((int)decoders[4].read());
                        mappingPosition[0][segment_i][0] = decoders[6].read();
                        splitType[0][segment_i] = SplitType.MappedDifferentRecordDifferentSequence;
                    } else {
                        accessUnitRecord[segment_i] = decoders[4].read();
                        recordIndex[segment_i] = decoders[6].read();
                        splitType[0][segment_i] = SplitType.UnmappedDifferentRecordDifferentAU;
                    }
                } else if (value_subsequence0 == 4){
                    read1First = true;
                    if(dataClass != DATA_CLASS.CLASS_U){
                        mappingSequenceIdentifiers[0][segment_i] = new SequenceIdentifier((int)decoders[5].read());
                        mappingPosition[0][segment_i][0] = decoders[7].read();
                        splitType[0][segment_i] = SplitType.MappedDifferentRecordDifferentSequence;
                    } else {
                        accessUnitRecord[segment_i] = decoders[5].read();
                        recordIndex[segment_i] = decoders[7].read();
                        splitType[0][segment_i] = SplitType.UnmappedDifferentRecordDifferentAU;
                    }
                } else if (value_subsequence0 == 5){
                    splitType[0][segment_i] = SplitType.Unpaired;
                    read1First = true;
                } else if (value_subsequence0 == 6){
                    splitType[0][segment_i] = SplitType.Unpaired;
                    read1First = false;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }

        return read1First;
    }

    public void readMoreAlignments(
            SequenceIdentifier[][] mappingSequenceIdentifiers,
            long[][][] mappingPosition,
            SplitType[][] splitType,
            int[] numberOfSegmentAlignments,
            boolean unpairedRead,
            int numberOfAlignments,
            int[][] alignPtr
    ) throws IOException {
        int alignIdx;
        for(int i=1; i<numberOfSegmentAlignments[0]; i++){
            splitType[i] = new SplitType[numberOfTemplateSegments];
            splitType[i][0] = SplitType.MappedSameRecord;
        }

        if((dataClass == DATA_CLASS.CLASS_P ||
                dataClass == DATA_CLASS.CLASS_N ||
                dataClass == DATA_CLASS.CLASS_M ||
                dataClass == DATA_CLASS.CLASS_I) &&
                !unpairedRead ){
            for(int segment_j=1; segment_j < numberOfTemplateSegments; segment_j++){
                int currAlignIdx = 0;
                for(int alignment_i=1; alignment_i < numberOfAlignments; alignment_i++){
                    alignIdx = alignPtr[alignment_i][segment_j];
                    if(alignIdx > currAlignIdx) {
                        currAlignIdx = alignIdx;
                        long value_subsequence0 = decoders[0].read();
                        if (value_subsequence0 == 0) {
                            splitType[alignIdx][segment_j] = SplitType.MappedSameRecord;
                            long value_subsequence1 = decoders[1].read();
                            long delta = value_subsequence1 >> 1;
                            if ((value_subsequence1 & 0x1) != 0) {
                                delta = -delta;
                            }
                            mappingPosition[alignIdx][segment_j][0] =
                                    mappingPosition[alignPtr[alignment_i][0]][0][0]+ delta;
                            mappingSequenceIdentifiers[alignIdx][segment_j] = seqId;
                        } else if (value_subsequence0 == 2) {
                            splitType[alignIdx][segment_j] = SplitType.MappedSameRecord;
                            mappingPosition[alignIdx][segment_j][0] = decoders[3].read();
                            mappingSequenceIdentifiers[alignIdx][segment_j] = seqId;
                        } else if (value_subsequence0 == 4) {
                            splitType[alignIdx][segment_j] = SplitType.MappedDifferentRecordDifferentSequence;
                            mappingSequenceIdentifiers[alignIdx][segment_j] =
                                    new SequenceIdentifier((int) decoders[5].read());
                            mappingPosition[alignIdx][segment_j][0] = decoders[7].read();
                        }
                    }
                }
            }
        }
    }

    public void readPairSpliced(
            int numberOfAlignedRecordSegments,
            long[][][] mappingPos,
            int[] numberOfSplicesSeg,
            long[][] splicedSegLength
    ) throws IOException {
        long prevSpliceMappingEnd;
        for(int segment_i=0; segment_i < numberOfAlignedRecordSegments; segment_i++){
            mappingPos[0][segment_i] = Arrays.copyOf(mappingPos[0][segment_i], numberOfSplicesSeg[segment_i]);
        }
        if(dataClass == DATA_CLASS.CLASS_I || dataClass == DATA_CLASS.CLASS_HM){
            for(int segment_i=0; segment_i < numberOfAlignedRecordSegments; segment_i++){
                for(int splice_j=1; splice_j < numberOfSplicesSeg[segment_i]; splice_j++){
                    prevSpliceMappingEnd = mappingPos[segment_i][segment_i][splice_j-1] +
                            splicedSegLength[segment_i][splice_j-1];
                    value_subsequence0 = decoders[0].read();
                    if(value_subsequence0 == 0){
                        long value_subsequence1 = decoders[1].read();
                        long delta = value_subsequence1  >> 1;
                        if((value_subsequence1 & 0x1) != 0){
                            delta = -delta;
                        }
                        mappingPos[0][segment_i][splice_j] = prevSpliceMappingEnd + delta;
                    } else if(value_subsequence0 == 2){
                        mappingPos[0][segment_i][splice_j] = decoders[3].read();
                    }
                }
            }
        }
    }


}


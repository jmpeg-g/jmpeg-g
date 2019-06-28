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

package es.gencom.mpegg;

import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.Operation;
import es.gencom.mpegg.format.SequenceIdentifier;

import java.util.Arrays;

public class Record {
    final private long readId;
    final private String readName;
    final private String readGroup;
    final private boolean read1First;
    final private boolean unpaired;

    //first dimension is segment, second is the nucleotide index
    final private byte[][] sequenceBytes;
    //first dimension is segment, second is the nucleotide index, third is the number of qualities
    final private short[][][] qualityValues;

    final private SequenceIdentifier sequenceId;
    //first dimension is alignment, second is splice
    final private long[][] mappingPositionsSegment0;
    //the dimension is the alignment
    final private SplitType[] splitMate;
    //the dimension is the alignment
    final private SequenceIdentifier[] sequenceMappingPositionSegment1;
    //first dimension is alignment, second is splice
    final private long[][] mappingPositionsSegment1;
    //dimensions: segment, alignment, splice; stores the length of the splices
    private final long[][][] lengthSplices;
    //dimensions: segment, alignment, splice, operation index
    final private byte[][][][] operationType;
    //dimensions: segment, alignment, splice, operation index
    final private int[][][][] operationLength;
    //dimensions: segment, alignment, splice, operation index
    final private byte[][][][] originalBase;
    //dimensions: segment, alignment, splice
    final private boolean[][][] reverseCompliment;
    //dimensions: segment, alignment, the number of mapping scores
    final private long[][][] mapping_score;
    //dimensions: alignment, numSegments. Contains all alignmentPairs
    final private int[][] alignPtr;
    //dimensions: alignment segment 0, alignment segment 1
    final private int[][] alignmentSegment0ToAlignmentSegment1;


    public Record(
            long readId,
            String readName,
            String readGroup,
            boolean read1First,
            boolean unpaired,
            byte[][] sequenceBytes,
            short[][][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment0,
            SplitType[] splitMate,
            SequenceIdentifier[] sequenceMappingPositionSegment1,
            long[][] mappingPositionsSegment1,
            long[][][] lengthSplices,
            byte[][][][] operationType,
            int[][][][] operationLength,
            byte[][][][] originalBase,
            boolean[][][] reverseCompliment,
            long[][][] mapping_score,
            int[][] alignPtr) {
        this.readId = readId;
        this.readName = readName;
        this.readGroup = readGroup;
        this.read1First = read1First;
        this.unpaired = unpaired;
        this.sequenceBytes = sequenceBytes;
        this.qualityValues = qualityValues;
        this.sequenceId = sequenceId;
        this.mappingPositionsSegment0 = mappingPositionsSegment0;
        this.splitMate = splitMate;
        this.sequenceMappingPositionSegment1 = sequenceMappingPositionSegment1;
        this.mappingPositionsSegment1 = mappingPositionsSegment1;
        this.lengthSplices = lengthSplices;
        this.operationType = operationType;
        this.operationLength = operationLength;
        this.originalBase = originalBase;
        this.reverseCompliment = reverseCompliment;
        this.mapping_score = mapping_score;
        this.alignPtr = alignPtr;
        if(alignPtr == null){
            if(!unpaired){
                throw new IllegalArgumentException();
            }
        }

        for(int segment_i=0; segment_i<operationType.length; segment_i++){
            if(operationType[segment_i] == null){
                operationType[segment_i] = new byte[0][][];
            }
            if(operationLength[segment_i] == null){
                operationType[segment_i] = new byte[0][][];
            }
        }

        if(mappingPositionsSegment0[0].length != lengthSplices[0].length){
            throw new IllegalArgumentException();
        }
        if(splitMate[0]==SplitType.SameRecord && mappingPositionsSegment1[0].length != lengthSplices[1].length){
            throw new IllegalArgumentException();
        }

        if(alignPtr != null && splitMate[0] != SplitType.Unpaired) {
            int[][] tmpAlignmentSegment0ToAlignmentSegment1 = new int[mappingPositionsSegment0.length][128];

            for (
                    int alignmentSegment0_i = 0;
                    alignmentSegment0_i < mappingPositionsSegment0.length;
                    alignmentSegment0_i++
            ) {
                int[] alignmentsSegment1;
                if (mappingPositionsSegment1 == null) {
                    alignmentsSegment1 = new int[0];
                } else {
                    alignmentsSegment1 = new int[mappingPositionsSegment1.length];
                }
                tmpAlignmentSegment0ToAlignmentSegment1[alignmentSegment0_i] = alignmentsSegment1;
            }
            int[] numberAlignmentsSegment0toAlignmentSegment1 = new int[mappingPositionsSegment0.length];

            for (int alignPair_i = 0; alignPair_i < alignPtr.length; alignPair_i++) {
                int alignmentSegment0 = alignPtr[alignPair_i][0];
                int alignmentSegment1 = alignPtr[alignPair_i][1];
                tmpAlignmentSegment0ToAlignmentSegment1
                        [alignmentSegment0]
                        [numberAlignmentsSegment0toAlignmentSegment1[alignmentSegment0]]
                        = alignmentSegment1;
                numberAlignmentsSegment0toAlignmentSegment1[alignmentSegment0]++;
            }

            for (
                    int alignmentSegment0_i = 0;
                    alignmentSegment0_i < mappingPositionsSegment0.length;
                    alignmentSegment0_i++
            ) {
                tmpAlignmentSegment0ToAlignmentSegment1[alignmentSegment0_i] = Arrays.copyOf(
                        tmpAlignmentSegment0ToAlignmentSegment1[alignmentSegment0_i],
                        numberAlignmentsSegment0toAlignmentSegment1[alignmentSegment0_i]
                );
            }
            alignmentSegment0ToAlignmentSegment1 = tmpAlignmentSegment0ToAlignmentSegment1;
        }else{
            alignmentSegment0ToAlignmentSegment1 = new int[0][];
        }
    }

    public byte getRecordSegments(){
        if(sequenceBytes.length == 1 || sequenceBytes[1]==null){
            return 1;
        }else {
            return 2;
        }
    }

    public byte getAlignedSegments() {
        byte result = 0;
        if(mappingPositionsSegment0.length != 0){
            result++;
        }
        if(mappingPositionsSegment1 != null && isTwoSegmentsStoredTogether()){
            if(mappingPositionsSegment1[0].length !=0 ){
                result++;
            }
        }
        return result;
    }

    public boolean isRead1First() {
        return read1First;
    }

    public long getAlignmentsSegment0() {
        return mappingPositionsSegment0.length;
    }

    public boolean isUnpaired() {
        return unpaired;
    }

    public boolean isTwoSegmentsStoredTogether(){
        return splitMate[0]==SplitType.SameRecord;
    }

    public boolean isFirstAlignmentSegment1SameSequence() {
        return sequenceMappingPositionSegment1[
            alignPtr[0][0]
        ] == sequenceId;
    }

    public boolean isSegment1Unmapped() {
        if(isUnpaired()){
            throw new IllegalArgumentException();
        }
        return sequenceMappingPositionSegment1.length == 0;
    }

    public SequenceIdentifier getSequenceIdSegment1FirstAlignment(){
        return sequenceMappingPositionSegment1[
                alignPtr[0][1]
            ];
    }

    public long getMappingPosSegment0FirstAlignment(){
        return mappingPositionsSegment0[0][0];
    }

    public long getMappingPosSegment1FirstAlignment(){
        return mappingPositionsSegment1[
                alignPtr[0][0]
        ][0];
    }

    public SequenceIdentifier[] getSequenceIdSegment1() {
        return sequenceMappingPositionSegment1;
    }

    public long[][] getMappingPositionsSegment0() {
        return mappingPositionsSegment0;
    }

    public long[][] getMappingPositionsSegment1() {
        return mappingPositionsSegment1;
    }

    public int getAlignmentIndex(int alignmentSegment1_i, int segment_i) {
        return alignPtr[alignmentSegment1_i][segment_i];
    }

    public SequenceIdentifier getSequenceId() {
        return sequenceId;
    }

    public long[][][] getSpliceLengths(){
        return lengthSplices;
    }

    public int[] getAlignmentIndexesSegment1(int segment0alignment_i) {
        return alignmentSegment0ToAlignmentSegment1[segment0alignment_i];
    }

    public byte[][][] getSoftclip(){
        byte[][][] softclips = new byte[getAlignedSegments()][2][];
        for(byte alignedSegment_i=0; alignedSegment_i < getAlignedSegments(); alignedSegment_i++){
            if(operationType[alignedSegment_i] == null || operationType[alignedSegment_i].length == 0){
                softclips[alignedSegment_i][0] = new byte[0];
                softclips[alignedSegment_i][1] = new byte[0];
                continue;
            }
            byte[] operationsFirstSplice = operationType[alignedSegment_i][0][0];
            int softClipStartPosition = -1;
            if(operationsFirstSplice[0] == Operation.HardClip) {
                if(operationsFirstSplice[1] == Operation.SoftClip) {
                    softClipStartPosition = 1;
                }
            } else if(operationsFirstSplice[0] == Operation.SoftClip) {
                softClipStartPosition = 0;
            }

            if(softClipStartPosition != -1) {
                softclips[alignedSegment_i][0] = Arrays.copyOfRange(
                        sequenceBytes[alignedSegment_i],
                        0,
                        operationLength[alignedSegment_i][0][0][softClipStartPosition]);
            } else {
                softclips[alignedSegment_i][0] = new byte[]{};
            }

            byte[] operationsLastSplice = operationType
                    [alignedSegment_i]
                    [operationType[alignedSegment_i].length - 1]
                    [operationType[alignedSegment_i][operationType[alignedSegment_i].length - 1].length - 1];
            int softClipEndPosition = -1;
            if(operationsLastSplice[operationsLastSplice.length - 1] == Operation.HardClip) {
                if(operationsLastSplice[operationsLastSplice.length - 2] == Operation.SoftClip) {
                    softClipEndPosition = operationsLastSplice.length - 2;
                }
            } else if(operationsLastSplice[operationsLastSplice.length - 1] == Operation.SoftClip) {
                softClipEndPosition = operationsLastSplice.length - 1;
            }

            if(softClipEndPosition != -1){
                int to = sequenceBytes[alignedSegment_i].length;
                int from = to - operationLength
                        [alignedSegment_i][operationType[alignedSegment_i].length-1]
                        [operationType[alignedSegment_i]
                        [operationType[alignedSegment_i].length-1].length-1]
                        [softClipEndPosition];
                softclips[alignedSegment_i][1] = Arrays.copyOfRange(
                        sequenceBytes[alignedSegment_i],
                        from,
                        to
                );
            }else{
                softclips[alignedSegment_i][1] = new byte[]{};
            }
        }

        return softclips;
    }

    public int[][] getHardclip(){
        int[][] hardclips = new int[getAlignedSegments()][2];
        for(byte alignedSegment_i=0; alignedSegment_i < getAlignedSegments(); alignedSegment_i++){
            if(operationType[alignedSegment_i]==null || operationType[alignedSegment_i].length == 0){
                hardclips[alignedSegment_i][0] = 0;
                hardclips[alignedSegment_i][1] = 0;
                continue;
            }
            byte[] operationsFirstSplice = operationType[alignedSegment_i][0][0];
            if(operationsFirstSplice[0] == Operation.HardClip) {
                hardclips[alignedSegment_i][0] = operationLength[alignedSegment_i][0][0][0];
            } else {
                hardclips[alignedSegment_i][0] = 0;
            }

            byte[] operationsLastSplice = operationType
                    [alignedSegment_i]
                    [operationType[alignedSegment_i].length-1]
                    [operationType[alignedSegment_i][operationType[alignedSegment_i].length-1].length-1];
            if(operationsLastSplice[operationsLastSplice.length-1] == Operation.HardClip) {
                hardclips[alignedSegment_i][1] =
                        operationLength
                                [alignedSegment_i]
                                [operationType[alignedSegment_i].length-1]
                                [operationType[alignedSegment_i][operationType[alignedSegment_i].length-1].length-1]
                                [operationsLastSplice.length-1];
            } else {
                hardclips[alignedSegment_i][1] = 0;
            }
        }

        return hardclips;
    }

    public boolean[][][] getReverseCompliment() {
        return reverseCompliment;
    }

    public byte[][][][] getOperationType() {
        return operationType;
    }

    public int[][][][] getOperationLength() {
        return operationLength;
    }

    public byte[][] getSequenceBytes() {
        return sequenceBytes;
    }

    public byte[][][][] getOriginalBase() {
        return originalBase;
    }

    public int[][] getAlignPtr() {
        return alignPtr;
    }

    public SplitType[] getSplitMate() {
        return splitMate;
    }

    public String getReadName() {
        return readName;
    }

    public short[][][] getQualityValues() {
        return qualityValues;
    }
}

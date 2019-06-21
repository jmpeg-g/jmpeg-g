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

package es.gencom.mpegg.decoder;

import es.gencom.mpegg.coder.quality.AbstractQualityValueParameterSet;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.Operation;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.tokens.TokensStructureDecoder;
import es.gencom.mpegg.decoder.descriptors.S_alphabets;
import es.gencom.mpegg.decoder.descriptors.streams.*;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

public class MappedAccessUnitDecoder extends AbstractAccessUnitDecoder {
    private static long auCreationCount = 0;

    private final long auId;


    final private long au_id;
    private final RlenStream rlenStream;
    private final GenomicPosition initialPosition;
    private GenomicPosition currentPosition;

    final protected AbstractSequencesSource sequencesSource;

    final private PosStream posStream;
    final private PairStream pairStream;
    final private MMapStream mMapStream;
    final private RCompStream rCompStream;
    final private FlagsStream flagsStream;
    final private MMposStream mPosStream;
    final private MMTypeStream mmTypeStream;
    final private ClipsStream clipsStream;
    final private QualityStream qualityStream;
    final private TokensStructureDecoder readIdentifierDecoder;

    final private byte[] changedNucleotides = new byte[]{};
    final private long[] changedPositions = new long[]{};

    final private ALPHABET_ID alphabet_id;


    private byte[] originalBases;
    private int encodedOriginalBases;

    private long readCount;


    public MappedAccessUnitDecoder(
            long au_id,
            GenomicPosition genomicPosition,
            PosStream posStream,
            PairStream pairStream,
            MMapStream mMapStream,
            RCompStream rCompStream,
            FlagsStream flagStream,
            RlenStream rlenStream,
            MMposStream mPosStream,
            MMTypeStream mmTypeStream,
            ClipsStream clipsStream,
            QualityStream qualityStream,
            AbstractSequencesSource sequencesSource,
            short[][][] tokensReadIdentifiers,
            ALPHABET_ID alphabet_id,
            AbstractQualityValueParameterSet qualityValueParameterSet
    ) {
        this.sequencesSource = sequencesSource;
        this.au_id = au_id;
        initialPosition = genomicPosition;
        currentPosition = genomicPosition;
        this.posStream = posStream;
        this.pairStream = pairStream;
        this.mMapStream = mMapStream;
        this.rCompStream = rCompStream;
        this.flagsStream = flagStream;
        this.rlenStream = rlenStream;
        this.mPosStream = mPosStream;
        this.mmTypeStream = mmTypeStream;
        this.clipsStream = clipsStream;
        this.qualityStream = qualityStream;
        readCount = 0;
        this.alphabet_id = alphabet_id;

        if(tokensReadIdentifiers == null){
            readIdentifierDecoder = null;
        } else {
            readIdentifierDecoder = new TokensStructureDecoder(tokensReadIdentifiers);
        }

        auId = auCreationCount;
        auCreationCount++;
    }

    @Override
    public boolean hasNext() throws IOException {
        return posStream.hasNext();
    }

    public Record getRecord() throws IOException {
        String readName = "";
        if(readIdentifierDecoder != null){
            readName = readIdentifierDecoder.getString();
        }

        PairStreamFirstSymbol pairStreamFirstSymbol = pairStream.readFirst();
        clipsStream.read(
                readCount,
                pairStreamFirstSymbol.getNumberOfRecordSegments(),
                pairStreamFirstSymbol.getNumberOfAlignedRecordSegments()
        );
        RlenStreamSymbol rlenStreamSymbol = rlenStream.read(
                pairStreamFirstSymbol.getNumberOfRecordSegments(),
                pairStreamFirstSymbol.getNumberOfAlignedRecordSegments(),
                clipsStream.getHard_clips()
        );
        MMapStreamSymbol mMapStreamSymbol = mMapStream.readSymbol(
                pairStreamFirstSymbol.isUnpairedRead(),
                pairStreamFirstSymbol.getNumberOfRecordSegments()
        );
        long[][] mappingPos = posStream.read(mMapStreamSymbol.getNumberOfSegmentAlignments());
        PairStreamSymbol pairStreamSymbol = pairStream.readUnknown(
                mMapStreamSymbol.getNumberOfSegmentAlignments(),
                mappingPos,
                initialPosition.getSequenceId(),
                pairStreamFirstSymbol.isUnpairedRead(),
                mMapStreamSymbol.getNumberOfAlignments(),
                mMapStreamSymbol.getAlignPtr(),
                pairStreamFirstSymbol.getNumberOfAlignedRecordSegments(),
                rlenStreamSymbol.getSplicedSegLength()
        );
        long[][][] resizedSplicedSegLength = new long[rlenStreamSymbol.getSplicedSegLength().length][1][];
        for(
                int segment_i = 0;
                segment_i < resizedSplicedSegLength.length;
                segment_i++
        ){
            resizedSplicedSegLength[segment_i][0] = rlenStreamSymbol.getSplicedSegLength()[segment_i];
        }
        boolean[][][] rCompSymbols = rCompStream.read(
                mMapStreamSymbol.getNumberOfSegmentAlignments(),
                resizedSplicedSegLength
        );
        int[][] mmOffsets = mPosStream.read(pairStreamFirstSymbol.getNumberOfAlignedRecordSegments());
        int[][] mmTypes = mmTypeStream.readMMType(mmOffsets);
        correctMmOffsetsByType(mmOffsets, mmTypes);
        int[][][] mmOffsetsPerSlice = correctMmOffsetsBySplices(mmOffsets, rlenStreamSymbol.getSplicedSegLength());
        int[][][] mmTypesPerSplice = correctMMTypesPerSlice(mmTypes, mmOffsetsPerSlice);


        SegmentsDecodingResult segmentsDecodingResult =  decode_aligned_segments(
                rlenStreamSymbol.getSplicedSegLength(),
                mmTypesPerSplice,
                mmOffsetsPerSlice,
                mmTypeStream,
                clipsStream.getSoft_clips(),
                combineMappingPos(mappingPos, pairStreamSymbol.getMateMappingPos()),
                initialPosition.getSequenceId(),
                sequencesSource,
                changedNucleotides,
                changedPositions,
                alphabet_id
        );

        short[][][] qualities = new short[2][1][];
        qualities[0][0] = qualityStream.getQualitiesAligned(
                segmentsDecodingResult.getOperations()[0][0],
                segmentsDecodingResult.getOperationLength()[0][0],
                mappingPos[0],
                initialPosition.getPosition()
        );
        if(pairStreamFirstSymbol.getNumberOfRecordSegments() == 2){
            qualities[1][0] = qualityStream.getQualitiesAligned(
                    segmentsDecodingResult.getOperations()[1][0],
                    segmentsDecodingResult.getOperationLength()[1][0],
                    pairStreamSymbol.getMateMappingPos()[0],
                    initialPosition.getPosition()
            );
        }

        Record result = new Record(
                readCount,
                readName,
                "",
                pairStreamSymbol.isRead_1_first(),
                pairStreamFirstSymbol.isUnpairedRead(),
                segmentsDecodingResult.getDecode_sequences(),
                qualities,
                initialPosition.getSequenceId(),
                mappingPos,
                pairStreamSymbol.getSplitMate(),
                pairStreamSymbol.getMateSeqId(),
                pairStreamSymbol.getMateMappingPos(),
                resizedSplicedSegLength,
                segmentsDecodingResult.getOperations(),
                segmentsDecodingResult.getOperationLength(),
                segmentsDecodingResult.getOriginal_nucleotides(),
                rCompSymbols,
                null,
                mMapStreamSymbol.getAlignPtr()
        );
        readCount++;
        return result;
    }

    private static long[][][] combineMappingPos(long[][] mappingPos, long[][] mateMappingPos) {
        return new long[][][]{
                mappingPos,
                mateMappingPos
        };
    }


    static int[][][] correctMMTypesPerSlice(int[][] mmTypes, int[][][] mmOffsetsPerSlice) {
        int[][][] result = new int[mmTypes.length][][];
        for(int segment_i=0; segment_i < result.length; segment_i++){
            result[segment_i] = new int[mmOffsetsPerSlice[segment_i].length][];
            int mm_operation_i = 0;
            for(int splice_i = 0; splice_i < mmOffsetsPerSlice[segment_i].length; splice_i++){
                result[segment_i][splice_i] = new int[mmOffsetsPerSlice[segment_i][splice_i].length];
                for(
                    int mm_operation_slice_i = 0;
                    mm_operation_slice_i < mmOffsetsPerSlice[segment_i][splice_i].length;
                    mm_operation_slice_i++
                ){
                    result[segment_i][splice_i][mm_operation_slice_i] = mmTypes[segment_i][mm_operation_i];
                    mm_operation_i++;
                }
            }
        }
        return result;
    }

    static SegmentsDecodingResult decode_aligned_segments(
            long[][] splicedSegLength,
            int[][][] mmType,
            int[][][] mmOffsets,
            MMTypeStreamInterface mmTypeStream,
            byte[][][] softClip,
            long[][][] mappingPos,
            SequenceIdentifier sequenceIdentifier,
            AbstractSequencesSource sequencesSource,
            byte[] changedNucleotides,
            long[] changedPositions,
            ALPHABET_ID alphabet_id
    ) throws IOException {
        int numberOfAlignedRecordSegments = mmType.length;
        Operation[][][][] operations = new Operation[numberOfAlignedRecordSegments][1][][];
        int[][][][] operationLength = new int[numberOfAlignedRecordSegments][1][][];
        byte[][][] decode_sequences = new byte[numberOfAlignedRecordSegments][][];
        byte[][][][] original_nucleotides = new byte[numberOfAlignedRecordSegments][1][][];
        int[][][] length_original_nucleotides = new int[numberOfAlignedRecordSegments][1][];
        for(
                short alignedRecordSegment_i = 0;
                alignedRecordSegment_i < numberOfAlignedRecordSegments;
                alignedRecordSegment_i++
        ){
            decode_sequences[alignedRecordSegment_i] = decode_aligned_segment(
                splicedSegLength[alignedRecordSegment_i],
                mmType[alignedRecordSegment_i],
                mmOffsets[alignedRecordSegment_i],
                mmTypeStream,
                softClip[alignedRecordSegment_i],
                mappingPos[alignedRecordSegment_i],
                sequenceIdentifier,
                sequencesSource,
                changedNucleotides,
                changedPositions,
                alphabet_id,
                operations[alignedRecordSegment_i],
                operationLength[alignedRecordSegment_i],
                original_nucleotides[alignedRecordSegment_i],
                length_original_nucleotides[alignedRecordSegment_i]
            );
        }
        return new SegmentsDecodingResult(decode_sequences, operations, operationLength, original_nucleotides);
    }

    static byte[][] decode_aligned_segment(
            long[] splicedSegLength,
            int[][] mmType,
            int[][] mmOffsets,
            MMTypeStreamInterface mmTypeStream,
            byte[][] softClip,
            long[][] mappingPos,
            SequenceIdentifier sequenceIdentifier,
            AbstractSequencesSource sequencesSource,
            byte[] changedNucleotides,
            long[] changedPositions,
            ALPHABET_ID alphabet_id,
            Operation[][][] operations,
            int[][][] operationLength,
            byte[][][] original_nucleotides,
            int[][] length_original_nucleotides
    ) throws IOException {
        int numberOfSplices = splicedSegLength.length;

        operations[0] = new Operation[numberOfSplices][];
        operationLength[0] = new int[numberOfSplices][];
        byte[][] decode_sequences = new byte[numberOfSplices][];
        original_nucleotides[0] = new byte[numberOfSplices][32];
        length_original_nucleotides[0] = new int[numberOfSplices];

        for(
                int splice_i=0;
                splice_i < numberOfSplices;
                splice_i++
        ){

            long position = mappingPos[0][splice_i];
            long mappedLength = splicedSegLength[splice_i];

            operations[0][splice_i] = new Operation[128];
            operationLength[0][splice_i] = new int[128];
            int numberOperations = 0;
            decode_sequences[splice_i] = new byte[Math.toIntExact(mappedLength)];
            int decodedPositions = 0;

            if(splice_i == 0) {
                mappedLength -= softClip[0].length;
            }
            if (splice_i == numberOfSplices - 1){
                mappedLength -= softClip[1].length;
            }

            long sizeToRequest = mappedLength;

            for(
                    int operation_i = 0;
                    operation_i < mmType[splice_i].length;
                    operation_i++
            ){
                if (mmType[splice_i][operation_i] == 2){
                    sizeToRequest++;
                }
            }

            byte[] base_decode = getNucleotidesSequence(
                    position,
                    sizeToRequest,
                    sequencesSource.getSubsequenceBytes(
                            sequenceIdentifier,
                            Math.toIntExact(position),
                            Math.toIntExact(position + sizeToRequest)
                    ),
                    changedNucleotides,
                    changedPositions
            );


            int previousOffset = 0;
            for(
                    int operation_i = 0;
                    operation_i < mmType[splice_i].length;
                    operation_i++
            ){
                int offset = mmOffsets[splice_i][operation_i];

                int delta = offset - previousOffset;
                previousOffset = offset;
                if(delta > 0){
                    numberOperations = addOperationAutomatic(
                            operations,
                            operationLength,
                            splice_i,
                            Operation.Match,
                            numberOperations,
                            delta
                    );
                }

                if(mmType[splice_i][operation_i] == 0){
                    byte newBase = mmTypeStream.readNewMismatchBase(
                            S_alphabets.alphabets[alphabet_id.ID],
                            base_decode[offset]
                    );
                    byte originalBase = base_decode[offset];
                    base_decode[offset] = newBase;
                    numberOperations = addOperationAutomatic(
                            operations,
                            operationLength,
                            splice_i,
                            Operation.Substitution,
                            numberOperations,
                            1
                    );
                    addOriginalBaseAutomatic(
                            original_nucleotides,
                            length_original_nucleotides[0],
                            splice_i,
                            originalBase
                    );
                    previousOffset++;
                }else if(mmType[splice_i][operation_i] == 1){
                    byte newBase = mmTypeStream.readNewInsertBase(S_alphabets.alphabets[alphabet_id.ID]);
                    insertAtPosition(base_decode, newBase, offset);
                    numberOperations = addOperationAutomatic(
                            operations,
                            operationLength,
                            splice_i,
                            Operation.Insert,
                            numberOperations,
                            1
                    );
                    previousOffset++;
                }else if(mmType[splice_i][operation_i] == 2){
                    byte originalBase = base_decode[offset];
                    base_decode = deleteAtPosition(base_decode, offset);
                    numberOperations = addOperationAutomatic(
                            operations,
                            operationLength,
                            splice_i,
                            Operation.Delete,
                            numberOperations,
                            1
                    );
                    addOriginalBaseAutomatic(
                            original_nucleotides,
                            length_original_nucleotides[0],
                            splice_i,
                            originalBase
                    );
                }
            }

            if(mappedLength != previousOffset) {
                numberOperations = addOperationAutomatic(
                        operations,
                        operationLength,
                        splice_i,
                        Operation.Match,
                        numberOperations,
                        Math.toIntExact(mappedLength - previousOffset)
                );
            }

            base_decode = Arrays.copyOf(base_decode, (int) mappedLength);


            if (splice_i == 0){
                if(softClip[0].length != 0) {
                    base_decode = prependToArrays(
                            base_decode,
                            softClip[0]
                    );
                    numberOperations = prependOperation(
                            operations,
                            operationLength,
                            splice_i,
                            Operation.SoftClip,
                            numberOperations,
                            softClip[0].length
                    );
                }
            }
            if (splice_i == numberOfSplices - 1){
                if(softClip[1].length != 0) {
                    base_decode = appendToArrays(
                            base_decode,
                            softClip[1]
                    );
                    numberOperations = addOperationAutomatic(
                            operations,
                            operationLength,
                            splice_i,
                            Operation.SoftClip,
                            numberOperations,
                            softClip[1].length
                    );
                }
            }

            original_nucleotides[0][splice_i] = Arrays.copyOf(
                    original_nucleotides[0][splice_i],
                    length_original_nucleotides[0][splice_i]
            );
            decode_sequences[splice_i] = base_decode;

            operations[0][splice_i] = Arrays.copyOf(operations[0][splice_i], numberOperations);
            operationLength[0][splice_i] = Arrays.copyOf(operationLength[0][splice_i], numberOperations);
        }
        return decode_sequences;
    }

    private static void addOriginalBaseAutomatic(
            byte[][][] original_nucleotides,
            int[] length_original_nucleotides,
            int splice_i,
            byte originalBase
    ) {
        original_nucleotides[0][splice_i]
                [length_original_nucleotides[splice_i]] = originalBase;
        length_original_nucleotides[splice_i]++;
        if(
                original_nucleotides[0][splice_i].length
                        == length_original_nucleotides[splice_i]
        ){
            original_nucleotides[0][splice_i] = Arrays.copyOf(
                    original_nucleotides[0][splice_i],
                    original_nucleotides[0][splice_i].length*2
            );
        }

    }

    private static int prependOperation(
            Operation[][][] operations,
            int[][][] operationLength,
            int splice_i,
            Operation operation,
            int numberOperations,
            int length
    ) {
        System.arraycopy(
                operations[0][splice_i],
                0,
                operations[0][splice_i],
                1,
                operations[0][splice_i].length-1
        );
        System.arraycopy(
                operationLength[0][splice_i],
                0,
                operationLength[0][splice_i],
                1,
                operationLength[0][splice_i].length-1
        );
        operations[0][splice_i][0] = operation;
        operationLength[0][splice_i][0] = length;
        return resizeOperationsArrays(operations, operationLength, splice_i, numberOperations);
    }

    private static int resizeOperationsArrays(
            Operation[][][] operations,
            int[][][] operationLength,
            int splice_i,
            int numberOperations
    ) {
        numberOperations++;
        if(operations[0][splice_i].length == numberOperations){
            operations[0][splice_i] = Arrays.copyOf(operations[0][splice_i], operations[0][splice_i].length*2);
            operationLength[0][splice_i] = Arrays.copyOf(
                    operationLength[0][splice_i], operationLength[0][splice_i].length*2
            );
        }
        return numberOperations;
    }

    private static int addOperationAutomatic(
            Operation[][][] operations,
            int[][][] operationLength,
            int splice_i,
            Operation operation,
            int numberOperations,
            int length
    ) {
        operations[0][splice_i][numberOperations] = operation;
        operationLength[0][splice_i][numberOperations] = length;
        return resizeOperationsArrays(operations, operationLength, splice_i, numberOperations);
    }



    /**
     *
     * @param position 0-based initial position of the sequence to retrieve
     * @param length length of the sequence to retrieve
     * @param nucleotideSequence the base sequence from which to retrieve a subsequence
     * @param changedNucleotides to which nucleotides are the reference sequences changed to
     * @param changedPositions positions at which the sequence is changed
     * @return returns a subsequence of the original sequence passed as parameter, taking into account the parametrized
     * changes
     */
    static byte[] getNucleotidesSequence(
        long position,
        long length,
        Payload nucleotideSequence,
        byte[] changedNucleotides,
        long[] changedPositions
    ) throws IOException {
        int positionCasted = Math.toIntExact(position);
        int lengthCasted = Math.toIntExact(length);
        byte[] result = new byte[lengthCasted];


        int changedPos = Arrays.binarySearch(changedPositions, position);
        if(changedPos < 0){
            changedPos = ~changedPos;
        }
        if(changedPos >= changedNucleotides.length){
            changedPos = -1;
        }

        for(int i=0; i<length; i++){
            if(changedPos >= 0 && changedPositions[changedPos] == (i+position)){
                result[i] = changedNucleotides[changedPos];
                changedPos++;
                if(changedPos >= changedNucleotides.length){
                    changedPos = -1;
                }
                nucleotideSequence.readByte();
            }else {
                result[i] = nucleotideSequence.readByte();
            }
        }
        return result;
    }

    static void correctMmOffsetsByType(int[][] mmOffsets, int[][] mmTypes){
        for(
                int alignedRecordSegment_i=0;
                alignedRecordSegment_i < mmOffsets.length;
                alignedRecordSegment_i++
        ){
            int numDeletions = 0;
            for(
                    int mismatch_i=0;
                    mismatch_i < mmTypes[alignedRecordSegment_i].length;
                    mismatch_i++
            ){
                mmOffsets[alignedRecordSegment_i][mismatch_i] -= numDeletions;
                if(mmTypes[alignedRecordSegment_i][mismatch_i] == 2){
                    numDeletions += 1;
                }
            }
        }
    }

    static int[][][] correctMmOffsetsBySplices(
            int[][] mmOffsets,
            long[][] splicedReadsLength
    ){
        int numAlignedRecordSegments = mmOffsets.length;

        int[][] splicedSegmentNumOffset = new int[numAlignedRecordSegments][];
        int[][][] splicedSegmentMismatchOffset = new int[numAlignedRecordSegments][][];
        for(int alignedRecordSegment_i = 0; alignedRecordSegment_i < numAlignedRecordSegments; alignedRecordSegment_i++){
            int numSplices = splicedReadsLength[alignedRecordSegment_i].length;

            splicedSegmentNumOffset[alignedRecordSegment_i] = new int[numSplices];
            splicedSegmentMismatchOffset[alignedRecordSegment_i] =
                    new int[splicedReadsLength[alignedRecordSegment_i].length][32];
        }

        for(int alignedRecordSegment_i = 0; alignedRecordSegment_i < numAlignedRecordSegments; alignedRecordSegment_i++){
            int spliceSegmentIndex = 0;
            int splicedSegmentStartOffset = 0;
            int splicedSegmentEndOffset = Math.toIntExact(
                    splicedSegmentStartOffset + splicedReadsLength[alignedRecordSegment_i][0]
            );

            for(int mismatch_i=0; mismatch_i < mmOffsets[alignedRecordSegment_i].length; mismatch_i++){
                while(mmOffsets[alignedRecordSegment_i][mismatch_i] >= splicedSegmentEndOffset){
                    spliceSegmentIndex++;
                    splicedSegmentStartOffset = splicedSegmentEndOffset;
                    splicedSegmentEndOffset = Math.toIntExact(
                            splicedSegmentStartOffset + splicedReadsLength[alignedRecordSegment_i][spliceSegmentIndex]
                    );
                }


                splicedSegmentMismatchOffset
                        [alignedRecordSegment_i]
                        [spliceSegmentIndex]
                        [
                            splicedSegmentNumOffset
                                [alignedRecordSegment_i]
                                [spliceSegmentIndex]
                        ] = mmOffsets[alignedRecordSegment_i][mismatch_i] - splicedSegmentStartOffset;
                splicedSegmentNumOffset
                        [alignedRecordSegment_i]
                        [spliceSegmentIndex]++;
                if(
                    splicedSegmentNumOffset
                        [alignedRecordSegment_i]
                        [spliceSegmentIndex]
                    == splicedSegmentMismatchOffset
                            [alignedRecordSegment_i]
                            [spliceSegmentIndex].length
                ){
                    splicedSegmentMismatchOffset
                            [alignedRecordSegment_i]
                            [spliceSegmentIndex]
                        = Arrays.copyOf(
                            splicedSegmentMismatchOffset
                                    [alignedRecordSegment_i]
                                    [spliceSegmentIndex],
                            splicedSegmentMismatchOffset
                                    [alignedRecordSegment_i]
                                    [spliceSegmentIndex].length * 2
                        );
                }
            }
        }
        for(int alignedRecordSegment_i = 0; alignedRecordSegment_i < numAlignedRecordSegments; alignedRecordSegment_i++) {
            int numSplices = splicedReadsLength[alignedRecordSegment_i].length;

            for(int splice_i = 0; splice_i < numSplices; splice_i++){
                splicedSegmentMismatchOffset[alignedRecordSegment_i][splice_i] = Arrays.copyOf(
                        splicedSegmentMismatchOffset[alignedRecordSegment_i][splice_i],
                        splicedSegmentNumOffset[alignedRecordSegment_i][splice_i]
                );
            }
        }
        return splicedSegmentMismatchOffset;
    }

    static void insertAtPosition(
            byte[] array,
            byte newValue,
            int position
    ){
        System.arraycopy(array, position, array, position+1, array.length - position - 1);
        array[position] = newValue;
    }

    static byte[] deleteAtPosition(
            byte[] array,
            int position
    ){
        System.arraycopy(array, position+1, array, position, array.length - position - 1);
        return Arrays.copyOf(array, array.length-1);
    }

    static byte[] prependToArrays(
            byte[] array,
            byte[] arrayToPrepend
    ){
        byte[] newArray = new byte[array.length + arrayToPrepend.length];
        System.arraycopy(arrayToPrepend, 0, newArray, 0, arrayToPrepend.length);
        System.arraycopy(array, 0, newArray, arrayToPrepend.length, array.length);
        return newArray;
    }

    static byte[] appendToArrays(
            byte[] array,
            byte[] arrayToAppend
    ){
        byte[] newArray = new byte[array.length + arrayToAppend.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        System.arraycopy(arrayToAppend, 0, newArray, array.length, arrayToAppend.length);
        return newArray;
    }
}

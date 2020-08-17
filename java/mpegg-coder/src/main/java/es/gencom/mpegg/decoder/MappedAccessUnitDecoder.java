/*
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

import es.gencom.mpegg.Record;
import es.gencom.mpegg.ReverseCompType;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.encoder.Operation;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.tokens.TokensStructureDecoder;
import es.gencom.mpegg.decoder.descriptors.streams.*;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

public class MappedAccessUnitDecoder implements AbstractAccessUnitDecoder {

    private final RlenStream rlenStream;
    private final GenomicPosition initialPosition;
    private final byte numberTemplateSegments;
    private final byte qv_depth;

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
    final private MScoreStream mScoreStream;
    final private RGroupStream rGroupStream;

    final private byte[] changedNucleotides = new byte[]{};
    final private long[] changedPositions = new long[]{};

    final private ALPHABET_ID alphabet_id;

    final private DATA_CLASS data_class;
    final private String[] readGroupNames;


    private long readCount;


    MappedAccessUnitDecoder(
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
            MScoreStream mScoreStream,
            RGroupStream rGroupStream,
            AbstractSequencesSource sequencesSource,
            short[][][] tokensReadIdentifiers,
            ALPHABET_ID alphabet_id,
            DATA_CLASS data_class,
            String[] readGroupNames,
            byte numberTemplateSegments,
            byte qv_depth
    ) {
        this.sequencesSource = sequencesSource;
        this.numberTemplateSegments = numberTemplateSegments;
        this.qv_depth = qv_depth;
        initialPosition = genomicPosition;
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
        this.mScoreStream = mScoreStream;
        this.rGroupStream = rGroupStream;
        this.readGroupNames = readGroupNames;
        readCount = 0;
        this.alphabet_id = alphabet_id;
        this.data_class = data_class;

        if(tokensReadIdentifiers == null){
            readIdentifierDecoder = null;
        } else {
            readIdentifierDecoder = new TokensStructureDecoder(tokensReadIdentifiers);
        }

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

        int numberOfAlignments = mMapStreamSymbol.getNumberOfAlignments();
        int maxNumberOfSegmentAlignments = mMapStreamSymbol.getMaxNumberOfSegmentAlignments();
        long[][][] mappingPos = posStream.read(
                mMapStreamSymbol.getNumberOfSegmentAlignments(),
                maxNumberOfSegmentAlignments
        );
        SequenceIdentifier[][] mappingSeqIds = new SequenceIdentifier[maxNumberOfSegmentAlignments][numberTemplateSegments];
        mappingSeqIds[0][0] = initialPosition.getSequenceId();
        long[] accessUnitRecord = new long[numberTemplateSegments];
        long[] recordIndex = new long[numberTemplateSegments];
        SplitType[][] splitType = new SplitType[maxNumberOfSegmentAlignments][numberTemplateSegments];
        splitType[0][0] = SplitType.MappedSameRecord;


        boolean isRead1First = pairStream.readFirstAlignment(
            mappingSeqIds,
            mappingPos,
            accessUnitRecord,
            recordIndex,
            splitType
        );

        pairStream.readMoreAlignments(
            mappingSeqIds,
            mappingPos,
            splitType,
            mMapStreamSymbol.getNumberOfSegmentAlignments(),
            pairStreamFirstSymbol.isUnpairedRead(),
            numberOfAlignments,
            mMapStreamSymbol.getAlignPtr()
        );

        pairStream.readPairSpliced(
                pairStreamFirstSymbol.getNumberOfAlignedRecordSegments(),
                mappingPos,
                rlenStreamSymbol.numberOfSplicedSegments,
                rlenStreamSymbol.splicedSegLength
        );

        ReverseCompType[][][] rCompSymbols = rCompStream.read(
                pairStreamFirstSymbol.getNumberOfAlignedRecordSegments(),
                mMapStreamSymbol.getNumberOfSegmentAlignments(),
                rlenStreamSymbol.numberOfSplicedSegments,
                maxNumberOfSegmentAlignments,
                pairStreamFirstSymbol.getNumberOfRecordSegments(),
                splitType
        );

        int[][] mmOffsets = mPosStream.read(pairStreamFirstSymbol.getNumberOfAlignedRecordSegments());
        int[][] mmTypes = mmTypeStream.readMMType(mmOffsets);
        correctMmOffsetsByType(mmOffsets, mmTypes);
        int[][][] mmOffsetsPerSlice = correctMmOffsetsBySplices(mmOffsets, rlenStreamSymbol.splicedSegLength);
        int[][][] mmTypesPerSplice = correctMMTypesPerSlice(mmTypes, mmOffsetsPerSlice);

        SegmentsDecodingResult segmentsDecodingResult =  decode_aligned_segments(
                rlenStreamSymbol.splicedSegLength,
                mmTypesPerSplice,
                mmOffsetsPerSlice,
                mmTypeStream,
                clipsStream.getSoft_clips(),
                mappingPos[0],
                initialPosition.getSequenceId(),
                sequencesSource,
                changedNucleotides,
                changedPositions,
                alphabet_id
        );

        short[][][] qualities = new short[pairStreamFirstSymbol.getNumberOfRecordSegments()][qv_depth][];
        for(int segment_i=0; segment_i < pairStreamFirstSymbol.getNumberOfRecordSegments(); segment_i++) {
            for(int qvDepth_i=0; qvDepth_i < qv_depth; qvDepth_i++) {
                qualities[segment_i][qvDepth_i] = qualityStream.getQualitiesAligned(
                        segmentsDecodingResult.getOperations()[0][segment_i],
                        segmentsDecodingResult.getOperationLength()[0][segment_i],
                        mappingPos[0][segment_i],
                        initialPosition.getPosition()
                );
            }
        }

        long[][][] mapping_score = mScoreStream.read(
                pairStreamFirstSymbol.getNumberOfAlignedRecordSegments(),
                mMapStreamSymbol.getNumberOfSegmentAlignments(),
                splitType
        );

        int rGroupId = rGroupStream.read();

        long[][][] spliceLengths = new long[][][]{rlenStreamSymbol.splicedSegLength};

        byte flags = 0;
        Record result = new Record(
                numberTemplateSegments,
                data_class,
                readName,
                readGroupNames[rGroupId],
                isRead1First,
                qualities,
                segmentsDecodingResult.getDecode_sequences(),
                mappingPos,
                mappingSeqIds,
                accessUnitRecord,
                recordIndex,
                splitType,
                spliceLengths,
                segmentsDecodingResult.getOperations(),
                segmentsDecodingResult.getOperationLength(),
                segmentsDecodingResult.getOriginal_nucleotides(),
                rCompSymbols,
                mapping_score,
                mMapStreamSymbol.getAlignPtr(),
                mMapStreamSymbol.getNumberOfSegmentAlignments(),
                flags,
                mMapStreamSymbol.isMoreAlignments(),
                mMapStreamSymbol.getMoreAlignmentsNextSeqId(),
                mMapStreamSymbol.getMoreAlignmentsNextPos()
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

    /**
     * Method to decode the aligned segment of a half-mapped record
     * @param splicedSegLength Array storing the size of each splice (first dimension the segment, second the splice)
     * @param mmType Matrix storing the types of operation, first dimension is the segment, second is the splice, third
     *               the mutation index
     * @param mmOffsets Matrix storing the position of operation, first dimension is the segment, second is the splice,
     *                  third the mutation index
     * @param mmTypeStream stream from which mutated nucleotides can be read.
     * @param softClip Nucleotides conforming the softclips. First dumension is the segment, length of second dimension
     *                 must be 2 (start and end), third dimension is length of softclip.
     * @param mappingPos First is the segment, second is the splice
     * @param sequenceIdentifier Identifier of sequence within the reference to be used
     * @param sequencesSource Source of reference sequences to use while decoding
     * @param changedNucleotides to which nucleotides are the reference sequences changed to
     * @param changedPositions positions at which the sequence is changed
     * @param alphabet_id identifier of the alphabet used in the access unit
     * @return the decoded segment
     * @throws IOException can be caused by multiple error sources.
     */
    static SegmentsDecodingResult decode_aligned_segments(
            long[][] splicedSegLength,
            int[][][] mmType,
            int[][][] mmOffsets,
            MMTypeStreamInterface mmTypeStream,
            byte[][][] softClip,
            long[][] mappingPos,
            SequenceIdentifier sequenceIdentifier,
            AbstractSequencesSource sequencesSource,
            byte[] changedNucleotides,
            long[] changedPositions,
            ALPHABET_ID alphabet_id) throws IOException {

        int numberOfAlignedRecordSegments = mmType.length;
        byte[][][][] operations = new byte[1][numberOfAlignedRecordSegments][][];
        int[][][][] operationLength = new int[1][numberOfAlignedRecordSegments][][];
        byte[][][] decode_sequences = new byte[numberOfAlignedRecordSegments][][];
        byte[][][][] original_nucleotides = new byte[1][numberOfAlignedRecordSegments][][];
        int[][][] length_original_nucleotides = new int[1][numberOfAlignedRecordSegments][];
        
        for(short alignedRecordSegment_i = 0;
                  alignedRecordSegment_i < numberOfAlignedRecordSegments;
                  alignedRecordSegment_i++) {
             SegmentDecodingResult segmentDecodingResult = decode_aligned_segment(
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
                alphabet_id);

            decode_sequences[alignedRecordSegment_i] = segmentDecodingResult.sequence;
            operations[0][alignedRecordSegment_i] = segmentDecodingResult.operations;
            operationLength[0][alignedRecordSegment_i] = segmentDecodingResult.operationLength;
            original_nucleotides[0][alignedRecordSegment_i] = segmentDecodingResult.original_nucleotides;

        }
        return new SegmentsDecodingResult(decode_sequences, operations, operationLength, original_nucleotides);
    }

    /**
     * Method to decode the aligned segment of a half-mapped record
     * @param splicedSegLength Array storing the size of each splice
     * @param mmType Matrix storing the types of operation, first dimension is the splice, second is the mutation index
     * @param mmOffsets Matrix storing the position of operation, first dimension is the splice, second is the mutation index
     * @param mmTypeStream stream from which mutated nucleotides can be read.
     * @param softClip Nucleotides conforming the softclips. Length of first dimension must be 2 (start and end), second
     *                 dimension is length of softclip.
     * @param mappingPos The dimension is the splice
     * @param sequenceIdentifier Identifier of sequence within the reference to be used
     * @param sequencesSource Source of reference sequences to use while decoding
     * @param changedNucleotides to which nucleotides are the reference sequences changed to
     * @param changedPositions positions at which the sequence is changed
     * @param alphabet_id identifier of the alphabet used in the access unit
     * @return the decoded segment
     * @throws IOException can be caused by multiple error sources.
     */
    static SegmentDecodingResult decode_aligned_segment(
            long[] splicedSegLength,
            int[][] mmType,
            int[][] mmOffsets,
            MMTypeStreamInterface mmTypeStream,
            byte[][] softClip,
            long[] mappingPos,
            SequenceIdentifier sequenceIdentifier,
            AbstractSequencesSource sequencesSource,
            byte[] changedNucleotides,
            long[] changedPositions,
            ALPHABET_ID alphabet_id) throws IOException {

        int numberOfSplices = splicedSegLength.length;

        byte[][] operations = new byte[numberOfSplices][];
        int[][] operationLength = new int[numberOfSplices][];
        byte[][] decode_sequences = new byte[numberOfSplices][];
        byte[][] original_nucleotides = new byte[numberOfSplices][32];
        int[] length_original_nucleotides = new int[numberOfSplices];

        for(
                int splice_i=0;
                splice_i < numberOfSplices;
                splice_i++
        ){

            long position = mappingPos[splice_i];
            long mappedLength = splicedSegLength[splice_i];

            operations[splice_i] = new byte[128];
            operationLength[splice_i] = new int[128];
            int numberOperations = 0;
            decode_sequences[splice_i] = new byte[Math.toIntExact(mappedLength)];

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
                    sequencesSource.getSubsequence(
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
                            delta);
                }

                if(mmType[splice_i][operation_i] == 0){
                    byte newBase = mmTypeStream.readNewMismatchBase(
                            ALPHABET_ID.ALPHABETS[alphabet_id.ID],
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
                            length_original_nucleotides,
                            splice_i,
                            originalBase
                    );
                    previousOffset++;
                }else if(mmType[splice_i][operation_i] == 1){
                    byte newBase = mmTypeStream.readNewInsertBase(ALPHABET_ID.ALPHABETS[alphabet_id.ID]);
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
                            length_original_nucleotides,
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

            original_nucleotides[splice_i] = Arrays.copyOf(
                    original_nucleotides[splice_i],
                    length_original_nucleotides[splice_i]
            );
            decode_sequences[splice_i] = base_decode;

            operations[splice_i] = Arrays.copyOf(operations[splice_i], numberOperations);
            operationLength[splice_i] = Arrays.copyOf(operationLength[splice_i], numberOperations);
        }
        return new SegmentDecodingResult(
                decode_sequences,
                operations,
                operationLength,
                original_nucleotides
        );
    }

    /**
     * Method to append an original base to a selected splice of the first alignment.
     * @param original_nucleotides the current lists of decoded nucleotides (first dimension is the splice, second
     *                             dimension is the length of nucleotides)
     * @param length_original_nucleotides length of the currently decoded nucleotides, the dimension corresponds to the
     *                                    number of splices in the first alignment
     * @param splice_i Index of the splice of the first alignment to which the orignal base must be appended
     * @param originalBase nucleotide to be appended.
     */
    private static void addOriginalBaseAutomatic(
            byte[][] original_nucleotides,
            int[] length_original_nucleotides,
            int splice_i,
            byte originalBase
    ) {
        original_nucleotides[splice_i]
                [length_original_nucleotides[splice_i]] = originalBase;
        length_original_nucleotides[splice_i]++;
        if(
                original_nucleotides[splice_i].length
                        == length_original_nucleotides[splice_i]
        ){
            original_nucleotides[splice_i] = Arrays.copyOf(
                    original_nucleotides[splice_i],
                    original_nucleotides[splice_i].length*2
            );
        }

    }

    /**
     * Adds operation as first operation in the list of operations for the specified splice
     * @param operations Current list of operations. First dimension is the splice, the second is the list of operations
     * @param operationLength Current length of operations. First dimension is the splice, the second is the list of
     *                        lengths of operations
     * @param splice_i Splice index for which the operation shall be added.
     * @param operation Operation to be prepended
     * @param numberOperations current number of operations
     * @param length length of the operation to add
     * @return the new number of operations.
     */
    private static int prependOperation(
            final byte[][] operations,
            final int[][] operationLength,
            final int splice_i,
            final byte operation,
            final int numberOperations,
            final int length) {

        System.arraycopy(
                operations[splice_i],
                0,
                operations[splice_i],
                1,
                operations[splice_i].length-1
        );
        System.arraycopy(
                operationLength[splice_i],
                0,
                operationLength[splice_i],
                1,
                operationLength[splice_i].length-1
        );
        operations[splice_i][0] = operation;
        operationLength[splice_i][0] = length;
        return resizeOperationsArrays(operations, operationLength, splice_i, numberOperations);
    }

    /**
     * Resizes the operations and operationLength arrays, for the indicated splice of the first alignment.
     * @param operations Current list of operations. First dimension is the alignment (only the first one is used),
     *                   second is the splice, the third is the list of operations
     * @param operationLength Current length of operations. First dimension is the splice, the second is the list of
     *                        lengths of operations
     * @param splice_i Splice index for which the operation shall be added.
     * @param numberOperations Past number of operations, i.e. the actual populated size
     * @return The new number of operations, i.e. past number plus one.
     */
    private static int resizeOperationsArrays(
            byte[][] operations,
            int[][] operationLength,
            int splice_i,
            int numberOperations
    ) {
        numberOperations++;
        if(operations[splice_i].length == numberOperations){
            operations[splice_i] = Arrays.copyOf(operations[splice_i], operations[splice_i].length*2);
            operationLength[splice_i] = Arrays.copyOf(
                    operationLength[splice_i], operationLength[splice_i].length*2
            );
        }
        return numberOperations;
    }

    /**
     * Append a new operation to the list operations for the selected splice of the first alignment,
     * @param operations Current list of operations. First dimension is the alignment (only the first one is used),
     *                   second is the splice, the third is the list of operations
     * @param operationLength Current length of operations. First dimension  is the splice, the second is the list
     *                       of lengths of operations
     * @param splice_i Splice index for which the operation shall be added.
     * @param operation Operation to be prepended
     * @param numberOperations current number of operations
     * @param length length of the operation to add
     * @return the new number of operations, i.e. the previous number plus one
     */
    private static int addOperationAutomatic(
            final byte[][] operations,
            final int[][] operationLength,
            final int splice_i,
            final byte operation,
            final int numberOperations,
            final int length) {

        operations[splice_i][numberOperations] = operation;
        operationLength[splice_i][numberOperations] = length;
        return resizeOperationsArrays(operations, operationLength, splice_i, numberOperations);
    }

    /**
     *
     * @param position 0-based position of the first base contained in the nucleotide sequence
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

    /**
     * Corrects the offsets of mutations based on the previous number of deletions
     * @param mmOffsets the offset of each mutation. First dimension is the splice index, second is the mutation.
     * @param mmTypes the type of the mutations. First dimension is the splice index, second is the mutation.
     */
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

    /**
     * Corrects the offsets of mutations based on the previous number of deletions
     * @param mmOffsets the offset of each mutation. First dimension is the splice index, second is the mutation.
     * @param splicedReadsLength the length of each splice. First dimension is the segment, the second is the splice
     */
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

    /**
     * Creates one array by prepending one array to another
     * @param array base array to work with
     * @param arrayToPrepend array which has to be prepended
     * @return a new array with value [arrayToPrepend, array]
     */
    static byte[] prependToArrays(
            byte[] array,
            byte[] arrayToPrepend
    ){
        byte[] newArray = new byte[array.length + arrayToPrepend.length];
        System.arraycopy(arrayToPrepend, 0, newArray, 0, arrayToPrepend.length);
        System.arraycopy(array, 0, newArray, arrayToPrepend.length, array.length);
        return newArray;
    }

    /**
     * Creates one array by appending one array to another
     * @param array base array to work with
     * @param arrayToAppend array which has to be appended
     * @return a new array with value [array, arrayToAppend]
     */
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

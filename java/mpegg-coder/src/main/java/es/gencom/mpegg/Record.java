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

import es.gencom.mpegg.decoder.AbstractSequencesSource;
import es.gencom.mpegg.encoder.Operation;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.List;

/**
 * <p>MPEG-G Record implementation (ISO/IEC 23092-2 13.2)</p>
 *
 */
public class Record {
    final public DATA_CLASS data_class;
    final private String readName;
    final private String readGroup;
    final private boolean read1First;
    final private byte number_of_template_segments;
    final private byte flags;

    //first dimension is segment, second is the nucleotide index
    final private byte[][] sequenceBytes;
    
    /**
     * <p>13.2.15 quality_values.</p>
     * first dimension is segment, second is the nucleotide index, third is the number of qualities
     */
    final private short[][][] qualityValues;

    final private int[] numberSegmentAlignments;
    //first dimension is alignment, second is segment, third is splice
    final private long[][][] mappingPositions;
    //first dimension is alignment, second is segment
    final private SequenceIdentifier[][] mappingSequenceIdentifier;
    //the first dimension is the alignment, the second is the index of the segment
    final private SplitType[][] splitMate;
    final public long[] unmappedRecordAUId;
    final public long[] unmappedRecordAUSegment;
    //dimensions: segment, alignment, splice; stores the length of the splices
    private final long[][][] lengthSplices;
    //dimensions: segment, alignment, splice, operation index
    final private byte[][][][] operationType;
    //dimensions: segment, alignment, splice, operation index
    final private int[][][][] operationLength;
    //dimensions: segment, alignment, splice, operation index
    final private byte[][][][] originalBase;
    //dimensions: alignment, segment, splice
    final private ReverseCompType[][][] reverseCompliment;
    //dimensions: segment, alignment, the number of mapping scores
    final private long[][][] mapping_score;
    //dimensions: alignment, numSegments. Contains all alignmentPairs
    final private int[][] alignPtr;
    final private boolean moreAlignments;
    final private SequenceIdentifier moreAlignmentsNextSeqId;
    final private long moreAlignmentsNextPos;

    public Record(
            byte number_of_template_segments,
            DATA_CLASS data_class,
            String readName,
            String readGroup,
            boolean read1First,
            short[][][] qualityValues,
            byte[][] sequenceBytes,
            long[][][] mappingPositions,
            SequenceIdentifier[][] mappingSequenceIdentifier,
            long[] unmappedRecordAUId,
            long[] unmappedRecordAUSegment,
            SplitType[][] splitMate,
            long[][][] lengthSplices,
            byte[][][][] operationType,
            int[][][][] operationLength,
            byte[][][][] originalBase,
            ReverseCompType[][][] reverseCompliment,
            long[][][] mapping_score,
            int[][] alignPtr,
            int[] numberSegmentAlignments,
            byte flags,
            boolean moreAlignments,
            SequenceIdentifier moreAlignmentsNextSeqId,
            long moreAlignmentsNextPos) {
        this.number_of_template_segments = number_of_template_segments;
        this.mappingPositions = mappingPositions;
        this.mappingSequenceIdentifier = mappingSequenceIdentifier;

        int numSegmentsBasedOnSplitMate = splitMate[0].length;
        for(int alignment_i = 1; alignment_i < splitMate.length; alignment_i++){
            if(numSegmentsBasedOnSplitMate != splitMate[alignment_i].length){
                throw new IllegalArgumentException("Split mate matrix is not consistent");
            }
        }


        this.data_class = data_class;
        this.readName = readName;
        this.readGroup = readGroup;
        this.read1First = read1First;
        this.sequenceBytes = sequenceBytes;
        this.qualityValues = qualityValues;
        this.splitMate = splitMate;
        this.lengthSplices = lengthSplices;
        this.operationType = operationType;
        this.operationLength = operationLength;
        this.originalBase = originalBase;
        this.reverseCompliment = reverseCompliment;
        this.mapping_score = mapping_score;
        this.alignPtr = alignPtr;
        this.numberSegmentAlignments = numberSegmentAlignments;
        this.unmappedRecordAUId = unmappedRecordAUId;
        this.unmappedRecordAUSegment = unmappedRecordAUSegment;

        for(int segment_i=0; segment_i<operationType.length; segment_i++){
            if(operationType[segment_i] == null){
                operationType[segment_i] = new byte[0][][];
            }
            if(operationLength[segment_i] == null){
                operationType[segment_i] = new byte[0][][];
            }
        }
        this.flags = flags;
        this.moreAlignments = moreAlignments;
        if(moreAlignments && moreAlignmentsNextSeqId == null){
            throw new IllegalArgumentException();
        }
        this.moreAlignmentsNextSeqId = moreAlignmentsNextSeqId;
        this.moreAlignmentsNextPos = moreAlignmentsNextPos;
    }

    public static Record read(ReadableMSBitFileChannel reader, boolean includesOriginalBases) throws IOException {
        short number_of_template_segments = reader.readUnsignedByte();
        short number_of_record_segments = reader.readUnsignedByte();
        int number_of_alignments = reader.readUnsignedShort();
        DATA_CLASS data_class = DATA_CLASS.getDataClass(reader.readByte());
        short read_group_len = reader.readUnsignedByte();
        boolean read_1_first = reader.readByte() == 1;
        int seq_ID = 0;
        short as_depth = 0;
        if(number_of_alignments > 0){
            seq_ID = reader.readUnsignedShort();
            as_depth = reader.readUnsignedByte();
        }
        int[] read_len = new int[number_of_record_segments];
        for(int segment_i=0; segment_i < number_of_record_segments; segment_i++){
            read_len[segment_i] = (int) reader.readBits(24);
        }
        short qv_depth = reader.readUnsignedByte();
        int read_name_len = reader.readUnsignedShort();
        char[] read_name_chars = reader.readChars(read_name_len);
        String read_name = new String(read_name_chars);
        char[] read_group_name_chars = reader.readChars(read_group_len);
        String read_group = new String(read_group_name_chars);

        byte[][] sequences = new byte[number_of_record_segments][];
        short[][][] qualityValues = new short[number_of_record_segments][][];

        for(int record_segment_i=0; record_segment_i < number_of_record_segments; record_segment_i++){
            sequences[record_segment_i] = new byte[read_len[record_segment_i]];
            qualityValues[record_segment_i] = new short[read_len[record_segment_i]][qv_depth];
            for(int base_i=0; base_i < read_len[record_segment_i]; base_i++){
                sequences[record_segment_i][base_i] = reader.readByte();
                for(int qs = 0; qs < qv_depth; qs++) {
                    qualityValues[record_segment_i][base_i][qs] = reader.readByte();
                }
            }
        }

        long[][][] expanded_mapping_pos = new long[number_of_alignments][number_of_template_segments][1];
        int[][] expanded_seqId = new int[number_of_alignments][number_of_template_segments];
        String[][] expanded_ecigars = new String[number_of_alignments][number_of_template_segments];
        ReverseCompType[][][] expanded_reverseCompTypes = new ReverseCompType[number_of_alignments][number_of_template_segments][1];
        long[][][] expanded_mappingScores = new long[number_of_alignments][number_of_template_segments][as_depth];
        SplitType[][] expanded_splitTypes = new SplitType[number_of_alignments][number_of_template_segments];

        for(int noa = 0; noa < number_of_alignments; noa++){
            expanded_mapping_pos[noa][0][0] = reader.readBits(40);
            expanded_seqId[noa][0] = seq_ID;
            int ecigar_len = Math.toIntExact(reader.readBits(24));
            expanded_ecigars[noa][0] = new String(reader.readChars(ecigar_len));
            expanded_reverseCompTypes[noa][0][0] = ReverseCompType.getReverseComp(reader.readByte());
            for(int as = 0; as < as_depth; as++){
                expanded_mappingScores[noa][0][as] = reader.readInt();
            }
            if(data_class != DATA_CLASS.CLASS_HM){
                for(int templateSegment_i=1; templateSegment_i < number_of_template_segments; templateSegment_i++){
                    byte split_tmp = reader.readByte();
                    if(split_tmp == 0){
                        long delta = reader.readBits(48);
                        expanded_mapping_pos[noa][templateSegment_i][0] = expanded_mapping_pos[noa][0][0] + delta;
                        expanded_seqId[noa][templateSegment_i] = seq_ID;
                        ecigar_len = Math.toIntExact(reader.readBits(24));
                        expanded_ecigars[noa][templateSegment_i] = new String(reader.readChars(ecigar_len));
                        expanded_reverseCompTypes[noa][templateSegment_i][0] = ReverseCompType.getReverseComp(reader.readByte());
                        for(int as = 0; as < as_depth; as++){
                            expanded_mappingScores[noa][templateSegment_i][as] = reader.readInt();
                        }
                        expanded_splitTypes[noa][templateSegment_i] = SplitType.MappedSameRecord;
                    } else if (split_tmp == 1){
                        expanded_mapping_pos[noa][templateSegment_i][0] = reader.readBits(40);
                        expanded_seqId[noa][templateSegment_i] = reader.readUnsignedShort();
                        if(expanded_seqId[noa][templateSegment_i] == seq_ID){
                            expanded_splitTypes[noa][templateSegment_i] = SplitType.MappedDifferentRecordSameSequence;
                        } else {
                            expanded_splitTypes[noa][templateSegment_i] = SplitType.MappedDifferentRecordDifferentSequence;
                        }
                    } else if (split_tmp == 2){
                        expanded_splitTypes[noa][templateSegment_i] = SplitType.Unpaired;
                    }
                }
            }
        }
        short flags = reader.readUnsignedByte();
        short more_alignments_selector = reader.readUnsignedByte();
        boolean more_alignments = false;
        long more_alignments_next_pos = 0;
        int more_alignments_next_seq_id = 0;
        if(more_alignments_selector == 1){
            more_alignments = true;
            more_alignments_next_pos = reader.readBits(40);
            more_alignments_next_seq_id = reader.readUnsignedShort();
        } else {
            if(more_alignments_selector != 0){
                throw new IllegalArgumentException();
            }
        }


        long[][][] mapping_pos = new long[number_of_alignments][number_of_template_segments][1];
        int[][] seqId = new int[number_of_alignments][number_of_template_segments];
        String[][] ecigars = new String[number_of_alignments][number_of_template_segments];
        ReverseCompType[][][] reverseCompTypes = new ReverseCompType[number_of_alignments][number_of_template_segments][1];
        long[][][] mappingScores = new long[number_of_alignments][number_of_template_segments][as_depth];
        SplitType[][] splitTypes = new SplitType[number_of_alignments][number_of_template_segments];

        int[][] alignPtr = new int[number_of_alignments][number_of_template_segments];
        List<HashMap<AlignmentIdentifier, Integer>> fromAlignmentToIdentifier =
                new ArrayList<>(number_of_template_segments);
        for(int segment_i=0; segment_i<number_of_template_segments; segment_i++){
            fromAlignmentToIdentifier.add(new HashMap<>());
        }

        int maxAlignmentIdentifier = -1;
        int[] numberAlignments = new int[number_of_template_segments];
        for(int noa = 0; noa < number_of_alignments; noa++){
            for(int segment_i=0; segment_i < number_of_template_segments; segment_i++){
                HashMap<AlignmentIdentifier, Integer> map = fromAlignmentToIdentifier.get(segment_i);
                AlignmentIdentifier currentAlignmentIdentifier = new AlignmentIdentifier(
                        expanded_seqId[noa][segment_i],
                        expanded_mapping_pos[noa][segment_i][0],
                        expanded_reverseCompTypes[noa][segment_i][0],
                        expanded_ecigars[noa][segment_i],
                        expanded_splitTypes[noa][segment_i]
                );
                Integer currentIdentifier = map.get(currentAlignmentIdentifier);
                if(currentIdentifier == null){
                    currentIdentifier = map.size();

                    maxAlignmentIdentifier = Integer.max(maxAlignmentIdentifier, currentIdentifier);

                    map.put(currentAlignmentIdentifier, currentIdentifier);
                    numberAlignments[segment_i]++;

                    mapping_pos[currentIdentifier][segment_i] = expanded_mapping_pos[noa][segment_i];
                    seqId[currentIdentifier][segment_i] = expanded_seqId[noa][segment_i];
                    ecigars[currentIdentifier][segment_i] = expanded_ecigars[noa][segment_i];
                    reverseCompTypes[currentIdentifier][segment_i] = expanded_reverseCompTypes[noa][segment_i];
                    mappingScores[currentIdentifier][segment_i] = expanded_mappingScores[noa][segment_i];
                    splitTypes[currentIdentifier][segment_i] = expanded_splitTypes[noa][segment_i];
                }

                alignPtr[noa][segment_i] = currentIdentifier;
            }
        }

        mapping_pos = Arrays.copyOf(mapping_pos, maxAlignmentIdentifier+1);
        seqId = Arrays.copyOf(seqId, maxAlignmentIdentifier+1);
        ecigars = Arrays.copyOf(ecigars, maxAlignmentIdentifier+1);
        reverseCompTypes = Arrays.copyOf(reverseCompTypes, maxAlignmentIdentifier+1);
        mappingScores = Arrays.copyOf(mappingScores, maxAlignmentIdentifier+1);
        splitTypes = Arrays.copyOf(splitTypes, maxAlignmentIdentifier+1);
        long[][][] splicesLength = new long[maxAlignmentIdentifier+1][number_of_template_segments][1];
        byte[][][][] operationType = new byte[maxAlignmentIdentifier+1][number_of_template_segments][][];
        int[][][][] operationLength = new int[maxAlignmentIdentifier+1][number_of_template_segments][][];


        populateBasedOnEcigars(
                mapping_pos,
                ecigars,
                reverseCompTypes,
                operationType,
                operationLength,
                splicesLength,
                numberAlignments,
                data_class == DATA_CLASS.CLASS_HM ? 1 : number_of_record_segments
        );

        SequenceIdentifier[][] mappingSequenceIdentifier =
                new SequenceIdentifier[maxAlignmentIdentifier+1][number_of_template_segments];
        for(int segment_i=0; segment_i < number_of_template_segments; segment_i++){
            for(int alignment_i=0; alignment_i < numberAlignments[segment_i]; alignment_i++){
                mappingSequenceIdentifier[alignment_i][segment_i] = new SequenceIdentifier(
                        seqId[alignment_i][segment_i]
                );
            }
        }

        byte[][][][] originalBases = new byte[maxAlignmentIdentifier+1][number_of_template_segments][][];

        if(includesOriginalBases){
            for(int segment_i=0; segment_i < number_of_template_segments; segment_i++){
                for(int alignment_i=0; alignment_i < numberAlignments[segment_i]; alignment_i++){
                    if(splitTypes[alignment_i][segment_i] == SplitType.MappedSameRecord) {
                        int numSplices = operationType[alignment_i][segment_i].length;
                        originalBases[alignment_i][segment_i] = new byte[numSplices][];
                        for (int splice_i = 0; splice_i < numSplices; splice_i++) {
                            originalBases[alignment_i][segment_i][splice_i] = new byte[16];
                            int numOriginalBases = 0;
                            int numOperations = operationType[alignment_i][segment_i][splice_i].length;
                            for (int operation_i = 0; operation_i < numOperations; operation_i++) {
                                byte selectedOperationType = operationType[alignment_i][segment_i][splice_i][operation_i];
                                if (selectedOperationType == Operation.Delete
                                        || selectedOperationType == Operation.Substitution
                                        || selectedOperationType == Operation.SubstitutionToN) {
                                    int selectedOperationLength =
                                            operationLength[alignment_i][segment_i][splice_i][operation_i];
                                    for (int originalBase_i = 0; originalBase_i < selectedOperationLength; originalBase_i++) {
                                        originalBases[alignment_i][segment_i][splice_i][numOriginalBases] = reader.readByte();
                                        numOriginalBases++;
                                        if (originalBases[alignment_i][segment_i][splice_i].length == numOriginalBases) {
                                            originalBases[alignment_i][segment_i][splice_i] = Arrays.copyOf(
                                                    originalBases[alignment_i][segment_i][splice_i],
                                                    numOriginalBases * 2
                                            );
                                        }

                                    }
                                }
                            }
                            originalBases[alignment_i][segment_i][splice_i] = Arrays.copyOf(
                                    originalBases[alignment_i][segment_i][splice_i],
                                    numOriginalBases
                            );
                        }
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }

        return new Record(
                (byte)number_of_template_segments,
                data_class,
                read_name,
                read_group,
                read_1_first,
                qualityValues,
                sequences,
                mapping_pos,
                mappingSequenceIdentifier,
                new long[number_of_template_segments],
                new long[number_of_template_segments],
                splitTypes,
                splicesLength,
                operationType,
                operationLength,
                originalBases,
                reverseCompTypes,
                mappingScores,
                alignPtr,
                numberAlignments,
                (byte)flags,
                more_alignments,
                new SequenceIdentifier(more_alignments_next_seq_id),
                more_alignments_next_pos
        );
    }

    private static void populateBasedOnEcigars(
            long[][][] mapping_pos,
            String[][] ecigars,
            ReverseCompType[][][] reverseCompTypes,
            byte[][][][] operationType,
            int[][][][] operationLength,
            long[][][] splicesLength,
            int[] numberAlignments,
            short number_of_record_segments){
        for(int segment_i= 0; segment_i < number_of_record_segments; segment_i++){
            for(int alignment_i=0; alignment_i < numberAlignments[segment_i]; alignment_i++){
                ExtendedCigarParser cigarParser = new ExtendedCigarParser(ecigars[alignment_i][segment_i]);
                int num_splices = cigarParser.getOperationType().length;

                mapping_pos[alignment_i][segment_i] = Arrays.copyOf(
                        mapping_pos[alignment_i][segment_i], num_splices);
                reverseCompTypes[alignment_i][segment_i] = Arrays.copyOf(
                        reverseCompTypes[alignment_i][segment_i], num_splices);
                splicesLength[alignment_i][segment_i] = cigarParser.getSpliceLength();
                operationType[alignment_i][segment_i] = cigarParser.getOperationType();
                operationLength[alignment_i][segment_i] = cigarParser.getLengthOperation();

                for(int splice_i=1; splice_i < num_splices; splice_i++){
                    mapping_pos[alignment_i][segment_i][splice_i] =
                            mapping_pos[alignment_i][segment_i][splice_i-1] + cigarParser.getSpliceOffset()[splice_i];
                }
            }
        }
    }

    public byte getNumRecordSegments(){
        if(sequenceBytes.length == 1 || sequenceBytes[1]==null){
            return 1;
        }else {
            return 2;
        }
    }

    public byte getNumAlignedSegments() {
        byte result = 0;
        for(int segment_i=0; segment_i < splitMate[0].length; segment_i++){
            if(splitMate[0][segment_i] == SplitType.MappedSameRecord){
                result++;
            }
        }
        return result;
    }

    public boolean isRead1First() {
        return read1First;
    }

    public int getNumAlignmentsForSegment(int segment_i) {
        if(alignPtr == null){
            if(segment_i != 0){
                return 0;
            } else {
                return splitMate.length;
            }
        }
        int maxAlignmentIndex = 0;
        for(int alignment_i=0; alignment_i < alignPtr.length; alignment_i++){
            if(alignPtr[alignment_i][segment_i] > maxAlignmentIndex){
                maxAlignmentIndex = alignPtr[alignment_i][segment_i];
            }
        }
        if(maxAlignmentIndex == 0 && (
                splitMate[0][segment_i] == SplitType.UnmappedSameRecord
                || splitMate[0][segment_i] == SplitType.UnmappedDifferentRecordSameAU
                || splitMate[0][segment_i] == SplitType.UnmappedDifferentRecordDifferentAU)){
            return 0;
        }
        return maxAlignmentIndex + 1;
    }

    public boolean isUnpaired() {
        return splitMate[0].length == 1 || splitMate[0][1]==SplitType.Unpaired;
    }

    public long getMappingPosSegment0FirstAlignment(){
        return mappingPositions[0][0][0];
    }

    public long[][][] getSpliceLengths(){
        return lengthSplices;
    }

    public int[] getAlignmentIndexesSegment1(int segment0alignment_i) {
        int[] result = new int[1024];
        int sizeResult = 0;
        for(int alingPtr_i=0; alingPtr_i < alignPtr.length; alingPtr_i++){
            if(alignPtr[alingPtr_i][0] == segment0alignment_i){
                result[sizeResult] = alignPtr[alingPtr_i][1];
                sizeResult++;
                if(sizeResult == result.length){
                    result = Arrays.copyOf(result, result.length*2);
                }
            }
        }
        result = Arrays.copyOf(result, sizeResult);

        return result;
    }

    public byte[][][] getSoftclip(){
        byte[][][] softclips = new byte[getNumAlignedSegments()][2][];
        for(byte alignedSegment_i = 0; alignedSegment_i < getNumAlignedSegments(); alignedSegment_i++){
            if(operationType[0][alignedSegment_i] == null || operationType[0][alignedSegment_i].length == 0){
                softclips[alignedSegment_i][0] = new byte[0];
                softclips[alignedSegment_i][1] = new byte[0];
                continue;
            }
            byte[] operationsFirstSplice = operationType[0][alignedSegment_i][0];
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
                        operationLength[0][alignedSegment_i][0][softClipStartPosition]);
            } else {
                softclips[alignedSegment_i][0] = new byte[]{};
            }

            int indexLastSplice = operationType[0][alignedSegment_i].length - 1;
            byte[] operationsLastSplice = operationType[0]
                    [alignedSegment_i]
                    [indexLastSplice];
            int softClipEndPosition = -1;
            if(operationsLastSplice[operationsLastSplice.length - 1] == Operation.HardClip) {
                if(operationsLastSplice[operationsLastSplice.length - 2] == Operation.SoftClip) {
                    softClipEndPosition = operationsLastSplice.length - 2;
                }
            } else if(operationsLastSplice[operationsLastSplice.length - 1] == Operation.SoftClip) {
                softClipEndPosition = operationsLastSplice.length - 1;
            }

            if(softClipEndPosition != -1){
                int[] operationsLengthLastSplice = operationLength[0]
                        [alignedSegment_i]
                        [indexLastSplice];

                int to = sequenceBytes[alignedSegment_i].length;
                int from = to - operationsLengthLastSplice
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
        int[][] hardclips = new int[getNumAlignedSegments()][2];
        for(byte alignedSegment_i = 0; alignedSegment_i < getNumAlignedSegments(); alignedSegment_i++){
            if(operationType[0]==null || operationType[0][alignedSegment_i].length == 0){
                hardclips[alignedSegment_i][0] = 0;
                hardclips[alignedSegment_i][1] = 0;
                continue;
            }
            byte[] operationsFirstSplice = operationType[0][alignedSegment_i][0];
            if(operationsFirstSplice[0] == Operation.HardClip) {
                hardclips[alignedSegment_i][0] = operationLength[0][alignedSegment_i][0][0];
            } else {
                hardclips[alignedSegment_i][0] = 0;
            }

            int indexLastSplice = operationType[0][alignedSegment_i].length-1;
            byte[] operationsLastSplice = operationType
                    [0]
                    [alignedSegment_i]
                    [indexLastSplice];
            if(operationsLastSplice[operationsLastSplice.length-1] == Operation.HardClip) {
                hardclips[alignedSegment_i][1] =
                        operationLength
                                [0]
                                [alignedSegment_i]
                                [indexLastSplice]
                                [operationsLastSplice.length-1];
            } else {
                hardclips[alignedSegment_i][1] = 0;
            }
        }

        return hardclips;
    }

    public ReverseCompType[][][] getReverseCompliment() {
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

    public SplitType[][] getSplitMate() {
        return splitMate;
    }

    public String getReadName() {
        return readName;
    }

    public short[][][] getQualityValues() {
        return qualityValues;
    }

    public String getGroupId() {
        return readGroup;
    }

    /*public static Record createOneAligned(
            DATA_CLASS data_class,
            long readId,
            String readName,
            String readGroup,
            boolean isRead1,
            byte[] sequenceBytes,
            short[][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment,
            byte[][][] operationType,
            int[][][] operationLength,
            byte[][][] originalBase,
            long[][] spliceLengths,
            ReverseCompType[][] reverseCompliment,
            long[][] mapping_score
    ) {

        int[][] alingPtr = new int[mappingPositionsSegment.length][1];
        for(int i=0; i<mappingPositionsSegment.length; i++){
            alingPtr[i][0] = i;
        }

        return new Record(
                data_class,
                readId,
                readName,
                readGroup,
                isRead1,
                true,
                new byte[][]{sequenceBytes},
                new short[][][]{qualityValues},
                sequenceId,
                mappingPositionsSegment,
                new SplitType[][]{{SplitType.SameRecord}},
                null,
                null,
                new long[][][]{spliceLengths},
                new byte[][][][]{operationType},
                new int[][][][]{operationLength},
                new byte[][][][]{originalBase},
                new ReverseCompType[][][]{reverseCompliment},
                new long[][][]{mapping_score},
                alingPtr
        );
    }

    public static Record createTwoAlignedSecondOtherRecordSameSequence(
            long readId,
            String readName,
            String readGroup,
            boolean read1First,
            byte[] sequenceBytes,
            short[][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment0,
            long[] mappingPositionsSegment1,
            byte[][][] operationType,
            int[][][] operationLength,
            byte[][][] originalBase,
            long[][][] spliceLengths,
            ReverseCompType[][] reverseCompliment,
            long[][] mapping_score,
            int[][] alignPtr,
            DATA_CLASS data_class) {

        SequenceIdentifier[] sequencesSegment1 = new SequenceIdentifier[mappingPositionsSegment1.length];
        Arrays.fill(sequencesSegment1, sequenceId);
        return createTwoAlignedSecondOtherRecord(
                data_class,
                readId,
                readName,
                readGroup,
                read1First,
                sequenceBytes,
                qualityValues,
                sequenceId,
                mappingPositionsSegment0,
                sequencesSegment1,
                mappingPositionsSegment1,
                operationType,
                operationLength,
                originalBase,
                spliceLengths,
                reverseCompliment,
                mapping_score,
                alignPtr);
    }

    public static Record createTwoAlignedSecondOtherRecordOtherSequence(
            DATA_CLASS data_class,
            long readId,
            String readName,
            String readGroup,
            boolean read1First,
            byte[] sequenceBytes,
            short[][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment0,
            SequenceIdentifier[] sequencesSegment1,
            long[] mappingPositionsSegment1,
            byte[][][] operationType,
            int[][][] operationLength,
            byte[][][] originalBase,
            long[][][] spliceLengths,
            ReverseCompType[][] reverseCompliment,
            long[][] mapping_score,
            int[][] alignPtr) {

        return createTwoAlignedSecondOtherRecord(
                data_class,
                readId,
                readName,
                readGroup,
                read1First,
                sequenceBytes,
                qualityValues,
                sequenceId,
                mappingPositionsSegment0,
                sequencesSegment1,
                mappingPositionsSegment1,
                operationType,
                operationLength,
                originalBase,
                spliceLengths,
                reverseCompliment,
                mapping_score,
                alignPtr);
    }

    private static Record createTwoAlignedSecondOtherRecord(
            DATA_CLASS data_class,
            long readId,
            String readName,
            String readGroup,
            boolean read1First,
            byte[] sequenceBytes,
            short[][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment0,
            SequenceIdentifier[] sequencesSegment1,
            long[] mappingPositionsSegment1,
            byte[][][] operationType,
            int[][][] operationLength,
            byte[][][] originalBase,
            long[][][] spliceLengths,
            ReverseCompType[][] reverseCompliment,
            long[][] mapping_score,
            int[][] alignPtr) {

        long[][] mappingPositionSegment1Restructured = new long[mappingPositionsSegment1.length][1];
        for(
                int mappingPositionSeg1_i=0;
                mappingPositionSeg1_i < mappingPositionsSegment1.length;
                mappingPositionSeg1_i++
        ){
            mappingPositionSegment1Restructured[mappingPositionSeg1_i][0] =
                    mappingPositionsSegment1[mappingPositionSeg1_i];
        }

        byte[][][][] operationTypeResized = new byte[][][][]{operationType};
        int[][][][] operationLengthResized = new int[][][][]{operationLength};
        byte[][][][] originalBaseResized = new byte[][][][]{originalBase};
        ReverseCompType[][][] reverseComplimentResized = new ReverseCompType[][][]{reverseCompliment};
        long[][][] mapping_scoreResized = new long[][][]{mapping_score};
        SplitType[][] splitMate = new SplitType[][]{{SplitType.SameRecord, SplitType.DifferentRecord}};



        return new Record(
                data_class,
                readId,
                readName,
                readGroup,
                read1First,
                false,
                new byte[][]{sequenceBytes},
                new short[][][]{qualityValues},
                sequenceId,
                mappingPositionsSegment0,
                splitMate,
                sequencesSegment1,
                mappingPositionSegment1Restructured,
                spliceLengths,
                operationTypeResized,
                operationLengthResized,
                originalBaseResized,
                reverseComplimentResized,
                mapping_scoreResized,
                alignPtr
        );
    }

    public static Record createOneAlignedSecondOtherRecordUnmapped(
            DATA_CLASS data_class,
            long readId,
            String readName,
            String readGroup,
            boolean read1First,
            byte[] sequenceBytes,
            short[][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment0,
            byte[][][] operationType,
            int[][][] operationLength,
            byte[][][] originalBase,
            long[][][] spliceLengths,
            ReverseCompType[][] reverseCompliment,
            long[][] mapping_score,
            int uAu_id,
            long uRecord_id) {

        byte[][][][] operationTypeResized = new byte[][][][]{operationType};
        int[][][][] operationLengthResized = new int[][][][]{operationLength};
        byte[][][][] originalBaseResized = new byte[][][][]{originalBase};
        ReverseCompType[][][] reverseComplimentResized = new ReverseCompType[][][]{reverseCompliment};
        long[][][] mapping_scoreResized = new long[][][]{mapping_score};
        SplitType[][] splitType = new SplitType[][]{{SplitType.SameRecord, SplitType.UnmappedOtherRecord}};

        return new Record(
                data_class,
                readId,
                readName,
                readGroup,
                read1First,
                false,
                new byte[][]{sequenceBytes},
                new short[][][]{qualityValues},
                sequenceId,
                mappingPositionsSegment0,
                splitType,
                null,
                null,
                spliceLengths,
                operationTypeResized,
                operationLengthResized,
                originalBaseResized,
                reverseComplimentResized,
                mapping_scoreResized,
                null);
    }

    public static Record createTwoAligned(
            DATA_CLASS data_class,
            long readId,
            String readName,
            String readGroup,
            boolean read1First,
            byte[][] sequenceBytes,
            short[][][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment0,
            long[][] mappingPositionsSegment1,
            byte[][][][] operationType,
            int[][][][] operationLength,
            byte[][][][] originalBase,
            long[][][] spliceLength,
            ReverseCompType[][][] reverseCompliment,
            long[][][] mapping_score,
            int[][] alignPtr) {

        SequenceIdentifier[] sequencesSegment1 = new SequenceIdentifier[mappingPositionsSegment1.length];
        Arrays.fill(sequencesSegment1, sequenceId);
        SplitType[][] splitMate = new SplitType[][]{{SplitType.SameRecord, SplitType.SameRecord}};

        return new Record(
                data_class,
                readId,
                readName,
                readGroup,
                read1First,
                false,
                sequenceBytes,
                qualityValues,
                sequenceId,
                mappingPositionsSegment0,
                splitMate,
                sequencesSegment1,
                mappingPositionsSegment1,
                spliceLength,
                operationType,
                operationLength,
                originalBase,
                reverseCompliment,
                mapping_score,
                alignPtr);
    }

    public static Record createOneAlignedOneUnmapped(
            DATA_CLASS data_class,
            long readId,
            String readName,
            String readGroup,
            boolean read1First,
            byte[][] sequenceBytes,
            short[][][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment0,
            byte[][][] operationType,
            int[][][] operationLength,
            byte[][][] originalBase,
            long[][][] spliceLength,
            ReverseCompType[][] reverseCompliment,
            long[][] mapping_score) {

        byte[][][][] operationTypeResized = new byte[][][][]{operationType};
        int[][][][] operationLengthResized = new int[][][][]{operationLength};
        byte[][][][] originalBaseResized = new byte[][][][]{originalBase};
        ReverseCompType[][][] reverseComplimentResized = new ReverseCompType[][][]{reverseCompliment};
        long[][][] mapping_scoreResized = new long[][][]{mapping_score};
        SplitType[][] splitTypes = new SplitType[][]{{SplitType.SameRecord, SplitType.UnmappedSameRecord}};

        return new Record(
                data_class,
                readId,
                readName,
                readGroup,
                read1First,
                false,
                sequenceBytes,
                qualityValues,
                sequenceId,
                mappingPositionsSegment0,
                splitTypes,
                new SequenceIdentifier[]{null},
                new long[][]{{0}},
                spliceLength,
                operationTypeResized,
                operationLengthResized,
                originalBaseResized,
                reverseComplimentResized,
                mapping_scoreResized,
                new int[][]{{0,0}});
    }*/

    /**
     * Returns the mapping scores
     * @return the mapping scores, the dimensions correspond to segment, alignment, the number of mapping scores
     */
    public long[][][] getMapping_score() {
        return mapping_score;
    }

    public short getAsDepth(){
        for(int segment_i=0; segment_i < mapping_score.length; segment_i++){
            for(int splice_i=0; splice_i < mapping_score[segment_i].length; splice_i++){
                return (short)mapping_score[segment_i][splice_i].length;
            }
        }
        throw new IllegalArgumentException();
    }

    public void write(MPEGWriter writer, boolean includeOriginalBases) throws IOException {
        short number_of_record_segments = getNumRecordSegments();
        int number_of_alignments = alignPtr.length;

        writer.writeUnsignedByte(number_of_template_segments);
        writer.writeUnsignedByte(number_of_record_segments);
        writer.writeUnsignedShort(number_of_alignments);
        writer.writeByte(data_class.ID);
        writer.writeUnsignedByte((short) readGroup.length());
        writer.writeByte((byte) (read1First? 1 : 0));

        if(number_of_alignments > 0){
            int as_depth = getAsDepth();
            if(mappingSequenceIdentifier.length == 0 || mappingSequenceIdentifier[0].length == 0){
                throw new IllegalArgumentException();
            }
            if(mappingSequenceIdentifier[0][0] == null){
                throw new IllegalArgumentException();
            }
            writer.writeUnsignedShort(mappingSequenceIdentifier[0][0].getSequenceIdentifier());
            writer.writeUnsignedByte(getAsDepth());
        }

        for(int segment_i=0; segment_i < sequenceBytes.length; segment_i++){
            writer.writeBits(sequenceBytes[segment_i].length, 24);
        }

        writer.writeUnsignedByte((short) qualityValues[0][0].length);
        writer.writeUnsignedShort(readName.length());
        writer.writeString(readName);
        writer.writeString(readGroup);

        for(int record_segment=0; record_segment < number_of_record_segments; record_segment++){
            writer.writeBytes(sequenceBytes[record_segment]);
            for(int qs=0; qs < qualityValues[record_segment].length; qs++){
                writer.writeUnsignedBytes(qualityValues[record_segment][qs]);
            }
        }

        for(int alignment_i=0; alignment_i < number_of_alignments; alignment_i++){
            int as_depth = getAsDepth();
            int alignmentIndexFirstSegment = alignPtr[alignment_i][0];

            writer.writeBits(mappingPositions[alignmentIndexFirstSegment][0][0], 40);

            String extendedCigar = ExtendedCigar.getExtendedCigarString(
                    operationType[alignmentIndexFirstSegment][0],
                    operationLength[alignmentIndexFirstSegment][0],
                    mappingPositions[alignmentIndexFirstSegment][0],
                    sequenceBytes[0]
            );

            writer.writeBits(extendedCigar.length(), 24);
            writer.writeString(extendedCigar);
            writer.writeByte(reverseCompliment[alignmentIndexFirstSegment][0][0].id);
            for(int as_i=0; as_i < as_depth; as_i++) {
                writer.writeInt((int)mapping_score[alignmentIndexFirstSegment][0][as_i]);
            }
            if(data_class != DATA_CLASS.CLASS_HM) {
                for(int templateSegment_i = 1; templateSegment_i < number_of_template_segments; templateSegment_i++) {
                    int alignmentIndexCurrentSegment = alignPtr[alignment_i][templateSegment_i];
                    if(splitMate[alignmentIndexCurrentSegment][templateSegment_i] == SplitType.MappedSameRecord){
                        writer.writeByte((byte) 0);
                        writer.writeBits(
                                mappingPositions[alignmentIndexCurrentSegment][templateSegment_i][0]
                                        - mappingPositions[alignmentIndexFirstSegment][0][0],
                                48);
                        String extendedCigarCurrentSegment = ExtendedCigar.getExtendedCigarString(
                                operationType[alignmentIndexFirstSegment][0],
                                operationLength[alignmentIndexFirstSegment][0],
                                mappingPositions[alignmentIndexFirstSegment][0],
                                sequenceBytes[0]
                        );
                        writer.writeBits(extendedCigarCurrentSegment.length(), 24);
                        writer.writeString(extendedCigarCurrentSegment);
                        writer.writeByte(reverseCompliment[alignmentIndexCurrentSegment][templateSegment_i][0].id);
                        for(int as_i=0; as_i < as_depth; as_i++) {
                            writer.writeInt((int)mapping_score[alignmentIndexFirstSegment][0][as_i]);
                        }
                    } else if (splitMate[alignmentIndexCurrentSegment][templateSegment_i]
                            == SplitType.MappedDifferentRecordSameSequence ||
                            splitMate[alignmentIndexCurrentSegment][templateSegment_i]
                                    == SplitType.MappedDifferentRecordDifferentSequence) {
                        writer.writeBits(mappingPositions[alignmentIndexCurrentSegment][templateSegment_i][0], 40);
                        writer.writeUnsignedShort(
                                mappingSequenceIdentifier[alignmentIndexCurrentSegment][templateSegment_i]
                                        .getSequenceIdentifier());
                    } else if (splitMate[alignmentIndexCurrentSegment][templateSegment_i] != SplitType.Unpaired) {
                        throw new IllegalArgumentException();
                    }
                }
            }
            writer.writeByte(flags);
            writer.writeByte((byte) (moreAlignments ? 1 : 0));
            if(hasMoreAlignments()){
                writer.writeBits(moreAlignmentsNextPos, 40);
                writer.writeUnsignedShort(moreAlignmentsNextSeqId.getSequenceIdentifier());
            }

        }

        if(includeOriginalBases) {
            for (int segment_i = 0; segment_i < number_of_template_segments; segment_i++) {
                for (int alignment_i = 0; alignment_i < numberSegmentAlignments[segment_i]; alignment_i++) {
                    if(splitMate[alignment_i][segment_i] == SplitType.MappedSameRecord) {
                        int numSplices = operationType[alignment_i][segment_i].length;
                        for (int splice_i = 0; splice_i < numSplices; splice_i++) {
                            int numOriginalBases = 0;
                            int numOperations = operationType[alignment_i][segment_i][splice_i].length;
                            for (int operation_i = 0; operation_i < numOperations; operation_i++) {
                                byte selectedOperationType = operationType[alignment_i][segment_i][splice_i][operation_i];
                                if (selectedOperationType == Operation.Delete
                                        || selectedOperationType == Operation.Substitution
                                        || selectedOperationType == Operation.SubstitutionToN) {
                                    int selectedOperationLength =
                                            operationLength[alignment_i][segment_i][splice_i][operation_i];
                                    for (int originalBase_i = 0; originalBase_i < selectedOperationLength; originalBase_i++) {
                                        writer.writeByte(originalBase[alignment_i][segment_i][splice_i][numOriginalBases]);
                                        numOriginalBases++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public long[][][] getMappingPositions() {
        return mappingPositions;
    }

    public boolean hasMoreAlignments(){
        return moreAlignments;
    }

    public SequenceIdentifier getMoreAlignmentsNextSeqId(){
        return moreAlignmentsNextSeqId;
    }

    public long getMoreAlignmentsNextPos(){
        return moreAlignmentsNextPos;
    }

    public boolean isSegment1InSameRecord() {
        return splitMate[0][1] == SplitType.MappedSameRecord || splitMate[0][1] == SplitType.UnmappedSameRecord;
    }

    public SequenceIdentifier[][] getMappingSequence() {
        return mappingSequenceIdentifier;
    }


    public SequenceIdentifier getSequenceId() {
        return mappingSequenceIdentifier[0][0];
    }

    public int getNumTemplateSegments() {
        return number_of_template_segments;
    }
}

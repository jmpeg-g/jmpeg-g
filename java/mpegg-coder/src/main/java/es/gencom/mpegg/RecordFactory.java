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

import es.gencom.mpegg.format.SequenceIdentifier;

import java.util.Arrays;

public class RecordFactory {
    public static Record createOneAligned(
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
            boolean[][] reverseCompliment,
            long[][] mapping_score) {

        int[][] alingPtr = new int[mappingPositionsSegment.length][1];
        for(int i=0; i<mappingPositionsSegment.length; i++){
            alingPtr[i][0] = i;
        }

        return new Record(
            readId,
            readName,
            readGroup,
            isRead1,
            true,
            new byte[][]{sequenceBytes},
            new short[][][]{qualityValues},
            sequenceId,
            mappingPositionsSegment,
            null,
            null,
            null,
            new long[][][]{spliceLengths},
            new byte[][][][]{operationType},
            new int[][][][]{operationLength},
            new byte[][][][]{originalBase},
            new boolean[][][]{reverseCompliment},
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
            boolean[][] reverseCompliment,
            long[][] mapping_score,
            int[][] alignPtr) {

        SequenceIdentifier[] sequencesSegment1 = new SequenceIdentifier[mappingPositionsSegment1.length];
        Arrays.fill(sequencesSegment1, sequenceId);
        SplitType[] splitMate = new SplitType[mappingPositionsSegment1.length];
        Arrays.fill(splitMate, SplitType.DifferentRecord);

        return createTwoAlignedSecondOtherRecord(
                readId,
                readName,
                readGroup,
                read1First,
                sequenceBytes,
                qualityValues,
                sequenceId,
                mappingPositionsSegment0,
                splitMate,
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
            boolean[][] reverseCompliment,
            long[][] mapping_score,
            int[][] alignPtr) {

        SplitType[] splitTypes = new SplitType[mappingPositionsSegment1.length];
        Arrays.fill(splitTypes, SplitType.DifferentRecord);

        return createTwoAlignedSecondOtherRecord(
                readId,
                readName,
                readGroup,
                read1First,
                sequenceBytes,
                qualityValues,
                sequenceId,
                mappingPositionsSegment0,
                splitTypes,
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
            long readId,
            String readName,
            String readGroup,
            boolean read1First,
            byte[] sequenceBytes,
            short[][] qualityValues,
            SequenceIdentifier sequenceId,
            long[][] mappingPositionsSegment0,
            SplitType[] splitTypes,
            SequenceIdentifier[] sequencesSegment1,
            long[] mappingPositionsSegment1,
            byte[][][] operationType,
            int[][][] operationLength,
            byte[][][] originalBase,
            long[][][] spliceLengths,
            boolean[][] reverseCompliment,
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
        boolean[][][] reverseComplimentResized = new boolean[][][]{reverseCompliment};
        long[][][] mapping_scoreResized = new long[][][]{mapping_score};



        return new Record(
                readId,
                readName,
                readGroup,
                read1First,
                false,
                new byte[][]{sequenceBytes},
                new short[][][]{qualityValues},
                sequenceId,
                mappingPositionsSegment0,
                splitTypes,
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
            boolean[][] reverseCompliment,
            long[][] mapping_score,
            int uAu_id,
            long uRecord_id) {

        byte[][][][] operationTypeResized = new byte[][][][]{operationType};
        int[][][][] operationLengthResized = new int[][][][]{operationLength};
        byte[][][][] originalBaseResized = new byte[][][][]{originalBase};
        boolean[][][] reverseComplimentResized = new boolean[][][]{reverseCompliment};
        long[][][] mapping_scoreResized = new long[][][]{mapping_score};
        SplitType[] splitType = new SplitType[]{SplitType.UnmappedOtherRecord};

        return new Record(
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
            boolean[][][] reverseCompliment,
            long[][][] mapping_score,
            int[][] alignPtr) {

        SequenceIdentifier[] sequencesSegment1 = new SequenceIdentifier[mappingPositionsSegment1.length];
        Arrays.fill(sequencesSegment1, sequenceId);
        SplitType[] splitMate = new SplitType[mappingPositionsSegment1.length];
        Arrays.fill(splitMate, SplitType.SameRecord);

        return new Record(
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
            boolean[][] reverseCompliment,
            long[][] mapping_score) {

        byte[][][][] operationTypeResized = new byte[][][][]{operationType};
        int[][][][] operationLengthResized = new int[][][][]{operationLength};
        byte[][][][] originalBaseResized = new byte[][][][]{originalBase};
        boolean[][][] reverseComplimentResized = new boolean[][][]{reverseCompliment};
        long[][][] mapping_scoreResized = new long[][][]{mapping_score};
        SplitType[] splitTypes = new SplitType[]{SplitType.UnmappedSameRecord};

        return new Record(
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
    }
}

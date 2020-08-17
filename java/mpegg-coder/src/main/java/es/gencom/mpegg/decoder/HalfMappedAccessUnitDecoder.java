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

import es.gencom.mpegg.Record;
import es.gencom.mpegg.ReverseCompType;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.tokens.TokensStructureDecoder;
import es.gencom.mpegg.decoder.descriptors.streams.*;

import java.io.IOException;

import static es.gencom.mpegg.decoder.MappedAccessUnitDecoder.*;

public class HalfMappedAccessUnitDecoder implements AbstractAccessUnitDecoder {

    private final RlenStream rlenStream;
    private final GenomicPosition initialPosition;
    private final UReadsStream uReadsStream;
    private final TokensStructureDecoder readIdentifierDecoder;

    final protected AbstractSequencesSource sequencesSource;

    final private PosStream posStream;
    final private RCompStream rCompStream;
    final private MMposStream mPosStream;
    final private MMTypeStream mmTypeStream;
    final private ClipsStream clipsStream;
    final private QualityStream qualityStream;
    final private MScoreStream mScoreStream;
    final private RGroupStream rGroupStream;

    final private byte[] changedNucleotides = new byte[]{};
    final private long[] changedPositions = new long[]{};

    final private ALPHABET_ID alphabet_id;
    final private String[] readGroupNames;
    private final PairStream pairStream;
    private final MMapStream mmapStream;
    private final int numberTemplateSegments;
    private byte qv_depth;


    private byte[] originalBases;
    private int encodedOriginalBases;

    private long readCount;

    HalfMappedAccessUnitDecoder(
            ALPHABET_ID alphabet_id,
            GenomicPosition initialPosition,
            AbstractSequencesSource sequencesSource,
            PosStream posStream,
            PairStream pairStream,
            MMapStream mMapStream,
            RlenStream rlenStream,
            RCompStream rCompStream,
            MMposStream mPosStream,
            MMTypeStream mmTypeStream,
            ClipsStream clipsStream,
            UReadsStream uReadsStream,
            QualityStream qualityStream,
            MScoreStream mScoreStream,
            RGroupStream rGroupStream,
            short[][][] tokensReadIdentifiers,
            String[] readGroupNames,
            int numberTemplateSegments,
            byte qv_depth
    ) {
        this.initialPosition = initialPosition;
        this.sequencesSource = sequencesSource;
        this.alphabet_id = alphabet_id;

        this.rlenStream = rlenStream;
        this.posStream = posStream;
        this.rCompStream = rCompStream;
        this.mPosStream = mPosStream;
        this.mmTypeStream = mmTypeStream;
        this.clipsStream = clipsStream;
        this.uReadsStream = uReadsStream;
        this.qualityStream = qualityStream;
        this.mScoreStream = mScoreStream;
        this.rGroupStream = rGroupStream;
        this.readGroupNames = readGroupNames;
        this.pairStream = pairStream;
        this.mmapStream = mMapStream;
        this.numberTemplateSegments = numberTemplateSegments;
        this.qv_depth = qv_depth;

        if(tokensReadIdentifiers == null){
            readIdentifierDecoder = null;
        } else {
            readIdentifierDecoder = new TokensStructureDecoder(tokensReadIdentifiers);
        }
    }

    /**
     * Method to decode the aligned segment of a half-mapped record
     * @param splicedSegLength Array storing the size of each splice
     * @param mmType Matrix storing the types of operation, first dimension is the splice, second is the mutation index
     * @param mmOffsets Matrix storing the position of operation, first dimension is the splice, second is the mutation index
     * @param mmTypeStream stream from which mutated nucleotides can be read.
     * @param softClip Nucleotides conforming the softclips. Length of first dimension must be 2 (start and end), second
     *                 dimension is length of softclip.
     * @param mappingPos First mapped position (0-based) for each splice in each alignment (first dimension alignment,
     *                   second is splice). Only the information for the first alignment is used in this method
     * @param sequenceIdentifier Identifier of sequence within the reference to be used
     * @param sequencesSource Source of reference sequences to use while decoding
     * @param changedNucleotides to which nucleotides are the reference sequences changed to
     * @param changedPositions positions at which the sequence is changed
     * @param alphabet_id identifier of the alphabet used in the access unit
     * @return the decoded segment
     * @throws IOException can be caused by multiple error sources.
     */
    private static SegmentsDecodingResult decode_aligned_segment(
            long[] splicedSegLength,
            int[][] mmType,
            int[][] mmOffsets,
            MMTypeStreamInterface mmTypeStream,
            byte[][] softClip,
            long[][][] mappingPos,
            SequenceIdentifier sequenceIdentifier,
            AbstractSequencesSource sequencesSource,
            byte[] changedNucleotides,
            long[] changedPositions,
            ALPHABET_ID alphabet_id
    ) throws IOException {

        SegmentDecodingResult segmentDecodingResult = MappedAccessUnitDecoder.decode_aligned_segment(
                splicedSegLength,
                mmType,
                mmOffsets,
                mmTypeStream,
                softClip,
                mappingPos[0][0],
                sequenceIdentifier,
                sequencesSource,
                changedNucleotides,
                changedPositions,
                alphabet_id
        );

        return new SegmentsDecodingResult(
                new byte[][][]{segmentDecodingResult.sequence},
                new byte[][][][]{new byte[][][]{segmentDecodingResult.operations}},
                new int[][][][]{new int[][][]{segmentDecodingResult.operationLength}},
                new byte[][][][]{new byte[][][]{segmentDecodingResult.original_nucleotides}});
    }

    @Override
    public boolean hasNext() throws IOException {
        return posStream.hasNext();
    }

    @Override
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

        MMapStreamSymbol mMapStreamSymbol = mmapStream.readSymbol(
                pairStreamFirstSymbol.isUnpairedRead(),
                pairStreamFirstSymbol.getNumberOfRecordSegments()
        );

        int numberOfAlignments = mMapStreamSymbol.getNumberOfAlignments();
        int maxNumberOfSegmentAlignments = mMapStreamSymbol.getMaxNumberOfSegmentAlignments();

        RlenStreamSymbol rlenStreamSymbol = rlenStream.read(
                pairStreamFirstSymbol.getNumberOfRecordSegments(),
                pairStreamFirstSymbol.getNumberOfAlignedRecordSegments(),
                clipsStream.getHard_clips()
        );

        long[][][] mappingPos = posStream.read(
                mMapStreamSymbol.getNumberOfSegmentAlignments(),
                maxNumberOfSegmentAlignments
        );
        SequenceIdentifier[][] mappingSeqIds = new SequenceIdentifier[maxNumberOfSegmentAlignments][numberTemplateSegments];
        mappingSeqIds[0][0] = initialPosition.getSequenceId();
        long[] accessUnitRecord = new long[numberTemplateSegments];
        long[] recordIndex = new long[numberTemplateSegments];
        SplitType[][] splitType = new SplitType[maxNumberOfSegmentAlignments][numberTemplateSegments];

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

        mappingSeqIds[0][1] = mappingSeqIds[0][0];
        mappingPos[0][1][0] = mappingPos[0][0][0];

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

        SegmentsDecodingResult alignedDecodingResult =  decode_aligned_segment(
                rlenStreamSymbol.splicedSegLength[0],
                mmTypesPerSplice[0],
                mmOffsetsPerSlice[0],
                mmTypeStream,
                clipsStream.getSoft_clips()[0],
                mappingPos,
                initialPosition.getSequenceId(),
                sequencesSource,
                changedNucleotides,
                changedPositions,
                alphabet_id
        );

        byte[] decoded_unaligned = uReadsStream.read(
                Math.toIntExact(rlenStreamSymbol.splicedSegLength[1][0]),
                ALPHABET_ID.ALPHABETS[alphabet_id.ID]
        );


        short[][][] qualities = new short[pairStreamFirstSymbol.getNumberOfRecordSegments()][qv_depth][];
        for(int segment_i=0; segment_i < pairStreamFirstSymbol.getNumberOfAlignedRecordSegments(); segment_i++) {
            for(int qvDepth_i=0; qvDepth_i < qv_depth; qvDepth_i++) {
                qualities[segment_i][qvDepth_i] = qualityStream.getQualitiesAligned(
                        alignedDecodingResult.getOperations()[segment_i][0],
                        alignedDecodingResult.getOperationLength()[segment_i][0],
                        mappingPos[segment_i][0],
                        initialPosition.getPosition()
                );
            }
        }
        for(
                int segment_i=pairStreamFirstSymbol.getNumberOfAlignedRecordSegments();
                segment_i < pairStreamFirstSymbol.getNumberOfRecordSegments();
                segment_i++){
            for(int qvDepth_i=0; qvDepth_i < qv_depth; qvDepth_i++) {
                qualities[segment_i][qvDepth_i] = qualityStream.getQualitiesUnaligned(
                        Math.toIntExact(rlenStreamSymbol.read_len[segment_i])
                );
            }
        }


        long[][][] mapping_score = mScoreStream.read(
                pairStreamFirstSymbol.getNumberOfAlignedRecordSegments(),
                mMapStreamSymbol.getNumberOfSegmentAlignments(),
                splitType
        );

        byte[][] decodedSequences = new byte[][]{
                alignedDecodingResult.getDecode_sequences()[0],
                decoded_unaligned
        };

        int rGroupId = rGroupStream.read();

        long[][][] spliceLengths = new long[][][]{rlenStreamSymbol.splicedSegLength};
        byte numberTemplateSegments = 2;
        byte flags = 0;

        readCount++;
        return new Record(
            numberTemplateSegments,
            DATA_CLASS.CLASS_HM,
            readName,
            readGroupNames[rGroupId],
            isRead1First,
            qualities,
            decodedSequences,
            mappingPos,
            mappingSeqIds,
            accessUnitRecord,
            recordIndex,
            splitType,
            spliceLengths,
            alignedDecodingResult.getOperations(),
            alignedDecodingResult.getOperationLength(),
            alignedDecodingResult.getOriginal_nucleotides(),
            rCompSymbols,
            mapping_score,
            mMapStreamSymbol.getAlignPtr(),
            mMapStreamSymbol.getNumberOfSegmentAlignments(),
            flags,
            mMapStreamSymbol.isMoreAlignments(),
            mMapStreamSymbol.getMoreAlignmentsNextSeqId(),
            mMapStreamSymbol.getMoreAlignmentsNextPos()
        );
    }
}

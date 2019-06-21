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

package es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders;

import es.gencom.mpegg.coder.quality.AbstractQualityValueParameterSet;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.coder.tokens.AbstractReadIdentifierEncoder;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.decoder.AbstractSequencesSource;
import es.gencom.mpegg.decoder.descriptors.S_alphabets;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;

import java.io.IOException;
import java.util.Arrays;

public class MappedAccessUnitEncoder extends AbstractAccessUnitEncoder{
    private final AbstractSequencesSource sequencesSource;
    private long previousPosition;
    private final byte[] qualityBookIndexes;

    public MappedAccessUnitEncoder(
        DATA_CLASS auType,
        short sequenceId,
        int auId,
        long auStartPosition,
        long auEndPosition,
        short mm_threshold,
        long extendedStartPosition,
        long extendedEndPosition,
        AbstractSequencesSource sequencesSource,
        DataUnitParameters dataUnitParameters,
        AbstractReadIdentifierEncoder encoder
    ) throws IOException {
        super(
            auType,
            sequenceId,
            auId,
            auStartPosition,
            auEndPosition,
            mm_threshold,
            extendedStartPosition,
            extendedEndPosition,
            dataUnitParameters,
            encoder
        );
        previousPosition = auStartPosition;
        this.sequencesSource = sequencesSource;

        long lengthAU = Long.max(auEndPosition, extendedEndPosition) - Long.min(auStartPosition, extendedStartPosition);
        //this 3 is only to be on the safe side
        qualityBookIndexes = new byte[Math.toIntExact(lengthAU + 3 * mm_threshold)];
        for(int qualityBookIndex_i = 0; qualityBookIndex_i < qualityBookIndexes.length; qualityBookIndex_i++){
            addSymbol(
                    qualityBookIndexes[qualityBookIndex_i],
                    DESCRIPTOR_ID.QV,
                    (byte)1,
                    symbols,
                    number_symbols
            );
        }
        number_symbols[DESCRIPTOR_ID.QV.ID][1] = 0;


    }


    @Override
    protected void writeSpecific(Record record) throws IOException {
        MappedAccessUnitEncoder.writeRecord(
                record,
                auType,
                numberTemplateSegments,
                getReadLength(),
                isSplicedRead(),
                isMultipleAlignment(),
                Math.toIntExact(readCount),
                getAlphabetId(),
                sequencesSource,
                previousPosition,
                symbols,
                number_symbols,
                auxiliaryData_symbols,
                number_auxiliaryDataSymbols,
                getQualityValueParameterSet(),
                qualityBookIndexes,
                getAuStartPosition()

        );
        previousPosition = record.getMappingPositionsSegment0()[0][0];
        readCount++;
    }

    static void writeRecord(
            Record record,
            DATA_CLASS dataClass,
            byte numberTemplateSegments,
            int readLengthParameter,
            boolean splicedReads,
            boolean multipleAlignmentFlag,
            int readCount,
            ALPHABET_ID alphabet_id,
            AbstractSequencesSource sequencesSource,
            long previousPosition,
            long[][][] symbolValues,
            int[][] numberSymbols,
            byte[][][] auxiliaryData,
            int[][] numberAuxiliaryData,
            AbstractQualityValueParameterSet qualityValueParameterSet,
            byte[] qualityBookIndexes,
            long auStartPosition
    ) throws IOException {
        byte recordSegments = record.getRecordSegments();
        if(recordSegments > numberTemplateSegments){
            throw new IllegalArgumentException();
        }
        byte alignedSegments = record.getAlignedSegments();
        if(dataClass == DATA_CLASS.CLASS_HM && (recordSegments != 2 || alignedSegments != 1)){
            throw new IllegalArgumentException();
        }

        writePairFirstSymbol(
                dataClass,
                record,
                numberTemplateSegments,
                symbolValues,
                numberSymbols
        );


        writeClips(
                dataClass,
                readCount,
                alphabet_id,
                record.getSoftclip(),
                record.getHardclip(),
                symbolValues,
                numberSymbols
        );

        writeRlen(
                record.getSpliceLengths(),
                record.getHardclip(),
                readLengthParameter,
                splicedReads,
                symbolValues,
                numberSymbols
        );

        writeMMAP(
                dataClass,
                multipleAlignmentFlag,
                record,
                symbolValues,
                numberSymbols
        );

        long[][] mappingPositionWithSegment = record.getMappingPositionsSegment0();
        long[] mappingPositions = new long[mappingPositionWithSegment.length];
        for(int alignment_i=0; alignment_i < mappingPositions.length; alignment_i++){
            mappingPositions[alignment_i] = mappingPositionWithSegment[alignment_i][0];
        }
        encodeGenomicPosition(
                mappingPositions,
                previousPosition,
                symbolValues,
                numberSymbols
        );

        writeSecondHalfPair(
                dataClass,
                record,
                symbolValues,
                numberSymbols,
                splicedReads
        );

        writeRcomp(record.getReverseCompliment(), symbolValues, numberSymbols);

        writeMMposAndMMtype(
                dataClass,
                record,
                symbolValues,
                numberSymbols,
                auxiliaryData,
                numberAuxiliaryData,
                alphabet_id,
                sequencesSource
        );

        writeQualities(
                record,
                qualityValueParameterSet,
                qualityBookIndexes,
                auStartPosition,
                symbolValues,
                numberSymbols
        );

    }

    private static void writeQualities(
            Record record,
            AbstractQualityValueParameterSet parameterSet,
            byte[] qualityBookIndexes,
            long auStartPosition,
            long[][][] symbols,
            int[][] numSymbols
    ){
        writeQualityAlignedSegment(
                record.getMappingPositionsSegment0()[0],
                record.getOperationLength()[0][0],
                record.getOperationType()[0][0],
                qualityBookIndexes,
                parameterSet,
                record.getQualityValues()[0][0],
                auStartPosition,
                symbols,
                numSymbols
        );
        if(record.getSplitMate()[0] == SplitType.SameRecord) {
            writeQualityAlignedSegment(
                    record.getMappingPositionsSegment1()[0],
                    record.getOperationLength()[1][0],
                    record.getOperationType()[1][0],
                    qualityBookIndexes,
                    parameterSet,
                    record.getQualityValues()[1][0],
                    auStartPosition,
                    symbols,
                    numSymbols
            );
        }
    }

    static void writeQualityAlignedSegment(
            long[] mappingPositions,
            int[][] operationLengths,
            Operation[][] operations,
            byte[] qualityBookIndexes,
            AbstractQualityValueParameterSet parameterSet,
            short[] qualities,
            long auStart,
            long[][][] symbols,
            int[][] numSymbols
    ) {
        addSymbol(
                1,
                DESCRIPTOR_ID.QV,
                (byte)0,
                symbols,
                numSymbols
        );
        for(int splice_i=0; splice_i < mappingPositions.length; splice_i++){
            int currentPosition = Math.toIntExact(mappingPositions[splice_i] - auStart);
            int currentQualityIndex = 0;
            for(int operation_i = 0; operation_i < operations[splice_i].length; operation_i++){
                if(operations[splice_i][operation_i] == Operation.HardClip) {
                    continue;
                } else if (operations[splice_i][operation_i] == Operation.Delete) {
                    currentPosition += operationLengths[splice_i][operation_i];
                    continue;
                }

                byte codebookId;

                if(operations[splice_i][operation_i] == Operation.Insert
                    || operations[splice_i][operation_i] == Operation.SoftClip
                ) {
                    codebookId = (byte) (parameterSet.getNumberQualityBooks()-1);
                } else {
                    codebookId = qualityBookIndexes[currentPosition];
                }




                for(
                        int operationBase_i = 0;
                        operationBase_i < operationLengths[splice_i][operation_i];
                        operationBase_i++
                ) {
                    numSymbols[DESCRIPTOR_ID.QV.ID][1] = Integer.max(
                            numSymbols[DESCRIPTOR_ID.QV.ID][1],
                            Math.toIntExact(currentPosition)
                    );
                    short encoded = parameterSet.getQualityBook(codebookId).encode(qualities[currentQualityIndex]);
                    addSymbol(
                            encoded,
                            DESCRIPTOR_ID.QV,
                            (byte) (codebookId + 2),
                            symbols,
                            numSymbols
                    );
                    currentQualityIndex++;
                }


                if(operations[splice_i][operation_i] == Operation.Match
                        || operations[splice_i][operation_i] == Operation.Substitution
                        || operations[splice_i][operation_i] == Operation.SubstitutionToN
                ){
                    currentPosition++;
                }
            }
        }
    }

    /**
     *
     * @param auType Type of the current Access unit
     * @param record Record for which the information should be written
     * @param templateRecordSegments The number of segments in a template, as transported in the encoding parameters
     * @param symbolValues Memory space for the generated symbols
     * @param numberSymbols Number of symbols in the memory space
     */
    static void writePairFirstSymbol(
            DATA_CLASS auType,
            Record record,
            int templateRecordSegments,
            long[][][] symbolValues,
            int[][] numberSymbols
    ){
        if(templateRecordSegments == 1){
            return;
        }
        if(record.isUnpaired()){
            if(!record.isRead1First()){
                addSymbol(5, DESCRIPTOR_ID.PAIR, (byte)0, symbolValues, numberSymbols);
            }else{
                addSymbol(6, DESCRIPTOR_ID.PAIR, (byte)0, symbolValues, numberSymbols);
            }
            return;
        }
        if(!record.isTwoSegmentsStoredTogether()){
            if(record.isFirstAlignmentSegment1SameSequence()){
                if(!record.isRead1First()){
                    addSymbol(1, DESCRIPTOR_ID.PAIR, (byte)0, symbolValues, numberSymbols);
                }else{
                    addSymbol(2, DESCRIPTOR_ID.PAIR, (byte)0, symbolValues, numberSymbols);
                }
            }else {
                if(!record.isRead1First()){
                    addSymbol(3, DESCRIPTOR_ID.PAIR, (byte)0, symbolValues, numberSymbols);
                }else{
                    addSymbol(4, DESCRIPTOR_ID.PAIR, (byte)0, symbolValues, numberSymbols);
                }
            }
        }else{
            if(record.isSegment1Unmapped()){
                if(record.isTwoSegmentsStoredTogether()) {
                    if (record.isRead1First()) {
                        addSymbol(0, DESCRIPTOR_ID.PAIR, (byte) 0, symbolValues, numberSymbols);
                    } else {
                        addSymbol(1, DESCRIPTOR_ID.PAIR, (byte) 0, symbolValues, numberSymbols);
                    }
                }
            }else {
                addSymbol(0, DESCRIPTOR_ID.PAIR, (byte) 0, symbolValues, numberSymbols);
            }
        }
    }

    static void writeClips(
        DATA_CLASS auType,
        int readCount,
        ALPHABET_ID alphabet_id,
        byte[][][] softClips,
        int[][] hardClips,
        long[][][] symbolValues,
        int[][] numberSymbols
    ){
        boolean hasClips = false;
        for(int segment_i=0; segment_i < softClips.length; segment_i++){
            if(softClips[segment_i].length != 2 || hardClips[segment_i].length != 2){
                throw new IllegalArgumentException();
            }
            if (softClips[segment_i][0].length != 0 || softClips[segment_i][1].length != 0){
                hasClips = true;
            }
            if(hardClips[segment_i][0] != 0 || hardClips[segment_i][1] != 0 ){
                hasClips = true;
            }
        }
        if(hasClips && (auType != DATA_CLASS.CLASS_I && auType != DATA_CLASS.CLASS_HM)){
            throw new IllegalArgumentException();
        }
        if(!hasClips){
            return;
        }
        addSymbol(readCount, DESCRIPTOR_ID.CLIPS, (byte) 0, symbolValues, numberSymbols);
        for(int segment_i=0; segment_i < softClips.length; segment_i++) {
            if (softClips[segment_i][0].length != 0) {
                addSymbol(segment_i << 1, DESCRIPTOR_ID.CLIPS, (byte) 1, symbolValues, numberSymbols);
                for (int symbol_i = 0; symbol_i < softClips[segment_i][0].length; symbol_i++) {
                    byte charToRepresent = softClips[segment_i][0][symbol_i];
                    addSymbol(
                            S_alphabets.charToId(alphabet_id, (char) charToRepresent),
                            DESCRIPTOR_ID.CLIPS,
                            (byte) 2,
                            symbolValues,
                            numberSymbols
                    );
                }
                addSymbol(
                        S_alphabets.alphabets[alphabet_id.ID].length,
                        DESCRIPTOR_ID.CLIPS,
                        (byte) 2,
                        symbolValues,
                        numberSymbols
                );
            }
            if (softClips[segment_i][1].length != 0) {
                addSymbol((segment_i << 1) + 1, DESCRIPTOR_ID.CLIPS, (byte) 1, symbolValues, numberSymbols);
                for (int symbol_i = 0; symbol_i < softClips[segment_i][1].length; symbol_i++) {
                    byte charToRepresent = softClips[segment_i][1][symbol_i];
                    addSymbol(
                            S_alphabets.charToId(alphabet_id, (char) charToRepresent),
                            DESCRIPTOR_ID.CLIPS,
                            (byte) 2,
                            symbolValues,
                            numberSymbols
                    );
                }
                addSymbol(
                        S_alphabets.alphabets[alphabet_id.ID].length,
                        DESCRIPTOR_ID.CLIPS,
                        (byte) 2,
                        symbolValues,
                        numberSymbols
                );
            }
        }
        for(int segment_i=0; segment_i < softClips.length; segment_i++){
            if(hardClips[segment_i][0] != 0){
                addSymbol((segment_i << 1) + 4, DESCRIPTOR_ID.CLIPS, (byte)1, symbolValues, numberSymbols);
                addSymbol(hardClips[segment_i][0], DESCRIPTOR_ID.CLIPS, (byte)3, symbolValues, numberSymbols);
            }
            if(hardClips[segment_i][1] != 0 ){
                addSymbol((segment_i << 1) + 5, DESCRIPTOR_ID.CLIPS, (byte)1, symbolValues, numberSymbols);
                addSymbol(hardClips[segment_i][1], DESCRIPTOR_ID.CLIPS, (byte)3, symbolValues, numberSymbols);
            }
        }
        addSymbol(8, DESCRIPTOR_ID.CLIPS, (byte)1, symbolValues, numberSymbols);
    }

    /**
     * @param spliceLength Matrix of splice lengths: first dimension corresponds to the segment, the second to the
     *                     splice. These lengths do not take into account the hard clips
     * @param hardClipsLength The length of the hardclips operations, the first dimension is the segment, the second is
     *                        the end of the segment (thus must be equal to 2)
     * @param readsLengthParameter The reads length parameter as transported in the encoding parameters
     * @param splicedReads The flag indicating if the reads can be spliced as transported in the encoding parameters
     * @param symbolValues Memory space for the generated symbols
     * @param numberSymbols Number of symbols in the memory space
     */
    static void writeRlen(
            long[][][] spliceLength,
            int[][] hardClipsLength,
            long readsLengthParameter,
            boolean splicedReads,
            long[][][] symbolValues,
            int[][] numberSymbols
    ){
        boolean hasSplicedReads = false;
        for(int segment_i=0; segment_i < spliceLength.length; segment_i++){
            if(spliceLength[segment_i]!= null && spliceLength[segment_i][0].length != 1){
                hasSplicedReads = true;
                break;
            }
        }
        if(hasSplicedReads && readsLengthParameter != 0){
            throw new IllegalArgumentException();
        }

        if(hasSplicedReads && !splicedReads){
            throw new IllegalArgumentException();
        }

        if(readsLengthParameter != 0){
            return;
        }

        long[] totalLengthConsideringHardships = new long[spliceLength.length];
        for(int segment_i=0; segment_i < spliceLength.length; segment_i++){
            if(spliceLength[segment_i] == null){
                continue;
            }

            long totalLengthSegment = 0;
            for(int splice_i=0; splice_i<spliceLength[segment_i][0].length; splice_i++){
                totalLengthSegment += spliceLength[segment_i][0][splice_i];
            }

            addSymbol(
                    totalLengthSegment - hardClipsLength[segment_i][0] - hardClipsLength[segment_i][1] - 1,
                    DESCRIPTOR_ID.RLEN,
                    (byte) 0,
                    symbolValues,
                    numberSymbols
            );
            totalLengthConsideringHardships[segment_i] =
                    totalLengthSegment - hardClipsLength[segment_i][0] - hardClipsLength[segment_i][1];
        }

        if(!splicedReads) return;

        for(int segment_i=0; segment_i < spliceLength.length; segment_i++){
            if(spliceLength[segment_i] == null){
                continue;
            }
            int indexFirstSplice = 0;
            int indexLastSplice = spliceLength[segment_i][0].length - 1;

            addSymbol(
                    totalLengthConsideringHardships[segment_i] - 1,
                    DESCRIPTOR_ID.RLEN,
                    (byte) 0,
                    symbolValues,
                    numberSymbols
            );

            for(int splice_i=0; splice_i<spliceLength[segment_i][0].length; splice_i++){
                long currentSpliceLength = spliceLength[segment_i][0][splice_i];
                if(splice_i == indexFirstSplice){
                    currentSpliceLength -= hardClipsLength[segment_i][0];
                }
                if(splice_i == indexLastSplice){
                    currentSpliceLength -= hardClipsLength[segment_i][1];
                }
                addSymbol(
                        currentSpliceLength - 1,
                        DESCRIPTOR_ID.RLEN,
                        (byte) 0,
                        symbolValues,
                        numberSymbols
                );
            }
        }
    }

    static void writeMMAP(
            DATA_CLASS dataClass,
            boolean multiple_alignment_flag,
            Record record,
            long[][][] symbols,
            int[][] numSymbols
    ){
        if(dataClass != DATA_CLASS.CLASS_U){
            if(multiple_alignment_flag){
                addSymbol(
                        record.getAlignmentsSegment0(),
                        DESCRIPTOR_ID.MMAP,
                        (byte)0,
                        symbols,
                        numSymbols
                );
            } else if( record.getAlignmentsSegment0()!=1){
                throw new IllegalArgumentException();
            }
        }

        int numListedSegment1Index = 0;
        if (
                !(
                        (record.isUnpaired() || dataClass == DATA_CLASS.CLASS_HM)
                        || dataClass == DATA_CLASS.CLASS_U
                )
        ){
            for(int segment0alignment_i=0; segment0alignment_i < record.getAlignmentsSegment0(); segment0alignment_i++){
                if(multiple_alignment_flag){
                    addSymbol(
                            record.getAlignmentIndexesSegment1(segment0alignment_i).length,
                            DESCRIPTOR_ID.MMAP,
                            (byte)0,
                            symbols,
                            numSymbols
                    );
                }
                for(
                        int segment1alignment_i = 0;
                        segment1alignment_i < record.getAlignmentIndexesSegment1(segment0alignment_i).length;
                        segment1alignment_i++
                ){
                    if(segment0alignment_i != 0){
                        int segment1AlignmentIndex =
                                record.getAlignmentIndexesSegment1(segment0alignment_i)[segment1alignment_i];
                        if(
                                segment1AlignmentIndex < 0
                                        || segment1AlignmentIndex > numListedSegment1Index
                        ){
                            throw new IllegalArgumentException();
                        }

                        long toWrite = numListedSegment1Index - segment1alignment_i;
                        if (toWrite == 0){
                            numListedSegment1Index++;
                        }

                        addSymbol(
                                toWrite,
                                DESCRIPTOR_ID.MMAP,
                                (byte)1,
                                symbols,
                                numSymbols
                        );
                    }
                }
            }
        }

        //todo add code for more alignments
    }



    static void encodeGenomicPosition(
            long[] positions,
            long previousPosition,
            long[][][] symbolValues,
            int[][] numberSymbols
    ){
        if(positions.length < 1){
            throw new IllegalArgumentException();
        }
        addSymbol(positions[0]-previousPosition, DESCRIPTOR_ID.POS, (byte) 0, symbolValues, numberSymbols);
        for(int alignment_i=1; alignment_i < positions.length; alignment_i++){
            addSymbol(
                    positions[alignment_i]-positions[alignment_i-1],
                    DESCRIPTOR_ID.POS,
                    (byte) 1,
                    symbolValues,
                    numberSymbols
            );
        }
    }

    static void writeSecondHalfPair(
            DATA_CLASS dataClass,
            Record record,
            long[][][] symbols,
            int[][] numSymbols,
            boolean splicedReads){
        if(dataClass == DATA_CLASS.CLASS_HM){
            addSymbol(
                    record.isRead1First() ? 0 : 1,
                    DESCRIPTOR_ID.MMAP,
                    (byte)1,
                    symbols,
                    numSymbols
            );
        }else{
            if(record.isUnpaired()){
                return;
            }
            if(!record.isTwoSegmentsStoredTogether()){
                if(record.isFirstAlignmentSegment1SameSequence()){
                    if(!record.isRead1First()) {
                        addSymbol(
                                record.getMappingPosSegment1FirstAlignment(),
                                DESCRIPTOR_ID.PAIR,
                                (byte) 2,
                                symbols,
                                numSymbols

                        );
                    }else {
                        addSymbol(
                                record.getMappingPosSegment1FirstAlignment(),
                                DESCRIPTOR_ID.PAIR,
                                (byte) 3,
                                symbols,
                                numSymbols

                        );
                    }
                }else {
                    if(!record.isRead1First()){
                        addSymbol(
                                record.getSequenceIdSegment1FirstAlignment().getSequenceIdentifier(),
                                DESCRIPTOR_ID.PAIR,
                                (byte) 4,
                                symbols,
                                numSymbols

                        );
                        addSymbol(
                                record.getMappingPosSegment1FirstAlignment(),
                                DESCRIPTOR_ID.PAIR,
                                (byte) 6,
                                symbols,
                                numSymbols
                        );
                    }else{
                        addSymbol(
                                record.getSequenceIdSegment1FirstAlignment().getSequenceIdentifier(),
                                DESCRIPTOR_ID.PAIR,
                                (byte) 5,
                                symbols,
                                numSymbols

                        );
                        addSymbol(
                                record.getMappingPosSegment1FirstAlignment(),
                                DESCRIPTOR_ID.PAIR,
                                (byte) 7,
                                symbols,
                                numSymbols
                        );
                    }
                }
            }else{
                long symbolToWrite = Math.abs(
                        record.getMappingPosSegment1FirstAlignment() - record.getMappingPosSegment0FirstAlignment()
                );
                symbolToWrite <<= 1;
                symbolToWrite |= (
                        record.getMappingPosSegment1FirstAlignment() < record.getMappingPosSegment0FirstAlignment()
                )? 1 : 0;
                addSymbol(symbolToWrite, DESCRIPTOR_ID.PAIR, (byte)1, symbols, numSymbols);
            }
        }
        if(
            (
                    dataClass == DATA_CLASS.CLASS_P
                            || dataClass == DATA_CLASS.CLASS_N
                            || dataClass == DATA_CLASS.CLASS_M
                            || dataClass == DATA_CLASS.CLASS_I
            ) && !record.isUnpaired()
        ){
            SequenceIdentifier[] sequenceSegment1 = record.getSequenceIdSegment1();
            long[][] positionsSegment1 = record.getMappingPositionsSegment1();

            int j = 1;
            int currAlignIdx = 0;
            for(int alignmentSegment1_i=1; alignmentSegment1_i < sequenceSegment1.length; alignmentSegment1_i++){
                int alignIdx = record.getAlignmentIndex(alignmentSegment1_i, j);
                if(alignIdx > currAlignIdx){
                    currAlignIdx = alignIdx;
                    if(
                        record.getSequenceId() == record.getSequenceIdSegment1()[alignIdx]
                        && Math.abs(
                            positionsSegment1[alignIdx][0] - record.getMappingPositionsSegment0()
                                            [record.getAlignmentIndex(alignmentSegment1_i, 0)]
                                            [0]
                        ) < 32767
                    ){
                        long symbolToWrite = Math.abs(
                                positionsSegment1[alignIdx][0] -
                                        record.getMappingPositionsSegment0()
                                                [record.getAlignmentIndex(alignmentSegment1_i, 0)]
                                                [0]
                        );
                        symbolToWrite = symbolToWrite << 1;
                        if(
                                record.getMappingPositionsSegment1()[alignIdx][0] <
                                record.getMappingPositionsSegment0()
                                        [record.getAlignmentIndex(alignmentSegment1_i, 0)]
                                        [0]
                        ){
                            symbolToWrite |= 1;
                        }
                        addSymbol(0, DESCRIPTOR_ID.PAIR, (byte)0, symbols, numSymbols);
                        addSymbol(symbolToWrite, DESCRIPTOR_ID.PAIR, (byte)1, symbols, numSymbols);
                    }else if(record.getSequenceId() == record.getSequenceIdSegment1()[alignIdx]){
                        addSymbol(2, DESCRIPTOR_ID.PAIR, (byte)0, symbols, numSymbols);
                        addSymbol(positionsSegment1[alignIdx][0], DESCRIPTOR_ID.PAIR, (byte)3, symbols, numSymbols);
                    }else{
                        addSymbol(4, DESCRIPTOR_ID.PAIR, (byte)0, symbols, numSymbols);
                        addSymbol(
                                record.getSequenceIdSegment1()[alignIdx].getSequenceIdentifier(),
                                DESCRIPTOR_ID.PAIR,
                                (byte)5,
                                symbols,
                                numSymbols
                        );
                        addSymbol(positionsSegment1[alignIdx][0], DESCRIPTOR_ID.PAIR, (byte)7, symbols, numSymbols);
                    }
                }


            }
        }
        if((dataClass == DATA_CLASS.CLASS_I || dataClass == DATA_CLASS.CLASS_HM) && splicedReads){
            for(
                int alignedRecordSegment_i=0;
                alignedRecordSegment_i < record.getAlignedSegments();
                alignedRecordSegment_i++
            ){
                for(
                    int splice_j = 1;
                    splice_j < record.getSpliceLengths()[alignedRecordSegment_i].length;
                    splice_j++
                ){
                    long prevSpliceMappingEnd;
                    if(alignedRecordSegment_i == 0) {
                        prevSpliceMappingEnd = record.getMappingPositionsSegment1()[alignedRecordSegment_i][splice_j-1]
                                + record.getSpliceLengths()[alignedRecordSegment_i][0][splice_j-1];
                        if(
                            Math.abs(
                                    record.getMappingPositionsSegment1()
                                            [alignedRecordSegment_i]
                                            [splice_j] - prevSpliceMappingEnd
                            ) < 32767
                        ){
                            long symbolToWrite = record.getMappingPositionsSegment1()
                                    [alignedRecordSegment_i]
                                    [splice_j] - prevSpliceMappingEnd;
                            symbolToWrite <<= 1;
                            if(
                                record.getMappingPositionsSegment1()
                                        [alignedRecordSegment_i]
                                        [splice_j] < prevSpliceMappingEnd
                            ){
                                symbolToWrite |= 1;
                            }
                            addSymbol(0, DESCRIPTOR_ID.PAIR, (byte)0, symbols, numSymbols);
                            addSymbol(symbolToWrite, DESCRIPTOR_ID.PAIR, (byte)1, symbols, numSymbols);
                        }else{
                            addSymbol(2, DESCRIPTOR_ID.PAIR, (byte)0, symbols, numSymbols);
                            addSymbol(
                                    record.getMappingPositionsSegment1()
                                            [alignedRecordSegment_i][splice_j],
                                    DESCRIPTOR_ID.PAIR, (byte)3, symbols, numSymbols
                            );
                        }
                    }
                }
            }
        }
    }

    static void writeRcomp(
            boolean[][][] reverseComp,
            long[][][] symbols,
            int[][] numSymbols
    ){
        for(
                int alignedRecordSegment_i=0;
                alignedRecordSegment_i < reverseComp.length;
                alignedRecordSegment_i++
        ){
            if(reverseComp[alignedRecordSegment_i] == null){
                continue;
            }
            for(
                int alignment_i=0;
                alignment_i < reverseComp[alignedRecordSegment_i].length;
                alignment_i++
            ){
                for(
                    int splice_i=0;
                    splice_i < reverseComp[alignedRecordSegment_i][alignment_i].length;
                    splice_i++
                ){
                    addSymbol(
                            reverseComp[alignedRecordSegment_i][alignment_i][splice_i] ? 1:0,
                            DESCRIPTOR_ID.RCOMP,
                            (byte)0,
                            symbols,
                            numSymbols
                    );
                }
            }
        }
    }

    static int[][][] combinePerSpliceOperationLength(
        byte[][][][] mmOffsets,
        int[][][] spliceLength
    ){
        int[][][] result = new int[mmOffsets.length][][];
        for(int alignedRecordSegment_i = 0; alignedRecordSegment_i < mmOffsets.length; alignedRecordSegment_i++){
            result[alignedRecordSegment_i] = new int[mmOffsets[alignedRecordSegment_i].length][];
            for(int alignment_i=0; alignment_i<mmOffsets[alignedRecordSegment_i].length; alignment_i++) {
                int numberOffsetsInAlignment = 0;
                for (int splice_i = 0; splice_i < spliceLength[alignedRecordSegment_i][alignment_i].length; splice_i++) {
                    numberOffsetsInAlignment += mmOffsets[alignedRecordSegment_i][alignment_i][splice_i].length;
                }

                result[alignedRecordSegment_i][alignment_i] = new int[numberOffsetsInAlignment];

                int mmOffsets_i = 0;
                byte currentOffset = 0;
                for (
                        int splice_i = 0;
                        splice_i < spliceLength[alignedRecordSegment_i][alignment_i].length;
                        splice_i++
                ) {
                    for (
                            int offset_i = 0;
                            offset_i < mmOffsets[alignedRecordSegment_i][alignment_i][splice_i].length;
                            offset_i++
                    ) {
                        result[alignedRecordSegment_i][alignment_i][mmOffsets_i] =
                                mmOffsets[alignedRecordSegment_i][alignment_i][splice_i][offset_i] +
                                currentOffset;
                        mmOffsets_i++;
                    }
                    currentOffset += spliceLength[alignedRecordSegment_i][alignment_i][splice_i];
                }
            }
        }
        return result;
    }

    static Operation[][][] combinePerSpliceMMType(
            Operation[][][][] mmType
    ){
        Operation[][][] result = new Operation[mmType.length][][];
        for(int alignedRecordSegment_i = 0; alignedRecordSegment_i < mmType.length; alignedRecordSegment_i++){
            result[alignedRecordSegment_i] = new Operation[mmType[alignedRecordSegment_i].length][];
            for(int alignment_i=0; alignment_i<mmType[alignedRecordSegment_i].length; alignment_i++) {
                int numberOffsetsInAlignment = 0;
                for (int splice_i = 0; splice_i < mmType[alignedRecordSegment_i][alignment_i].length; splice_i++) {
                    numberOffsetsInAlignment += mmType[alignedRecordSegment_i][alignment_i][splice_i].length;
                }

                result[alignedRecordSegment_i][alignment_i] = new Operation[numberOffsetsInAlignment];

                int mmOffsets_i = 0;
                for (
                        int splice_i = 0;
                        splice_i < mmType[alignedRecordSegment_i][alignment_i].length;
                        splice_i++
                ) {
                    for (
                            int offset_i = 0;
                            offset_i < mmType[alignedRecordSegment_i][alignment_i][splice_i].length;
                            offset_i++
                    ) {
                        result[alignedRecordSegment_i][alignment_i][mmOffsets_i] =
                                mmType[alignedRecordSegment_i][alignment_i][splice_i][offset_i];
                        mmOffsets_i++;
                    }
                }
            }
        }
        return result;
    }

    private static void writeMMposAndMMtype(
            DATA_CLASS dataClass,
            Record record,
            long[][][] symbols,
            int[][] numSymbols,
            byte[][][] auxiliaryData,
            int[][] numAuxiliaryData,
            ALPHABET_ID alphabet_id,
            AbstractSequencesSource sequencesSource
    ) throws IOException {
        for(int alignedSegment_i=0; alignedSegment_i<record.getAlignedSegments(); alignedSegment_i++){
            writeMMposAndMMtypeSegment(
                dataClass,
                record,
                symbols,
                numSymbols,
                auxiliaryData,
                numAuxiliaryData,
                alphabet_id,
                sequencesSource,
                alignedSegment_i
            );
        }
    }

    static void writeMMposAndMMtypeSegment(
            DATA_CLASS dataClass,
            Record record,
            long[][][] symbols,
            int[][] numSymbols,
            byte[][][] auxiliaryData,
            int[][] numAuxiliaryData,
            ALPHABET_ID alphabet_id,
            AbstractSequencesSource sequencesSource,
            int segment_i
    ) throws IOException {
        int alignment_i = 0;
        int offsetDeltaDueToSplice = 0;
        for(
                int splice_i=0;
                splice_i<record.getOperationType()[segment_i][alignment_i].length;
                splice_i++
        ){
            Operation[] originalOperations = record.getOperationType()[segment_i][alignment_i][splice_i];
            int lengthFirstSoftClip = 0;
            int lengthLastSoftClip = 0;
            int retainFrom;
            if(originalOperations[0] == Operation.HardClip){
                if(originalOperations[1] == Operation.SoftClip){
                    retainFrom = 2;
                    lengthFirstSoftClip = record.getOperationLength()[segment_i][alignment_i][splice_i][1];
                }else{
                    retainFrom = 1;
                }
            }else{
                if(originalOperations[0] == Operation.SoftClip){
                    retainFrom = 1;
                    lengthFirstSoftClip = record.getOperationLength()[segment_i][alignment_i][splice_i][0];
                }else{
                    retainFrom = 0;
                }
            }
            int retainTo;
            if(originalOperations[originalOperations.length-1] == Operation.HardClip){
                if(originalOperations[originalOperations.length-2] == Operation.SoftClip){
                    retainTo = originalOperations.length-2;
                    lengthLastSoftClip = record.getOperationLength()
                            [segment_i]
                            [alignment_i]
                            [splice_i]
                            [originalOperations.length-2];
                }else{
                    retainTo = originalOperations.length-1;
                }
            }else{
                if(originalOperations[originalOperations.length-1] == Operation.SoftClip){
                    retainTo = originalOperations.length-1;
                    lengthLastSoftClip = record.getOperationLength()
                            [segment_i]
                            [alignment_i]
                            [splice_i]
                            [originalOperations.length-1];
                }else{
                    retainTo = originalOperations.length;
                }
            }

            Operation[] operations = new Operation[retainTo - retainFrom];
            int[] operationsLength = new int[retainTo - retainFrom];
            System.arraycopy(originalOperations, retainFrom, operations, 0, retainTo - retainFrom);
            try {
                System.arraycopy(
                        record.getOperationLength()[segment_i][alignment_i][splice_i],
                        retainFrom,
                        operationsLength,
                        0,
                        retainTo - retainFrom
                );
            }catch (ArrayStoreException e){
                throw e;
            }

            long startPos = segment_i == 0 ?
                    record.getMappingPositionsSegment0()[alignment_i][splice_i] :
                    record.getMappingPositionsSegment1()[alignment_i][splice_i];
            long maxLength = record.getSpliceLengths()[segment_i][0][splice_i];
            for(
                    int operation_i=0;
                    operation_i < operations.length;
                    operation_i++
            ){
                if(
                        operations[operation_i] == Operation.Delete
                ){
                    maxLength++;
                }
            }
            Payload reference = sequencesSource.getSubsequenceBytes(
                    record.getSequenceId(),
                    Math.toIntExact(startPos),
                    Math.toIntExact(startPos + maxLength)
            );

            writeMMposAndMMtypeForOneSegmentOneAlignmentOneSplice(
                    dataClass,
                    operations,
                    operationsLength,
                    Arrays.copyOfRange(
                            record.getSequenceBytes()[segment_i],
                            lengthFirstSoftClip,
                            record.getSequenceBytes()[segment_i].length - lengthLastSoftClip
                    ),
                    reference,
                    symbols,
                    numSymbols,
                    auxiliaryData,
                    numAuxiliaryData,
                    alphabet_id,
                    offsetDeltaDueToSplice
            );
            offsetDeltaDueToSplice += record.getSpliceLengths()[segment_i][alignment_i][splice_i];
        }
    }

    static void writeMMposAndMMtypeForOneSegmentOneAlignmentOneSplice(
            DATA_CLASS dataClass,
            Operation[] operations,
            int[] operationLength,
            byte[] nucleotidesRead,
            Payload referenceSequence,
            long[][][] symbols,
            int[][] numSymbols,
            byte[][][] auxiliaryData,
            int[][] numAuxiliaryData,
            ALPHABET_ID alphabet_id,
            long totalSizePreviousSplice
    ) throws IOException {
        if(dataClass == DATA_CLASS.CLASS_P){
            return;
        }

        int positionOnReadSequence = 0;
        int positionOnReferenceSequence = 0;
        int firstOperation = 0;
        if(operations[0] == Operation.HardClip){
            if(operations[1] == Operation.SoftClip){
                positionOnReadSequence = operationLength[1];
                positionOnReferenceSequence = operationLength[1];
                firstOperation = 2;
            }
        }else if(operations[0] == Operation.SoftClip){
            positionOnReadSequence = operationLength[0];
            positionOnReferenceSequence = operationLength[0];
            firstOperation = 1;
        }

        long previousReportedPosition = 0;
        Operation previousOperation = Operation.Match;

        int previousDeletes = 0;

        for(int i=firstOperation; i<operations.length; i++){
            Operation mutationTypeIdentifier;
            byte newBaseIdentifier = 0;
            byte oldBaseIdentifier = 0;
            Operation currentOperation;

            if(operations[i] != Operation.Match && dataClass == DATA_CLASS.CLASS_P){
                throw new IllegalArgumentException();
            }

            if(operations[i] == Operation.Substitution || operations[i] == Operation.SubstitutionToN){
                mutationTypeIdentifier = operations[i];
                oldBaseIdentifier = S_alphabets.charToId(
                        alphabet_id,
                        (char)referenceSequence.readByte()
                );

                newBaseIdentifier = S_alphabets.charToId(
                        alphabet_id,
                        (char) nucleotidesRead[positionOnReadSequence]
                );

                if(nucleotidesRead[positionOnReadSequence] != 'N' && dataClass == DATA_CLASS.CLASS_N){
                    throw new IllegalArgumentException();
                }
                currentOperation = operations[i];
            }else if(operations[i] == Operation.Insert){
                mutationTypeIdentifier = Operation.Insert;
                newBaseIdentifier = S_alphabets.charToId(
                        alphabet_id,
                        (char) nucleotidesRead[positionOnReadSequence]
                );
                if(dataClass!=DATA_CLASS.CLASS_HM && dataClass != DATA_CLASS.CLASS_I){
                    throw new IllegalArgumentException();
                }
                currentOperation = Operation.Insert;
            }else if(operations[i] == Operation.Delete){
                mutationTypeIdentifier = Operation.Delete;
                if(dataClass!=DATA_CLASS.CLASS_HM && dataClass != DATA_CLASS.CLASS_I){
                    throw new IllegalArgumentException();
                }
                currentOperation = Operation.Delete;
            }else if(operations[i] == Operation.Match){
                positionOnReadSequence += operationLength[i];
                positionOnReferenceSequence += operationLength[i];
                referenceSequence.readByte();
                continue;
            }else if(operations[i] == Operation.SoftClip && (i == operations.length-1 || i == operations.length-2)) {
                break;
            }else{
                throw new IllegalArgumentException();
            }


            addSymbol(0, DESCRIPTOR_ID.MMPOS, (byte)0, symbols, numSymbols);

            long symbolValue = positionOnReadSequence + totalSizePreviousSplice
                    - previousReportedPosition
                    -((previousOperation == Operation.Match || previousOperation == Operation.Delete)? 0 : 1)
                    + (previousOperation == Operation.Delete ? 0 : previousDeletes);
            addSymbol(
                    symbolValue,
                    DESCRIPTOR_ID.MMPOS,
                    (byte)1,
                    symbols,
                    numSymbols
            );
            if(operations[i] == Operation.Delete){
                previousDeletes++;
            }else{
                previousDeletes = 0;
            }
            previousOperation = currentOperation;
            previousReportedPosition = positionOnReadSequence + totalSizePreviousSplice;

            if(dataClass != DATA_CLASS.CLASS_N) {
                if (
                        mutationTypeIdentifier == Operation.Substitution || mutationTypeIdentifier == Operation.SubstitutionToN
                ) {
                    addSymbol(0, DESCRIPTOR_ID.MMTYPE, (byte) 0, symbols, numSymbols);
                } else if (mutationTypeIdentifier == Operation.Insert) {
                    addSymbol(1, DESCRIPTOR_ID.MMTYPE, (byte) 0, symbols, numSymbols);
                } else {
                    addSymbol(2, DESCRIPTOR_ID.MMTYPE, (byte) 0, symbols, numSymbols);
                }


                if (
                        (mutationTypeIdentifier == Operation.SubstitutionToN )
                                || mutationTypeIdentifier == Operation.Substitution
                ) {
                    addAuxiliarySymbol((byte) oldBaseIdentifier, DESCRIPTOR_ID.MMTYPE, (byte) 1, auxiliaryData, numAuxiliaryData);
                    addSymbol(newBaseIdentifier, DESCRIPTOR_ID.MMTYPE, (byte) 1, symbols, numSymbols);
                } else if (mutationTypeIdentifier == Operation.Insert) {
                    addSymbol(newBaseIdentifier, DESCRIPTOR_ID.MMTYPE, (byte) 2, symbols, numSymbols);
                }
            }

            if(operations[i] == Operation.Substitution || operations[i] == Operation.SubstitutionToN){
                positionOnReadSequence++;
            }else if(operations[i] == Operation.Insert){
                positionOnReadSequence++;
            }else if(operations[i] == Operation.Delete){
                referenceSequence.readByte();
            }

        }
        addSymbol(1, DESCRIPTOR_ID.MMPOS, (byte)0, symbols, numSymbols);
    }
}

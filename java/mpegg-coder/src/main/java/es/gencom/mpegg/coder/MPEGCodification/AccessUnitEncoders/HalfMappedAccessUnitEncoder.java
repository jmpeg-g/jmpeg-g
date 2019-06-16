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
import es.gencom.mpegg.coder.tokens.AbstractReadIdentifierEncoder;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.decoder.AbstractSequencesSource;
import es.gencom.mpegg.decoder.descriptors.S_alphabets;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;

import java.io.IOException;

import static es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.MappedAccessUnitEncoder.writeQualityAlignedSegment;

public class HalfMappedAccessUnitEncoder extends AbstractAccessUnitEncoder{
    private final AbstractSequencesSource sequencesSource;
    private long previousPosition;
    private final byte[] qualityBookIndexes;

    public HalfMappedAccessUnitEncoder(
            DATA_CLASS auType,
            short sequenceId,
            int auId,
            long auStartPosition,
            long auEndPosition,
            short mm_threshold,
            long extendedStartPosition,
            long extendedEndPosition,
            DataUnitParameters dataUnitParameters,
            AbstractSequencesSource sequencesSource,
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
        this.sequencesSource = sequencesSource;
        previousPosition = auStartPosition;

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
        writeRecord(
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
            long auStart
    ) throws IOException {
        byte recordSegments = record.getRecordSegments();
        if(recordSegments > numberTemplateSegments){
            throw new IllegalArgumentException();
        }
        byte alignedSegments = record.getAlignedSegments();
        if(dataClass != DATA_CLASS.CLASS_HM) {
            throw new IllegalArgumentException();
        }
        if( recordSegments != 2 ){
            throw new IllegalArgumentException();
        }

        MappedAccessUnitEncoder.writePairFirstSymbol(
                dataClass,
                record,
                numberTemplateSegments,
                symbolValues,
                numberSymbols
        );


        writeClipsHalfMapped(
                dataClass,
                readCount,
                alphabet_id,
                record.getSoftclip(),
                record.getHardclip(),
                symbolValues,
                numberSymbols
        );

        writeRlenHalfMapped(
                record.getSpliceLengths(),
                record.getHardclip(),
                readLengthParameter,
                splicedReads,
                symbolValues,
                numberSymbols
        );

        MappedAccessUnitEncoder.writeMMAP(
                dataClass,
                multipleAlignmentFlag,
                record,
                symbolValues,
                numberSymbols
        );

        long[][] mappingPositionWithSplices = record.getMappingPositionsSegment0();
        long[] mappingPositions = new long[mappingPositionWithSplices.length];
        for(int alignment_i=0; alignment_i < mappingPositions.length; alignment_i++){
            mappingPositions[alignment_i] = mappingPositionWithSplices[alignment_i][0];
        }
        MappedAccessUnitEncoder.encodeGenomicPosition(
                mappingPositions,
                previousPosition,
                symbolValues,
                numberSymbols
        );

        writeRcompHalfMapped(record.getReverseCompliment(), symbolValues, numberSymbols);

        writeMMposAndMMtypeHalfMapped(
                dataClass,
                record,
                symbolValues,
                numberSymbols,
                auxiliaryData,
                numberAuxiliaryData,
                alphabet_id,
                sequencesSource
        );

        writeUnmappedMate(
                record,
                symbolValues,
                numberSymbols,
                alphabet_id
        );

        writeQuality(
                record,
                qualityValueParameterSet,
                qualityBookIndexes,
                auStart,
                symbolValues,
                numberSymbols
        );

    }

    private static void writeUnmappedMate(
            Record record,
            long[][][] symbolValues,
            int[][] numberSymbols,
            ALPHABET_ID alphabet_id
    ) {
        for(int base_i = 0; base_i < record.getSequenceBytes()[1].length; base_i++){
            byte encodedBase = S_alphabets.charToId(alphabet_id, (char)record.getSequenceBytes()[1][base_i]);
            addSymbol(
                    encodedBase,
                    DESCRIPTOR_ID.UREADS,
                    (byte)0,
                    symbolValues,
                    numberSymbols
            );
        }
    }

    static void writeClipsHalfMapped(
            DATA_CLASS auType,
            int readCount,
            ALPHABET_ID alphabet_id,
            byte[][][] softClips,
            int[][] hardClips,
            long[][][] symbolValues,
            int[][] numberSymbols
    ){
        boolean hasClips = false;


        if(softClips[0].length != 2 || hardClips[0].length != 2){
            throw new IllegalArgumentException();
        }
        if (softClips[0][0].length != 0 || softClips[0][1].length != 0){
            hasClips = true;
        }
        if(hardClips[0][0] != 0 || hardClips[0][1] != 0 ){
            hasClips = true;
        }

        if(!hasClips){
            return;
        }
        addSymbol(readCount, DESCRIPTOR_ID.CLIPS, (byte) 0, symbolValues, numberSymbols);

        if (softClips[0][0].length != 0) {
            addSymbol(0, DESCRIPTOR_ID.CLIPS, (byte) 1, symbolValues, numberSymbols);
            for (int symbol_i = 0; symbol_i < softClips[0][0].length; symbol_i++) {
                byte charToRepresent = softClips[0][0][symbol_i];
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
        if (softClips[0][1].length != 0) {
            addSymbol( 1, DESCRIPTOR_ID.CLIPS, (byte) 1, symbolValues, numberSymbols);
            for (int symbol_i = 0; symbol_i < softClips[0][1].length; symbol_i++) {
                byte charToRepresent = softClips[0][1][symbol_i];
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


        if(hardClips[0][0] != 0){
            addSymbol( 4, DESCRIPTOR_ID.CLIPS, (byte)1, symbolValues, numberSymbols);
            addSymbol(hardClips[0][0], DESCRIPTOR_ID.CLIPS, (byte)3, symbolValues, numberSymbols);
        }
        if(hardClips[0][1] != 0 ){
            addSymbol( 5, DESCRIPTOR_ID.CLIPS, (byte)1, symbolValues, numberSymbols);
            addSymbol(hardClips[0][1], DESCRIPTOR_ID.CLIPS, (byte)3, symbolValues, numberSymbols);
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
    private static void writeRlenHalfMapped(
            long[][][] spliceLength,
            int[][] hardClipsLength,
            long readsLengthParameter,
            boolean splicedReads,
            long[][][] symbolValues,
            int[][] numberSymbols
    ){
        boolean hasSplicedReads = false;

        if(spliceLength[0][0].length != 1){
            hasSplicedReads = true;
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

        long totalLengthSegment0 = 0;
        for(int splice_i=0; splice_i<spliceLength[0][0].length; splice_i++){
            totalLengthSegment0 += spliceLength[0][0][splice_i];
        }

        addSymbol(
                totalLengthSegment0 - hardClipsLength[0][0] - hardClipsLength[0][1] - 1,
                DESCRIPTOR_ID.RLEN,
                (byte) 0,
                symbolValues,
                numberSymbols
        );

        addSymbol(
                spliceLength[1][0][0],
                DESCRIPTOR_ID.RLEN,
                (byte) 0,
                symbolValues,
                numberSymbols
        );

        if(!splicedReads) return;


    }

    private static void writeRcompHalfMapped(
            boolean[][][] reverseComp,
            long[][][] symbols,
            int[][] numSymbols
    ){
        for(
                int alignment_i=0;
                alignment_i < reverseComp[0].length;
                alignment_i++
        ){
            for(
                    int splice_i=0;
                    splice_i < reverseComp[0][alignment_i].length;
                    splice_i++
            ){
                addSymbol(
                        reverseComp[0][alignment_i][splice_i] ? 1:0,
                        DESCRIPTOR_ID.RCOMP,
                        (byte)0,
                        symbols,
                        numSymbols
                );
            }
        }
    }

    private static void writeMMposAndMMtypeHalfMapped(
            DATA_CLASS dataClass,
            Record record,
            long[][][] symbols,
            int[][] numSymbols,
            byte[][][] auxiliaryData,
            int[][] numAuxiliaryData,
            ALPHABET_ID alphabet_id,
            AbstractSequencesSource sequencesSource
    ) throws IOException {
        MappedAccessUnitEncoder.writeMMposAndMMtypeSegment(
                dataClass,
                record,
                symbols,
                numSymbols,
                auxiliaryData,
                numAuxiliaryData,
                alphabet_id,
                sequencesSource,
                0
        );

    }

    private static void writeQuality(
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

        writeQualityUnalignedSegment(
                parameterSet,
                record.getQualityValues()[1][0],
                symbols,
                numSymbols
        );

    }

    private static void writeQualityUnalignedSegment(
            AbstractQualityValueParameterSet parameterSet,
            short[] qualities,
            long[][][] symbols,
            int[][] numSymbols
    ) {
        addSymbol(1, DESCRIPTOR_ID.QV, (byte) 0, symbols, numSymbols);
        for(int quality_i = 0; quality_i < qualities.length; quality_i++){
            short encoded = parameterSet
                    .getQualityBook(parameterSet.getNumberQualityBooks()-1)
                    .encode(qualities[quality_i]);
            addSymbol(encoded, DESCRIPTOR_ID.QV, (byte) 2, symbols, numSymbols);
        }
    }


}

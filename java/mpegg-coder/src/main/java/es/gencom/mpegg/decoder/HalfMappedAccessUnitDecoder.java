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
import es.gencom.mpegg.RecordFactory;
import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.Operation;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.tokens.TokensStructureDecoder;
import es.gencom.mpegg.decoder.descriptors.S_alphabets;
import es.gencom.mpegg.decoder.descriptors.streams.*;

import java.io.IOException;

import static es.gencom.mpegg.decoder.MappedAccessUnitDecoder.*;

public class HalfMappedAccessUnitDecoder extends AbstractAccessUnitDecoder {
    final private long au_id;
    private final RlenStream rlenStream;
    private final GenomicPosition initialPosition;
    private final UReadsStream uReadsStream;
    private final TokensStructureDecoder readIdentifierDecoder;
    private GenomicPosition currentPosition;

    final protected AbstractSequencesSource sequencesSource;

    final private PosStream posStream;
    final private RCompStream rCompStream;
    final private MMposStream mPosStream;
    final private MMTypeStream mmTypeStream;
    final private ClipsStream clipsStream;
    final private QualityStream qualityStream;

    final private byte[] changedNucleotides = new byte[]{};
    final private long[] changedPositions = new long[]{};

    final private ALPHABET_ID alphabet_id;


    private byte[] originalBases;
    private int encodedOriginalBases;

    private long readCount;

    public HalfMappedAccessUnitDecoder(
        long au_id,
        ALPHABET_ID alphabet_id,
        GenomicPosition initialPosition,
        AbstractSequencesSource sequencesSource,
        PosStream posStream,
        RlenStream rlenStream,
        RCompStream rCompStream,
        MMposStream mPosStream,
        MMTypeStream mmTypeStream,
        ClipsStream clipsStream,
        UReadsStream uReadsStream,
        QualityStream qualityStream,
        short[][][] tokensReadIdentifiers
    ) {
        this.au_id = au_id;
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

        if(tokensReadIdentifiers == null){
            readIdentifierDecoder = null;
        } else {
            readIdentifierDecoder = new TokensStructureDecoder(tokensReadIdentifiers);
        }
    }

    static SegmentsDecodingResult decode_aligned_segment(
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
            ALPHABET_ID alphabet_id
    ) throws IOException {
        Operation[][][] operations = new Operation[1][][];
        int[][][] operationLength = new int[1][][];
        byte[][] decode_sequences;
        byte[][][] original_nucleotides = new byte[1][][];
        int[][] length_original_nucleotides = new int[1][];

        decode_sequences = MappedAccessUnitDecoder.decode_aligned_segment(
                splicedSegLength,
                mmType,
                mmOffsets,
                mmTypeStream,
                softClip,
                mappingPos,
                sequenceIdentifier,
                sequencesSource,
                changedNucleotides,
                changedPositions,
                alphabet_id,
                operations,
                operationLength,
                original_nucleotides,
                length_original_nucleotides
        );

        return new SegmentsDecodingResult(
                new byte[][][]{decode_sequences},
                new Operation[][][][]{operations},
                new int[][][][]{operationLength},
                new byte[][][][]{original_nucleotides}
        );
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

        short numberOfRecordSegments = 2;
        short numberOfAlignedSegments = 1;

        clipsStream.read(readCount, numberOfRecordSegments, numberOfAlignedSegments);
        RlenStreamSymbol rlenStreamSymbol = rlenStream.read(
                numberOfRecordSegments,
                numberOfAlignedSegments,
                clipsStream.getHard_clips()
        );
        long[][] positions = posStream.read(new int[]{1});

        long[][][] resizedSplicedSegLength = new long[rlenStreamSymbol.getSplicedSegLength().length][1][];
        for(
                int segment_i = 0;
                segment_i < resizedSplicedSegLength.length;
                segment_i++
        ){
            resizedSplicedSegLength[segment_i][0] = rlenStreamSymbol.getSplicedSegLength()[segment_i];
        }
        boolean[][][] rCompSymbols = rCompStream.read(
                new int[]{1,0},
                resizedSplicedSegLength
        );

        int[][] mmOffsets = mPosStream.read(numberOfAlignedSegments);
        int[][] mmTypes = mmTypeStream.readMMType(mmOffsets);
        correctMmOffsetsByType(mmOffsets, mmTypes);
        int[][][] mmOffsetsPerSlice = correctMmOffsetsBySplices(mmOffsets, rlenStreamSymbol.getSplicedSegLength());
        int[][][] mmTypesPerSplice = correctMMTypesPerSlice(mmTypes, mmOffsetsPerSlice);


        SegmentsDecodingResult alignedDecodingResult =  decode_aligned_segment(
                rlenStreamSymbol.getSplicedSegLength()[0],
                mmTypesPerSplice[0],
                mmOffsetsPerSlice[0],
                mmTypeStream,
                clipsStream.getSoft_clips()[0],
                positions,
                initialPosition.getSequenceId(),
                sequencesSource,
                changedNucleotides,
                changedPositions,
                alphabet_id
        );

        byte[] decoded_unaligned = uReadsStream.read(
                Math.toIntExact(rlenStreamSymbol.getSplicedSegLength()[0][0]),
                S_alphabets.alphabets[alphabet_id.ID]
        );

        short[][][] qualityValues = new short[2][1][];
        qualityValues[0][0] = qualityStream.getQualitiesAligned(
                alignedDecodingResult.getOperations()[0][0],
                alignedDecodingResult.getOperationLength()[0][0],
                positions[0],
                initialPosition.getPosition()
        );
        qualityValues[1][0] = qualityStream.getQualitiesUnaligned(decoded_unaligned.length);

        long[][][] spliceLength = new long[2][1][];
        spliceLength[0][0] = rlenStreamSymbol.getSplicedSegLength()[0];
        spliceLength[1][0] = new long[]{rlenStreamSymbol.getRead_len()[1]};

        readCount++;
        return RecordFactory.createOneAlignedOneUnmapped(
            readCount,
            readName,
            "",
            true,
            new byte[][]{
                    alignedDecodingResult.getDecode_sequences()[0],
                    decoded_unaligned
            },
            qualityValues,
            initialPosition.getSequenceId(),
            positions,
            alignedDecodingResult.getOperations()[0],
            alignedDecodingResult.getOperationLength()[0],
            alignedDecodingResult.getOriginal_nucleotides()[0],
            spliceLength,
            rCompSymbols[0],
            new long[][]{{0},{0}}
        );
    }
}

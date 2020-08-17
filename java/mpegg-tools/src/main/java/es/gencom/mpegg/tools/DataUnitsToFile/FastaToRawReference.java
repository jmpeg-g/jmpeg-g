package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.io.WritableMSBitChannel;
import es.gencom.mpegg.dataunits.DataUnitRawReference;
import es.gencom.mpegg.dataunits.DataUnits;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;

public class FastaToRawReference {
    public static void convert(Path inputFastaPath, Path outputPath) throws IOException {
        FastaFileReader fastaReader = new FastaFileReader(inputFastaPath);

        int[] sequenceIds = new int[23];
        Payload[] sequences = new Payload[23];
        long[] startPos = new long[23];
        long[] endPos = new long[23];

        int numberSequences = 0;
        for(FastaSequence fastaSequence : fastaReader){
            sequenceIds[numberSequences] = numberSequences;
            sequences[numberSequences] = new Payload(fastaSequence.sequence);
            startPos[numberSequences] = 0;
            endPos[numberSequences] = fastaSequence.sequence.length;

            numberSequences++;

            if(numberSequences == sequenceIds.length){
                sequenceIds = Arrays.copyOf(sequenceIds, numberSequences*2);
                sequences = Arrays.copyOf(sequences, numberSequences*2);
                startPos = Arrays.copyOf(startPos, numberSequences*2);
                endPos = Arrays.copyOf(endPos, numberSequences*2);
            }
        }

        sequenceIds = Arrays.copyOf(sequenceIds, numberSequences);
        sequences = Arrays.copyOf(sequences, numberSequences);
        startPos = Arrays.copyOf(startPos, numberSequences);
        endPos = Arrays.copyOf(endPos, numberSequences);

        DataUnits dataUnits = new DataUnits();
        DataUnitRawReference dataUnitRawReference = new DataUnitRawReference(
                dataUnits,
                sequenceIds,
                sequences,
                startPos,
                endPos
        );

        MPEGWriter writer = new WritableMSBitChannel(
                Files.newByteChannel(outputPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        );
        dataUnitRawReference.writeDataUnitContent(writer);
    }
}

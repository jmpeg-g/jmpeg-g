package es.gencom.mpegg.tools;

import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.dataunits.DataUnitRawReference;
import es.gencom.mpegg.format.ref.ExternalReference;
import es.gencom.mpegg.format.ref.REFERENCE_TYPE;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FormatReferenceToRawReference {

    public static DataUnitRawReference convert(ExternalReference reference) 
            throws IOException {

        DataUnitRawReference rawReference;
        if (reference.reference_type == REFERENCE_TYPE.MPEGG_REF) {
            System.err.println("Reference cannot be directly extracted");
            return null;
        }

        if (reference.reference_type == REFERENCE_TYPE.RAW_REF) {
            MPEGReader readerOriginal = new ReadableMSBitFileChannel(
                    FileChannel.open(Paths.get(reference.getReferenceURI())));

            rawReference = DataUnitRawReference.read(readerOriginal, null);
        } else {
            final String uri = reference.getReferenceURI().replace("fa","rawReference");
            final Path path = Paths.get(uri);
            if(Files.exists(path)) {
                MPEGReader reader = new ReadableMSBitFileChannel(FileChannel.open(path));
                rawReference = DataUnitRawReference.read(reader, null);
            } else {
                rawReference = FASTAToRawReference(reference);
            }
        }

        return rawReference;
    }

    public static DataUnitRawReference FASTAToRawReference(ExternalReference reference) 
            throws IOException {
        return FASTAToRawReference(reference.getReferenceURI());
    }

    public static DataUnitRawReference FASTAToRawReference(String fastaPath) 
            throws IOException {

        FastaFileReader fastaReader = new FastaFileReader(Paths.get(fastaPath));

        int[] sequence_ids = new int[23];
        Payload[] sequences = new Payload[23];
        long[] startPos = new long[23];
        long[] endPos = new long[23];
        int sequence_id = 0;
        for(FastaSequence fastaSequence : fastaReader) {
            sequence_ids[sequence_id]= sequence_id;
            sequences[sequence_id] = new Payload(fastaSequence.sequence);
            startPos[sequence_id] = 0;
            endPos[sequence_id] = fastaSequence.sequence.length;
            sequence_id++;
            if(sequence_ids.length == sequence_id){
                sequence_ids = Arrays.copyOf(sequence_ids, sequence_id*2);
                sequences = Arrays.copyOf(sequences, sequence_id*2);
                startPos = Arrays.copyOf(startPos, sequence_id*2);
                endPos = Arrays.copyOf(endPos, sequence_id*2);
            }
        }

        return new DataUnitRawReference(null, sequence_ids, sequences, startPos, endPos);
    }
}

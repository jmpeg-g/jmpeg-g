package es.gencom.mpegg.tools;

import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.coder.dataunits.DataUnitRawReference;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.format.ref.ExternalLocation;
import es.gencom.mpegg.format.ref.InternalLocation;
import es.gencom.mpegg.format.ref.REFERENCE_TYPE;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

public class FormatReferenceToRawReference {
    public static DataUnitRawReference convert(Reference reference) throws IOException {
        DataUnitRawReference rawReference;
        if (reference.getLocation() instanceof InternalLocation) {
            throw new IllegalArgumentException();
        } else {
            ExternalLocation externalLocation = (ExternalLocation) reference.getLocation();
            if (externalLocation.getReferenceType() == REFERENCE_TYPE.MPEGG_REF) {
                throw new IllegalArgumentException();
            } else if (externalLocation.getReferenceType() == REFERENCE_TYPE.RAW_REF) {

                MPEGReader readerOriginal = new ReadableMSBitFileChannel(
                        FileChannel.open(Paths.get(externalLocation.getRef_uri()))
                );
                rawReference = DataUnitRawReference.read(
                        readerOriginal,
                        null
                );

            } else {
                String URI = externalLocation.getRef_uri().replace("fa","rawReference");
                File file = Paths.get(URI).toFile();
                if(file.exists()){
                    MPEGReader reader = new ReadableMSBitFileChannel(FileChannel.open(file.toPath()));
                    rawReference = DataUnitRawReference.read(reader, null);
                }else {
                    rawReference = FASTAToRawReference(reference);
                }
            }
        }
        return rawReference;
    }

    public static DataUnitRawReference FASTAToRawReference(
            Reference reference
    ) throws IOException {
        return FASTAToRawReference(((ExternalLocation) reference.getLocation()).getRef_uri());
    }

    public static DataUnitRawReference FASTAToRawReference(
            String fastaPath
    ) throws IOException {
        FastaFileReader fastaReader = new FastaFileReader(
                Paths.get(fastaPath)
        );

        int[] sequence_ids = new int[23];
        Payload[] sequences = new Payload[23];
        long[] startPos = new long[23];
        long[] endPos = new long[23];
        int sequence_id = 0;
        for(FastaSequence fastaSequence : fastaReader){
            SequenceIdentifier sequenceIdentifier = new SequenceIdentifier(sequence_id);


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

        return new DataUnitRawReference(
                null,
                sequence_ids,
                sequences,
                startPos,
                endPos
        );
    }
}

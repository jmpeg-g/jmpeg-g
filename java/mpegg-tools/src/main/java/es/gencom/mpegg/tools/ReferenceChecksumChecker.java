package es.gencom.mpegg.tools;

import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Base64;

import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.dataunits.DataUnitRawReference;
import es.gencom.mpegg.format.ref.*;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ReferenceChecksumChecker {
    public static boolean check(CheckedExternalReference reference) throws IOException {
        return check(reference, System.out);
    }

    public static boolean check(CheckedExternalReference reference, PrintStream outputStream) throws IOException {

        MessageDigest md;
        try {
            switch(reference.getChecksumAlgorithm()) {
                case MD5: md = MessageDigest.getInstance("MD5"); break;
                case SHA256: md = MessageDigest.getInstance("SHA-256"); break;
                default: throw new UnsupportedOperationException();
            }
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
        
        final int num_sequences = reference.getNumberSequences();

        if(reference.reference_type == REFERENCE_TYPE.FASTA_REF) {
            FastaFileReader fastaReader = new FastaFileReader(
                    Paths.get(reference.getReferenceURI()));
            
            int sequence_id = 0;
            for(FastaSequence fastaSequence : fastaReader) {
                if(sequence_id >= num_sequences) {
                    System.err.println("FASTA being validated against reference has too many sequences");
                    return false;
                }

                byte[] providedValue = reference.getReferenceSequenceChecksum(sequence_id);
                byte[] calculatedValue = md.digest(fastaSequence.sequence);
                outputStream.println(Base64.getEncoder().encodeToString(providedValue));
                if(!Arrays.equals(providedValue, calculatedValue)) {
                    return false;
                }
                sequence_id++;
            }
            if(sequence_id != num_sequences) {
                System.err.println("FASTA being validated against reference has not enough sequences");
                return false;
            }
            return true;
        } else {
            DataUnitRawReference rawReference = FormatReferenceToRawReference.convert(reference);

            for (int sequence_i = 0; sequence_i < num_sequences; sequence_i++) {
                Payload dataPayload = rawReference.getSequence(sequence_i);
                for (int byteBuffer_i = 0; byteBuffer_i < dataPayload.getByteBuffers().length; byteBuffer_i++) {
                    ByteBuffer byteBuffer = dataPayload.getByteBuffers()[byteBuffer_i];
                    byteBuffer.position(0);
                    md.update(byteBuffer);
                }

                byte[] providedValue = reference.getReferenceSequenceChecksum(sequence_i);
                byte[] calculatedValue = md.digest();
                outputStream.println(Base64.getEncoder().encodeToString(providedValue));
                if (!Arrays.equals(providedValue, calculatedValue)) {
                    return false;
                }
            }
            return true;
        }
    }
}

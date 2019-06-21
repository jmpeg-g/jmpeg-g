package es.gencom.mpegg.tools;

import java.util.Base64;
import es.gencom.mpegg.coder.dataunits.DataUnitRawReference;
import es.gencom.mpegg.format.ChecksumAlgorithm;
import es.gencom.mpegg.format.ref.*;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ReferenceChecksumChecker {
    public static boolean check(Reference reference) throws IOException {
        if(reference.getLocation() instanceof InternalLocation){
            throw new IllegalArgumentException();
        }
        ExternalLocation externalLocation = (ExternalLocation) reference.getLocation();

        ChecksumAlgorithm checksumAlgorithm = externalLocation.getChecksum_alg();

        if(externalLocation.getReference_type() == REFERENCE_TYPE.MPEGG_REF){
            throw new UnsupportedOperationException();
        }
        if(externalLocation.getExtRef_info() instanceof MPEGG_ExtRef_info){
            throw new IllegalArgumentException();
        }

        RawOrFasta_ExtRef_info extRef_info = (RawOrFasta_ExtRef_info) externalLocation.getExtRef_info();
        DataUnitRawReference rawReference = FormatReferenceToRawReference.convert(reference);
        int num_sequences = reference.getNumberSequences();
        for(int sequence_i=0; sequence_i < num_sequences; sequence_i++){
            MessageDigest md;
            try {
                if (checksumAlgorithm == ChecksumAlgorithm.MD5) {
                    md = MessageDigest.getInstance("MD5");
                } else {
                    md = MessageDigest.getInstance("SHA-256");
                }
            }catch (NoSuchAlgorithmException e){
                throw new UnsupportedOperationException(e);
            }
            Payload dataPayload = rawReference.getSequence(sequence_i);
            for(int byteBuffer_i=0; byteBuffer_i < dataPayload.getByteBuffers().length; byteBuffer_i++) {
                ByteBuffer byteBuffer = dataPayload.getByteBuffers()[byteBuffer_i];
                byteBuffer.position(0);
                md.update(byteBuffer);
            }

            byte[] providedValue = extRef_info.getChecksum(sequence_i);
            byte[] calculatedValue = md.digest();
            System.out.println(Base64.getEncoder().encodeToString(providedValue));
            if(!Arrays.equals(providedValue, calculatedValue)){
                return false;
            }
        }
        return true;
    }
}

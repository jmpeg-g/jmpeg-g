package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.format.ChecksumAlgorithm;
import es.gencom.mpegg.format.DatasetGroupContainer;
import es.gencom.mpegg.format.ref.FASTA_Reference;
import es.gencom.mpegg.format.ref.Reference;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FASTAToFASTAReference {
    public static Reference generate(
            DatasetGroupContainer datasetGroupContainer,
            Path inputFastaPath,
            String reference_name,
            short reference_major_version,
            short reference_minor_version,
            short reference_patch_version,
            String reportedPath,
            ChecksumAlgorithm checksumAlgorithm
    ) throws IOException {
        FastaFileReader fastaReader = new FastaFileReader(inputFastaPath);

        byte maxAllocatedReferenceId = -1;
        for(Reference reference : datasetGroupContainer.getReferences()){
            maxAllocatedReferenceId = (byte) Integer.max(
                    maxAllocatedReferenceId,
                    reference.getReferenceId()
            );
        }

        int numberSequences = 0;
        String[] sequence_names = new String[23];
        byte[][] checksums = new byte[23][];

        for(FastaSequence fastaSequence : fastaReader){
            sequence_names[numberSequences] = fastaSequence.getSequenceName();
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
            checksums[numberSequences] = md.digest(fastaSequence.sequence);
            numberSequences++;
            if(sequence_names.length == numberSequences){
                sequence_names = Arrays.copyOf(sequence_names, sequence_names.length*2);
                checksums = Arrays.copyOf(checksums, checksums.length*2);
            }
        }
        sequence_names = Arrays.copyOf(sequence_names, numberSequences);
        checksums = Arrays.copyOf(checksums, numberSequences);

        Reference reference = new FASTA_Reference(
                datasetGroupContainer.getDatasetGroupHeader().getDatasetGroupId(),
                (byte) (maxAllocatedReferenceId + 1),
                reference_name,
                reference_major_version,
                reference_minor_version,
                reference_patch_version,
                sequence_names,
                reportedPath,
                checksumAlgorithm,
                checksums);

        datasetGroupContainer.addReference(reference);

        return reference;
    }
}

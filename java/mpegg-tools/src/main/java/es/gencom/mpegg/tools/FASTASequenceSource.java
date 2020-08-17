package es.gencom.mpegg.tools;

import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.decoder.AbstractSequencesSource;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FASTASequenceSource extends AbstractSequencesSource {
    private final String[] sequenceNames;
    private final byte[][] sequences;

    public FASTASequenceSource(Path fastaPath) throws IOException {
        FastaFileReader fastaFileReader = new FastaFileReader(fastaPath);

        int sequenceCount = 0;
        String[] sequenceNamesTmp = new String[23];
        byte[][] sequencesTmp = new byte[23][];

        for(FastaSequence fastaSequence : fastaFileReader){
            sequenceNamesTmp[sequenceCount] = fastaSequence.getSequenceName();
            sequencesTmp[sequenceCount] = fastaSequence.sequence;
            sequenceCount++;
            if(sequenceNamesTmp.length == sequenceCount){
                sequenceNamesTmp = Arrays.copyOf(sequenceNamesTmp, sequenceNamesTmp.length*2);
                sequencesTmp = Arrays.copyOf(sequencesTmp, sequenceNamesTmp.length*2);
            }
        }

        sequenceNames = Arrays.copyOf(sequenceNamesTmp, sequenceCount);
        sequences = Arrays.copyOf(sequencesTmp, sequenceCount);
    }

    @Override
    public SequenceIdentifier getSequenceIdentifier(String sequenceName) {
        for(int sequence_i=0; sequence_i < sequenceNames.length; sequence_i++){
            if(sequenceNames[sequence_i].equalsIgnoreCase(sequenceName)){
                return new SequenceIdentifier(sequence_i);
            }
        }
        return null;
    }

    @Override
    protected Payload getSequence(SequenceIdentifier sequenceIdentifier) {
        return new Payload(ByteBuffer.wrap(sequences[sequenceIdentifier.getSequenceIdentifier()]));
    }

    @Override
    public Payload getSubsequence(SequenceIdentifier sequenceIdentifier, int startPos, int endPos) throws IOException {
        return new Payload(
                ByteBuffer.wrap(
                        AbstractSequencesSource.obtainSubsequence(
                                sequences[sequenceIdentifier.getSequenceIdentifier()], startPos, endPos)
                )
        );
    }
}

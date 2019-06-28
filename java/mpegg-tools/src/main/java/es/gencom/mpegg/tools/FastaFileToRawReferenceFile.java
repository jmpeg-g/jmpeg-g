package es.gencom.mpegg.tools;

import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.coder.dataunits.DataUnitRawReference;
import es.gencom.mpegg.io.Payload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.EnumSet;

public class FastaFileToRawReferenceFile {
    private final Path rawReferencePath;
    private final String[] sequenceNames;

    public FastaFileToRawReferenceFile(String fastaPath) throws IOException {
        File rawReferenceFile = File.createTempFile("convertedFasta", ".rawReference");
        rawReferenceFile.deleteOnExit();


        FileChannel rawReferenceOutputChannel = FileChannel.open(
                rawReferenceFile.toPath(),
                EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.TRUNCATE_EXISTING)
        );
        rawReferenceOutputChannel.write(ByteBuffer.allocate(8));
        rawReferenceOutputChannel.write(ByteBuffer.allocate(2));
        ByteBuffer bufferRawReferenceSize = rawReferenceOutputChannel.map(FileChannel.MapMode.READ_WRITE, 0, 8);
        ByteBuffer bufferSeqCount = rawReferenceOutputChannel.map(FileChannel.MapMode.READ_WRITE, 8, 2);
        rawReferenceOutputChannel.position(10);

        FastaFileReader fastaFileReader = new FastaFileReader(Paths.get(fastaPath));
        long size = 8+2;
        short sequenceCount = 0;
        String[] sequenceNamesTmp = new String[23];
        for(FastaSequence fastaSequence : fastaFileReader){
            ByteBuffer byteBuffer = ByteBuffer.allocate(2+5+5).order(ByteOrder.BIG_ENDIAN);
            size += 2 + 5+ 5;
            byteBuffer.putShort(sequenceCount);
            byteBuffer.put((byte) 0);
            byteBuffer.putInt(0);
            byteBuffer.put((byte) 0);
            byteBuffer.putInt(fastaSequence.length);
            byteBuffer.rewind();
            rawReferenceOutputChannel.write(byteBuffer);
            rawReferenceOutputChannel.write(ByteBuffer.wrap(fastaSequence.sequence));
            size += fastaSequence.length;

            sequenceNamesTmp[sequenceCount] = fastaSequence.getSequenceName();
            sequenceCount++;

            if(sequenceNamesTmp.length == sequenceCount){
                sequenceNamesTmp = Arrays.copyOf(sequenceNamesTmp, sequenceNamesTmp.length*2);
            }
        }
        sequenceNames = Arrays.copyOf(sequenceNamesTmp, sequenceCount);

        bufferRawReferenceSize.putLong(size);
        bufferSeqCount.putShort(sequenceCount);
        rawReferenceOutputChannel.close();

        rawReferencePath = rawReferenceFile.toPath();
    }

    public Path getRawReferencePath() {
        return rawReferencePath;
    }

    public String[] getSequenceNames() {
        return sequenceNames;
    }
}

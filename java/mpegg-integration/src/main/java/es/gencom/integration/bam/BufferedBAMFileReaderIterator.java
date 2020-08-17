package es.gencom.integration.bam;

import java.util.Iterator;

public class BufferedBAMFileReaderIterator implements Iterator<BAMRecord> {
    private final BufferedBAMFileReader bufferedBAMFileReader;

    public BufferedBAMFileReaderIterator(BufferedBAMFileReader bufferedBAMFileReader) {
        this.bufferedBAMFileReader = bufferedBAMFileReader;
    }

    @Override
    public boolean hasNext() {
        return bufferedBAMFileReader.hasNext();
    }

    @Override
    public BAMRecord next() {
        return bufferedBAMFileReader.next();
    }
}

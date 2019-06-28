package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMRecord;
import es.gencom.mpegg.decoder.Exceptions.InvalidSymbolException;
import es.gencom.mpegg.decoder.Exceptions.MissingRequiredDescriptorException;

import java.io.IOException;
import java.util.Iterator;
import java.util.zip.DataFormatException;

public class MPEGGDecodingIterator implements Iterator<BAMRecord> {
    private final MPEGGdecodingTask mpegGdecodingTask;
    private BAMRecord bamRecord;

    public MPEGGDecodingIterator(MPEGGdecodingTask mpegGdecodingTask) throws DataFormatException, MissingRequiredDescriptorException, InvalidSymbolException, IOException {
        this.mpegGdecodingTask = mpegGdecodingTask;
        this.bamRecord = mpegGdecodingTask.getNextSAMRecord();
    }



    @Override
    public boolean hasNext() {
        try {
            return mpegGdecodingTask.hasNext();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public BAMRecord next() {
        if(bamRecord == null){
            try {
                bamRecord = mpegGdecodingTask.getNextSAMRecord();
            }catch (Exception e){
                throw new InternalError(e);
            }
        }
        BAMRecord currentRecord = bamRecord;
        bamRecord = null;
        return currentRecord;
    }
}

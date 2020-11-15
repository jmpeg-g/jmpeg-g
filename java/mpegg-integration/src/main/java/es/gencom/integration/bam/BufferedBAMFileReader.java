package es.gencom.integration.bam;

import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;

public class BufferedBAMFileReader implements Iterable<BAMRecord>{
    private final BAMFileReader bamFileReader;
    private final Queue<BAMRecord> bufferedRecords;
    private final Map<String, List<BAMRecord>> recordsByName;
    private final Iterator<BAMRecord> inputBAMRecords;

    public BufferedBAMFileReader(BAMFileReader bamFileReader) {
        this(100000, bamFileReader);
    }
    
    public BufferedBAMFileReader(int bufferedSize, BAMFileReader bamFileReader) {
        this.bamFileReader = bamFileReader;
        this.recordsByName = new HashMap<>(); ;
        bufferedRecords = new ArrayDeque<>();

        inputBAMRecords = bamFileReader.iterator();
        int numRecords = 0;
        while(numRecords < bufferedSize && inputBAMRecords.hasNext()){
            BAMRecord newRecord = inputBAMRecords.next();
            numRecords++;
            bufferedRecords.add(newRecord);
            List<BAMRecord> bamRecords = recordsByName.computeIfAbsent(newRecord.getQName(), k -> new ArrayList<>());
            bamRecords.add(newRecord);
        }
    }

    private BAMRecord getNextIfPossible(){
        if(inputBAMRecords.hasNext()){
            return inputBAMRecords.next();
        } else {
            return null;
        }
    }

    @Override
    public Iterator<BAMRecord> iterator() {
        return new BufferedBAMFileReaderIterator(this);
    }

    public boolean hasNext() {
        return bufferedRecords.size() > 0;
    }

    public BAMRecord next() {

        BAMRecord recordToReturn = bufferedRecords.poll();
        if(recordToReturn == null){
            throw new InternalError();
        }

        List records = recordsByName.get(recordToReturn.getQName());
        records.remove(recordToReturn);
        if(records.size() == 0){
            recordsByName.remove(recordToReturn.getQName());
        }

        BAMRecord nextRecord = getNextIfPossible();
        if(nextRecord != null){
            bufferedRecords.add(nextRecord);
            List<BAMRecord> bamRecordsForName = recordsByName.computeIfAbsent(nextRecord.getQName(), k -> new ArrayList<>());
            bamRecordsForName.add(nextRecord);
        }
        return recordToReturn;
    }

    public BAMHeader getBAMHeader() {
        return bamFileReader.getBAMHeader();
    }

    public BAMRecord searchByName(String qName, int refID, int position) throws IOException, DataFormatException {
        List<BAMRecord> bamRecords = recordsByName.get(qName);
        if(bamRecords != null) {
            for (BAMRecord bamRecord : bamRecords) {
                if (bamRecord.getRefID() == refID && bamRecord.getPositionStart() == position) {
                    bamRecords.remove(bamRecord);
                    return bamRecord;
                }
            }
        }
        for (BAMRecord possibleOtherAlignment : bamFileReader.search(refID, position, position)) {
            if (possibleOtherAlignment.getQName().equals(qName)) {
                return possibleOtherAlignment;
            }
        }
        return null;
    }

    public long getMaxDistanceToPosition(int nextRefID, int nextPositionStart) {
        return bamFileReader.getMaxDistanceToPosition(nextRefID, nextPositionStart);
    }

    public List<BAMRecord> search(int refID, int from, int to) throws IOException, DataFormatException {
        return bamFileReader.search(refID, from, to);
    }

    public boolean hasByName(String qName, int refID, int position) {
        List<BAMRecord> bamRecords = recordsByName.get(qName);
        if(bamRecords == null) {
            return false;
        }
        for (BAMRecord bamRecord : bamRecords) {
            if (bamRecord.getRefID() == refID && bamRecord.getPositionStart() == position) {
                return true;
            }
        }
        return false;
    }
}

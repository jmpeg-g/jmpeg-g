package es.gencom.mpegg.tools;

import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;

import java.util.Arrays;

public class RequiredRanges {
    private SequenceIdentifier[] sequenceIdentifiers;
    private long[] starts;
    private long[] ends;
    private DATA_CLASS[][] data_classes;
    private int size;

    public RequiredRanges(){
        sequenceIdentifiers = new SequenceIdentifier[32];
        starts = new long[32];
        ends = new long[32];
        data_classes = new DATA_CLASS[32][];
        size = 0;
    }

    public void addRequiredRange(
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ) {
        addRequiredRange(
                sequenceIdentifier,
                start,
                end,
                null
        );
    }

    public void addRequiredRange(
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end,
            DATA_CLASS[] data_class
    ) {
        sequenceIdentifiers[size] = sequenceIdentifier;
        starts[size] = start;
        ends[size] = end;
        data_classes[size] = data_class;


        size++;
        if(sequenceIdentifiers.length == size){
            sequenceIdentifiers = Arrays.copyOf(sequenceIdentifiers, size*2);
            starts = Arrays.copyOf(starts, size*2);
            ends = Arrays.copyOf(ends, size*2);
            data_classes = Arrays.copyOf(data_classes, size*2);
        }
    }

    public boolean isRangeRequired(
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end,
            DATA_CLASS data_class
    ){
        for(int i=0; i < size; i++){
            if(sequenceIdentifiers[i].equals(sequenceIdentifier)){
                long maxStart = Long.max(start, starts[i]);
                long minEnd = Long.min(end, ends[i]);
                if(maxStart <= minEnd){
                    if(data_classes[i]==null || data_class == null) {
                        return true;
                    }
                }
                if(data_classes[i] != null) {
                    for (DATA_CLASS rangeDataClass : data_classes[i]) {
                        if (rangeDataClass == data_class) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public SequenceIdentifier[] getSequenceIdentifiers() {
        return sequenceIdentifiers;
    }

    public long[] getStarts() {
        return starts;
    }

    public long[] getEnds() {
        return ends;
    }

    public int getSize() {
        return size;
    }

    public DATA_CLASS[][] getData_classes() {
        return data_classes;
    }
}

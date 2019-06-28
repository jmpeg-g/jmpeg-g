package es.gencom.mpegg.tools;

import es.gencom.mpegg.format.SequenceIdentifier;

import java.util.Arrays;

public class RequiredRanges {
    private SequenceIdentifier[] sequenceIdentifiers;
    private long[] starts;
    private long[] ends;
    private int size;

    public RequiredRanges(){
        sequenceIdentifiers = new SequenceIdentifier[32];
        starts = new long[32];
        ends = new long[32];
        size = 0;
    }

    public void addRequiredRange(
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ) {
        sequenceIdentifiers[size] = sequenceIdentifier;
        starts[size] = start;
        ends[size] = end;

        size++;
        if(sequenceIdentifiers.length == size){
            sequenceIdentifiers = Arrays.copyOf(sequenceIdentifiers, size*2);
            starts = Arrays.copyOf(starts, size*2);
            ends = Arrays.copyOf(ends, size*2);
        }
    }

    public boolean isRangeRequired(
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ){
        for(int i=0; i < size; i++){
            if(sequenceIdentifiers[i] == sequenceIdentifier){
                long maxStart = Long.max(start, starts[i]);
                long minEnd = Long.min(end, ends[i]);
                if(maxStart <= minEnd){
                    return true;
                }
            }
        }
        return false;
    }
}

package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DatasetGroupContainer;
import es.gencom.mpegg.format.MPEGFile;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.tools.RequiredRanges;

import java.io.IOException;

public class RangeSelector {
    private final Reference reference;

    public RangeSelector() throws IOException {
        reference = null;
    }

    public RangeSelector(final Reference reference) {
        this.reference = reference;
    }

    public void addRequiredRange(
            RequiredRanges requiredRanges,
            String referenceName,
            long start,
            long end) {
        addRequiredRange(requiredRanges, referenceName, start, end, null);
    }

    public void addRequiredRange(
            RequiredRanges requiredRanges,
            String referenceName,
            long start,
            long end,
            DATA_CLASS[] data_classes) {

        String[] sequenceNames = reference.getSequenceNames();
        for(int sequence_i=0; sequence_i < sequenceNames.length; sequence_i++){
            if(sequenceNames[sequence_i].equals(referenceName)){
                requiredRanges.addRequiredRange(
                        new SequenceIdentifier(sequence_i),
                        start,
                        end,
                        data_classes
                );
                return;
            }
        }
        throw new IllegalArgumentException();
    }


    public void addRequiredRange(
            RequiredRanges requiredRanges,
            short sequenceId,
            long start,
            long end) {
        addRequiredRange(requiredRanges, sequenceId, start, end, null);
    }

    public void addRequiredRange(
            RequiredRanges requiredRanges,
            short sequenceId,
            long start,
            long end,
            DATA_CLASS[] data_classes
    ){
        requiredRanges.addRequiredRange(
                new SequenceIdentifier(sequenceId),
                start,
                end,
                data_classes
        );
    }
}

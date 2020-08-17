package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMRecord;

class InformationAlignment{
    private final int sequenceId;
    private final long position;
    private final boolean unmapped;
    private final boolean reverseCompliment;
    private BAMRecord bamRecord;

    InformationAlignment() {
        this(true, 0, 0, false, null);
    }

    InformationAlignment(BAMRecord bamRecord) {
        this(true, 0, 0, false, bamRecord);
    }

    InformationAlignment(int sequenceId, long position, boolean reverseCompliment) {
        this(false, sequenceId, position, reverseCompliment, null);
    }

    InformationAlignment(int sequenceId, long position, boolean reverseCompliment, BAMRecord bamRecord) {
        this(false, sequenceId, position, reverseCompliment, bamRecord);
    }

    InformationAlignment(
            boolean unmapped,
            int sequenceId,
            long position,
            boolean reverseCompliment,
            BAMRecord samRecord
    ) {
        this.unmapped = unmapped;
        this.sequenceId = sequenceId;
        this.position = position;
        this.reverseCompliment = reverseCompliment;
        this.bamRecord = samRecord;
    }

    boolean isReverseCompliment() {
        return reverseCompliment;
    }

    BAMRecord getBamRecord() {
        return bamRecord;
    }

    boolean hasRecord(){
        return getBamRecord() != null;
    }

    void setSAMRecord(BAMRecord bamRecord) {
        this.bamRecord = bamRecord;
    }

    boolean isUnmapped() {
        return unmapped;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    long getPosition() {
        return position;
    }
}

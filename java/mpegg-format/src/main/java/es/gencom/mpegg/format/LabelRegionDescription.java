package es.gencom.mpegg.format;

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;
import java.util.Arrays;

public  class LabelRegionDescription{
    private short sequenceId;
    private DATA_CLASS[] classIds;
    private long start_pos;
    private long end_pos;

    public LabelRegionDescription(){

    }

    public LabelRegionDescription(short sequenceId, DATA_CLASS[] classIds, int start_pos, int end_pos) {
        this.sequenceId = sequenceId;
        this.classIds = classIds;
        this.start_pos = start_pos;
        this.end_pos = end_pos;
    }

    public LabelRegionDescription read(final MPEGReader reader) throws IOException {
        sequenceId = reader.readShort();
        int numRegions = (int) reader.readBits(4) & 0xFF;
        classIds = new DATA_CLASS[numRegions];
        for(int region_i = 0; region_i < numRegions; region_i++){
            classIds[region_i] = DATA_CLASS.getDataClass((byte) reader.readBits(4));
        }
        start_pos = reader.readBits(40);
        end_pos = reader.readBits(40);

        return this;
    }

    public void write(final MPEGWriter writer) throws IOException {
        writer.writeShort(sequenceId);
        byte numClasses = (byte) classIds.length;
        writer.writeBits(numClasses, 4);
        for(DATA_CLASS classId : classIds){
            writer.writeBits(classId.ID, 4);
        }
        writer.writeBits(start_pos, 40);
        writer.writeBits(end_pos, 40);
        writer.flush();
    }

    /**
     * @return Returns the sizeInBits in bits
     */
    public long sizeInBits(){
        long sizeInBits = 0;
        sizeInBits += 16; //seqId
        sizeInBits += 4; //numClasses;
        sizeInBits += classIds.length * 4; //each ClassId
        sizeInBits += 40; //startPos
        sizeInBits += 40; //endPos

        return sizeInBits;
    }

    public short getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(short sequenceId) {
        this.sequenceId = sequenceId;
    }

    public DATA_CLASS[] getClassIds() {
        return classIds;
    }

    public void setClassIds(DATA_CLASS[] classIds) {
        this.classIds = classIds;
    }

    public long getStart_pos() {
        return start_pos;
    }

    public void setStart_pos(long start_pos) {
        this.start_pos = start_pos;
    }

    public long getEnd_pos() {
        return end_pos;
    }

    public void setEnd_pos(long end_pos) {
        this.end_pos = end_pos;
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof LabelRegionDescription))return false;
        LabelRegionDescription castedOther = (LabelRegionDescription)other;
        return sequenceId == castedOther.sequenceId &&
                Arrays.equals(classIds ,castedOther.classIds) &&
                start_pos == castedOther.start_pos &&
                end_pos == castedOther.end_pos;
    }
}

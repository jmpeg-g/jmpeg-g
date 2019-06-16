/**
 * *****************************************************************************
 * Copyright (C) 2019 Spanish National Bioinformatics Institute (INB) and
 * Barcelona Supercomputing Center
 *
 * Modifications to the initial code base are copyright of their respective
 * authors, or their employers as appropriate.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *****************************************************************************
 */

package es.gencom.mpegg.format;

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * <p>
 * A label associated to one or more Datasets (ISO/IEC DIS 23092-1 6.4.1.4.3 Label).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class Label extends GenInfo<Label> {
    
    public final static String KEY = "lbll";

    public static class LabelDatasetLocation {
        public static class LabelRegionDescription{
            private short sequenceId;
            private byte[] classIds;
            private long start_pos;
            private long end_pos;

            public LabelRegionDescription(){

            }

            public LabelRegionDescription(short sequenceId, byte[] classIds, int start_pos, int end_pos) {
                this.sequenceId = sequenceId;
                this.classIds = classIds;
                this.start_pos = start_pos;
                this.end_pos = end_pos;
            }

            public LabelRegionDescription read(final MPEGReader reader) throws IOException {
                sequenceId = reader.readShort();
                int numRegions = (int) reader.readBits(4) & 0xFF;
                classIds = new byte[numRegions];
                for(int region_i = 0; region_i < numRegions; region_i++){
                    classIds[region_i] = (byte) reader.readBits(4);
                }
                start_pos = reader.readBits(40);
                end_pos = reader.readBits(40);

                return this;
            }

            public void write(final MPEGWriter writer) throws IOException {
                writer.writeShort(sequenceId);
                byte numClasses = (byte) classIds.length;
                writer.writeBits(numClasses, 4);
                for(byte classId : classIds){
                    writer.writeBits(classId, 4);
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

            public byte[] getClassIds() {
                return classIds;
            }

            public void setClassIds(byte[] classIds) {
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
        short datasetId;
        LabelRegionDescription[] regionDescriptions;

        public LabelDatasetLocation() {

        }

        public LabelDatasetLocation(short datasetId, LabelRegionDescription[] regionDescriptions) {
            this.datasetId = datasetId;
            this.regionDescriptions = regionDescriptions;
        }

        public LabelDatasetLocation read(MPEGReader reader) throws IOException {
            datasetId = reader.readShort();

            int numberRegions = (int) reader.readBits(8) & 0xFF;
            regionDescriptions = new LabelRegionDescription[numberRegions];
            for(int region_i=0; region_i < numberRegions; region_i++){
                regionDescriptions[region_i] = new LabelRegionDescription();
                regionDescriptions[region_i].read(reader);
            }
            return this;
        }

        public void write(MPEGWriter writer) throws IOException {
            writer.writeShort(datasetId);

            writer.writeByte((byte) regionDescriptions.length);
            for(LabelRegionDescription regionDescription : regionDescriptions){
                regionDescription.write(writer);
            }
        }

        /**
         * @return returns the sizeInBits in bits
         */
        public long sizeInBits(){
            long sizeInBits = 0;
            sizeInBits += 16; //datasetId

            sizeInBits += 8; //numRegions
            for(LabelRegionDescription regionDescription : regionDescriptions){
                sizeInBits += regionDescription.sizeInBits();
            }
            return sizeInBits;
        }

        public short getDatasetId() {
            return datasetId;
        }


        public LabelRegionDescription[] getRegionDescriptions() {
            return regionDescriptions;
        }

        public void setDatasetId(short datasetId) {
            this.datasetId = datasetId;
        }

        public void setRegionDescriptions(LabelRegionDescription[] regionDescriptions) {
            this.regionDescriptions = regionDescriptions;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof LabelDatasetLocation)) return false;

            LabelDatasetLocation castedOther = (LabelDatasetLocation) other;
            return getDatasetId() == castedOther.getDatasetId() &&
                    Arrays.equals(getRegionDescriptions(), castedOther.getRegionDescriptions());
        }

    }

    private String labelId;
    private LabelDatasetLocation[] labelDatasetLocations;

    public Label() {
        super(KEY);
    }

    public Label(final String labelId, 
                 final LabelDatasetLocation[] labelDatasetLocations) {

        super(KEY);

        this.labelId = labelId;
        this.labelDatasetLocations = labelDatasetLocations;
    }

    @Override
    protected long size() {
        long sizeInBits = 0;
        sizeInBits += labelId.toCharArray().length*8;
        sizeInBits += 8; //Null string terminator
        sizeInBits += 16; //numdatasets
        for(LabelDatasetLocation labelDatasetLocation: labelDatasetLocations){
            sizeInBits += labelDatasetLocation.sizeInBits();
        }
        return (sizeInBits >> 3) + (sizeInBits % 8 != 0 ? 1:0);
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeNTString(labelId);
        writer.writeShort((short) labelDatasetLocations.length);
        for(LabelDatasetLocation labelDatasetLocation : labelDatasetLocations){
            labelDatasetLocation.write(writer);
        }
    }

    @Override
    public Label read(final MPEGReader reader, final long size) throws IOException, ParsedSizeMismatchException {
        labelId = reader.readNTString();

        short numDatasets = reader.readShort();
        labelDatasetLocations = new LabelDatasetLocation[numDatasets];
        for(short dataset_i=0; dataset_i<numDatasets; dataset_i++){
            labelDatasetLocations[dataset_i] = new LabelDatasetLocation();
            labelDatasetLocations[dataset_i].read(reader);
        }

        if(size != size()){
            throw new ParsedSizeMismatchException();
        }
        return this;
    }

    public String getLabelId() {
        return labelId;
    }

    public void setLabelId(final String labelId) {
        this.labelId = labelId;
    }

    public LabelDatasetLocation[] getLabelDatasetLocations() {
        return labelDatasetLocations;
    }

    public void setLabelDatasetLocations(final LabelDatasetLocation[] labelDatasetLocations) {
        this.labelDatasetLocations = labelDatasetLocations;
    }
    
    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Label)) return false;

        Label castedOther = (Label) other;
        return getLabelId().equals(castedOther.getLabelId()) &&
                Arrays.equals(getLabelDatasetLocations(), castedOther.getLabelDatasetLocations());
    }
}

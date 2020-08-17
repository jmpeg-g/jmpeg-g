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

public class LabelDatasetLocation {
    
    private short datasetId;
    private LabelRegionDescription[] regionDescriptions;

    public LabelDatasetLocation() {

    }

    public LabelDatasetLocation(
            final short datasetId, 
            final LabelRegionDescription[] regionDescriptions) {

        this.datasetId = datasetId;
        this.regionDescriptions = regionDescriptions;
    }

    public short getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(short datasetId) {
        this.datasetId = datasetId;
    }

    public LabelRegionDescription[] getRegionDescriptions() {
        return regionDescriptions;
    }

    public void setRegionDescriptions(LabelRegionDescription[] regionDescriptions) {
        this.regionDescriptions = regionDescriptions;
    }

    public LabelDatasetLocation read(final MPEGReader reader) throws IOException {
        datasetId = reader.readShort();

        int numberRegions = (int) reader.readBits(8) & 0xFF;
        regionDescriptions = new LabelRegionDescription[numberRegions];
        for(int region_i=0; region_i < numberRegions; region_i++){
            regionDescriptions[region_i] = new LabelRegionDescription();
            regionDescriptions[region_i].read(reader);
        }
        return this;
    }

    public void write(final MPEGWriter writer) throws IOException {
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
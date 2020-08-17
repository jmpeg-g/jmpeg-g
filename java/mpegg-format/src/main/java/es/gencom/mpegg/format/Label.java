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

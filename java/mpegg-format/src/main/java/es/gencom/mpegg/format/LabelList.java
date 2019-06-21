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
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The list of labels (ISO/IEC DIS 23092-1 6.4.1.4 Label List).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class LabelList extends GenInfo {
    
    public final static String KEY = "labl";

    private byte dataset_group_id; // u(8)
    private List<Label> labels;
    
    public LabelList() {
        super(KEY);
    }
    
    public byte getDatasetGroupId() {
        return dataset_group_id;
    }
    
    public void setDatasetGroupId(final byte dataset_group_id) {
        this.dataset_group_id = dataset_group_id;
    }
    
    public List<Label> getLabels() {
        if (labels == null) {
            labels = new ArrayList<>();
        }
        return labels;
    }

    public void setLabels(final List<Label> labels) {
        this.labels = labels;
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException, InvalidMPEGStructure {
        writer.writeByte(dataset_group_id);
        
        if (labels == null || labels.isEmpty()) {
            writer.writeUnsignedShort(0);
        } else {
            writer.writeUnsignedShort(labels.size());
            for (Label label : labels) {
                label.writeWithHeader(writer);
            }
        }
    }

    @Override
    public LabelList read(final MPEGReader reader, final long size) throws IOException, InvalidMPEGStructure, ParsedSizeMismatchException {

        dataset_group_id = reader.readByte();
        final int num_labels = reader.readUnsignedShort();
        if (num_labels > 0) {
            labels = new ArrayList<>(num_labels);
            for (int i = 0; i < num_labels; i++) {
                final Header header = Header.read(reader);
                if(! header.key.equals(Label.KEY)){
                    throw new InvalidMPEGStructure("Key info different to "+Label.KEY+" in labelList");
                }
                final Label label = new Label().read(reader, header.getContentSize());
                labels.add(label);
            }
        }
        if(size != size()){
            throw new ParsedSizeMismatchException();
        }
        
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if(!(obj instanceof LabelList)){
            return false;
        }
        LabelList labelList = (LabelList)obj;

        if (getDatasetGroupId() != labelList.getDatasetGroupId()){
            return false;
        }

        if (getLabels().size() != labelList.getLabels().size()){
            return false;
        }

        boolean labelsEquals = true;

        int label_i=0;
        for(Label label : getLabels()){
            Label toCompare = labelList.getLabels().get(label_i);
            if (!(label.equals(toCompare))){
                labelsEquals = false;
                break;
            }
            label_i++;
        }

        return labelsEquals;
    }

    @Override
    public long size(){
        long result = 0;
        result += 1; //dataset_group_id
        if (labels == null || labels.isEmpty()) {
            result += 2; //labels.size()
        } else {
            result += 2; //labels.size()
            for (Label label : labels) {
                result += label.sizeWithHeader();
            }
        }
        return result;
    }
}

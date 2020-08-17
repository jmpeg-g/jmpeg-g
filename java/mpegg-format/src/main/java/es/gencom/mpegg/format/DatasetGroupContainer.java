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

import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A high-level container that holds datasets (ISO/IEC DIS 23092-1 6.4.1 Dataset group).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DatasetGroupContainer extends GenInfo<DatasetGroupContainer> {
    
    public final static String KEY = "dgcn";
    
    private DatasetGroupHeader dataset_group_header;
    private List<Reference> references;
    private ReferenceMetadata reference_metadata;
    private LabelList label_list;
    private List<DatasetContainer> datasetContainers;
    private DatasetGroupMetadata datasetGroupMetadata;
    private DatasetGroupProtection datasetGroupProtection;

    public DatasetGroupContainer() {
        super(KEY);
        
        dataset_group_header = new DatasetGroupHeader();
        datasetContainers = new ArrayList<>();
        references = new ArrayList<>();
    }

    public DatasetGroupProtection getDatasetGroupProtection() {
        return datasetGroupProtection;
    }

    public List<DatasetContainer> getListDatasetContainer() {
        return datasetContainers;
    }

    public DatasetContainer getDatasetContainerByIndex(final int index) {
        return datasetContainers.get(index);
    }

    public DatasetContainer getDatasetContainerById(final int datasetId) {
        for(DatasetContainer datasetContainer: datasetContainers){
            if(datasetContainer.getDatasetHeader().getDatasetId() == datasetId){
                return datasetContainer;
            }
        }
        return null;
    }

    public void addDatasetContainer(DatasetContainer datasetContainer){
        datasetContainers.add(datasetContainer);
        dataset_group_header.addDatasetId(datasetContainer.getDatasetHeader().getDatasetId());
    }
            
    public DatasetGroupHeader getDatasetGroupHeader() {
        return dataset_group_header;
    }

    public void setDatasetGroupHeader(
            final DatasetGroupHeader dataset_group_header) {
        this.dataset_group_header = dataset_group_header;
    }

    public DatasetGroupMetadata getDatasetGroupMetadata(){
        return datasetGroupMetadata;
    }

    public void setDatasetGroupMetadata(DatasetGroupMetadata datasetGroupMetadata) {
        this.datasetGroupMetadata = datasetGroupMetadata;
    }
    
    public List<Reference> getReferences() {
        if (references == null) {
            references = new ArrayList<>();
        }
        return references;
    }

    public Reference getReference(final int reference_id){
        return references.get(reference_id);
    }

    public void addReference(final Reference reference){
        references.add(reference);
    }
        
    public ReferenceMetadata getReferenceMetadata() {
        return reference_metadata;
    }
    
    public void setReferenceMetadata(
            final ReferenceMetadata reference_metadata) {
        this.reference_metadata = reference_metadata;
    }

    public LabelList getLabelList() {
        if (label_list == null) {
            label_list = new LabelList();
        }
        return label_list;
    }
    
    public void setLabelList(final LabelList label_list) {
        this.label_list = label_list;
    }

    public List<DatasetContainer> getDatasetContainers() {
        if (datasetContainers == null) {
            datasetContainers = new ArrayList<>();
        }
        return datasetContainers;
    }

    @Override
    public long size() {
        long result = 0;
        result += dataset_group_header.sizeWithHeader();
        for(Reference reference : references) {
            result += reference.sizeWithHeader();
        }
        if (reference_metadata != null) {
            result += reference_metadata.sizeWithHeader();
        }
        if (label_list != null) {
            result += label_list.sizeWithHeader();
        }
        for(DatasetContainer datasetContainer : datasetContainers) {
            result += datasetContainer.sizeWithHeader();
        }
        if (datasetGroupMetadata != null) {
            result += datasetGroupMetadata.sizeWithHeader();
        }
        if (datasetGroupProtection != null) {
            result += datasetGroupProtection.sizeWithHeader();
        }
        return result;
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException, InvalidMPEGStructureException {
        dataset_group_header.writeWithHeader(writer);
        for(Reference reference : references) {
            reference.writeWithHeader(writer);
        }
        if (reference_metadata != null) {
            reference_metadata.writeWithHeader(writer);
        }
        if (label_list != null) {
            label_list.writeWithHeader(writer);
        }
        for(DatasetContainer datasetContainer : datasetContainers) {
            datasetContainer.writeWithHeader(writer);
        }
        if (datasetGroupMetadata != null) {
            datasetGroupMetadata.writeWithHeader(writer);
        }
        if (datasetGroupProtection != null) {
            datasetGroupProtection.writeWithHeader(writer);
        }
    }

    @Override
    public DatasetGroupContainer read(final MPEGReader reader, final long size) 
            throws IOException, InvalidMPEGStructureException, ParsedSizeMismatchException, InvalidMPEGGFileException {

        references = new ArrayList<>();
        
        Header header = Header.read(reader);

        if (!DatasetGroupHeader.KEY.equals(header.key)){
            throw new InvalidDatasetGroupException("Dataset group does not start with a dataset group header.");
        }
        dataset_group_header = new DatasetGroupHeader().read(reader, header.length - Header.SIZE);

        header = Header.read(reader);
        while(Reference.KEY.equals(header.key)){
            references.add(Reference.readReference(reader));
            header = Header.read(reader);
        }

        if (ReferenceMetadata.KEY.equals(header.key)){
            reference_metadata = new ReferenceMetadata().read(reader, header.length - Header.SIZE);
            header = Header.read(reader);
        }

        if (LabelList.KEY.equals(header.key)){
            label_list = new LabelList().read(reader, header.length - Header.SIZE);
            header = Header.read(reader);
        }

        if (!DatasetContainer.KEY.equals(header.key)){
            throw new InvalidDatasetGroupException("Dataset is mandatory in dataset group.");
        }
        while(DatasetContainer.KEY.equals(header.key)){
            getDatasetContainers().add(new DatasetContainer().read(reader, header.getContentSize()));
            if(size == size()){
                return this;
            }
            header = Header.read(reader);
        }

        if(DatasetGroupMetadata.KEY.equals(header.key)){
            datasetGroupMetadata = new DatasetGroupMetadata().read(reader, header.getContentSize());
            if(size == size()){
                return this;
            }
            header = Header.read(reader);
        }

        if(DatasetGroupProtection.KEY.equals(header.key)){
            datasetGroupProtection = new DatasetGroupProtection().read(reader, header.getContentSize());
        } else {
            throw new InvalidMPEGStructureException("Dataset group container does not contain element of type: "+header.key);
        }

        if(size != size()) {
            throw new ParsedSizeMismatchException();
        }

        return this;
    }

    public static class InvalidDatasetGroupException extends InvalidMPEGStructureException {
        public InvalidDatasetGroupException(String message) {
            super(message);
        }
    }
}

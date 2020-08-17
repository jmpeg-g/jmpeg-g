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

import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.BidirectionalMap;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;

/**
 * <p>
 * A Dataset container that holds sequence reads and possibly alignment information (ISO/IEC DIS 23092-1 6.4.2 Dataset).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DatasetContainer extends GenInfo<DatasetContainer> {
    
    public final static String KEY = "dtcn";
    
    private DatasetHeader dataset_header;
    private List<DatasetParameterSet> dataset_parameters;
    private MasterIndexTable masterIndexTable;
    private List<AccessUnitContainer> accessUnitContainers;
    private List<List<DescriptorStreamContainer>> descriptorStreamContainers;
    private DatasetMetadata datasetMetadata;
    private DatasetProtection datasetProtection;
    private BidirectionalMap<AccessUnitContainer, AU_Id_triplet> accessUnitContainerToAuIdTriplet;
    private AccessUnitContainer[] unalignedAccessUnitContainer;
    private BidirectionalMap<DescriptorStreamIdDuplet, DescriptorStreamContainer> descriptorStreamIdDupletToContainer;
    private TreeSet<Long> accessUnitContainerOffsets;
    private BidirectionalMap<Long, AccessUnitContainer> offsetToAccessUnits;

    public DatasetContainer() {
        super(KEY);

        dataset_parameters = new ArrayList<>();
        accessUnitContainers = new ArrayList<>();
        descriptorStreamContainers = new ArrayList<>();
        datasetMetadata = null;
        datasetProtection = null;
        dataset_header = new DatasetHeader();
        offsetToAccessUnits = new BidirectionalMap<>();
    }

    public DatasetHeader getDatasetHeader() {
        return dataset_header;
    }

    public void setDatasetHeader(final DatasetHeader datasetHeader) {
        this.dataset_header = datasetHeader;
    }

    public void addDatasetParameters(
            final DatasetParameterSet datasetParameterSet) {
        dataset_parameters.add(datasetParameterSet);
    }

    public List<DatasetParameterSet> getDatasetParameters(){
        return dataset_parameters;
    }

    public MasterIndexTable getMasterIndexTable() {
        return masterIndexTable;
    }

    public DatasetSequenceIndex getSequenceIndex(
            final SequenceIdentifier sequenceId) throws SequenceNotAvailableException {
        return dataset_header.getSequenceIndex(sequenceId);
    }

    public boolean offsetIsValid(final long offset) {
        return accessUnitContainerOffsets.contains(offset);
    }

    public Iterable<? extends DatasetParameterSet> getDataset_parameters() {
        return dataset_parameters;
    }

    public void addAccessUnit(final AccessUnitContainer accessUnitContainer) {
        accessUnitContainers.add(accessUnitContainer);
    }

    public long getOffsetOfAccessUnitContainer(AccessUnitContainer accessUnitContainer) {
        return offsetToAccessUnits.getReverse(accessUnitContainer);
    }

    private static class DescriptorStreamIdDuplet {
        private DataClassIndex classIndex;
        private short descriptorId;

        public DescriptorStreamIdDuplet(
                final DataClassIndex dataClassIndex, 
                final short descriptorId) {

            this.classIndex = dataClassIndex;
            this.descriptorId = descriptorId;
        }

        public DataClassIndex getClassIndex() {
            return classIndex;
        }

        public void setClassIndex(final DataClassIndex classIndex) {
            this.classIndex = classIndex;
        }

        public short getDescriptorId() {
            return descriptorId;
        }

        public void setDescriptorId(final short descriptorId) {
            this.descriptorId = descriptorId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DescriptorStreamIdDuplet)) return false;
            DescriptorStreamIdDuplet that = (DescriptorStreamIdDuplet) o;
            return getClassIndex().equals(that.getClassIndex()) &&
                    getDescriptorId() == that.getDescriptorId();
        }

        @Override
        public int hashCode() {

            return Objects.hash(getClassIndex(), getDescriptorId());
        }
    }


    @Override
    public void write(MPEGWriter writer) throws IOException, InvalidMPEGStructureException {
        if(dataset_header == null){
            throw new InvalidMPEGStructureException("Missing mandatory dataset header element.");
        }
        dataset_header.writeWithHeader(writer);
        for(DatasetParameterSet datasetParameterSet : dataset_parameters){
            datasetParameterSet.writeWithHeader(writer);
        }
        if(dataset_header.isMIT()) {
            if (masterIndexTable == null) {
                throw new InvalidMPEGStructureException("Missing mandatory master index table.");
            }
            masterIndexTable.writeWithHeader(writer);
        }


        for(AccessUnitContainer accessUnitContainer: accessUnitContainers){
            accessUnitContainer.writeWithHeader(writer);
        }

        if(!dataset_header.isBlockHeaderFlag()){
            for(List<DescriptorStreamContainer> streamContainerInClass : descriptorStreamContainers){
                for(DescriptorStreamContainer streamContainer : streamContainerInClass) {
                    streamContainer.writeWithHeader(writer);
                }
            }
        }

        if(datasetMetadata != null) {
            datasetMetadata.writeWithHeader(writer);
        }
        if(datasetProtection != null) {
            datasetProtection.writeWithHeader(writer);
        }
    }

    @Override
    public DatasetContainer read(
            final MPEGReader reader, final long size) 
            throws IOException, InvalidMPEGStructureException, ParsedSizeMismatchException, InvalidMPEGGFileException {

        long datasetInitialPosition = reader.getPosition();

        accessUnitContainerToAuIdTriplet = new BidirectionalMap<>();
        descriptorStreamIdDupletToContainer = new BidirectionalMap<>();
        accessUnitContainerOffsets = new TreeSet<>();

        Header header = Header.read(reader);
        if(!header.key.equals(DatasetHeader.KEY)){
            throw new InvalidMPEGStructureException("Dataset container is missing mandatory dataset header.");
        }

        dataset_header = new DatasetHeader().read(reader, header.getContentSize());

        unalignedAccessUnitContainer = new AccessUnitContainer[(int)dataset_header.getNumberUAccessUnits()];

        if(!dataset_header.isBlockHeaderFlag()) {
            descriptorStreamContainers = new ArrayList<>(DATA_CLASS.values().length);
            for (DATA_CLASS dataClass : DATA_CLASS.values()) {
                try {
                    int numberDescriptors = dataset_header.getNumberOfDescriptors(dataClass);
                    descriptorStreamContainers.add(new ArrayList<>(numberDescriptors));
                } catch (DataClassNotFoundException ignore) {

                }
            }
        }

        header = Header.read(reader);
        while(header.key.equals(DatasetParameterSet.KEY)){
            DatasetParameterSet datasetParameterSet = new DatasetParameterSet();
            datasetParameterSet.read(reader, header.getContentSize());
            dataset_parameters.add(datasetParameterSet);

            header = Header.read(reader);
        }

        if(header.key.equals(MasterIndexTable.KEY)){
            masterIndexTable = new MasterIndexTable(dataset_header).read(reader, header.getContentSize());
            masterIndexTable.setDatasetInitialPosition(datasetInitialPosition);
            header = Header.read(reader);
        }


        long readerPosition = reader.getPosition();
        long auContainerOffset = reader.getPosition();

        while (header.key.equals(AccessUnitContainer.KEY)){
            AccessUnitContainer accessUnitContainer = new AccessUnitContainer(dataset_header);

            accessUnitContainer.read(reader, header.getContentSize());
            accessUnitContainer.setAccessUnitOffset(auContainerOffset);
            accessUnitContainerOffsets.add(auContainerOffset);

            offsetToAccessUnits.put(auContainerOffset, accessUnitContainer);
            accessUnitContainers.add(accessUnitContainer);

            if(accessUnitContainer.getAccessUnitHeader().getAUType() != DATA_CLASS.CLASS_U) {
                AU_Id_triplet auIdTriplet = null;
                try {
                    if (masterIndexTable != null) {
                        auIdTriplet = masterIndexTable
                                .getAuIdTriplet(
                                        accessUnitContainer.getAccessUnitHeader(),
                                        readerPosition - 12
                                );
                        accessUnitContainer.getAccessUnitHeader().setAUStartPosition(
                                masterIndexTable.getAuStart(
                                        auIdTriplet.getSeq(),
                                        auIdTriplet.getClass_i(),
                                        (int)auIdTriplet.getAuId()
                                )
                        );
                        accessUnitContainer.getAccessUnitHeader().setAUEndPosition(
                                masterIndexTable.getAuEnd(
                                        auIdTriplet.getSeq(),
                                        auIdTriplet.getClass_i(),
                                        (int)auIdTriplet.getAuId()
                                )
                        );
                    }
                } catch (DataClassNotFoundException e) {
                    throw new InvalidMPEGGFileException("Data class not found.", e);
                }

                if (auIdTriplet != null) {
                    accessUnitContainerToAuIdTriplet.put(accessUnitContainer, auIdTriplet);
                }
            } else {
                unalignedAccessUnitContainer[
                        accessUnitContainer.getAccessUnitHeader().getAccessUnitID()
                ] = accessUnitContainer;
            }

            readerPosition = reader.getPosition();

            try {
                if(size != size()) {
                    auContainerOffset = reader.getPosition();
                    accessUnitContainerOffsets.add(auContainerOffset);
                    header = Header.read(reader);
                    readerPosition = reader.getPosition();
                }else{
                    return this;
                }
            } catch (EOFException e){
                if(size != size()){
                    throw new ParsedSizeMismatchException();
                }

                return this;
            }
        }

        if(!dataset_header.isBlockHeaderFlag()){
            if (!header.key.equals(DescriptorStreamContainer.KEY)){
                throw new InvalidMPEGStructureException("Dataset container is missing mandatory descriptor stream.");
            }

            while(header.key.equals(DescriptorStreamContainer.KEY)) {
                try {
                    DescriptorStreamContainer descriptorStreamContainer = new DescriptorStreamContainer();
                    descriptorStreamContainer.read(reader, header.getContentSize());
                    descriptorStreamContainer.setStartDataset(datasetInitialPosition);

                    DATA_CLASS classId = descriptorStreamContainer.getDescriptorStreamHeader().getClassID();
                    DataClassIndex dataClassIndex = dataset_header.getClassIndex(classId);

                    descriptorStreamContainers.get(dataClassIndex.getIndex()).add(descriptorStreamContainer);


                    descriptorStreamIdDupletToContainer.put(
                            new DescriptorStreamIdDuplet(
                                    dataClassIndex,
                                    dataset_header.getDescriptorIndex(
                                            classId,
                                            descriptorStreamContainer.getDescriptorStreamHeader().getDescriptorID()
                                    ).getDescriptor_index()
                            ),
                            descriptorStreamContainer
                    );

                    try {
                        if (size != size()) {
                            header = Header.read(reader);
                        } else {
                            return this;
                        }
                    } catch (EOFException e) {
                        if (size != size()) {
                            throw new ParsedSizeMismatchException();
                        }

                        return this;
                    }
                }catch (DataClassNotFoundException | NoSuchFieldException e){
                    throw new InvalidMPEGGFileException(
                            "DescriptorStream which type is not indicated in the header.",
                            e
                    );
                }
            }
        }

        if(header.key.equals(DatasetMetadata.KEY)){
            datasetMetadata = new DatasetMetadata();
            datasetMetadata.read(reader, header.getContentSize());

            try {
                if(size != size()) {
                    header = Header.read(reader);
                }else{
                    return this;
                }
            } catch (EOFException e){
                if(size != size()){
                    throw new ParsedSizeMismatchException();
                }

                return this;
            }
        } else {
            datasetMetadata = null;
        }

        if(header.key.equals(DatasetProtection.KEY)){
            datasetProtection = new DatasetProtection();
            datasetProtection.read(reader, header.getContentSize());

        } else {
            datasetProtection = null;
        }

        if(size != size()) {
            throw new ParsedSizeMismatchException();
        }

        return this;
    }

    @Override
    public long size() {
        long result = 0;
        if(dataset_header == null) {
            throw new IllegalArgumentException();
        }
        result += dataset_header.sizeWithHeader();            
        
        for(DatasetParameterSet datasetParameterSet : dataset_parameters){
            result += datasetParameterSet.sizeWithHeader();
        }

        if(dataset_header.isMIT()) {
            if (masterIndexTable != null) {
                result += masterIndexTable.sizeWithHeader();
            }else{
                throw new IllegalArgumentException();
            }
        }

        for(AccessUnitContainer accessUnitContainer: accessUnitContainers){
            result += accessUnitContainer.sizeWithHeader();
        }

        if(dataset_header!= null && !dataset_header.isBlockHeaderFlag()){
            for(List<DescriptorStreamContainer> streamContainerInClass : descriptorStreamContainers){
                for(DescriptorStreamContainer streamContainer : streamContainerInClass) {
                    if(streamContainer != null) {
                        result += streamContainer.sizeWithHeader();
                    }
                }
            }
        }

        if(datasetMetadata != null) {
            result += datasetMetadata.sizeWithHeader();
        }
        if(datasetProtection != null) {
            result += datasetProtection.sizeWithHeader();
        }
        return result;
    }

    public long getAuStart(AU_Id_triplet au_id_triplet){
        return getMasterIndexTable().getAuStart(
            au_id_triplet.getSeq(),
            au_id_triplet.getClass_i(),
            (int)au_id_triplet.getAuId()
        );
    }

    public long getAuEnd(AU_Id_triplet au_id_triplet){
        return getMasterIndexTable().getAuEnd(
                au_id_triplet.getSeq(),
                au_id_triplet.getClass_i(),
                (int)au_id_triplet.getAuId()
        );
    }

    public void setMasterIndexTable(MasterIndexTable masterIndexTable){
        this.masterIndexTable = masterIndexTable;
    }

    public void setAccessUnitContainers(List<AccessUnitContainer> accessUnits){
        this.accessUnitContainers = accessUnits;
    }

    public void addDescriptorStream(DescriptorStreamContainer descriptorStreamContainer) throws DataClassNotFoundException {
        if(descriptorStreamContainers.size() == 0){
            descriptorStreamContainers = new ArrayList<>(DATA_CLASS.values().length);
            for(DATA_CLASS class_id : dataset_header.getClassIDs()){
                int numberDescriptors = dataset_header.getNumberOfDescriptors(class_id);
                List<DescriptorStreamContainer> descriptorsForClass = new ArrayList<>(numberDescriptors);
                for(int i=0; i<numberDescriptors; i++){
                    descriptorsForClass.add(null);
                }

                descriptorStreamContainers.add(descriptorsForClass);
            }
        }

        DATA_CLASS dataClass = descriptorStreamContainer.getDescriptorStreamHeader().getClassID();
        DataClassIndex dataClassIndex = dataset_header.getClassIndex(dataClass);

        try {
            descriptorStreamContainers
                .get(dataClassIndex.getIndex())
                .set(
                    dataset_header.getDescriptorIndex(
                            dataClass,
                            descriptorStreamContainer.getDescriptorStreamHeader().getDescriptorID()
                    ).getDescriptor_index(), descriptorStreamContainer
                );
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public List<AccessUnitContainer> getAccessUnitContainers(){
        return accessUnitContainers;
    }

    public List<List<DescriptorStreamContainer>> getDescriptorStreamContainers(){
        return descriptorStreamContainers;
    };

    public DatasetParameterSet getDatasetParameterSetById(final short datasetParameter_id){
        for(DatasetParameterSet datasetParameterSet : getDatasetParameters()){
            if(datasetParameterSet.getParameter_set_ID() == datasetParameter_id){
                return datasetParameterSet;
            }
        }
        return null;
    }

    public DatasetParameterSet getDatasetParameterSet(final short datasetParameter_index){
        return dataset_parameters.get(datasetParameter_index);
    }

    public DatasetProtection getDatasetProtection() {
        return datasetProtection;
    }

    public short getSequenceIndex(String sequenceName, DatasetGroupContainer datasetGroupContainer){
        short referenceId = dataset_header.getReferenceId();
        for(short sequenceIndex = 0; sequenceIndex < dataset_header.getSeqIds().length; sequenceIndex++){
            SequenceIdentifier sequenceIdentifier = dataset_header
                    .getReferenceSequenceId(
                            new DatasetSequenceIndex(sequenceIndex)
                    );
            if(
                    datasetGroupContainer
                            .getReference(referenceId)
                            .getSequenceName(sequenceIdentifier)
                            .equals(sequenceName)
            ){
                return sequenceIndex;
            };
        }
        return -1;
    }

    public Long getFirstAUOffset(){
        return accessUnitContainerOffsets.first();
    }

    public Long getNextAUOffset(final long currentOffset){
        return accessUnitContainerOffsets.higher(currentOffset);
    }

    public Long getLastAUOffset(){
        return accessUnitContainerOffsets.last();
    }

    public BidirectionalMap<AccessUnitContainer, AU_Id_triplet> getAccessUnitContainerToAuIdTriplet() {
        return accessUnitContainerToAuIdTriplet;
    }

    public AccessUnitContainer getUnalignedAccessUnitContainer(int index){
        return unalignedAccessUnitContainer[index];
    }
}

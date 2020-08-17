package es.gencom.mpegg.tools;

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.format.*;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.io.MSBitBuffer;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.dataunits.DataUnitAccessUnitHeader;
import es.gencom.mpegg.dataunits.DataUnitParameters;
import es.gencom.mpegg.dataunits.DataUnits;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DatasetToDataUnits {

    public static DataUnits getDataUnits(
            DatasetGroupContainer datasetGroupContainer,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ) throws IOException, DataClassNotFoundException, SequenceNotAvailableException, NoSuchFieldException {
        Reference reference = datasetGroupContainer.getReference(datasetContainer.getDatasetHeader().getReferenceId());
        DataUnitsExtractor dataUnitsExtractor = new DataUnitsExtractor(reference);
        addParameters(dataUnitsExtractor, datasetContainer);
        getDataUnits(
                dataUnitsExtractor,
                datasetContainer,
                sequenceIdentifier,
                start,
                end,
                new DATA_CLASS[]{
                        DATA_CLASS.CLASS_P,
                        DATA_CLASS.CLASS_N,
                        DATA_CLASS.CLASS_M,
                        DATA_CLASS.CLASS_I,
                        DATA_CLASS.CLASS_HM
                }
        );
        return dataUnitsExtractor.constructDataUnits();
    }

    public static DataUnits getDataUnits(
            DatasetGroupContainer datasetGroupContainer,
            DatasetContainer datasetContainer
    ) throws IOException, DataClassNotFoundException, SequenceNotAvailableException, NoSuchFieldException {
        Reference reference;
        try {
            reference = datasetGroupContainer.getReference(datasetContainer.getDatasetHeader().getReferenceId());
        } catch (IndexOutOfBoundsException e){
            reference = null;
        }

        DataUnitsExtractor dataUnitsExtractor = new DataUnitsExtractor(reference);
        addParameters(dataUnitsExtractor, datasetContainer);
        for(SequenceIdentifier sequenceIdentifier : datasetContainer.getDatasetHeader().getSeqIds()){
            getDataUnits(dataUnitsExtractor, datasetContainer, sequenceIdentifier, 0, Long.MAX_VALUE,
                new DATA_CLASS[]{
                    DATA_CLASS.CLASS_P,
                    DATA_CLASS.CLASS_N,
                    DATA_CLASS.CLASS_M,
                    DATA_CLASS.CLASS_I,
                    DATA_CLASS.CLASS_HM
                }
            );
        }
        if(datasetContainer.getDatasetHeader().getNumberUAccessUnits() > 0) {
            getUnmappedDataUnits(dataUnitsExtractor, datasetContainer);
        }
        return dataUnitsExtractor.constructDataUnits();
    }

    public static void getDataUnits(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end,
            DATA_CLASS[] data_classes
    ) throws IOException, DataClassNotFoundException, SequenceNotAvailableException, NoSuchFieldException {
        if(datasetContainer.getDatasetHeader().isBlockHeaderFlag()) {
            if(!datasetContainer.getDatasetHeader().isMIT()) {
                addDataUnitsBlockHeadersNoMIT(
                        dataUnitsExtractor,
                        datasetContainer,
                        sequenceIdentifier,
                        start,
                        end,
                        data_classes);
            } else {
                addDataUnitsBlockHeadersMIT(
                        dataUnitsExtractor,
                        datasetContainer,
                        sequenceIdentifier,
                        start,
                        end,
                        data_classes);
            }
        } else {
            getDataUnitsNoBlockHeader(
                    dataUnitsExtractor,
                    datasetContainer,
                    sequenceIdentifier,
                    start,
                    end,
                    data_classes
            );
        }
    }

    public static void getUnmappedDataUnits(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer
    ) throws IOException {


        if(datasetContainer.getDatasetHeader().isBlockHeaderFlag()) {
            if(!datasetContainer.getDatasetHeader().isMIT()) {
                addDataUnitsBlockHeadersNoMITUnmapped(dataUnitsExtractor, datasetContainer);
            } else {
                addDataUnitsBlockHeadersMITUnmapped(dataUnitsExtractor, datasetContainer);
            }
        } else {
            getDataUnitsNoBlockHeaderUnmapped(dataUnitsExtractor, datasetContainer);
        }
    }

    private static void getDataUnitsNoBlockHeaderUnmapped(
        DataUnitsExtractor dataUnitsExtractor,
        DatasetContainer datasetContainer
    ) throws IOException {
        long unmappedBlocksNumber = datasetContainer.getDatasetHeader().getNumberUAccessUnits();

        DataClassIndex dataClassIndex;
        try {
            dataClassIndex = datasetContainer.getDatasetHeader().getClassIndex(DATA_CLASS.CLASS_U);
        } catch (DataClassNotFoundException e){
            throw new IllegalArgumentException(e);
        }
        for(int block_i=0; block_i<unmappedBlocksNumber; block_i++){
            AccessUnitContainer accessUnitContainer = datasetContainer.getUnalignedAccessUnitContainer(block_i);
            DATA_CLASS[] dataClasses = datasetContainer.getDatasetHeader().getClassIDs();

            DataUnitAccessUnitHeader header = new DataUnitAccessUnitHeader(
                    block_i,
                    accessUnitContainer.getAccessUnitHeader().getNumberOfBlocks(),
                    accessUnitContainer.getAccessUnitHeader().getParameterSetID(),
                    accessUnitContainer.getAccessUnitHeader().getAUType(),
                    accessUnitContainer.getAccessUnitHeader().getReadsCount(),
                    accessUnitContainer.getAccessUnitHeader().getMmThreshold(),
                    accessUnitContainer.getAccessUnitHeader().getMmCount(),
                    accessUnitContainer.getAccessUnitHeader().getReferenceSequenceID(),
                    accessUnitContainer.getAccessUnitHeader().getRefStartPosition(),
                    accessUnitContainer.getAccessUnitHeader().getRefEndPosition(),
                    null,
                    0,
                    0,
                    accessUnitContainer.getAccessUnitHeader().getExtendedAUStartPosition(),
                    accessUnitContainer.getAccessUnitHeader().getExtendedAUEndPosition()
            );

            byte[] descriptor_ids;

            try {
                descriptor_ids = datasetContainer.getDatasetHeader().getDescriptors(
                        accessUnitContainer.getAccessUnitHeader().getAUType()
                );
            } catch (DataClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }

            int numDescriptors = descriptor_ids.length;
            AccessUnitBlock[] blocks = new AccessUnitBlock[numDescriptors];

            DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                    accessUnitContainer.getAccessUnitHeader().getParameterSetID()
            );
            EncodingParameters encodingParameters = new EncodingParameters();
            ByteBuffer byteBuffer = parameterSet.getParameters();
            byteBuffer.rewind();
            encodingParameters.read(new MSBitBuffer(byteBuffer));

            for(int descriptor_i=0; descriptor_i<numDescriptors; descriptor_i++){
                byte descriptor_id = descriptor_ids[descriptor_i];
                try {
                    DescriptorIndex descriptorIndex = datasetContainer.getDatasetHeader().getDescriptorIndex(
                            accessUnitContainer.getAccessUnitHeader().getAUType(),
                            descriptor_id
                    );

                    long startDescriptor = datasetContainer.getMasterIndexTable().getUnmappedBlockByteOffset(
                            block_i,
                            descriptorIndex
                    );


                    Payload allSubsequences;

                    try {
                        long endDescriptor = datasetContainer.getMasterIndexTable().getUnmappedNextBlockStart(
                                block_i,
                                descriptorIndex
                        );

                        allSubsequences = datasetContainer
                                .getDescriptorStreamContainers()
                                .get(dataClassIndex.getIndex())
                                .get(descriptorIndex.getDescriptor_index())
                                .getPayloadFromTo(startDescriptor, endDescriptor);
                    } catch (NullPointerException e) {
                        allSubsequences = datasetContainer
                                .getDescriptorStreamContainers()
                                .get(dataClassIndex.getIndex())
                                .get(descriptorIndex.getDescriptor_index())
                                .getPayloadFromToEnd(startDescriptor);
                    }
                    allSubsequences.rewind();
                    long blockSize = allSubsequences.remaining();

                    Payload[] subsequences = AccessUnitBlock.readSubsequences(
                            DESCRIPTOR_ID.getDescriptorId(descriptor_id),
                            encodingParameters,
                            header.au_type,
                            allSubsequences,
                            blockSize
                    );

                    blocks[descriptor_i] = new AccessUnitBlock(
                            DESCRIPTOR_ID.getDescriptorId(descriptor_id),
                            subsequences
                    );
                } catch (NoSuchFieldException | DataClassNotFoundException e){
                    throw new IllegalArgumentException(e);
                }
            }
            DataUnitAccessUnit dataUnitAccessUnit = new DataUnitAccessUnit(
                    header,
                    dataUnitsExtractor.getDataUnits(),
                    blocks
            );
            dataUnitsExtractor.addDataUnitAccessUnit(
                    dataUnitAccessUnit
            );
        }
    }

    private static void getDataUnitsNoBlockHeader(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end, DATA_CLASS[] data_classes_requested)
            throws SequenceNotAvailableException, DataClassNotFoundException, IOException, NoSuchFieldException {

        DatasetSequenceIndex datasetSequenceIndex = datasetContainer.getSequenceIndex(sequenceIdentifier);
        long referenceBlocksNum = datasetContainer.getDatasetHeader().getReferenceSequenceBlocks(datasetSequenceIndex);
        for(int block_i=0; block_i<referenceBlocksNum; block_i++){
            DATA_CLASS[] dataClasses = datasetContainer.getDatasetHeader().getClassIDs();
            for(int dataClass_i = 0; dataClass_i < dataClasses.length; dataClass_i++){

                boolean isClassIncluded = false;
                if(data_classes_requested == null) {
                    isClassIncluded = true;
                } else {
                    for (DATA_CLASS data_class_requested : data_classes_requested) {
                        if (data_class_requested == dataClasses[dataClass_i]) {
                            isClassIncluded = true;
                            break;
                        }
                    }
                }
                if(!isClassIncluded){
                    continue;
                }

                DATA_CLASS dataClass = dataClasses[dataClass_i];
                DataClassIndex dataClassIndex = datasetContainer.getDatasetHeader().getClassIndex(dataClass);


                AU_Id_triplet au_id_triplet = new AU_Id_triplet(
                        datasetSequenceIndex,
                        datasetContainer.getDatasetHeader().getClassIndex(dataClasses[dataClass_i]),
                        block_i
                );

                AccessUnitContainer accessUnitContainer = datasetContainer
                        .getAccessUnitContainerToAuIdTriplet()
                        .getReverse(au_id_triplet);
                if(accessUnitContainer == null){
                    continue;
                }

                SequenceIdentifier sequenceIdentifierAU = datasetContainer.getDatasetHeader().getReferenceSequenceId(
                        au_id_triplet.getSeq()
                );
                if(!sequenceIdentifierAU.equals(sequenceIdentifier)){
                    continue;
                }
                DatasetSequenceIndex sequenceIndex = au_id_triplet.getSeq();
                long accessUnitStart = datasetContainer.getMasterIndexTable().getAuStart(
                        sequenceIndex,
                        au_id_triplet.getClass_i(),
                        (int)au_id_triplet.getAuId()
                );
                long accessUnitEnd = datasetContainer.getMasterIndexTable().getAuEnd(
                        sequenceIndex,
                        au_id_triplet.getClass_i(),
                        (int)au_id_triplet.getAuId()
                );
                long maximum_accessUnitEnd =
                        accessUnitContainer.getAccessUnitHeader().getAUEndPosition()
                                + datasetContainer.getDatasetHeader().getThreshold(sequenceIndex)
                                + 2* 100;

                boolean isOverlapping = Long.max(start, accessUnitStart) <= Long.min(end, maximum_accessUnitEnd);
                if(!isOverlapping){
                    continue;
                }

                DataUnitAccessUnitHeader header = new DataUnitAccessUnitHeader(
                        au_id_triplet.getAuId(),
                        accessUnitContainer.getAccessUnitHeader().getNumberOfBlocks(),
                        accessUnitContainer.getAccessUnitHeader().getParameterSetID(),
                        accessUnitContainer.getAccessUnitHeader().getAUType(),
                        accessUnitContainer.getAccessUnitHeader().getReadsCount(),
                        accessUnitContainer.getAccessUnitHeader().getMmThreshold(),
                        accessUnitContainer.getAccessUnitHeader().getMmCount(),
                        accessUnitContainer.getAccessUnitHeader().getReferenceSequenceID(),
                        accessUnitContainer.getAccessUnitHeader().getRefStartPosition(),
                        accessUnitContainer.getAccessUnitHeader().getRefEndPosition(),
                        sequenceIdentifierAU,
                        accessUnitStart,
                        accessUnitEnd,
                        accessUnitContainer.getAccessUnitHeader().getExtendedAUStartPosition(),
                        accessUnitContainer.getAccessUnitHeader().getExtendedAUEndPosition()
                );


                byte[] descriptor_ids = datasetContainer.getDatasetHeader().getDescriptors(
                        accessUnitContainer.getAccessUnitHeader().getAUType()
                );
                int numDescriptors = descriptor_ids.length;
                AccessUnitBlock[] blocks = new AccessUnitBlock[numDescriptors];

                DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                        accessUnitContainer.getAccessUnitHeader().getParameterSetID()
                );
                EncodingParameters encodingParameters = new EncodingParameters();
                ByteBuffer byteBuffer = parameterSet.getParameters();
                byteBuffer.rewind();
                encodingParameters.read(new MSBitBuffer(byteBuffer));

                for(int descriptor_i=0; descriptor_i<numDescriptors; descriptor_i++){
                    byte descriptor_id = descriptor_ids[descriptor_i];
                    DescriptorIndex descriptorIndex = datasetContainer.getDatasetHeader().getDescriptorIndex(
                            accessUnitContainer.getAccessUnitHeader().getAUType(),
                            descriptor_id
                    );
                    long startDescriptor = datasetContainer.getMasterIndexTable().getBlockByteOffset(
                            sequenceIndex,
                            dataClassIndex,
                            block_i,
                            descriptorIndex
                    );

                    Payload allSubsequences;

                    try {
                        long endDescriptor = datasetContainer.getMasterIndexTable().getNextBlockStart(
                                sequenceIndex,
                                dataClassIndex,
                                block_i,
                                descriptorIndex
                        );

                        allSubsequences = datasetContainer
                                .getDescriptorStreamContainers()
                                .get(dataClassIndex.getIndex())
                                .get(descriptorIndex.getDescriptor_index())
                                .getPayloadFromTo(startDescriptor, endDescriptor);
                    }catch (NullPointerException e){
                        allSubsequences = datasetContainer
                                .getDescriptorStreamContainers()
                                .get(dataClassIndex.getIndex())
                                .get(descriptorIndex.getDescriptor_index())
                                .getPayloadFromToEnd(startDescriptor);
                    }
                    allSubsequences.rewind();
                    long blockSize = allSubsequences.remaining();

                    Payload[] subsequences = AccessUnitBlock.readSubsequences(
                            DESCRIPTOR_ID.getDescriptorId(descriptor_id),
                            encodingParameters,
                            header.au_type,
                            allSubsequences,
                            blockSize
                    );



                    blocks[descriptor_i] = new AccessUnitBlock(
                            DESCRIPTOR_ID.getDescriptorId(descriptor_id),
                            subsequences
                    );
                }
                DataUnitAccessUnit dataUnitAccessUnit = new DataUnitAccessUnit(
                        header,
                        dataUnitsExtractor.getDataUnits(),
                        blocks
                );
                dataUnitsExtractor.addDataUnitAccessUnit(
                        dataUnitAccessUnit
                );
            }
        }
    }

    public static void addParameters(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer
    ) throws IOException {
        for(DatasetParameterSet datasetParameterSet : datasetContainer.getDatasetParameters()){
            EncodingParameters encodingParameters = new EncodingParameters();

            ByteBuffer byteBuffer = datasetParameterSet.getParameters();
            byteBuffer.rewind();
            encodingParameters.read(new MSBitBuffer(byteBuffer));

            DataUnitParameters dataUnitParameters = new DataUnitParameters(
                    datasetParameterSet.getParent_parameter_set_ID(),
                    datasetParameterSet.getParameter_set_ID(),
                    encodingParameters,
                    dataUnitsExtractor.getDataUnits()
            );

            dataUnitsExtractor.addDataUnitParameters(dataUnitParameters);
        }
    }

    private static void addDataUnitsBlockHeadersNoMIT(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end,
            DATA_CLASS[] data_classes
    ) throws IOException {

        for(AccessUnitContainer accessUnitContainer : datasetContainer.getAccessUnitContainers()){
            if(accessUnitContainer.getAccessUnitHeader().getAUType() == DATA_CLASS.CLASS_U){
                continue;
            }
            if(!accessUnitContainer.getAccessUnitHeader().getSequenceID().equals(sequenceIdentifier)){
                continue;
            }
            DatasetSequenceIndex sequenceIndex;
            try{
                sequenceIndex = datasetContainer.getSequenceIndex(sequenceIdentifier);
            } catch (SequenceNotAvailableException e){
                throw new IllegalArgumentException(e);
            }

            long accessUnitStart =
                    accessUnitContainer.getAccessUnitHeader().getAUStartPosition();
            long maximum_accessUnitEnd =
                    accessUnitContainer.getAccessUnitHeader().getAUEndPosition()
                    + datasetContainer.getDatasetHeader().getThreshold(sequenceIndex)
                    + 2* 100;

            boolean isOverlapping = Long.max(start, accessUnitStart) <= Long.min(end, maximum_accessUnitEnd);
            if(!isOverlapping){
                continue;
            }

            boolean isClassToBeIncluded = false;
            if(data_classes == null){
                isClassToBeIncluded = true;
            }else {
                for (DATA_CLASS data_class : data_classes) {
                    if (data_class == accessUnitContainer.getAccessUnitHeader().getAUType()) {
                        isClassToBeIncluded = true;
                        break;
                    }
                }
            }
            if(!isClassToBeIncluded){
                continue;
            }

            DataUnitAccessUnitHeader header = new DataUnitAccessUnitHeader(
                    accessUnitContainer.getAccessUnitHeader()
            );


            AccessUnitBlock[] blocks = new AccessUnitBlock[accessUnitContainer.getBlocks().size()];

            DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                    accessUnitContainer.getAccessUnitHeader().getParameterSetID()
            );
            EncodingParameters encodingParameters = new EncodingParameters();
            ByteBuffer byteBuffer = parameterSet.getParameters();
            byteBuffer.rewind();
            encodingParameters.read(new MSBitBuffer(byteBuffer));

            for(int i=0; i<blocks.length; i++){
                es.gencom.mpegg.format.Block accessUnitBlock = accessUnitContainer.getBlocks().get(i);

                Payload allSubsequences = accessUnitBlock.getPayload();
                allSubsequences.rewind();
                long blockSize = allSubsequences.remaining();

                Payload[] subsequences = AccessUnitBlock.readSubsequences(
                        DESCRIPTOR_ID.getDescriptorId(accessUnitBlock.getBlockHeader().getDescriptorId()),
                        encodingParameters,
                        header.au_type,
                        allSubsequences,
                        blockSize);

                blocks[i] = new AccessUnitBlock(
                        DESCRIPTOR_ID.getDescriptorId(accessUnitBlock.getBlockHeader().getDescriptorId()),
                        subsequences);
            }
            DataUnitAccessUnit dataUnitAccessUnit = new DataUnitAccessUnit(
                    header,
                    dataUnitsExtractor.getDataUnits(),
                    blocks
            );
            dataUnitsExtractor.addDataUnitAccessUnit(
                    dataUnitAccessUnit
            );

        }
    }

    private static void addDataUnitsBlockHeadersNoMITUnmapped(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer
    ) throws IOException {
        for(AccessUnitContainer accessUnitContainer : datasetContainer.getAccessUnitContainers()){
            if(accessUnitContainer.getAccessUnitHeader().getAUType() != DATA_CLASS.CLASS_U){
                continue;
            }

            DataUnitAccessUnitHeader header = new DataUnitAccessUnitHeader(
                    accessUnitContainer.getAccessUnitHeader()
            );


            AccessUnitBlock[] blocks = new AccessUnitBlock[accessUnitContainer.getBlocks().size()];

            DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                    accessUnitContainer.getAccessUnitHeader().getParameterSetID()
            );
            EncodingParameters encodingParameters = new EncodingParameters();
            ByteBuffer byteBuffer = parameterSet.getParameters();
            byteBuffer.rewind();
            encodingParameters.read(new MSBitBuffer(byteBuffer));

            for(int i=0; i<blocks.length; i++){
                es.gencom.mpegg.format.Block accessUnitBlock = accessUnitContainer.getBlocks().get(i);

                Payload allSubsequences = accessUnitBlock.getPayload();
                allSubsequences.rewind();
                long blockSize = allSubsequences.remaining();

                Payload[] subsequences = AccessUnitBlock.readSubsequences(
                        DESCRIPTOR_ID.getDescriptorId(accessUnitBlock.getBlockHeader().getDescriptorId()),
                        encodingParameters,
                        header.au_type,
                        allSubsequences,
                        blockSize);

                blocks[i] = new AccessUnitBlock(
                        DESCRIPTOR_ID.getDescriptorId(accessUnitBlock.getBlockHeader().getDescriptorId()),
                        subsequences);
            }
            DataUnitAccessUnit dataUnitAccessUnit = new DataUnitAccessUnit(
                    header,
                    dataUnitsExtractor.getDataUnits(),
                    blocks
            );
            dataUnitsExtractor.addDataUnitAccessUnit(
                    dataUnitAccessUnit
            );

        }
    }

    private static void addDataUnitsBlockHeadersMITUnmapped(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer
    ) throws IOException {
        long numberUAccessUnits = datasetContainer.getDatasetHeader().getNumberUAccessUnits();
        for(int uAccessUnit_i = 0; uAccessUnit_i < numberUAccessUnits; uAccessUnit_i++) {
            AccessUnitContainer accessUnitContainer = datasetContainer.getUnalignedAccessUnitContainer(uAccessUnit_i);

            DataUnitAccessUnitHeader header = new DataUnitAccessUnitHeader(
                    uAccessUnit_i,
                    accessUnitContainer.getAccessUnitHeader().getNumberOfBlocks(),
                    accessUnitContainer.getAccessUnitHeader().getParameterSetID(),
                    accessUnitContainer.getAccessUnitHeader().getAUType(),
                    accessUnitContainer.getAccessUnitHeader().getReadsCount(),
                    accessUnitContainer.getAccessUnitHeader().getMmThreshold(),
                    accessUnitContainer.getAccessUnitHeader().getMmCount(),
                    accessUnitContainer.getAccessUnitHeader().getReferenceSequenceID(),
                    accessUnitContainer.getAccessUnitHeader().getRefStartPosition(),
                    accessUnitContainer.getAccessUnitHeader().getRefEndPosition(),
                    null,
                    0,
                    0,
                    accessUnitContainer.getAccessUnitHeader().getExtendedAUStartPosition(),
                    accessUnitContainer.getAccessUnitHeader().getExtendedAUEndPosition()
            );


            AccessUnitBlock[] blocks = new AccessUnitBlock[accessUnitContainer.getBlocks().size()];

            DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                    accessUnitContainer.getAccessUnitHeader().getParameterSetID()
            );
            EncodingParameters encodingParameters = new EncodingParameters();
            ByteBuffer byteBuffer = parameterSet.getParameters();
            byteBuffer.rewind();
            encodingParameters.read(new MSBitBuffer(byteBuffer));

            for(int i=0; i<blocks.length; i++){
                es.gencom.mpegg.format.Block accessUnitBlock = accessUnitContainer.getBlocks().get(i);

                Payload allSubsequences = accessUnitBlock.getPayload();
                allSubsequences.rewind();
                long blockSize = allSubsequences.remaining();

                Payload[] subsequences = AccessUnitBlock.readSubsequences(
                        DESCRIPTOR_ID.getDescriptorId(accessUnitBlock.getBlockHeader().getDescriptorId()),
                        encodingParameters,
                        header.au_type,
                        allSubsequences,
                        blockSize);

                blocks[i] = new AccessUnitBlock(
                        DESCRIPTOR_ID.getDescriptorId(accessUnitBlock.getBlockHeader().getDescriptorId()),
                        subsequences
                );
            }
            DataUnitAccessUnit dataUnitAccessUnit = new DataUnitAccessUnit(
                    header,
                    dataUnitsExtractor.getDataUnits(),
                    blocks
            );
            dataUnitsExtractor.addDataUnitAccessUnit(
                    dataUnitAccessUnit
            );
        }
    }

    private static void addDataUnitsBlockHeadersMIT(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end,
            DATA_CLASS[] data_classes_requested
    ) throws IOException, SequenceNotAvailableException, DataClassNotFoundException {
        DatasetSequenceIndex datasetSequenceIndex = datasetContainer.getSequenceIndex(sequenceIdentifier);
        long referenceBlocksNum = datasetContainer.getDatasetHeader().getReferenceSequenceBlocks(datasetSequenceIndex);
        for(int block_i=0; block_i<referenceBlocksNum; block_i++){
            DATA_CLASS[] dataClasses = datasetContainer.getDatasetHeader().getClassIDs();
            for(int dataClass_i = 0; dataClass_i < dataClasses.length; dataClass_i++){


                boolean isClassIncluded = false;
                if(data_classes_requested == null){
                    isClassIncluded = true;
                } else {
                    for (DATA_CLASS data_class_requested : data_classes_requested) {
                        if (data_class_requested == dataClasses[dataClass_i]) {
                            isClassIncluded = true;
                            break;
                        }
                    }
                }
                if(!isClassIncluded){
                    continue;
                }

                AU_Id_triplet au_id_triplet = new AU_Id_triplet(
                        datasetSequenceIndex,
                        datasetContainer.getDatasetHeader().getClassIndex(dataClasses[dataClass_i]),
                        block_i
                );

                AccessUnitContainer accessUnitContainer = datasetContainer
                        .getAccessUnitContainerToAuIdTriplet()
                        .getReverse(au_id_triplet);
                if(accessUnitContainer == null){
                    continue;
                }

                SequenceIdentifier sequenceIdentifierAU = datasetContainer.getDatasetHeader().getReferenceSequenceId(
                        au_id_triplet.getSeq()
                );
                if(!sequenceIdentifierAU.equals(sequenceIdentifier)){
                    continue;
                }
                DatasetSequenceIndex sequenceIndex = au_id_triplet.getSeq();
                long accessUnitStart = datasetContainer.getMasterIndexTable().getAuStart(
                        sequenceIndex,
                        au_id_triplet.getClass_i(),
                        (int)au_id_triplet.getAuId()
                );
                long accessUnitEnd = datasetContainer.getMasterIndexTable().getAuEnd(
                        sequenceIndex,
                        au_id_triplet.getClass_i(),
                        (int)au_id_triplet.getAuId()
                );
                long maximum_accessUnitEnd =
                        accessUnitContainer.getAccessUnitHeader().getAUEndPosition()
                                + datasetContainer.getDatasetHeader().getThreshold(sequenceIndex)
                                + 2* 100;

                boolean isOverlapping = Long.max(start, accessUnitStart) <= Long.min(end, maximum_accessUnitEnd);
                if(!isOverlapping){
                    continue;
                }

                DataUnitAccessUnitHeader header = new DataUnitAccessUnitHeader(
                        au_id_triplet.getAuId(),
                        accessUnitContainer.getAccessUnitHeader().getNumberOfBlocks(),
                        accessUnitContainer.getAccessUnitHeader().getParameterSetID(),
                        accessUnitContainer.getAccessUnitHeader().getAUType(),
                        accessUnitContainer.getAccessUnitHeader().getReadsCount(),
                        accessUnitContainer.getAccessUnitHeader().getMmThreshold(),
                        accessUnitContainer.getAccessUnitHeader().getMmCount(),
                        accessUnitContainer.getAccessUnitHeader().getReferenceSequenceID(),
                        accessUnitContainer.getAccessUnitHeader().getRefStartPosition(),
                        accessUnitContainer.getAccessUnitHeader().getRefEndPosition(),
                        sequenceIdentifierAU,
                        accessUnitStart,
                        accessUnitEnd,
                        accessUnitContainer.getAccessUnitHeader().getExtendedAUStartPosition(),
                        accessUnitContainer.getAccessUnitHeader().getExtendedAUEndPosition()
                );


                AccessUnitBlock[] blocks = new AccessUnitBlock[accessUnitContainer.getBlocks().size()];

                DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                        accessUnitContainer.getAccessUnitHeader().getParameterSetID()
                );
                EncodingParameters encodingParameters = new EncodingParameters();
                ByteBuffer byteBuffer = parameterSet.getParameters();
                byteBuffer.rewind();
                encodingParameters.read(new MSBitBuffer(byteBuffer));

                for(int i=0; i<blocks.length; i++){
                    es.gencom.mpegg.format.Block accessUnitBlock = accessUnitContainer.getBlocks().get(i);

                    Payload allSubsequences = accessUnitBlock.getPayload();
                    allSubsequences.rewind();
                    long blockSize = allSubsequences.remaining();

                    Payload[] subsequences = AccessUnitBlock.readSubsequences(
                            DESCRIPTOR_ID.getDescriptorId(accessUnitBlock.getBlockHeader().getDescriptorId()),
                            encodingParameters,
                            header.au_type,
                            allSubsequences,
                            blockSize);

                    blocks[i] = new AccessUnitBlock(
                            DESCRIPTOR_ID.getDescriptorId(accessUnitBlock.getBlockHeader().getDescriptorId()),
                            subsequences);
                }
                DataUnitAccessUnit dataUnitAccessUnit = new DataUnitAccessUnit(
                        header,
                        dataUnitsExtractor.getDataUnits(),
                        blocks
                );
                dataUnitsExtractor.addDataUnitAccessUnit(
                        dataUnitAccessUnit
                );
            }
        }
    }


    public boolean offsetIsValid(DatasetContainer datasetContainer, long offset) {
        return datasetContainer.getAccessUnitContainers().contains(offset);
    }

    /*public void addDataUnitFromAccessUnitContainer(
            DatasetContainer datasetContainer,
            DataUnitsConstructor dataUnitsConstructor,
            AccessUnitContainer accessUnitContainer
    ) throws IOException, DataFormatException {
        AU_Id_triplet au_id_triplet =
                datasetContainer.getAccessUnitContainerToAuIdTriplet().getForward(accessUnitContainer);
        if(au_id_triplet == null){
            throw new InternalError("No alignment information could be retrieved ");
        }

        long au_start_position = datasetContainer.getAuStart(au_id_triplet);
        long au_end_position = datasetContainer.getAuEnd(au_id_triplet);
        SequenceIdentifier sequenceIdentifier = datasetContainer
                .getDatasetHeader()
                .getReferenceSequenceId(au_id_triplet.getSeq());



        accessUnitContainer.addToDataUnitsConstructor(
                dataUnitsConstructor,
                sequenceIdentifier,
                au_start_position,
                au_end_position
        );
    }*/

    /*public void addDataUnitsAccessUnitsByOffset(
            DatasetContainer datasetContainer,
            DataUnitsConstructor dataUnitsConstructor,
            long startOffset,
            long endOffset
    ) throws IOException, DataFormatException {
        if( !offsetIsValid(datasetContainer, startOffset) || !offsetIsValid(datasetContainer, endOffset)){
            throw new IllegalArgumentException(
                    "The provided offsets are not offsets which are present in the Dataset container"
            );
        }

        SortedSet<OffsetToAccessUnit> foundAUs = datasetContainer.getOffsetToAccessUnits().subSet(
                new OffsetToAccessUnit(startOffset, null),
                new OffsetToAccessUnit(endOffset, null)
        );

        for(OffsetToAccessUnit foundAU : foundAUs){
            addDataUnitFromAccessUnitContainer(
                    dataUnitsConstructor,
                    compression,
                    foundAU.getAccessUnitContainer()
            );
        }
    }*/


    /*public long addToDataUnitsConstructor(
            DataUnitsConstructor dataUnitsConstructor,
            SequenceIdentifier sequenceId,
            long au_start_position,
            long au_end_position
    ) throws IOException, DataFormatException {
        byte parameterPayload = 0;


        DataUnitAccessUnit dataUnitAccessUnit = accessUnitToDataUnit(dataUnitsConstructor.getDataUnits());


        dataUnitsConstructor.addDataUnitAccessUnit(dataUnitAccessUnit, parameterPayload);

        return 0;
    }*/

    /*public DataUnitAccessUnit accessUnitToDataUnit(
            AccessUnitContainer accessUnitContainer,
            DataUnits dataUnits
    ) throws IOException, DataFormatException {
        List<Block> blocks = accessUnitContainer.getBlocks();

        DataUnitAccessUnit.Block blocksArray[] = new DataUnitAccessUnit.Block[blocks.size()];
        int block_i=0;

        for(Block block : blocks) {
            BlockHeader blockHeader = block.getBlockHeader();
            Payload originalPayloads = block.getPayload();
            //todo this is creating a block with what appears to be one subsequence, the Part 1 payload
            // needs to be parsed down for the part2 dataunit
            blocksArray[block_i] = new DataUnitAccessUnit.Block(
                    blockHeader.getDescriptorId(),
                    originalPayloads
            );
            block_i++;
        }

        DataUnitAccessUnit.DataUnitAccessUnitHeader dataUnitAccessUnitHeader =
                new DataUnitAccessUnit.DataUnitAccessUnitHeader(
                        accessUnitContainer.getAccessUnitHeader()
                );

        return new DataUnitAccessUnit(
                dataUnitAccessUnitHeader,
                dataUnits,
                blocksArray
        );
    }*/
}

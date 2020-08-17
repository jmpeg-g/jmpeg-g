package es.gencom.mpegg.tools;

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.dataunits.DataUnitAccessUnitHeader;
import es.gencom.mpegg.dataunits.DataUnitParameters;
import es.gencom.mpegg.dataunits.DataUnits;
import es.gencom.mpegg.format.AU_Id_triplet;
import es.gencom.mpegg.format.AccessUnitContainer;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DataClassIndex;
import es.gencom.mpegg.format.DataClassNotFoundException;
import es.gencom.mpegg.format.DatasetContainer;
import es.gencom.mpegg.format.DatasetGroupContainer;
import es.gencom.mpegg.format.DatasetParameterSet;
import es.gencom.mpegg.format.DatasetSequenceIndex;
import es.gencom.mpegg.format.DescriptorIndex;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.format.SequenceNotAvailableException;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.io.MSBitBuffer;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DatasetReferenceToDataUnits {
    public static DataUnits getReferenceDataUnits(
            DatasetGroupContainer datasetGroupContainer,
            DatasetContainer datasetContainer,
            RequiredRanges requiredRanges) 
            throws IOException, DataClassNotFoundException, SequenceNotAvailableException, NoSuchFieldException {

        Reference reference = datasetGroupContainer.getReference(datasetContainer.getDatasetHeader().getReferenceId());

        DataUnitsExtractor dataUnitsExtractor = new DataUnitsExtractor(reference);
        addParameters(dataUnitsExtractor, datasetContainer);
        getReferenceDataUnits(
                dataUnitsExtractor,
                datasetContainer,
                requiredRanges);

        return dataUnitsExtractor.constructDataUnits();
    }

    private static void getReferenceDataUnits(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            RequiredRanges requiredRanges
    ) throws IOException, DataClassNotFoundException, SequenceNotAvailableException, NoSuchFieldException {
        if(datasetContainer.getDatasetHeader().isBlockHeaderFlag()) {
            if(!datasetContainer.getDatasetHeader().isMIT()){
                addReferenceDataUnitsBlockHeadersNoMIT(
                        dataUnitsExtractor,
                        datasetContainer,
                        requiredRanges);
            } else {
                addReferenceDataUnitsBlockHeadersMIT(
                        dataUnitsExtractor,
                        datasetContainer,
                        requiredRanges);
            }
        } else {
            addReferenceDataUnitsNoBlockHeader(
                    dataUnitsExtractor,
                    datasetContainer,
                    requiredRanges);

            addReferenceDataUnitsNoBlockHeaderUnmapped(
                    dataUnitsExtractor,
                    datasetContainer,
                    requiredRanges);
        }
    }


    private static void addReferenceDataUnitsBlockHeadersNoMIT(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            RequiredRanges requiredRanges
    ) throws IOException {

        for(AccessUnitContainer accessUnitContainer : datasetContainer.getAccessUnitContainers()){
            if(requiredRanges.isRangeRequired(
                    accessUnitContainer.getAccessUnitHeader().getReferenceSequenceID(),
                    accessUnitContainer.getAccessUnitHeader().getRefStartPosition(),
                    accessUnitContainer.getAccessUnitHeader().getRefEndPosition(),
                    accessUnitContainer.getAccessUnitHeader().getAUType()
            )){
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
    }


    private static void addReferenceDataUnitsBlockHeadersMIT(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            RequiredRanges requiredRanges
    ) throws IOException, SequenceNotAvailableException, DataClassNotFoundException {
        for(
                short sequenceIndex=0;
                sequenceIndex < datasetContainer.getDatasetHeader().getReferenceSequencesCount();
                sequenceIndex++
        ){
            DatasetSequenceIndex datasetSequenceIndex = new DatasetSequenceIndex(sequenceIndex);
            long referenceBlocksNum = datasetContainer.getDatasetHeader().getReferenceSequenceBlocks(datasetSequenceIndex);
            for(int block_i=0; block_i<referenceBlocksNum; block_i++){
                DATA_CLASS[] dataClasses = datasetContainer.getDatasetHeader().getClassIDs();
                for(int dataClass_i = 0; dataClass_i < dataClasses.length; dataClass_i++){
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

                    SequenceIdentifier accessUnitRefSequence = datasetContainer.getMasterIndexTable().getAuRefSequence(
                            au_id_triplet.getSeq(),
                            au_id_triplet.getClass_i(),
                            (int) au_id_triplet.getAuId()
                    );

                    long accessUnitRefStart = datasetContainer.getMasterIndexTable().getAuRefStart(
                            au_id_triplet.getSeq(),
                            au_id_triplet.getClass_i(),
                            (int) au_id_triplet.getAuId()
                    );
                    long accessUnitRefEnd = datasetContainer.getMasterIndexTable().getAuRefEnd(
                            au_id_triplet.getSeq(),
                            au_id_triplet.getClass_i(),
                            (int) au_id_triplet.getAuId()
                    );

                    if(!requiredRanges.isRangeRequired(
                            accessUnitRefSequence,
                            accessUnitRefStart,
                            accessUnitRefEnd,
                            null
                    )){
                        continue;
                    }

                    SequenceIdentifier sequenceIdentifier = datasetContainer.getDatasetHeader().getReferenceSequenceId(
                            datasetSequenceIndex
                    );
                    long accessUnitStart = datasetContainer.getMasterIndexTable().getAuStart(
                            au_id_triplet.getSeq(),
                            au_id_triplet.getClass_i(),
                            (int) au_id_triplet.getAuId()
                    );
                    long accessUnitEnd = datasetContainer.getMasterIndexTable().getAuEnd(
                            au_id_triplet.getSeq(),
                            au_id_triplet.getClass_i(),
                            (int) au_id_triplet.getAuId()
                    );

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
                            sequenceIdentifier,
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
    }

    private static void addReferenceDataUnitsNoBlockHeader(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            RequiredRanges requiredRanges
    )throws DataClassNotFoundException, IOException, NoSuchFieldException {
        for(
                short sequenceIndex=0;
                sequenceIndex < datasetContainer.getDatasetHeader().getReferenceSequencesCount();
                sequenceIndex++
        ) {
            DatasetSequenceIndex datasetSequenceIndex = new DatasetSequenceIndex(sequenceIndex);
            long referenceBlocksNum = datasetContainer.getDatasetHeader().getReferenceSequenceBlocks(datasetSequenceIndex);
            for (int block_i = 0; block_i < referenceBlocksNum; block_i++) {
                DATA_CLASS[] dataClasses = datasetContainer.getDatasetHeader().getClassIDs();
                for (int dataClass_i = 0; dataClass_i < dataClasses.length; dataClass_i++) {
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
                    if (accessUnitContainer == null) {
                        continue;
                    }

                    SequenceIdentifier sequenceIdentifierAU = datasetContainer.getDatasetHeader().getReferenceSequenceId(
                            au_id_triplet.getSeq()
                    );
                    long accessUnitStart = datasetContainer.getMasterIndexTable().getAuStart(
                            au_id_triplet.getSeq(),
                            au_id_triplet.getClass_i(),
                            (int) au_id_triplet.getAuId()
                    );
                    long accessUnitEnd = datasetContainer.getMasterIndexTable().getAuEnd(
                            au_id_triplet.getSeq(),
                            au_id_triplet.getClass_i(),
                            (int) au_id_triplet.getAuId()
                    );


                    if (!requiredRanges.isRangeRequired(
                            accessUnitContainer.getAccessUnitHeader().getReferenceSequenceID(),
                            accessUnitContainer.getAccessUnitHeader().getRefStartPosition(),
                            accessUnitContainer.getAccessUnitHeader().getRefEndPosition(),
                            accessUnitContainer.getAccessUnitHeader().getAUType()
                    )){
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

                    for (int descriptor_i = 0; descriptor_i < numDescriptors; descriptor_i++) {
                        byte descriptor_id = descriptor_ids[descriptor_i];
                        DescriptorIndex descriptorIndex = datasetContainer.getDatasetHeader().getDescriptorIndex(
                                accessUnitContainer.getAccessUnitHeader().getAUType(),
                                descriptor_id
                        );
                        long startDescriptor = datasetContainer.getMasterIndexTable().getBlockByteOffset(
                                datasetSequenceIndex,
                                dataClassIndex,
                                block_i,
                                descriptorIndex
                        );

                        Payload allSubsequences;

                        try {
                            long endDescriptor = datasetContainer.getMasterIndexTable().getNextBlockStart(
                                    datasetSequenceIndex,
                                    dataClassIndex,
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
                                blockSize);


                        blocks[descriptor_i] = new AccessUnitBlock(
                                DESCRIPTOR_ID.getDescriptorId(descriptor_id),
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
    }

    private static void addReferenceDataUnitsNoBlockHeaderUnmapped(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            RequiredRanges requiredRanges
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
            if(!requiredRanges.isRangeRequired(
                    accessUnitContainer.getAccessUnitHeader().getReferenceSequenceID(),
                    accessUnitContainer.getAccessUnitHeader().getRefStartPosition(),
                    accessUnitContainer.getAccessUnitHeader().getRefEndPosition(),
                    accessUnitContainer.getAccessUnitHeader().getAUType()
            )){
                continue;
            }

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

    private static void addDataUnitsBlockHeadersMITUnmapped(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer,
            RequiredRanges requiredRanges
    ) throws IOException {
        long numberUAccessUnits = datasetContainer.getDatasetHeader().getNumberUAccessUnits();
        for(int uAccessUnit_i = 0; uAccessUnit_i < numberUAccessUnits; uAccessUnit_i++) {
            AccessUnitContainer accessUnitContainer = datasetContainer.getUnalignedAccessUnitContainer(uAccessUnit_i);

            if(!requiredRanges.isRangeRequired(
                    accessUnitContainer.getAccessUnitHeader().getReferenceSequenceID(),
                    accessUnitContainer.getAccessUnitHeader().getRefStartPosition(),
                    accessUnitContainer.getAccessUnitHeader().getRefEndPosition(),
                    accessUnitContainer.getAccessUnitHeader().getAUType()
            )){
                continue;
            }

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

    private static void addParameters(
            DataUnitsExtractor dataUnitsExtractor,
            DatasetContainer datasetContainer) throws IOException {

        for(DatasetParameterSet datasetParameterSet : datasetContainer.getDatasetParameters()) {
            EncodingParameters encodingParameters = new EncodingParameters();

            ByteBuffer byteBuffer = datasetParameterSet.getParameters();
            byteBuffer.rewind();
            encodingParameters.read(new MSBitBuffer(byteBuffer));

            DataUnitParameters dataUnitParameters = new DataUnitParameters(
                    datasetParameterSet.getParent_parameter_set_ID(),
                    datasetParameterSet.getParameter_set_ID(),
                    encodingParameters,
                    dataUnitsExtractor.getDataUnits());

            dataUnitsExtractor.addDataUnitParameters(dataUnitParameters);
        }
    }
}














package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.format.*;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.format.signatures.Signature;
import es.gencom.mpegg.io.MSBitOutputArray;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;
import es.gencom.mpegg.coder.dataunits.DataUnits;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.DataFormatException;

public class DataUnitsToDSCDataset extends AbstractDataUnitsToDataset {
    /*Only to be used for debugging*/
    private final boolean randomizedDescriptor;


    private Map<SequenceIdentifier, Map<DATA_CLASS, Map<DESCRIPTOR_ID, Map<Long, DataUnitAccessUnit.Block>>>>
            blockPerSequencePerClassPerDescriptorPerAU;
    private Map<DESCRIPTOR_ID, Map<Long, DataUnitAccessUnit.Block>> blockPerDescriptorPerUnmappedAU;

    public DataUnitsToDSCDataset(){
        this(false);
    }

    /*Only to be used for debugging*/
    public DataUnitsToDSCDataset(boolean randomizedDescriptor){
        this.randomizedDescriptor = randomizedDescriptor;
        blockPerSequencePerClassPerDescriptorPerAU = new TreeMap<>();
        blockPerDescriptorPerUnmappedAU = new TreeMap<>();
    }

    @Override
    public void addAsDataset(
            DatasetGroupContainer datasetGroupContainer,
            int datasetId,
            DataUnits dataUnits,
            boolean use40BitsPositions,
            short referenceId,
            int default_threshold
    ) throws IOException, DataFormatException {

        DatasetContainer datasetContainer = new DatasetContainer();

        boolean multiple_alignment_flag = inferMultiAlignment(dataUnits);
        boolean byte_offset_size_flag = use40BitsPositions;
        boolean non_overlapping_au_range = inferNonOverlapping(dataUnits);
        boolean pos_40_bits = use40BitsPositions;
        boolean block_header_flag = false;
        boolean MIT_flag = true;
        short reference_id = referenceId;
        Reference reference = datasetGroupContainer.getReference(reference_id);
        boolean CC_mode_flag = inferClassContiguousFlag(reference, dataUnits);
        boolean ordered_blocks_flag = false;
        long[] seq_blocks = countBlocksPerSequence(reference, dataUnits);
        SequenceIdentifier[] seqId = createSequenceIdentifiers(reference, seq_blocks);
        seq_blocks = discardZeros(seq_blocks);
        DatasetType dataset_type = inferDatasetType(dataUnits);
        DATA_CLASS[] dataClasses = getDataClasses(dataUnits);
        DESCRIPTOR_ID[][] descriptorIdentifiers = getDescriptorIdentifiers(dataUnits, dataClasses);
        Alphabet alphabet = Alphabet.DNA_IUPAC;
        long num_u_access_units = countNonAligned(dataUnits);
        long num_u_clusters = 0;
        int multiple_signature_base = 0;
        byte u_signature_size = 0;
        boolean u_signature_constant_length = true;
        short u_signature_length = 0;
        int[] thresholds = new int[seqId.length];
        Arrays.fill(thresholds, default_threshold);

        for(DataUnitParameters parameters : dataUnits.getParameters()){
            MSBitOutputArray bufferParameter = new MSBitOutputArray();
            try {
                parameters.getEncodingParameters().write(bufferParameter);
            } catch (IOException e){
                throw new InternalError(e);
            }
            DatasetParameterSet datasetParameterSet = new DatasetParameterSet(
                    datasetGroupContainer.getDatasetGroupHeader().getDatasetGroupId(),
                    (short)datasetContainer.getDatasetHeader().getDatasetId(),
                    parameters.getParent_parameter_set_ID(),
                    parameters.getParameter_set_ID(),
                    ByteBuffer.wrap(bufferParameter.getArray())
            );
            datasetContainer.addDatasetParameters(datasetParameterSet);
        }

        byte[][] descriptorIdentifiersCasted = new byte[descriptorIdentifiers.length][];
        for(int descriptor_i=0; descriptor_i < descriptorIdentifiers.length; descriptor_i++){
            descriptorIdentifiersCasted[descriptor_i] = new byte[descriptorIdentifiers[descriptor_i].length];
            for(int descriptor_j=0; descriptor_j < descriptorIdentifiers[descriptor_i].length; descriptor_j++){
                descriptorIdentifiersCasted[descriptor_i][descriptor_j] =
                        descriptorIdentifiers[descriptor_i][descriptor_j].ID;
            }
        }

        DatasetHeader datasetHeader = new DatasetHeader(
                datasetGroupContainer.getDatasetGroupHeader().getDatasetGroupId(),
                datasetId,
                new byte[]{'1','9','0','0'},
                multiple_alignment_flag,
                byte_offset_size_flag,
                non_overlapping_au_range,
                pos_40_bits,
                block_header_flag,
                MIT_flag,
                CC_mode_flag,
                ordered_blocks_flag,
                reference_id,
                seqId,
                seq_blocks,
                dataset_type,
                dataClasses,
                descriptorIdentifiersCasted,
                alphabet,
                num_u_access_units,
                num_u_clusters,
                multiple_signature_base,
                u_signature_size,
                u_signature_constant_length,
                u_signature_length,
                thresholds
        );

        datasetContainer.setDatasetHeader(datasetHeader);
        datasetGroupContainer.addDatasetContainer(datasetContainer);


        scanDataUnitsAndUpdateMasterIndex(dataUnits, datasetContainer);

        datasetHeader.setOrderedBlocks(datasetContainer.getMasterIndexTable().areBlocksOrdered());
        System.out.println(datasetHeader.isOrderedBlocks());
    }

    private void putBlocksInfoAligned(DatasetContainer datasetContainer, DataUnitAccessUnit dataUnitAccessUnit){
        Map<DATA_CLASS, Map<DESCRIPTOR_ID, Map<Long, DataUnitAccessUnit.Block>>> blockPerClassPerDescriptorPerAU;
        blockPerClassPerDescriptorPerAU = blockPerSequencePerClassPerDescriptorPerAU.computeIfAbsent(
                dataUnitAccessUnit.getHeader().getSequence_ID(), k -> new TreeMap<>()
        );
        Map<DESCRIPTOR_ID, Map<Long, DataUnitAccessUnit.Block>> blockPerDescriptorPerAu;
        blockPerDescriptorPerAu = blockPerClassPerDescriptorPerAU.computeIfAbsent(
                dataUnitAccessUnit.getHeader().getAU_type(), k -> new TreeMap<>()
        );

        DataUnitAccessUnit.Block[] blocks = dataUnitAccessUnit.getBlocks();
        for (int block_i = 0; block_i < blocks.length; block_i++) {
            try {
                datasetContainer.getDatasetHeader().getDescriptorIndex(
                        dataUnitAccessUnit.getAUType(),
                        blocks[block_i].getDescriptorIdentifier().ID
                );
            } catch (DataClassNotFoundException | NoSuchFieldException e) {
                throw new IllegalArgumentException(e);
            }
        }
        byte[] descriptor_idsToProvide;
        try {
            descriptor_idsToProvide = datasetContainer.getDatasetHeader().getDescriptors(dataUnitAccessUnit.getAUType());
        } catch (DataClassNotFoundException e) {
            throw new InternalError(e);
        }

        DESCRIPTOR_ID[] descriptor_idsToProvideCasted = new DESCRIPTOR_ID[descriptor_idsToProvide.length];
        for(int descriptor_i=0; descriptor_i < descriptor_idsToProvideCasted.length; descriptor_i++){
            descriptor_idsToProvideCasted[descriptor_i] = DESCRIPTOR_ID.getDescriptorId(descriptor_idsToProvide[descriptor_i]);
        }

        for (int descriptor_i = 0; descriptor_i < descriptor_idsToProvide.length; descriptor_i++) {
            DESCRIPTOR_ID descriptor_id = descriptor_idsToProvideCasted[descriptor_i];
            Map<Long, DataUnitAccessUnit.Block> blockPerAu = blockPerDescriptorPerAu.computeIfAbsent(
                    descriptor_id, k -> new TreeMap<>()
            );

            DataUnitAccessUnit.Block newBlock = dataUnitAccessUnit.getBlockByDescriptorId(descriptor_id);
            blockPerAu.put(dataUnitAccessUnit.getHeader().getAccess_unit_ID(), newBlock);
        }
    }

    private void putBlocksInfoUnaligned(DatasetContainer datasetContainer, DataUnitAccessUnit dataUnitAccessUnit){
        DataUnitAccessUnit.Block[] blocks = dataUnitAccessUnit.getBlocks();
        for (int block_i = 0; block_i < blocks.length; block_i++) {
            try {
                datasetContainer.getDatasetHeader().getDescriptorIndex(
                        dataUnitAccessUnit.getAUType(),
                        blocks[block_i].getDescriptorIdentifier().ID
                );
            } catch (DataClassNotFoundException | NoSuchFieldException e) {
                throw new IllegalArgumentException(e);
            }
        }
        byte[] descriptor_idsToProvide;
        try {
            descriptor_idsToProvide = datasetContainer.getDatasetHeader().getDescriptors(dataUnitAccessUnit.getAUType());
        } catch (DataClassNotFoundException e) {
            throw new InternalError(e);
        }

        DESCRIPTOR_ID[] descriptor_idsToProvideCasted = new DESCRIPTOR_ID[descriptor_idsToProvide.length];
        for(int descriptor_i=0; descriptor_i < descriptor_idsToProvideCasted.length; descriptor_i++){
            descriptor_idsToProvideCasted[descriptor_i] = DESCRIPTOR_ID.getDescriptorId(descriptor_idsToProvide[descriptor_i]);
        }

        for (int descriptor_i = 0; descriptor_i < descriptor_idsToProvide.length; descriptor_i++) {
            DESCRIPTOR_ID descriptor_id = descriptor_idsToProvideCasted[descriptor_i];
            Map<Long, DataUnitAccessUnit.Block> blockPerAu = blockPerDescriptorPerUnmappedAU.computeIfAbsent(
                    descriptor_id, k -> new TreeMap<>()
            );

            DataUnitAccessUnit.Block newBlock = dataUnitAccessUnit.getBlockByDescriptorId(descriptor_id);
            blockPerAu.put(dataUnitAccessUnit.getHeader().getAccess_unit_ID(), newBlock);
        }
    }

    private void scanDataUnits(
            DatasetContainer datasetContainer,
            DataUnits dataUnits
    ){
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()) {
            if(dataUnitAccessUnit.getAUType() != DATA_CLASS.CLASS_U){
                putBlocksInfoAligned(datasetContainer, dataUnitAccessUnit);
            } else {
                putBlocksInfoUnaligned(datasetContainer, dataUnitAccessUnit);
            }

            AccessUnitHeader accessUnitHeader = new AccessUnitHeader(
                    datasetContainer.getDatasetHeader(),
                    (int) dataUnitAccessUnit.getHeader().getAccess_unit_ID(),
                    (byte) dataUnitAccessUnit.getBlocks().length,
                    dataUnitAccessUnit.getHeader().getParameter_set_ID(),
                    dataUnitAccessUnit.getHeader().getAU_type(),
                    (int) dataUnitAccessUnit.getHeader().getRead_count(),
                    (short) dataUnitAccessUnit.getHeader().getMm_threshold(),
                    (int) dataUnitAccessUnit.getHeader().getMm_count(),
                    dataUnitAccessUnit.getHeader().getRef_sequence_id(),
                    dataUnitAccessUnit.getHeader().getRef_start_position(),
                    dataUnitAccessUnit.getHeader().getRef_end_position(),
                    dataUnitAccessUnit.getHeader().getSequence_ID(),
                    dataUnitAccessUnit.getHeader().getAu_start_position(),
                    dataUnitAccessUnit.getHeader().getAu_end_position(),
                    dataUnitAccessUnit.getHeader().getExtended_au_start_position(),
                    dataUnitAccessUnit.getHeader().getExtended_au_end_position()
            );

            AccessUnitContainer accessUnitContainer = new AccessUnitContainer(
                    datasetContainer,
                    accessUnitHeader
            );

            if(dataUnitAccessUnit.getAUType() != DATA_CLASS.CLASS_U) {
                putInSequence(new IndexInfo(accessUnitContainer));
            } else {
                putInUnaligned(accessUnitContainer);
            }
        }
    }

    private void scanDataUnitsAndUpdateMasterIndex(
            DataUnits dataUnits,
            DatasetContainer datasetContainer
    ) {
        DatasetHeader datasetHeader = datasetContainer.getDatasetHeader();
        scanDataUnits(datasetContainer, dataUnits);

        long[][][] au_start_position = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][] au_end_position = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][] extended_au_start_position = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][] extended_au_end_position = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][] au_byte_offset = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][][] block_byte_offset = new long[datasetHeader.getReferenceSequencesCount()][][][];
        long[] u_au_byte_offset = new long[unmappedAccessUnitContainers.size()];
        long[][] u_au_block_byte_offset;
        try{
            u_au_block_byte_offset = new long[unmappedAccessUnitContainers.size()]
                    [datasetHeader.getNumberOfDescriptors(DATA_CLASS.CLASS_U)];
        } catch (DataClassNotFoundException e){
            throw new IllegalArgumentException(e);
        }

        for(short sequenceIndex = 0; sequenceIndex < datasetHeader.getSeqIds().length; sequenceIndex++) {
            DatasetSequenceIndex datasetSequenceIndex = new DatasetSequenceIndex(sequenceIndex);

            SequenceIdentifier sequenceId = datasetHeader.getSeqIds()[sequenceIndex];
            au_start_position[sequenceIndex] = new long[datasetHeader.getNumberOfClasses()][];
            au_end_position[sequenceIndex] = new long[datasetHeader.getNumberOfClasses()][];
            extended_au_start_position[sequenceIndex] = new long[datasetHeader.getNumberOfClasses()][];
            extended_au_end_position[sequenceIndex] = new long[datasetHeader.getNumberOfClasses()][];
            au_byte_offset[sequenceIndex] = new long[datasetHeader.getNumberOfClasses()][];
            block_byte_offset[sequenceIndex] = new long[datasetHeader.getNumberOfClasses()][][];

            for (byte class_index = 0; class_index < datasetHeader.getNumberOfClasses(); class_index++) {
                DataClassIndex dataClassIndex = new DataClassIndex(class_index);

                block_byte_offset[sequenceIndex][class_index]
                        = new long[(int) datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)][];
                au_start_position[sequenceIndex][class_index] = new long[(int)datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                au_end_position[sequenceIndex][class_index] = new long[(int)datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                extended_au_start_position[sequenceIndex][class_index] = new long[(int)datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                extended_au_end_position[sequenceIndex][class_index] = new long[(int)datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                au_byte_offset[sequenceIndex][class_index] = new long[(int)datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                for (
                        int au_i = 0;
                        au_i < datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex);
                        au_i++
                ) {
                    try {
                        block_byte_offset[sequenceIndex][class_index][au_i] = new long[
                                datasetHeader.getNumberOfDescriptors(datasetHeader.getClassId(dataClassIndex))
                                ];
                        Arrays.fill(block_byte_offset[sequenceIndex][class_index][au_i], -1);
                    } catch (DataClassNotFoundException e) {
                        throw new InternalError(e);
                    }
                }
            }
        }

        MasterIndexTable masterIndexTable = new MasterIndexTable(
                datasetHeader,
                au_start_position,
                au_end_position,
                extended_au_start_position,
                extended_au_end_position,
                au_byte_offset,
                block_byte_offset,
                new Signature[0][],
                u_au_byte_offset,
                new long[0][]
        );
        masterIndexTable.setDefaultAUOffset(0);
        datasetContainer.setMasterIndexTable(masterIndexTable);


        long prior_au_byte_offset = datasetContainer.size();

        for(
                Map.Entry<SequenceIdentifier, TreeMap<DATA_CLASS, TreeSet<IndexInfo>>> classAlignment :
                indexationInfos.entrySet()
        ) {
            DatasetSequenceIndex sequenceIndex;
            try {
                sequenceIndex = datasetHeader.getSequenceIndex(classAlignment.getKey());
            } catch (SequenceNotAvailableException e) {
                throw new InternalError(e);
            }

            for (
                    Map.Entry<DATA_CLASS, TreeSet<IndexInfo>> accessUnitsForClass :
                    classAlignment.getValue().entrySet()
            ) {
                DataClassIndex class_index;
                try {
                    class_index = datasetHeader.getClassIndex(accessUnitsForClass.getKey());
                } catch (DataClassNotFoundException e) {
                    throw new InternalError(e);
                }


                int alignmentIndex = 0;
                for (IndexInfo indexInfo : accessUnitsForClass.getValue()) {
                    au_start_position[sequenceIndex.getIndex()][class_index.getIndex()][alignmentIndex] =
                            (int) (indexInfo.au_start_position > Integer.MAX_VALUE ? Integer.MAX_VALUE : indexInfo.au_start_position);
                    au_end_position[sequenceIndex.getIndex()][class_index.getIndex()][alignmentIndex] =
                            (int) (indexInfo.au_end_position > Integer.MAX_VALUE ? Integer.MAX_VALUE : indexInfo.au_end_position);
                    extended_au_start_position[sequenceIndex.getIndex()][class_index.getIndex()][alignmentIndex] = 0;
                    extended_au_end_position[sequenceIndex.getIndex()][class_index.getIndex()][alignmentIndex] = 0;
                    au_byte_offset[sequenceIndex.getIndex()][class_index.getIndex()][alignmentIndex] = prior_au_byte_offset;
                    prior_au_byte_offset += indexInfo.accessUnitContainer.sizeWithHeader();

                    datasetContainer.addAccessUnit(indexInfo.getAccessUnitContainer());

                    Arrays.fill(block_byte_offset[sequenceIndex.getIndex()][class_index.getIndex()][alignmentIndex], -1);
                    alignmentIndex++;
                }
            }
        }


        for(int u_au_i=0; u_au_i < unmappedAccessUnitContainers.size(); u_au_i++){
            u_au_byte_offset[u_au_i] = prior_au_byte_offset;
            prior_au_byte_offset += unmappedAccessUnitContainers.get(u_au_i).sizeWithHeader();

            Arrays.fill(u_au_block_byte_offset[u_au_i], -1);
        }


        for(DATA_CLASS dataClass : datasetHeader.getClass_ids()) {
            byte[] descriptor_idsForClass;
            try {
                descriptor_idsForClass = datasetHeader.getDescriptors(dataClass);
            }catch (DataClassNotFoundException e) {
                throw new InternalError(e);
            }

            if(dataClass != DATA_CLASS.CLASS_U) {
                createDescriptorAligned(
                    descriptor_idsForClass,
                    datasetContainer,
                    datasetHeader,
                    dataClass,
                    block_byte_offset
                );
            } else {
                createDescriptorUnaligned(
                        descriptor_idsForClass,
                        datasetContainer,
                        datasetHeader,
                        u_au_block_byte_offset
                );
            }
        }
    }

    private void createDescriptorAligned(
            byte[] descriptor_idsForClass,
            DatasetContainer datasetContainer,
            DatasetHeader datasetHeader,
            DATA_CLASS dataClass,
            long[][][][] block_byte_offset
    ){
        for (byte descriptor_id : descriptor_idsForClass) {
            Payload descriptorPayload = new Payload(new byte[0]);
            long blockStart = datasetContainer.size() + 12 + 12 + 6;
            //12: descriptor stream container header,  12: descriptor stream header, 6: descriptor header content

            int numBlocks = 0;
            for (SequenceIdentifier sequenceIdentifier : blockPerSequencePerClassPerDescriptorPerAU.keySet()) {
                Map<Long, DataUnitAccessUnit.Block> blocks = blockPerSequencePerClassPerDescriptorPerAU
                        .get(sequenceIdentifier).get(dataClass).get(DESCRIPTOR_ID.getDescriptorId(descriptor_id));

                DatasetSequenceIndex datasetSequenceIndex;
                try {
                    datasetSequenceIndex = datasetHeader.getSequenceIndex(sequenceIdentifier);
                } catch (SequenceNotAvailableException e) {
                    throw new InternalError(e);
                }
                DataClassIndex classIndex;
                try {
                    classIndex = datasetHeader.getClassIndex(dataClass);
                } catch (DataClassNotFoundException e) {
                    throw new InternalError(e);
                }

                DescriptorIndex descriptorIndex;
                try {
                    descriptorIndex = datasetHeader.getDescriptorIndex(dataClass, descriptor_id);
                } catch (DataClassNotFoundException | NoSuchFieldException e) {
                    throw new InternalError(e);
                }


                Collection<Map.Entry<Long, DataUnitAccessUnit.Block>> blocksToWrite = blocks.entrySet();

                if (randomizedDescriptor) {
                    List<Map.Entry<Long, DataUnitAccessUnit.Block>> tmpBlocks =
                            new ArrayList<Map.Entry<Long, DataUnitAccessUnit.Block>>(blocksToWrite);
                    Collections.shuffle(tmpBlocks);
                    blocksToWrite = tmpBlocks;
                }

                for (Map.Entry<Long, DataUnitAccessUnit.Block> entry_block : blocksToWrite) {

                    if (entry_block.getValue() != null) {
                        Payload blockPayload = entry_block.getValue().getDescriptorSpecificData();
                        block_byte_offset
                                [datasetSequenceIndex.getIndex()]
                                [classIndex.getIndex()]
                                [Math.toIntExact(entry_block.getKey())]
                                [descriptorIndex.getDescriptor_index()] = blockStart;

                        blockStart += blockPayload.size();
                        numBlocks++;

                        descriptorPayload = descriptorPayload.addPayload(blockPayload);
                    }
                }
            }

            DescriptorStreamContainer descriptorStreamContainer = new DescriptorStreamContainer();
            descriptorStreamContainer.setDescriptorStreamHeader(new DescriptorStreamHeader(
                    descriptor_id,
                    dataClass,
                    numBlocks
            ));
            descriptorStreamContainer.setPayload(descriptorPayload);
            try {
                datasetContainer.addDescriptorStream(descriptorStreamContainer);
            } catch (DataClassNotFoundException e) {
                throw new InternalError(e);
            }
        }
    }

    private void createDescriptorUnaligned(
            byte[] descriptor_idsForClass,
            DatasetContainer datasetContainer,
            DatasetHeader datasetHeader,
            long[][] u_au_block_byte_offset
    ){
        for (byte descriptor_id : descriptor_idsForClass) {
            Payload descriptorPayload = new Payload(new byte[0]);
            long blockStart = datasetContainer.size() + 12 + 12 + 6;
            //12: descriptor stream container header,  12: descriptor stream header, 6: descriptor header content

            int numBlocks = 0;

            Map<Long, DataUnitAccessUnit.Block> blocks =
                    blockPerDescriptorPerUnmappedAU.get(DESCRIPTOR_ID.getDescriptorId(descriptor_id));

            DescriptorIndex descriptorIndex;
            try {
                descriptorIndex = datasetHeader.getDescriptorIndex(DATA_CLASS.CLASS_U, descriptor_id);
            } catch (DataClassNotFoundException | NoSuchFieldException e) {
                throw new InternalError(e);
            }


            Collection<Map.Entry<Long, DataUnitAccessUnit.Block>> blocksToWrite = blocks.entrySet();

            if (randomizedDescriptor) {
                List<Map.Entry<Long, DataUnitAccessUnit.Block>> tmpBlocks =
                        new ArrayList<Map.Entry<Long, DataUnitAccessUnit.Block>>(blocksToWrite);
                Collections.shuffle(tmpBlocks);
                blocksToWrite = tmpBlocks;
            }

            for (Map.Entry<Long, DataUnitAccessUnit.Block> entry_block : blocksToWrite) {

                if (entry_block.getValue() != null) {
                    Payload blockPayload = entry_block.getValue().getDescriptorSpecificData();
                    u_au_block_byte_offset
                            [Math.toIntExact(entry_block.getKey())]
                            [descriptorIndex.getDescriptor_index()] = blockStart;

                    blockStart += blockPayload.size();
                    numBlocks++;

                    descriptorPayload = descriptorPayload.addPayload(blockPayload);
                }
            }

            DescriptorStreamContainer descriptorStreamContainer = new DescriptorStreamContainer();
            descriptorStreamContainer.setDescriptorStreamHeader(new DescriptorStreamHeader(
                    descriptor_id,
                    DATA_CLASS.CLASS_U,
                    numBlocks
            ));
            descriptorStreamContainer.setPayload(descriptorPayload);
            try {
                datasetContainer.addDescriptorStream(descriptorStreamContainer);
            } catch (DataClassNotFoundException e) {
                throw new InternalError(e);
            }
        }
    }
}

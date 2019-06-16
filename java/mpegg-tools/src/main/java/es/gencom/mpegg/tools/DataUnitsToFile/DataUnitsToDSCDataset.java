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
    private Map<SequenceIdentifier, Map<DATA_CLASS, Map<DESCRIPTOR_ID, List<DataUnitAccessUnit.Block>>>>
            blockPerSequencePerClassPerDescriptorPerAU;

    public DataUnitsToDSCDataset(){
        blockPerSequencePerClassPerDescriptorPerAU = new TreeMap<>();
    }

    @Override
    public void addAsDataset(
            DatasetGroupContainer datasetGroupContainer,
            DataUnits dataUnits,
            boolean use40BitsPositions,
            short referenceId,
            int default_threshold
    ) throws IOException, DataFormatException {
        DatasetContainer datasetContainer = new DatasetContainer();

        int maxId = -1;
        for(int dataset_i =0; dataset_i < datasetGroupContainer.getDatasetGroupHeader().getNumDatasets(); dataset_i++){
            maxId = Integer.max(maxId, datasetGroupContainer.getDatasetGroupHeader().getDatasetId(dataset_i));
        }

        int datasetId = maxId+1;

        boolean multiple_alignment_flag = inferMultiAlignment(dataUnits);
        boolean byte_offset_size_flag = use40BitsPositions;
        boolean non_overlapping_au_range = inferNonOverlapping(dataUnits);
        boolean pos_40_bits = use40BitsPositions;
        boolean block_header_flag = false;
        boolean MIT_flag = true;
        short reference_id = referenceId;
        Reference reference = datasetGroupContainer.getReference(reference_id);
        boolean CC_mode_flag = inferClassContiguousFlag(reference, dataUnits);
        boolean ordered_blocks_flag = true;
        long[] seq_blocks = countBlocksPerSequence(reference, dataUnits);
        SequenceIdentifier[] seqId = createSequenceIdentifiers(reference, seq_blocks);
        seq_blocks = discardZeros(seq_blocks);
        DatasetType dataset_type = DatasetType.ALIGNED;
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
    }

    private void scanDataUnits(
            DatasetContainer datasetContainer,
            DataUnits dataUnits
    ){
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()) {
            Map<DATA_CLASS, Map<DESCRIPTOR_ID, List<DataUnitAccessUnit.Block>>> blockPerClassPerDescriptorPerAU;
            blockPerClassPerDescriptorPerAU = blockPerSequencePerClassPerDescriptorPerAU.computeIfAbsent(
                    dataUnitAccessUnit.getHeader().getSequence_ID(), k -> new TreeMap<>()
            );
            Map<DESCRIPTOR_ID, List<DataUnitAccessUnit.Block>> blockPerDescriptorPerAu;
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
                List<DataUnitAccessUnit.Block> blockPerAu = blockPerDescriptorPerAu.computeIfAbsent(
                        descriptor_id, k -> new ArrayList<>()
                );

                DataUnitAccessUnit.Block newBlock = dataUnitAccessUnit.getBlockByDescriptorId(descriptor_id);
                blockPerAu.add(newBlock);
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

            putInSequence(new IndexInfo(accessUnitContainer));
        }
    }

    private void scanDataUnitsAndUpdateMasterIndex(
            DataUnits dataUnits,
            DatasetContainer datasetContainer
    ){
        DatasetHeader datasetHeader = datasetContainer.getDatasetHeader();
        scanDataUnits(datasetContainer, dataUnits);

        long[][][] au_start_position = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][] au_end_position = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][] extended_au_start_position = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][] extended_au_end_position = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][] au_byte_offset = new long[datasetHeader.getReferenceSequencesCount()][][];
        long[][][][] block_byte_offset = new long[datasetHeader.getReferenceSequencesCount()][][][];

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
                new long[0],
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


        for(DATA_CLASS dataClass : datasetHeader.getClass_ids()) {
            byte[] descriptor_idsForClass;
            try {
                descriptor_idsForClass = datasetHeader.getDescriptors(dataClass);
            }catch (DataClassNotFoundException e) {
                throw new InternalError(e);
            }
            for(byte descriptor_id : descriptor_idsForClass) {
                Payload descriptorPayload = new Payload(new byte[0]);
                long blockStart = datasetContainer.size()+ 12 + 12 + 6;
                //12: descriptor stream container header,  12: descriptor stream header, 6: descriptor header content

                int numBlocks = 0;
                for(SequenceIdentifier sequenceIdentifier : blockPerSequencePerClassPerDescriptorPerAU.keySet()) {
                    List<DataUnitAccessUnit.Block> blocks = blockPerSequencePerClassPerDescriptorPerAU
                            .get(sequenceIdentifier).get(dataClass).get(descriptor_id);

                    DatasetSequenceIndex datasetSequenceIndex;
                    try {
                        datasetSequenceIndex = datasetHeader.getSequenceIndex(sequenceIdentifier);
                    }catch (SequenceNotAvailableException e) {
                        throw new InternalError(e);
                    }
                    DataClassIndex classIndex;
                    try {
                        classIndex = datasetHeader.getClassIndex(dataClass);
                    }catch (DataClassNotFoundException e) {
                        throw new InternalError(e);
                    }

                    DescriptorIndex descriptorIndex;
                    try {
                        descriptorIndex = datasetHeader.getDescriptorIndex(dataClass, descriptor_id);
                    } catch (DataClassNotFoundException | NoSuchFieldException e) {
                        throw new InternalError(e);
                    }
                    for(int block_i=0; block_i < blocks.size(); block_i++) {
                        DataUnitAccessUnit.Block block = blocks.get(block_i);



                        if(block != null) {
                            Payload blockPayload = block.getDescriptorSpecificData();
                            block_byte_offset
                                    [datasetSequenceIndex.getIndex()]
                                    [classIndex.getIndex()]
                                    [block_i]
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
                }catch (DataClassNotFoundException e) {
                    throw new InternalError(e);
                }
            }
        }
    }
}

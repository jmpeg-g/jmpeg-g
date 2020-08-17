package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.format.signatures.Signature;
import es.gencom.mpegg.io.MSBitOutputArray;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.dataunits.DataUnitParameters;
import es.gencom.mpegg.dataunits.DataUnits;
import es.gencom.mpegg.format.ALPHABET;
import es.gencom.mpegg.format.AccessUnitContainer;
import es.gencom.mpegg.format.AccessUnitHeader;
import es.gencom.mpegg.format.Block;
import es.gencom.mpegg.format.BlockHeader;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DataClassIndex;
import es.gencom.mpegg.format.DataClassNotFoundException;
import es.gencom.mpegg.format.DatasetContainer;
import es.gencom.mpegg.format.DatasetGroupContainer;
import es.gencom.mpegg.format.DatasetHeader;
import es.gencom.mpegg.format.DatasetParameterSet;
import es.gencom.mpegg.format.DatasetSequenceIndex;
import es.gencom.mpegg.format.DatasetType;
import es.gencom.mpegg.format.MasterIndexTable;
import es.gencom.mpegg.format.SequenceIdentifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class DataUnitsToAUCMITDataset extends AbstractDataUnitsToDataset{

    private void scanDataUnitsAndUpdateMasterIndex(DataUnits dataUnits, DatasetContainer datasetContainer)
            throws IOException {
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()) {
            addDataUnit((int)dataUnitAccessUnit.header.access_unit_id, dataUnitAccessUnit, datasetContainer);
        }
        createMasterIndexTable(datasetContainer,0);
        DatasetHeader datasetHeader = datasetContainer.getDatasetHeader();
        long sizePriorToAus = datasetHeader.sizeWithHeader();
        for(DatasetParameterSet datasetParameterSet : datasetContainer.getDataset_parameters()){
            sizePriorToAus += datasetParameterSet.sizeWithHeader();
        }
        sizePriorToAus += datasetContainer.getMasterIndexTable().sizeWithHeader();
        createMasterIndexTable(datasetContainer, sizePriorToAus );
    }


    public void addDataUnit(
            int auId,
            DataUnitAccessUnit dataUnit,
            DatasetContainer datasetContainer
    ) {
        AccessUnitHeader accessUnitHeader = new AccessUnitHeader(
                datasetContainer.getDatasetHeader(),
                auId,
                (byte)dataUnit.getBlocks().length,
                dataUnit.header.parameter_set_id,
                dataUnit.header.au_type,
                (int)dataUnit.header.read_count,
                (short)dataUnit.header.mm_threshold,
                (int)dataUnit.header.mm_count,
                dataUnit.header.ref_sequence_id,
                dataUnit.header.ref_start_position,
                dataUnit.header.ref_end_position,
                dataUnit.header.sequence_id,
                dataUnit.header.au_start_position,
                dataUnit.header.au_end_position,
                dataUnit.header.extended_au_start_position,
                dataUnit.header.extended_au_end_position
        );

        AccessUnitContainer accessUnitContainer = new AccessUnitContainer(
                datasetContainer.getDatasetHeader(),
                accessUnitHeader);

        for(AccessUnitBlock block : dataUnit.getBlocks()){
            Block accessUnitContainerBlock = new Block(datasetContainer.getDatasetHeader());
            Payload payload = block.getDescriptorSpecificData();

            accessUnitContainerBlock.setBlockHeader(new BlockHeader(
                block.descriptor_id.ID,
                (int) payload.size()));
            
            accessUnitContainerBlock.setPayload(payload);

            accessUnitContainer.addBlock(accessUnitContainerBlock);
        }

        if(dataUnit.getAUType() != DATA_CLASS.CLASS_U) {
            putInSequence(new IndexInfo(accessUnitContainer));
        } else {
            putInUnaligned(accessUnitContainer);
        }
    }




    public void createMasterIndexTable(DatasetContainer datasetContainer, long prior_au_byte_offset){
        DatasetHeader datasetHeader = datasetContainer.getDatasetHeader();
        try {
            long initial_au_offset = prior_au_byte_offset;
            for (short sequenceIndex = 0; sequenceIndex < datasetHeader.getSeqIds().length; sequenceIndex++) {
                DatasetSequenceIndex datasetSequenceIndex = new DatasetSequenceIndex(sequenceIndex);
                SequenceIdentifier sequenceId = datasetHeader.getReferenceSequenceId(datasetSequenceIndex);

                if (!indexationInfos.containsKey(sequenceId)) {
                    continue;
                }
                for (DATA_CLASS classId : datasetHeader.getClassIDs()) {
                    if (classId != DATA_CLASS.CLASS_U) {
                        if (!indexationInfos.get(sequenceId).containsKey(classId)) {
                            throw new IllegalArgumentException();
                        }
                        if (
                                indexationInfos.get(sequenceId).get(classId).size()
                                        > datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)
                                ) {
                            throw new IllegalArgumentException();
                        }
                    }
                }
            }

            long[][][] au_start_position = new long[datasetHeader.getReferenceSequencesCount()][][];
            long[][][] au_end_position = new long[datasetHeader.getReferenceSequencesCount()][][];
            long[][][] extended_au_start_position = new long[datasetHeader.getReferenceSequencesCount()][][];
            long[][][] extended_au_end_position = new long[datasetHeader.getReferenceSequencesCount()][][];
            long[][][] au_byte_offset = new long[datasetHeader.getReferenceSequencesCount()][][];
            long[] u_au_byte_offset = new long[unmappedAccessUnitContainers.size()];

            for (short sequenceIndex = 0; sequenceIndex < datasetHeader.getSeqIds().length; sequenceIndex++) {
                DatasetSequenceIndex datasetSequenceIndex = new DatasetSequenceIndex(sequenceIndex);

                short sequenceId = (short) datasetHeader.getSeqIds()[sequenceIndex].getSequenceIdentifier();
                au_start_position[sequenceIndex] = new long[datasetHeader.getNumberAlignedClasses()][];
                au_end_position[sequenceIndex] = new long[datasetHeader.getNumberAlignedClasses()][];
                extended_au_start_position[sequenceIndex] = new long[datasetHeader.getNumberAlignedClasses()][];
                extended_au_end_position[sequenceIndex] = new long[datasetHeader.getNumberAlignedClasses()][];
                au_byte_offset[sequenceIndex] = new long[datasetHeader.getNumberAlignedClasses()][];

                if (!indexationInfos.containsKey(new SequenceIdentifier(sequenceId))) {
                    continue;
                }
                for (Map.Entry<DATA_CLASS, TreeSet<IndexInfo>> classAlignments : indexationInfos.get(new SequenceIdentifier(sequenceId)).entrySet()) {
                    DataClassIndex class_index = datasetHeader.getClassIndex(classAlignments.getKey());

                    au_start_position[sequenceIndex][class_index.getIndex()] =
                            new long[(int) datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                    au_end_position[sequenceIndex][class_index.getIndex()] =
                            new long[(int) datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                    extended_au_start_position[sequenceIndex][class_index.getIndex()] =
                            new long[(int) datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                    extended_au_end_position[sequenceIndex][class_index.getIndex()] =
                            new long[(int) datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];
                    au_byte_offset[sequenceIndex][class_index.getIndex()] =
                            new long[(int) datasetHeader.getReferenceSequenceBlocks(datasetSequenceIndex)];

                    Arrays.fill(au_start_position[sequenceIndex][class_index.getIndex()], -1);
                    Arrays.fill(au_end_position[sequenceIndex][class_index.getIndex()], -1);
                    Arrays.fill(extended_au_start_position[sequenceIndex][class_index.getIndex()], -1);
                    Arrays.fill(extended_au_end_position[sequenceIndex][class_index.getIndex()], -1);
                    Arrays.fill(au_byte_offset[sequenceIndex][class_index.getIndex()], initial_au_offset);

                    int alignmentIndex = 0;
                    for (IndexInfo indexInfo : classAlignments.getValue()) {
                        au_start_position[sequenceIndex][class_index.getIndex()][alignmentIndex] =
                                (int) (indexInfo.au_start_position > Integer.MAX_VALUE ? Integer.MAX_VALUE : indexInfo.au_start_position);
                        au_end_position[sequenceIndex][class_index.getIndex()][alignmentIndex] =
                                (int) (indexInfo.au_end_position > Integer.MAX_VALUE ? Integer.MAX_VALUE : indexInfo.au_end_position);
                        if (indexInfo.au_end_position < indexInfo.au_start_position) {
                            System.err.println("error detected in dataunits to auc file");
                        }
                        extended_au_start_position[sequenceIndex][class_index.getIndex()][alignmentIndex] = 0;
                        extended_au_end_position[sequenceIndex][class_index.getIndex()][alignmentIndex] = 0;
                        au_byte_offset[sequenceIndex][class_index.getIndex()][alignmentIndex] = prior_au_byte_offset;

                        prior_au_byte_offset += indexInfo.accessUnitContainer.sizeWithHeader();

                        alignmentIndex++;
                    }
                }
            }

            int u_au_id = 0;
            for(AccessUnitContainer accessUnitContainer : unmappedAccessUnitContainers){
                u_au_byte_offset[u_au_id] = prior_au_byte_offset;
                prior_au_byte_offset += accessUnitContainer.sizeWithHeader();
                u_au_id++;
            }

            MasterIndexTable createdMaster = new MasterIndexTable(
                    datasetHeader,
                    au_start_position,
                    au_end_position,
                    extended_au_start_position,
                    extended_au_end_position,
                    au_byte_offset,
                    new Signature[0][],
                    u_au_byte_offset,
                    new long[0][]
            );

            createdMaster.setDefaultAUOffset(initial_au_offset);

            datasetContainer.setMasterIndexTable(
                    createdMaster
            );
        }catch (DataClassNotFoundException e){
            throw new InternalError(e);
        }
    }

    @Override
    public void addAsDataset(
            DatasetGroupContainer datasetGroupContainer,
            int datasetId,
            DataUnits dataUnits,
            boolean use40BitsPositions,
            short referenceId,
            int default_threshold,
            ALPHABET alphabet) throws IOException {
        DatasetContainer datasetContainer = new DatasetContainer();

        DatasetType dataset_type = inferDatasetType(dataUnits);
        boolean multiple_alignment_flag = inferMultiAlignment(dataUnits);
        boolean byte_offset_size_flag = dataset_type == DatasetType.REFERENCE || use40BitsPositions;
        boolean non_overlapping_au_range = inferNonOverlapping(dataUnits);
        boolean pos_40_bits = use40BitsPositions;
        boolean block_header_flag = true;
        boolean MIT_flag = true;
        short reference_id = referenceId;
        Reference reference = datasetGroupContainer.getReference(reference_id);
        boolean CC_mode_flag = inferClassContiguousFlag(reference, dataUnits);
        boolean ordered_blocks_flag = true;
        long[] seq_blocks = countBlocksPerSequence(reference, dataUnits);
        SequenceIdentifier[] seqId = createSequenceIdentifiers(reference, seq_blocks);
        seq_blocks = discardZeros(seq_blocks);
        DATA_CLASS[] dataClasses = getDataClasses(dataUnits);
        DESCRIPTOR_ID[][] descriptorIdentifiers = getDescriptorIdentifiers(dataUnits, dataClasses);
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
                    parameters.parent_parameter_set_id,
                    parameters.parameter_set_id,
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

        addAccessUnits(datasetContainer);
    }

    private void addAccessUnits(DatasetContainer datasetContainer) {
        for(
                Map.Entry<SequenceIdentifier, TreeMap<DATA_CLASS, TreeSet<IndexInfo>>> sequenceEntry
                : indexationInfos.entrySet()
        ){
            for(Map.Entry<DATA_CLASS, TreeSet<IndexInfo>> classEntry: sequenceEntry.getValue().entrySet()){
                for(IndexInfo indexInfo : classEntry.getValue()) {
                    datasetContainer.addAccessUnit(indexInfo.getAccessUnitContainer());
                }
            }
        }
        for(
                AccessUnitContainer accessUnitContainer :
                unmappedAccessUnitContainers
        ){
            datasetContainer.addAccessUnit(accessUnitContainer);
        }
    }
}

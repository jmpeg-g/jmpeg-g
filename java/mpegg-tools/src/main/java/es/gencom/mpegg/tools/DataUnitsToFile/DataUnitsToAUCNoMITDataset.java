package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.format.*;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.io.MSBitOutputArray;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;
import es.gencom.mpegg.coder.dataunits.DataUnits;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataUnitsToAUCNoMITDataset extends AbstractDataUnitsToDataset{
    public void addAsDataset(
            DatasetGroupContainer datasetGroupContainer,
            int datasetId,
            DataUnits dataUnits,
            boolean use40BitsPositions,
            short referenceId,
            int default_threshold,
            Alphabet alphabet
    ){
        DatasetContainer datasetContainer = new DatasetContainer();

        DatasetType dataset_type = inferDatasetType(dataUnits);
        boolean multiple_alignment_flag = inferMultiAlignment(dataUnits);
        boolean byte_offset_size_flag = dataset_type == DatasetType.REFERENCE || use40BitsPositions;
        boolean non_overlapping_au_range = inferNonOverlapping(dataUnits);
        boolean pos_40_bits = use40BitsPositions;
        boolean block_header_flag = true;
        boolean MIT_flag = false;
        short reference_id = referenceId;

        DATA_CLASS[] dataClasses = getDataClasses(dataUnits);
        boolean CC_mode_flag = true;
        long[] seq_blocks = new long[0];
        Reference reference;
        SequenceIdentifier[] seqId = new SequenceIdentifier[0];
        int[][] allocatedMappedAuIds = new int[0][];
        try {
            reference = datasetGroupContainer.getReference(reference_id);
            seq_blocks = countBlocksPerSequence(reference, dataUnits);
            CC_mode_flag = inferClassContiguousFlag(reference, dataUnits);
            seqId = createSequenceIdentifiers(reference, seq_blocks);
            allocatedMappedAuIds = new int[reference.getNumberSequences()][5];
        } catch (Exception e){
            if(!(dataClasses.length == 1 && dataClasses[0]==DATA_CLASS.CLASS_U)){
                throw new IllegalArgumentException(e);
            }
        }

        boolean ordered_blocks_flag = true;


        seq_blocks = discardZeros(seq_blocks);

        DESCRIPTOR_ID[][] descriptorIdentifiers = getDescriptorIdentifiers(dataUnits, dataClasses);
        long num_u_access_units = countNonAligned(dataUnits);
        long num_u_clusters = 0;
        int multiple_signature_base = 0;
        byte u_signature_size = 0;
        boolean u_signature_constant_length = true;
        short u_signature_length = 32;
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

        int allocatedUnmappedAUIds = 0;

        for(DataUnitAccessUnit accessUnit : dataUnits.getDataUnitAccessUnits()){
            int auId;
            if(accessUnit.getAUType() == DATA_CLASS.CLASS_U){
                auId = allocatedUnmappedAUIds;
                allocatedUnmappedAUIds++;
            } else {
                auId = allocatedMappedAuIds
                        [accessUnit.getSequenceId().getSequenceIdentifier()]
                        [accessUnit.getAUType().ID - 1];
                allocatedMappedAuIds
                        [accessUnit.getSequenceId().getSequenceIdentifier()]
                        [accessUnit.getAUType().ID - 1]++;
            }

            AccessUnitContainer accessUnitContainer = new AccessUnitContainer(datasetContainer);
            AccessUnitHeader accessUnitHeader = new AccessUnitHeader(
                    datasetHeader,
                    auId,
                    (byte)accessUnit.getBlocks().length,
                    accessUnit.getHeader().getParameter_set_ID(),
                    accessUnit.getHeader().getAU_type(),
                    (int)accessUnit.getHeader().getRead_count(),
                    (short)accessUnit.getHeader().getMm_threshold(),
                    (int)accessUnit.getHeader().getMm_count(),
                    accessUnit.getHeader().getRef_sequence_id(),
                    accessUnit.getHeader().getRef_start_position(),
                    accessUnit.getHeader().getRef_end_position(),
                    accessUnit.getHeader().getSequence_ID(),
                    accessUnit.getHeader().getAu_start_position(),
                    accessUnit.getHeader().getAu_end_position(),
                    accessUnit.getHeader().getExtended_au_start_position(),
                    accessUnit.getHeader().getExtended_au_end_position()
            );

            accessUnitContainer.setAccessUnitHeader(accessUnitHeader);
            List<Block> blocks = new ArrayList<>(accessUnit.getBlocks().length);
            for(DataUnitAccessUnit.Block dataUnitBlock : accessUnit.getBlocks()){
                Block newBlock = new Block(datasetHeader);


                Payload blockData = dataUnitBlock.getDescriptorSpecificData();
                blockData.rewind();

                newBlock.setBlockHeader(new BlockHeader(
                        dataUnitBlock.getDescriptorIdentifier().ID,
                        blockData.remaining()
                ));
                newBlock.setPayload(blockData);

                blocks.add(newBlock);
            }
            accessUnitContainer.setBlocks(blocks);
            datasetContainer.addAccessUnit(accessUnitContainer);
        }
    }
}

package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.mpegg.format.*;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.dataunits.DataUnits;

import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;

public abstract class AbstractDataUnitsToFile {

    protected final DatasetContainer datasetContainer;
    protected final DatasetHeader datasetHeader;

    public abstract void populateDataset(
            DatasetContainer datasetContainer,
            DataUnits dataUnits,
            Reference reference) throws IOException, DataFormatException;

    public AbstractDataUnitsToFile(DatasetContainer datasetContainer){
        this.datasetContainer = datasetContainer;
        this.datasetHeader =
                datasetContainer.getDatasetHeader() == null ? new DatasetHeader() : datasetContainer.getDatasetHeader();

        this.datasetHeader.setDatasetType(DatasetType.ALIGNED);
        /*this.datasetHeader.setAlphabetId(Alphabet.DNA_NON_IUPAC);

        this.datasetHeader.setUnmappedIndexing(false);
        this.datasetHeader.setByteOffsetSize(false);
        this.datasetHeader.setNonOverlappingAURange(true);
        this.datasetHeader.setMIT(true);
        this.datasetHeader.setCCMode(true);*/
    }


    public void scanDataUnitsAndUpdateHeader(DataUnits dataUnits, Reference reference){
        int[][] numAlignedClassesPerClass = new int[reference.getNumberSequences()][5];
        int numUnalignedClasses = 0;
        Map<SequenceIdentifier, TreeMap<DATA_CLASS, Integer>> infoForHeader = new TreeMap<>();
        for(DataUnitAccessUnit dataUnit : dataUnits.getDataUnitAccessUnits()){
            SequenceIdentifier sequenceId = dataUnit.header.sequence_id;

            DATA_CLASS dataClass = dataUnit.header.au_type;

            if(dataClass != DATA_CLASS.CLASS_U) {
                numAlignedClassesPerClass[sequenceId.getSequenceIdentifier()][dataClass.ID - 1] += 1;
            } else {
                numUnalignedClasses++;
            }
        }

        TreeSet<DATA_CLASS> classesFound = new TreeSet<>();
        for(int sequenceId = 0; sequenceId < reference.getNumberSequences(); sequenceId++){
            for(int class_i=0; class_i < 5; class_i++) {
                if (numAlignedClassesPerClass[sequenceId][class_i] != 0) {
                    classesFound.add(DATA_CLASS.getDataClass((byte) (class_i + 1)));
                }
            }
            if(numUnalignedClasses != 0){
                classesFound.add(DATA_CLASS.CLASS_U);
            }
        }

        DATA_CLASS[] dataClasses = new DATA_CLASS[classesFound.size()];
        int class_i=0;
        for(DATA_CLASS dataClass : classesFound){
            dataClasses[class_i] = dataClass;
            class_i++;
        }

        short sequenceIds[];
        HashMap<SequenceIdentifier, DatasetSequenceIndex> sequenceIdentifierToIndex = new HashMap<>();


        sequenceIds = new short[reference.getNumberSequences()];
        for (short sequence_index = 0; sequence_index < reference.getNumberSequences(); sequence_index++) {
            sequenceIds[sequence_index] = sequence_index;
            sequenceIdentifierToIndex.put(new SequenceIdentifier(sequence_index), new DatasetSequenceIndex(sequence_index));
        }




        Map<DATA_CLASS, TreeSet<Byte>> classToDescriptor = new TreeMap<>();
        /*for(DataUnitAccessUnit dataUnit : dataUnits){
            TreeSet<Byte> descriptorsUsedInClass =
                    classToDescriptor.computeIfAbsent(dataUnit.getHeader().getAU_type(), k -> new TreeSet<>());
            for(DataUnitAccessUnit.Block block : dataUnit.getBlocks()){
                descriptorsUsedInClass.add(block.getDescriptorId());
            }
        }*/



        DATA_CLASS classIds[] = new DATA_CLASS[classesFound.size()];
        int class_index = 0;
        for(DATA_CLASS classId : classesFound){
            classIds[class_index] = classId;
            class_index++;
        }

        int blocksPerSequence[];

        blocksPerSequence = new int[reference.getNumberSequences()];
        Arrays.fill(blocksPerSequence, 0);

        for (Map.Entry<SequenceIdentifier, TreeMap<DATA_CLASS, Integer>> blocksPerClassInSequence : infoForHeader.entrySet()) {
            Integer numberBlocks = 0;
            for (Integer numberBlocksInClassInSequence : blocksPerClassInSequence.getValue().values()) {
                numberBlocks = Integer.max(numberBlocks, numberBlocksInClassInSequence);
            }
            blocksPerSequence[sequenceIdentifierToIndex.get(blocksPerClassInSequence.getKey()).getIndex()]
                    = numberBlocks;
        }


        /*datasetHeader.setSeq_ids(sequenceIds);
        datasetHeader.setDefaultThreshold(100000);
        datasetHeader.setClass_ids(classIds);
        datasetHeader.setSeq_blocks(blocksPerSequence);
        datasetHeader.setOrderedBlocks(true);*/

        /*byte descIds[][] = new byte[classesFound.size()][];
        int class_i=0;
        for(Map.Entry<DataClass, TreeSet<Byte>> classInfo : classToDescriptor.entrySet()){
            int desc_i=0;
            descIds[class_i] = new byte[classInfo.getValue().size()];
            for(Byte desc_Id : classInfo.getValue()){
                descIds[class_i][desc_i] = desc_Id;
                desc_i++;
            }
            class_i++;
        }*/
        /*datasetHeader.setDesc_ids(descIds);
        datasetHeader.setNumberOfUnmappedClusters(0);
        datasetHeader.setUnmappedSignatureSize((byte)1);
        datasetHeader.setUnmappedSignatureLength((byte)32);
        datasetHeader.setNumberOfUnalignedAccessUnits(0);
        datasetHeader.setMultipleAlignment(false);
        datasetHeader.setMultipleSignatureBase(0);

        datasetHeader.setByteOffsetSize(true);*/
    }
}

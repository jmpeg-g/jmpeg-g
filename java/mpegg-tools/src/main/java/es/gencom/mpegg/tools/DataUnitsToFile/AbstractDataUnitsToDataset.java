package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DatasetGroupContainer;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.tools.DataUnitsIndexation;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnits;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

public abstract class AbstractDataUnitsToDataset {
    protected Map<SequenceIdentifier, TreeMap<DATA_CLASS, TreeSet<IndexInfo>>> indexationInfos;

    protected static boolean inferMultiAlignment(DataUnits dataUnits){
        boolean initialized = false;
        boolean value = true;
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){

            if(dataUnitAccessUnit.getAUType() == DATA_CLASS.CLASS_U){
                continue;
            }

            if(!initialized){
                value = dataUnitAccessUnit.getParameter().isMultiple_alignments_flag();
                initialized = true;
            }else{
                if(value != dataUnitAccessUnit.getParameter().isMultiple_alignments_flag()){
                    throw new IllegalArgumentException("Contradictory values regarding multiple alignments");
                }
            }
        }

        return value;
    }

    protected static boolean inferNonOverlapping(DataUnits dataUnits){
        DataUnitsIndexation dataUnitsIndexation = new DataUnitsIndexation(
                dataUnits
        );
        boolean overlapping = false;
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            DataUnitAccessUnit[] resultIntersect = dataUnitsIndexation.getDataUnits(
                    dataUnitAccessUnit.getSequenceId(),
                    dataUnitAccessUnit.getStart(),
                    dataUnitAccessUnit.getEnd()
            );

            if(dataUnitAccessUnit.getAUType() == DATA_CLASS.CLASS_U){
                continue;
            }

            if(resultIntersect.length == 0){
                throw new InternalError();
            }else{
                if(resultIntersect.length > 1){
                    overlapping = true;
                    break;
                }
            }
        }
        return overlapping;
    }

    protected static SequenceIdentifier[] createSequenceIdentifiers(Reference reference, long[] seq_blocks) {
        int numberSequences = reference.getNumberSequences();
        SequenceIdentifier[] sequenceIdentifiers = new SequenceIdentifier[numberSequences];
        int numberNonZero = 0;

        for(int sequenceIdentifier_i = 0; sequenceIdentifier_i < numberSequences; sequenceIdentifier_i++){
            if(seq_blocks[sequenceIdentifier_i] != 0) {
                sequenceIdentifiers[numberNonZero] = new SequenceIdentifier(sequenceIdentifier_i);
                numberNonZero++;
            }
        }

        sequenceIdentifiers = Arrays.copyOf(sequenceIdentifiers, numberNonZero);

        return sequenceIdentifiers;
    }

    protected static boolean inferClassContiguousFlag(Reference reference, DataUnits dataUnits) {
        boolean[][] hasData = new boolean[reference.getNumberSequences()][5];
        boolean hasDataUnmapped = false;

        int priorSequence = -1;
        DATA_CLASS priorClass = null;

        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            if(dataUnitAccessUnit.getAUType() == DATA_CLASS.CLASS_U){
                if(hasDataUnmapped){
                    if(priorClass != DATA_CLASS.CLASS_U){
                        return false;
                    }
                }else{
                    hasDataUnmapped = true;
                    priorClass = DATA_CLASS.CLASS_U;
                }
            } else {
                if(
                        hasData
                                [dataUnitAccessUnit.getSequenceId().getSequenceIdentifier()]
                                [dataUnitAccessUnit.getAUType().ID-1]
                ){
                    if(priorSequence == dataUnitAccessUnit.getSequenceId().getSequenceIdentifier()){
                        if(priorClass != dataUnitAccessUnit.getAUType()){
                            return false;
                        }
                    }
                }else{
                    hasData
                            [dataUnitAccessUnit.getSequenceId().getSequenceIdentifier()]
                            [dataUnitAccessUnit.getAUType().ID-1] = true;
                }
                priorClass = dataUnitAccessUnit.getAUType();
                priorSequence = dataUnitAccessUnit.getSequenceId().getSequenceIdentifier();
            }
        }
        return true;
    }

    protected static long[] discardZeros(long[] seq_blocks) {
        long[] result = new long[seq_blocks.length];

        int numberNonZero = 0;
        for(int i=0; i<seq_blocks.length; i++){
            if(seq_blocks[i] != 0) {
                result[numberNonZero] = seq_blocks[numberNonZero];
                numberNonZero++;
            }
        }
        return Arrays.copyOf(result, numberNonZero);
    }

    private static long[] countAccessUnitsPerSequence(Reference reference, DataUnits dataUnits) {
        int numberSequences = reference.getNumberSequences();
        long[] numberAccessUnits = new long[numberSequences];

        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            if(dataUnitAccessUnit.getAUType() == DATA_CLASS.CLASS_U){
                continue;
            }

            numberAccessUnits[dataUnitAccessUnit.getSequenceId().getSequenceIdentifier()] += 1;
        }

        return numberAccessUnits;
    }

    protected static long[] countBlocksPerSequence(Reference reference, DataUnits dataUnits) {
        int numberSequences = reference.getNumberSequences();
        long[] maxAUId = new long[numberSequences];
        Arrays.fill(maxAUId, -1);

        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            if(dataUnitAccessUnit.getAUType() == DATA_CLASS.CLASS_U){
                continue;
            }

            maxAUId[dataUnitAccessUnit.getSequenceId().getSequenceIdentifier()] =
                    Long.max(
                            maxAUId[dataUnitAccessUnit.getSequenceId().getSequenceIdentifier()],
                            dataUnitAccessUnit.getHeader().getAccess_unit_ID()
                    );
        }

        long[] numberAccessUnits = new long[numberSequences];
        for(int i=0; i<numberSequences; i++){
            numberAccessUnits[i] = maxAUId[i]+1;
        }

        return numberAccessUnits;
    }

    protected static DATA_CLASS[] getDataClasses(DataUnits dataUnits) {
        boolean[] hasData = new boolean[6];

        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            hasData[dataUnitAccessUnit.getAUType().ID - 1] = true;
        }

        int numberClasses = 0;
        for(int class_i=0; class_i<6; class_i++){
            if(hasData[class_i]) numberClasses++;
        }

        DATA_CLASS[] dataClasses = new DATA_CLASS[numberClasses];
        int classesStored = 0;
        for(int class_i=0; class_i<6; class_i++){
            if(hasData[class_i]){
                dataClasses[classesStored] = DATA_CLASS.getDataClass((byte) (class_i+1));
                classesStored++;
            }
        }

        return dataClasses;
    }

    protected static DESCRIPTOR_ID[][] getDescriptorIdentifiers(DataUnits dataUnits, DATA_CLASS[] dataClasses) {
        boolean[][] hasDescriptor = new boolean[5][18];
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            for(DataUnitAccessUnit.Block block : dataUnitAccessUnit.getBlocks()){
                hasDescriptor[dataUnitAccessUnit.getAUType().ID-1][block.getDescriptorId()] = true;
            }
        }

        DESCRIPTOR_ID[][] result = new DESCRIPTOR_ID[dataClasses.length][];
        for(DATA_CLASS dataClass : dataClasses){
            int numberDescriptors = 0;
            for(int descriptor_i = 0; descriptor_i < 18; descriptor_i++){
                if(hasDescriptor[dataClass.ID - 1][descriptor_i]){
                    numberDescriptors++;
                }
            }

            result[dataClass.ID - 1] = new DESCRIPTOR_ID[numberDescriptors];
            int descriptorsWritten = 0;
            for(int descriptor_i = 0; descriptor_i < 18; descriptor_i++){
                if(hasDescriptor[dataClass.ID - 1][descriptor_i]){
                    result[dataClass.ID - 1][descriptorsWritten] = DESCRIPTOR_ID.getDescriptorId((byte) descriptor_i);
                    descriptorsWritten++;
                }
            }
        }
        return result;
    }


    protected static long countNonAligned(DataUnits dataUnits) {
        long count = 0;

        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            if(dataUnitAccessUnit.getAUType() == DATA_CLASS.CLASS_U){
                count++;
            }
        }
        return count;
    }

    protected void putInSequence(IndexInfo indexInfo) {
        TreeMap<DATA_CLASS, TreeSet<IndexInfo>> treeForSequence
                = indexationInfos.computeIfAbsent(indexInfo.getAccessUnitHeader().getSequenceID(), k -> new TreeMap<>());
        putInClass(treeForSequence, indexInfo);
    }

    protected void putInClass(Map<DATA_CLASS, TreeSet<IndexInfo>> treeForSequence, IndexInfo indexInfo){
        TreeSet<IndexInfo> setForClass
                = treeForSequence.get(indexInfo.getAccessUnitHeader().getAUType());
        if(setForClass == null){
            setForClass = new TreeSet<>();
            setForClass.add(indexInfo);
            treeForSequence.put(
                    indexInfo.getAccessUnitHeader().getAUType(),
                    setForClass
            );
        }else{
            setForClass.add(indexInfo);
        }
    }

    public abstract void addAsDataset(
            DatasetGroupContainer datasetGroupContainer,
            DataUnits dataUnits,
            boolean use40BitsPositions,
            short referenceId,
            int default_threshold
    ) throws IOException, DataFormatException;

    AbstractDataUnitsToDataset(){
        indexationInfos = new TreeMap<>();
    }
}

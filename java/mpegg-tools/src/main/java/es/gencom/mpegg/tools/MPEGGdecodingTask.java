package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMRecord;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.decoder.AbstractSequencesSource;
import es.gencom.mpegg.decoder.DataUnitAccessUnitDecoder;
import es.gencom.mpegg.decoder.Exceptions.InvalidSymbolException;
import es.gencom.mpegg.decoder.Exceptions.MissingRequiredDescriptorException;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnits;

import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;

public class MPEGGdecodingTask {
    private final DataUnits dataUnitsToDecode;
    private final TreeMap<SequenceIdentifier, HashMap<DATA_CLASS, HashMap<Long, DataUnitAccessUnit>>> dataUnitsMap;
    private final PriorityQueue<SAMLikeAlignment> decodedAlignments;
    private final Set<DataUnitAccessUnit> decodedAUs;
    private final DataUnitsIndexation dataUnitsIndexation;
    private final String[] sequenceNames;
    private final AbstractSequencesSource sequencesSource;

    private int currentAU = -1;
    private SequenceIdentifier currentSequence;
    private long finalPositionLoaded;


    public MPEGGdecodingTask(
            String[] sequenceNames,
            DataUnits dataUnits,
            AbstractSequencesSource sequencesSource
    ){

        dataUnitsToDecode = dataUnits;
        dataUnitsMap = new TreeMap<>();
        decodedAlignments = new PriorityQueue<>();
        decodedAUs = new HashSet<>();
        dataUnitsIndexation = new DataUnitsIndexation(dataUnits);
        this.sequencesSource = sequencesSource;
        long auId = 0;
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            HashMap<DATA_CLASS, HashMap<Long, DataUnitAccessUnit>> ausInSequence =
                    dataUnitsMap.computeIfAbsent(dataUnitAccessUnit.getHeader().getSequence_ID(), (k)->new HashMap<>());
            HashMap<Long, DataUnitAccessUnit> ausInSequenceAndClass =
                    ausInSequence.computeIfAbsent(dataUnitAccessUnit.getHeader().getAU_type(), (k)->new HashMap<>());
            ausInSequenceAndClass.put(auId, dataUnitAccessUnit);

            if(auId == 0){
                currentSequence = dataUnitAccessUnit.getHeader().getSequence_ID();
            }

            auId++;
        }
        this.sequenceNames = sequenceNames;
    }

    private SAMReadsCollection decodeDataUnitIfRequired(
        DataUnitAccessUnit dataUnitToDecode
    ) throws IOException {

        if(decodedAUs.contains(dataUnitToDecode)){
            return new SAMReadsCollection();
        }
        System.out.println(
                "\tdecoding au of type :"+dataUnitToDecode.getHeader().getAU_type()
                +" id "+dataUnitToDecode.getHeader().getAccess_unit_ID());

        SAMReadsCollection samReadsCollection = new SAMReadsCollection();

        List<Record> records = DataUnitAccessUnitDecoder.decode(
                dataUnitToDecode,
                sequencesSource
        );

        for (Record record : records) {
            samReadsCollection.addRead(record);
        }

        decodedAUs.add(dataUnitToDecode);
        return samReadsCollection;
    }

    private void loadPriorToRead(
            SAMLikeAlignment tentativeSamLikeAlignment
    ) throws IOException {
        long startLookup =
                tentativeSamLikeAlignment.getPosition()
                        - 100*2 //todo improve hardcoded value by reading parameters
                        - 50000;
        long endLookup =
                tentativeSamLikeAlignment.getPosition()
                        + 100*2
                        + 50000;
        DataUnitAccessUnit[] dataUnitAccessUnits =
                dataUnitsIndexation.getDataUnits(
                        tentativeSamLikeAlignment.getSequenceId(),
                        startLookup,
                        endLookup
                );
        for(int dataUnitAccessUnit_i=0; dataUnitAccessUnit_i < dataUnitAccessUnits.length; dataUnitAccessUnit_i++){
            DataUnitAccessUnit extraDataUnit = dataUnitAccessUnits[dataUnitAccessUnit_i];
            SAMReadsCollection samReadsCollectionOverlapping = decodeDataUnitIfRequired(
                    extraDataUnit
            );
            if (samReadsCollectionOverlapping != null) {
                for (SAMLikeAlignment samLikeAlignment : samReadsCollectionOverlapping) {
                    int currentSize = decodedAlignments.size();
                    decodedAlignments.add(samLikeAlignment);
                    if(decodedAlignments.size() != currentSize+1){
                        System.out.println("error detected");
                    }
                }
            }
        }
    }

    private boolean loadMore()
            throws MissingRequiredDescriptorException,
            IndexOutOfBoundsException, IOException, DataFormatException, InvalidSymbolException {
        do {
            currentAU++;
            if (currentAU >= dataUnitsToDecode.getNumberDataUnits()) {
                currentAU = dataUnitsToDecode.getNumberDataUnits();
                return false;
            }

            DataUnitAccessUnit dataUnitToDecode = dataUnitsToDecode.getDataUnitAccessUnit(currentAU);
            System.out.println("loading au " + currentAU + " sequence is " + dataUnitToDecode.getHeader().getSequence_ID());


            SAMReadsCollection samReadsCollection = decodeDataUnitIfRequired(dataUnitToDecode);
            if (samReadsCollection != null) {
                for (SAMLikeAlignment samLikeAlignment : samReadsCollection) {
                    decodedAlignments.add(samLikeAlignment);
                }
            }

            SequenceIdentifier sequenceId = dataUnitToDecode.getHeader().getSequence_ID();

            currentSequence = dataUnitToDecode.getHeader().getSequence_ID();
            if (currentSequence != dataUnitToDecode.getHeader().getSequence_ID()) {
                loadAllInSequence(currentSequence);
            }

            long start = dataUnitToDecode.getHeader().getAu_start_position();
            //todo change this
            long end = start + 100000;


            finalPositionLoaded = end;

            //find and decode AUs which might overlap with the currently decoded one, in order to have it as ordered as
            //possible
            DataUnitAccessUnit[] dataUnitAccessUnits =
                    dataUnitsIndexation.getDataUnits(
                            sequenceId,
                            start - 50000,
                            end + 70000 < 0 ? Long.MAX_VALUE - 1 : end + 70000
                    );
            for(int dataUnitAccessUnit_i=0; dataUnitAccessUnit_i < dataUnitAccessUnits.length; dataUnitAccessUnit_i++){
                DataUnitAccessUnit extraDataUnit = dataUnitAccessUnits[dataUnitAccessUnit_i];
                SAMReadsCollection samReadsCollectionOverlapping = decodeDataUnitIfRequired(
                        extraDataUnit
                );
                if (samReadsCollectionOverlapping != null) {
                    for (SAMLikeAlignment samLikeAlignment : samReadsCollectionOverlapping) {
                        int currentSize = decodedAlignments.size();
                        decodedAlignments.add(samLikeAlignment);
                        if(decodedAlignments.size() != currentSize+1){
                            System.out.println("error detected");
                        }
                    }
                }
            }
        } while(decodedAlignments.isEmpty());
        return true;
    }

    private void loadAllInSequence(SequenceIdentifier sequenceId) throws MissingRequiredDescriptorException, DataFormatException, IOException, IndexOutOfBoundsException, InvalidSymbolException {
        System.out.println("Decoding remaining for :"+sequenceId);
        HashMap<DATA_CLASS, HashMap<Long, DataUnitAccessUnit>> dataUnitsInSequence =  dataUnitsMap.get(sequenceId);
        for(HashMap<Long, DataUnitAccessUnit> dataUnitsInSequenceAndClass : dataUnitsInSequence.values()){
            for(Map.Entry<Long, DataUnitAccessUnit> dataUnitAccessUnitEntry : dataUnitsInSequenceAndClass.entrySet()){
                decodeDataUnitIfRequired(
                        dataUnitAccessUnitEntry.getValue()
                );
            }
        }
        System.out.println("Finished decoding remaining for :"+sequenceId);
    }


    public boolean hasNext() throws MissingRequiredDescriptorException, DataFormatException, IOException, IndexOutOfBoundsException, InvalidSymbolException {
        if (!decodedAlignments.isEmpty()){
            return true;
        }else{
            System.out.println("no more alignments.");
            loadMore();
            return !decodedAlignments.isEmpty();
        }
    }


    public BAMRecord getNextSAMRecord()
            throws IOException, DataFormatException, InvalidSymbolException, MissingRequiredDescriptorException {

        if (decodedAlignments.isEmpty()){
            System.out.println("no more alignments.");
            loadMore();

        }
        SAMLikeAlignment tentativeSamLikeAlignment = decodedAlignments.peek();
        if(tentativeSamLikeAlignment != null) {
            loadPriorToRead(tentativeSamLikeAlignment);
        }

        SAMLikeAlignment samLikeAlignment = decodedAlignments.poll();
        if(samLikeAlignment != null){
            if(
                samLikeAlignment.getSequenceId() == currentSequence
                && samLikeAlignment.getPosition() > (finalPositionLoaded - 50000)
            ){
                loadMore();
            }
            return BAMRecordBuilder.build(sequenceNames, samLikeAlignment);

        }else{
            return null;
        }

    }
}

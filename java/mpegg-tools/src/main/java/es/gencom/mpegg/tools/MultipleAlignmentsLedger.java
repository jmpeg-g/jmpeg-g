package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMFileReader;
import es.gencom.integration.bam.BAMRecord;
import es.gencom.mpegg.decoder.GenomicPosition;
import es.gencom.mpegg.format.SequenceIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MultipleAlignmentsLedger {
    private final BAMFileReader fileReader;
    private final HashMap<String, HashMap<SequenceIdentifier, List<Integer>>> indicesFirstSegment;
    private final HashMap<String, HashMap<SequenceIdentifier, List<Integer>>> indicesLastSegment;
    private final HashMap<String, GenomicPosition> genomicPositionPrincipalFirstSegment = new HashMap<>();
    private final HashMap<String, GenomicPosition> genomicPositionPrincipalLastSegment = new HashMap<>();

    public MultipleAlignmentsLedger(BAMFileReader fileReader) {
        this.fileReader = fileReader;
        this.indicesFirstSegment = new HashMap<>();
        this.indicesLastSegment = new HashMap<>();

        /*for(BAMRecord bamRecord : fileReader){
            if(bamRecord.getTag("CC") == null){
                continue;
            }
            GenomicPosition currentPosition = new GenomicPosition(
                    new SequenceIdentifier(bamRecord.getRefID()),
                    bamRecord.getPositionStart()
            );


            HashMap<SequenceIdentifier, List<Integer>> genomicPositions;

            if(bamRecord.isFirstSegment()) {
                genomicPositions =
                    indicesFirstSegment.computeIfAbsent(bamRecord.getQName(), k -> new HashMap<>());
            } else {
                if(!bamRecord.isLastSegment()){
                    throw new UnsupportedOperationException();
                }
                genomicPositions =
                        indicesLastSegment.computeIfAbsent(bamRecord.getQName(), k -> new HashMap<>());
            }
            List<Integer> genomicPositionsOnSequence =
                    genomicPositions.computeIfAbsent(
                            new SequenceIdentifier(bamRecord.getRefID()), k -> new ArrayList<>());
            genomicPositionsOnSequence.add(bamRecord.getPositionStart());
            if(bamRecord.isPrimary()){
                if(bamRecord.isFirstSegment()) {
                    genomicPositionPrincipalFirstSegment.put(bamRecord.getQName(), currentPosition);
                } else {
                    if(!bamRecord.isLastSegment()){
                        throw new UnsupportedOperationException();
                    }
                    genomicPositionPrincipalLastSegment.put(bamRecord.getQName(), currentPosition);
                }
            }
        }*/
    }

    public List<Integer> getPositionsForSegment(
            String readName, int segment_i, SequenceIdentifier sequenceIdentifier){
        HashMap<SequenceIdentifier, List<Integer>> genomicPositions;
        if(segment_i == 0){
            genomicPositions = indicesFirstSegment.get(readName);
        } else if(segment_i == 1){
            genomicPositions = indicesLastSegment.get(readName);
        } else {
            throw new IllegalArgumentException();
        }
        if(genomicPositions == null){
            return null;
        }
        return genomicPositions.get(sequenceIdentifier);
    }

    public boolean isMultialigned(String readName, int segment_i){
        boolean hasData;
        boolean hasPrincipal;
        if(segment_i == 0){
            hasData = indicesFirstSegment.containsKey(readName);
            hasPrincipal = genomicPositionPrincipalFirstSegment.containsKey(readName);
        } else if(segment_i == 1){
            hasData = indicesLastSegment.containsKey(readName);
            hasPrincipal = genomicPositionPrincipalLastSegment.containsKey(readName);
        } else {
            throw new IllegalArgumentException();
        }

        if(hasData != hasPrincipal){
            throw new IllegalArgumentException();
        }
        return hasData;
    }

    public GenomicPosition getPrincipalPosition(String readName, int segment_i){
        if(segment_i == 0){
            return genomicPositionPrincipalFirstSegment.get(readName);
        } else if(segment_i == 1){
            return genomicPositionPrincipalLastSegment.get(readName);
        } else {
            throw new IllegalArgumentException();
        }
    }


}

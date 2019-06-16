package es.gencom.mpegg.tools;

import es.gencom.mpegg.Record;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.Operation;
import es.gencom.mpegg.format.SequenceIdentifier;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

public class SAMReadsCollection implements Iterable<SAMLikeAlignment> {
    final private TreeSet<SAMLikeAlignment> samReads;

    public SAMReadsCollection() {
        this.samReads = new TreeSet<>();
    }

    public void addRead(Record record){
        if(record.getRecordSegments() > 2){
            throw new UnsupportedOperationException();
        }


        for(int alignment_i=0; alignment_i < record.getMappingPositionsSegment0().length; alignment_i++){
            long[][] mappingPositions = record.getMappingPositionsSegment0();
            if(mappingPositions[alignment_i].length == 0){
                throw new InternalError();
            } else if (mappingPositions[alignment_i].length > 1){
                throw new UnsupportedOperationException();
            }

            boolean noMate;
            boolean mateUnmapped;
            boolean mateOnReverse;
            SequenceIdentifier mateSequenceIdentifier;
            long matePosition;
            if(record.getSplitMate()[0] != SplitType.Unpaired){
                noMate = false;

                int[] alignmentsSegment1 = record.getAlignmentIndexesSegment1(alignment_i);
                if(alignmentsSegment1.length == 0) {
                    throw new InternalError();
                }else if(alignmentsSegment1.length > 1){
                    throw new UnsupportedOperationException();
                }

                if(
                        record.getSplitMate()[alignmentsSegment1[0]] == SplitType.UnmappedOtherRecord
                                || record.getSplitMate()[alignmentsSegment1[0]] == SplitType.UnmappedSameRecord
                ){
                    mateUnmapped = true;
                    mateOnReverse = false;
                    mateSequenceIdentifier = null;
                    matePosition = 0;
                }else {
                    mateUnmapped = false;
                    mateOnReverse = record.getReverseCompliment()[1][
                            record.getAlignmentIndexesSegment1(alignment_i)[0]
                            ][0];
                    mateSequenceIdentifier = record.getSequenceIdSegment1()[
                            record.getAlignmentIndexesSegment1(alignment_i)[0]
                            ];
                    matePosition = record.getMappingPositionsSegment1()[
                            record.getAlignmentIndexesSegment1(alignment_i)[0]
                            ][0];
                }
            }else{
                noMate = true;
                mateUnmapped = false;
                mateOnReverse = false;
                mateSequenceIdentifier = null;
                matePosition = 0;
            }

            Operation[][] operationsMerged = new Operation[record.getOperationType()[0][alignment_i].length][];
            int[][] operationLengthsMerged = new int[record.getOperationLength()[0][alignment_i].length][];
            for(int splice_i=0; splice_i < operationsMerged.length; splice_i++){
                SAMLikeAlignment.mergeOperations(
                        record.getOperationType()[0][alignment_i],
                        record.getOperationLength()[0][alignment_i],
                        operationsMerged,
                        operationLengthsMerged,
                        splice_i
                );
            }

            SAMLikeAlignment samLikeSegment0 = new SAMLikeAlignment(
                    record.getReadName(),
                    record.getSequenceId(),
                    mappingPositions[alignment_i][0],
                    record.getSequenceBytes()[0],
                    record.getQualityValues()[0][0],
                    SAMLikeAlignment.getCigarString(
                            operationsMerged,
                            operationLengthsMerged
                    ),
                    SAMLikeAlignment.getMDTag(
                            operationsMerged,
                            operationLengthsMerged,
                            record.getOriginalBase()[0][alignment_i]
                    ),
                    record.getReverseCompliment()[0][alignment_i][0],
                    noMate,
                    mateSequenceIdentifier,
                    matePosition,
                    record.isRead1First(),
                    record.isUnpaired() || !record.isUnpaired() && !record.isRead1First(),
                    record.isUnpaired(),
                    mateUnmapped,
                    mateOnReverse
            );
            samReads.add(samLikeSegment0);
        }

        if(!(
                record.getSplitMate()[0] == SplitType.SameRecord ||
                        record.getSplitMate()[0] == SplitType.UnmappedSameRecord)
        ) {
            return;
        }

        for(int alignment_i=0; alignment_i < record.getMappingPositionsSegment1().length; alignment_i++){
            if(record.getSplitMate()[alignment_i] != SplitType.UnmappedSameRecord) {
                long[][] mappingPositions = record.getMappingPositionsSegment1();
                if (mappingPositions[alignment_i].length == 0) {
                    throw new InternalError();
                } else if (mappingPositions[alignment_i].length > 1) {
                    throw new UnsupportedOperationException();
                }

                boolean noMate = false;
                boolean mateUnmapped = false;
                boolean mateOnReverse = record.getReverseCompliment()[0][0][0];
                SequenceIdentifier mateSequenceIdentifier = record.getSequenceId();
                long matePosition = record.getMappingPositionsSegment0()[0][0];

                Operation[][] operationsMerged = new Operation[record.getOperationType()[1][alignment_i].length][];
                int[][] operationLengthsMerged = new int[record.getOperationLength()[1][alignment_i].length][];
                for (int splice_i = 0; splice_i < operationsMerged.length; splice_i++) {
                    SAMLikeAlignment.mergeOperations(
                            record.getOperationType()[1][alignment_i],
                            record.getOperationLength()[1][alignment_i],
                            operationsMerged,
                            operationLengthsMerged,
                            splice_i
                    );
                }

                SAMLikeAlignment samLikeSegment1 = new SAMLikeAlignment(
                        record.getReadName(),
                        record.getSequenceIdSegment1()[0],
                        mappingPositions[alignment_i][0],
                        record.getSequenceBytes()[1],
                        record.getQualityValues()[1][0],
                        SAMLikeAlignment.getCigarString(
                                operationsMerged,
                                operationLengthsMerged
                        ),
                        SAMLikeAlignment.getMDTag(
                                operationsMerged,
                                operationLengthsMerged,
                                record.getOriginalBase()[1][alignment_i]
                        ),
                        record.getReverseCompliment()[1][alignment_i][0],
                        noMate,
                        mateSequenceIdentifier,
                        matePosition,
                        !record.isRead1First(),
                        record.isUnpaired() || !record.isUnpaired() && record.isRead1First(),
                        record.isUnpaired(),
                        mateUnmapped,
                        mateOnReverse
                );
                samReads.add(samLikeSegment1);
            }else{
                long[][] mappingPositions = record.getMappingPositionsSegment0();
                if (mappingPositions[alignment_i].length == 0) {
                    throw new InternalError();
                } else if (mappingPositions[alignment_i].length > 1) {
                    throw new UnsupportedOperationException();
                }

                boolean noMate = false;
                boolean mateUnmapped = false;
                boolean mateOnReverse = record.getReverseCompliment()[0][0][0];
                SequenceIdentifier mateSequenceIdentifier = record.getSequenceId();
                long matePosition = record.getMappingPositionsSegment0()[0][0];


                SAMLikeAlignment samLikeSegment1 = new SAMLikeAlignment(
                        record.getReadName(),
                        record.getSequenceId(),
                        mappingPositions[alignment_i][0],
                        record.getSequenceBytes()[1],
                        record.getQualityValues()[1][0],
                        "*",
                        null,
                        record.getReverseCompliment()[0][0][0],
                        noMate,
                        mateSequenceIdentifier,
                        matePosition,
                        !record.isRead1First(),
                        record.isUnpaired() || !record.isUnpaired() && record.isRead1First(),
                        record.isUnpaired(),
                        mateUnmapped,
                        mateOnReverse
                );
                samReads.add(samLikeSegment1);
            }
        }
    }

    @Override
    public Iterator<SAMLikeAlignment> iterator() {
        return samReads.iterator();
    }

    public void remove(Collection<SAMLikeAlignment> toRemove) {
        samReads.removeAll(toRemove);
    }
}

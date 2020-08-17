package es.gencom.mpegg.tools;

import es.gencom.mpegg.Record;
import es.gencom.mpegg.ReverseCompType;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.encoder.Operation;
import es.gencom.mpegg.format.SequenceIdentifier;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

public class SAMReadsCollection implements Iterable<SAMLikeAlignment> {
    final private TreeSet<SAMLikeAlignment> samReads;

    public SAMReadsCollection() {
        this.samReads = new TreeSet<>();
    }

    public void addRead(Record record) {
        if (record.getNumRecordSegments() > 2) {
            throw new UnsupportedOperationException();
        }

        for (int segment_i = 0; segment_i < record.getNumRecordSegments(); segment_i++) {
            int numAlignmentsForSegment = record.getNumAlignmentsForSegment(segment_i);
            if (numAlignmentsForSegment == 0) {
                addUnmappedRead(record, segment_i);
            } else {
                addMappedRead(record, segment_i, numAlignmentsForSegment);
            }
        }
    }

    private void addMappedRead(Record record, int segment_i, int numAlignmentsForSegment) {
        long[][][] mappingPositions = record.getMappingPositions();
        for(int alignment_i=0; alignment_i < numAlignmentsForSegment; alignment_i++) {
            int numSplices = mappingPositions[alignment_i][segment_i].length;

            if(numSplices == 0){
                throw new IllegalArgumentException();
            }
            if(numSplices > 1){
                throw new UnsupportedOperationException();
            }

            byte[][] operationsMerged = new byte[numSplices][];
            int[][] operationLengthsMerged = new int[numSplices][];
            for(int splice_i=0; splice_i < numSplices; splice_i++){
                SAMLikeAlignment.mergeOperations(
                        record.getOperationType()[alignment_i][segment_i],
                        record.getOperationLength()[alignment_i][segment_i],
                        operationsMerged,
                        operationLengthsMerged,
                        splice_i
                );
            }

            boolean noMate = false;
            if(record.getNumRecordSegments() == 0 || record.getSplitMate()[0][(segment_i+1)%record.getNumTemplateSegments()] == SplitType.Unpaired){
                noMate = true;
            }

            SequenceIdentifier mateSequenceIdentifier = null;
            long matePosition = 0;
            boolean mateUnmapped = true;
            boolean mateOnReverse = true;

            if(!noMate){
                int indexNextMate = (segment_i+1)%record.getNumTemplateSegments();
                mateSequenceIdentifier = record.getMappingSequence()[0][indexNextMate];
                matePosition = mappingPositions[0][indexNextMate][0];
                SplitType splitType = record.getSplitMate()[0][indexNextMate];
                mateUnmapped = splitType == SplitType.UnmappedSameRecord
                        || splitType == SplitType.UnmappedDifferentRecordSameAU
                        || splitType == SplitType.UnmappedDifferentRecordDifferentAU;
                if(record.getNumTemplateSegments() == record.getNumAlignedSegments()) {
                    mateOnReverse = record.getReverseCompliment()[0][indexNextMate][0] == ReverseCompType.Reverse;
                }
            }

            SAMLikeAlignment samLikeSegment = new SAMLikeAlignment(
                    record.getReadName(),
                    record.getSequenceId(),
                    mappingPositions[alignment_i][segment_i][0],
                    record.getSequenceBytes()[segment_i],
                    record.getQualityValues()[segment_i][0],
                    SAMLikeAlignment.getCigarString(
                            operationsMerged,
                            operationLengthsMerged
                    ),
                    SAMLikeAlignment.getMDTag(
                            operationsMerged,
                            operationLengthsMerged,
                            record.getOriginalBase()[alignment_i][segment_i]
                    ),
                    record.getReverseCompliment()[alignment_i][segment_i][0] == ReverseCompType.Reverse,
                    noMate,
                    mateSequenceIdentifier,
                    matePosition,
                    record.isRead1First(),
                    record.isUnpaired() || !record.isUnpaired() && !record.isRead1First(),
                    record.isUnpaired(),
                    mateUnmapped,
                    mateOnReverse,
                    (short)record.getMapping_score()[alignment_i][segment_i][0],
                    record.getGroupId()
            );
            samReads.add(samLikeSegment);
        }
    }

    private void addUnmappedRead(Record record, int segment_i) {
        long mappingPositionToReport = 0;
        String cigarString = "*";
        String mdTag = null;
        boolean isOnReverseToReport = false;
        SequenceIdentifier mateSequenceIdentifier = null;

        boolean noMate = false;
        boolean mateUnmapped = false;
        boolean mateOnReverse = false;
        long matePosition = 0;

        if(record.getNumRecordSegments() > 0) {
            noMate = true;
            int nextSegmentIndex = (segment_i + 1) % record.getNumTemplateSegments();
            SplitType splitType = record.getSplitMate()[0][nextSegmentIndex];
            mateUnmapped = splitType == SplitType.UnmappedSameRecord
                    || splitType == SplitType.UnmappedDifferentRecordSameAU
                    || splitType == SplitType.UnmappedDifferentRecordDifferentAU;

            if(!mateUnmapped){
                mappingPositionToReport = record.getMappingPositions()[0][nextSegmentIndex][0];
                mateOnReverse = record.getReverseCompliment()[0][nextSegmentIndex][0] == ReverseCompType.Reverse;
                mateSequenceIdentifier = record.getMappingSequence()[0][nextSegmentIndex];
                matePosition = mappingPositionToReport;
            }
        }


        SAMLikeAlignment samLikeSegment1 = new SAMLikeAlignment(
                record.getReadName(),
                record.getSequenceId(),
                mappingPositionToReport,
                record.getSequenceBytes()[segment_i],
                record.getQualityValues()[segment_i][0],
                cigarString,
                mdTag,
                isOnReverseToReport,
                noMate,
                mateSequenceIdentifier,
                matePosition,
                !record.isRead1First(),
                record.isUnpaired() || !record.isUnpaired() && record.isRead1First(),
                record.isUnpaired(),
                mateUnmapped,
                mateOnReverse,
                (short)0,
                record.getGroupId()
        );
        samReads.add(samLikeSegment1);


    }

    @Override
    public Iterator<SAMLikeAlignment> iterator() {
        return samReads.iterator();
    }

    public void remove(Collection<SAMLikeAlignment> toRemove) {
        samReads.removeAll(toRemove);
    }
}

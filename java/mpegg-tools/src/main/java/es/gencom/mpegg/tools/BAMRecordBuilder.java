package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMRecord;
import es.gencom.integration.sam.tag.MD;

public class BAMRecordBuilder {
    public static BAMRecord build(
            String[] sequenceNames,
            SAMLikeAlignment samLikeAlignment
    ) {
        BAMRecord bamRecord = new BAMRecord();
        bamRecord.setQName(samLikeAlignment.getReadName());
        bamRecord.setPositionStart((int) samLikeAlignment.getPosition()+1);
        bamRecord.setSequence(samLikeAlignment.getSequence());
        bamRecord.setCIGAR(samLikeAlignment.getCigarString());
        bamRecord.setRefID(samLikeAlignment.getSequenceId().getSequenceIdentifier());
        bamRecord.setRName(sequenceNames[samLikeAlignment.getSequenceId().getSequenceIdentifier()]);

        if(!samLikeAlignment.isPaired()){
            bamRecord.setNext_refID(-1);
            bamRecord.setRNameNext("*");
            bamRecord.setNextPositionStart(0);
        }else {
            if (samLikeAlignment.isMateUnmapped()) {
                bamRecord.setNext_refID(samLikeAlignment.getSequenceId().getSequenceIdentifier());
                bamRecord.setRNameNext(sequenceNames[samLikeAlignment.getSequenceId().getSequenceIdentifier()]);
                bamRecord.setNextPositionStart((int) samLikeAlignment.getPosition() + 1);
            } else {
                bamRecord.setNext_refID(samLikeAlignment.getMateSequenceId().getSequenceIdentifier());
                bamRecord.setRNameNext(sequenceNames[samLikeAlignment.getMateSequenceId().getSequenceIdentifier()]);
                bamRecord.setNextPositionStart((int) samLikeAlignment.getMatePosition() + 1);
            }
        }
        bamRecord.setNextSegmentReverseComplemented(samLikeAlignment.isMateOnReverse());
        bamRecord.setNextSegmentUnmapped(samLikeAlignment.isMateUnmapped());

        bamRecord.setHasMultipleSegments(samLikeAlignment.isPaired());
        bamRecord.setFirstSegment(samLikeAlignment.isFirstMate());
        bamRecord.setLastSegment(samLikeAlignment.isLastMate());
        bamRecord.setReverseComplemented(samLikeAlignment.isOnReverse());

        if(samLikeAlignment.hasQualities()) {
            short[] qualityArray = samLikeAlignment.getQualities();
            byte[] castedQualities = new byte[qualityArray.length];
            for(int quality_i = 0; quality_i < qualityArray.length; quality_i++){
                castedQualities[quality_i] = (byte) qualityArray[quality_i];
            }
            bamRecord.setQualityBytes(castedQualities);
        }

        if(samLikeAlignment.getMDTag() != null) {
            bamRecord.setTag(new MD(samLikeAlignment.getMDTag()));
        }

        return bamRecord;
    }
}
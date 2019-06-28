package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMHeader;
import es.gencom.integration.bam.BAMRecord;
import es.gencom.integration.sam.CIGARDecoder;
import es.gencom.integration.sam.SAMRecord;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.Operation;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.decoder.AbstractSequencesSource;
import es.gencom.mpegg.decoder.GenomicPosition;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class RecordConstructor {
    private final long readId;
    private final String readName;
    private final String groupName;
    private BAMRecord[] records;
    private int numberRecords;
    private DATA_CLASS dataClass = DATA_CLASS.CLASS_P;
    private long lastPositionRegistered;
    private boolean requiresTwoSegments;
    private boolean hasRecordFirst;
    private boolean hasRecordSecond;
    private boolean hasDataFirst;
    private boolean hasDataSecond;
    private boolean toBeDoneInOneRecord;
    private boolean twoReadsToBeDoneInOneRecord;

    public RecordConstructor(long readId, String readName, String groupName, BAMRecord bamRecord, short threshold){
        this.readId = readId;
        this.readName = readName;
        this.groupName = groupName;

        records = new BAMRecord[2];
        numberRecords = 0;

        requiresTwoSegments = bamRecord.hasMultipleSegments();
        hasDataFirst = bamRecord.isFirstSegment();
        hasRecordFirst = bamRecord.isFirstSegment();
        hasDataSecond = bamRecord.isLastSegment();
        hasRecordSecond = bamRecord.isLastSegment();

        if(requiresTwoSegments){
            if(bamRecord.isNextSegmentUnmapped()){
                toBeDoneInOneRecord = true;
                twoReadsToBeDoneInOneRecord = true;
            }else{
                if(bamRecord.getRefID() == bamRecord.getNextRefID()){
                    toBeDoneInOneRecord = Long.max(
                            bamRecord.getPositionStart(),
                            bamRecord.getNextPositionStart()
                    ) - Long.min(
                            bamRecord.getPositionStart(),
                            bamRecord.getNextPositionStart()
                    ) < threshold;
                    twoReadsToBeDoneInOneRecord = toBeDoneInOneRecord;
                }else{
                    toBeDoneInOneRecord = false;
                    twoReadsToBeDoneInOneRecord = false;
                }
            }
        }else{
            toBeDoneInOneRecord = true;
            twoReadsToBeDoneInOneRecord = false;
        }

        addSegment(bamRecord);
    }

    public boolean couldBeCompleted(){
        if(requiresTwoSegments){
            if(toBeDoneInOneRecord){
                return hasRecordFirst && hasRecordSecond;
            }else{
                return hasDataFirst && hasDataSecond;
            }
        }else{
            return true;
        }
    }

    public void addSegment(BAMRecord bamRecord){
        if(!bamRecord.getQName().equals(readName)){
            throw new IllegalArgumentException();
        }

        if(records.length == numberRecords){
            records = Arrays.copyOf(records, records.length*2);
        }
        records[numberRecords] = bamRecord;
        numberRecords++;

        if(bamRecord.isFirstSegment()){
            hasRecordFirst = true;
            hasDataFirst = true;
        }
        if(bamRecord.isLastSegment()){
            hasRecordSecond = true;
            hasDataSecond = true;
        }

        if(bamRecord.hasMultipleSegments() && !bamRecord.isNextSegmentUnmapped()){
            if(bamRecord.isFirstSegment()){
                hasDataSecond = true;
            }
            if(bamRecord.isLastSegment()){
                hasDataFirst = true;
            }
        }
        if(!bamRecord.isUnmappedSegment()) {
            lastPositionRegistered = bamRecord.getPositionStart() - 1;
        }
    }

    private static class InformationAlignment{
        private final int sequenceId;
        private final long position;
        private final boolean unmapped;
        private final boolean reverseCompliment;
        private BAMRecord bamRecord;

        private InformationAlignment() {
            this(true, 0, 0, false, null);
        }

        private InformationAlignment(BAMRecord bamRecord) {
            this(true, 0, 0, false, bamRecord);
        }

        private InformationAlignment(int sequenceId, long position, boolean reverseCompliment) {
            this(false, sequenceId, position, reverseCompliment, null);
        }

        private InformationAlignment(int sequenceId, long position, boolean reverseCompliment, BAMRecord bamRecord) {
            this(false, sequenceId, position, reverseCompliment, bamRecord);
        }

        private InformationAlignment(
                boolean unmapped,
                int sequenceId,
                long position,
                boolean reverseCompliment,
                BAMRecord samRecord
        ) {
            this.unmapped = unmapped;
            this.sequenceId = sequenceId;
            this.position = position;
            this.reverseCompliment = reverseCompliment;
            this.bamRecord = samRecord;
        }

        public boolean isReverseCompliment() {
            return reverseCompliment;
        }

        public BAMRecord getBamRecord() {
            return bamRecord;
        }

        public boolean hasRecord(){
            return getBamRecord() != null;
        }

        public void setSAMRecord(BAMRecord bamRecord) {
            this.bamRecord = bamRecord;
        }

        public boolean isUnmapped() {
            return unmapped;
        }
    }



    public Record construct(AbstractSequencesSource sequencesSource, BAMHeader bamHeader) throws IOException {
        ArrayList<Map<GenomicPosition, InformationAlignment>> sortedRecords = new ArrayList<>(2);
        sortedRecords.add(new TreeMap<>());
        sortedRecords.add(new TreeMap<>());

        sortRecords(
                sequencesSource, records, numberRecords, sortedRecords, bamHeader
        );


        SequenceIdentifier[][] sequenceInformation = new SequenceIdentifier[2][];
        long[][][] positionInformation = new long[2][][];
        SplitType[][] splitTypeInformation = new SplitType[2][];
        boolean[][][] reverseCompInformation = new boolean[2][][];
        BAMRecord[][] bamRecordsInformation = new BAMRecord[2][];
        for(int segment_i=0; segment_i<2; segment_i++){
            sequenceInformation[segment_i] = new SequenceIdentifier[sortedRecords.get(segment_i).size()];
            positionInformation[segment_i] = new long[sortedRecords.get(segment_i).size()][1];
            splitTypeInformation[segment_i] = new SplitType[sortedRecords.get(segment_i).size()];
            reverseCompInformation[segment_i] = new boolean[sortedRecords.get(segment_i).size()][1];
            bamRecordsInformation[segment_i] = new BAMRecord[sortedRecords.get(segment_i).size()];

            int alignment_i=0;
            for(
                    Map.Entry<GenomicPosition, InformationAlignment> alignmentEntry
                    : sortedRecords.get(segment_i).entrySet())
            {
                GenomicPosition genomicPosition = alignmentEntry.getKey();

                sequenceInformation[segment_i][alignment_i] = genomicPosition.getSequenceId();
                positionInformation[segment_i][alignment_i][0] = genomicPosition.getPosition();
                reverseCompInformation[segment_i][alignment_i][0] = alignmentEntry.getValue().isReverseCompliment();
                if(alignmentEntry.getValue().hasRecord()){
                    if(alignmentEntry.getValue().isUnmapped()){
                        splitTypeInformation[segment_i][alignment_i] = SplitType.UnmappedSameRecord;
                    }else {
                        splitTypeInformation[segment_i][alignment_i] = SplitType.SameRecord;
                    }
                    bamRecordsInformation[segment_i][alignment_i] = alignmentEntry.getValue().getBamRecord();
                }else {
                    splitTypeInformation[segment_i][alignment_i] = SplitType.DifferentRecord;
                    bamRecordsInformation[segment_i][alignment_i] = null;
                }
            }
        }

        if(requiresTwoSegments) {
            placeInRecordDataFirst(
                    sequenceInformation,
                    positionInformation,
                    splitTypeInformation,
                    reverseCompInformation,
                    bamRecordsInformation
            );
        }else{
            sequenceInformation[1] = null;
            positionInformation[1] = null;
            splitTypeInformation[1] = new SplitType[]{SplitType.Unpaired};
            reverseCompInformation[1] = null;
            bamRecordsInformation[1] = null;




        }

        boolean unpaired;
        if(!requiresTwoSegments){
            unpaired = true;
        }else {
            unpaired = sequenceInformation[0].length == 0 || sequenceInformation[1].length == 0;
        }
        boolean twoSegmentsStoredTogether = false;
        if(!unpaired){
            twoSegmentsStoredTogether =
                    (splitTypeInformation[0][0]==SplitType.SameRecord
                            || splitTypeInformation[0][0]==SplitType.UnmappedSameRecord)
                            && ( splitTypeInformation[1][0]==SplitType.SameRecord
                            || splitTypeInformation[1][0]==SplitType.UnmappedSameRecord);
        }

        boolean isEncodingRead0 = true;
        if(!unpaired) {
            if (twoSegmentsStoredTogether) {
                if(splitTypeInformation[0][0] == SplitType.UnmappedSameRecord){
                    isEncodingRead0 = false;
                }else if(splitTypeInformation[1][0] != SplitType.UnmappedSameRecord){
                    isEncodingRead0 = positionInformation[0][0][0] <= positionInformation[1][0][0];
                }
            }else{
                isEncodingRead0 = splitTypeInformation[0][0]==SplitType.SameRecord;
            }
        }else{
            isEncodingRead0 = sequenceInformation[0].length != 0;
        }

        byte[][] sequenceBytes = new byte[2][];
        short[][][] qualityValues = new short[2][2][];
        for(int segment_i=0; segment_i<(requiresTwoSegments ? 2:1); segment_i++) {
            if (
                    bamRecordsInformation[segment_i] != null
                            && bamRecordsInformation[segment_i].length > 0
                            && bamRecordsInformation[segment_i][0] != null
            ) {
                SAMRecord samRecord = bamRecordsInformation[segment_i][0];
                sequenceBytes[segment_i] = samRecord.getSequenceBytes();
                qualityValues[segment_i][0] = new short[samRecord.getQualityBytes().length];
                for(int i=0; i<samRecord.getQualityBytes().length; i++){
                    qualityValues[segment_i][0][i] = samRecord.getQualityBytes()[i];
                }
                if(samRecord.getOriginalQualityBytes() != null) {
                    qualityValues[segment_i][1] = new short[samRecord.getOriginalQualityBytes().length];
                    for (int i = 0; i < samRecord.getQualityBytes().length; i++) {
                        qualityValues[segment_i][1][i] = samRecord.getOriginalQualityBytes()[i];
                    }
                }else{
                    qualityValues[segment_i][1] = null;
                }
            }
        }



        SequenceIdentifier sequenceId = sequencesSource.getSequenceIdentifier(
                bamHeader.getReference(bamRecordsInformation[isEncodingRead0 ? 0 : 1][0].getRefID()).name
        );

        long[][][] lengthSplices = new long[2][][];
        for(int segment_i=0; segment_i<2; segment_i++) {
            SequenceIdentifier[] sequenceInformationForSegment = sequenceInformation[segment_i];
            if(sequenceInformationForSegment == null){
                continue;
            }
            lengthSplices[segment_i] = new long[sequenceInformationForSegment.length][1];
            for(int alignment_i=0; alignment_i<sequenceInformationForSegment.length; alignment_i++){
                if(
                        splitTypeInformation[segment_i][alignment_i]==SplitType.SameRecord
                                || splitTypeInformation[segment_i][alignment_i]==SplitType.UnmappedSameRecord
                ){
                    lengthSplices[segment_i][alignment_i][0] = sequenceBytes[segment_i].length;
                }else {
                    lengthSplices[segment_i] = null;
                }
            }
        }

        byte[][][][] operationType = new byte[2][][][];
        int[][][][] operationLength = new int[2][][][];
        byte[][][][] originalBase = new byte[2][][][];

        for(int segment_i=0; segment_i<(requiresTwoSegments ? 2:1); segment_i++){
            operationType[segment_i] = new byte[sequenceInformation[segment_i].length][][];
            operationLength[segment_i] = new int[sequenceInformation[segment_i].length][][];
            originalBase[segment_i] = new byte[sequenceInformation[segment_i].length][][];

            for(int alignment_i = 0; alignment_i < sequenceInformation[segment_i].length; alignment_i++){
                if(splitTypeInformation[segment_i][alignment_i] == SplitType.SameRecord){
                    DATA_CLASS classNextAlignment = populateCigar(
                            bamRecordsInformation[segment_i][alignment_i],
                            sequencesSource,
                            operationType[segment_i],
                            operationLength[segment_i],
                            originalBase[segment_i],
                            alignment_i,
                            bamHeader
                    );
                    if(alignment_i == 0) {
                        if (classNextAlignment.ID > dataClass.ID) {
                            dataClass = classNextAlignment;
                        }
                    }
                }else{
                    operationType[segment_i][alignment_i] = null;
                    operationLength[segment_i][alignment_i] = null;
                    originalBase[segment_i][alignment_i] = null;
                }
            }
        }

        long[][][] mapping_score = new long[2][][];
        for(int segment_i=0; segment_i<(requiresTwoSegments ? 2:1); segment_i++){
            mapping_score[segment_i] = new long[sequenceInformation[segment_i].length][1];
            for(int alignment_i = 0; alignment_i < sequenceInformation[segment_i].length; alignment_i++) {
                if (splitTypeInformation[segment_i][alignment_i] == SplitType.SameRecord) {
                    mapping_score[segment_i][alignment_i][0] =
                            bamRecordsInformation[segment_i][alignment_i].getMappingQuality();
                }else{
                    mapping_score[segment_i][alignment_i][0] = 0;
                }
            }
        }

        if(!isEncodingRead0) {
            swapFirstDimension(sequenceInformation);
            swapFirstDimension(positionInformation);
            swapFirstDimension(splitTypeInformation);
            swapFirstDimension(reverseCompInformation);
            swapFirstDimension(bamRecordsInformation);
            swapFirstDimension(lengthSplices);
            swapFirstDimension(operationType);
            swapFirstDimension(operationLength);
            swapFirstDimension(originalBase);
            swapFirstDimension(mapping_score);
            swapFirstDimension(sequenceBytes);
            swapFirstDimension(qualityValues);
        }

        if(requiresTwoSegments && splitTypeInformation.length == 2) {
            if (splitTypeInformation[0][0] != SplitType.UnmappedSameRecord &&
                    splitTypeInformation[1][0] == SplitType.UnmappedSameRecord) {
                dataClass = DATA_CLASS.CLASS_HM;
            }
        }

        int[][] alignPtr = null;
        if(!unpaired) {
            int maxAlignPtrLength = 1;
            for(int segment_i=0; segment_i<2; segment_i++){
                maxAlignPtrLength *= sequenceInformation[segment_i].length;
            }
            alignPtr = new int[maxAlignPtrLength][2];

            int entry_i=0;
            for(int alignment0_i = 0; alignment0_i<sequenceInformation[0].length; alignment0_i++){
                if(splitTypeInformation[1][alignment0_i] == SplitType.SameRecord){
                    SequenceIdentifier sequenceMate = sequenceInformation[1][alignment0_i];
                    long sequencePosition = positionInformation[1][alignment0_i][0];

                    int alignment1 = -1;
                    for(int alignment1_i=0; alignment1_i<sequenceInformation[1].length; alignment1_i++){
                        if(
                                sequenceInformation[1][alignment1_i] == sequenceMate
                                        && positionInformation[1][alignment1_i][0] == sequencePosition
                        ){
                            alignment1 = alignment1_i;
                            break;
                        }
                    }
                    if(alignment1 == -1){
                        throw new InternalError();
                    }
                    alignPtr[entry_i][0] = alignment0_i;
                    alignPtr[entry_i][1] = alignment1;
                }else if(splitTypeInformation[1][alignment0_i] == SplitType.UnmappedSameRecord){
                    alignPtr[entry_i][0] = alignment0_i;
                    alignPtr[entry_i][1] = 0;
                }
            }
        }

        removeDataNotInRecordForEncodingSegment(
                sequenceInformation,
                positionInformation,
                splitTypeInformation,
                reverseCompInformation,
                bamRecordsInformation,
                lengthSplices,
                operationType,
                operationLength,
                originalBase);

        return new Record(
                readId,
                readName,
                groupName,
                isEncodingRead0,
                unpaired,
                sequenceBytes,
                qualityValues,
                sequenceId,
                positionInformation[0],
                splitTypeInformation[1],
                sequenceInformation[1],
                positionInformation[1],
                lengthSplices,
                operationType,
                operationLength,
                originalBase,
                reverseCompInformation,
                mapping_score,
                alignPtr
        );
    }

    private void removeDataNotInRecordForEncodingSegment(
            SequenceIdentifier[][] sequenceInformation,
            long[][][] positionInformation,
            SplitType[][] splitTypeInformation,
            boolean[][][] reverseCompInformation,
            BAMRecord[][] bamRecordsInformation,
            long[][][] lengthSplices,
            byte[][][][] operationType,
            int[][][][] operationLength,
            byte[][][][] originalBase
    ) {
        int countSegment0InRecord = 0;
        for(int alignment_i=0; alignment_i < splitTypeInformation[0].length; alignment_i++){
            if(splitTypeInformation[0][alignment_i] == SplitType.SameRecord){
                countSegment0InRecord++;
            }
        }

        SequenceIdentifier[] sequenceInformationBuffer = new SequenceIdentifier[countSegment0InRecord];
        long[][] positionInformationBuffer = new long[countSegment0InRecord][];
        SplitType[] splitTypeInformationBuffer = new SplitType[countSegment0InRecord];
        boolean[][] reverseCompInformationBuffer = new boolean[countSegment0InRecord][];
        BAMRecord[] bamRecordsInformationBuffer = new BAMRecord[countSegment0InRecord];
        long[][] lengthSplicesBuffer = new long[countSegment0InRecord][];
        byte[][][] operationTypeBuffer = new byte[countSegment0InRecord][][];
        int[][][] operationLengthBuffer = new int[countSegment0InRecord][][];
        byte[][][] originalBaseBuffer = new byte[countSegment0InRecord][][];

        int entry_i=0;
        for(int alignment_i=0; alignment_i < splitTypeInformation[0].length; alignment_i++){
            if(splitTypeInformation[0][alignment_i] == SplitType.SameRecord){
                sequenceInformationBuffer[entry_i] = sequenceInformation[0][alignment_i];
                positionInformationBuffer[entry_i] = positionInformation[0][alignment_i];
                splitTypeInformationBuffer[entry_i] = splitTypeInformation[0][alignment_i];
                reverseCompInformationBuffer[entry_i] = reverseCompInformation[0][alignment_i];
                bamRecordsInformationBuffer[entry_i] = bamRecordsInformation[0][alignment_i];
                lengthSplicesBuffer[entry_i] = lengthSplices[0][alignment_i];
                operationTypeBuffer[entry_i] = operationType[0][alignment_i];
                operationLengthBuffer[entry_i] = operationLength[0][alignment_i];
                originalBaseBuffer[entry_i] = originalBase[0][alignment_i];
                entry_i++;
            }
        }

        sequenceInformation[0] = sequenceInformationBuffer;
        positionInformation[0] = positionInformationBuffer;
        splitTypeInformation[0] = splitTypeInformationBuffer;
        reverseCompInformation[0] = reverseCompInformationBuffer;
        bamRecordsInformation[0] = bamRecordsInformationBuffer;
        lengthSplices[0] = lengthSplicesBuffer;
        operationType[0] = operationTypeBuffer;
        operationLength[0] = operationLengthBuffer;
        originalBase[0] = originalBaseBuffer;
    }

    private static <T> void swapFirstDimension(T[] data) {
        if(data.length != 2){
            throw new IllegalArgumentException();
        }
        T buffer = data[0];
        data[0] = data[1];
        data[1] = buffer;
    }

    private static void placeInRecordDataFirst(
            SequenceIdentifier[][] sequenceInformation,
            long[][][] positionInformation,
            SplitType[][] splitTypeInformation,
            boolean[][][] reverseCompInformation,
            SAMRecord[][] samRecordsInformation
    ) {
        for(int segment_i=0; segment_i<2; segment_i++){
            for(int alignment_i=0; alignment_i < sequenceInformation[segment_i].length; alignment_i++){
                if(splitTypeInformation[segment_i][alignment_i] == SplitType.SameRecord){

                    SequenceIdentifier sequenceInformationBuffer = sequenceInformation[segment_i][alignment_i];
                    long[] positionInformationBuffer = positionInformation[segment_i][alignment_i];
                    SplitType splitTypeInformationBuffer = splitTypeInformation[segment_i][alignment_i];
                    boolean[] reverseCompInformationBuffer = reverseCompInformation[segment_i][alignment_i];
                    SAMRecord samRecordsInformationBuffer = samRecordsInformation[segment_i][alignment_i];

                    sequenceInformation[segment_i][alignment_i] = sequenceInformation[segment_i][0];
                    positionInformation[segment_i][alignment_i] = positionInformation[segment_i][0];
                    splitTypeInformation[segment_i][alignment_i] = splitTypeInformation[segment_i][0];
                    reverseCompInformation[segment_i][alignment_i] = reverseCompInformation[segment_i][0];
                    samRecordsInformation[segment_i][alignment_i] = samRecordsInformation[segment_i][0];

                    sequenceInformation[segment_i][0] = sequenceInformationBuffer;
                    positionInformation[segment_i][0] = positionInformationBuffer;
                    splitTypeInformation[segment_i][0] = splitTypeInformationBuffer;
                    reverseCompInformation[segment_i][0] = reverseCompInformationBuffer;
                    samRecordsInformation[segment_i][0] = samRecordsInformationBuffer;

                    break;
                }
            }
        }
    }

    private static boolean[][][] populateReverseCompliment(SAMRecord[][] samRecordsPerSegment, int[] numberRecords) {
        boolean[][][] result = new boolean[2][][];

        for(int segment_i=0; segment_i<2; segment_i++){
            result[segment_i] = new boolean[numberRecords[segment_i]][1];

            for(int record_i=0; record_i < numberRecords[segment_i]; record_i++){
                result[segment_i][record_i][0] = samRecordsPerSegment[segment_i][record_i].isReverseComplemented();
            }
        }

        return result;
    }

    public DATA_CLASS getDataClass(){
        return dataClass;
    }


    private static DATA_CLASS populateCigar(
            BAMRecord bamRecord,
            AbstractSequencesSource sequencesSource,
            byte[][][] operations,
            int[][][] operationLength,
            byte[][][] originalBases,
            int alignment_i,
            BAMHeader bamHeader
    ) throws IOException {
        CIGARDecoder cigar = new CIGARDecoder(bamRecord.getCIGAR());
        int toCopy = 0;

        int sizeFirstClipping = 0;
        if(cigar.getOperation()[0]=='S' || cigar.getOperation()[0]=='H'){
            sizeFirstClipping = cigar.getOperationLength()[0];
        }

        for(int i=0; i<cigar.getOperation().length; i++){
            if(cigar.getOperation()[i]=='M' || cigar.getOperation()[i]=='X' || cigar.getOperation()[i]=='='){
                toCopy += cigar.getOperationLength()[i];
            } else if (cigar.getOperation()[i]=='D'){
                toCopy += cigar.getOperationLength()[i];
            }
        }

        Payload referenceSubSequence = sequencesSource.getSubsequence(
                sequencesSource.getSequenceIdentifier(bamHeader.getReference(bamRecord.getRefID()).name),
                bamRecord.getPositionStart()-1,
                bamRecord.getPositionStart() + toCopy
        );

        operations[alignment_i] = new byte[1][128];
        operationLength[alignment_i] = new int[1][128];
        originalBases[alignment_i] = new byte[1][128];

        int numberOperations = 0;
        int numberOriginalBases = 0;

        int currentPositionInReadString = 0;
        int currentPositionInCopy = 0;
        int currentIndex = 0;

        DATA_CLASS dataClass = DATA_CLASS.CLASS_P;

        for(int operation_i=0; operation_i<cigar.getOperation().length; operation_i++){
            if(
                    cigar.getOperation()[operation_i]=='M'
                            || cigar.getOperation()[operation_i]=='X'
                            || cigar.getOperation()[operation_i]=='='
            ){
                //M
                for(int i=0; i<cigar.getOperationLength()[operation_i]; i++){
                    byte originalBase = (byte) Character.toUpperCase(referenceSubSequence.readByte());
                    if (
                            bamRecord.getSequenceBytes()[currentPositionInReadString] == originalBase
                    ){
                        operations[alignment_i][0][numberOperations] = Operation.Match;
                        operationLength[alignment_i][0][numberOperations] = 1;
                        numberOperations++;

                        if(operations[alignment_i][0].length == numberOperations){
                            operations[alignment_i][0] = Arrays.copyOf(
                                    operations[alignment_i][0], numberOperations*2
                            );
                            operationLength[alignment_i][0] = Arrays.copyOf(
                                    operationLength[alignment_i][0], numberOperations*2
                            );
                        }
                    }else{
                        byte operationType;
                        if(bamRecord.getSequenceBytes()[currentPositionInReadString] == 'N'){
                            operationType = Operation.SubstitutionToN;

                            if (dataClass == DATA_CLASS.CLASS_P){
                                dataClass = DATA_CLASS.CLASS_N;
                            }
                        }else {
                            operationType = Operation.Substitution;

                            if (dataClass == DATA_CLASS.CLASS_P || dataClass == DATA_CLASS.CLASS_N){
                                dataClass = DATA_CLASS.CLASS_M;
                            }
                        }
                        operations[alignment_i][0][numberOperations] = operationType;
                        operationLength[alignment_i][0][numberOperations] = 1;
                        numberOperations++;

                        if(operations[alignment_i][0].length == numberOperations){
                            operations[alignment_i][0] = Arrays.copyOf(
                                    operations[alignment_i][0], numberOperations*2
                            );
                            operationLength[alignment_i][0] = Arrays.copyOf(
                                    operationLength[alignment_i][0], numberOperations*2
                            );
                        }

                        originalBases[alignment_i][0][numberOriginalBases] = originalBase;
                        numberOriginalBases++;

                        if(originalBases[alignment_i][0].length == numberOriginalBases){
                            originalBases[alignment_i][0] = Arrays.copyOf(
                                    originalBases[alignment_i][0], numberOriginalBases*2
                            );
                        }
                    }
                    currentPositionInReadString++;
                    currentPositionInCopy++;
                }
                currentIndex++;
            } else if (cigar.getOperation()[operation_i]=='I' || cigar.getOperation()[operation_i]=='D'){
                //I or D
                dataClass = DATA_CLASS.CLASS_I;
                if(cigar.getOperation()[operation_i]=='I'){
                    //I
                    for(int i=0; i<cigar.getOperationLength()[operation_i]; i++){
                        operations[alignment_i][0][numberOperations] = Operation.Insert;
                        operationLength[alignment_i][0][numberOperations] = 1;
                        numberOperations++;

                        if(operations[alignment_i][0].length == numberOperations){
                            operations[alignment_i][0] = Arrays.copyOf(
                                    operations[alignment_i][0], numberOperations*2
                            );
                            operationLength[alignment_i][0] = Arrays.copyOf(
                                    operationLength[alignment_i][0], numberOperations*2
                            );
                        }

                        currentPositionInReadString++;
                    }
                }else{
                    //D
                    for(int i=0; i<cigar.getOperationLength()[operation_i]; i++){
                        operations[alignment_i][0][numberOperations] = Operation.Delete;
                        operationLength[alignment_i][0][numberOperations] = 1;
                        numberOperations++;

                        if(operations[alignment_i][0].length == numberOperations){
                            operations[alignment_i][0] = Arrays.copyOf(
                                    operations[alignment_i][0], numberOperations*2
                            );
                            operationLength[alignment_i][0] = Arrays.copyOf(
                                    operationLength[alignment_i][0], numberOperations*2
                            );
                        }

                        originalBases[alignment_i][0][numberOriginalBases] =
                                referenceSubSequence.readByte();
                        numberOriginalBases++;

                        if(originalBases[alignment_i][0].length == numberOriginalBases){
                            originalBases[alignment_i][0] = Arrays.copyOf(
                                    originalBases[alignment_i][0], numberOriginalBases*2
                            );
                        }

                        currentPositionInCopy++;
                    }
                }
                currentIndex++;
            } else if (cigar.getOperation()[operation_i]=='S'){
                dataClass = DATA_CLASS.CLASS_I;

                operations[alignment_i][0][numberOperations] = Operation.SoftClip;
                operationLength[alignment_i][0][numberOperations] = cigar.getOperationLength()[operation_i];
                numberOperations++;

                if(operations[alignment_i][0].length == numberOperations){
                    operations[alignment_i][0] = Arrays.copyOf(
                            operations[alignment_i][0], numberOperations*2
                    );
                    operationLength[alignment_i][0] = Arrays.copyOf(
                            operationLength[alignment_i][0], numberOperations*2
                    );
                }

                currentPositionInReadString += cigar.getOperationLength()[operation_i];
                currentIndex += cigar.getOperationLength()[operation_i];
            }
        }
        operations[alignment_i][0] = Arrays.copyOf(operations[alignment_i][0], numberOperations);
        operationLength[alignment_i][0] = Arrays.copyOf(operationLength[alignment_i][0], numberOperations);
        originalBases[alignment_i][0] = Arrays.copyOf(originalBases[alignment_i][0], numberOriginalBases);
        return dataClass;
    }

    static private void sortRecords(
            AbstractSequencesSource sequencesSource,
            BAMRecord[] records,
            int numberRecords,
            ArrayList<Map<GenomicPosition, InformationAlignment>> recordInformations,
            BAMHeader bamHeader
    ){
        for(int record_i = 0; record_i < numberRecords; record_i++){
            BAMRecord samRecordToSort = records[record_i];

            byte segmentRead = 0;
            byte segmentMate = 1;
            GenomicPosition recordPosition;

            if(samRecordToSort.isUnmappedSegment()){
                if(samRecordToSort.hasMultipleSegments() && samRecordToSort.isLastSegment()) {
                    segmentRead = 1;
                    segmentMate = 0;
                }
                recordPosition = GenomicPosition.getUnmapped();
            } else {
                recordPosition = recordToGenomicPosition(samRecordToSort, sequencesSource, bamHeader);
                if(samRecordToSort.hasMultipleSegments() && samRecordToSort.isLastSegment()) {
                    segmentRead = 1;
                    segmentMate = 0;
                }
            }

            if(recordInformations.get(segmentRead).containsKey(recordPosition)){
                recordInformations.get(segmentRead).get(recordPosition).setSAMRecord(samRecordToSort);
            }else{
                InformationAlignment informationAlignment;
                if(recordPosition.isUnmapped()){
                    informationAlignment = new InformationAlignment(samRecordToSort);
                }else{
                    informationAlignment = new InformationAlignment(
                            recordPosition.getSequenceId().getSequenceIdentifier(),
                            recordPosition.getPosition(),
                            samRecordToSort.isReverseComplemented(), samRecordToSort
                    );
                }
                recordInformations.get(segmentRead).put(
                        recordPosition, informationAlignment
                );
            }
            if(samRecordToSort.hasMultipleSegments()){
                if(samRecordToSort.isNextSegmentUnmapped()){
                    GenomicPosition matePosition = GenomicPosition.getUnmapped();
                    if (!recordInformations.get(segmentMate).containsKey(matePosition)) {
                        recordInformations.get(segmentMate).put(
                                matePosition,
                                new InformationAlignment()
                        );
                    }
                }else {
                    GenomicPosition matePosition;
                    try{
                        matePosition = new GenomicPosition(
                                sequencesSource.getSequenceIdentifier(
                                        bamHeader.getReference(samRecordToSort.getNextRefID()).name
                                ),
                                samRecordToSort.getNextPositionStart()-1
                        );
                    }catch (IndexOutOfBoundsException e){
                        //In case where the file has a mistake, such as marking the aligned mate with reference *:
                        InformationAlignment inferredInformationAlignment = recordInformations
                                .get(segmentMate).values().iterator().next();
                        matePosition = new GenomicPosition(
                                new SequenceIdentifier(inferredInformationAlignment.sequenceId),
                                inferredInformationAlignment.position
                        );
                    }
                    if (!recordInformations.get(segmentMate).containsKey(matePosition)) {
                        InformationAlignment informationAlignment;
                        if(matePosition.isUnmapped()){
                            informationAlignment = new InformationAlignment();
                        }else{
                            informationAlignment = new InformationAlignment(
                                    matePosition.getSequenceId().getSequenceIdentifier(),
                                    matePosition.getPosition(),
                                    samRecordToSort.isNextSegmentReverseComplemented()
                            );
                        }
                        recordInformations.get(segmentMate).put(
                                matePosition,
                                informationAlignment
                        );
                    }
                }

            }

        }
    }

    public long getLastPositionRegistered() {
        return lastPositionRegistered;
    }

    private static GenomicPosition recordToGenomicPosition
            (BAMRecord bamRecord, AbstractSequencesSource sequencesSource, BAMHeader bamHeader)
    {
        return new GenomicPosition(
                sequencesSource.getSequenceIdentifier(
                        bamHeader.getReference(bamRecord.getRefID()).name
                ),
                bamRecord.getPositionStart()-1
        );
    }

    public BAMRecord getFirstRecord() {
        return records[0];
    }

    public BAMRecord[] getRecords() {
        return records;
    }
}

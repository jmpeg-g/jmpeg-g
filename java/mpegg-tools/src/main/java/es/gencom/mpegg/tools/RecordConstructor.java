package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMHeader;
import es.gencom.integration.bam.BAMRecord;
import es.gencom.integration.sam.CIGARDecoder;
import es.gencom.integration.sam.SAMRecord;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.ReverseCompType;
import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.encoder.Operation;
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
    private static SequenceIdentifier[] cachedSequenceIdentifiers;
    private static boolean[] isCachedSequenceIdentifiersIntegers;
    private static int[] cachedSequenceIdentifiersIntegers;
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
                //If the next segment is unmapped, we are in the Half Mapped case, and both segments must be stored
                // together
                toBeDoneInOneRecord = true;
            }else{
                if(bamRecord.getRefID() == bamRecord.getNextRefID()){
                    //If the next segment is not in the threshold window, it cannot be stored together
                    toBeDoneInOneRecord = Long.max(
                            bamRecord.getPositionStart(),
                            bamRecord.getNextPositionStart()
                    ) - Long.min(
                            bamRecord.getPositionStart(),
                            bamRecord.getNextPositionStart()
                    ) < threshold;
                }else{
                    toBeDoneInOneRecord = false;
                }
            }
        }else{
            //There is only one segment: can only be completed in one record.
            toBeDoneInOneRecord = true;
        }

        addSegment(bamRecord);
    }

    public boolean couldBeCompleted(){
        if(requiresTwoSegments){
            if(toBeDoneInOneRecord){
                //if the information is to be stored together, we need to have both records available
                return hasRecordFirst && hasRecordSecond;
            }else{
                //if the information is not to be stored together, as long as we now all positions, the record cam be
                // completed
                return hasDataFirst && hasDataSecond;
            }
        }else{
            //if there is only one segment in the record, it can be directly completed
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

    public Record construct(AbstractSequencesSource sequencesSource, BAMHeader bamHeader) throws IOException {
        ArrayList<Map<GenomicPosition, InformationAlignment>> sortedRecords = new ArrayList<>(2);
        sortedRecords.add(new TreeMap<>());
        sortedRecords.add(new TreeMap<>());

        sortRecords(
                sequencesSource, records, numberRecords, sortedRecords, bamHeader
        );

        //We find which mate has the most alignments
        int maxAlignments = 0;
        for(int segment_i=0; segment_i<2; segment_i++){
            maxAlignments = Integer.max(maxAlignments, sortedRecords.get(segment_i).size());
        }

        //We allocate the different arrays according to the number of alignments.
        // The fact that the alignment dimension comes first is to make it more similar to the specification.
        //The number of segments = 2 is to cover all cases.
        SequenceIdentifier[][] sequenceInformation = new SequenceIdentifier[maxAlignments][2];
        long[][][] positionInformation = new long[maxAlignments][2][1];
        SplitType[][] splitTypeInformation = new SplitType[maxAlignments][2];
        ReverseCompType[][][] reverseCompInformation = new ReverseCompType[maxAlignments][2][1];
        BAMRecord[][] bamRecordsInformation = new BAMRecord[maxAlignments][2];

        /*We populate the different fields by iterating over each alignment entry for each segment
         */
        for(int segment_i=0; segment_i<2; segment_i++){
            int alignment_i=0;
            for(
                    Map.Entry<GenomicPosition, InformationAlignment> alignmentEntry
                    : sortedRecords.get(segment_i).entrySet())
            {
                GenomicPosition genomicPosition = alignmentEntry.getKey();

                if(genomicPosition.isUnmapped()){
                    //Default values for unmapped
                    sequenceInformation[alignment_i][segment_i] = null;
                    positionInformation[alignment_i][segment_i][0] = 0;
                } else {
                    sequenceInformation[alignment_i][segment_i] = genomicPosition.getSequenceId();
                    positionInformation[alignment_i][segment_i][0] = genomicPosition.getPosition();
                }
                reverseCompInformation[alignment_i][segment_i][0] =
                        alignmentEntry.getValue().isReverseCompliment() ?
                                ReverseCompType.Reverse : ReverseCompType.Forward;
                if(alignmentEntry.getValue().hasRecord()){
                    if(alignmentEntry.getValue().isUnmapped()){
                        splitTypeInformation[alignment_i][segment_i] = SplitType.UnmappedSameRecord;
                    }else {
                        splitTypeInformation[alignment_i][segment_i] = SplitType.MappedSameRecord;
                    }
                    bamRecordsInformation[alignment_i][segment_i] = alignmentEntry.getValue().getBamRecord();
                }else {
                    if(!alignmentEntry.getValue().isUnmapped()) {
                        splitTypeInformation[alignment_i][segment_i] = SplitType.MappedDifferentRecordDifferentSequence;
                    } else {
                        throw new UnsupportedOperationException();
                    }
                    bamRecordsInformation[alignment_i][segment_i] = null;
                }
            }
        }

        boolean firstSegmentWasOriginallyUnmappedSameRecord = splitTypeInformation[0][0] == SplitType.UnmappedSameRecord;
        if(requiresTwoSegments) {
            /*MPEG-G requires of the first segment being stored in the record. The call to this method ensures that one
            of the proper segment is first.*/
            placeInRecordDataFirst(
                    sequenceInformation,
                    positionInformation,
                    splitTypeInformation,
                    reverseCompInformation,
                    bamRecordsInformation
            );
        }else{
            sequenceInformation[0][1] = null;
            positionInformation[0][1] = null;
            splitTypeInformation[0][1] = SplitType.Unpaired;
            reverseCompInformation[0][1] = null;
            bamRecordsInformation[0][1] = null;
        }

        boolean unpaired;
        if(!requiresTwoSegments){
            unpaired = true;
        }else {
            unpaired = splitTypeInformation[0][0] == SplitType.Unpaired || splitTypeInformation[0][1] == SplitType.Unpaired;
        }
        boolean twoSegmentsStoredTogether = false;
        if(!unpaired){
            twoSegmentsStoredTogether =
                    (splitTypeInformation[0][0]==SplitType.MappedSameRecord
                            || splitTypeInformation[0][0]==SplitType.UnmappedSameRecord)
                            && ( splitTypeInformation[0][1]==SplitType.MappedSameRecord
                            || splitTypeInformation[0][1]==SplitType.UnmappedSameRecord);
        }

        boolean isEncodingRead0 = true;
        if(!unpaired) {
            if (twoSegmentsStoredTogether) {
                if(splitTypeInformation[0][0] == SplitType.UnmappedSameRecord){
                    isEncodingRead0 = false;
                }else if(splitTypeInformation[0][1] != SplitType.UnmappedSameRecord){
                    isEncodingRead0 = positionInformation[0][0][0] <= positionInformation[0][1][0];
                }
            }else{
                isEncodingRead0 = splitTypeInformation[0][0]==SplitType.MappedSameRecord;
            }
        }else{
            isEncodingRead0 = sequenceInformation[0].length != 0;
        }

        byte[][] sequenceBytes = new byte[2][];
        short[][][] qualityValues = new short[2][2][];
        for(int segment_i=0; segment_i<(requiresTwoSegments ? 2:1); segment_i++) {
            if (
                    bamRecordsInformation[0][segment_i] != null
                            && bamRecordsInformation[0][segment_i] != null
            ) {
                SAMRecord samRecord = bamRecordsInformation[0][segment_i];
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


        long[][][] lengthSplices = new long[maxAlignments][2][];
        for(int alignment_i=0; alignment_i<maxAlignments; alignment_i++){
            for(int segment_i=0; segment_i<sequenceInformation[alignment_i].length; segment_i++) {
                lengthSplices[alignment_i][segment_i] = new long[1];
                if(
                        splitTypeInformation[alignment_i][segment_i]==SplitType.MappedSameRecord
                                || splitTypeInformation[alignment_i][segment_i]==SplitType.UnmappedSameRecord
                ){
                    lengthSplices[alignment_i][segment_i][0] = sequenceBytes[segment_i].length;
                }else {
                    lengthSplices[alignment_i][segment_i] = null;
                }
            }
        }

        byte[][][][] operationType = new byte[maxAlignments][2][][];
        int[][][][] operationLength = new int[maxAlignments][2][][];
        byte[][][][] originalBase = new byte[maxAlignments][2][][];

        for(int alignment_i = 0; alignment_i < maxAlignments; alignment_i++){
            for(int segment_i=0; segment_i<splitTypeInformation[alignment_i].length; segment_i++){
                if(splitTypeInformation[alignment_i][segment_i] == SplitType.MappedSameRecord){
                    DATA_CLASS classNextAlignment = populateCigar(
                            bamRecordsInformation[alignment_i][segment_i],
                            sequencesSource,
                            operationType[alignment_i],
                            operationLength[alignment_i],
                            originalBase[alignment_i],
                            segment_i,
                            bamHeader
                    );
                    if(alignment_i == 0) {
                        if (classNextAlignment.ID > dataClass.ID) {
                            dataClass = classNextAlignment;
                        }
                    }
                }else{
                    operationType[alignment_i][segment_i] = null;
                    operationLength[alignment_i][segment_i] = null;
                    originalBase[alignment_i][segment_i] = null;
                }
            }
        }

        long[][][] mapping_score = new long[maxAlignments][2][];
        for(int alignment_i=0; alignment_i<maxAlignments; alignment_i++) {
            for (int segment_i = 0; segment_i < sequenceInformation[alignment_i].length; segment_i++) {
                mapping_score[alignment_i][segment_i] = new long[1];

                if (splitTypeInformation[alignment_i][segment_i] == SplitType.MappedSameRecord) {
                    mapping_score[alignment_i][segment_i][0] =
                            bamRecordsInformation[alignment_i][segment_i].getMappingQuality();
                } else {
                    mapping_score[alignment_i][segment_i][0] = 0;
               }
            }
        }

        if(!isEncodingRead0) {
            for(int alignment_i=0; alignment_i < maxAlignments; alignment_i++) {
                swapFirstDimension(sequenceInformation[alignment_i]);
                swapFirstDimension(positionInformation[alignment_i]);
                swapFirstDimension(splitTypeInformation[alignment_i]);
                swapFirstDimension(reverseCompInformation[alignment_i]);
                swapFirstDimension(bamRecordsInformation[alignment_i]);
                swapFirstDimension(lengthSplices[alignment_i]);
                swapFirstDimension(operationType[alignment_i]);
                swapFirstDimension(operationLength[alignment_i]);
                swapFirstDimension(originalBase[alignment_i]);
                swapFirstDimension(mapping_score[alignment_i]);
            }
            swapFirstDimension(sequenceBytes);
            swapFirstDimension(qualityValues);
        }

        if(requiresTwoSegments && splitTypeInformation[0].length == 2) {
            if (splitTypeInformation[0][0] != SplitType.UnmappedSameRecord &&
                    splitTypeInformation[0][1] == SplitType.UnmappedSameRecord) {
                dataClass = DATA_CLASS.CLASS_HM;
            }
        }

        int[][] alignPtr = null;
        if(!unpaired) {
            int maxAlignPtrLength = maxAlignments * maxAlignments;
            alignPtr = new int[maxAlignPtrLength][2];

            int entry_i=0;
            for(int alignment0_i = 0; alignment0_i<maxAlignments; alignment0_i++) {
                if (splitTypeInformation[alignment0_i][0] == SplitType.MappedSameRecord) {
                    int sequenceMate = fromBAMSequenceIdentifierToMPEGGSequenceIdentifierInteger(
                            bamRecordsInformation[alignment0_i][0].getNextRefID(),
                            bamHeader,
                            sequencesSource
                    );
                    long positionMate = bamRecordsInformation[alignment0_i][0].getNextPositionStart()-1;

                    int alignment1 = -1;
                    if(
                            splitTypeInformation[0][1] == SplitType.UnmappedSameRecord ||
                                    splitTypeInformation[0][1] == SplitType.UnmappedDifferentRecordDifferentAU ||
                                    splitTypeInformation[0][1] == SplitType.UnmappedDifferentRecordSameAU){
                        alignment1 = 0;
                    }else {
                        for (int alignment_j = 0; alignment_j < maxAlignments; alignment_j++) {
                            if (
                                    sequenceInformation[alignment_j][1].equals(new SequenceIdentifier(sequenceMate))
                                            && positionInformation[alignment_j][1][0] == positionMate
                            ) {
                                alignment1 = alignment_j;
                                break;
                            }
                        }
                    }
                    if(alignment1 == -1){
                        throw new InternalError();
                    }
                    alignPtr[entry_i][0] = alignment0_i;
                    alignPtr[entry_i][1] = alignment1;
                }else if(splitTypeInformation[alignment0_i][1] == SplitType.UnmappedSameRecord){
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

        byte numberTemplateSegments = 2;
        byte flags = 0;
        return new Record(
                numberTemplateSegments,
                dataClass,
                readName,
                groupName,
                dataClass != DATA_CLASS.CLASS_HM ? isEncodingRead0 : !firstSegmentWasOriginallyUnmappedSameRecord,
                qualityValues,
                sequenceBytes,
                positionInformation,
                sequenceInformation,
                new long[numberRecords],
                new long[numberRecords],
                splitTypeInformation,
                lengthSplices,
                operationType,
                operationLength,
                originalBase,
                reverseCompInformation,
                mapping_score,
                alignPtr,
                new int[numberRecords],
                flags,
                false,
                null,
                0
        );
    }

    private void removeDataNotInRecordForEncodingSegment(
            SequenceIdentifier[][] sequenceInformation,
            long[][][] positionInformation,
            SplitType[][] splitTypeInformation,
            ReverseCompType[][][] reverseCompInformation,
            BAMRecord[][] bamRecordsInformation,
            long[][][] lengthSplices,
            byte[][][][] operationType,
            int[][][][] operationLength,
            byte[][][][] originalBase
    ) {
        int countSegmentsInRecord = 0;
        for(int alignment_i=0; alignment_i < splitTypeInformation[0].length; alignment_i++){
            if(splitTypeInformation[0][alignment_i] == SplitType.MappedSameRecord || splitTypeInformation[0][alignment_i] == SplitType.UnmappedSameRecord){
                countSegmentsInRecord++;
            }
        }

        ReverseCompType[][] reverseCompInformationBuffer = new ReverseCompType[countSegmentsInRecord][];
        BAMRecord[] bamRecordsInformationBuffer = new BAMRecord[countSegmentsInRecord];
        long[][] lengthSplicesBuffer = new long[countSegmentsInRecord][];
        byte[][][] operationTypeBuffer = new byte[countSegmentsInRecord][][];
        int[][][] operationLengthBuffer = new int[countSegmentsInRecord][][];
        byte[][][] originalBaseBuffer = new byte[countSegmentsInRecord][][];

        int entry_i=0;
        for(int alignment_i=0; alignment_i < splitTypeInformation[0].length; alignment_i++){
            if(splitTypeInformation[0][alignment_i] == SplitType.MappedSameRecord || splitTypeInformation[0][alignment_i] == SplitType.UnmappedSameRecord){
                reverseCompInformationBuffer[entry_i] = reverseCompInformation[0][alignment_i];
                bamRecordsInformationBuffer[entry_i] = bamRecordsInformation[0][alignment_i];
                lengthSplicesBuffer[entry_i] = lengthSplices[0][alignment_i];
                operationTypeBuffer[entry_i] = operationType[0][alignment_i];
                operationLengthBuffer[entry_i] = operationLength[0][alignment_i];
                originalBaseBuffer[entry_i] = originalBase[0][alignment_i];
                entry_i++;
            }
        }

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

    /**
     * Swapes the information of the first segment which is mapped in this record, with the first information provided
     * for the first segment
     * @param sequenceInformation sequence identifiers (first dimension is alignment, second is segment index)
     * @param positionInformation position information (first dimension is alignment, second is segment index)
     * @param splitTypeInformation information of where the information is stored (first dimension is alignment,
     *                             second is segment index)
     * @param reverseCompInformation aligned to forward or reverse strand (first dimension is alignment, second is
     *                               segment index)
     * @param samRecordsInformation the actual record if provided (first dimension is alignment, second is segment index)
     */
    private static void placeInRecordDataFirst(
            SequenceIdentifier[][] sequenceInformation,
            long[][][] positionInformation,
            SplitType[][] splitTypeInformation,
            ReverseCompType[][][] reverseCompInformation,
            SAMRecord[][] samRecordsInformation) {
        for(int segment_i=0; segment_i<2; segment_i++){
            if(splitTypeInformation[0][segment_i] == SplitType.MappedSameRecord){
                //if it is the first segment stored in the record, swap all its information (for all alignments) with
                // the information of segment_i==0
                for(int alignment_i=0; alignment_i < sequenceInformation.length; alignment_i++) {
                    SequenceIdentifier sequenceInformationBuffer = sequenceInformation[alignment_i][segment_i];
                    long[] positionInformationBuffer = positionInformation[alignment_i][segment_i];
                    SplitType splitTypeInformationBuffer = splitTypeInformation[alignment_i][segment_i];
                    ReverseCompType[] reverseCompInformationBuffer = reverseCompInformation[alignment_i][segment_i];
                    SAMRecord samRecordsInformationBuffer = samRecordsInformation[alignment_i][segment_i];

                    sequenceInformation[alignment_i][segment_i] = sequenceInformation[alignment_i][0];
                    positionInformation[alignment_i][segment_i] = positionInformation[alignment_i][0];
                    splitTypeInformation[alignment_i][segment_i] = splitTypeInformation[alignment_i][0];
                    reverseCompInformation[alignment_i][segment_i] = reverseCompInformation[alignment_i][0];
                    samRecordsInformation[alignment_i][segment_i] = samRecordsInformation[alignment_i][0];

                    sequenceInformation[alignment_i][0] = sequenceInformationBuffer;
                    positionInformation[alignment_i][0] = positionInformationBuffer;
                    splitTypeInformation[alignment_i][0] = splitTypeInformationBuffer;
                    reverseCompInformation[alignment_i][0] = reverseCompInformationBuffer;
                    samRecordsInformation[alignment_i][0] = samRecordsInformationBuffer;

                }
                break;
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
            int segment_i,
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

        operations[segment_i] = new byte[1][128];
        operationLength[segment_i] = new int[1][128];
        originalBases[segment_i] = new byte[1][128];

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
                        operations[segment_i][0][numberOperations] = Operation.Match;
                        operationLength[segment_i][0][numberOperations] = 1;
                        numberOperations++;

                        if(operations[segment_i][0].length == numberOperations){
                            operations[segment_i][0] = Arrays.copyOf(
                                    operations[segment_i][0], numberOperations*2
                            );
                            operationLength[segment_i][0] = Arrays.copyOf(
                                    operationLength[segment_i][0], numberOperations*2
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
                        operations[segment_i][0][numberOperations] = operationType;
                        operationLength[segment_i][0][numberOperations] = 1;
                        numberOperations++;

                        if(operations[segment_i][0].length == numberOperations){
                            operations[segment_i][0] = Arrays.copyOf(
                                    operations[segment_i][0], numberOperations*2
                            );
                            operationLength[segment_i][0] = Arrays.copyOf(
                                    operationLength[segment_i][0], numberOperations*2
                            );
                        }

                        originalBases[segment_i][0][numberOriginalBases] = originalBase;
                        numberOriginalBases++;

                        if(originalBases[segment_i][0].length == numberOriginalBases){
                            originalBases[segment_i][0] = Arrays.copyOf(
                                    originalBases[segment_i][0], numberOriginalBases*2
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
                        operations[segment_i][0][numberOperations] = Operation.Insert;
                        operationLength[segment_i][0][numberOperations] = 1;
                        numberOperations++;

                        if(operations[segment_i][0].length == numberOperations){
                            operations[segment_i][0] = Arrays.copyOf(
                                    operations[segment_i][0], numberOperations*2
                            );
                            operationLength[segment_i][0] = Arrays.copyOf(
                                    operationLength[segment_i][0], numberOperations*2
                            );
                        }

                        currentPositionInReadString++;
                    }
                }else{
                    //D
                    for(int i=0; i<cigar.getOperationLength()[operation_i]; i++){
                        operations[segment_i][0][numberOperations] = Operation.Delete;
                        operationLength[segment_i][0][numberOperations] = 1;
                        numberOperations++;

                        if(operations[segment_i][0].length == numberOperations){
                            operations[segment_i][0] = Arrays.copyOf(
                                    operations[segment_i][0], numberOperations*2
                            );
                            operationLength[segment_i][0] = Arrays.copyOf(
                                    operationLength[segment_i][0], numberOperations*2
                            );
                        }

                        originalBases[segment_i][0][numberOriginalBases] =
                                referenceSubSequence.readByte();
                        numberOriginalBases++;

                        if(originalBases[segment_i][0].length == numberOriginalBases){
                            originalBases[segment_i][0] = Arrays.copyOf(
                                    originalBases[segment_i][0], numberOriginalBases*2
                            );
                        }

                        currentPositionInCopy++;
                    }
                }
                currentIndex++;
            } else if (cigar.getOperation()[operation_i]=='S'){
                dataClass = DATA_CLASS.CLASS_I;

                operations[segment_i][0][numberOperations] = Operation.SoftClip;
                operationLength[segment_i][0][numberOperations] = cigar.getOperationLength()[operation_i];
                numberOperations++;

                if(operations[segment_i][0].length == numberOperations){
                    operations[segment_i][0] = Arrays.copyOf(
                            operations[segment_i][0], numberOperations*2
                    );
                    operationLength[segment_i][0] = Arrays.copyOf(
                            operationLength[segment_i][0], numberOperations*2
                    );
                }

                currentPositionInReadString += cigar.getOperationLength()[operation_i];
                currentIndex += cigar.getOperationLength()[operation_i];
            }
        }
        operations[segment_i][0] = Arrays.copyOf(operations[segment_i][0], numberOperations);
        operationLength[segment_i][0] = Arrays.copyOf(operationLength[segment_i][0], numberOperations);
        originalBases[segment_i][0] = Arrays.copyOf(originalBases[segment_i][0], numberOriginalBases);
        return dataClass;
    }

    /**
     * Takes an array of records, and populates the record informations: alignments information for each position
     * @param sequencesSource sources of reference nucleotides (here used to pass from sequence name to sequence id)
     * @param records array of records
     * @param numberRecords number of records actually stored in the array of records.
     * @param recordInformations the structure to populate
     * @param bamHeader the bam header related to the provided records
     */
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
                                new SequenceIdentifier(inferredInformationAlignment.getSequenceId()),
                                inferredInformationAlignment.getPosition()
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
                fromBAMSequenceIdentifierToMPEGGSequenceIdentifier(bamRecord.getRefID(), bamHeader, sequencesSource),
                bamRecord.getPositionStart()-1
        );
    }

    private static int fromBAMSequenceIdentifierToMPEGGSequenceIdentifierInteger(
            int bamSequenceId,
            BAMHeader bamHeader,
            AbstractSequencesSource sequencesSource
    ){
        if (isCachedSequenceIdentifiersIntegers == null){
            isCachedSequenceIdentifiersIntegers = new boolean[bamHeader.getReferences().size()];
            cachedSequenceIdentifiersIntegers = new int[bamHeader.getReferences().size()];
        }

        if(isCachedSequenceIdentifiersIntegers[bamSequenceId]){
            return cachedSequenceIdentifiersIntegers[bamSequenceId];
        }

        isCachedSequenceIdentifiersIntegers[bamSequenceId] = true;
        cachedSequenceIdentifiersIntegers[bamSequenceId] = fromBAMSequenceIdentifierToMPEGGSequenceIdentifier(
                bamSequenceId, bamHeader, sequencesSource
        ).getSequenceIdentifier();

        return cachedSequenceIdentifiersIntegers[bamSequenceId];
    }

    private static SequenceIdentifier fromBAMSequenceIdentifierToMPEGGSequenceIdentifier(
            int bamSequenceId,
            BAMHeader bamHeader,
            AbstractSequencesSource sequencesSource
    ){
        if (cachedSequenceIdentifiers == null){
            cachedSequenceIdentifiers = new SequenceIdentifier[bamHeader.getReferences().size()];
        } else {
            if(cachedSequenceIdentifiers[bamSequenceId] != null){
                return cachedSequenceIdentifiers[bamSequenceId];
            }
        }

        cachedSequenceIdentifiers[bamSequenceId] = sequencesSource.getSequenceIdentifier(
                bamHeader.getReference(bamSequenceId).name
        );
        return cachedSequenceIdentifiers[bamSequenceId];
    }

    public BAMRecord getFirstRecord() {
        return records[0];
    }

    public BAMRecord[] getRecords() {
        return records;
    }

    @Override
    public String toString() {
        return "RecordConstructor{" +
                "readId=" + readId +
                ", readName='" + readName + '\'' +
                ", groupName='" + groupName + '\'' +
                ", records=" + Arrays.toString(records) +
                '}';
    }
}

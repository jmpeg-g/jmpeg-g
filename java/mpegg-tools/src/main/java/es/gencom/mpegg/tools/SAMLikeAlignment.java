package es.gencom.mpegg.tools;

import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.Operation;
import es.gencom.mpegg.format.SequenceIdentifier;

import java.util.Arrays;

public class SAMLikeAlignment implements Comparable<SAMLikeAlignment>{
    static long instancesCount = 0;

    final private String readName;
    final private long instanceId;
    final private SequenceIdentifier sequenceId;
    final private long position;
    final private byte[] sequence;
    final private String cigarString;
    final private String MDTag;
    final private boolean isOnReverse;
    final private boolean noMate;
    final private SequenceIdentifier mateSequenceId;
    final private long matePosition;
    final private boolean firstMate;
    final private boolean lastMate;
    final private boolean mateUnmapped;
    final private boolean mateOnReverse;
    final private boolean paired;
    private short[] qualities;

    public SAMLikeAlignment(
            String readName,
            SequenceIdentifier sequenceId,
            long position,
            byte[] sequence,
            short[] qualities,
            String cigarString,
            String mdTag,
            boolean isOnReverse,
            boolean noMate,
            SequenceIdentifier mateSequenceId,
            long matePosition,
            boolean firstMate,
            boolean lastMate,
            boolean unpaired,
            boolean mateUnmapped,
            boolean mateOnReverse
    ) {
        this.instanceId = instancesCount;
        instancesCount++;

        this.readName = readName;
        this.sequenceId = sequenceId;
        this.position = position;
        this.sequence = sequence;
        this.qualities = qualities;
        this.cigarString = cigarString;
        this.MDTag = mdTag;
        this.isOnReverse = isOnReverse;
        this.noMate = noMate;
        this.mateSequenceId = mateSequenceId;
        this.matePosition = matePosition;
        this.firstMate = firstMate;
        this.lastMate = lastMate;
        this.paired = !unpaired;
        this.mateUnmapped = mateUnmapped;
        this.mateOnReverse = mateOnReverse;
    }

    static void mergeOperations(
            Operation[][] operations,
            int[][] operationLengths,
            Operation[][] operationsMerged,
            int[][] operationLengthsMerged,
            int splice_i

    ){
        Operation[] operationsInSplice = operations[splice_i];
        int[] operationLengthInSplice = operationLengths[splice_i];

        Operation[] operationsInSpliceMerged = new Operation[operations[splice_i].length];
        int[] operationLengthInSpliceMerged = new int[operationLengths[splice_i].length];

        Operation currentOperation = operationsInSplice[0];
        int currentLength = operationLengthInSplice[0];
        int numberMergedOperations = 0;
        for(int operation_i=1; operation_i < operationLengthInSplice.length; operation_i++){
            Operation newOperation  = operationsInSplice[operation_i];
            if(newOperation == currentOperation){
                currentLength += operationLengthInSplice[operation_i];
            }else{
                operationsInSpliceMerged[numberMergedOperations] = currentOperation;
                operationLengthInSpliceMerged[numberMergedOperations] = currentLength;
                numberMergedOperations++;
                currentOperation = newOperation;
                currentLength = operationLengthInSplice[operation_i];
            }
        }
        operationsInSpliceMerged[numberMergedOperations] = currentOperation;
        operationLengthInSpliceMerged[numberMergedOperations] = currentLength;
        numberMergedOperations++;
        operationsInSpliceMerged = Arrays.copyOf(operationsInSpliceMerged, numberMergedOperations);
        operationLengthInSpliceMerged = Arrays.copyOf(operationLengthInSpliceMerged, numberMergedOperations);

        operationsMerged[splice_i] = operationsInSpliceMerged;
        operationLengthsMerged[splice_i] = operationLengthInSpliceMerged;
    }

    static String getCigarString(Operation[][] operationsMerged, int[][] operationLengthsMerged) {
        if(operationLengthsMerged.length == 0){
            throw new InternalError();
        } else if(operationLengthsMerged.length > 1){
            throw new UnsupportedOperationException();
        }

        StringBuilder stringBuilder = new StringBuilder();
        int currentLength = 0;
        for(int operation_i = 0; operation_i < operationsMerged[0].length; operation_i++){
            switch (operationsMerged[0][operation_i]){
                case SubstitutionToN:
                case Substitution:
                case Match:
                    currentLength += operationLengthsMerged[0][operation_i];
                    break;
                case Delete:
                    if(currentLength != 0){
                        stringBuilder.append(currentLength).append('M');
                    }
                    currentLength = 0;
                    stringBuilder.append(operationLengthsMerged[0][operation_i]).append('D');
                    break;
                case Insert:
                    if(currentLength != 0){
                        stringBuilder.append(currentLength).append('M');
                    }
                    currentLength = 0;
                    stringBuilder.append(operationLengthsMerged[0][operation_i]).append('I');
                    break;
                case SoftClip:
                    if(currentLength != 0){
                        stringBuilder.append(currentLength).append('M');
                    }
                    currentLength = 0;
                    stringBuilder.append(operationLengthsMerged[0][operation_i]).append('S');
                    break;
                case HardClip:
                    if(currentLength != 0){
                        stringBuilder.append(currentLength).append('M');
                    }
                    currentLength = 0;
                    stringBuilder.append(operationLengthsMerged[0][operation_i]).append('H');
                    break;
            }
        }
        if(currentLength != 0){
            stringBuilder.append(currentLength).append('M');
        }
        return stringBuilder.toString();
    }

    public static String getMDTag(Operation[][] operations, int[][] operationsLength, byte[][] originalNucleotides) {
        if(operations.length == 0){
            throw new InternalError();
        } else if(operations.length > 1){
            throw new UnsupportedOperationException();
        }
        StringBuilder stringBuilder = new StringBuilder();
        int originalNucleotide_i = 0;

        int currentLength = 0;
        for(int operation_i = 0; operation_i < operations[0].length; operation_i++){
            switch (operations[0][operation_i]){
                case SoftClip:
                case HardClip:
                case Insert:
                    break;
                case Delete:
                    stringBuilder.append(currentLength);
                    stringBuilder.append('^');
                    for(int deleted_base_i=0; deleted_base_i < operationsLength[0][operation_i]; deleted_base_i++){
                        stringBuilder.append((char)originalNucleotides[0][originalNucleotide_i]);
                        originalNucleotide_i++;
                    }
                    currentLength = 0;
                    break;
                case Match:
                    currentLength += operationsLength[0][operation_i];
                    break;
                case Substitution:
                case SubstitutionToN:
                    for(
                            int subsituted_base_i = 0;
                            subsituted_base_i < operationsLength[0][operation_i];
                            subsituted_base_i++
                    ) {
                        stringBuilder.append(currentLength);
                        stringBuilder.append((char) originalNucleotides[0][originalNucleotide_i]);
                        currentLength = 0;
                        originalNucleotide_i++;
                    }
                    break;
            }
        }
        stringBuilder.append(currentLength);
        return stringBuilder.toString();
    }

    public SequenceIdentifier getSequenceId() {
        return sequenceId;
    }

    public long getPosition() {
        return position;
    }

    public String getSequence() {
        return new String(sequence);
    }

    @Override
    public int compareTo(SAMLikeAlignment samLikeAlignment) {
        if (sequenceId.getSequenceIdentifier() != samLikeAlignment.getSequenceId().getSequenceIdentifier()) {
            return Integer.compare(sequenceId.getSequenceIdentifier(), samLikeAlignment.getSequenceId().getSequenceIdentifier());
        }
        if(position != samLikeAlignment.getPosition()) {
            return Long.compare(position, samLikeAlignment.getPosition());
        }
        return Long.compare(instanceId, samLikeAlignment.instanceId);
    }

    public String getReadName() {
        return readName;
    }


    public String toString() {
        return
                sequenceId
                        +"\t"+(position+1)
                        +"\t"+sequence
                        +"\t"+cigarString
                        +"\tReadId: "+readName
                        +"\tReverse: "+ (isOnReverse?"true":"false")
                        +"\t"+mateSequenceId
                        +"\t"+(matePosition+1)
                        +"\t"+(firstMate?"T":"F")
                        +"\t"+(lastMate?"T":"F")
                        +"\t"+(mateUnmapped?"T":"F")
                        +"\t"+MDTag;
    }

    public String getCigarString(){
        return cigarString;
    }

    public SequenceIdentifier getMateSequenceId() {
        return mateSequenceId;
    }

    public long getMatePosition() {
        return matePosition;
    }

    public boolean isMateUnmapped() {
        return mateUnmapped;
    }

    public boolean isMateOnReverse() {
        return mateOnReverse;
    }

    public boolean isFirstMate() {
        return firstMate;
    }

    public boolean isLastMate() {
        return lastMate;
    }

    public boolean isOnReverse() {
        return isOnReverse;
    }

    public String getMDTag() {
        return MDTag;
    }

    public boolean isPaired() {
        return paired;
    }

    public boolean hasQualities(){
        return qualities != null;
    }

    public short[] getQualities() {
        return qualities;
    }
}

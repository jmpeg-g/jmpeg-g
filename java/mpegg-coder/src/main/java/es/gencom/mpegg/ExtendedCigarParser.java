package es.gencom.mpegg;

import es.gencom.mpegg.encoder.Operation;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtendedCigarParser {
    final static Pattern cigarStart = Pattern.compile("(?>\\[(\\d+)])?(?>\\((\\d+)\\))?");
    final static Pattern cigarEnd = Pattern.compile("(?>\\((\\d+)\\))?(?>\\[(\\d+)])?");
    final static Pattern cigarMiddle = Pattern.compile("(?>(\\d+)=|(\\d+)\\+|(\\d+)-|([a-zA-Z])|(\\d+)[*/%])");

    private final byte[][] operationType;
    private final int[][] lengthOperation;
    private final long[] spliceLength;
    private final long[] spliceOffset;


    public ExtendedCigarParser(String extendedCigar){
        byte[][] tmpOperationType = new byte[2][126];
        int[] numOperations = new int[2];
        int[][] tmpLengthOperation = new int[2][126];
        int currentSplice = 0;
        long[] tmpSpliceLength = new long[2];
        long[] tmpSpliceOffset = new long[2];
        int tmpSpliceOffsetCalc = 0;


        Matcher cigarStartMatcher = cigarStart.matcher(extendedCigar);
        Matcher cigarEndMatcher = cigarEnd.matcher(extendedCigar);
        Matcher cigarMiddleMatcher = cigarMiddle.matcher(extendedCigar);

        if(!cigarStartMatcher.find(0)){
            throw new IllegalArgumentException();
        }
        if(cigarStartMatcher.group(1) != null){
            tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.HardClip;
            tmpLengthOperation[currentSplice][numOperations[currentSplice]] = Integer.parseInt(
                    cigarStartMatcher.group(1)
            );
            numOperations[currentSplice]++;
        }
        if(cigarStartMatcher.group(2) != null){
            tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.SoftClip;
            tmpLengthOperation[currentSplice][numOperations[currentSplice]] = Integer.parseInt(
                    cigarStartMatcher.group(2)
            );
            numOperations[currentSplice]++;
            tmpSpliceLength[currentSplice] += tmpLengthOperation[currentSplice][numOperations[currentSplice]];
        }

        int start = cigarStartMatcher.end();

        while (cigarMiddleMatcher.find(start)){
            if(cigarMiddleMatcher.group(1) != null){
                tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.Match;
                tmpLengthOperation[currentSplice][numOperations[currentSplice]] = Integer.parseInt(
                        cigarMiddleMatcher.group(1)
                );
                numOperations[currentSplice]++;
                tmpSpliceLength[currentSplice] += tmpLengthOperation[currentSplice][numOperations[currentSplice]];
                tmpSpliceOffsetCalc += tmpLengthOperation[currentSplice][numOperations[currentSplice]];
            } else if(cigarMiddleMatcher.group(2) != null){
                tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.Insert;
                tmpLengthOperation[currentSplice][numOperations[currentSplice]] = Integer.parseInt(
                        cigarMiddleMatcher.group(2)
                );
                numOperations[currentSplice]++;
                tmpSpliceLength[currentSplice] += tmpLengthOperation[currentSplice][numOperations[currentSplice]];
            } else if(cigarMiddleMatcher.group(3) != null){
                tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.Delete;
                tmpLengthOperation[currentSplice][numOperations[currentSplice]] = Integer.parseInt(
                        cigarMiddleMatcher.group(3)
                );
                numOperations[currentSplice]++;
                tmpSpliceOffsetCalc += tmpLengthOperation[currentSplice][numOperations[currentSplice]];
            } else if(cigarMiddleMatcher.group(4) != null){
                if(cigarMiddleMatcher.group(4).charAt(0)=='N' || cigarMiddleMatcher.group(4).charAt(0)=='n') {
                    tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.SubstitutionToN;
                } else {
                    tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.Substitution;
                }
                tmpLengthOperation[currentSplice][numOperations[currentSplice]] = 1;
                numOperations[currentSplice]++;
                tmpSpliceLength[currentSplice] += tmpLengthOperation[currentSplice][numOperations[currentSplice]];
                tmpSpliceOffsetCalc += tmpLengthOperation[currentSplice][numOperations[currentSplice]];
            } else if(cigarMiddleMatcher.group(5) != null){
                currentSplice++;
                tmpSpliceOffset[currentSplice] = tmpSpliceOffsetCalc + Long.parseLong(cigarMiddleMatcher.group(5));
                tmpSpliceOffsetCalc = 0;
                if(tmpOperationType.length == currentSplice+1){
                    int oldSize = tmpOperationType.length;
                    tmpOperationType = Arrays.copyOf(tmpOperationType, oldSize*2);
                    tmpLengthOperation = Arrays.copyOf(tmpLengthOperation, oldSize*2);
                    tmpSpliceLength = Arrays.copyOf(tmpSpliceLength, oldSize*2);

                    numOperations = Arrays.copyOf(numOperations, tmpOperationType.length*2);

                    for(int i=oldSize; i < tmpOperationType.length; i++){
                        tmpOperationType[i] = new byte[126];
                        tmpLengthOperation[i] = new int[126];
                    }
                }
            }
            if(numOperations[currentSplice] == tmpOperationType[currentSplice].length){
                tmpOperationType[currentSplice] = Arrays.copyOf(
                        tmpOperationType[currentSplice],
                        tmpOperationType.length*2
                );
                tmpLengthOperation[currentSplice] = Arrays.copyOf(
                        tmpLengthOperation[currentSplice],
                        tmpLengthOperation.length*2
                );
            }
            start = cigarMiddleMatcher.end();
        }

        if(!cigarEndMatcher.find(start)){
            throw new IllegalArgumentException();
        }
        if(cigarEndMatcher.group(1) != null){
            tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.SoftClip;
            tmpLengthOperation[currentSplice][numOperations[currentSplice]] = Integer.parseInt(
                    cigarEndMatcher.group(1)
            );
            numOperations[currentSplice]++;
        }
        if(cigarEndMatcher.group(2) != null){
            tmpOperationType[currentSplice][numOperations[currentSplice]] = Operation.HardClip;
            tmpLengthOperation[currentSplice][numOperations[currentSplice]] = Integer.parseInt(
                    cigarEndMatcher.group(2)
            );
            numOperations[currentSplice]++;
        }

        if(cigarEndMatcher.end() != extendedCigar.length()){
            throw new IllegalArgumentException();
        }

        int numSplice = currentSplice+1;
        operationType = new byte[numSplice][];
        lengthOperation = new int[numSplice][];
        for(int splice_i=0; splice_i < numSplice; splice_i++){
            operationType[splice_i] = Arrays.copyOf(tmpOperationType[splice_i], numOperations[splice_i]);
            lengthOperation[splice_i] = Arrays.copyOf(tmpLengthOperation[splice_i], numOperations[splice_i]);
        }
        spliceLength = Arrays.copyOf(tmpSpliceLength, numSplice);
        spliceOffset = Arrays.copyOf(tmpSpliceOffset, numSplice);
    }


    public byte[][] getOperationType() {
        return operationType;
    }

    public int[][] getLengthOperation() {
        return lengthOperation;
    }

    public long[] getSpliceLength() {
        return spliceLength;
    }

    public long[] getSpliceOffset() {
        return spliceOffset;
    }
}

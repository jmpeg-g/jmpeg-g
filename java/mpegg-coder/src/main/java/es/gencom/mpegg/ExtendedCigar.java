package es.gencom.mpegg;

import es.gencom.mpegg.encoder.Operation;

public class ExtendedCigar {
    static String getExtendedCigarString(
            byte[][] operations,
            int[][] operationLengths,
            long[] positionPerSplice,
            byte[] sequence) {
        if(operationLengths.length == 0){
            throw new InternalError();
        } else if(operationLengths.length > 1){
            throw new UnsupportedOperationException();
        }

        int positionOnSequence = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for(int splice_i=0; splice_i < operations.length; splice_i++) {
            if(splice_i != 0){
                stringBuilder.append(positionPerSplice[splice_i]-positionPerSplice[splice_i-1]).append('*');
            }
            int currentLength = 0;
            for (int operation_i = 0; operation_i < operations[splice_i].length; operation_i++) {
                switch (operations[splice_i][operation_i]) {
                    case Operation.Match:
                        currentLength += operationLengths[splice_i][operation_i];
                        positionOnSequence += operationLengths[splice_i][operation_i];
                        break;
                    case Operation.Delete:
                        if (currentLength != 0) {
                            stringBuilder.append(currentLength).append('=');
                        }
                        currentLength = 0;
                        stringBuilder.append(operationLengths[splice_i][operation_i]).append('-');
                        break;
                    case Operation.Insert:
                        if (currentLength != 0) {
                            stringBuilder.append(currentLength).append('=');
                        }
                        currentLength = 0;
                        stringBuilder.append(operationLengths[splice_i][operation_i]).append('+');
                        positionOnSequence += operationLengths[splice_i][operation_i];
                        break;
                    case Operation.SoftClip:
                        if (currentLength != 0) {
                            stringBuilder.append(currentLength).append('=');
                        }
                        currentLength = 0;
                        stringBuilder.append('(').append(operationLengths[splice_i][operation_i]).append(')');
                        positionOnSequence += operationLengths[splice_i][operation_i];
                        break;
                    case Operation.HardClip:
                        if (currentLength != 0) {
                            stringBuilder.append(currentLength).append('=');
                        }
                        currentLength = 0;
                        stringBuilder.append('[').append(operationLengths[splice_i][operation_i]).append(']');
                        break;
                    case Operation.Substitution:
                    case Operation.SubstitutionToN:
                        stringBuilder.append(sequence[positionOnSequence]);
                }
            }
            if (currentLength != 0) {
                stringBuilder.append(currentLength).append('=');
            }
        }
        return stringBuilder.toString();
    }
}

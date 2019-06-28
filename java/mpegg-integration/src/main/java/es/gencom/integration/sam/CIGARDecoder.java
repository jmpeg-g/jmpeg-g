package es.gencom.integration.sam;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CIGARDecoder {
    private final static String CIGAR = "MIDNSHP=X";
    private final static Pattern PATTERN = Pattern.compile("([0-9]+)([MIDNSHP=X]{1})");

    private final byte[] operation;
    private final int[] operationLength;

    public CIGARDecoder(String cigar) {
        byte[] operationTmp = new byte[1];
        int[] operationLengthTmp = new int[1];
        final Matcher m = PATTERN.matcher(cigar);

        while (m.find()) {
            final String ln = m.group(1);
            final String ch = m.group(2);
            if (ln != null && ch != null) {
                operationTmp = Arrays.copyOf(operationTmp, operationTmp.length + 1);
                operationLengthTmp = Arrays.copyOf(operationLengthTmp, operationLengthTmp.length + 1);


                operationTmp[operationTmp.length - 1] = ch.getBytes()[0];
                operationLengthTmp[operationTmp.length - 1] = Integer.parseInt(ln);
            }
        }
        operation = operationTmp;
        operationLength = operationLengthTmp;
    }

    public final byte[] getOperation() {
        return operation;
    }

    public final int[] getOperationLength() {
        return operationLength;
    }
}
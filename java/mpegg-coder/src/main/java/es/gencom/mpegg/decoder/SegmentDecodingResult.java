package es.gencom.mpegg.decoder;

public class SegmentDecodingResult {
    public final byte[][] sequence;
    public final byte[][] operations;
    public final int[][] operationLength;
    public final byte[][] original_nucleotides;

    public SegmentDecodingResult(
            byte[][] sequence,
            byte[][] operations,
            int[][] operationLength,
            byte[][] original_nucleotides) {
        this.sequence = sequence;
        this.operations = operations;
        this.operationLength = operationLength;
        this.original_nucleotides = original_nucleotides;
    }
}

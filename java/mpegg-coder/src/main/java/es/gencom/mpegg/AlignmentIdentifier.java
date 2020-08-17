package es.gencom.mpegg;

import java.util.Objects;

public class AlignmentIdentifier {
    private final int sequence;
    private final long position;
    private final ReverseCompType reverseCompType;
    private final String ecigar;
    private final SplitType splitType;

    public AlignmentIdentifier(int sequence, long position, ReverseCompType reverseCompType, String ecigar, SplitType splitType) {
        this.sequence = sequence;
        this.position = position;
        this.reverseCompType = reverseCompType;
        this.ecigar = ecigar;
        this.splitType = splitType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlignmentIdentifier)) return false;
        AlignmentIdentifier that = (AlignmentIdentifier) o;
        return sequence == that.sequence &&
                position == that.position &&
                reverseCompType == that.reverseCompType &&
                Objects.equals(ecigar, that.ecigar) &&
                splitType == that.splitType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, position, reverseCompType, ecigar, splitType);
    }
}

package es.gencom.integration.sam.header;

public enum AlignmentsGrouping {
    NONE,
    QUERY,
    REFERENCE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

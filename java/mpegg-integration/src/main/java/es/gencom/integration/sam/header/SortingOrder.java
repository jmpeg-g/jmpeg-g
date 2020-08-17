package es.gencom.integration.sam.header;

public enum SortingOrder {
    UNKNOWN,
    UNSORTED,
    QUERYNAME,
    COORDINATE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

package es.gencom.integration.sam.header;

public class ReferenceLineAlternativeLocus {
    private final String chromosome;
    private final long start;
    private final long end;

    public ReferenceLineAlternativeLocus(String chromosome, long start, long end) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
    }

    public ReferenceLineAlternativeLocus(String substring) {
        String[] firstSplit = substring.split(":");
        if(firstSplit.length != 2){
            throw new IllegalArgumentException();
        }
        String[] secondSplit = firstSplit[1].split("-");
        if(secondSplit.length != 2) {
            throw new IllegalArgumentException();
        }
        chromosome = firstSplit[0];
        try {
            start = Long.parseLong(secondSplit[0]);
            end = Long.parseLong(secondSplit[1]);
        }catch (NumberFormatException e){
            throw new IllegalArgumentException(e);
        }
    }

    public String getChromosome() {
        return chromosome;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return chromosome + ':' + start + "-" + end;
    }
}

package es.gencom.integration.sam.header;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;

public class HeaderLine extends AbstractHeaderLine {
    
    public final static String TAG = "@HD";

    public final static String[] TAGS = {"VN", "SO", "GO", "SS"};
    
    public final String version;
    public final SortingOrder sortingOrder;
    public final AlignmentsGrouping alignmentsGrouping;
    public final String subSorting;

    public HeaderLine(
            final String version,
            final SortingOrder sortingOrder,
            final AlignmentsGrouping alignmentsGrouping,
            final String subSorting) {
        this.version = version;
        this.sortingOrder = sortingOrder;
        this.alignmentsGrouping = alignmentsGrouping;
        this.subSorting = subSorting;
    }

    public HeaderLine(final String line) {

        final String[] tags = SAMHeader.parseHeaderLine(line, Arrays.copyOf(TAGS, TAGS.length));

        version = tags[0];
        sortingOrder = tags[1] == null ? null : SortingOrder.valueOf(tags[1].toUpperCase());
        alignmentsGrouping = tags[2] == null ? null : AlignmentsGrouping.valueOf(tags[2].toUpperCase());
        subSorting = tags[3];
    }
    
    @Override
    public void write(final PrintStream out) throws IOException {
        out.append(TAG);
        
        if (version != null) {
            out.append("\tVN:").append(version);
        }
        if(sortingOrder != null) {
            out.append("\tSO:").print(sortingOrder);
        }
        if(alignmentsGrouping != null) {
            out.append("\tGO:").print(alignmentsGrouping);
        }
        if(subSorting != null) {
            out.append("\tSS:").append(subSorting);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HeaderLine)) return false;
        HeaderLine that = (HeaderLine) o;
        return Objects.equals(version, that.version) &&
                sortingOrder == that.sortingOrder &&
                alignmentsGrouping == that.alignmentsGrouping &&
                Objects.equals(subSorting, that.subSorting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, sortingOrder, alignmentsGrouping, subSorting);
    }
}

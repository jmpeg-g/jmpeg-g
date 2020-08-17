package es.gencom.integration.sam.header;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;

public class CommentLine extends AbstractHeaderLine {
    
    public final static String TAG = "@CO";
    
    public final String comment;

    public CommentLine(final String comment) {
        this.comment = comment;
    }

    @Override
    public void write(final PrintStream out) throws IOException {
        out.append(TAG).append('\t').append(comment);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentLine)) return false;
        CommentLine that = (CommentLine) o;
        return Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment);
    }
}

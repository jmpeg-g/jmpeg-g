package es.gencom.integration.sam.header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SAMHeader {
    protected HeaderLine header_line;
    protected List<ReferenceLine> references;
    protected List<ReadGroupLine> readGroups;
    protected List<ProgramLine> programs;
    protected List<CommentLine> comments;

    protected SAMHeader() {}

    public SAMHeader(final HeaderLine header) {
        this.header_line = header;
    }

    public SAMHeader(final SAMHeader header) {
        this.header_line = header.header_line;
        this.references = header.references;
        this.readGroups = header.readGroups;
        this.programs = header.programs;
        this.comments = header.comments;        
    }
    
    public SAMHeader(
            final HeaderLine header,
            final List<ReferenceLine> references,
            final List<ReadGroupLine> readGroups,
            final List<ProgramLine> programs,
            final List<CommentLine> comments) {

        this.header_line = header;
        this.references = references;
        this.readGroups = readGroups;
        this.programs = programs;
        this.comments = comments;
    }
    
    public SAMHeader(final String header) {
        final String[] lines = header.split("\n");

        if (lines.length > 0) {
            if(lines[0].startsWith(HeaderLine.TAG)) {
                header_line = new HeaderLine(lines[0].substring(Math.min(lines[0].length(), 4)));
            }

            for(int i = header_line == null ? 0 : 1; i < lines.length; i++) {
                switch (lines[i].substring(0, 3)) {
                    case ReferenceLine.TAG:
                        getReferences().add(new ReferenceLine(lines[i].substring(4)));
                        break;
                    case ReadGroupLine.TAG:
                        getReadGroups().add(new ReadGroupLine(lines[i].substring(4)));
                        break;
                    case ProgramLine.TAG:
                        getPrograms().add(new ProgramLine(lines[i].substring(4)));
                        break;
                    case CommentLine.TAG:
                        getComments().add(new CommentLine(lines[i].substring(4)));
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }
    }

    public HeaderLine getHeaderLine() {
        return header_line;
    }

    public void setHeaderLine(final HeaderLine header_line) {
        this.header_line = header_line;
    }

    public final List<ReferenceLine> getReferences() {
        if (references == null) {
            references = new ArrayList<>();
        }
        return references;
    }

    public final List<ReadGroupLine> getReadGroups() {
        if (readGroups == null) {
            readGroups = new ArrayList<>();
        }
        return readGroups;
    }

    public final List<ProgramLine> getPrograms() {
        if (programs == null) {
            programs = new ArrayList<>();
        }        
        return programs;
    }

    public final List<CommentLine> getComments() {
        if (comments == null) {
            comments = new ArrayList<>();
        }   
        return comments;
    }
    
    public final String[] getGroupIds() {
        String[] groupIds = new String[getReadGroups().size()];
        for(int i = 0; i < groupIds.length; i++){
            groupIds[i] = readGroups.get(i).readGroupId;
        }
        return groupIds;
    }

    public void write(final PrintStream out) throws IOException {

        if (header_line != null) {
            header_line.write(out);
            out.append('\n');
        }

        if (references != null) {
            for(ReferenceLine line : references) {
                line.write(out);
                out.append('\n');
            }
        }
        if (readGroups != null) {
            for(ReadGroupLine line : readGroups) {
                line.write(out);
                out.append('\n');
            }
        }
        if (programs != null) {
            for(ProgramLine line : programs) {
                line.write(out);
                out.append('\n');
            }
        }
        if (comments != null) {
            for(CommentLine line : comments) {
                line.write(out);
                out.append('\n');
            }
        }
    }

    @Override
    public String toString() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            write(new PrintStream(out, false, StandardCharsets.US_ASCII.name()));
            return out.toString(StandardCharsets.US_ASCII.name());
        } catch (IOException ex) {}
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SAMHeader)) return false;
        SAMHeader samHeader = (SAMHeader) o;
        return Objects.equals(header_line, samHeader.header_line) &&
               Objects.equals(references, samHeader.references) &&
               Objects.equals(readGroups, samHeader.readGroups) &&
               Objects.equals(programs, samHeader.programs) &&
               Objects.equals(comments, samHeader.comments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getHeaderLine());
        if (references != null) {
            result = 31 * result + references.hashCode();
        }
        if (readGroups != null) {
            result = 31 * result + readGroups.hashCode();
        }
        if (programs != null) {
            result = 31 * result + programs.hashCode();
        }
        if (comments != null) {
            result = 31 * result + comments.hashCode();
        }
        return result;
    }
    
    /**
     * <p>
     * Parses the SAM header line looking for defined tags.
     * </p>
     * N.B. Method returns the same input tags array with found values or null 
     * if no value found for the tag.
     * 
     * @param line SAM header line to be parsed
     * @param tags SAM header tags to be found in the line
     * 
     * @return an array of requested tags with found values
     */
    public static String[] parseHeaderLine(final String line, final String[] tags) {
        
        final String[] elements = line.split("\t");
        if(elements.length == 0) {
            throw new IllegalArgumentException();
        }
        
        label:
        for (int i = 0, n = tags.length; i < n; i++) {
            for (int j = 0, m = elements.length; j < m; j++) {
                if (elements[j].length() > 3 && 
                    elements[j].startsWith(tags[i]) &&
                    elements[j].charAt(2) == ':') {
                    tags[i] = elements[j].substring(3);
                    continue label;
                }
            }
            tags[i] = null;
        }
        return tags;
    }
}

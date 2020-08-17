package es.gencom.integration.sam.header;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;

public class ProgramLine extends AbstractHeaderLine{
    
    public final static String TAG = "@PG";
    
    public final static String[] TAGS = {"ID", "PN", "CL", "PP", "DS", "VN"};
    
    public final String id;
    public final String programName;
    public final String commandLine;
    public final String previousProgramId;
    public final String description;
    public final String programVersion;

    public ProgramLine(
            final String id,
            final String programName,
            final String commandLine,
            final String previousProgramId,
            final String description,
            final String programVersion) {
        this.id = id;
        this.programName = programName;
        this.commandLine = commandLine;
        this.previousProgramId = previousProgramId;
        this.description = description;
        this.programVersion = programVersion;
    }
            
    public ProgramLine(final String line) {
        
        final String[] tags = SAMHeader.parseHeaderLine(line, Arrays.copyOf(TAGS, TAGS.length));
        
        id = tags[0];
        programName = tags[1];
        commandLine = tags[2];
        previousProgramId = tags[3];
        description = tags[4];
        programVersion = tags[5];
    }

    @Override
    public void write(final PrintStream out) throws IOException {
        out.append(TAG);
        
        if (id != null) {
            out.append("\tID:").append(id);
        }
        if(programName != null){
            out.append("\tPN:").append(programName);
        }
        if(previousProgramId != null){
            out.append("\tPP:").append(previousProgramId);
        }
        if(description != null){
            out.append("\tDS:").append(description);
        }
        if(programVersion != null){
            out.append("\tVN:").append(programVersion);
        }
        if(commandLine != null){
            out.append("\tCL:").append(commandLine);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgramLine)) return false;
        ProgramLine that = (ProgramLine) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(programName, that.programName) &&
               Objects.equals(commandLine, that.commandLine) &&
               Objects.equals(previousProgramId, that.previousProgramId) &&
               Objects.equals(description, that.description) &&
               Objects.equals(programVersion, that.programVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, programName, commandLine, previousProgramId, description, programVersion);
    }
}

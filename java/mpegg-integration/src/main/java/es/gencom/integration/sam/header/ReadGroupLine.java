package es.gencom.integration.sam.header;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;

public class ReadGroupLine extends AbstractHeaderLine {
    
    public final static String TAG = "@RG";
    
    public final static String[] TAGS = {"ID", "BC", "CN", "DS", "DT", "FO", 
        "KS", "LB", "PG", "PI", "PL", "PM", "PU", "SM"};
    
    public final String readGroupId;
    public final String barcodes;
    public final String sequencingCenter;
    public final String description;
    public final String date;
    public final String flowOrder;
    public final String keySequence;
    public final String library;
    public final String programs;
    public final Double median;
    public final ReadGroupLinePlatform platform;
    public final String platformModel;
    public final String platformUnit;
    public final String sample;

    public ReadGroupLine(
            final String readGroupId,
            final String barcodes,
            final String sequencingCenter,
            final String description,
            final String date,
            final String flowOrder,
            final String keySequence,
            final String library,
            final String programs,
            final Double median,
            final ReadGroupLinePlatform platform,
            final String platformModel,
            final String platformUnit,
            final String sample) {

        this.readGroupId = readGroupId;
        this.barcodes = barcodes;
        this.sequencingCenter = sequencingCenter;
        this.description = description;
        this.date = date;
        this.flowOrder = flowOrder;
        this.keySequence = keySequence;
        this.library = library;
        this.programs = programs;
        this.median = median;
        this.platform = platform;
        this.platformModel = platformModel;
        this.platformUnit = platformUnit;
        this.sample = sample;
    }

    public ReadGroupLine(final String line) {
        
        final String[] tags = SAMHeader.parseHeaderLine(line, Arrays.copyOf(TAGS, TAGS.length));
        
        readGroupId = tags[0];
        barcodes = tags[1];
        sequencingCenter = tags[2];
        description = tags[3];
        date = tags[4];
        flowOrder = tags[5];
        keySequence = tags[6];
        library = tags[7];
        programs = tags[8];
        median = tags[9] == null ? null : Double.valueOf(tags[9]);
        platform = tags[10] == null ? null : ReadGroupLinePlatform.valueOf(tags[10]);
        platformModel = tags[11];
        platformUnit = tags[12];
        sample = tags[13];
    }

    @Override
    public void write(final PrintStream out) throws IOException {
        out.append(TAG);
        
        if (readGroupId != null) {
            out.append("\tID:").append(readGroupId);
        }
        if(platform != null) {
            out.append("\tPL:").print(platform);
        }
        if(platformUnit != null){
            out.append("\tPU:").append(platformUnit);
        }
        if(library != null) {
            out.append("\tLB:").append(library);
        }
        if(description != null) {
            out.append("\tDS:").append(description);
        }
        if(barcodes != null) {
            out.append("\tBC:").append(barcodes);
        }
        if(date != null) {
            out.append("\tDT:").append(date);
        }
        if(flowOrder != null) {
            out.append("\tFO:").append(flowOrder);
        }
        if(keySequence != null) {
            out.append("\tKS:").append(keySequence);
        }
        if(programs != null) {
            out.append("\tPG:").append(programs);
        }
        if(median != null) {
            out.append("\tPI:").print(median);
        }
        if(platformModel != null) {
            out.append("\tPM:").append(platformModel);
        }
        if(sample != null) {
            out.append("\tSM:").append(sample);
        }
        if(sequencingCenter != null) {
            out.append("\tCN:").append(sequencingCenter);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadGroupLine)) return false;
        ReadGroupLine that = (ReadGroupLine) o;
        return Objects.equals(readGroupId, that.readGroupId) &&
               Objects.equals(barcodes, that.barcodes) &&
               Objects.equals(sequencingCenter, that.sequencingCenter) &&
               Objects.equals(description, that.description) &&
               Objects.equals(date, that.date) &&
               Objects.equals(flowOrder, that.flowOrder) &&
               Objects.equals(keySequence, that.keySequence) &&
               Objects.equals(library, that.library) &&
               Objects.equals(programs, that.programs) &&
               Double.compare(median, that.median) == 0 &&
               platform == that.platform &&
               Objects.equals(platformModel, that.platformModel) &&
               Objects.equals(platformUnit, that.platformUnit) &&
               Objects.equals(sample, that.sample);
    }

    @Override
    public int hashCode() {
        return Objects.hash(readGroupId, barcodes, sequencingCenter, description, 
                date, flowOrder, keySequence, library, programs, median, platform, 
                platformModel, platformUnit, sample);
    }
}

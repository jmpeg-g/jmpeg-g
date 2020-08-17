package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMFileInputStream;
import es.gencom.integration.bam.BAMRecord;
import es.gencom.integration.bam.BAMRecordsIterator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.DataFormatException;

public class QualityHistogram {
    public static byte[] obtain(String bamPath) throws IOException, DataFormatException {
        long[][] values = new long[93][2];

        for(int i=0; i<93; i++){
            values[i][1] = i;
        }

        BAMFileInputStream bamFileInputStream = new BAMFileInputStream(Paths.get(bamPath));
        BAMRecordsIterator bamRecordsIterator = new BAMRecordsIterator(bamFileInputStream);

        while(bamRecordsIterator.hasNext()){
            BAMRecord bamRecord = bamRecordsIterator.next();
            byte[] qualities = bamRecord.getQualityBytes();
            for(int i=0; i<qualities.length; i++){
                values[qualities[i]][0] += 1;
            }
        }

        Arrays.sort(values, new Comparator<long[]>() {
            @Override
            public int compare(long[] thisArray, long[] thatArray) {
                return -Long.compare(thisArray[0], thatArray[0]);
            }
        });

        byte[] histogram = new byte[values.length];
        for(int i=0; i<values.length; i++){
            histogram[i] = (byte) values[i][1];
        }
        return histogram;
    }

}

package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMFileOutputStream;
import es.gencom.integration.bam.BAMHeader;
import es.gencom.integration.bam.BAMRecord;
import es.gencom.integration.sam.header.SAMHeader;
import es.gencom.integration.sam.header.HeaderLine;
import es.gencom.integration.sam.header.ReadGroupLine;
import es.gencom.integration.sam.header.SortingOrder;
import es.gencom.mpegg.decoder.Exceptions.InvalidSymbolException;
import es.gencom.mpegg.decoder.Exceptions.MissingRequiredDescriptorException;
import es.gencom.mpegg.decoder.SequencesFromDataUnitsRawReference;
import es.gencom.mpegg.dataunits.DataUnits;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DataFormatException;

public class MPEGGBytestreamToBAM {
    public static void decode(
            DataUnits dataUnits,
            String outputPathBAM,
            String[] sequencesNames,
            String[] groupNames
    ) throws IOException, DataFormatException, InvalidSymbolException, MissingRequiredDescriptorException {
        MPEGGdecodingTask mpegGdecodingTask = new MPEGGdecodingTask(
                sequencesNames,
                dataUnits,
                new SequencesFromDataUnitsRawReference(
                        dataUnits.getDataUnitRawReference(),
                        sequencesNames
                )
        );
        System.out.println("finished creating decoding taks");


        String currentSequence = null;
        long currentPosition = -1;
        long readCount = 0;
        Iterator<BAMRecord> bamRecordIterator = new MPEGGDecodingIterator(mpegGdecodingTask);

        BAMHeader.Reference[] references = new BAMHeader.Reference[sequencesNames.length];
        for(int reference_i=0; reference_i < sequencesNames.length; reference_i++){
            references[reference_i] = new BAMHeader.Reference(
                    sequencesNames[reference_i],
                    0
            );
        }

        List<ReadGroupLine> groupsLines = new ArrayList<>();
        for(int group_i = 0; group_i < groupNames.length; group_i++){
            groupsLines.add(new ReadGroupLine(
                    groupNames[group_i],
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
        }

        SAMHeader samHeader = new SAMHeader(
                new HeaderLine("1.4", SortingOrder.COORDINATE, null, null));
        samHeader.getReadGroups().addAll(groupsLines);

        BAMHeader bamHeader = new BAMHeader(samHeader, references);
        
        try(BAMFileOutputStream bamFileOutputStream = 
                new BAMFileOutputStream(Paths.get(outputPathBAM),bamHeader)) {

            while (bamRecordIterator.hasNext()) {
                BAMRecord bamRecord = bamRecordIterator.next();

                if (currentSequence == null) {
                    currentSequence = bamRecord.getRName();
                } else {
                    if (!bamRecord.isUnmappedSegment() && !bamRecord.getRName().equals(currentSequence)) {
                        currentSequence = bamRecord.getRName();
                        currentPosition = -1;
                        System.out.println("current sequence : " + currentSequence);
                    }
                }
                if (Math.abs(bamRecord.getPositionStart() - currentPosition) > 10000000) {
                    currentPosition = bamRecord.getPositionStart();
                    System.out.println("\tcurrent position: " + currentPosition);
                }

                bamFileOutputStream.write(bamRecord);

                readCount++;
            }
        }
    }
}

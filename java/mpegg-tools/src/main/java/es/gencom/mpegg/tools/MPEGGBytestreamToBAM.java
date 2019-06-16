package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMFileOutputStream;
import es.gencom.integration.bam.BAMHeader;
import es.gencom.integration.bam.BAMRecord;
import es.gencom.mpegg.decoder.exceptions.InvalidSymbolException;
import es.gencom.mpegg.decoder.exceptions.MissingRequiredDescriptorException;
import es.gencom.mpegg.decoder.SequencesFromDataUnitsRawReference;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;
import es.gencom.mpegg.coder.dataunits.DataUnitRawReference;
import es.gencom.mpegg.coder.dataunits.DataUnits;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.zip.DataFormatException;

public class MPEGGBytestreamToBAM {
    public static void decode(
            String inputPathMPEGG,
            String rawReferenceFileName,
            String outputPathBAM,
            String[] sequencesNames
    ) throws IOException, DataFormatException, InvalidSymbolException, MissingRequiredDescriptorException {

        FileInputStream dataUnitsInputStream = new FileInputStream(new File(inputPathMPEGG));
        MPEGReader mpegReader = new ReadableMSBitFileChannel(dataUnitsInputStream.getChannel());
        DataUnits dataUnits = DataUnits.read(mpegReader);


        MPEGGdecodingTask mpegGdecodingTask = new MPEGGdecodingTask(
                sequencesNames,
                dataUnits,
                new SequencesFromDataUnitsRawReference(
                        DataUnitRawReference.read(
                                new ReadableMSBitFileChannel(FileChannel.open(Paths.get(rawReferenceFileName))),
                                dataUnits
                        ),
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

        BAMHeader bamHeader = new BAMHeader("", references);
        
        try(BAMFileOutputStream bamFileOutputStream = 
                new BAMFileOutputStream(Paths.get(outputPathBAM),bamHeader)) {

            while (bamRecordIterator.hasNext()) {
                BAMRecord bamRecord = bamRecordIterator.next();

                if (currentSequence == null) {
                    currentSequence = bamRecord.getRName();
                } else {
                    if (!bamRecord.getRName().equals(currentSequence)) {
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

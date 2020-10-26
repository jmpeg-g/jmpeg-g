package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMFileReader;
import es.gencom.integration.bam.BAMHeader;
import es.gencom.integration.bam.BAMRecord;
import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaIterator;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.AbstractAccessUnitEncoder;
import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.HalfMappedAccessUnitEncoder;
import es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders.MappedAccessUnitEncoder;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ENCODING_MODE_ID;
import es.gencom.mpegg.coder.compression.QV_CODING_MODE;
import es.gencom.mpegg.coder.configuration.DescriptorDecoderConfigurationFactory;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.coder.tokens.AbstractReadIdentifierEncoder;
import es.gencom.mpegg.coder.tokens.GeneralReadIdentifierEncoder;
import es.gencom.mpegg.coder.tokens.IlluminaReadIdentifierEncoder;
import es.gencom.mpegg.coder.tokens.TokentypeDecoderConfigurationFactory;
import es.gencom.mpegg.decoder.AbstractSequencesSource;
import es.gencom.mpegg.decoder.SequencesFromDataUnitsRawReference;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DatasetType;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;
import es.gencom.mpegg.io.WritableMSBitChannel;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;
import es.gencom.mpegg.coder.dataunits.DataUnitRawReference;
import es.gencom.mpegg.coder.dataunits.DataUnits;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.DataFormatException;

import static es.gencom.mpegg.coder.compression.DESCRIPTOR_ID.*;

public class BAMToMPEGGBytestream {
    private static long numDiscardedRecords = 0;

    private static AbstractAccessUnitEncoder[] createAccessUnitEncoders(
            int current_sequence_i,
            int auId,
            long startPosition,
            long endPosition,
            short threshold,
            AbstractSequencesSource sequencesSource,
            DataUnitParameters[] encodingParametersPerClass,
            boolean useIlluminaReadIdentifierEncoder
    ) throws IOException {
        AbstractAccessUnitEncoder[] result = new AbstractAccessUnitEncoder[5];
        for (byte dataClass_i = 1; dataClass_i < 5; dataClass_i++) {
            AbstractReadIdentifierEncoder readIdentifierEncoder;
            if(useIlluminaReadIdentifierEncoder){
                readIdentifierEncoder = new IlluminaReadIdentifierEncoder();
            }else{
                readIdentifierEncoder = new GeneralReadIdentifierEncoder();
            }
            result[dataClass_i - 1] = new MappedAccessUnitEncoder(
                    DATA_CLASS.getDataClass(dataClass_i),
                    (short)current_sequence_i,
                    auId,
                    startPosition,
                    endPosition,
                    threshold,
                    startPosition,
                    endPosition,
                    sequencesSource,
                    encodingParametersPerClass[dataClass_i-1],
                    readIdentifierEncoder
            );
        }
        AbstractReadIdentifierEncoder readIdentifierEncoder;
        if(useIlluminaReadIdentifierEncoder){
            readIdentifierEncoder = new IlluminaReadIdentifierEncoder();
        }else{
            readIdentifierEncoder = new GeneralReadIdentifierEncoder();
        }
        result[DATA_CLASS.CLASS_HM.ID - 1] = new HalfMappedAccessUnitEncoder(
                DATA_CLASS.CLASS_HM,
                (short)current_sequence_i,
                auId,
                startPosition,
                endPosition,
                threshold,
                startPosition,
                endPosition,
                encodingParametersPerClass[DATA_CLASS.CLASS_HM.ID - 1],
                sequencesSource,
                readIdentifierEncoder
        );
        return result;
    }

    private static EncodingParameters createEncodingParameters(
            byte number_template_segments,
            DATA_CLASS dataClass
    ) throws IOException {
        EncodingParameters encodingParameters = new EncodingParameters();
        encodingParameters.setDatasetType(DatasetType.ALIGNED);
        ALPHABET_ID alphabet_id = ALPHABET_ID.IUPAC;
        encodingParameters.setAlphabetId(alphabet_id);
        encodingParameters.setReadsLength(0);
        encodingParameters.setMaximumAUDataUnitSize((int) Math.pow(2, 28));
        encodingParameters.setPos40Bits(false);
        encodingParameters.setQVDepth((byte) 1);
        encodingParameters.setASDepth((byte)1);
        encodingParameters.resetAndResizeClasses(1);
        encodingParameters.setClassId(0, dataClass.ID);

        encodingParameters.resetAndResizeReadGroups(0);

        encodingParameters.setQv_coding_mode(0, QV_CODING_MODE.UNQUANTIZED.ID);
        encodingParameters.setQvps_flag(0, false);
        encodingParameters.setDefault_qvps_ID(0, (byte) 0);

        encodingParameters.setSpliced_reads_flag(false);
        encodingParameters.setMultiple_signature_base(0);
        encodingParameters.setU_signature_size((short) 8);
        encodingParameters.setCrps_flag(false);
        boolean multipleAlignments = false;
        encodingParameters.setMultiple_alignments_flag(multipleAlignments);
        encodingParameters.setNumberOfTemplateSegments(number_template_segments);


        for(byte descriptor_i=0; descriptor_i<18; descriptor_i++){
            if(descriptor_i == RNAME.ID || descriptor_i == MSAR.ID){
                encodingParameters.setDefaultDecoderConfiguration(
                        DESCRIPTOR_ID.getDescriptorId(descriptor_i),
                        TokentypeDecoderConfigurationFactory.getDefaultDecoderConfiguration(ENCODING_MODE_ID.CABAC)
                );
            } else {
                encodingParameters.setDefaultDecoderConfiguration(
                        DESCRIPTOR_ID.getDescriptorId(descriptor_i),
                        DescriptorDecoderConfigurationFactory.getDefaultDecoderConfiguration(
                                ENCODING_MODE_ID.CABAC,
                                DESCRIPTOR_ID.getDescriptorId(descriptor_i),
                                encodingParameters.getQualityValueParameterSet(dataClass).getNumberQualityBooks(),
                                alphabet_id,
                                !multipleAlignments
                        )
                );
            }
        }

        return encodingParameters;
    }

    private static AbstractAccessUnitEncoder[] writeCompletedRecords(
            AbstractAccessUnitEncoder[] accessUnitEncoders,
            LinkedHashMap<String, RecordConstructor> alignments,
            MPEGWriter writer,
            DataUnits dataUnits,
            short threshold,
            AbstractSequencesSource sequencesSource,
            long auWidth,
            DataUnitParameters[] dataUnitParametersPerClass,
            boolean useIlluminaReadIdentifierEncoder,
            BAMHeader bamHeader,
            SequenceIdentifier currentSequenceIdentifier,
            long currentSequencePosition
    ) throws IOException {
        long endPosition;
        int currentSequence;
        int auId;

        if(accessUnitEncoders != null){
            endPosition = accessUnitEncoders[0].getAuEndPosition();
            currentSequence = accessUnitEncoders[0].getSequenceId();
            auId = accessUnitEncoders[0].getAuId();
        }

        /*Discard reads which should have been completed and are not*/
        Set<String> discardedRecords = new HashSet<>();
        for (Map.Entry<String, RecordConstructor> entry : alignments.entrySet()) {
            if(!entry.getValue().couldBeCompleted()){

                SequenceIdentifier sequenceId = sequencesSource.getSequenceIdentifier(
                        bamHeader.getReference(entry.getValue().getFirstRecord().getRefID()).name
                );
                if(!currentSequenceIdentifier.equals(sequenceId)){
                    discardedRecords.add(entry.getKey());
                    continue;
                }
                if(entry.getValue().getFirstRecord().getNextPositionStart() + 2000 < currentSequencePosition){
                    discardedRecords.add(entry.getKey());
                }
            }

        }
        for(String readNameToDiscard : discardedRecords){
            //System.err.println("An error was detected with read "+readNameToDiscard+" discarding it.");
            alignments.remove(readNameToDiscard);
            numDiscardedRecords++;
            if(numDiscardedRecords % 1000 == 0){
                System.err.println("Has already discarded "+numDiscardedRecords+ " records.");
            }
        }


        Set<String> writtenRecords = new HashSet<>();
        for (Map.Entry<String, RecordConstructor> entry : alignments.entrySet()) {
            if (
                    entry.getValue().couldBeCompleted()
            ) {
                Record record = entry.getValue().construct(sequencesSource, bamHeader);
                DATA_CLASS dataClass = entry.getValue().getDataClass();

                if(accessUnitEncoders == null){
                    currentSequence = record.getSequenceId().getSequenceIdentifier();
                    accessUnitEncoders = createAccessUnitEncoders(
                            currentSequence,
                            0,
                            record.getMappingPositionsSegment0()[0][0],
                            record.getMappingPositionsSegment0()[0][0] + auWidth,
                            threshold,
                            sequencesSource,
                            dataUnitParametersPerClass,
                            useIlluminaReadIdentifierEncoder
                    );
                    endPosition = accessUnitEncoders[0].getAuEndPosition();
                    auId = accessUnitEncoders[0].getAuId();
                }else{
                    endPosition = accessUnitEncoders[0].getAuEndPosition();
                    currentSequence = accessUnitEncoders[0].getSequenceId();
                    auId = accessUnitEncoders[0].getAuId();
                }

                boolean hasToCloseAccessUnits = false;
                boolean hasToStartNewSequence = false;
                boolean keepPositions = false;
                if(record.getSequenceId().getSequenceIdentifier() != currentSequence) {
                    System.out.println("Starting sequence " + record.getSequenceId().getSequenceIdentifier());
                    currentSequence =record.getSequenceId().getSequenceIdentifier();
                    hasToStartNewSequence = true;
                    hasToCloseAccessUnits = true;
                }else if(record.getMappingPositionsSegment0()[0][0] >= endPosition) {
                    hasToCloseAccessUnits = true;
                }

                if(!hasToCloseAccessUnits){
                    long maxMemory = 0;
                    for(AbstractAccessUnitEncoder abstractAccessUnitEncoder : accessUnitEncoders){
                        maxMemory = Long.max(maxMemory, abstractAccessUnitEncoder.getTotalSizeInMemory());
                    }
                    if(maxMemory > 500000){
                        hasToCloseAccessUnits = true;
                        keepPositions = true;
                    }
                }

                if(hasToCloseAccessUnits){

                    System.out.println("Finished with aus with id = "+accessUnitEncoders[0].getAuId());
                    for (AbstractAccessUnitEncoder abstractAccessUnitEncoder : accessUnitEncoders) {
                        if(abstractAccessUnitEncoder.getReadCount() != 0) {
                            SAMtoMPEGG.writeAccessUnitToFile(
                                    writer,
                                    abstractAccessUnitEncoder,
                                    dataUnits.getParameter(abstractAccessUnitEncoder.getEncodingParametersId())
                            );
                        }
                    }
                    long startPosition = accessUnitEncoders[0].getAuStartPosition();
                    auId++;
                    if(!keepPositions) {
                        startPosition = accessUnitEncoders[0].getAuStartPosition() + auWidth;
                        endPosition += auWidth;
                        if (hasToStartNewSequence) {
                            startPosition = 0;
                            endPosition = auWidth;
                            auId = 0;
                        }

                        while (record.getMappingPositionsSegment0()[0][0] >= endPosition) {
                            startPosition += auWidth;
                            endPosition += auWidth;
                        }
                    }
                    accessUnitEncoders = createAccessUnitEncoders(
                            currentSequence,
                            auId,
                            startPosition,
                            endPosition,
                            threshold,
                            sequencesSource,
                            dataUnitParametersPerClass,
                            useIlluminaReadIdentifierEncoder
                    );
                }

                accessUnitEncoders[dataClass.ID - 1].write(record);
                writtenRecords.add(entry.getKey());
            } else {
                break;
            }
        }

        for(String readName : writtenRecords){
            if(alignments.remove(readName)==null){
                System.out.println("not removing: "+readName);
            };
        }

        return accessUnitEncoders;
    }


    private static AbstractAccessUnitEncoder[] writeAndFinish(
            AbstractAccessUnitEncoder[] accessUnitEncoders,
            LinkedHashMap<String, RecordConstructor> alignments,
            MPEGWriter writer,
            DataUnits dataUnits,
            short threshold,
            AbstractSequencesSource rawReference,
            long auWidth,
            DataUnitParameters[] dataUnitParametersPerClass,
            boolean useIlluminaReadIdentifierEncoder,
            BAMHeader bamHeader
    ) throws IOException {
        long endPosition;
        int currentSequence;
        int auId;

        Set<String> writtenRecords = new HashSet<>();

        for (Map.Entry<String, RecordConstructor> entry : alignments.entrySet()) {
            if (
                    entry.getValue().couldBeCompleted()
            ) {
                Record record = entry.getValue().construct(rawReference, bamHeader);
                DATA_CLASS dataClass = entry.getValue().getDataClass();

                accessUnitEncoders[dataClass.ID - 1].write(record);
                writtenRecords.add(entry.getKey());
            } else {
                break;
            }
        }

        for(String readName : writtenRecords){
            alignments.remove(readName);
        }
        if(alignments.size() > 0){
            throw new InternalError();
        }

        if (accessUnitEncoders != null && accessUnitEncoders.length > 0) {
            System.out.println("Finished with aus with id = " + accessUnitEncoders[0].getAuId());

            for (AbstractAccessUnitEncoder abstractAccessUnitEncoder : accessUnitEncoders) {
                if(abstractAccessUnitEncoder.getReadCount() != 0) {
                    SAMtoMPEGG.writeAccessUnitToFile(
                            writer,
                            abstractAccessUnitEncoder,
                            dataUnits.getParameter(abstractAccessUnitEncoder.getEncodingParametersId())
                    );
                }
            }
        }

        for(String readName : writtenRecords){
            alignments.remove(readName);
        }

        return accessUnitEncoders;
    }

    static void encode(
            String inputBamPath,
            String fastaReferencePath,
            String outputBsPath,
            boolean useIlluminaReadIdentifierEncoder
    ) throws IOException, DataFormatException {
        String rawReferencePath = fastaReferencePath.replace("fa", "rawReference");
        String sequenceNamesPath = fastaReferencePath.replace("fa", "sequenceNames");
        String[] sequenceNames;
        File rawReferenceFile = new File(rawReferencePath);
        File sequenceNamesFile = new File(sequenceNamesPath);
        if(rawReferenceFile.exists() && sequenceNamesFile.exists()) {
            System.out.println("Raw reference already exists.");
            sequenceNames = SequenceNamesParser.getSequenceNames(Paths.get(sequenceNamesPath));
        } else {
            System.out.println("Starting conversion to raw reference");
            FastaFileToRawReferenceFile fastaFileToRawReferenceFile = new FastaFileToRawReferenceFile(fastaReferencePath);
            rawReferencePath = fastaFileToRawReferenceFile.getRawReferencePath().toString();
            sequenceNames = fastaFileToRawReferenceFile.getSequenceNames();
            System.out.println("Finished conversion to raw reference");
        }

        System.out.println("Reading raw reference");
        DataUnitRawReference dataUnitRawReference = DataUnitRawReference.read(

                new ReadableMSBitFileChannel(new FileInputStream(Paths.get(rawReferencePath).toFile()).getChannel()),
                null
        );

        SequencesFromDataUnitsRawReference rawReference = new SequencesFromDataUnitsRawReference(
                dataUnitRawReference,
                sequenceNames
        );

        FileChannel testOutput = FileChannel.open(
                Paths.get(outputBsPath), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        WritableMSBitChannel writer = new WritableMSBitChannel(testOutput);

        byte number_template_segments = 2;

        DataUnits dataUnits = new DataUnits();
        DataUnitParameters[] dataUnitParametersPerClass = new DataUnitParameters[5];
        for(byte data_class_i=0; data_class_i<5; data_class_i++){
            EncodingParameters encodingParameters = createEncodingParameters(
                    number_template_segments,
                    DATA_CLASS.getDataClass((byte) (data_class_i+1))
            );
            dataUnitParametersPerClass[data_class_i] = new DataUnitParameters(
                    (short)data_class_i,
                    (short)data_class_i,
                    encodingParameters,
                    dataUnits
            );
            dataUnits.addDataUnitParameters(dataUnitParametersPerClass[data_class_i]);

            dataUnitParametersPerClass[data_class_i].write(writer);
        }
        System.out.println("Encoding parameters read");


        BAMFileReader bamFileReader = new BAMFileReader(Paths.get(inputBamPath));

        long auWidth = 6000000;

        Iterator<BAMRecord> bamIterator = bamFileReader.iterator();
        int current_sequence_i = -1;
        long currentPosition = -1;
        short threshold = 5000;
        long numberReads = 0;

        AbstractAccessUnitEncoder[] accessUnitEncoders = null;
        LinkedHashMap<String, RecordConstructor> alignments = new LinkedHashMap<>();
        List<BAMRecord> previouslyReadUnmappedFromHalfMapped = new ArrayList<>();
        while (bamIterator.hasNext()){
            ListIterator<BAMRecord> unmappedRecords_iter = previouslyReadUnmappedFromHalfMapped.listIterator();
            while(unmappedRecords_iter.hasNext()){
                BAMRecord unmappedRecord = unmappedRecords_iter.next();
                if(alignments.get(unmappedRecord.getQName()) != null){
                    alignments.get(unmappedRecord.getQName()).addSegment(unmappedRecord);
                    unmappedRecords_iter.remove();
                }
            }

            BAMRecord bamRecord = bamIterator.next();
            if(bamRecord.isUnmappedSegment()) {
                if (bamRecord.isNextSegmentUnmapped()) {
                    continue;
                } else {
                    if (bamRecord.isUnmappedSegment()) {
                        previouslyReadUnmappedFromHalfMapped.add(bamRecord);
                        continue;
                    }
                }
            }
            String readName = bamRecord.getQName();

            if (alignments.containsKey(readName)) {
                alignments.get(readName).addSegment(bamRecord);
            } else {
                RecordConstructor constructor = new RecordConstructor(
                        numberReads,
                        bamRecord.getQName(),
                        "", //todo add getter for group name
                        bamRecord,
                        threshold
                );
                numberReads++;

                alignments.put(readName, constructor);
            }

            SequenceIdentifier sequenceId = rawReference.getSequenceIdentifier(
                    bamFileReader.getBAMHeader().getReference(bamRecord.getRefID()).name
            );

            accessUnitEncoders =  writeCompletedRecords(
                accessUnitEncoders,
                alignments,
                writer,
                dataUnits,
                threshold,
                rawReference,
                auWidth,
                dataUnitParametersPerClass,
                useIlluminaReadIdentifierEncoder,
                bamFileReader.getBAMHeader(),
                sequenceId,
                bamRecord.getPositionStart() - 1
            );
        }
        writeAndFinish(
                accessUnitEncoders,
                alignments,
                writer,
                dataUnits,
                threshold,
                rawReference,
                auWidth,
                dataUnitParametersPerClass,
                useIlluminaReadIdentifierEncoder,
                bamFileReader.getBAMHeader()
        );
        testOutput.close();
    }

    public static long getNumDiscardedRecords() {
        return numDiscardedRecords;
    }
}

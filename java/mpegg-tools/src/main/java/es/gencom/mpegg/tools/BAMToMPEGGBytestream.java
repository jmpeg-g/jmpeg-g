package es.gencom.mpegg.tools;

import es.gencom.integration.bam.BAMFileReader;
import es.gencom.integration.bam.BAMHeader;
import es.gencom.integration.bam.BAMRecord;
import es.gencom.integration.bam.BufferedBAMFileReader;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ENCODING_MODE_ID;
import es.gencom.mpegg.coder.compression.QV_CODING_MODE;
import es.gencom.mpegg.coder.configuration.DescriptorDecoderConfigurationFactory;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.coder.configuration.QualityValuesParameterSet;
import es.gencom.mpegg.coder.tokens.AbstractReadIdentifierEncoder;
import es.gencom.mpegg.coder.tokens.GeneralReadIdentifierEncoder;
import es.gencom.mpegg.coder.tokens.IlluminaReadIdentifierEncoder;
import es.gencom.mpegg.coder.tokens.TokentypeDecoderConfigurationFactory;
import es.gencom.mpegg.dataunits.DataUnitParameters;
import es.gencom.mpegg.dataunits.DataUnitRawReference;
import es.gencom.mpegg.dataunits.DataUnits;
import es.gencom.mpegg.decoder.AbstractSequencesSource;
import es.gencom.mpegg.decoder.GenomicPosition;
import es.gencom.mpegg.decoder.SequencesFromDataUnitsRawReference;
import es.gencom.mpegg.encoder.AbstractAccessUnitEncoder;
import es.gencom.mpegg.encoder.HalfMappedAccessUnitEncoder;
import es.gencom.mpegg.encoder.MappedAccessUnitEncoder;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DatasetType;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;
import es.gencom.mpegg.io.WritableMSBitChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.DataFormatException;

import static es.gencom.mpegg.coder.compression.DESCRIPTOR_ID.MSAR;
import static es.gencom.mpegg.coder.compression.DESCRIPTOR_ID.RNAME;

public class BAMToMPEGGBytestream {
    private static long numDiscardedRecords = 0;
    private static SequenceIdentifier[] cachedSequenceIdentifiers;
    private static int numStuckedAt = 0;
    private static Set<String> previousSamePositionCompleted = new HashSet<>();
    private static int previousSamePositionSequence = -1;
    private static int previousSamePositionPosition = -1;

    /**
     * Creates an array of new access unit encoders (one for each mapped classes, i.e. P, N, M, I, HM)
     * @param current_sequence_i The id of the sequence the records will be mapped to
     * @param auId The id to use for the acces units
     * @param startPosition The start position of the access unit
     * @param endPosition The end position as reported in the headers
     * @param threshold The threshold at which paired template must be split
     * @param sequencesSource Source for the reference nucleotides
     * @param encodingParametersPerClass Entropy encoders
     * @param useIlluminaReadIdentifierEncoder Should the illumina read identifier be used or the general one
     * @return An array of created access unit encoders, one for each mapped classes (P, N, M, I, HM)
     * @throws IOException
     */
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

    /**
     * Generates the encoding parameters for one class
     * @param number_template_segments The number of segments (either 1 or 2)
     * @param dataClass The data class for which the encoding parameters is being generated.
     * @param qualityHistogram Either null if no quality histogram has been calculated, or the different quality values
     *                         present in the access unit ordered from most frequent to less frequent. (This corresponds
     *                         to one qualitybook, and only one quality book is accepted).
     * @param readGroups The read groups present in the access unit
     * @return Returns an EncodingParameters instance configured as requested.
     * @throws IOException
     */
    private static EncodingParameters createEncodingParameters(
            byte number_template_segments,
            DATA_CLASS dataClass,
            byte[] qualityHistogram,
            String[] readGroups
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

        encodingParameters.resetAndResizeReadGroups(readGroups.length);
        for(int group_i=0; group_i < readGroups.length; group_i++){
            encodingParameters.setReadGroupId(group_i, readGroups[group_i]);
        }

        encodingParameters.setQv_coding_mode(0, QV_CODING_MODE.UNQUANTIZED.ID);
        if(qualityHistogram == null){
            encodingParameters.setQvps_flag(0, false);
            encodingParameters.setDefault_qvps_ID(0, (byte) 0);
        }else {
            encodingParameters.setQvps_flag(0, true);
            encodingParameters.setQualityValuesParameterSet(0,
                    new QualityValuesParameterSet(qualityHistogram)
            );
        }




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

    /**
     * Writes all records which are in the queue and can be completed (upon finding the first one which cannot be
     * completed).
     * @param accessUnitEncoders The access units currently being used. They might get closed, and new ones generated.
     * @param alignments The records being currently constructed
     * @param writer The output mpeg writer
     * @param dataUnits The DataUnits instance being completed
     * @param threshold The threshold as specified in the specification: controls when two segments of a same template
     *                  should be stored together or separeted.
     * @param sequencesSource The source of reference nucleotides.
     * @param auWidth The width in terms of positions an access unit shall have.
     * @param dataUnitParametersPerClass The different parameters to use for each of the class (mainly entropy encoding).
     * @param useIlluminaReadIdentifierEncoder Whether the illumina read identifier encoder shall be used or another one.
     * @param bamHeader The bam header of the file being transcoded.
     * @param currentSequenceIdentifier The currently being encoded sequence
     * @param currentSequencePosition The last encoded position.
     * @return returns the same access unit encoders given as input, or new ones if the previous ones had to be closed
     * @throws IOException
     */
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


        /*Discard reads which should have been completed and are not*/
        /*This could happen if for example a mate is somehow missing in the original file*/
        /*discardedRecords contains all read names of records which will be deleted*/
        Set<String> discardedRecords = new HashSet<>();
        for (Map.Entry<String, RecordConstructor> entry : alignments.entrySet()) {
            if(!entry.getValue().couldBeCompleted()){

                SequenceIdentifier sequenceId = fromBAMSequenceIdentifierToMPEGGSequenceIdentifier(
                        entry.getValue().getFirstRecord().getRefID(),
                        bamHeader,
                        sequencesSource
                );
                if(!currentSequenceIdentifier.equals(sequenceId)){
                    discardedRecords.add(entry.getKey());
                    continue;
                }
                /*If the mate should have apperead already 2000 position ago, add it to the list to discard*/
                if(entry.getValue().getFirstRecord().getNextPositionStart() + 2000 < currentSequencePosition){
                    discardedRecords.add(entry.getKey());
                }
            }

        }
        /*Discard the selected reads*/
        for(String readNameToDiscard : discardedRecords){
            System.err.println("An error was detected with read "+readNameToDiscard+" discarding it.");
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
                            record.getMappingPositions()[0][0][0],
                            record.getMappingPositions()[0][0][0] + auWidth,
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

                //Tests if new access units should be opened (because a new sequence has begun, or it is passed the end
                // of the current units)
                if(record.getSequenceId().getSequenceIdentifier() != currentSequence) {
                    System.out.println("Starting sequence " + record.getSequenceId().getSequenceIdentifier());
                    currentSequence =record.getSequenceId().getSequenceIdentifier();
                    hasToStartNewSequence = true;
                    hasToCloseAccessUnits = true;
                }else if(record.getMappingPositions()[0][0][0] >= endPosition) {
                    hasToCloseAccessUnits = true;
                }

                //An arbitrary decision is taken on the size of the access units: if the size increases above a given
                // size, new access units are also opened.
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
                    //Open new access units if the current ones are full or not relevant for current region
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

                        while (record.getMappingPositions()[0][0][0] >= endPosition) {
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
                numStuckedAt=0;
            } else {
                //We keep track of the number of instance we were not able to complete a record.
                numStuckedAt++;
                if(numStuckedAt > 1){
                    System.out.println(numStuckedAt+": "+entry.getKey()+" "+entry.getValue().toString());
                }
                if(numStuckedAt > 100){
                    System.exit(-1);
                }
                break;
            }
        }

        for(String readName : writtenRecords){
            //We remove all
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

        for(String readName : writtenRecords){
            alignments.remove(readName);
        }

        return accessUnitEncoders;
    }

    /**
     * Function to convert a bam file to bitstream. The reference being used is the one provided
     * @param inputBamPath The path of the BAM file to transcode
     * @param fastaReferencePath The path of the fasta reference to use
     * @param outputBsPath The output path to which the result of the transcoding should be written to.
     * @param useIlluminaReadIdentifierEncoder Indicates if the illumina read identifier encoder should be used or the
     *                                         general one.
     * @throws IOException
     * @throws DataFormatException
     */
    static void encode(
            String inputBamPath,
            String fastaReferencePath,
            String outputBsPath,
            boolean useIlluminaReadIdentifierEncoder
    ) throws IOException, DataFormatException {

        /*In order to access easily to the reference's nucleotides, the program uses the raw reference format of MPEGG.
        * The program thus tests if there is already this file available*/
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

        //Currently disabled: order by frequency (higher to lower) each of the quality symbols.
        //System.out.println("obtaining quality histogram");
        //byte[] qualityHistogram = QualityHistogram.obtain(inputBamPath);
        //System.out.println("obtained quality histogram");
        byte[] qualityHistogram = null;

        FileChannel output = FileChannel.open(
                Paths.get(outputBsPath), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        WritableMSBitChannel writer = new WritableMSBitChannel(output);

        //Currently hard coded to 2 as this allows to encode both cases (1 and 2 segments per template)
        byte number_template_segments = 2;

        //We use a buffered reader as this will allow to peak into the future and complete records faster
        BufferedBAMFileReader bufferedBAMFileReader = new BufferedBAMFileReader(
                2000000, new BAMFileReader(Paths.get(inputBamPath))
        );

        DataUnits dataUnits = new DataUnits();
        DataUnitParameters[] dataUnitParametersPerClass = new DataUnitParameters[5];
        for(byte data_class_i=0; data_class_i<5; data_class_i++){
            EncodingParameters encodingParameters = createEncodingParameters(
                    number_template_segments,
                    DATA_CLASS.getDataClass((byte) (data_class_i+1)),
                    qualityHistogram,
                    bufferedBAMFileReader.getBAMHeader().getGroupIds()
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

        System.out.println("Scanning for multiple alignments");
        BAMFileReader bamFileReaderForLedger = new BAMFileReader(Paths.get(inputBamPath));
        MultipleAlignmentsLedger multipleAlignmentsLedger = new MultipleAlignmentsLedger(bamFileReaderForLedger);
        System.out.println("Scanned");





        long auWidth = 6000000;

        Iterator<BAMRecord> bamIterator = bufferedBAMFileReader.iterator();
        int current_sequence_i = -1;
        long currentPosition = -1;
        short threshold = 5000;
        long numberReads = 0;

        AbstractAccessUnitEncoder[] accessUnitEncoders = null;
        LinkedHashMap<String, RecordConstructor> alignments = new LinkedHashMap<>();
        List<BAMRecord> previouslyReadUnmappedFromHalfMapped = new ArrayList<>();

        while (bamIterator.hasNext()) {
            /*The unmapped read of a half mapped record must be added second, however it could be appear first in the
            file. Therefore, unmapped records which cannot be combined are buffered, and now are added to there
            respective constructor if possible.*/
            ListIterator<BAMRecord> unmappedRecords_iter = previouslyReadUnmappedFromHalfMapped.listIterator();
            while (unmappedRecords_iter.hasNext()) {
                BAMRecord unmappedRecord = unmappedRecords_iter.next();
                if (alignments.get(unmappedRecord.getQName()) != null) {
                    alignments.get(unmappedRecord.getQName()).addSegment(unmappedRecord);
                    unmappedRecords_iter.remove();
                }
            }

            /*The next read is obtained from the file*/
            BAMRecord bamRecord = bamIterator.next();
            String readName = bamRecord.getQName();

            if (bamRecord.isUnmappedSegment()) {
                if (bamRecord.isNextSegmentUnmapped()) {
                    //This corresponds to a class U access unit which is not yet implemented.
                    continue;
                } else {
                    //If we had previously read the mapped mate, this record is already completed and marked as such
                    if (!previousSamePositionCompleted.contains(readName)) {
                        //we add the read to list to add to add to existing records.
                        previouslyReadUnmappedFromHalfMapped.add(bamRecord);
                        continue;
                    }
                }
            }

            int segment_i = bamRecord.isFirstSegment() ? 0 : 1;


            if (multipleAlignmentsLedger.isMultialigned(readName, segment_i)) {
                if (bamRecord.isSecondary()) {
                    GenomicPosition principalPosition =
                            multipleAlignmentsLedger.getPrincipalPosition(readName, segment_i);
                    boolean storableWithPrimary = principalPosition.getSequenceId().getSequenceIdentifier()
                            == bamRecord.getRefID();
                    if (!storableWithPrimary) {
                        RecordConstructor constructor = new RecordConstructor(
                                numberReads,
                                bamRecord.getQName(),
                                bamRecord.getGroup(),
                                bamRecord,
                                threshold
                        );
                        numberReads++;
                        List<Integer> positions = multipleAlignmentsLedger.getPositionsForSegment(
                                readName, segment_i, new SequenceIdentifier(bamRecord.getRefID()));

                        if (positions.get(0) != bamRecord.getPositionStart()) {
                            continue;
                        }

                        for (int position : positions) {
                            BAMRecord possibleOtherAlignment = bufferedBAMFileReader.searchByName(bamRecord.getQName(), bamRecord.getRefID(), position);
                            if(possibleOtherAlignment == null){
                                constructor.addSegment(possibleOtherAlignment);
                            }
                        }
                        alignments.put(readName, constructor);
                    }
                } else {
                    RecordConstructor constructor;
                    //We test if we have already a record constructor attached to this name
                    if( alignments.containsKey(readName)) {
                        constructor = alignments.get(readName);
                    } else {
                        constructor = new RecordConstructor(
                                numberReads,
                                bamRecord.getQName(),
                                bamRecord.getGroup(),
                                bamRecord,
                                threshold
                        );
                        numberReads++;
                        alignments.put(readName, constructor);
                    }


                    List<Integer> positions = multipleAlignmentsLedger.getPositionsForSegment(
                            readName, segment_i, new SequenceIdentifier(bamRecord.getRefID()));

                    for (int position : positions) {
                        BAMRecord possibleOtherAlignment = bufferedBAMFileReader.searchByName(bamRecord.getQName(), bamRecord.getRefID(), position);
                        if(possibleOtherAlignment == null){
                            constructor.addSegment(possibleOtherAlignment);
                        }
                    }

                }
            } else {
                //We test if we have already a record constructor attached to this name
                if (alignments.containsKey(readName)) {
                    alignments.get(readName).addSegment(bamRecord);
                } else {
                    //we must first verify if a new record should be created
                    if(previousSamePositionSequence == bamRecord.getRefID()
                            && previousSamePositionPosition == bamRecord.getPositionStart()
                            && previousSamePositionCompleted.contains(readName)){
                        /*A record with this name has already been completed for this name at this position, a new record
                        should not be created*/
                        continue;
                    }
                    if (
                            /*If the read has a position greater than his mate, and is within the threshold window,
                            * a new record shall not be created.*/
                            (bamRecord.getRefID() != bamRecord.getNextRefID() ||
                                    bamRecord.getPositionStart() <= bamRecord.getNextPositionStart() ||
                                    Math.abs(bamRecord.getNextPositionStart() - bamRecord.getPositionStart()) > threshold)
                            || (!bamRecord.isUnmappedSegment() && bamRecord.isNextSegmentUnmapped())) {

                        RecordConstructor constructor = new RecordConstructor(
                                numberReads,
                                bamRecord.getQName(),
                                bamRecord.getGroup(),
                                bamRecord,
                                threshold
                        );
                        numberReads++;

                        alignments.put(readName, constructor);

                        if(!bamRecord.hasMultipleSegments() || bamRecord.isNextSegmentUnmapped()){
                            /*If there are no more segments, or if the next segment is unmapped, we stop here as either
                            * it is complete, or it will be completed through another path */
                            continue;
                        }

                        //If the mate could be stored in the same record
                        if (
                                bamRecord.getRefID() == bamRecord.getNextRefID() &&
                                        (
                                                bamRecord.getPositionStart() < bamRecord.getNextPositionStart() ||
                                                (
                                                        bamRecord.getPositionStart() == bamRecord.getNextPositionStart()
                                                        && !previousSamePositionCompleted.contains(bamRecord.getQName())
                                                )
                                        ) &&
                                        Math.abs(bamRecord.getNextPositionStart() - bamRecord.getPositionStart()) < threshold
                        ) {
                            //If the information of the mate is close by or already buffered (otherwise the file will be
                            // read until found
                            if (bufferedBAMFileReader
                                    .getMaxDistanceToPosition(bamRecord.getNextRefID(), bamRecord.getNextPositionStart())
                                    < 10000000
                                || bufferedBAMFileReader.hasByName(
                                    bamRecord.getQName(),
                                    bamRecord.getNextRefID(),
                                    bamRecord.getNextPositionStart()
                                )
                            ) {
                                BAMRecord mateFound = bufferedBAMFileReader.searchByName(
                                        bamRecord.getQName(),
                                        bamRecord.getNextRefID(),
                                        bamRecord.getNextPositionStart());
                                if (mateFound == null) {
                                    System.out.println(bamRecord.getQName());
                                    throw new IllegalArgumentException();
                                }
                                constructor.addSegment(mateFound);

                                if(previousSamePositionSequence != bamRecord.getRefID()
                                        || previousSamePositionPosition != bamRecord.getPositionStart()){
                                    previousSamePositionSequence = bamRecord.getRefID();
                                    previousSamePositionPosition = bamRecord.getPositionStart();
                                    previousSamePositionCompleted.clear();
                                }
                                previousSamePositionCompleted.add(bamRecord.getQName());
                            }
                        }
                    }
                }

                SequenceIdentifier sequenceId = fromBAMSequenceIdentifierToMPEGGSequenceIdentifier(
                        bamRecord.getRefID(),
                        bufferedBAMFileReader.getBAMHeader(),
                        rawReference
                );

                //we request all records which could be completed to be written to file
                accessUnitEncoders = writeCompletedRecords(
                        accessUnitEncoders,
                        alignments,
                        writer,
                        dataUnits,
                        threshold,
                        rawReference,
                        auWidth,
                        dataUnitParametersPerClass,
                        useIlluminaReadIdentifierEncoder,
                        bufferedBAMFileReader.getBAMHeader(),
                        sequenceId,
                        bamRecord.getPositionStart() - 1
                );

            }
        }
        //we request all records to be written
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
                bufferedBAMFileReader.getBAMHeader()
        );
        output.close();
    }

    public static long getNumDiscardedRecords() {
        return numDiscardedRecords;
    }

    private static SequenceIdentifier fromBAMSequenceIdentifierToMPEGGSequenceIdentifier(
            int bamSequenceId,
            BAMHeader bamHeader,
            AbstractSequencesSource sequencesSource
    ){
        if (cachedSequenceIdentifiers == null){
            cachedSequenceIdentifiers = new SequenceIdentifier[bamHeader.getReferences().size()];
        } else {
            if(cachedSequenceIdentifiers[bamSequenceId] != null){
                return cachedSequenceIdentifiers[bamSequenceId];
            }
        }

        cachedSequenceIdentifiers[bamSequenceId] = sequencesSource.getSequenceIdentifier(
                bamHeader.getReference(bamSequenceId).name
        );
        return cachedSequenceIdentifiers[bamSequenceId];
    }
}

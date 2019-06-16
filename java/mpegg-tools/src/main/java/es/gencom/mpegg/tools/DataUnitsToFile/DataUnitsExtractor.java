package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.integration.fasta.FastaFileReader;
import es.gencom.integration.fasta.FastaSequence;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.format.ref.*;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;
import es.gencom.mpegg.coder.dataunits.DataUnitRawReference;
import es.gencom.mpegg.coder.dataunits.DataUnits;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.*;

public class DataUnitsExtractor {
    private final DataUnits dataUnits;
    private final HashMap<Short, DataUnitParameters> parametersMap;
    private final Set<Short> writtenParametersSet;
    private final Set<SequenceIdentifier> requiredSequences;
    private final Reference reference;

    public DataUnitsExtractor(Reference reference){
        dataUnits = new DataUnits();
        parametersMap = new HashMap<>();
        writtenParametersSet = new HashSet<>();
        requiredSequences = new TreeSet<>();
        this.reference = reference;
    }

    public void addDataUnitAccessUnit(DataUnitAccessUnit dataUnitAccessUnit) {
        dataUnits.addDataUnit(dataUnitAccessUnit);
        if(dataUnitAccessUnit.getAUType() != DATA_CLASS.CLASS_U) {
            requiredSequences.add(dataUnitAccessUnit.getHeader().getSequence_ID());
        }
    }

    public void addDataUnitParameters(DataUnitParameters dataUnitParameters) {
        parametersMap.put(dataUnitParameters.getParameter_set_ID(), dataUnitParameters);
    }

    private void addParameter(DataUnitParameters dataUnitParameters){
        if(dataUnitParameters.getParameter_set_ID() != dataUnitParameters.getParent_parameter_set_ID()){
            if(!writtenParametersSet.contains(dataUnitParameters.getParent_parameter_set_ID())){
                addParameter(parametersMap.get(dataUnitParameters.getParent_parameter_set_ID()));
            }
        }
        dataUnits.addDataUnitParameters(dataUnitParameters);
        writtenParametersSet.add(dataUnitParameters.getParameter_set_ID());
    }

    public DataUnits getDataUnits(){
        return dataUnits;
    }

    public DataUnits constructDataUnits() throws IOException {

        DataUnitRawReference rawReference;
        if(reference.getLocation() instanceof InternalLocation){
            throw new IllegalArgumentException();
        } else {
            ExternalLocation externalLocation = (ExternalLocation)reference.getLocation();
            if(externalLocation.getReference_type() == REFERENCE_TYPE.MPEGG_REF){
                throw new IllegalArgumentException();
            } else if (externalLocation.getReference_type() == REFERENCE_TYPE.RAW_REF){

                MPEGReader readerOriginal = new ReadableMSBitFileChannel(
                        FileChannel.open(Paths.get(externalLocation.getRef_uri()))
                );
                DataUnitRawReference originalReference = DataUnitRawReference.read(
                        readerOriginal,
                        dataUnits
                );


                int[] requiredSequencesCasted = new int[requiredSequences.size()];
                int required_i = 0;
                for(SequenceIdentifier value : requiredSequences){
                    requiredSequencesCasted[required_i] = value.getSequenceIdentifier();
                    required_i++;
                }
                rawReference = originalReference.selectSubset(requiredSequencesCasted);
            } else {
                rawReference = FASTAToRawReference(reference, requiredSequences);
            }
        }
        dataUnits.setDataUnitRawReference(rawReference);
        for(Map.Entry<Short, DataUnitParameters> parametersEntry : parametersMap.entrySet()){
            addParameter(parametersEntry.getValue());
        }

        return dataUnits;
    }

    private DataUnitRawReference FASTAToRawReference(
            Reference reference,
            Set<SequenceIdentifier> requiredSequences
    ) throws IOException {
        FastaFileReader fastaReader = new FastaFileReader(
                Paths.get(((ExternalLocation) reference.getLocation()).getRef_uri())
        );

        int[] sequence_ids = new int[requiredSequences.size()];
        Payload[] sequences = new Payload[requiredSequences.size()];
        long[] startPos = new long[requiredSequences.size()];
        long[] endPos = new long[requiredSequences.size()];
        int allocated_sequences = 0;
        int sequence_id = 0;
        for(FastaSequence fastaSequence : fastaReader){
            SequenceIdentifier sequenceIdentifier = new SequenceIdentifier(sequence_id);

            if(requiredSequences.contains(sequenceIdentifier)){
                sequence_ids[allocated_sequences]= sequence_id;
                sequences[allocated_sequences] = new Payload(fastaSequence.sequence);
                startPos[allocated_sequences] = 0;
                endPos[allocated_sequences] = fastaSequence.sequence.length;
                allocated_sequences++;
            }else{
                System.out.println("sequence discarded");
            }
            sequence_id++;
        }

        return new DataUnitRawReference(
            dataUnits,
            sequence_ids,
            sequences,
            startPos,
            endPos
        );
    }
}
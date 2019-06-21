package es.gencom.mpegg.tools;

import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.format.ref.*;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;
import es.gencom.mpegg.coder.dataunits.DataUnitRawReference;
import es.gencom.mpegg.coder.dataunits.DataUnits;

import java.io.IOException;
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
        DataUnitRawReference rawReference = FormatReferenceToRawReference.convert(reference);

        int[] requiredSequencesCasted = new int[requiredSequences.size()];
        int required_i = 0;
        for(SequenceIdentifier value : requiredSequences){
            requiredSequencesCasted[required_i] = value.getSequenceIdentifier();
            required_i++;
        }

        rawReference = rawReference.selectSubset(requiredSequencesCasted);
        dataUnits.setDataUnitRawReference(rawReference);
        for(Map.Entry<Short, DataUnitParameters> parametersEntry : parametersMap.entrySet()){
            addParameter(parametersEntry.getValue());
        }

        return dataUnits;
    }
}
package es.gencom.mpegg.tools;

import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.format.ref.*;
import es.gencom.mpegg.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.dataunits.DataUnitParameters;
import es.gencom.mpegg.dataunits.DataUnitRawReference;
import es.gencom.mpegg.dataunits.DataUnits;

import java.io.IOException;
import java.util.*;

public class DataUnitsExtractor {
    private final DataUnits dataUnits;
    private final HashMap<Short, DataUnitParameters> parametersMap;
    private final Set<Short> writtenParametersSet;
    private final Set<SequenceIdentifier> requiredSequences;
    private final Reference reference;

    public DataUnitsExtractor(Reference reference) {
        dataUnits = new DataUnits();
        parametersMap = new HashMap<>();
        writtenParametersSet = new HashSet<>();
        requiredSequences = new TreeSet<>();
        this.reference = reference;
    }

    public void addDataUnitAccessUnit(DataUnitAccessUnit dataUnitAccessUnit) {
        dataUnits.addDataUnit(dataUnitAccessUnit);
        if(dataUnitAccessUnit.getAUType() != DATA_CLASS.CLASS_U) {
            requiredSequences.add(dataUnitAccessUnit.header.sequence_id);
        }
    }

    public void addDataUnitParameters(DataUnitParameters dataUnitParameters) {
        parametersMap.put(dataUnitParameters.parameter_set_id, dataUnitParameters);
    }

    private void addParameter(DataUnitParameters dataUnitParameters) {
        if(dataUnitParameters.parameter_set_id != dataUnitParameters.parent_parameter_set_id) {
            if(!writtenParametersSet.contains(dataUnitParameters.parent_parameter_set_id)) {
                addParameter(parametersMap.get(dataUnitParameters.parent_parameter_set_id));
            }
        }
        dataUnits.addDataUnitParameters(dataUnitParameters);
        writtenParametersSet.add(dataUnitParameters.parameter_set_id);
    }

    public DataUnits getDataUnits() {
        return dataUnits;
    }

    public DataUnits constructDataUnits() throws IOException {
        if(reference != null && reference.isExternalReference()) {
            DataUnitRawReference rawReference = FormatReferenceToRawReference.convert((ExternalReference)reference);

            int[] requiredSequencesCasted = new int[requiredSequences.size()];
            int required_i = 0;
            for (SequenceIdentifier value : requiredSequences) {
                requiredSequencesCasted[required_i] = value.getSequenceIdentifier();
                required_i++;
            }

            if(rawReference != null) {
                rawReference = rawReference.selectSubset(requiredSequencesCasted);
                dataUnits.setDataUnitRawReference(rawReference);
            }
        }
        Set<Short> parametersRequired = new TreeSet<>();
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()) {
            parametersRequired.add(dataUnitAccessUnit.header.parameter_set_id);
            markParametersParentAsRequired(
                    parametersMap.get(dataUnitAccessUnit.header.parameter_set_id),
                                      parametersRequired);
        }

        for(Map.Entry<Short, DataUnitParameters> parametersEntry : parametersMap.entrySet()) {
            if(parametersRequired.contains(parametersEntry.getKey())) {
                addParameter(parametersEntry.getValue());
            }
        }

        return dataUnits;
    }

    private void markParametersParentAsRequired(
            DataUnitParameters dataUnitParameters,
            Set<Short> parametersRequired) {
        
        if(dataUnitParameters.parameter_set_id != dataUnitParameters.parent_parameter_set_id) {
            parametersRequired.add(dataUnitParameters.parent_parameter_set_id);
            DataUnitParameters parentDataUnitParameters = 
                    parametersMap.get(dataUnitParameters.parent_parameter_set_id);

            markParametersParentAsRequired(parentDataUnitParameters, parametersRequired);
        }
    }
}
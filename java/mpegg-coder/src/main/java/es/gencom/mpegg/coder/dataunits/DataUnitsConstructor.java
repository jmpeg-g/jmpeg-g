/**
 * *****************************************************************************
 * Copyright (C) 2019 Spanish National Bioinformatics Institute (INB) and
 * Barcelona Supercomputing Center
 *
 * Modifications to the initial code base are copyright of their respective
 * authors, or their employers as appropriate.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *****************************************************************************
 */

package es.gencom.mpegg.coder.dataunits;

import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.format.ref.*;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.*;

public class DataUnitsConstructor {
    private final DataUnits dataUnits;
    private final HashMap<Short, DataUnitParameters> parametersMap;
    private final Set<Short> writtenParametersSet;
    private final Set<SequenceIdentifier> requiredSequences;
    private final Reference reference;

    public DataUnitsConstructor(Reference reference){
        dataUnits = new DataUnits();
        parametersMap = new HashMap<>();
        writtenParametersSet = new HashSet<>();
        requiredSequences = new HashSet<>();
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
                }
                rawReference = originalReference.selectSubset(requiredSequencesCasted);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        dataUnits.setDataUnitRawReference(rawReference);
        for(Map.Entry<Short, DataUnitParameters> parametersEntry : parametersMap.entrySet()){
            addParameter(parametersEntry.getValue());
        }

        return dataUnits;
    }
}

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

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.EOFException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataUnits {
    private DataUnitRawReference dataUnitRawReference;

    private final List<DataUnitParameters> dataUnitParametersList;
    private final List<DataUnitAccessUnit> dataUnits;

    public DataUnits() {
        dataUnitParametersList = new ArrayList<>();
        dataUnits = new ArrayList<>();
    }

    public void setDataUnitRawReference(
            final DataUnitRawReference dataUnitRawReference) {

        this.dataUnitRawReference = dataUnitRawReference;
    }

    public void addDataUnitParameters(DataUnitParameters dataUnitParameters) {
        dataUnitParametersList.add(dataUnitParameters);
    }

    public void addDataUnit(final DataUnitAccessUnit dataUnit) {
        dataUnits.add(dataUnit);
    }

    public void write(final MPEGWriter writer) throws IOException {
        write(writer, writer);
    }

    public void write(final MPEGWriter writer, final MPEGWriter writerRawRef) throws IOException {
        for(AbstractDataUnit dataUnitParameter : dataUnitParametersList){
            dataUnitParameter.write(writer);
        }

        if(dataUnitRawReference != null) {
            dataUnitRawReference.write(writerRawRef);
        }

        for(AbstractDataUnit dataUnit : dataUnits) {
            dataUnit.write(writer);
        }
    }

    public List<DataUnitAccessUnit> getDataUnitAccessUnits() {
        return dataUnits;
    }


    public DataUnitParameters getParameter(final int id){
        for(DataUnitParameters dataUnitParameters: dataUnitParametersList){
            if(dataUnitParameters.getParameter_set_ID() == id){
                return dataUnitParameters;
            }
        }
        throw new IllegalArgumentException();
    }

    public int getNumberDataUnits(){
        return dataUnits.size();
    }

    public DataUnitAccessUnit getDataUnitAccessUnit(final int index){
        return dataUnits.get(index);
    }
    
    public static DataUnits read(final MPEGReader reader) throws IOException {
        final DataUnits dataUnits = new DataUnits();

        while (true) {
            try {
                final DATAUNIT_TYPE_ID dataunit_type_id = DATAUNIT_TYPE_ID.read(reader);
                switch(dataunit_type_id) {
                    case RAW_REF: dataUnits.setDataUnitRawReference(DataUnitRawReference.read(reader, dataUnits));
                    case PARAMS: dataUnits.addDataUnitParameters(DataUnitParameters.read(reader, dataUnits));
                                 break;
                    case AU:     dataUnits.addDataUnit(DataUnitAccessUnit.read(reader, dataUnits));
                                 break;
                }
            } catch(EOFException ex) {
                 break;
            }
        }

        return dataUnits;
    }

    public Iterable<DataUnitParameters> getParameters() {
        return dataUnitParametersList;
    }

    public DataUnitRawReference getDataUnitRawReference() {
        return dataUnitRawReference;
    }
}

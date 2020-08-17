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

package es.gencom.mpegg.dataunits;

import es.gencom.mpegg.coder.quality.AbstractQualityValueParameterSet;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DatasetType;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;

public class DataUnitParameters extends AbstractDataUnit {

    public final short parent_parameter_set_id;
    public final short parameter_set_id;

    private final EncodingParameters encodingParameters;

    public DataUnitParameters(
            final short parent_parameter_set_id,
            final short parameter_set_id,
            final EncodingParameters encodingParameters,
            final DataUnits dataUnits) {

        super(DATAUNIT_TYPE_ID.PARAMS, dataUnits);

        this.parent_parameter_set_id = parent_parameter_set_id;
        this.parameter_set_id = parameter_set_id;
        this.encodingParameters = encodingParameters;
    }

    public static DataUnitParameters read(
            final MPEGReader reader, 
            final DataUnits dataUnits) throws IOException {

        final long data_unit_size = reader.readUnsignedInt();
        if(data_unit_size<0 || data_unit_size> Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }

        short readParentParameterSetId = (short) reader.readBits(8);
        short readParameterSetId = (short) reader.readBits(8);
        //Payload payload = reader.getDescriptorSpecificData(data_unit_size-3);
        EncodingParameters encodingParameters = new EncodingParameters();
        encodingParameters.read(reader);
        return new DataUnitParameters(
                readParentParameterSetId,
                readParameterSetId,
                encodingParameters,
                dataUnits
        );
    }

    protected void writeDataUnitContent(MPEGWriter writer) throws IOException{
        writer.writeBits(0, 10);
        writer.writeBits(5+2+encodingParameters.size(), 22);
        writer.writeBits(parent_parameter_set_id, 8);
        writer.writeBits(parameter_set_id, 8);
        encodingParameters.write(writer);
        writer.flush();
    }

    public long getReadLength(){
        return encodingParameters.getReadsLength();
    }

    public DatasetType getDatasetType(){
        return encodingParameters.getDatasetType();
    }

    public boolean isPosSize40() {
        return encodingParameters.isPos40Bits();
    }

    public boolean isMultiple_alignments_flag(){
        return encodingParameters.isMultiple_alignments_flag();
    }

    public long getMultiple_signature_base() {
        return encodingParameters.getMultiple_signature_base();
    }

    public AbstractQualityValueParameterSet getQualityValueParameterSet(DATA_CLASS dataUnitClass) {
        return encodingParameters.getQualityValueParameterSet(dataUnitClass);
    }

    public byte getNumberTemplateSegments() {
        return encodingParameters.getNumberOfTemplateSegments();
    }

    public boolean isSplicedReadsFlag() {
        return encodingParameters.isSpliced_reads_flag();
    }

    public final EncodingParameters getEncodingParameters() {
        return encodingParameters;
    }
}

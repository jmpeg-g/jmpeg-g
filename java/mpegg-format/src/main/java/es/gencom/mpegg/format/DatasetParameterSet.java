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

package es.gencom.mpegg.format;

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro 
 */

public class DatasetParameterSet extends GenInfo<DatasetParameterSet> {
    
    public final static String KEY = "pars";

    private short dataset_group_id;
    private int dataset_id;
    private short parameter_set_ID;
    private short parent_parameter_set_ID;
    private ByteBuffer parameters;
    
    public DatasetParameterSet() {
        super(KEY);
    }

    public DatasetParameterSet(
            byte dataset_group_id,
            short dataset_id,
            short parameter_set_ID,
            short parent_parameter_set_ID,
            ByteBuffer parameters
    ) {
        this();
        this.dataset_group_id = dataset_group_id;
        this.dataset_id = dataset_id;
        this.parameter_set_ID = parameter_set_ID;
        this.parent_parameter_set_ID = parent_parameter_set_ID;
        this.parameters = parameters;
    }

    public short getDatasetGroupId() {
        return dataset_group_id;
    }

    public int getDatasetId() {
        return dataset_id;
    }


    public ByteBuffer getParameters() {
        return parameters;
    }

    public byte[] getParametersArray(){
        parameters.rewind();
        byte array[] = new byte[parameters.remaining()];
        parameters.get(array);
        return array;
    }

    @Override
    public void write(MPEGWriter writer) throws IOException {
        writer.writeByte((byte) dataset_group_id);
        writer.writeShort((short) dataset_id);
        writer.writeByte((byte)parameter_set_ID);
        writer.writeByte((byte)parent_parameter_set_ID);
        parameters.rewind();
        writer.writeByteBuffer(parameters);
    }

    @Override
    public DatasetParameterSet read(MPEGReader reader, final long size) throws IOException, ParsedSizeMismatchException {
        dataset_group_id = reader.readByte();
        dataset_id = reader.readShort();
        parameter_set_ID = reader.readByte();
        parent_parameter_set_ID = reader.readByte();

        parameters = reader.readByteBuffer((int) size-5);

        if(size != size()){
            throw new ParsedSizeMismatchException();
        }
        return this;
    }

    @Override
    public long size(){
        long result = 0;
        result += 1; //dataset_group_id
        result += 2; //dataset_id
        result += 1; //parameter_set_ID
        result += 1; //parent_parameter_set_ID
        result += parameters.limit();
        return result;
    }

    public short getParameter_set_ID() {
        return parameter_set_ID;
    }

    public short getParent_parameter_set_ID() {
        return parent_parameter_set_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatasetParameterSet)) return false;
        DatasetParameterSet that = (DatasetParameterSet) o;
        return getDatasetGroupId() == that.getDatasetGroupId() &&
                getDatasetId() == that.getDatasetId() &&
                getParameter_set_ID() == that.getParameter_set_ID() &&
                getParent_parameter_set_ID() == that.getParent_parameter_set_ID() &&
                Arrays.equals(getParametersArray(), that.getParametersArray());
    }
}

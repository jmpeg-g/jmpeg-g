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

package es.gencom.mpegg.format.ref;

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;

public class InternalLocation extends AbstractLocation {
    private byte dataset_group_id;
    private short dataset_id;

    public InternalLocation() {
    }

    public InternalLocation(byte dataset_group_id, short dataset_id) {
        this.dataset_group_id = dataset_group_id;
        this.dataset_id = dataset_id;
    }

    @Override
    public void read(final MPEGReader reader, final int numberSequences) throws IOException {
        dataset_group_id = (byte) reader.readBits(8);
        dataset_id = reader.readShort();
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeByte((byte) 0);//external
        writer.writeByte(dataset_group_id);
        writer.writeShort(dataset_id);
    }

    @Override
    public long size() {
        return 1+1+2;//1:external flag, 1: dataset group Id + 2 datasetId
    }

    public byte getDatasetGroupId() {
        return dataset_group_id;
    }

    public void setDatasetGroupId(final byte dataset_group_id) {
        this.dataset_group_id = dataset_group_id;
    }

    public short getDatasetId() {
        return dataset_id;
    }

    public void setDatasetId(final short dataset_id) {
        this.dataset_id = dataset_id;
    }
}
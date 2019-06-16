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

import es.gencom.mpegg.format.ChecksumAlgorithm;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;
import java.util.Arrays;

class MPEGG_ExtRef_info extends Abstract_ExtRef_info {
    private byte dataset_group_id;
    private short dataset_id;
    private byte[] checksum;

    MPEGG_ExtRef_info() {}
    
    MPEGG_ExtRef_info(
            final byte dataset_group_id,
            final short dataset_id,
            final byte[] checksum) {

        this.dataset_group_id = dataset_group_id;
        this.dataset_id = dataset_id;
        this.checksum = Arrays.copyOf(checksum, checksum.length);
    }

    @Override
    void write(final MPEGWriter writer) throws IOException {
        writer.writeByte(dataset_group_id);
        writer.writeShort(dataset_id);
        writer.writeBytes(checksum);
    }

    @Override
    void read(
            final MPEGReader reader, 
            final ChecksumAlgorithm algorithm, 
            final int numberSequences) throws IOException {

        dataset_group_id = reader.readByte();
        dataset_id = reader.readShort();
        checksum= new byte[algorithm.size/8];
        reader.readByteBuffer(algorithm.size/8).get(checksum);
    }

    @Override
    long getSize() {
        return 1 //dataset_group_id
                + 2 //dataset_id
                + checksum.length;
    }

    public byte getDatasetGroupId() {
        return dataset_group_id;
    }

    public short getDataset_id() {
        return dataset_id;
    }

    public byte[] getChecksum() {
        return checksum;
    }
}
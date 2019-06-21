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
import java.nio.ByteBuffer;

public class RawOrFasta_ExtRef_info extends Abstract_ExtRef_info{
    byte[][] checksums;

    RawOrFasta_ExtRef_info() {}
    RawOrFasta_ExtRef_info(final byte[][] checksums) {
        this.checksums = checksums;
    }


    @Override
    void write(final MPEGWriter writer) throws IOException {
        for(int seq_i=0; seq_i < checksums.length; seq_i++){
            writer.writeBytes(checksums[seq_i]);
        }
    }

    @Override
    void read(final MPEGReader reader, 
              final ChecksumAlgorithm algorithm, 
              final int numberSequences) throws IOException {

        checksums = new byte[numberSequences][];
        for (int i = 0; i < numberSequences; i++) {
            checksums[i]= new byte[algorithm.size/8];
            ByteBuffer algorithmChecksumValue = reader.readByteBuffer(algorithm.size/8);
            algorithmChecksumValue.position(0);
            algorithmChecksumValue.get(checksums[i]);
        }
    }

    @Override
    long getSize() {
        long size = 0;
        for(int seq_i=0; seq_i < checksums.length; seq_i++){
            size += checksums[seq_i].length;
        }
        return size;
    }

    public byte[] getChecksum(int sequence_index){
        return checksums[sequence_index];
    }
}

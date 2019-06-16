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
import es.gencom.mpegg.io.WritableMSBitChannel;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * Describes a Block Header (ISO/IEC DIS 23092-1 6.4.4.1)
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class BlockHeader implements Content {
    /**
     * Unambiguously identifies the Descriptor, as specified in ISO/IEC 23092-2.
     */
    private byte descriptor_id;     // u(7)
    /**
     * The number of bytes composing the Block payload.
     */
    private long block_payload_size; // u(32)

    public BlockHeader() {}

    public BlockHeader(
            final byte descriptor_id, 
            final long block_payload_size) {

        this.descriptor_id = descriptor_id;
        this.block_payload_size = block_payload_size;
    }
    
    public byte getDescriptorId() {
        return descriptor_id;
    }

    long getBlockPayloadSize() {
        return block_payload_size;
    }

    public long size() {
        //reserved(1) + descriptor_Id(7) + block_payload_size(32) = 40 bits
        return 5;
    }

    @Override
    public void write(final WritableByteChannel channel) throws IOException {
        write(new WritableMSBitChannel(channel));
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBoolean(false);
        writer.writeBits(descriptor_id, 7);
        writer.writeBits(block_payload_size, 32);
        writer.flush();
    }

    public void read(final MPEGReader reader) throws IOException {
        reader.readBoolean();
        descriptor_id = (byte)reader.readBits(7);
        block_payload_size = reader.readUnsignedInt();
    }
}

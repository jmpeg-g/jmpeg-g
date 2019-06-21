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

import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.io.WritableMSBitChannel;
import es.gencom.mpegg.io.MPEGReader;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * <p>
 * Block of data (ISO/IEC DIS 23092-1 6.4.4 Block).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class Block implements Content {
    
    private final DatasetHeader dataset_header;
    
    private BlockHeader block_header;
    private Payload payload;
    
    public Block(final DatasetHeader dataset_header) {
        this.dataset_header = dataset_header;
    }

    public void setBlockHeader(final BlockHeader block_header){
        this.block_header = block_header;
    }

    public BlockHeader getBlockHeader() {
        return block_header;
    }

    public void setPayload(final Payload payload){
        this.payload = payload;
    }

    public Payload getPayload(){
        return payload;
    }

    public long size() {
        long header_size = 0;
        if (dataset_header.isBlockHeader()) {
            if (block_header != null) {
                header_size += block_header.size();
            } else {
                throw new IllegalArgumentException();
            }
        }

        return header_size + payload.size();
    }

    @Override
    public void write(final WritableByteChannel channel) throws IOException {
        write(new WritableMSBitChannel(channel));
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        if (dataset_header.isBlockHeader()) {
            if (block_header != null) {
                block_header.write(writer);
            } else {
                throw new IllegalArgumentException();
            }
        }

        payload.rewind();
        writer.writePayload(payload);
    }

    public Block read(final MPEGReader reader) throws IOException {
        block_header = new BlockHeader();
        block_header.read(reader);

        payload = reader.readPayload(block_header.getBlockPayloadSize());

        return this;
    }
}

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
import es.gencom.mpegg.io.Payload;

import java.io.IOException;

public class DescriptorStreamContainer extends GenInfo<DescriptorStreamContainer>{
    public final static String KEY = "dscn";

    private DescriptorStreamHeader descriptor_stream_header;
    private Payload payload;
    private long streamByteOffset;
    private long streamSize;
    private long startDataset;

    public DescriptorStreamContainer() {
        super(KEY);

        descriptor_stream_header = new DescriptorStreamHeader();
    }

    public void setDescriptorStreamHeader(
            final DescriptorStreamHeader descriptorStreamHeader) {
        descriptor_stream_header = descriptorStreamHeader;
    }

    public void setPayload(final Payload payload){
        this.payload = payload;
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException, InvalidMPEGStructureException {
        if(descriptor_stream_header == null){
            throw new InvalidMPEGStructureException("Missing mandatory descriptor stream header.");
        }
        descriptor_stream_header.writeWithHeader(writer);
        payload.rewind();
        writer.writePayload(payload);
    }

    @Override
    public DescriptorStreamContainer read(final MPEGReader reader, final long size) 
            throws IOException, InvalidMPEGStructureException, ParsedSizeMismatchException {

        final Header header = Header.read(reader);
        if(!descriptor_stream_header.key.equals(header.key)) {
            throw new InvalidMPEGStructureException("The expected descriptor stream header is not found");
        }
        descriptor_stream_header.read(reader, header.getContentSize());
        final long descriptorStreamHeaderSize = descriptor_stream_header.sizeWithHeader();

        streamByteOffset = reader.getPosition();
        streamSize = size - descriptorStreamHeaderSize;
        payload = reader.readPayload(streamSize);

        if(size != size()) {
            throw new ParsedSizeMismatchException("Stream container has not the indicated size.");
        }

        return this;
    }

    @Override
    public long size() {
        long result = descriptor_stream_header.sizeWithHeader();
        payload.rewind();
        result += payload.remaining();
        return result;
    }

    public DescriptorStreamHeader getDescriptorStreamHeader() {
        return descriptor_stream_header;
    }

    public Payload getPayloadFromTo(long blockStart, long blockEnd) throws IOException {
        long currentPosition = payload.getPosition();

        long correctedStartPosition = blockStart - (streamByteOffset - startDataset);
        long size = blockEnd-blockStart;

        payload.position(correctedStartPosition);
        Payload result = payload.readPayload(size);

        payload.position(currentPosition);
        return result;
    }

    public Payload getPayloadFromToEnd(long blockStart) throws IOException {
        return getPayloadFromTo(
                blockStart,
                streamSize + streamByteOffset - startDataset
        );
    }

    public void setStartDataset(long startDataset) {
        this.startDataset = startDataset;
    }
}

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

/**
 * <p>
 * Metadata associated to the reference (ISO/IEC DIS 23092-1 6.4.1.3 Reference Metadata).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class ReferenceMetadata extends GenInfo<ReferenceMetadata> {
    
    public final static String KEY = "rfmd";
    
    private byte dataset_group_id;               // u(8)
    private byte reference_id;                   // u(8)
    private ByteBuffer reference_metadata_value;
            
    public ReferenceMetadata() {
        super(KEY);
    }

    public ReferenceMetadata(
            final byte datasetGroupId, 
            final byte referenceId, 
            final byte[] payload) {

        super(KEY);

        dataset_group_id = datasetGroupId;
        reference_id = referenceId;
        reference_metadata_value = ByteBuffer.wrap(payload);
    }

    public ReferenceMetadata(final ByteBuffer reference_metadata_value) {
        super(KEY);

        this.reference_metadata_value = reference_metadata_value;
    }

    public byte getDatasetGroupId() {
        return dataset_group_id;
    }

    public byte getReferenceId() {
        return reference_id;
    }
    
    public void setReferenceId(final byte reference_id) {
        this.reference_id = reference_id;
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(dataset_group_id, 8);
        writer.writeBits(reference_id, 8);
        reference_metadata_value.position(0);
        writer.writeByteBuffer(reference_metadata_value);
    }
    
    @Override
    public ReferenceMetadata read(final MPEGReader reader, final long size) throws IOException {
        dataset_group_id = (byte)reader.readBits(8);
        reference_id = (byte)reader.readBits(8);
        reference_metadata_value = reader.readByteBuffer((int)(size-2));
        return this;
    }

    public ByteBuffer getValue() {
        ByteBuffer slicedBuffer = reference_metadata_value.slice();
        slicedBuffer.position(0);
        return slicedBuffer;
    }

    public int getValueSize() {
        return reference_metadata_value.capacity();
    }
}

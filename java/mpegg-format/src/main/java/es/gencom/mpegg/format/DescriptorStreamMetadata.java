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
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DescriptorStreamMetadata extends GenInfo<DescriptorStreamMetadata> {

    public final static String KEY = "aupr";

    private ByteBuffer ds_metadata_value;

    public DescriptorStreamMetadata() {
        super(KEY);
    }

    public DescriptorStreamMetadata(final ByteBuffer ds_metadata_value) {
        super(KEY);

        this.ds_metadata_value = ds_metadata_value;
    }

    public ByteBuffer getValue() {
        return ds_metadata_value;
    }

    public void setValue(final ByteBuffer dg_metadata_value) {
        this.ds_metadata_value = dg_metadata_value;
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeByteBuffer(ds_metadata_value);
    }

    @Override
    public DescriptorStreamMetadata read(final MPEGReader reader, final long size) throws IOException {
        ds_metadata_value = reader.readByteBuffer((int)size);
        return this;
    }
}

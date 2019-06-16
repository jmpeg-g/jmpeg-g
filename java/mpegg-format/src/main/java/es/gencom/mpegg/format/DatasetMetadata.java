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
import java.io.IOException;
import es.gencom.mpegg.io.MPEGWriter;
import java.nio.ByteBuffer;

/**
 * Metadata associated to the Dataset (ISO/IEC DIS 23092-1 6.4.2.3 Dataset Metadata).
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DatasetMetadata extends GenInfo<DatasetMetadata> {
    
    public final static String KEY = "dtmd";
    
    private ByteBuffer dt_metadata_value;

    public DatasetMetadata() {
        super(KEY);
    }

    public DatasetMetadata(final ByteBuffer dt_metadata_value) {
        super(KEY);

        this.dt_metadata_value = dt_metadata_value;
    }
    
    public ByteBuffer getValue() {
        return dt_metadata_value;
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeByteBuffer(dt_metadata_value);
    }

    @Override
    public DatasetMetadata read(final MPEGReader reader, final long size) throws IOException {
        dt_metadata_value = reader.readByteBuffer((int)size);
        return this;
    }
}

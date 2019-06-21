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

public class DatasetProtection extends GenInfo<DatasetProtection> {
    
    public final static String KEY = "dtpr";

    private ByteBuffer dt_protection_value;
    
    public DatasetProtection() {
        super(KEY);
    }

    public ByteBuffer getValue() {
        return dt_protection_value;
    }

    public void setValue(final ByteBuffer dt_protection_value) {
        this.dt_protection_value = dt_protection_value;
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeByteBuffer(dt_protection_value);
    }

    @Override
    public DatasetProtection read(final MPEGReader reader, long size) throws IOException {
        dt_protection_value = reader.readByteBuffer((int) size);
        return this;
    }
}
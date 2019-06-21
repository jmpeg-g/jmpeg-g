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
import java.nio.ByteBuffer;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DescriptorStreamProtection extends GenInfo<DescriptorStreamProtection> {

    public final static String KEY = "aupr";

    private ByteBuffer ds_protection_value;

    public DescriptorStreamProtection() {
        super(KEY);
    }

    public DescriptorStreamProtection(final ByteBuffer ds_protection_value) {
        super(KEY);

        this.ds_protection_value = ds_protection_value;
    }

    public ByteBuffer getValue() {
        return ds_protection_value;
    }

    public void setValue(final ByteBuffer dg_metadata_value) {
        this.ds_protection_value = dg_metadata_value;
    }

    boolean isDescriptorStreamHeaderEncrypted() {
        return false;
    }

    boolean areDescriptorStreamBlocksEncrypted() {
        return false;
    }

    boolean isDescriptorStreamHeaderSigned() {
        return false;
    }

    boolean areDescriptorStreamBlocksSigned() {
        return false;
    }

    Payload decryptDescriptorStreamHeader() {
        throw new UnsupportedOperationException();
    }

    Payload decryptDescriptorStreamBlocks() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeByteBuffer(ds_protection_value);
    }

    @Override
    public DescriptorStreamProtection read(final MPEGReader reader, final long size) throws IOException {
        ds_protection_value = reader.readByteBuffer((int)size);
        return this;
    }
}

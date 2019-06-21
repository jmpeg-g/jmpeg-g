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

public class DescriptorStreamHeader extends GenInfo<DescriptorStreamHeader>{
    
    public final static String KEY = "dshd";

    private static final long FIXED_SIZE = 6;

    private byte descriptor_id;
    private DATA_CLASS class_id;
    private int num_blocks;

    public DescriptorStreamHeader() {
        super(KEY);
    }

    public DescriptorStreamHeader(
            final byte descriptor_id, 
            final DATA_CLASS class_id, 
            final int num_blocks) {

        super(KEY);
        
        this.descriptor_id = descriptor_id;
        this.class_id = class_id;
        this.num_blocks = num_blocks;
    }

    public byte getDescriptorID() {
        return descriptor_id;
    }

    public DATA_CLASS getClassID() {
        return class_id;
    }

    public int getNumBlocks() {
        return num_blocks;
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBoolean(false);
        writer.writeBits(descriptor_id, 7);
        writer.writeBits(class_id.ID,4);
        writer.writeInt(num_blocks);
        writer.align();
    }

    @Override
    public DescriptorStreamHeader read(final MPEGReader reader, final long size) 
            throws IOException, ParsedSizeMismatchException {

        reader.readBoolean();
        descriptor_id = (byte) reader.readBits(7);
        class_id = DATA_CLASS.getDataClass((byte) reader.readBits(4));
        num_blocks = reader.readInt();
        reader.align();

        if(size != size()) {
            throw new ParsedSizeMismatchException("Descriptor stream header has not the indicated size");
        }
        return this;
    }

    @Override
    public long size() {
        return getFixedSize();
    }

    public static long getFixedSize() {
        return FIXED_SIZE;
    }
}

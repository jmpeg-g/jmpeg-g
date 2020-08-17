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

import es.gencom.mpegg.format.InvalidMPEGGFileException;
import es.gencom.mpegg.format.InvalidMPEGStructureException;
import es.gencom.mpegg.format.ParsedSizeMismatchException;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * Description for the Reference embedded into a Dataset (external_ref_flag = 0).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DatasetReference extends Reference {
    
    private byte internal_dataset_group_id;
    private short internal_dataset_id;
    
    public DatasetReference(
            final byte dataset_group_id,
            final byte reference_id,
            final String reference_name,
            final short reference_major_version,
            final short reference_minor_version,
            final short reference_patch_version,
            final String[] sequence_name,
            final byte internal_dataset_group_id,
            final short internal_dataset_id) {
        
        super(dataset_group_id,
              reference_id,
              reference_name,
              reference_major_version,
              reference_minor_version,
              reference_patch_version,
              sequence_name);
        
        this.internal_dataset_group_id = internal_dataset_group_id;
        this.internal_dataset_id = internal_dataset_id;
    }

    @Override
    public boolean isExternalReference() {
        return false;
    }
    
    public byte getInternalDatasetGroupId() {
        return internal_dataset_group_id;
    }
    
    public void setInternalDatasetGroupId(final byte internal_dataset_group_id) {
        this.internal_dataset_group_id = internal_dataset_group_id;
    }
    
    public short getInternalDatasetId() {
        return internal_dataset_id;
    }
    
    public void setInternalDatasetId(final short internal_dataset_id) {
        this.internal_dataset_id = internal_dataset_id;
    }
    
    @Override
    public long size() {
        long size = super.size();

        size += 1; // internal_dataset_group_id [ u(8) ]
        size += 2; // internal_dataset_id [ u(16) ]
        
        return size;
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        super.write(writer);

        writer.writeByte(internal_dataset_group_id);
        writer.writeShort(internal_dataset_id);
    }
    
    @Override
    public Reference read(MPEGReader reader, long size) 
            throws IOException, InvalidMPEGStructureException, 
                   ParsedSizeMismatchException, InvalidMPEGGFileException {
        
        super.read(reader, size);
        
        internal_dataset_group_id = reader.readByte();
        internal_dataset_id = reader.readShort();
        
        return this;
    }
}

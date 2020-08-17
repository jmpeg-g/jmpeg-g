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

import es.gencom.mpegg.format.ChecksumAlgorithm;
import es.gencom.mpegg.format.InvalidMPEGGFileException;
import es.gencom.mpegg.format.InvalidMPEGStructureException;
import es.gencom.mpegg.format.ParsedSizeMismatchException;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * External reference encoded as a dataset (23092-1 6.5.2 Dataset).
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MPEGG_Reference extends ExternalReference {
    
    private short external_dataset_group_id;
    private int external_dataset_id;
    private byte[] ref_checksum;
            
    public MPEGG_Reference() {
        super(REFERENCE_TYPE.MPEGG_REF);
    }

    public MPEGG_Reference(
            final byte dataset_group_id,
            final byte reference_id,
            final String reference_name,
            final short reference_major_version,
            final short reference_minor_version,
            final short reference_patch_version,
            final String[] sequence_name,
            final String ref_uri,
            final byte external_dataset_group_id,
            final short external_dataset_id,
            final ChecksumAlgorithm checksum_alg,
            final byte[] ref_checksum) {

        super(REFERENCE_TYPE.MPEGG_REF,
              dataset_group_id,
              reference_id,
              reference_name,
              reference_major_version,
              reference_minor_version,
              reference_patch_version,
              sequence_name,
              ref_uri,
              checksum_alg);
        
        this.external_dataset_group_id = external_dataset_group_id;
        this.external_dataset_id = external_dataset_id;
        this.ref_checksum = ref_checksum;
    }
    
    public short getExternalDatasetGroupId() {
        return external_dataset_group_id;
    }
    
    public void setExternalDatasetGroupId(final short external_dataset_group_id) {
        this.external_dataset_group_id = external_dataset_group_id;
    }

    public int getExternalDatasetId() {
        return external_dataset_id;
    }
    
    public void setExternalDatasetId(final int external_dataset_id) {
        this.external_dataset_id = external_dataset_id;
    }
    
    public byte[] getReferenceChecksum() {
        return ref_checksum;
    }
    
    public void setReferenceChecksum(final byte[] ref_checksum) {
        this.ref_checksum = ref_checksum;
    }

    @Override
    public long size() {
        long size = super.size();
        
        size += 1; // external_dataset_group_id [ u(8) ]
        size += 2; // external_dataset_id       [ u(16) ]
        size += ref_checksum.length;
        
        return size;
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        super.write(writer);
        
        writer.writeUnsignedByte(external_dataset_group_id);
        writer.writeUnsignedShort(external_dataset_id);
        writer.writeBytes(ref_checksum);
    }
    
    @Override
    public Reference read(MPEGReader reader, long size) throws IOException, InvalidMPEGStructureException, ParsedSizeMismatchException, InvalidMPEGGFileException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

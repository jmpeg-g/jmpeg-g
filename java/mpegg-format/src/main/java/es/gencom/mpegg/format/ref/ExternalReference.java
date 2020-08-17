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
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * Abstract External Reference class (external_ref_flag = 1).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class ExternalReference extends Reference {
    
    public final REFERENCE_TYPE reference_type;
    
    private String ref_uri;
    private ChecksumAlgorithm checksum_alg;
    
    public ExternalReference(final REFERENCE_TYPE reference_type) {
        this.reference_type = reference_type;
    }
    
    public ExternalReference(
            final REFERENCE_TYPE reference_type,
            final byte dataset_group_id,
            final byte reference_id,
            final String reference_name,
            final short reference_major_version,
            final short reference_minor_version,
            final short reference_patch_version,
            final String[] sequence_name,
            final String ref_uri,
            final ChecksumAlgorithm checksum_alg) {
        
        super(dataset_group_id,
              reference_id,
              reference_name,
              reference_major_version,
              reference_minor_version,
              reference_patch_version,
              sequence_name);
        
        this.reference_type = reference_type;
        this.ref_uri = ref_uri;
        this.checksum_alg = checksum_alg;
    }
    
    @Override
    public boolean isExternalReference() {
        return true;
    }
    
    public String getReferenceURI() {
        return ref_uri;
    }
    
    public void setReferenceURI(final String ref_uri) {
        this.ref_uri = ref_uri;
    }

    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksum_alg;
    }

    public void setChecksumAlgorithm(final ChecksumAlgorithm checksum_alg) {
        this.checksum_alg = checksum_alg;
    }
    
    @Override
    public long size() {
        long size = super.size();
        
        size += ref_uri.getBytes(StandardCharsets.UTF_8).length + 1; // ref_uri [ st(v) ]
        size += 1; // checksum_alg [ u(8) ]
        size += 1; // reference_type [ u(8) ]
        
        return size;
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {
        super.write(writer);

        writer.writeNTString(ref_uri);

        checksum_alg.write(writer);
        reference_type.write(writer);
    }
    
    @Override
    public Reference read(MPEGReader reader, long size) 
            throws IOException, InvalidMPEGStructureException, 
                   ParsedSizeMismatchException, InvalidMPEGGFileException {
        
        super.read(reader, size);
        
        ref_uri = reader.readNTString();
        checksum_alg = ChecksumAlgorithm.read(reader);
        
        final REFERENCE_TYPE ref_type = REFERENCE_TYPE.read(reader);
        if (reference_type != ref_type) {
            throw new IOException();
        }
        
        return this;
    }
}

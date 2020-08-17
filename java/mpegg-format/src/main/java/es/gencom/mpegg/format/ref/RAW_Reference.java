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

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class RAW_Reference extends CheckedExternalReference {
    
    public RAW_Reference() {
        super(REFERENCE_TYPE.RAW_REF);
    }

    public RAW_Reference(
            final byte dataset_group_id,
            final byte reference_id,
            final String reference_name,
            final short reference_major_version,
            final short reference_minor_version,
            final short reference_patch_version,
            final String[] sequence_name,
            final String ref_uri,
            final ChecksumAlgorithm checksum_alg,
            final byte[][] ref_seq_checksum) {

        super(REFERENCE_TYPE.RAW_REF,
              dataset_group_id,
              reference_id,
              reference_name,
              reference_major_version,
              reference_minor_version,
              reference_patch_version,
              sequence_name,
              ref_uri,
              checksum_alg,
              ref_seq_checksum);
    }
}

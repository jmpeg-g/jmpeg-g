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
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ExternalLocation extends AbstractLocation {
    private String ref_uri;
    private ChecksumAlgorithm checksum_alg;
    private REFERENCE_TYPE reference_type;
    private Abstract_ExtRef_info extRef_info;

    public ExternalLocation() {
    }

    public ExternalLocation(
            String ref_uri,
            ChecksumAlgorithm checksum_alg,
            byte dataset_group_id,
            short dataset_id,
            byte[] checksums
    ) {
        this.ref_uri = ref_uri;
        this.checksum_alg = checksum_alg;
        this.reference_type = REFERENCE_TYPE.MPEGG_REF;
        this.extRef_info = new MPEGG_ExtRef_info(
                dataset_group_id,
                dataset_id,
                checksums
        );
    }

    public ExternalLocation(
            String ref_uri,
            ChecksumAlgorithm checksum_alg,
            REFERENCE_TYPE reference_type,
            byte[][] checksums
    ) {
        this.ref_uri = ref_uri;
        this.checksum_alg = checksum_alg;
        if(reference_type == REFERENCE_TYPE.MPEGG_REF){
            throw new IllegalArgumentException();
        }
        this.reference_type = reference_type;
        this.extRef_info = new RawOrFasta_ExtRef_info(
                checksums
        );
    }



    @Override
    public void read(final MPEGReader reader,
                     final int numberSequences) throws IOException {

        ref_uri = reader.readNTString();

        checksum_alg = ChecksumAlgorithm.read(reader);
        reference_type = REFERENCE_TYPE.read(reader);

        if(reference_type == REFERENCE_TYPE.MPEGG_REF) {
            extRef_info = new MPEGG_ExtRef_info();
            extRef_info.read(reader, checksum_alg, numberSequences);
        } else {
            extRef_info = new RawOrFasta_ExtRef_info();
            extRef_info.read(reader, checksum_alg, numberSequences);
        }


    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeByte((byte) 1);//external
        writer.writeNTString(ref_uri);

        checksum_alg.write(writer);
        reference_type.write(writer);

        extRef_info.write(writer);
    }

    @Override
    public long size() {
        long result = 1; //external flag
        result += ref_uri.getBytes(StandardCharsets.UTF_8).length + 1;
        result += 1; //algorithm
        result += 1; //ref_type
        result +=  extRef_info.getSize();
        return result;
    }

    public String getRef_uri() {
        return ref_uri;
    }

    public Abstract_ExtRef_info getExtRef_info(){
        return extRef_info;
    }

    public ChecksumAlgorithm getChecksum_alg() {
        return checksum_alg;
    }

    public REFERENCE_TYPE getReference_type() {
        return reference_type;
    }
}

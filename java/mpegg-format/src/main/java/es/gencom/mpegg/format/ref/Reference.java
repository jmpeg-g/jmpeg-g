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

import es.gencom.mpegg.format.GenInfo;
import es.gencom.mpegg.format.ParsedSizeMismatchException;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This is an optional box containing the information needed to retrieve an external 
 * or internal reference and its description as a set of reference sequences.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class Reference extends GenInfo<Reference> {
    
    public final static String KEY = "rfgn";

    private byte dataset_group_id;         // u(8)
    private byte reference_id;             // u(8)
    private String reference_name;
    private short reference_major_version;
    private short reference_minor_version;
    private short reference_patch_version;
    private String[] sequence_name;
    private AbstractLocation location;

    public Reference() {
        super(KEY);
    }
    
    public Reference(
            final byte dataset_group_id,
            final byte reference_id,
            final String reference_name,
            final short reference_major_version,
            final short reference_minor_version,
            final short reference_patch_version,
            final String[] sequence_name,
            final AbstractLocation location) {

        super(KEY);

        this.dataset_group_id = dataset_group_id;
        this.reference_id = reference_id;
        this.reference_name = reference_name;
        this.reference_major_version = reference_major_version;
        this.reference_minor_version = reference_minor_version;
        this.reference_patch_version = reference_patch_version;
        this.sequence_name = sequence_name;
        this.location = location;

    }
    
    public byte getReferenceId() {
        return reference_id;
    }
    
    public void setReferenceId(final byte reference_id) {
        this.reference_id = reference_id;
    }
    
    public String getReferenceName() {
        return reference_name;
    }
    
    public short getReferenceMajorVersion() {
        return reference_major_version;
    }
    
    public short getReferenceMinorVersion() {
        return reference_minor_version;
    }
    
    public short getReferencePatchVersion() {
        return reference_patch_version;
    }

    public String getSequence_name(final SequenceIdentifier sequenceIdentifier) {
        return sequence_name[sequenceIdentifier.getSequenceIdentifier()];
    }

    public AbstractLocation getLocation() {
        return location;
    }

    private AbstractLocation readLocation(final MPEGReader reader, final int numberSequences) throws IOException {
        final byte external_ref_flag = (byte) reader.readBits(8);
        AbstractLocation location;
        if (external_ref_flag == 0) {
            location = new InternalLocation();
        }else{
            location = new ExternalLocation();
        }
        location.read(reader, numberSequences);
        return location;
    }

    @Override
    public long size() {
        long len = 0;
        len += 1;// dataset_group_id
        len += 1;// referenceId
        len += reference_name.getBytes(StandardCharsets.UTF_8).length + 1; //reference name
        len += 2; //major version
        len += 2; //minor version
        len += 2; //patch version
        len += 2; //seq_count
        if(sequence_name != null) {
            for (String seq_name : sequence_name) {
                len += seq_name.getBytes(StandardCharsets.UTF_8).length + 1; //seq name
            }
        }
        len += location.size();
        return len;
    }

    @Override
    final public void write(final MPEGWriter writer) throws IOException {
        writer.writeByte(dataset_group_id);
        writer.writeByte(reference_id);
        writer.writeNTString(reference_name);
        writer.writeShort(reference_major_version);
        writer.writeShort(reference_minor_version);
        writer.writeShort(reference_patch_version);
        writer.writeShort((short) sequence_name.length);

        for (String seq_name : sequence_name) {
            writer.writeNTString(seq_name);
        }
        location.write(writer);
    }

    @Override
    public Reference read(
            final MPEGReader reader, 
            final long size) throws IOException, ParsedSizeMismatchException {

        dataset_group_id = (byte) reader.readBits(8);
        reference_id = (byte) reader.readBits(8);
        reference_name = reader.readNTString();
        reference_major_version = reader.readShort();
        reference_minor_version = reader.readShort();
        reference_patch_version = reader.readShort();
        short seq_count = reader.readShort();
        sequence_name = new String[seq_count];
        for(int seq_i=0; seq_i < seq_count; seq_i++){
            sequence_name[seq_i] = reader.readNTString();
        }
        location = readLocation(reader, seq_count);

        if(size != size()){
            throw new ParsedSizeMismatchException("The reference box has not the indicated size.");
        }
        return this;
    }

    public int getNumberSequences(){
        return sequence_name.length;
    }

}

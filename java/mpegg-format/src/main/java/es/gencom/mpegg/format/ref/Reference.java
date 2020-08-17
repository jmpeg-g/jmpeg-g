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
import es.gencom.mpegg.format.GenInfo;
import es.gencom.mpegg.format.InvalidMPEGGFileException;
import es.gencom.mpegg.format.InvalidMPEGStructureException;
import es.gencom.mpegg.format.ParsedSizeMismatchException;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * This is an optional box containing the information needed to retrieve an external 
 * or internal reference and its description as a set of reference sequences.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class Reference extends GenInfo<Reference> {
    
    public final static String KEY = "rfgn";

    private byte dataset_group_id;         // u(8)
    private byte reference_id;             // u(8)
    private String reference_name;
    private short reference_major_version;
    private short reference_minor_version;
    private short reference_patch_version;
    private String[] sequence_name;

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
            final String[] sequence_name) {

        super(KEY);

        this.dataset_group_id = dataset_group_id;
        this.reference_id = reference_id;
        this.reference_name = reference_name;
        this.reference_major_version = reference_major_version;
        this.reference_minor_version = reference_minor_version;
        this.reference_patch_version = reference_patch_version;
        this.sequence_name = sequence_name;
    }

    /**
     * Checks whether the reference is external.
     * 
     * @return 'external_ref_flag' value
     */
    public abstract boolean isExternalReference();
    
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

    public String getSequenceName(final SequenceIdentifier sequenceIdentifier) {
        return sequence_name[sequenceIdentifier.getSequenceIdentifier()];
    }

    public int getNumberSequences(){
        return sequence_name.length;
    }

    public String[] getSequenceNames() {
        return sequence_name;
    }

    @Override
    public long size() {
        long size = 0;
        size += 1;// reference_group_id [ u(8) ]
        size += 1;// reference_id       [ u(8) ]
        size += reference_name.getBytes(StandardCharsets.UTF_8).length + 1; // reference_name [ st(v) ]
        size += 2; //reference_major_version [ u(16) ]
        size += 2; //reference_minor_version [ u(16) ]
        size += 2; //reference_patch_version [ u(16) ]
        size += 2; //seq_count [ u(16) ]
        if(sequence_name != null) {
            for (String seq_name : sequence_name) {
                size += seq_name.getBytes(StandardCharsets.UTF_8).length + 1; // sequence_name [ st(v) ]
            }
        }
        size += 1; // reserved + external_ref_flag [ u(7) + u(1) ]
        
        return size;
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeByte(dataset_group_id);
        writer.writeByte(reference_id);
        writer.writeNTString(reference_name);
        writer.writeShort(reference_major_version);
        writer.writeShort(reference_minor_version);
        writer.writeShort(reference_patch_version);
        writer.writeShort((short)sequence_name.length);

        for (String seq_name : sequence_name) {
            writer.writeNTString(seq_name);
        }
        
        writer.writeBits(0, 7);                     // reserved
        writer.writeBoolean(isExternalReference()); // external_ref_flag
    }

    @Override
    public Reference read(MPEGReader reader, long size) 
            throws IOException, InvalidMPEGStructureException, 
                   ParsedSizeMismatchException, InvalidMPEGGFileException {

        dataset_group_id = reader.readByte();
        reference_id = reader.readByte();
        reference_name = reader.readNTString();
        reference_major_version = reader.readShort();
        reference_minor_version = reader.readShort();
        reference_patch_version = reader.readShort();
        
        final int seq_count = reader.readUnsignedShort();
        sequence_name = new String[seq_count];
        for(int i = 0; i < seq_count; i++) {
            sequence_name[i] = reader.readNTString();
        }
        
        reader.readByte(); // skip 'reserved' && 'external_ref_flag'
        
        return this;
    }

    public static Reference readReference(final MPEGReader reader) 
            throws IOException, ParsedSizeMismatchException {

        final byte dataset_group_id = reader.readByte();
        final byte reference_id = reader.readByte();
        final String reference_name = reader.readNTString();
        final short reference_major_version = reader.readShort();
        final short reference_minor_version = reader.readShort();
        final short reference_patch_version = reader.readShort();
        
        final int seq_count = reader.readUnsignedShort();
        final String[] sequence_name = new String[seq_count];
        for(int i = 0; i < seq_count; i++) {
            sequence_name[i] = reader.readNTString();
        }
        
        final byte external_ref_flag = (byte) reader.readBits(8);
        if (external_ref_flag == 0) {
            final byte internal_dataset_group_id = reader.readByte();
            final short internal_dataset_id = reader.readShort();
            
            return new DatasetReference(
                    dataset_group_id,
                    reference_id,
                    reference_name,
                    reference_major_version,
                    reference_minor_version,
                    reference_patch_version,
                    sequence_name,
                    internal_dataset_group_id,
                    internal_dataset_id);
        }

        final String ref_uri = reader.readNTString();

        final ChecksumAlgorithm checksum_alg = ChecksumAlgorithm.read(reader);
        final REFERENCE_TYPE reference_type = REFERENCE_TYPE.read(reader);

        if(reference_type == REFERENCE_TYPE.MPEGG_REF) {
            final byte external_dataset_group_id = reader.readByte();
            final short external_dataset_id = reader.readShort();
            
            final byte[] checksum;
            
            final ByteBuffer buf = reader.readByteBuffer(checksum_alg.size / 8);
            if (buf.hasArray()) {
                checksum = buf.array();
            } else {
                checksum = new byte[buf.limit()];
                buf.position(0);
                buf.get(checksum);
            }
            return new MPEGG_Reference(
                    dataset_group_id,
                    reference_id,
                    reference_name,
                    reference_major_version,
                    reference_minor_version,
                    reference_patch_version,
                    sequence_name,
                    ref_uri,
                    external_dataset_group_id,
                    external_dataset_id,
                    checksum_alg,
                    checksum);
        }

        final byte[][] checksums = new byte[seq_count][];
        for (int i = 0; i < seq_count; i++) {
            final ByteBuffer buf = reader.readByteBuffer(checksum_alg.size / 8);
            if (buf.hasArray()) {
                checksums[i] = buf.array();
            } else {
                checksums[i] = new byte[buf.limit()];
                buf.position(0);
                buf.get(checksums[i]);
            }
            checksums[i]= new byte[checksum_alg.size / 8];
        }

        if(reference_type == REFERENCE_TYPE.FASTA_REF) {
            return new FASTA_Reference(
                    dataset_group_id,
                    reference_id,
                    reference_name,
                    reference_major_version,
                    reference_minor_version,
                    reference_patch_version,
                    sequence_name,
                    ref_uri,
                    checksum_alg,
                    checksums);
        }
        
        if(reference_type == REFERENCE_TYPE.RAW_REF) {
            return new RAW_Reference(
                    dataset_group_id,
                    reference_id,
                    reference_name,
                    reference_major_version,
                    reference_minor_version,
                    reference_patch_version,
                    sequence_name,
                    ref_uri,
                    checksum_alg,
                    checksums);
        }
        
        throw new IOException("illegal reference type");
    }
}

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
import es.gencom.mpegg.io.MSBitBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class AccessUnitHeader extends GenInfo<AccessUnitHeader> {
    
    public final static String KEY = "auhd";
    
    public final DatasetHeader header;
    
    private int access_unit_id;             // u(32)
    private byte num_blocks;                // u(8)
    private short parameter_set_id;         // u(12)
    private DATA_CLASS au_type;              // u(4)
    private int reads_count;                // u(32)
    private short mm_threshold;             // u(16)
    private int mm_count;                   // u(32)
    private SequenceIdentifier ref_sequence_id;
    private long ref_start_position;
    private long ref_end_position;
    private byte reference_id;               // u(8)
    private SequenceIdentifier sequence_id;  // u(16)
    private long au_start_position;          // u(32) or u(40)
    private long au_end_position;            // u(32) or u(40)
    private long extended_au_start_position; // u(32) or u(40)
    private long extended_au_end_position;   // u(32) or u(40)
    private int[] u_cluster_signatures;
    
    public AccessUnitHeader(final DatasetHeader header) {
        super(KEY);
        this.header = header;
    }

    public AccessUnitHeader(
            final DatasetHeader datasetHeader,
            final int access_unit_id,
            final byte num_blocks,
            final short parameter_set_id,
            final DATA_CLASS au_type,
            final int reads_count,
            final short mm_threshold,
            final int mm_count){

        this(datasetHeader,
             access_unit_id,
             num_blocks,
             parameter_set_id,
             au_type,
             reads_count,
             mm_threshold,
             mm_count,
             null,
             0,
             0);
    }

    public AccessUnitHeader(
            final DatasetHeader datasetHeader,
            final int access_unit_id,
            final byte num_blocks,
            final short parameter_set_id,
            final DATA_CLASS au_type,
            final int reads_count,
            final short mm_threshold,
            final int mm_count,
            final SequenceIdentifier ref_sequence_id,
            final long ref_start_position,
            final long ref_end_position) {

        this(datasetHeader);

        this.access_unit_id = access_unit_id;
        this.num_blocks = num_blocks;
        this.parameter_set_id = parameter_set_id;
        this.au_type = au_type;
        this.reads_count = reads_count;
        this.mm_threshold = mm_threshold;
        this.mm_count = mm_count;
        this.ref_sequence_id = ref_sequence_id;
        this.ref_start_position = ref_start_position;
        this.ref_end_position = ref_end_position;
    }

    public AccessUnitHeader(
            final DatasetHeader datasetHeader,
            final int access_unit_id,
            final byte num_blocks,
            final short parameter_set_id,
            final DATA_CLASS au_type,
            final int reads_count,
            final short mm_threshold,
            final int mm_count,
            final SequenceIdentifier ref_sequence_id,
            final long ref_start_position,
            final long ref_end_position,
            final SequenceIdentifier sequence_id,
            final long au_start_position,
            final long au_end_position,
            final long extended_au_start,
            final long extended_au_end
    ) {

        this(datasetHeader);

        this.access_unit_id = access_unit_id;
        this.num_blocks = num_blocks;
        this.parameter_set_id = parameter_set_id;
        this.au_type = au_type;
        this.reads_count = reads_count;
        this.mm_threshold = mm_threshold;
        this.mm_count = mm_count;
        this.ref_sequence_id = ref_sequence_id;
        this.ref_start_position = ref_start_position;
        this.ref_end_position = ref_end_position;
        this.sequence_id = sequence_id;
        this.au_start_position = au_start_position;
        this.au_end_position = au_end_position;
        this.extended_au_start_position = extended_au_start;
        this.extended_au_end_position = extended_au_end;
    }

    public int getAccessUnitId() {
        return access_unit_id;
    }
    
    public void setAccessUnitId(final int access_unit_id) {
        this.access_unit_id = access_unit_id;
    }
    
    public byte getNumberOfBlocks() {
        return num_blocks;
    }

    public int getAccessUnitID() {
        return access_unit_id;
    }

    public byte getNumBlocks() {
        return num_blocks;
    }

    public void setNumBlocks(final byte num_blocks) {
        this.num_blocks = num_blocks;
    }

    public short getParameterSetID() {
        return parameter_set_id;
    }

    public void setParameterSetID(final short parameter_set_id) {
        this.parameter_set_id = parameter_set_id;
    }

    public DATA_CLASS getAUType() {
        return au_type;
    }

    public void setAUType(final DATA_CLASS au_type) {
        this.au_type = au_type;
    }

    public int getReadsCount() {
        return reads_count;
    }

    public void setReadsCount(final int reads_count) {
        this.reads_count = reads_count;
    }

    public short getMmThreshold() {
        return mm_threshold;
    }

    public void setMmThreshold(final short mm_threshold) {
        this.mm_threshold = mm_threshold;
    }

    public int getMmCount() {
        return mm_count;
    }

    public void setMmCount(final int mm_count) {
        this.mm_count = mm_count;
    }

    public byte getReferenceID() {
        return reference_id;
    }

    public void setReferenceID(final byte reference_id) {
        this.reference_id = reference_id;
    }

    public SequenceIdentifier getSequenceID() {
        return sequence_id;
    }

    public void setSequenceID(final SequenceIdentifier sequence_id) {
        this.sequence_id = sequence_id;
    }

    public long getAUStartPosition() {
        return au_start_position;
    }

    public void setAUStartPosition(final long au_start_position) {
        this.au_start_position = au_start_position;
    }

    public long getAUEndPosition() {
        return au_end_position;
    }

    public void setAUEndPosition(final long au_end_position) {
        this.au_end_position = au_end_position;
    }

    public long getExtendedAUStartPosition() {
        return extended_au_start_position;
    }


    public long getExtendedAUEndPosition() {
        return extended_au_end_position;
    }

    public int[] getUClusterSignatures() {
        return u_cluster_signatures;
    }

    public void setUClusterSignatures(final int[] u_cluster_signatures) {
        this.u_cluster_signatures = u_cluster_signatures;
    }

    public SequenceIdentifier getReferenceSequenceID() {
        return ref_sequence_id;
    }

    public long getRefStartPosition() {
        return ref_start_position;
    }

    public long getRefEndPosition() {
        return ref_end_position;
    }

    @Override
    public long size() {
        long result = 0;

        result += 32; //access unit Id
        result += 8;  //numBlocks
        result += 12; //parameter set id
        result += 4;  // au_type
        result += 32; //reads count

        if (au_type == DATA_CLASS.CLASS_M ||
            au_type == DATA_CLASS.CLASS_N) {

            result += 16; //mm_threshold
            result += 32; //mm_count
        }

        final byte pos_size = (byte)(header.isPos_40_bits() ? 40 : 32);

        if (header.getDatasetType() == DatasetType.REFERENCE){
            result += 16;
            result += pos_size;
            result += pos_size;
        }

        if (!header.isMIT()) {

            if (au_type != DATA_CLASS.CLASS_U) {
                result += 16; //sequenceId
                result += pos_size; //start pos
                result += pos_size; //end pos

                if (header.isMultipleAlignment()) {
                    result += pos_size; //ext start pos
                    result += pos_size; //ext end pos
                }
            } else if (u_cluster_signatures != null & u_cluster_signatures.length > 0) {
                final int multiple_signature_base = header.getMultipleSignatureBase();
                if (multiple_signature_base != 0) {
                    final byte u_signature_size = header.getUnmappedSignatureSize();
                    if (multiple_signature_base == u_cluster_signatures.length) {
                        //todo implement
                    } else {
                        //todo implement
                    }
                }
            }
        }

        return (long) Math.ceil((long)result/(long)8);
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {

        writer.writeBits(access_unit_id, 32);
        writer.writeBits(num_blocks, 8);
        writer.writeBits(parameter_set_id, 8);
        writer.writeBits(au_type.ID, 4);
        writer.writeBits(reads_count, 32);
        
        if (au_type == DATA_CLASS.CLASS_M ||
            au_type == DATA_CLASS.CLASS_N) {
                writer.writeBits(mm_threshold, 16);
                writer.writeBits(mm_count, 32);
        }

        final byte pos_size = (byte)(header.isPos_40_bits() ? 40 : 32);

        if(header.getDatasetType() == DatasetType.REFERENCE){
            writer.writeShort((short)ref_sequence_id.getSequenceIdentifier());
            writer.writeBits(ref_start_position, pos_size);
            writer.writeBits(ref_end_position, pos_size);
        }


        if (!header.isMIT()) {
            if (au_type != DATA_CLASS.CLASS_U) {
                writer.writeBits(sequence_id.getSequenceIdentifier(), 16);

                byte posSize = (byte)(header.isPos_40_bits() ? 40 : 32);

                writer.writeBits(au_start_position, posSize);
                writer.writeBits(au_end_position, posSize);

                if (header.isMultipleAlignment()) {
                    writer.writeBits(extended_au_start_position, posSize);
                    writer.writeBits(extended_au_end_position, posSize);
                }
            } else if (u_cluster_signatures != null & u_cluster_signatures.length > 0) {
                final int multiple_signature_base = header.getMultipleSignatureBase();
                if (multiple_signature_base != 0) {
                    final byte u_signature_size = header.getUnmappedSignatureSize();
                    if (multiple_signature_base == u_cluster_signatures.length) {
                        for (int i = 0; i < u_cluster_signatures.length; i++) {
                            writer.writeBits(u_cluster_signatures[i], u_signature_size);
                        }
                    } else {
                        writer.writeBits(0xFFFFFFFF, u_signature_size);
                        writer.writeBits(u_cluster_signatures.length, 16);
                        for (int i = 0; i < u_cluster_signatures.length; i++) {
                            writer.writeBits(u_cluster_signatures[i], u_signature_size);
                        }
                    }
                }
            }
        }
        writer.flush();

    }
    
    @Override
    public AccessUnitHeader read(final MPEGReader reader, final long size) throws IOException {
        access_unit_id = (int)reader.readBits(32);
        num_blocks = (byte)reader.readBits(8);
        parameter_set_id = (short)reader.readBits(8);
        au_type = DATA_CLASS.getDataClass((byte)reader.readBits(4));
        reads_count = (int)reader.readBits(32);
        
        if (au_type == DATA_CLASS.CLASS_M ||
            au_type == DATA_CLASS.CLASS_N) {
                mm_threshold = (short)reader.readBits(16);
                mm_count = (int)reader.readBits(32);
        }

        final byte pos_size = (byte)(header.isPos_40_bits() ? 40 : 32);
        if(header.getDatasetType() == DatasetType.REFERENCE){
            ref_sequence_id = new SequenceIdentifier(reader.readUnsignedShort());
            ref_start_position = reader.readBits(pos_size);
            ref_end_position = reader.readBits(pos_size);
        }


        if (!header.isMIT()) {
            sequence_id = new SequenceIdentifier((short) reader.readBits(16));

            if (au_type != DATA_CLASS.CLASS_U) {

                byte posSize = (byte)(header.isPos_40_bits() ? 40 : 32);
                au_start_position = (int) reader.readBits(posSize);
                au_end_position = (int) reader.readBits(posSize);

                if (header.isMultipleAlignment()) {
                    extended_au_start_position = (int) reader.readBits(posSize);
                    extended_au_end_position = (int) reader.readBits(posSize);
                }
            } else {
                final int multiple_signature_base = header.getMultipleSignatureBase();
                if (multiple_signature_base != 0) {
                    final int u_signature_size = header.getUnmappedSignatureSize() & 0xFF;
                    final int u_cluster_signature_0 = (int) reader.readBits(u_signature_size);
                    if (u_cluster_signature_0 != (1 << u_signature_size) - 1) {
                        u_cluster_signatures = new int[multiple_signature_base];
                        u_cluster_signatures[0] = u_cluster_signature_0;
                        for (int i = 1; i < multiple_signature_base; i++) {
                            final int u_cluster_signature = (int) reader.readBits(u_signature_size);
                            u_cluster_signatures[i] = u_cluster_signature;
                        }
                    } else {
                        final int num_signatures = (int) reader.readBits(16);
                        u_cluster_signatures = new int[num_signatures];
                        for (int i = 0; i < num_signatures; i++) {
                            final int u_cluster_signature = (int) reader.readBits(u_signature_size);
                            u_cluster_signatures[i] = u_cluster_signature;
                        }
                    }
                }
            }
        }

        reader.align();
        return this;
    }
    
    public static AccessUnitHeader read(final DatasetHeader header, final ReadableByteChannel channel) throws IOException {
        return read(header, Header.read(channel), channel);
    }
    
    private static AccessUnitHeader read(final DatasetHeader dataset_header, final Header header, final ReadableByteChannel channel) throws IOException {
        if (!DatasetHeader.KEY.equals(header.key)) {
            throw new IOException(String.format("Invalid AccessUnitHeader KEY: %s (must be %s)", header.key, AccessUnitHeader.KEY));
        }

        final ByteBuffer buf = ByteBuffer.allocate((int)header.length).order(BYTE_ORDER);
        while (buf.hasRemaining() && channel.read(buf) >= 0){}

        buf.rewind();

        return new AccessUnitHeader(dataset_header).read(new MSBitBuffer(buf), header.length - Header.SIZE);
    }

    public void setU_cluster_signatures(int[] u_cluster_signatures) {
        this.u_cluster_signatures = u_cluster_signatures;
    }
}
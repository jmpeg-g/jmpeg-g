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
import static es.gencom.mpegg.format.DatasetType.REFERENCE;

import es.gencom.mpegg.format.signatures.Signature;
import es.gencom.mpegg.format.signatures.SignatureReader;
import es.gencom.mpegg.format.signatures.SignatureSizeEvaluator;
import es.gencom.mpegg.format.signatures.SignatureWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MasterIndexTable extends GenInfo<MasterIndexTable> {
    
    public final static String KEY = "mitb";
    
    public final DatasetHeader dataset_header;

    private long defaultAUOffset = -1L;
    private long datasetInitialPosition = 0;
    private long[][][] au_start_position;
    private long[][][] au_end_position;
    private SequenceIdentifier[][][] ref_sequence_id;
    private long[][][] ref_start_position;
    private long[][][] ref_end_position;
    private long[][][] extended_au_start_position;
    private long[][][] extended_au_end_position;
    private long[][][] au_byte_offset;             // u(byte_offset_size)
    private long[][][][] block_byte_offset;        // u(byte_offset_size)
    private ArrayList<ArrayList<TreeSet<Long>>> blockStartsPerClassPerDescriptor;
    private Signature[][] u_cluster_signature;
    private long[] u_au_byte_offset;
    private SequenceIdentifier[] u_ref_sequence_id;
    private long[] u_ref_start_position;
    private long[] u_ref_end_position;
    private long[][] u_block_byte_offset;
    
    public MasterIndexTable(final DatasetHeader dataset_header) {
        super(KEY);
        
        this.dataset_header = dataset_header;
    }

    public MasterIndexTable(
            final DatasetHeader datasetHeader,
            final long[][][] au_start_position,
            final long[][][] au_end_position,
            final long[][][] extended_au_start_position,
            final long[][][] extended_au_end_position,
            final long[][][] au_byte_offset,
            final long[][][][] block_byte_offset,
            final Signature[][] signatures,
            final long[] ints,
            final long[][] ints1) {
        this(
            datasetHeader,
            au_start_position,
            au_end_position,
            extended_au_start_position,
            extended_au_end_position,
            au_byte_offset,
            signatures,
            ints,
            ints1
        );
        this.block_byte_offset = block_byte_offset;
    }

    public MasterIndexTable(
            final DatasetHeader datasetHeader,
            final long[][][] au_start_position,
            final long[][][] au_end_position,
            final long[][][] extended_au_start_position,
            final long[][][] extended_au_end_position,
            final long[][][] au_byte_offset,
            final Signature[][] u_cluster_signature,
            final long[] u_au_byte_offset,
            final long[][] u_block_byte_offset) {

        this(datasetHeader);
        this.au_start_position = au_start_position;
        this.au_end_position = au_end_position;
        this.extended_au_start_position = extended_au_start_position;
        this.extended_au_end_position = extended_au_end_position;
        this.au_byte_offset = au_byte_offset;
        this.block_byte_offset = null;
        this.blockStartsPerClassPerDescriptor = null;
        this.u_cluster_signature = u_cluster_signature;
        this.u_au_byte_offset = u_au_byte_offset;
        this.u_block_byte_offset = u_block_byte_offset;
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        try {
            final int byte_offset_size = dataset_header.isByteOffsetSize() ? 64 : 32;

            for (short seq = 0; seq < au_start_position.length; seq++) {
                for (int ci = 0; ci < au_start_position[seq].length; ci++) {
                    if (au_start_position[seq][ci] == null) {
                        for (int au_id = 0; au_id < dataset_header.getReferenceSequenceBlocks(new DatasetSequenceIndex(seq)); au_id++) {
                            writer.writeBits(-1L, 32);
                            writer.writeBits(-1L, 32);
                            if (dataset_header.isMultipleAlignment()) {
                                writer.writeBits(-1L, 32);
                                writer.writeBits(-1L, 32);
                            }

                            writer.writeBits(defaultAUOffset, byte_offset_size);

                            if (!dataset_header.isBlockHeader()) {
                                for (int desc_id = 0; desc_id < block_byte_offset[seq][ci][au_id].length; desc_id++) {
                                    writer.writeBits(-1L, byte_offset_size);
                                }
                            }
                        }
                    } else {
                        for (int au_id = 0; au_id < au_start_position[seq][ci].length; au_id++) {

                            writer.writeBits(au_byte_offset[seq][ci][au_id], byte_offset_size);
                            writer.writeBits(au_start_position[seq][ci][au_id], 32);
                            writer.writeBits(au_end_position[seq][ci][au_id], 32);



                            if (dataset_header.isMultipleAlignment()) {
                                writer.writeBits(extended_au_start_position[seq][ci][au_id], 32);
                                writer.writeBits(extended_au_end_position[seq][ci][au_id], 32);
                            }


                            if (!dataset_header.isBlockHeader()) {
                                for (int desc_id = 0; desc_id < block_byte_offset[seq][ci][au_id].length; desc_id++) {
                                    writer.writeBits(block_byte_offset[seq][ci][au_id][desc_id], byte_offset_size);
                                }
                            }
                        }
                    }
                }
            }

            if (dataset_header.isUnmappedIndexing() &&
                    dataset_header.getDatasetType() != REFERENCE) {

                final int multiple_signature_base = dataset_header.getMultipleSignatureBase();
                final int u_signature_size = dataset_header.getUnmappedSignatureSize() & 0xFF;

                final long num_u_access_units = dataset_header.getNumberUAccessUnits();
                for (int uau_id = 0; uau_id < num_u_access_units; uau_id++) {
                    if (multiple_signature_base != 0) {
                        SignatureWriter signatureWriter =
                                new SignatureWriter(
                                        writer,
                                        dataset_header.getUnmappedSignatureLength(),
                                        u_signature_size,
                                        dataset_header.getAlphabetId().bits
                                );
                        if (multiple_signature_base == u_cluster_signature[uau_id].length) {
                            for (int i = 0; i < multiple_signature_base; i++) {
                                signatureWriter.writeSignature(u_cluster_signature[uau_id][i]);
                            }
                        } else {
                            writer.writeBits(0xFFFFFFFF, u_signature_size);
                            writer.writeBits(u_cluster_signature[uau_id].length, 16);
                            for (int i = 0; i < u_cluster_signature[uau_id].length; i++) {
                                signatureWriter.writeSignature(u_cluster_signature[uau_id][i]);
                            }
                        }
                    }
                    writer.flush();

                    if (dataset_header.isBlockHeader()) {
                        writer.writeBits(u_au_byte_offset[uau_id], byte_offset_size);
                    } else {
                        final int num_descriptors = dataset_header.getNumberOfDescriptors(DATA_CLASS.CLASS_U);
                        for (int desc_id = 0; desc_id < num_descriptors; desc_id++) {
                            writer.writeBits(u_block_byte_offset[uau_id][desc_id], byte_offset_size);
                        }
                    }
                }
            }
        }catch (DataClassNotFoundException e){
            throw new InternalError(e);
        }
    }

    @Override
    public MasterIndexTable read(final MPEGReader reader, final long size) throws IOException, InvalidMPEGGFileException, ParsedSizeMismatchException {
        final int byte_offset_size = dataset_header.isByteOffsetSize() ? 64 : 32;
        byte posSize = (byte) (dataset_header.isByteOffsetSize() ? 40 : 32);

        try {

            final int seq_count = dataset_header.getReferenceSequencesCount();
            final int num_classes = dataset_header.getNumberAlignedClasses();

            if(dataset_header.getDatasetType() == REFERENCE){
                ref_sequence_id = new SequenceIdentifier[seq_count][num_classes][];
                ref_start_position = new long[seq_count][num_classes][];
                ref_end_position = new long[seq_count][num_classes][];
            }else{
                ref_sequence_id = null;
                ref_start_position = null;
                ref_end_position = null;
            }

            au_start_position = new long[seq_count][num_classes][];
            au_end_position = new long[seq_count][num_classes][];

            if (dataset_header.isMultipleAlignment()) {
                extended_au_start_position = new long[seq_count][num_classes][];
                extended_au_end_position = new long[seq_count][num_classes][];
            }

            au_byte_offset = new long[seq_count][num_classes][];


            if (!dataset_header.isBlockHeader()) {
                block_byte_offset = new long[seq_count][num_classes][][];

                blockStartsPerClassPerDescriptor = new ArrayList<>(DATA_CLASS.values().length);

                int class_i = 0;
                for (DATA_CLASS data_class : DATA_CLASS.values()) {
                    try {
                        final int num_descriptors = dataset_header.getNumberOfDescriptors(data_class);
                        blockStartsPerClassPerDescriptor.add(new ArrayList<>(num_descriptors));

                        for (int descriptor_i = 0; descriptor_i < num_descriptors; descriptor_i++) {
                            blockStartsPerClassPerDescriptor.get(class_i).add(new TreeSet<>());
                        }

                        class_i++;
                    }catch (DataClassNotFoundException ignore){

                    }
                }
            }

            for (short seq = 0; seq < seq_count; seq++) {
                final int seq_block = (int) dataset_header.getReferenceSequenceBlocks(new DatasetSequenceIndex(seq));
                int ci = 0;
                for (DATA_CLASS data_class : dataset_header.getAlignedClassIds()) {
                    try {
                        au_start_position[seq][ci] = new long[seq_block];
                        au_end_position[seq][ci] = new long[seq_block];

                        if(dataset_header.getDatasetType() == REFERENCE){
                            ref_sequence_id[seq][ci] = new SequenceIdentifier[seq_block];
                            ref_start_position[seq][ci] = new long[seq_block];
                            ref_end_position[seq][ci] = new long[seq_block];
                        }

                        if (dataset_header.isMultipleAlignment()) {
                            extended_au_start_position[seq][ci] = new long[seq_block];
                            extended_au_end_position[seq][ci] = new long[seq_block];
                        }

                        au_byte_offset[seq][ci] = new long[seq_block];

                        if (!dataset_header.isBlockHeader()) {
                            block_byte_offset[seq][ci] = new long[seq_block][];
                        }

                        for (int au_id = 0; au_id < seq_block; au_id++) {
                            au_byte_offset[seq][ci][au_id] = reader.readBits(byte_offset_size);
                            au_start_position[seq][ci][au_id] = (int) reader.readBits(posSize);
                            au_end_position[seq][ci][au_id] = (int) reader.readBits(posSize);

                            if (dataset_header.getDatasetType() == REFERENCE){
                                ref_sequence_id[seq][ci][au_id] = new SequenceIdentifier((int) reader.readBits(16));
                                ref_start_position[seq][ci][au_id] = (int) reader.readBits(posSize);
                                ref_end_position[seq][ci][au_id] = (int) reader.readBits(posSize);
                            }

                            if (dataset_header.isMultipleAlignment()) {
                                extended_au_start_position[seq][ci][au_id] = (int) reader.readBits(posSize);
                                extended_au_end_position[seq][ci][au_id] = (int) reader.readBits(posSize);
                            }


                            if (!dataset_header.isBlockHeader()) {
                                final int num_descriptors = dataset_header.getNumberOfDescriptors(data_class);
                                block_byte_offset[seq][ci][au_id] = new long[num_descriptors];
                                for (int desc_id = 0; desc_id < num_descriptors; desc_id++) {
                                    long byte_offset = reader.readBits(byte_offset_size);
                                    block_byte_offset[seq][ci][au_id][desc_id] = byte_offset;
                                    blockStartsPerClassPerDescriptor.get(ci).get(desc_id).add(byte_offset);
                                }
                            }
                        }

                        ci++;
                    }catch (DataClassNotFoundException ignore){}
                }
            }

            final int num_u_access_units = (int) dataset_header.getNumberUAccessUnits();


            u_au_byte_offset = new long[num_u_access_units];
            u_block_byte_offset = new long[num_u_access_units][dataset_header.getNumberOfDescriptors(DATA_CLASS.CLASS_U)];

            if(dataset_header.getDatasetType() == REFERENCE){
                u_ref_sequence_id = new SequenceIdentifier[num_u_access_units];
                u_ref_start_position = new long[num_u_access_units];
                u_ref_end_position = new long[num_u_access_units];

                for(int u_au_id=0; u_au_id < num_u_access_units; u_au_id++){
                    u_ref_sequence_id[u_au_id] = new SequenceIdentifier((int) reader.readBits(16));
                    u_ref_start_position[u_au_id] = reader.readBits(posSize);
                    u_ref_end_position[u_au_id] = reader.readBits(posSize);
                }
            } else {
                final int multiple_signature_base = dataset_header.getMultipleSignatureBase();
                final int u_signature_size = dataset_header.getUnmappedSignatureSize() & 0xFF;

                for (int uau_id = 0; uau_id < num_u_access_units; uau_id++) {
                    if (multiple_signature_base != 0) {
                        SignatureReader signatureReader =
                                new SignatureReader(
                                        reader,
                                        dataset_header.getAlphabetId().bits,
                                        (byte) u_signature_size,
                                        dataset_header.getUnmappedSignatureLength()
                                );
                        final long u_cluster_signature_0 = reader.readBits(u_signature_size);
                        if (u_cluster_signature_0 != (1 << u_signature_size) - 1) {
                            u_cluster_signature[uau_id] = new Signature[multiple_signature_base];
                            u_cluster_signature[uau_id][0] = signatureReader.read(u_cluster_signature_0);
                            for (int i = 1; i < multiple_signature_base; i++) {
                                u_cluster_signature[uau_id][i] = signatureReader.read();
                            }
                        } else {
                            final int num_signatures = (int) reader.readBits(16);
                            u_cluster_signature[uau_id] = new Signature[num_signatures];
                            for (int i = 0; i < num_signatures; i++) {
                                u_cluster_signature[uau_id][i] = signatureReader.read();
                            }
                        }
                    }
                }
            }


            for (int uau_id = 0; uau_id < num_u_access_units; uau_id++) {
                for (
                        int descriptor_i = 0;
                        descriptor_i < dataset_header.getNumberOfDescriptors(DATA_CLASS.CLASS_U);
                        descriptor_i++
                ) {
                    u_block_byte_offset[uau_id][descriptor_i] = reader.readBits(byte_offset_size);
                }
            }

            reader.align();
        } catch (DataClassNotFoundException ex) {
            throw new IllegalArgumentException(ex);
        }

        if(size() != size){
            throw new ParsedSizeMismatchException();
        }

        return this;
    }

    public static MasterIndexTable read(final DatasetHeader header, final ReadableByteChannel channel)
            throws IOException, InvalidMPEGGFileException, ParsedSizeMismatchException {
        return read(header, Header.read(channel), channel);
    }

    public static MasterIndexTable read(final DatasetHeader dataset_header, final Header header, final ReadableByteChannel channel)
            throws IOException, InvalidMPEGGFileException, ParsedSizeMismatchException {
        if (!MasterIndexTable.KEY.equals(header.key)) {
            throw new IOException(String.format("Invalid MasterIndexTable KEY: %s (must be %s)", header.key, MasterIndexTable.KEY));
        }

        final ByteBuffer buf = ByteBuffer.allocate((int)header.length).order(BYTE_ORDER);
        while (buf.hasRemaining() && channel.read(buf) >= 0){}

        buf.rewind();

        return new MasterIndexTable(dataset_header).read(new MSBitBuffer(buf), header.length); // read(dataset_header, new MSBitBuffer(buf));
    }

    @Override
    public long size() {
        try {
            long result = 0;
            final int byte_offset_size = dataset_header.isByteOffsetSize() ? 64 : 32;
            final int pos_size = dataset_header.isPos_40_bits() ? 5: 4;
            for (short seq = 0; seq < au_start_position.length; seq++) {
                for (int ci = 0; ci < au_start_position[seq].length; ci++) {
                    int loopSize;
                    if (au_start_position[seq][ci] == null) {
                        loopSize = (int) dataset_header.getReferenceSequenceBlocks(new DatasetSequenceIndex(seq));
                    } else {
                        loopSize = au_start_position[seq][ci].length;
                    }
                    for (int au_id = 0; au_id < loopSize; au_id++) {
                        result += pos_size; //au_start
                        result += pos_size; //au_end
                        if(dataset_header.getDatasetType() == REFERENCE){
                            result += 2; //ref_sequence
                            result += pos_size; //ref_start
                            result += pos_size; //ref_end
                        }
                        if (dataset_header.isMultipleAlignment()) {
                            result += pos_size; //ext_au_start
                            result += pos_size; //ext_au_end
                        }

                        result += Math.ceil(byte_offset_size / 8); //au_byte_offset
                        if (!dataset_header.isBlockHeader()) {
                            for (int desc_id = 0; desc_id < block_byte_offset[seq][ci][au_id].length; desc_id++) {
                                result += Math.ceil(byte_offset_size / 8); //block_byte_offset
                            }
                        }
                    }

                }
            }

            if (dataset_header.isUnmappedIndexing() &&
                    dataset_header.getDatasetType() != REFERENCE) {

                final int multiple_signature_base = dataset_header.getMultipleSignatureBase();
                final int u_signature_size = dataset_header.getUnmappedSignatureSize() & 0xFF;

                final int num_u_access_units = (int) dataset_header.getNumberUAccessUnits();
                for (int uau_id = 0; uau_id < num_u_access_units; uau_id++) {
                    SignatureSizeEvaluator signatureSizeEvaluator =
                            new SignatureSizeEvaluator(
                                    dataset_header.getUnmappedSignatureLength(),
                                    u_signature_size,
                                    dataset_header.getAlphabetId().bits
                            );
                    if (multiple_signature_base != 0) {
                        if (multiple_signature_base == u_cluster_signature[uau_id].length) {
                            for (int i = 0; i < multiple_signature_base; i++) {
                                signatureSizeEvaluator.simulate(u_cluster_signature[uau_id][i]);
                            }
                        } else {
                            signatureSizeEvaluator.simulateBits(u_signature_size);
                            signatureSizeEvaluator.simulateBits(16);
                            for (int i = 0; i < u_cluster_signature[uau_id].length; i++) {
                                signatureSizeEvaluator.simulate(u_cluster_signature[uau_id][i]);
                            }
                        }
                    }
                    result += Math.ceil(signatureSizeEvaluator.getSizeInBits() / 8);

                    if (dataset_header.isBlockHeader()) {
                        result += byte_offset_size;
                    } else {
                        final int num_descriptors = dataset_header.getNumberOfDescriptors(DATA_CLASS.CLASS_U);
                        for (int desc_id = 0; desc_id < num_descriptors; desc_id++) {
                            result += byte_offset_size;
                        }
                    }
                }
            }

            return result;
        }catch (DataClassNotFoundException e){
            throw new InternalError(e);
        }
    }

    public AU_Id_triplet getAuIdTriplet(final AccessUnitHeader accessUnitHeader, final long readerPosition)
            throws DataClassNotFoundException {
        int accessUnitId = accessUnitHeader.getAccessUnitId();
        DataClassIndex dataClassIndex = dataset_header.getClassIndex(accessUnitHeader.getAUType());
        if(accessUnitHeader.getAUType() != DATA_CLASS.CLASS_U){
            for(short sequence_i=0; sequence_i<au_start_position.length; sequence_i++){
                if(accessUnitId < au_byte_offset[sequence_i][dataClassIndex.getIndex()].length) {
                    if ((readerPosition - datasetInitialPosition) ==
                            au_byte_offset[sequence_i][dataClassIndex.getIndex()][accessUnitId]) {
                        return new AU_Id_triplet(
                                new DatasetSequenceIndex(sequence_i),
                                dataClassIndex,
                                accessUnitId
                        );
                    }
                }
            }
            return null;
        }else{
            throw new UnsupportedOperationException();
        }

    }

    public long getBlockByteOffset(
            final DatasetSequenceIndex sequenceIndex,
            final DataClassIndex classIndex,
            final int auId,
            final DescriptorIndex descId) {

        return block_byte_offset[sequenceIndex.getIndex()][classIndex.getIndex()][auId][descId.getDescriptor_index()];
    }

    public Long getNextBlockStart(
            final DatasetSequenceIndex seq,
            final DataClassIndex class_index,
            final int auId,
            final DescriptorIndex descriptorIndex) {

        final long currentBlockStart = getBlockByteOffset(seq, class_index, auId, descriptorIndex);

        TreeSet<Long> descriptorPositions = blockStartsPerClassPerDescriptor.get(class_index.getIndex())
                .get(descriptorIndex.getDescriptor_index());

        if(!descriptorPositions.contains(currentBlockStart)){
            throw new InternalError();
        }

        return descriptorPositions.higher(currentBlockStart);
    }

    public long getAuStart(
            final DatasetSequenceIndex seq,
            final DataClassIndex classIndex,
            final int auId) {

        try {
            return au_start_position[seq.getIndex()][classIndex.getIndex()][auId];
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getAuEnd(
            final DatasetSequenceIndex seq,
            final DataClassIndex classIndex,
            final int auId) {

        return au_end_position[seq.getIndex()][classIndex.getIndex()][auId];
    }

    void setDatasetInitialPosition(final long initialPosition){
        datasetInitialPosition = initialPosition;
    }

    public void setDefaultAUOffset(final long defaultAUOffset){
        this.defaultAUOffset = defaultAUOffset;
    }
}

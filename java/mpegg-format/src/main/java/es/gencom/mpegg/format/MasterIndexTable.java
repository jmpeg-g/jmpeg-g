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
import java.util.Arrays;
import java.util.Comparator;
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
        final int byte_offset_size = dataset_header.isByteOffsetSize() ? 64 : 32;
        final byte posSize = (byte) (dataset_header.isByteOffsetSize() ? 40 : 32);

        for (short seq = 0; seq < au_start_position.length; seq++) {
            for (int ci = 0; ci < au_start_position[seq].length; ci++) {
                for (int au_id = 0; au_id < au_start_position[seq][ci].length; au_id++) {

                    writer.writeBits(au_byte_offset[seq][ci][au_id], byte_offset_size);
                    writer.writeBits(au_start_position[seq][ci][au_id], posSize);
                    writer.writeBits(au_end_position[seq][ci][au_id], posSize);

                    if(dataset_header.getDatasetType() == REFERENCE){
                        writer.writeBits(ref_sequence_id[seq][ci][au_id].getSequenceIdentifier(), 16);
                        writer.writeBits(ref_start_position[seq][ci][au_id], posSize);
                        writer.writeBits(ref_end_position[seq][ci][au_id], posSize);
                    }



                    if (dataset_header.isMultipleAlignment()) {
                        writer.writeBits(extended_au_start_position[seq][ci][au_id], posSize);
                        writer.writeBits(extended_au_end_position[seq][ci][au_id], posSize);
                    }


                    if (!dataset_header.isBlockHeaderFlag()) {
                        for (int desc_id = 0; desc_id < block_byte_offset[seq][ci][au_id].length; desc_id++) {
                            writer.writeBits(block_byte_offset[seq][ci][au_id][desc_id], byte_offset_size);
                        }
                    }
                }
            }
        }

        final long num_u_access_units = dataset_header.getNumberUAccessUnits();
        final int multiple_signature_base = dataset_header.getMultipleSignatureBase();
        final int u_signature_size = dataset_header.getUnmappedSignatureSize() & 0xFF;

        for (int uau_id = 0; uau_id < num_u_access_units; uau_id++) {
            writer.writeBits(u_au_byte_offset[uau_id], byte_offset_size);

            if(dataset_header.getDatasetType() == REFERENCE){
                writer.writeBits(u_ref_sequence_id[uau_id].getSequenceIdentifier(), 16);
                writer.writeBits(u_ref_start_position[uau_id], posSize);
                writer.writeBits(u_ref_end_position[uau_id], posSize);
            } else {
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
            }
            if (!dataset_header.isBlockHeaderFlag()) {
                for (int desc_id = 0; desc_id < u_block_byte_offset[uau_id].length; desc_id++) {
                    writer.writeBits(u_block_byte_offset[uau_id][desc_id], byte_offset_size);
                }
            }
        }

    }

    @Override
    public MasterIndexTable read(final MPEGReader reader, final long size) throws IOException, InvalidMPEGGFileException, ParsedSizeMismatchException {
        final int byte_offset_size = dataset_header.isByteOffsetSize() ? 64 : 32;
        final byte posSize = (byte) (dataset_header.isByteOffsetSize() ? 40 : 32);

        try {

            final int seq_count = dataset_header.getReferenceSequencesCount();
            final int num_classes_aligned = dataset_header.getNumberAlignedClasses();

            if(dataset_header.getDatasetType() == REFERENCE){
                ref_sequence_id = new SequenceIdentifier[seq_count][num_classes_aligned][];
                ref_start_position = new long[seq_count][num_classes_aligned][];
                ref_end_position = new long[seq_count][num_classes_aligned][];
            }else{
                ref_sequence_id = null;
                ref_start_position = null;
                ref_end_position = null;
            }

            au_start_position = new long[seq_count][num_classes_aligned][];
            au_end_position = new long[seq_count][num_classes_aligned][];

            if (dataset_header.isMultipleAlignment()) {
                extended_au_start_position = new long[seq_count][num_classes_aligned][];
                extended_au_end_position = new long[seq_count][num_classes_aligned][];
            }

            au_byte_offset = new long[seq_count][num_classes_aligned][];


            if (!dataset_header.isBlockHeaderFlag()) {
                block_byte_offset = new long[seq_count][num_classes_aligned][][];

                blockStartsPerClassPerDescriptor = new ArrayList<>(DATA_CLASS.values().length);

                int class_i = 0;
                for (DATA_CLASS data_class : dataset_header.getClassIDs()) {
                    final int num_descriptors = dataset_header.getNumberOfDescriptors(data_class);
                    blockStartsPerClassPerDescriptor.add(new ArrayList<>(num_descriptors));

                    for (int descriptor_i = 0; descriptor_i < num_descriptors; descriptor_i++) {
                        blockStartsPerClassPerDescriptor.get(class_i).add(new TreeSet<>());
                    }

                    class_i++;
                }
            }

            for (short seq = 0; seq < seq_count; seq++) {
                final int seq_block = (int) dataset_header.getReferenceSequenceBlocks(new DatasetSequenceIndex(seq));
                int ci = 0;
                for (DATA_CLASS data_class : dataset_header.getClassIDs()) {
                    if (data_class == DATA_CLASS.CLASS_U) {
                        continue;
                    }
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

                    if (!dataset_header.isBlockHeaderFlag()) {
                        block_byte_offset[seq][ci] = new long[seq_block][];
                    }

                    for (int au_id = 0; au_id < seq_block; au_id++) {
                        au_byte_offset[seq][ci][au_id] = reader.readBits(byte_offset_size);
                        au_start_position[seq][ci][au_id] = reader.readBits(posSize);
                        au_end_position[seq][ci][au_id] = reader.readBits(posSize);

                        if(au_end_position[seq][ci][au_id] <= au_start_position[seq][ci][au_id]){
                            if(au_byte_offset[seq][ci][au_id] != (1L<<byte_offset_size)-1){
                                throw new IllegalArgumentException();
                            }
                        }

                        if (dataset_header.getDatasetType() == REFERENCE){
                            ref_sequence_id[seq][ci][au_id] = new SequenceIdentifier((int) reader.readBits(16));
                            ref_start_position[seq][ci][au_id] = reader.readBits(posSize);
                            ref_end_position[seq][ci][au_id] = reader.readBits(posSize);
                        }

                        if (dataset_header.isMultipleAlignment()) {
                            extended_au_start_position[seq][ci][au_id] = reader.readBits(posSize);
                            extended_au_end_position[seq][ci][au_id] = reader.readBits(posSize);
                        }


                        if (!dataset_header.isBlockHeaderFlag()) {
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
                }
            }

            final int num_u_access_units = (int) dataset_header.getNumberUAccessUnits();
            final int multiple_signature_base = dataset_header.getMultipleSignatureBase();
            final int u_signature_size = dataset_header.getUnmappedSignatureSize() & 0xFF;

            if(num_u_access_units != 0) {
                u_au_byte_offset = new long[num_u_access_units];
                DataClassIndex dataClassIndex = dataset_header.getClassIndex(DATA_CLASS.CLASS_U);
                if(!dataset_header.isBlockHeaderFlag()) {
                    u_block_byte_offset = new long[num_u_access_units][dataset_header.getNumberOfDescriptors(DATA_CLASS.CLASS_U)];
                }
                if (dataset_header.getDatasetType() == REFERENCE) {
                    u_ref_sequence_id = new SequenceIdentifier[num_u_access_units];
                    u_ref_start_position = new long[num_u_access_units];
                    u_ref_end_position = new long[num_u_access_units];
                }

                for(int uau_id = 0; uau_id < num_u_access_units; uau_id++){
                    u_au_byte_offset[uau_id] = reader.readBits(byte_offset_size);
                    if(dataset_header.getDatasetType() == REFERENCE) {
                        u_ref_sequence_id[uau_id] = new SequenceIdentifier((int) reader.readBits(16));
                        u_ref_start_position[uau_id] = reader.readBits(posSize);
                        u_ref_end_position[uau_id] = reader.readBits(posSize);
                    } else {
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
                    if(!dataset_header.isBlockHeaderFlag()) {
                        for (
                                int descriptor_i = 0;
                                descriptor_i < dataset_header.getNumberOfDescriptors(DATA_CLASS.CLASS_U);
                                descriptor_i++
                        ) {
                            u_block_byte_offset[uau_id][descriptor_i] = reader.readBits(byte_offset_size);
                            blockStartsPerClassPerDescriptor.get(dataClassIndex.getIndex()).get(descriptor_i).add(
                                    u_block_byte_offset[uau_id][descriptor_i]
                            );
                        }
                    }
                }
            }

            reader.align();
            System.out.println(reader.getPosition());
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
        long result = 0;
        final int byte_offset_size = dataset_header.isByteOffsetSize() ? 8 : 4;
        final byte posSize = (byte) (dataset_header.isByteOffsetSize() ? 5 : 4);

        for (short seq = 0; seq < au_start_position.length; seq++) {
            for (int ci = 0; ci < au_start_position[seq].length; ci++) {
                for (int au_id = 0; au_id < au_start_position[seq][ci].length; au_id++) {
                    result += byte_offset_size; //au_byte_offset
                    result += posSize; //au_start
                    result += posSize; //au_end

                    if(dataset_header.getDatasetType() == REFERENCE){
                        result += 2; //ref_sequence
                        result += posSize; //ref_start_pos
                        result += posSize; //ref_end_pos
                    }

                    if (dataset_header.isMultipleAlignment()) {
                        result += posSize; //extended_au_start
                        result += posSize; //extended_au_end
                    }

                    if (!dataset_header.isBlockHeaderFlag()) {
                        for (int desc_id = 0; desc_id < block_byte_offset[seq][ci][au_id].length; desc_id++) {
                            result += byte_offset_size; //block_byte_offset
                        }
                    }
                }
            }
        }

        final long num_u_access_units = dataset_header.getNumberUAccessUnits();
        final int multiple_signature_base = dataset_header.getMultipleSignatureBase();
        final int u_signature_size = dataset_header.getUnmappedSignatureSize() & 0xFF;

        for (int uau_id = 0; uau_id < num_u_access_units; uau_id++) {
            result += byte_offset_size; //u_au_byte_offset

            if(dataset_header.getDatasetType() == REFERENCE){
                result += 2; //u_ref_sequence_id
                result += posSize; //u_ref_start_position
                result += posSize; //u_ref_end_position
            } else {
                if (multiple_signature_base != 0) {
                    SignatureSizeEvaluator signatureSizeEvaluator =
                            new SignatureSizeEvaluator(
                                    dataset_header.getUnmappedSignatureLength(),
                                    u_signature_size,
                                    dataset_header.getAlphabetId().bits
                            );

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

                    result += Math.ceil(signatureSizeEvaluator.getSizeInBits() / 8);
                }
            }
            if (!dataset_header.isBlockHeaderFlag()) {
                for (int desc_id = 0; desc_id < u_block_byte_offset[uau_id].length; desc_id++) {
                    result += byte_offset_size; //u_offset
                }
            }
        }
        return result;
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

    public Long getUnmappedNextBlockStart(
            final int auId,
            final DescriptorIndex descriptorIndex) {

        final long currentBlockStart = getUnmappedBlockByteOffset(auId, descriptorIndex);
        DataClassIndex index;
        try {
            index = dataset_header.getClassIndex(DATA_CLASS.CLASS_U);
        } catch (DataClassNotFoundException e){
            throw new IllegalArgumentException(e);
        }

        TreeSet<Long> descriptorPositions = blockStartsPerClassPerDescriptor
                .get(index.getIndex())
                .get(descriptorIndex.getDescriptor_index());

        if(!descriptorPositions.contains(currentBlockStart)){
            throw new InternalError();
        }

        return descriptorPositions.higher(currentBlockStart);
    }

    public SequenceIdentifier getAuRefSequence(
            final DatasetSequenceIndex seq,
            final DataClassIndex classIndex,
            final int auId) {

        return ref_sequence_id[seq.getIndex()][classIndex.getIndex()][auId];
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

    public long getAuRefStart(
            final DatasetSequenceIndex seq,
            final DataClassIndex classIndex,
            final int auId) {

        try {
            return ref_start_position[seq.getIndex()][classIndex.getIndex()][auId];
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

    public long getAuRefEnd(
            final DatasetSequenceIndex seq,
            final DataClassIndex classIndex,
            final int auId) {

        return ref_end_position[seq.getIndex()][classIndex.getIndex()][auId];
    }

    void setDatasetInitialPosition(final long initialPosition){
        datasetInitialPosition = initialPosition;
    }

    public void setDefaultAUOffset(final long defaultAUOffset){
        this.defaultAUOffset = defaultAUOffset;
    }

    public boolean areBlocksOrdered() {
        if(block_byte_offset == null){
            throw new IllegalArgumentException();
        }
        final long posNotPresent =  (dataset_header.isByteOffsetSize() ? 0xffffffffffL : 0xffffffffL);
        final long offsetNotPresent = (dataset_header.isByteOffsetSize() ? 0xffffffffffffffffL : 0xffffffffL);
        for(int seq_i=0; seq_i<au_start_position.length; seq_i++){
            for(int class_i=0; class_i < au_start_position[seq_i].length; class_i++){
                int num_aus = au_start_position[seq_i][class_i].length;
                if(num_aus <= 1){
                    continue;
                }
                int num_descriptors = block_byte_offset[seq_i][class_i][0].length;

                for(int desc_i=0; desc_i < num_descriptors; desc_i++){
                    long[][] entries = new long[num_aus][2];

                    int num_effective_entries = 0;
                    for(int au_i=0; au_i < num_aus; au_i++){
                        if(
                                au_start_position[seq_i][class_i][au_i] == posNotPresent ||
                                block_byte_offset[seq_i][class_i][au_i][desc_i] == offsetNotPresent
                        ){
                            continue;
                        }
                        entries[num_effective_entries] = new long[]{
                                au_start_position[seq_i][class_i][au_i],
                                block_byte_offset[seq_i][class_i][au_i][desc_i]
                        };
                        num_effective_entries++;
                    }
                    if(num_effective_entries == 0){
                        continue;
                    }
                    entries = Arrays.copyOf(entries, num_effective_entries);
                    Arrays.sort(entries, new Comparator<long[]>() {
                        @Override
                        public int compare(long[] o1, long[] o2) {
                            long auStartItemOne = o1[0];
                            long auStartItemTwo = o2[0];

                            return Long.compare(auStartItemOne, auStartItemTwo);
                        }
                    });

                    long prior = entries[0][1];
                    for(int entry_i=1; entry_i < entries.length; entry_i++){
                        if(prior >= entries[entry_i][1]){
                            return false;
                        }
                        prior = entries[entry_i][1];
                    }
                }
            }
        }
        return true;
    }

    public long getUnmappedBlockByteOffset(int unmappedBlock_i, DescriptorIndex descriptorIndex) {
        return u_block_byte_offset[unmappedBlock_i][descriptorIndex.getDescriptor_index()];
    }
}

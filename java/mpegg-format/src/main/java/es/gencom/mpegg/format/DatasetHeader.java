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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * <p>
 * ISO/IEC 23092-1 Dataset Header implementation (6.5.2.2).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DatasetHeader extends GenInfo<DatasetHeader> {
    
    public final static String KEY = "dthd";
    
    private short dataset_group_id;                // u(8)
    private int dataset_id;                     // u(16)
    private byte[] version;                       // c(4)
    private boolean multiple_alignment_flag;      // u(1)
    private boolean byte_offset_size_flag;        // u(1)
    private boolean non_overlapping_au_range;     // u(1)
    private boolean pos_40_bits;                  // u(1)
    private boolean block_header_flag;            // u(1)
    private boolean mit_flag;                     // u(1)
    private boolean cc_mode_flag;                 // u(1)
    private boolean ordered_blocks_flag;          // u(1)

    private short reference_id;                    // u(8)
    private SequenceIdentifier[] seq_ids;                      // u[](16)
    private long[] seq_blocks;
    private DatasetType dataset_type;

    private DATA_CLASS[] class_ids;
    private byte[][] desc_ids;
    private ALPHABET alphabet_id;
    private long num_u_access_units;
    private long num_u_clusters;
    private int multiple_signature_base;
    private byte u_signature_size;
    private boolean u_signature_constant_length;
    private short u_signature_length;

    private int[] thresholds;
    
    public DatasetHeader() {
        super(KEY);
        version = new byte[]{'1','9','0','0'};
    }

    public DatasetHeader(
            short dataset_group_id,
            int dataset_id,
            byte[] version,
            boolean multiple_alignment_flag,
            boolean byte_offset_size_flag,
            boolean non_overlapping_au_range,
            boolean pos_40_bits,
            boolean block_header_flag,
            boolean MIT_flag,
            boolean CC_mode_flag,
            boolean ordered_blocks_flag,
            short reference_id,
            SequenceIdentifier[] seqId,
            long[] seq_blocks,
            DatasetType dataset_type,
            DATA_CLASS[] dataClasses,
            byte[][] descriptorIdentifiers,
            ALPHABET alphabet,
            long num_u_access_units,
            long num_u_clusters,
            int multiple_signature_base,
            byte u_signature_size,
            boolean u_signature_constant_length,
            short u_signature_length,
            int[] thresholds) {
        
        super(KEY);

        if(version.length != 4){
            throw new IllegalArgumentException();
        }
        if(seqId.length != seq_blocks.length){
            throw new IllegalArgumentException();
        }
        if(seqId.length != thresholds.length){
            throw new IllegalArgumentException();
        }
        if(!block_header_flag){
            if(dataClasses.length != descriptorIdentifiers.length){
                throw new IllegalArgumentException();
            }
        }


        this.dataset_group_id = dataset_group_id;
        this.dataset_id = dataset_id;
        this.version = Arrays.copyOf(version, version.length);
        this.byte_offset_size_flag = byte_offset_size_flag;
        this.multiple_alignment_flag = multiple_alignment_flag;
        this.non_overlapping_au_range = non_overlapping_au_range;
        this.pos_40_bits = pos_40_bits;
        this.block_header_flag = block_header_flag;
        this.mit_flag = MIT_flag;
        this.cc_mode_flag = CC_mode_flag;
        this.ordered_blocks_flag = ordered_blocks_flag;
        this.reference_id = reference_id;
        this.seq_ids = seqId;
        this.seq_blocks = seq_blocks;
        this.dataset_type = dataset_type;
        this.class_ids = dataClasses;
        this.desc_ids = descriptorIdentifiers;
        this.alphabet_id = alphabet;
        this.num_u_access_units = num_u_access_units;
        this.num_u_clusters = num_u_clusters;
        this.multiple_signature_base = multiple_signature_base;
        this.u_signature_size = u_signature_size;
        this.u_signature_constant_length = u_signature_constant_length;
        this.u_signature_length = u_signature_length;
        this.thresholds = thresholds;
    }
    
    public short getDatasetGroupId() {
        return dataset_group_id;
    }

    public int getDatasetId() {
        return dataset_id;
    }

    public byte[] getVersion() {
        return version;
    }

    public boolean isByteOffsetSize() {
        return byte_offset_size_flag;
    }

    public boolean isNonOverlappingAURange() {
        return non_overlapping_au_range;
    }
    
    public boolean isBlockHeaderFlag() {
        return block_header_flag;
    }

    public void setBlockHeaderFlag(final boolean block_header_flag) {
        this.block_header_flag = block_header_flag;
    }
    
    public boolean isMIT() {
        return mit_flag;
    }

    public boolean isCCMode() {
        return cc_mode_flag;
    }

    public boolean isOrderedBlocks() {
        return ordered_blocks_flag;
    }

    public short getReferenceId() {
        return reference_id;
    }
    
    public void setReferenceId(byte reference_id) {
        this.reference_id = reference_id;
    }
    
    /**
     * Get the number of reference sequences used in this Dataset.
     * 
     * @return 'seq_count' property
     */
    public int getReferenceSequencesCount() {
        return seq_ids.length;
    }

    public SequenceIdentifier[] getSeqIds(){
        return seq_ids;
    }
    
    /**
     * Get an identification number of the reference sequence.
     * 
     * @param sequenceIndex an index such that 0 &lt; = index &lt; 'seq_count'
     * @return a unique identification number of the reference used in the Dataset.
     */
    public SequenceIdentifier getReferenceSequenceId(DatasetSequenceIndex sequenceIndex) {
        int idx = sequenceIndex.getIndex();
        if (idx < 0 || idx >= seq_ids.length) {
            throw new IllegalArgumentException();
        }
        return seq_ids[idx];
    }
    

    /**
     * Get a number of blocks for the sequence.
     * 
     * @param datasetSequenceIndex an index such that 0 &lt; = index &lt; 'seq_count'
     * @return a number of blocks for the sequence (0 means 'unspecified')
     */
    public long getReferenceSequenceBlocks(DatasetSequenceIndex datasetSequenceIndex) {
        short idx = datasetSequenceIndex.getIndex();
        if (idx < 0 || idx >= seq_blocks.length) {
            throw new IllegalArgumentException();
        }
        return seq_blocks[idx];
    }
    
    public DatasetType getDatasetType() {
        return dataset_type;
    }
    
    public void setDatasetType(DatasetType dataset_type) {
        this.dataset_type = dataset_type;
    }
    
    public int getNumberOfClasses() {
        return class_ids.length;
    }

    public int getNumberAlignedClasses() {
        for(int i = 0; i < class_ids.length; i++){
            if(class_ids[i] == DATA_CLASS.CLASS_U){
                return class_ids.length - 1;
            }
        }
        return class_ids.length;
    }

    /**
     * Get the maximum number of descriptors per class encoded in the dataset.
     * 
     * @param dataClass the Data Class for which the number of descriptors is consulted
     * @return the number of descriptors for the class
     */
    public int getNumberOfDescriptors(DATA_CLASS dataClass) throws DataClassNotFoundException {
        DataClassIndex index = getClassIndex(dataClass);
        return desc_ids[index.getIndex()] != null ? desc_ids[index.getIndex()].length : 0;
    }

    public byte[] getDescriptors(DATA_CLASS dataClass) throws DataClassNotFoundException {
        DataClassIndex index = getClassIndex(dataClass);
        if(desc_ids[index.getIndex()] == null){
            return new byte[0];
        }
        return desc_ids[index.getIndex()];
    }
    
    public byte getDescriptorId(
            DATA_CLASS dataClass,
            DescriptorIndex descriptorIndex) throws DataClassNotFoundException {
        
        DataClassIndex classIndex = getClassIndex(dataClass);
        if (desc_ids[classIndex.getIndex()] == null || 
            descriptorIndex.getDescriptor_index() >= desc_ids[classIndex.getIndex()].length) {
            
            throw new IllegalArgumentException();
        }
        
        return desc_ids[classIndex.getIndex()][descriptorIndex.getDescriptor_index()];
    }

    public DescriptorIndex getDescriptorIndex(
            DATA_CLASS dataClass,
            byte descriptorIdentifier
    ) throws DataClassNotFoundException, NoSuchFieldException {
        DataClassIndex class_index = getClassIndex(dataClass);
        byte numberDescriptor = (byte) desc_ids[class_index.getIndex()].length;
        for(byte descriptor_i = 0; descriptor_i < numberDescriptor; descriptor_i++) {
            if(desc_ids[class_index.getIndex()][descriptor_i] == descriptorIdentifier) {
                return new DescriptorIndex(descriptor_i);
            }
        }
        throw new NoSuchFieldException();
    }
    
    public ALPHABET getAlphabetId() {
        return alphabet_id;
    }
    
    public long getNumberOfUnmappedClusters() {
        return num_u_clusters;
    }
    
    public boolean isUnmappedSignatureConstantLength() {
        return u_signature_length >= 0;
    }
    
    public byte getUnmappedSignatureSize() {
        return u_signature_size;
    }
    
    public int getMultipleSignatureBase() {
        return multiple_signature_base;
    }
    
    public void setMultipleSignatureBase(final int multiple_signature_base) {
        this.multiple_signature_base = multiple_signature_base;
    }
    
    
    /**
     * Get the length of cluster signature as number of nucleotides.
     * 
     * @return 'u_signature_length' property.
     */
    public short getUnmappedSignatureLength() {
        return u_signature_length;
    }
    
    public void setUnmappedSignatureLength(final byte u_signature_length) {
        this.u_signature_length = u_signature_length;
    }

    public void setNumberOfUnalignedAccessUnits(final int num_u_access_units) {
        this.num_u_access_units = num_u_access_units;
    }
    
    public int getThreshold(DatasetSequenceIndex sequenceIndex) {
        if (sequenceIndex.getIndex() >= seq_ids.length) {
            throw new IndexOutOfBoundsException();
        }
        return thresholds[sequenceIndex.getIndex()];
    }
    
    public DATA_CLASS getClassId(DataClassIndex class_i){
        return class_ids[class_i.getIndex()];
    }

    public DataClassIndex getClassIndex(DATA_CLASS dataClass) throws DataClassNotFoundException {
        for(byte class_i = 0; class_i < class_ids.length; class_i++){
            if(class_ids[class_i].equals(dataClass)){
                return new DataClassIndex(class_i);
            }
        }
        throw new DataClassNotFoundException(dataClass);
    }

    public DATA_CLASS[] getClassIDs() {
        return class_ids;
    }

    public void setClassIDs(DATA_CLASS[] class_ids) {
        this.class_ids = class_ids;
    }

    public DatasetSequenceIndex getSequenceIndex(SequenceIdentifier sequenceId) throws SequenceNotAvailableException {
        for(short sequenceIndex = 0; sequenceIndex < seq_ids.length; sequenceIndex++ ) {
            if(seq_ids[sequenceIndex].equals(sequenceId)) {
                return new DatasetSequenceIndex(sequenceIndex);
            }
        }
        throw new SequenceNotAvailableException();
    }

    public boolean isPos40bits() {
        return pos_40_bits;
    }

    public void setPos40bits(final boolean pos_40_bits) {
        this.pos_40_bits = pos_40_bits;
    }
    
    public boolean isMultipleAlignment() {
        return multiple_alignment_flag;
    }

    public void setMultipleAlignment(final boolean multiple_alignment_flag) {
        this.multiple_alignment_flag = multiple_alignment_flag;
    }
    
    public long getNumberUAccessUnits() {
        return num_u_access_units;
    }

    public void setOrderedBlocks(boolean orderedBlocks) {
        ordered_blocks_flag = orderedBlocks;
    }
    
    @Override
    public void write(final MPEGWriter writer) throws IOException {

        writer.writeBits(dataset_group_id, 8);
        writer.writeBits(dataset_id, 16);
        writer.writeBytes(version);
        writer.writeBoolean(multiple_alignment_flag);
        writer.writeBoolean(byte_offset_size_flag);
        writer.writeBoolean(non_overlapping_au_range);
        writer.writeBoolean(pos_40_bits);
        writer.writeBoolean(block_header_flag);
        if (block_header_flag) {
            writer.writeBoolean(mit_flag);
            writer.writeBoolean(cc_mode_flag);
        } else {
            writer.writeBoolean(ordered_blocks_flag);
        }
        writer.writeShort((short) seq_blocks.length);
        if (seq_blocks.length > 0) {
            writer.writeByte((byte)reference_id);
            for (int seq_i = 0; seq_i < seq_blocks.length; seq_i++) {
                writer.writeShort(
                        (short) seq_ids[seq_i].getSequenceIdentifier()
                );
            }
            for (int seq_i = 0; seq_i < seq_blocks.length; seq_i++) {
                writer.writeInt((int) seq_blocks[seq_i]);
            }
        }

        dataset_type.write(writer);

        if (isMIT()) {
            writer.writeBits(class_ids.length, 4);
            for (int class_i = 0; class_i < class_ids.length; class_i++) {
                writer.writeBits(class_ids[class_i].ID, 4);
                if (!block_header_flag) {
                    writer.writeBits(getNumberOfDescriptors(class_ids[class_i]), 5);
                    for (int desc_i = 0; desc_i < getNumberOfDescriptors(class_ids[class_i]); desc_i++) {
                        writer.writeBits(desc_ids[class_i][desc_i], 7);
                    }
                }
            }
        }

        writer.writeByte(alphabet_id.id);
        writer.writeInt((int) num_u_access_units);
        if (num_u_access_units > 0) {
            writer.writeInt((int) num_u_clusters);
            writer.writeBits(multiple_signature_base, 31);
            if (multiple_signature_base > 0) {
                writer.writeBits(u_signature_size, 6);
            }
            writer.writeBoolean(u_signature_constant_length);
            if (u_signature_constant_length) {
                writer.writeByte((byte) u_signature_length);
            }
        }

        if (seq_ids.length > 0) {
            writer.writeBoolean(true);
            int previous_writer_threshold = thresholds[0];
            writer.writeBits(thresholds[0], 31);
            for (int seq_i = 1; seq_i < seq_ids.length; seq_i++) {
                int current_threshold = thresholds[seq_i];
                if (current_threshold != previous_writer_threshold) {
                    writer.writeBoolean(true);
                    writer.writeBits(current_threshold, 31);
                    previous_writer_threshold = current_threshold;
                } else {
                    writer.writeBoolean(false);
                }
            }
        }

        writer.flush();
    }
    
    @Override
    public DatasetHeader read(final MPEGReader reader, long size) throws IOException, ParsedSizeMismatchException {

        dataset_group_id = (byte)(reader.readBits(8));
        dataset_id = (short)(reader.readBits(16));

        ByteBuffer versionByteBuffer = reader.readByteBuffer(4);
        versionByteBuffer.position(0);
        version = new byte[4];
        versionByteBuffer.get(version);
        multiple_alignment_flag = reader.readBoolean();
        byte_offset_size_flag = reader.readBoolean();
        non_overlapping_au_range = reader.readBoolean();
        pos_40_bits = reader.readBoolean();
        block_header_flag = reader.readBoolean();
        
        if (block_header_flag) {
            mit_flag = reader.readBoolean();
            cc_mode_flag = reader.readBoolean();
        } else {
            mit_flag = true;
            ordered_blocks_flag = reader.readBoolean();
        }

        final int seq_count = (int)(reader.readBits(16));

        if(seq_count > 0){
            reference_id = (byte)(reader.readBits(8));
            seq_ids = new SequenceIdentifier[seq_count];
            for (int i = 0; i < seq_count; i++) {
                final short seq_id = (short)(reader.readBits(16));
                seq_ids[i] = new SequenceIdentifier(seq_id);
            }

            seq_blocks = new long[seq_count];
            for (int i = 0; i < seq_count; i++) {
                final int seq_block = (int)(reader.readBits(32));
                seq_blocks[i] = seq_block;
            }
        } else {
            reference_id = 0;
            seq_ids = new SequenceIdentifier[0];
            seq_blocks = new long[0];
        }

        dataset_type = DatasetType.read(reader);

        if(mit_flag) {
            final byte num_classes = (byte)reader.readBits(4);
            class_ids = new DATA_CLASS[num_classes];
            if(!block_header_flag){
                desc_ids = new byte[num_classes][];
            }
            for (int class_i = 0; class_i < num_classes; class_i++) {
                class_ids[class_i] = DATA_CLASS.getDataClass((byte) reader.readBits(4));
                if(!block_header_flag) {
                    final int num_descriptors = (int) reader.readBits(5);
                    desc_ids[class_i] = new byte[num_descriptors];
                    for (int j = 0; j < num_descriptors; j++) {
                        final byte descriptor_id = (byte) reader.readBits(7);
                        desc_ids[class_i][j] = descriptor_id;
                    }
                }
            }
        }
        alphabet_id = ALPHABET.getAlphabet(reader.readByte());
        num_u_access_units = reader.readInt();
        if(num_u_access_units > 0){
            num_u_clusters = reader.readInt();
            multiple_signature_base = (int) reader.readBits(31);
            if(multiple_signature_base > 0){
                u_signature_size = (byte) reader.readBits(6);
            }
            u_signature_constant_length = reader.readBoolean();
            if(u_signature_constant_length){
                u_signature_length = reader.readUnsignedByte();
            }
        }
        if(seq_count > 0){
            thresholds = new int[seq_count];
            if(!reader.readBoolean()){
                throw new IllegalArgumentException("First threshold flag must be one");
            }

            int previous_read_threshold = (int) reader.readBits(31);
            thresholds[0]=previous_read_threshold;
            for(int seq_i=1; seq_i<seq_count; seq_i++){
                if(reader.readBoolean()){
                    previous_read_threshold = (int) reader.readBits(31);
                }
                thresholds[seq_i]=previous_read_threshold;
            }
        }
        reader.align();
        if(size() != size){
            throw new ParsedSizeMismatchException();
        }
        
        return this;
    }
    
    @Override
    protected long size() {
        long sizeInBits = 0;
        sizeInBits += 8; //datasetGroupId
        sizeInBits += 16; //dataset_Id
        sizeInBits += 4*8; //version
        sizeInBits += 1; //multiple_alignment_flag
        sizeInBits += 1; //byte_offset_size_flag
        sizeInBits += 1; //non_overlapping_au_range
        sizeInBits += 1; //pos_40_bits
        sizeInBits += 1; //block_header_flag
        if(block_header_flag) {
            sizeInBits += 1; //MIT_flag
            sizeInBits += 1; //ClassContiguous
        } else {
            sizeInBits += 1; //ordered_blocks_flag
        }
        sizeInBits += 16; //seq_count
        if(seq_ids.length > 0) {
            sizeInBits += 8; //referenceID
            sizeInBits += 16 * seq_ids.length;
            sizeInBits += 32 * seq_blocks.length;
        }
        sizeInBits += 4; //datasetType
        if(mit_flag) {
            sizeInBits += 4; //num_classes
            for(int class_i = 0; class_i < class_ids.length; class_i++) {
                sizeInBits += 4; //class id
                if(!block_header_flag) {
                    sizeInBits += 5; //num_descriptors
                    sizeInBits += 7 * desc_ids[class_i].length; //descriptor ids
                }
            }
        }
        sizeInBits += 8; //alphabetId
        sizeInBits += 32; //num_u_access_units
        if(num_u_access_units > 0) {
            sizeInBits += 32; //num_u_clusters
            sizeInBits += 31; //multiple_signature_base
            if(multiple_signature_base > 0) {
                sizeInBits += 6;
            }
            sizeInBits += 1; //u_signature_constant_length
            if(u_signature_constant_length) {
                sizeInBits += 8; //u_signature_length
            }
        }

        if(seq_ids.length > 0) {
            int previous_threshold = thresholds[0];
            sizeInBits += 1; //initial flag
            sizeInBits += 31; //initial value
            for(short seq_i = 1; seq_i < getSeqIds().length; seq_i++) {
                int current_threshold = thresholds[seq_i - 1];
                sizeInBits += 1; //flag
                if(current_threshold != previous_threshold) {
                    sizeInBits += 31;//threshold
                    previous_threshold = current_threshold;
                }
            }
        }

        return (sizeInBits >> 3) + (sizeInBits % 8 != 0 ? 1 : 0);
    }
}

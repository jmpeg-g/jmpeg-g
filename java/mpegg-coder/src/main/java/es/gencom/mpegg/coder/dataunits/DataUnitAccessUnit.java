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

package es.gencom.mpegg.coder.dataunits;

import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.BitReader;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.SubstreamsPerDescriptor;
import es.gencom.mpegg.format.AccessUnitHeader;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DatasetType;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.configuration.EncodingParameters;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class DataUnitAccessUnit extends AbstractDataUnit {
    private final DataUnitAccessUnitHeader header;
    private final Block blocks[];
    private long maximalPositionDecode = -1;

    public long getMaximalPositionDecode() {
        return maximalPositionDecode;
    }

    public SequenceIdentifier getSequenceId() {
        return header.sequence_ID;
    }

    public long getStart() {
        return header.getAu_start_position();
    }

    public long getEnd() {
        return header.getAu_end_position();
    }

    public static class Block {
        private final DESCRIPTOR_ID descriptorId;
        private final Payload[] payloads;

        public Block(DESCRIPTOR_ID descriptorId, Payload[] originalPayloads) {
            this.descriptorId = descriptorId;
            this.payloads = originalPayloads;
        }

        public Block(DESCRIPTOR_ID descriptorId, Payload originalPayload) {
            this.descriptorId = descriptorId;
            this.payloads = new Payload[]{originalPayload};
        }

        public Block copy() throws EOFException {
            Payload[] copyPayloads = new Payload[payloads.length];
            for(int i=0; i<payloads.length; i++){
                copyPayloads[i] = payloads[i];
            }

            return new Block(
                    descriptorId,
                    copyPayloads
            );
        }

        public Payload[] getPayloads() {
            return payloads;
        }

        public BitReader[] getBitReaders(){
            BitReader[] result = new BitReader[payloads.length];
            for(int payload_i = 0; payload_i < payloads.length; payload_i++){
                result[payload_i] = payloads[payload_i];
            }
            return result;
        }

        private long sizeDescriptorSpecificData(){
            long size = 0;
            if(payloads.length == 1) {
                size += payloads[0].size();
            }else{
                for(int i=0; i < payloads.length-1; i++){
                    size += 4;
                    size += payloads[i].size();
                }
                size += payloads[payloads.length-1].size();
            }
            return size;
        }

        public long size(){
            long size = 5;
            size += sizeDescriptorSpecificData();
            return size;
        }

        public void write(MPEGWriter writer) throws IOException {
            writer.writeBoolean(false);
            writer.writeBits(descriptorId.ID, 7);
            writer.writeInt((int) size()-5);
            writer.writePayload(getDescriptorSpecificData());
            writer.flush();
        }

        public byte getDescriptorId() {
            return descriptorId.ID;
        }

        public DESCRIPTOR_ID getDescriptorIdentifier() {
            return descriptorId;
        }

        public Payload getSubstream(int substream_index){
            return payloads[substream_index];
        }

        public Payload getDescriptorSpecificData() {
            ByteBuffer[] byteBuffers = new ByteBuffer[10];
            int numByteBuffers = 0;

            if(payloads.length == 1){
                ByteBuffer[] subsequenceByteBuffers = payloads[0].getByteBuffers();

                for(int i=0; i<subsequenceByteBuffers.length; i++){
                    int position = subsequenceByteBuffers[i].position();
                    subsequenceByteBuffers[i].position(0);
                    byteBuffers[numByteBuffers] = subsequenceByteBuffers[i].slice();
                    subsequenceByteBuffers[i].position(position);

                    numByteBuffers++;
                    if(byteBuffers.length == numByteBuffers){
                        byteBuffers = Arrays.copyOf(byteBuffers, byteBuffers.length*2);
                    }
                }
            }else{
                for(int i=0; i<payloads.length-1; i++){
                    ByteBuffer sizeByteBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
                    sizeByteBuffer.putInt((int)payloads[i].size());
                    sizeByteBuffer.position(0);
                    byteBuffers[numByteBuffers] = sizeByteBuffer;
                    numByteBuffers++;
                    if(byteBuffers.length == numByteBuffers){
                        byteBuffers = Arrays.copyOf(byteBuffers, byteBuffers.length*2);
                    }

                    ByteBuffer[] subsequenceByteBuffers = payloads[i].getByteBuffers();
                    for(
                            int subsequenceByteBuffers_i=0;
                            subsequenceByteBuffers_i<subsequenceByteBuffers.length;
                            subsequenceByteBuffers_i++
                    ){
                        int position = subsequenceByteBuffers[subsequenceByteBuffers_i].position();
                        subsequenceByteBuffers[subsequenceByteBuffers_i].position(0);
                        byteBuffers[numByteBuffers] = subsequenceByteBuffers[subsequenceByteBuffers_i].slice();
                        subsequenceByteBuffers[subsequenceByteBuffers_i].position(position);

                        numByteBuffers++;
                        if(byteBuffers.length == numByteBuffers){
                            byteBuffers = Arrays.copyOf(byteBuffers, byteBuffers.length*2);
                        }
                    }
                }

                ByteBuffer[] subsequenceByteBuffers = payloads[payloads.length-1].getByteBuffers();
                for(
                        int subsequenceByteBuffers_i=0;
                        subsequenceByteBuffers_i<subsequenceByteBuffers.length;
                        subsequenceByteBuffers_i++
                ){
                    int position = subsequenceByteBuffers[subsequenceByteBuffers_i].position();
                    subsequenceByteBuffers[subsequenceByteBuffers_i].position(0);
                    byteBuffers[numByteBuffers] = subsequenceByteBuffers[subsequenceByteBuffers_i].slice();
                    subsequenceByteBuffers[subsequenceByteBuffers_i].position(position);

                    numByteBuffers++;
                    if(byteBuffers.length == numByteBuffers){
                        byteBuffers = Arrays.copyOf(byteBuffers, byteBuffers.length*2);
                    }
                }
            }
            return new Payload(Arrays.copyOf(byteBuffers, numByteBuffers));
        }

        public static Payload[] readSubsequences(
                DESCRIPTOR_ID descriptorIdentifier,
                EncodingParameters encodingParameters,
                DATA_CLASS dataClass,
                MPEGReader reader,
                long blockSize
        ) throws IOException {
            byte numberSubstreams = SubstreamsPerDescriptor.getNumberSubstreams(
                    descriptorIdentifier,
                    encodingParameters,
                    dataClass
            );

            Payload[] payloads;
            if(numberSubstreams == 1){
                payloads = new Payload[1];
                payloads[0] = reader.readPayload(blockSize);
            } else {
                payloads = new Payload[numberSubstreams];
                long currentSize = 0;
                Arrays.fill(payloads, null);
                for(byte i=0; i<numberSubstreams-1; i++) {
                    long size = reader.readUnsignedInt();
                    try {
                        payloads[i] = reader.readPayload(size);
                    }catch (EOFException e){
                        throw e;
                    }
                    currentSize += size + 4;
                }
                payloads[numberSubstreams-1] = reader.readPayload(blockSize-currentSize);
            }
            return payloads;
        }

        static Block readBlock(
                MPEGReader reader,
                EncodingParameters encodingParameters,
                DATA_CLASS dataClass
        ) throws IOException {
            reader.readBoolean();
            DESCRIPTOR_ID descriptorIdentifier = DESCRIPTOR_ID.getDescriptorId((byte) reader.readBits(7));

            long blockSize = reader.readBits(32);

            Payload[] payloads = readSubsequences(
                    descriptorIdentifier,
                    encodingParameters,
                    dataClass,
                    reader,
                    blockSize
            );

            reader.align();

            return new Block(descriptorIdentifier, payloads);
        }


    }


    public static class DataUnitAccessUnitHeader{
        private final long access_unit_ID;
        private final short num_blocks;
        private final short parameter_set_ID;
        private final DATA_CLASS AU_type;
        private final long read_count ;

        private final int mm_threshold;
        private final long mm_count;

        private final SequenceIdentifier ref_sequence_id;
        private final long ref_start_position;
        private final long ref_end_position;


        private final SequenceIdentifier sequence_ID;
        private final long au_start_position;
        private final long au_end_position;
        private final long extended_au_start_position;
        private final long extended_au_end_position;

        public DataUnitAccessUnitHeader(AccessUnitHeader accessUnitHeader) {
            this(
                    accessUnitHeader.getAccessUnitID(),
                    accessUnitHeader.getNumBlocks(),
                    accessUnitHeader.getParameterSetID(),
                    accessUnitHeader.getAUType(),
                    accessUnitHeader.getReadsCount(),
                    accessUnitHeader.getMmThreshold(),
                    accessUnitHeader.getMmCount(),
                    accessUnitHeader.getReferenceSequenceID(),
                    accessUnitHeader.getRefStartPosition(),
                    accessUnitHeader.getRefEndPosition(),
                    accessUnitHeader.getSequenceID(),
                    accessUnitHeader.getAUStartPosition(),
                    accessUnitHeader.getAUEndPosition(),
                    accessUnitHeader.getExtendedAUStartPosition(),
                    accessUnitHeader.getExtendedAUEndPosition()
            );
        }

        public long size(DataUnitParameters parameters) {
            long sizeInBits = 0;
            sizeInBits += 32;//access_unit_Id
            sizeInBits += 8; //num_blocks
            sizeInBits += 8; //parameter set Id
            sizeInBits += 4; //AU_type
            sizeInBits += 32; //readCount
            if(AU_type == DATA_CLASS.CLASS_N || AU_type == DATA_CLASS.CLASS_M){
                sizeInBits += 16; //mm_threshold
                sizeInBits += 32; //mm_count
            }
            if(parameters.getDatasetType() == DatasetType.REFERENCE){
                sizeInBits += 16; //ref_sequence_id
                sizeInBits += parameters.isPosSize40() ? 40 : 32; //ref_start_position
                sizeInBits += parameters.isPosSize40() ? 40 : 32; //ref_end_position
            }

            if(AU_type != DATA_CLASS.CLASS_U){
                sizeInBits += 16; //sequence_id
                sizeInBits += parameters.isPosSize40() ? 40 : 32; //au_start_position
                sizeInBits += parameters.isPosSize40() ? 40 : 32; //au_end_position
                if(parameters.isMultiple_alignments_flag()){
                    sizeInBits += parameters.isPosSize40() ? 40:32; //extended_au_start_position
                    sizeInBits += parameters.isPosSize40() ? 40:32; //extended_au_end_position
                }
            }else{
                if(parameters.getMultiple_signature_base() != 0) {
                    throw new UnsupportedOperationException();
                }
            }
            return (long)Math.ceil((double)sizeInBits/8);
        }

        public void write(MPEGWriter writer, DataUnitParameters parameters) throws IOException {
            writer.writeUnsignedInt(access_unit_ID);
            writer.writeUnsignedByte(num_blocks);
            writer.writeUnsignedByte(parameters.getParameter_set_ID());
            writer.writeBits(AU_type.ID, 4);
            writer.writeUnsignedInt(read_count);
            if(AU_type == DATA_CLASS.CLASS_N || AU_type == DATA_CLASS.CLASS_M){
                writer.writeUnsignedShort(mm_threshold);
                writer.writeUnsignedInt(mm_count);
            }

            if(parameters.getDatasetType() == DatasetType.REFERENCE){
                writer.writeShort((short) ref_sequence_id.getSequenceIdentifier());
                writer.writeBits(ref_start_position, parameters.isPosSize40() ? 40:32);
                writer.writeBits(ref_end_position, parameters.isPosSize40() ? 40:32);
            }
            if(AU_type != DATA_CLASS.CLASS_U){
                writer.writeShort((short) sequence_ID.getSequenceIdentifier());
                writer.writeBits(au_start_position, parameters.isPosSize40() ? 40:32);
                writer.writeBits(au_end_position, parameters.isPosSize40() ? 40:32);
                if(parameters.isMultiple_alignments_flag()){
                    writer.writeBits(extended_au_start_position, parameters.isPosSize40() ? 40:32);
                    writer.writeBits(extended_au_end_position, parameters.isPosSize40() ? 40:32);
                }
            }else{
                if(parameters.getMultiple_signature_base() != 0) {
                    throw new UnsupportedOperationException();
                }
            }
            writer.align();
        }

        public static DataUnitAccessUnitHeader read(
                MPEGReader reader,
                DataUnits dataUnits
        ) throws IOException {
            long access_unit_ID = reader.readUnsignedInt();
            short num_blocks = reader.readUnsignedByte();
            short parameter_set_ID = (short) reader.readUnsignedByte();

            DataUnitParameters parameters = dataUnits.getParameter(parameter_set_ID);
            byte posLengthInBits = (byte) (parameters.isPosSize40() ? 40 : 32);

            DATA_CLASS AU_type = DATA_CLASS.getDataClass((byte) reader.readBits(4));
            long read_count = reader.readUnsignedInt();
            int ref_sequence_id = 0;
            long ref_start_position = 0;
            long ref_end_position = 0;

            int sequenceId = 0;
            long au_start_position = 0;
            long au_end_position = 0;
            long extended_au_start_position = 0;
            long extended_au_end_position = 0;
            int mm_threshold = 0;
            long mm_count = 0;

            if(AU_type == DATA_CLASS.CLASS_N || AU_type == DATA_CLASS.CLASS_M){
                mm_threshold = reader.readUnsignedShort();
                mm_count = reader.readUnsignedInt();
            }

            if(parameters.getDatasetType() == DatasetType.REFERENCE){
                ref_sequence_id = reader.readUnsignedShort();
                ref_start_position = reader.readBits(posLengthInBits);
                ref_end_position = reader.readBits(posLengthInBits);
            }

            if (AU_type != DATA_CLASS.CLASS_U) {
                sequenceId = reader.readUnsignedShort();
                au_start_position = reader.readBits(posLengthInBits);
                au_end_position = reader.readBits(posLengthInBits);

                if(parameters.isMultiple_alignments_flag()){
                    extended_au_start_position = reader.readBits(posLengthInBits);
                    extended_au_end_position = reader.readBits(posLengthInBits);
                }
            }else{
                if(parameters.getMultiple_signature_base() != 0){
                    throw new UnsupportedOperationException();
                }
            }
            reader.align();

            return new DataUnitAccessUnitHeader(
                access_unit_ID,
                num_blocks,
                parameter_set_ID,
                AU_type,
                read_count,
                mm_threshold,
                mm_count,
                new SequenceIdentifier(ref_sequence_id),
                ref_start_position,
                ref_end_position,
                new SequenceIdentifier(sequenceId),
                au_start_position,
                au_end_position,
                extended_au_start_position,
                extended_au_end_position
            );
        }

        public DATA_CLASS getAU_type() {
            return AU_type;
        }


        public DataUnitAccessUnitHeader(
                long access_unit_ID,
                short num_blocks,
                short parameter_set_ID,
                DATA_CLASS AU_type,
                long read_count,
                int mm_threshold,
                long mm_count,
                SequenceIdentifier ref_sequence_id,
                long ref_start_position,
                long ref_end_position,
                SequenceIdentifier sequence_ID,
                long au_start_position,
                long au_end_position,
                long extended_au_start_position,
                long extended_au_end_position
        ) {
            this.access_unit_ID = access_unit_ID;
            this.num_blocks = num_blocks;
            this.parameter_set_ID = parameter_set_ID;
            this.AU_type = AU_type;
            this.read_count = read_count;
            this.mm_threshold = mm_threshold;
            this.mm_count = mm_count;
            this.ref_sequence_id = ref_sequence_id;
            this.ref_start_position = ref_start_position;
            this.ref_end_position = ref_end_position;
            this.sequence_ID = sequence_ID;
            this.au_start_position = au_start_position;
            this.au_end_position = au_end_position;
            this.extended_au_start_position = extended_au_start_position;
            this.extended_au_end_position = extended_au_end_position;
        }

        public long getAccess_unit_ID() {
            return access_unit_ID;
        }

        public int getNum_blocks() {
            return num_blocks;
        }

        public short getParameter_set_ID() {
            return parameter_set_ID;
        }

        public SequenceIdentifier getSequence_ID() {
            return sequence_ID;
        }

        public long getRef_start_position() {
            return ref_start_position;
        }

        public long getRef_end_position() {
            return ref_end_position;
        }

        public SequenceIdentifier getRef_sequence_id() {
            return ref_sequence_id;
        }

        public long getAu_start_position() {
            return au_start_position;
        }

        public long getAu_end_position() {
            return au_end_position;
        }

        public long getExtended_au_start_position() {
            return extended_au_start_position;
        }

        public long getExtended_au_end_position() {
            return extended_au_end_position;
        }

        public long getRead_count() {
            return read_count;
        }

        public int getMm_threshold() {
            return mm_threshold;
        }

        public long getMm_count() {
            return mm_count;
        }

    }

    public DataUnitAccessUnit(
        AccessUnitHeader accessUnitHeader,
        Block blocks[]
    ) {
        this(new DataUnitAccessUnitHeader(accessUnitHeader), null, blocks);
    }

    public DataUnitAccessUnit(
        DataUnitAccessUnitHeader dataUnitAccessUnitHeader,
        DataUnits dataUnits,
        Block blocks[]
    ) {

        super(DATAUNIT_TYPE_ID.AU, dataUnits);

        header = dataUnitAccessUnitHeader;
        this.blocks = blocks;
    }

    public DataUnitAccessUnitHeader getHeader() {
        return header;
    }

    public static DataUnitAccessUnit read(MPEGReader reader, DataUnits dataUnits) throws IOException {
        
        final int data_unit_size = (int)reader.readBits(29);
        reader.readBits(3);
                
        DataUnitAccessUnitHeader dataUnitAccessUnitHeader = DataUnitAccessUnitHeader.read(reader, dataUnits);
        Block blocks[] = new Block[dataUnitAccessUnitHeader.num_blocks];
        for(int i=0; i<dataUnitAccessUnitHeader.num_blocks; i++){
            blocks[i] = Block.readBlock(
                    reader,
                    dataUnits.getParameter(dataUnitAccessUnitHeader.parameter_set_ID).getEncodingParameters(),
                    dataUnitAccessUnitHeader.AU_type
            );
        }
        return new DataUnitAccessUnit(dataUnitAccessUnitHeader, dataUnits, blocks);
    }

    @Override
    protected void writeDataUnitContent(MPEGWriter writer) throws IOException {
        long data_unit_size = header.size(getParameter());
        for(Block block : blocks){
            data_unit_size += block.size();
        }

        writer.writeBits(0, 3);
        writer.writeBits(data_unit_size + 5, 29);
        header.write(writer, getParameter());
        for(Block block: blocks){
            block.write(writer);
        }
    }

    public Block[] getBlocks() {
        return blocks;
    }

    public DataUnitParameters getParameter(){
        if(getParentStructure() == null){
            return null;
        }
        return getParentStructure().getParameter(getHeader().parameter_set_ID);
    }

    public DATA_CLASS getAUType(){
        return getHeader().getAU_type();
    }

    public Block getBlockByDescriptorId(DESCRIPTOR_ID descriptorId){
        for(Block block : blocks){
            if(block.getDescriptorId() == descriptorId.ID){
                return block;
            }
        }
        return null;
    }
}

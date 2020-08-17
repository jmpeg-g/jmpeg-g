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

package es.gencom.mpegg.coder.configuration;

import es.gencom.mpegg.coder.quality.QualityValueParameterSet;
import es.gencom.mpegg.coder.quality.DefaultQualityValueParameterSet_0;
import es.gencom.mpegg.coder.quality.AbstractQualityValueParameterSet;
import es.gencom.mpegg.coder.quality.DefaultQualityValueParameterSet_1;
import es.gencom.mpegg.coder.quality.DefaultQualityValueParameterSet_2;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DatasetType;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DecoderConfiguration;
import es.gencom.mpegg.coder.tokens.TokentypeDecoderConfigurationFactory;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Encoding Parameters serialization / deserialization.
 *
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class EncodingParameters {
    public static final byte NUM_DESCRIPTORS = 18;

    private DatasetType dataset_type;         // u(4)
    private ALPHABET_ID alphabet_id;          // u(8)
    private int read_length;                  // u(24)
    private byte number_of_template_segments; // u(2)
    private byte reserved;                    // u(6)
    private int max_au_data_unit_size;        // u(29)
    private boolean pos_40_bits;              // u(1)
    private byte qv_depth;                    // u(3)
    private byte as_depth;                    // u(3)
    private byte[] clids;
    private final DecoderConfiguration[][] decoder_configurations;
    private String[] rgroup_id;
    private boolean multiple_alignments_flag;
    private boolean spliced_reads_flag;
    private long multiple_signature_base;
    private short u_signature_size;
    private byte[] qv_coding_mode;
    private boolean[] qvps_flag;
    private QualityValuesParameterSet[] parameter_set_qvps;
    private boolean[] qv_reverse_flag;
    private byte[] qvps_preset_id;
    private boolean crps_flag;
    private Object parameter_set_crps;
    private EncodingParameters parent;

    public EncodingParameters() {
        decoder_configurations = new DecoderConfiguration[NUM_DESCRIPTORS][];
    }

    public EncodingParameters(
            DatasetType datasetType,
            int read_length,
            boolean pos_40_bits,
            byte number_template_segments) {

        this.dataset_type = datasetType;
        this.read_length = read_length;
        this.pos_40_bits = pos_40_bits;
        this.number_of_template_segments = number_template_segments;

        decoder_configurations = new DecoderConfiguration[NUM_DESCRIPTORS][];
    }

    public DatasetType getDatasetType() {
        return dataset_type;
    }

    public void setDatasetType(final DatasetType dataset_type) {
        this.dataset_type = dataset_type;
    }

    public ALPHABET_ID getAlphabetId() {
        return alphabet_id;
    }

    public void setAlphabetId(ALPHABET_ID alphabet_id) {
        this.alphabet_id = alphabet_id;
    }

    public int getReadsLength() {
        return read_length;
    }

    public void setReadsLength(final int reads_length) {
        this.read_length = reads_length;
    }

    public byte getNumberOfTemplateSegments() {
        return number_of_template_segments;
    }

    public void setNumberOfTemplateSegments(final byte number_of_template_segments) {
        this.number_of_template_segments = number_of_template_segments;
    }

    public int getMaxAUDataUnitSize() {
        return max_au_data_unit_size;
    }

    public void setMaxAUDataUnitSize(final int max_au_data_unit_size) {
        this.max_au_data_unit_size = max_au_data_unit_size;
    }

    public boolean isPos40Bits() {
        return pos_40_bits;
    }

    public void setPos40Bits(final boolean pos_40_bits) {
        this.pos_40_bits = pos_40_bits;
    }

    public byte getQVDepth() {
        return qv_depth;
    }
    public void setQVDepth(final byte qv_depth) {
        this.qv_depth = qv_depth;
    }

    public byte getASDepth() {
        return as_depth;
    }

    public void setASDepth(final byte as_depth) {
        this.as_depth = as_depth;
    }

    public byte[] getClassIds() {
        return clids;
    }

    public void setClassId(final int class_index, final byte class_id) {
        clids[class_index] = class_id;
    }

    public int getNumberOfClasses() {
        return clids != null ? clids.length : 0;
    }

    public void resetAndResizeReadGroups(int numberOfGroups){
        rgroup_id = new String[numberOfGroups];
    }

    public void setQv_coding_mode(int class_index, byte qv_mode){
        qv_coding_mode[class_index] = qv_mode;
    }

    public void setQvps_flag(int class_index, boolean qvps){
        qvps_flag[class_index] = qvps;
    }

    public byte getDefault_qvps_ID(final int class_index) {
        return qvps_preset_id[class_index];
    }

    public void setDefault_qvps_ID(final int class_index, final byte default_qvps){
        qvps_preset_id[class_index] = default_qvps;
    }

    public void setMultiple_alignments_flag(boolean multiple_alignments_flag) {
        this.multiple_alignments_flag = multiple_alignments_flag;
    }

    public void setSpliced_reads_flag(boolean spliced_reads_flag) {
        this.spliced_reads_flag = spliced_reads_flag;
    }

    public void setMultiple_signature_base(long multiple_signature_base) {
        this.multiple_signature_base = multiple_signature_base;
    }

    public void setU_signature_size(short u_signature_size) {
        this.u_signature_size = u_signature_size;
    }

    public void setCrps_flag(boolean crps_flag) {
        this.crps_flag = crps_flag;
    }

    public void setParent(EncodingParameters parent) {
        this.parent = parent;
    }

    public <T extends DecoderConfiguration> T getDecoderConfiguration(final DESCRIPTOR_ID descriptor_id, final DATA_CLASS class_id) {
        byte classIndex = getClassIndex(class_id);
        if (decoder_configurations.length > descriptor_id.ID &&
                decoder_configurations[descriptor_id.ID] != null &&
                (
                        decoder_configurations[descriptor_id.ID].length == 1 ||
                        decoder_configurations[descriptor_id.ID].length > classIndex
                )
        ) {

            if (decoder_configurations[descriptor_id.ID].length == 1) {
                return (T)decoder_configurations[descriptor_id.ID][0];
            }
            return (T)decoder_configurations[descriptor_id.ID][classIndex];

        }

        throw new IndexOutOfBoundsException();
    }

    public void setDefaultDecoderConfiguration(
            final DESCRIPTOR_ID descriptor_id,
            final DecoderConfiguration decoderConfiguration) {

        decoder_configurations[descriptor_id.ID] = 
                new DecoderConfiguration[] { decoderConfiguration };
    }

    public void setClassSpecificDecoderConfiguration(
            final DESCRIPTOR_ID descriptor_id,
            final DATA_CLASS class_id,
            final DecoderConfiguration decoderConfiguration) {

        final int classIndex = getClassIndex(class_id);
        decoder_configurations[descriptor_id.ID][classIndex] = decoderConfiguration;
    }
    
    public int getNumberOfGroups() {
        return rgroup_id.length;
    }

    public String getReadGroupId(final int group_index) {
        return rgroup_id[group_index];
    }

    public void setReadGroupId(int group_index, String group_id){
        rgroup_id[group_index] = group_id;
    }

    public String[] getReadGroupIds() {
        return rgroup_id;
    }

    public boolean isMultiple_alignments_flag() {
        return multiple_alignments_flag;
    }

    public boolean isSpliced_reads_flag() {
        return spliced_reads_flag;
    }

    public long getMultiple_signature_base(){
        return multiple_signature_base;
    }

    public short getU_signature_size() {
        return u_signature_size;
    }

    public byte getQv_coding_mode(int class_it){
        return qv_coding_mode[class_it];
    }

    public boolean getQvps_flag(int class_it) {
        return qvps_flag[class_it];
    }

    public boolean getCrps_flag() {
        return crps_flag;
    }

    public int getMaximumAUDataUnitSize() {
        return max_au_data_unit_size;
    }

    public void setMaximumAUDataUnitSize(int max_au_data_unit_size) {
        this.max_au_data_unit_size = max_au_data_unit_size;
    }

    public byte getQualityValuesDepth() {
        return qv_depth;
    }

    public void resetAndResizeClasses(final int numberOfClasses) {
        clids = new byte[numberOfClasses];
        qv_coding_mode = new byte[numberOfClasses];
        qvps_flag = new boolean[numberOfClasses];
        parameter_set_qvps = new QualityValuesParameterSet[numberOfClasses];
        qvps_preset_id = new byte[numberOfClasses];
        for(int descriptor_i=0; descriptor_i < NUM_DESCRIPTORS; descriptor_i++) {
            decoder_configurations[descriptor_i] = new DecoderConfiguration[numberOfClasses];
        }
        qv_reverse_flag = new boolean[numberOfClasses];
    }

    public void write(final MPEGWriter writer) throws IOException {

        dataset_type.write(writer);
        alphabet_id.write(writer);
        writer.writeBits(read_length, 24);
        writer.writeBits(number_of_template_segments - 1, 2);
        writer.writeBits(reserved, 6);
        writer.writeBits(max_au_data_unit_size, 29);
        writer.writeBoolean(pos_40_bits);
        writer.writeBits(qv_depth, 3);
        writer.writeBits(as_depth, 3);

        writer.writeBits(clids.length, 4);
        for (int i = 0; i < clids.length; i++) {
            writer.writeBits(clids[i], 4);
        }

        for(int i = 0; i < NUM_DESCRIPTORS; i++) {
            writer.writeBoolean(decoder_configurations[i].length != 1 );

            for (int j = 0; j < decoder_configurations[i].length; j++) {
                byte dec_cfg_preset = 0;
                writer.writeUnsignedByte(dec_cfg_preset);
                decoder_configurations[i][j].write(writer);
            }

        }

        writer.writeUnsignedShort(rgroup_id.length);
        for(String rgroupId : rgroup_id){
            writer.writeNTString(rgroupId);
        }

        writer.writeBoolean(multiple_alignments_flag);
        writer.writeBoolean(spliced_reads_flag);
        writer.writeBits(multiple_signature_base, 31);
        if(multiple_signature_base > 0) {
            writer.writeBits(u_signature_size, 6);
        }

        for(int i = 0; i < qv_coding_mode.length; i++){
            writer.writeBits(qv_coding_mode[i], 4);
            if(qv_coding_mode[i] == 1){
                writer.writeBoolean(qvps_flag[i]);
                if(qvps_flag[i]){
                    parameter_set_qvps[i].write(writer);
                }else{
                    writer.writeBits(qvps_preset_id[i], 4);
                }
            }
            writer.writeBoolean(qv_reverse_flag[i]);
        }

        writer.writeBoolean(crps_flag);
        if(crps_flag){
            throw new UnsupportedOperationException();
        }
        writer.flush();
    }

    public void read(final MPEGReader reader) throws IOException {
        dataset_type = DatasetType.read(reader);
        alphabet_id = ALPHABET_ID.read(reader);
        read_length = (int)reader.readBits(24);
        number_of_template_segments = (byte)(reader.readBits(2) + 1);
        reserved = (byte)reader.readBits(6);
        max_au_data_unit_size = (int)reader.readBits(29);
        pos_40_bits = reader.readBoolean();
        qv_depth = (byte)reader.readBits(3);
        as_depth = (byte)reader.readBits(3);

        final byte num_classes = (byte)reader.readBits(4);
        clids = new byte[num_classes];
        for (int i = 0; i < num_classes; i++) {
            clids[i] = (byte)reader.readBits(4);
        }


        for(int i = 0; i < NUM_DESCRIPTORS; i++) {
            final boolean class_specific_dec_cfg_flag = reader.readBits(1) != 0x00; // u(1)
            if(class_specific_dec_cfg_flag) {
                decoder_configurations[i] = new DecoderConfiguration[num_classes];
                for (int j = 0; j < num_classes; j++) {
                    decoder_configurations[i][j] = readDescriptorConfiguration(reader, i);
                }
            } else {
                decoder_configurations[i] = new DecoderConfiguration[1];
                decoder_configurations[i][0] = readDescriptorConfiguration(reader, i);
            }
        }

        final int num_groups = reader.readUnsignedShort(); // u(16)

        rgroup_id = new String[num_groups];
        for(int i = 0; i < num_groups; i++) {
            rgroup_id[i] = reader.readNTString();
        }

        multiple_alignments_flag = reader.readBits(1) != 0x00;
        spliced_reads_flag = reader.readBits(1) != 0x00;
        multiple_signature_base = reader.readBits(31);

        if(multiple_signature_base > 0) {
            u_signature_size = (short)reader.readBits(6);
        }

        qv_coding_mode = new byte[num_classes];
        qvps_flag = new boolean[num_classes];
        qvps_preset_id = new byte[num_classes];
        qv_reverse_flag = new boolean[num_classes];

        for(int i = 0; i < num_classes; i++) {
            qv_coding_mode[i] = (byte)reader.readBits(4);
            if(qv_coding_mode[i] == 1){
                qvps_flag[i] = reader.readBoolean();
                if(qvps_flag[i]) {
                    parameter_set_qvps[i] = new QualityValuesParameterSet();
                    parameter_set_qvps[i].read(reader);
                } else {
                    qvps_preset_id[i] = (byte)reader.readBits(4);
                }
            } else {
                System.err.println("unknown coding mode");
            }
            qv_reverse_flag[i] = reader.readBoolean();
        }
        crps_flag = reader.readBoolean();
        if(crps_flag) {
            parameter_set_crps = null; //todo change this
        }
        reader.align();
    }

    /**
     * 7.3.2.1 Descriptor configuration syntax and semantics
     *
     * @param reader
     * @param desc_id
     * @return
     * @throws IOException
     */
    private DecoderConfiguration readDescriptorConfiguration(
            final MPEGReader reader, final int desc_id) throws IOException {

        final int dec_cfg_preset = reader.readUnsignedByte(); // u(8)
        if (dec_cfg_preset != 0) {
            throw new IllegalArgumentException();
        }
        
        if (desc_id == DESCRIPTOR_ID.MSAR.ID || desc_id == DESCRIPTOR_ID.RNAME.ID) {
            return TokentypeDecoderConfigurationFactory.read(reader);
        }

        return DescriptorDecoderConfigurationFactory.read(reader);
    }

    public long size() {
        long sizeInBits = 0;
        sizeInBits += 4; //dataset_type
        sizeInBits += ALPHABET_ID.ENCODING_SIZE_IN_BITS;
        sizeInBits += 24; //read_length
        sizeInBits += 2; //number_of_template_segments
        sizeInBits += 6; //reserved
        sizeInBits += 29; //max_au
        sizeInBits += 1; //pos_40_bits
        sizeInBits += 3; //qv_depth
        sizeInBits += 3; //as_depth
        sizeInBits += 4; //num_classes
        for (int i = 0; i < clids.length; i++) {
            sizeInBits += 4; //clid
        }
        for(int i = 0; i < NUM_DESCRIPTORS; i++){
            sizeInBits += 1; //class_specific
            for(int class_i=0; class_i < decoder_configurations[i].length; class_i++) {
                sizeInBits += 8; //dec_cfg_preset
                sizeInBits += 8; //encoding_mode_id[i];
                sizeInBits += decoder_configurations[i][class_i].sizeInBits();
            }
        }
        sizeInBits += 16; //num_groups;
        for(int i = 0; i < rgroup_id.length; i++){
            sizeInBits += (rgroup_id[i].length()+1)*8;
        }
        sizeInBits += 1; //multiple_alignments_flag;
        sizeInBits += 1; //spliced_reads
        sizeInBits += 31; //multiple_signature_base
        if(multiple_signature_base > 0){
            sizeInBits += 6; //u_signature_size
        }
        for(int i = 0; i < clids.length; i++) {
            sizeInBits += 4; //qv_coding_mode
            if(qv_coding_mode[i] == 1){
                sizeInBits += 1; //qvps_flag
                if(qvps_flag[i]){
                    sizeInBits += parameter_set_qvps[i].sizeInBits();
                } else {
                    sizeInBits += 4; //default_qvps
                }
            }
            sizeInBits += 1; //qv_reverse
        }
        sizeInBits += 1; //crps_flag
        return (long) Math.ceil((double)sizeInBits/(double)8);
    }

    private byte getClassIndex(DATA_CLASS dataClass){
        for(byte class_i=0; class_i < getNumberOfClasses(); class_i++){
            if(clids[class_i] == dataClass.ID){
                return class_i;
            }
        }
        throw new IllegalArgumentException("The class is not listed in the encoding parameters");
    }

    public AbstractQualityValueParameterSet getQualityValueParameterSet(final DATA_CLASS dataClass) {
        if(getQvps_flag(getClassIndex(dataClass))) {
            return new QualityValueParameterSet(parameter_set_qvps[getClassIndex(dataClass)]);
        } else {
            switch (qvps_preset_id[getClassIndex(dataClass)]) {
                case 0:
                    return new DefaultQualityValueParameterSet_0();
                case 1:
                    return new DefaultQualityValueParameterSet_1();
                case 2:
                    return new DefaultQualityValueParameterSet_2();
                default:
                    throw new IllegalArgumentException("The quality preset id does not exist");
            }
        }
    }

    public void setQualityValuesParameterSet(final int class_index, 
            final QualityValuesParameterSet parameters) {
        parameter_set_qvps[class_index] = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncodingParameters)) return false;
        EncodingParameters that = (EncodingParameters) o;
        return read_length == that.read_length &&
                number_of_template_segments == that.number_of_template_segments &&
                reserved == that.reserved &&
                max_au_data_unit_size == that.max_au_data_unit_size &&
                pos_40_bits == that.pos_40_bits &&
                qv_depth == that.qv_depth &&
                as_depth == that.as_depth &&
                isMultiple_alignments_flag() == that.isMultiple_alignments_flag() &&
                isSpliced_reads_flag() == that.isSpliced_reads_flag() &&
                getMultiple_signature_base() == that.getMultiple_signature_base() &&
                getU_signature_size() == that.getU_signature_size() &&
                getCrps_flag() == that.getCrps_flag() &&
                dataset_type == that.dataset_type &&
                alphabet_id == that.alphabet_id &&
                Arrays.equals(getClassIds(), that.getClassIds()) &&
                Arrays.equals(decoder_configurations, that.decoder_configurations) &&
                Arrays.equals(rgroup_id, that.rgroup_id) &&
                Arrays.equals(qv_coding_mode, that.qv_coding_mode) &&
                Arrays.equals(qvps_flag, that.qvps_flag) &&
                Arrays.equals(parameter_set_qvps, that.parameter_set_qvps) &&
                Arrays.equals(qvps_preset_id, that.qvps_preset_id) &&
                Objects.equals(parameter_set_crps, that.parameter_set_crps) &&
                Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(dataset_type, alphabet_id, read_length, number_of_template_segments, reserved, max_au_data_unit_size, pos_40_bits, qv_depth, as_depth, isMultiple_alignments_flag(), isSpliced_reads_flag(), getMultiple_signature_base(), getU_signature_size(), getCrps_flag(), parameter_set_crps, parent);
        result = 31 * result + Arrays.hashCode(getClassIds());
        result = 31 * result + Arrays.hashCode(decoder_configurations);
        result = 31 * result + Arrays.hashCode(rgroup_id);
        result = 31 * result + Arrays.hashCode(qv_coding_mode);
        result = 31 * result + Arrays.hashCode(qvps_flag);
        result = 31 * result + Arrays.hashCode(parameter_set_qvps);
        result = 31 * result + Arrays.hashCode(qvps_preset_id);
        return result;
    }
}

package es.gencom.mpegg.dataunits;

import es.gencom.mpegg.format.AccessUnitHeader;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.DatasetType;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

public class DataUnitAccessUnitHeader {
    
    public final long access_unit_id;
    public final short num_blocks;
    public final short parameter_set_id;
    public final DATA_CLASS au_type;
    public final long read_count ;

    public final int mm_threshold;
    public final long mm_count;

    public final SequenceIdentifier ref_sequence_id;
    public final long ref_start_position;
    public final long ref_end_position;

    public final SequenceIdentifier sequence_id;
    public final long au_start_position;
    public final long au_end_position;
    public final long extended_au_start_position;
    public final long extended_au_end_position;

    public DataUnitAccessUnitHeader(
            final long access_unit_id,
            final short num_blocks,
            final short parameter_set_id,
            final DATA_CLASS au_type,
            final long read_count,
            final int mm_threshold,
            final long mm_count,
            final SequenceIdentifier ref_sequence_id,
            final long ref_start_position,
            final long ref_end_position,
            final SequenceIdentifier sequence_id,
            final long au_start_position,
            final long au_end_position,
            final long extended_au_start_position,
            final long extended_au_end_position) {
        
        this.access_unit_id = access_unit_id;
        this.num_blocks = num_blocks;
        this.parameter_set_id = parameter_set_id;
        this.au_type = au_type;
        this.read_count = read_count;
        this.mm_threshold = mm_threshold;
        this.mm_count = mm_count;
        this.ref_sequence_id = ref_sequence_id;
        this.ref_start_position = ref_start_position;
        this.ref_end_position = ref_end_position;
        this.sequence_id = sequence_id;
        this.au_start_position = au_start_position;
        this.au_end_position = au_end_position;
        this.extended_au_start_position = extended_au_start_position;
        this.extended_au_end_position = extended_au_end_position;
    }

    public DataUnitAccessUnitHeader(AccessUnitHeader accessUnitHeader) {
        this(
                accessUnitHeader.getAccessUnitID(),
                accessUnitHeader.getNumberOfBlocks(),
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
        if(au_type == DATA_CLASS.CLASS_N || au_type == DATA_CLASS.CLASS_M){
            sizeInBits += 16; //mm_threshold
            sizeInBits += 32; //mm_count
        }
        if(parameters.getDatasetType() == DatasetType.REFERENCE){
            sizeInBits += 16; //ref_sequence_id
            sizeInBits += parameters.isPosSize40() ? 40 : 32; //ref_start_position
            sizeInBits += parameters.isPosSize40() ? 40 : 32; //ref_end_position
        }

        if(au_type != DATA_CLASS.CLASS_U){
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
        writer.writeUnsignedInt(access_unit_id);
        writer.writeUnsignedByte(num_blocks);
        writer.writeUnsignedByte(parameters.parameter_set_id);
        writer.writeBits(au_type.ID, 4);
        writer.writeUnsignedInt(read_count);
        if(au_type == DATA_CLASS.CLASS_N || au_type == DATA_CLASS.CLASS_M){
            writer.writeUnsignedShort(mm_threshold);
            writer.writeUnsignedInt(mm_count);
        }

        if(parameters.getDatasetType() == DatasetType.REFERENCE){
            writer.writeShort((short) ref_sequence_id.getSequenceIdentifier());
            writer.writeBits(ref_start_position, parameters.isPosSize40() ? 40:32);
            writer.writeBits(ref_end_position, parameters.isPosSize40() ? 40:32);
        }
        if(au_type != DATA_CLASS.CLASS_U){
            writer.writeShort((short) sequence_id.getSequenceIdentifier());
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
            final MPEGReader reader,
            final DataUnits dataUnits) throws IOException {

        long access_unit_id = reader.readUnsignedInt();
        short num_blocks = reader.readUnsignedByte();
        short parameter_set_id = (short) reader.readUnsignedByte();

        DataUnitParameters parameters = dataUnits.getParameter(parameter_set_id);
        byte posLengthInBits = (byte) (parameters.isPosSize40() ? 40 : 32);

        DATA_CLASS au_type = DATA_CLASS.getDataClass((byte) reader.readBits(4));
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

        if(au_type == DATA_CLASS.CLASS_N || au_type == DATA_CLASS.CLASS_M){
            mm_threshold = reader.readUnsignedShort();
            mm_count = reader.readUnsignedInt();
        }

        if(parameters.getDatasetType() == DatasetType.REFERENCE){
            ref_sequence_id = reader.readUnsignedShort();
            ref_start_position = reader.readBits(posLengthInBits);
            ref_end_position = reader.readBits(posLengthInBits);
        }

        if (au_type != DATA_CLASS.CLASS_U) {
            sequenceId = reader.readUnsignedShort();
            au_start_position = reader.readBits(posLengthInBits);
            au_end_position = reader.readBits(posLengthInBits);

            if(parameters.isMultiple_alignments_flag()){
                extended_au_start_position = reader.readBits(posLengthInBits);
                extended_au_end_position = reader.readBits(posLengthInBits);
            }
        } else if(parameters.getMultiple_signature_base() != 0) {
            throw new UnsupportedOperationException();
        }

        reader.align();

        return new DataUnitAccessUnitHeader(
            access_unit_id,
            num_blocks,
            parameter_set_id,
            au_type,
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
}

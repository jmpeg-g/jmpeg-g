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

package es.gencom.mpegg.dataunits;

import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;

import java.io.IOException;

public class DataUnitAccessUnit extends AbstractDataUnit {
    public final DataUnitAccessUnitHeader header;
    private final AccessUnitBlock blocks[];
    private long maximalPositionDecode = -1;

    public DataUnitAccessUnit(
        final DataUnitAccessUnitHeader accessUnitHeader,
        final DataUnits dataUnits,
        final AccessUnitBlock blocks[]) {

        super(DATAUNIT_TYPE_ID.AU, dataUnits);

        header = accessUnitHeader;
        this.blocks = blocks;
    }

    public long getMaximalPositionDecode() {
        return maximalPositionDecode;
    }

    public SequenceIdentifier getSequenceId() {
        return header.sequence_id;
    }

    public long getStart() {
        return header.au_start_position;
    }

    public long getEnd() {
        return header.au_end_position;
    }

    public static DataUnitAccessUnit read(MPEGReader reader, DataUnits dataUnits) throws IOException {
        
        final int data_unit_size = (int)reader.readBits(29);
        reader.readBits(3);
                
        DataUnitAccessUnitHeader dataUnitAccessUnitHeader = DataUnitAccessUnitHeader.read(reader, dataUnits);
        AccessUnitBlock blocks[] = new AccessUnitBlock[dataUnitAccessUnitHeader.num_blocks];
        for(int i = 0; i < blocks.length; i++) {
            blocks[i] = AccessUnitBlock.readBlock(reader,
                    dataUnits.getParameter(dataUnitAccessUnitHeader.parameter_set_id).getEncodingParameters(),
                    dataUnitAccessUnitHeader.au_type);
        }
        return new DataUnitAccessUnit(dataUnitAccessUnitHeader, dataUnits, blocks);
    }

    @Override
    protected void writeDataUnitContent(MPEGWriter writer) throws IOException {
        long data_unit_size = header.size(getParameter());
        for(AccessUnitBlock block : blocks){
            data_unit_size += block.size();
        }

        writer.writeBits(0, 3);
        writer.writeBits(data_unit_size + 5, 29);
        header.write(writer, getParameter());
        for(AccessUnitBlock block: blocks){
            block.write(writer);
        }
    }

    public AccessUnitBlock[] getBlocks() {
        return blocks;
    }

    public DataUnitParameters getParameter(){
        if(getParentStructure() == null){
            return null;
        }
        return getParentStructure().getParameter(header.parameter_set_id);
    }

    public DATA_CLASS getAUType(){
        return header.au_type;
    }

    public AccessUnitBlock getBlockByDescriptorId(DESCRIPTOR_ID descriptorId){
        for(AccessUnitBlock block : blocks) {
            if(block.descriptor_id == descriptorId) {
                return block;
            }
        }
        return null;
    }

    public long getId() {
        return header.access_unit_id;
    }
}

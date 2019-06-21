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

import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AccessUnitContainer extends GenInfo<AccessUnitContainer>{
    public final static String KEY = "aucn";

    private final DatasetContainer datasetContainer;
    private final DatasetHeader datasetHeader;
    private AccessUnitHeader access_unit_header;
    private List<Block> blocks;
    private AccessUnitInformation au_information;
    private AccessUnitProtection au_protection;

    //position prior to gen_info header of the accessUnitContainer
    private Long au_offset = null;

    public AccessUnitContainer(
            final DatasetContainer datasetContainer,
            final AccessUnitHeader access_unit_header) {
        
        super(KEY);

        this.datasetContainer = datasetContainer;
        this.datasetHeader = datasetContainer.getDatasetHeader();
        this.access_unit_header = access_unit_header;
        blocks = new ArrayList<>();
    }

    public AccessUnitContainer(final DatasetContainer datasetContainer) {
        this(datasetContainer, 
             new AccessUnitHeader(datasetContainer.getDatasetHeader()));
    }

    public AccessUnitInformation getAccessUnitInformation() {
        return au_information;
    }
    
    public AccessUnitProtection getAccessUnitProtection() { 
        return au_protection; 
    }

    public void setAccessUnitHeader(final AccessUnitHeader accessUnitHeader) {
        this.access_unit_header = accessUnitHeader;
    }

    public AccessUnitHeader getAccessUnitHeader() {
        return access_unit_header;
    }

    public void setBlocks(final List<Block> blocks) {
        this.blocks = blocks;
        access_unit_header.setNumBlocks((byte) blocks.size());
    }

    public void addBlock(final Block block) {
        blocks.add(block);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public DatasetParameterSet getParameterSet() {
        return datasetContainer.getDatasetParameterSet(access_unit_header.getParameterSetID());
    }

    public long getAccessUnitOffset() {
        return au_offset;
    }

    public void setAccessUnitOffset(final long au_offset) {
        this.au_offset = au_offset;
    }

    @Override
    public long size(){
        long result = 0;
        result += access_unit_header.sizeWithHeader();
        if(datasetHeader.isBlockHeader()){
            for(Block block : blocks){
                result += block.size();
            }
        }
        if(au_information != null){
            result += au_information.size();
        }
        if(au_protection != null){
            result += au_protection.size();
        }
        return result;
    }

    @Override
    public void write(final MPEGWriter writer) 
            throws IOException, InvalidMPEGStructure {

        access_unit_header.writeWithHeader(writer);
        if(datasetHeader.isBlockHeader()){
            if(blocks.size() != access_unit_header.getNumberOfBlocks()){
                //todo add check
            }
            for(Block block : blocks){
                block.write(writer);
            }
        }

        if (au_information != null) {
            au_information.writeWithHeader(writer);
        }

        if (au_protection != null) {
            au_protection.writeWithHeader(writer);
        }
    }

    @Override
    public AccessUnitContainer read(final MPEGReader reader, final long size) 
            throws IOException, InvalidMPEGStructure, ParsedSizeMismatchException {

        Header header = Header.read(reader);
        if(AccessUnitHeader.KEY.equals(header.key)) {
            access_unit_header.read(reader, header.getContentSize());
        } else {
            throw new InvalidMPEGStructure("Access unit container misses the mandatory access unit header element.");
        }

        final long remainingBytes = size - access_unit_header.sizeWithHeader();
        if(remainingBytes != 0) {
            Payload possibleInformationPossibleProtectionPossibleBlock = reader.readPayload(remainingBytes);
            Payload copyPossibleInformationPossibleProtectionPossibleBlock =
                    possibleInformationPossibleProtectionPossibleBlock.createCopy();

            Payload possibleProtectionPossibleBlock;
            header = Header.read(possibleInformationPossibleProtectionPossibleBlock);
            if (AccessUnitInformation.KEY.equals(header.key)) {
                au_information = new AccessUnitInformation();
                au_information.read(
                        possibleInformationPossibleProtectionPossibleBlock,
                        header.getContentSize()
                );
                possibleProtectionPossibleBlock = possibleInformationPossibleProtectionPossibleBlock;
            } else {
                possibleProtectionPossibleBlock = copyPossibleInformationPossibleProtectionPossibleBlock;
            }

            Payload copyPossibleProtectionPossibleBlock = possibleProtectionPossibleBlock.createCopy();
            Payload possibleBlock;
            header = Header.read(possibleProtectionPossibleBlock);
            if (AccessUnitProtection.KEY.equals(header.key)) {
                au_protection = new AccessUnitProtection();
                au_protection.read(possibleProtectionPossibleBlock, header.getContentSize());
                possibleBlock = possibleProtectionPossibleBlock;
            } else {
                possibleBlock = copyPossibleProtectionPossibleBlock;
            }

            if (datasetHeader.isBlockHeader()) {
                for (byte block_i = 0; block_i < access_unit_header.getNumberOfBlocks(); block_i++) {
                    Block block = new Block(datasetHeader);
                    block.read(possibleBlock);
                    blocks.add(block);
                }
            }
        }
        if (size != size()) {
            throw new ParsedSizeMismatchException();
        }

        return this;
    }
}

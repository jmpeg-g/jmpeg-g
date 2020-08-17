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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * <p>
 * ISO/IEC 23092 MPEG-G Data Format container implementation (Part 1).
 * </p>
 * The class is an extension of the Java ArrayList and contains a list 
 * of MPEG-G Dataset Groups ("dgcn") along to the MPEG-G Header ("flhd").
 *
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MPEGFile extends ArrayList<DatasetGroupContainer> {
    private MPEGFileHeader fileHeader;

    public MPEGFile() {
        fileHeader = new MPEGFileHeader();
    }

    /**
     * <p>
     * Set MPEG-G File Header (6.5.1).
     * </p>
     * 
     * @param fileHeader MPEG-G file header ('flhd')
     */
    public void setFileHeader(final MPEGFileHeader fileHeader) {
        this.fileHeader = fileHeader;
    }

    public MPEGFileHeader getFileHeader() {
        return fileHeader;
    }
    
    public DatasetGroupContainer getDatasetGroupContainerByIndex(final int idx) {
        return get(idx);
    }

    /**
     * <p>
     *  Find a Dataset Group by its dataset group id.
     * </p>
     * 
     * @param dataset_group_id the dataset group id
     * 
     * @return the Dataset Group that corresponds to the provided dataset group id 
     * or null if not found.
     */
    public DatasetGroupContainer getDatasetGroupContainerById(final short dataset_group_id) {
        for (int i = 0, n = size(); i < n; i++) {
            final DatasetGroupContainer datasetGroupContainer = get(i);
            if(datasetGroupContainer.getDatasetGroupHeader().getDatasetGroupId() == dataset_group_id){
                return datasetGroupContainer;
            }
        }
        return null;
    }

    /**
     * <p>
     * Add MPEG-G Dataset Group (6.4.1).
     * </p>
     * 
     * @param datasetGroupContainer MPEG-G dataset group ('dgcn')
     */
    public void addDatasetGroupContainer(
            final DatasetGroupContainer datasetGroupContainer) {

        add(datasetGroupContainer);
    }

    public void read(final MPEGReader mpegReader) 
            throws IOException, InvalidMPEGStructureException, ParsedSizeMismatchException, InvalidMPEGGFileException {

        GenInfo.Header header = GenInfo.Header.read(mpegReader);

        if(!header.key.equals(MPEGFileHeader.KEY)){
            throw new InvalidMPEGStructureException("File is missing mandatory file header.");
        }

        fileHeader = new MPEGFileHeader();
        fileHeader.read(mpegReader, header.getContentSize());

        header = GenInfo.Header.read(mpegReader);
        if(!header.key.equals(DatasetGroupContainer.KEY)) {
            throw new InvalidMPEGStructureException("File is missing mandatory dataset group container.");
        }
        while(header.key.equals(DatasetGroupContainer.KEY)) {
            DatasetGroupContainer datasetGroupContainer = new DatasetGroupContainer();
            datasetGroupContainer.read(mpegReader, header.getContentSize());
            add(datasetGroupContainer);
            try {
                header = GenInfo.Header.read(mpegReader);
            } catch (EOFException e) {
                return;
            }
        }
    }

    public void write(final MPEGWriter writer)
            throws InvalidMPEGStructureException, IOException {
        
        if(fileHeader == null){
            throw new InvalidMPEGStructureException("Missing mandatory file header.");
        }
        fileHeader.writeWithHeader(writer);
        for (int i = 0, n = size(); i < n; i++) {
            get(i).writeWithHeader(writer);
        }
    }
}

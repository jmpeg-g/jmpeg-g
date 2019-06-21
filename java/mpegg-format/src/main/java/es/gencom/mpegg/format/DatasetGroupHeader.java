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
import java.util.Arrays;

/**
 * <p>
 * Dataset Group descriptor (ISO/IEC DIS 23092-1 6.4.1.1 Dataset Group Header).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DatasetGroupHeader extends GenInfo<DatasetGroupHeader> {

    public final static String KEY = "dghd";

    private byte dataset_group_id;
    private byte version_number;
    private int[] datasets;
    private int numDatasets;

    public DatasetGroupHeader() {
        this((byte)0,(byte)0);
    }

    public DatasetGroupHeader(
            final byte dataset_group_id, 
            final byte version_number) {
        
        super(KEY);

        this.dataset_group_id = dataset_group_id;
        this.version_number = version_number;
        datasets = new int[16];
        numDatasets = 0;
    }

    public byte getDatasetGroupId() {
        return dataset_group_id;
    }


    public byte getVersionNumber() {
        return version_number;
    }

    public int getNumDatasets() {
        return numDatasets;
    }

    void addDatasetId(int datasetId) {
        if( datasets.length == numDatasets){
            datasets = Arrays.copyOf(datasets, datasets.length * 2);
        }
        datasets[numDatasets] = datasetId;
        numDatasets++;
    }

    public int[] getDatasetIds(){
        return datasets;
    }

    public int getDatasetId(final int dataset_idx){
        if(dataset_idx < 0 || dataset_idx >= numDatasets){
            throw new IndexOutOfBoundsException();
        }
        return datasets[dataset_idx];
    }
    
    @Override
    protected long size() {
        return 2 + 2 * (numDatasets);
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(dataset_group_id, 8);
        writer.writeBits(version_number, 8);
        for (int dataset_i = 0; dataset_i < numDatasets; dataset_i++) {
            writer.writeBits(datasets[dataset_i], 16);
        }
    }

    @Override
    public DatasetGroupHeader read(
            final MPEGReader reader, final long size) throws IOException, ParsedSizeMismatchException {

        dataset_group_id = (byte) reader.readBits(8);
        version_number = (byte) reader.readBits(8);

        if(size % 2 != 0) {
            throw new IllegalArgumentException();
        }
        int datasetsNumber = (int)(size >> 1) - 1;
        datasets = new int[datasetsNumber];
        for (int i = 0; i < datasetsNumber; i++) {
            addDatasetId(reader.readShort());
        }

        if(size != size()){
            throw new ParsedSizeMismatchException("Dataset group header has not the indicated size");
        }
        return this;
    }
}

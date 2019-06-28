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

package es.gencom.mpegg.decoder.descriptors.streams;

import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

public class MMposStream {
    private final DATA_CLASS dataClass;
    private final DescriptorDecoder[] decoders;
    private int[] offsets;
    private int indexSymbol = 0;


    public MMposStream(
            DATA_CLASS dataClass,
            DataUnitAccessUnit.Block block,
            EncodingParameters encodingParameters) {

        this.dataClass = dataClass;
        
        DescriptorDecoderConfiguration conf = 
                encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.MMPOS, dataClass);

        if(block == null) {
            decoders = null;
            return;
        }
        Payload[] sub_streams = block.getPayloads();
        decoders = new DescriptorDecoder[sub_streams.length];

        for(int substream_index = 0; substream_index < sub_streams.length; substream_index++){

            decoders[substream_index] = conf.getDescriptorDecoder(
                sub_streams[substream_index],
                DESCRIPTOR_ID.MMPOS,
                substream_index,
                encodingParameters.getAlphabetId()
            );
        }
    }

    public int[][] read(
            long numberOfAlignedRecordSegments
    ) throws IOException {
        int[][] mismatchOffset = new int[Math.toIntExact(numberOfAlignedRecordSegments)][];
        for(
            int alignedRecordSegment_i = 0;
            alignedRecordSegment_i<numberOfAlignedRecordSegments;
            alignedRecordSegment_i++
        ){
            if(dataClass == DATA_CLASS.CLASS_P){
                mismatchOffset[alignedRecordSegment_i] = new int[0];
                continue;
            }
            mismatchOffset[alignedRecordSegment_i] = new int[32];
            int numOffsets = 0;
            int previousOffset = 0;
            long valueSubsequence0 = decoders[0].read();

            while (valueSubsequence0 == 0){
                mismatchOffset[alignedRecordSegment_i][numOffsets] =
                    Math.toIntExact(decoders[1].read() + previousOffset);
                previousOffset =  mismatchOffset[alignedRecordSegment_i][numOffsets] + 1;
                numOffsets++;
                if(numOffsets == mismatchOffset[alignedRecordSegment_i].length){
                    mismatchOffset[alignedRecordSegment_i] = Arrays.copyOf(
                        mismatchOffset[alignedRecordSegment_i],
                        mismatchOffset[alignedRecordSegment_i].length*2
                    );
                }
                valueSubsequence0 = decoders[0].read();
            }
            mismatchOffset[alignedRecordSegment_i] = Arrays.copyOf(mismatchOffset[alignedRecordSegment_i], numOffsets);
        }
        return mismatchOffset;
    }
}

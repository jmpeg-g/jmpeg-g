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

import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class MMTypeStream implements MMTypeStreamInterface{
    private final DATA_CLASS dataClass;
    private final Payload sub_streams[];
    private final DescriptorDecoder decoders[];
    private long [] mismatch_type;
    private char[] mismatches;

    private int j4_0;
    private int j4_1;
    private int j4_2;

    private final ExternalDataInputStream externalDataProvider;

    public MMTypeStream(
            final DATA_CLASS dataClass, 
            final AccessUnitBlock block, 
            final EncodingParameters encodingParameters) {

        this.dataClass = dataClass;
        DescriptorDecoderConfiguration conf = 
                encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.MMTYPE, dataClass);
        
        externalDataProvider = new ExternalDataInputStream();

        if (block == null){
            sub_streams = null;
            decoders = null;
        } else {
            sub_streams = block.getPayloads();
            decoders = new DescriptorDecoder[sub_streams.length];

            for(int substream_index=0; substream_index < sub_streams.length; substream_index++) {
                if(substream_index == 0) {
                    decoders[substream_index] = conf.getDescriptorDecoder(
                            sub_streams[substream_index],
                            DESCRIPTOR_ID.MMTYPE,
                            substream_index,
                            encodingParameters.getAlphabetId()
                    );
                } else if (substream_index == 1) {
                    decoders[substream_index] = conf.getDescriptorDecoder(
                            sub_streams[substream_index],
                            DESCRIPTOR_ID.MMTYPE,
                            substream_index,
                            encodingParameters.getAlphabetId(),
                            externalDataProvider);
                } else {
                    decoders[substream_index] = conf.getDescriptorDecoder(
                            sub_streams[substream_index],
                            DESCRIPTOR_ID.MMTYPE,
                            substream_index,
                            encodingParameters.getAlphabetId()
                    );
                }
            }

        }
    }

    public long[] readMismatchesType(int num_offsets) throws IOException {
        mismatch_type = new long[num_offsets];
        mismatches = new char[num_offsets];

        int j=0;
        while (j < num_offsets) {
            mismatch_type[j] = decoders[0].read();
            j++;
        }
        return mismatch_type;
    }

    public byte readNewMismatchBase(byte[] s_alphabet_id, final byte originalCharacter) throws IOException {
        if(dataClass == DATA_CLASS.CLASS_N){
            return 'N';
        }

        externalDataProvider.symbol = ALPHABET_ID.charToId(s_alphabet_id, Character.toUpperCase((char)originalCharacter));
        
        int index;
        try{
            index = (int) decoders[1].read();
        } catch (ArrayIndexOutOfBoundsException e){
            throw e;
        }
        byte newBase = s_alphabet_id[index];
        j4_1++;
        j4_0++;

        return newBase;
    }

    public byte readNewInsertBase(byte[] s_alphabet_id) throws IOException {

        int decodedValue = (int) decoders[2].read();
        byte newBase = s_alphabet_id[decodedValue];
        j4_2++;
        j4_0++;

        return newBase;
    }

    public int[][] readMMType(int[][] mmOffsets) throws IOException {
        if(dataClass == DATA_CLASS.CLASS_N || dataClass == DATA_CLASS.CLASS_M){
            int[][] result = new int[mmOffsets.length][];
            for(int i=0; i<mmOffsets.length; i++){
                result[i] = new int[mmOffsets[i].length];
                Arrays.fill(result[i], 0);
            }
            return result;
        }
        int[][] mmTypes = new int[mmOffsets.length][];
        for(int alignedRecordSegment_i=0; alignedRecordSegment_i < mmOffsets.length; alignedRecordSegment_i++){
            mmTypes[alignedRecordSegment_i] = new int[mmOffsets[alignedRecordSegment_i].length];
            for(int offset_i=0; offset_i < mmOffsets[alignedRecordSegment_i].length; offset_i++){
                int value = Math.toIntExact(decoders[0].read());
                mmTypes[alignedRecordSegment_i][offset_i] = Math.toIntExact(value);
            }
        }
        return mmTypes;
    }

    private static class ExternalDataInputStream extends InputStream {

        public byte symbol;
        
        @Override
        public int read() throws IOException {
            return symbol & 0xFF;
        }
        
    }
}

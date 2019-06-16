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

import es.gencom.mpegg.CABAC.configuration.CABAC_DescriptorDecoderConfiguration;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

import static es.gencom.mpegg.decoder.descriptors.streams.StreamsConstantsParams.MAX_NUM_TEMPLATE_SEGMENTS;

public class RlenStream {
    private final DATA_CLASS dataClass;
    private final Payload sub_streams[];
    private final DescriptorDecoder decoders[];
    private int j7_0 = 0;
    private final long reads_length;
    private final boolean splicedReadsFlag;

    long read_len[];

    public RlenStream(
            final DATA_CLASS dataClass,
            final DataUnitAccessUnit.Block block,
            final int reads_length,
            final boolean splicedReadsFlag,
            final EncodingParameters encodingParameters) {
        
        this.dataClass = dataClass;
        this.reads_length = reads_length;
        this.splicedReadsFlag = splicedReadsFlag;

        if(block == null){
            sub_streams = null;
            decoders = null;
            return;
        }

        CABAC_DescriptorDecoderConfiguration conf = encodingParameters.getDecoderConfiguration(
                DESCRIPTOR_ID.RLEN, dataClass
        );
        sub_streams = block.getPayloads();

        decoders = new DescriptorDecoder[sub_streams.length];
        for(byte substream_index=0; substream_index < sub_streams.length; substream_index++){
            decoders[substream_index] = conf.getDescriptorDecoder(
                    sub_streams[substream_index],
                    DESCRIPTOR_ID.RLEN,
                    substream_index,
                    encodingParameters.getAlphabetId()
            );
        }
    }

    public RlenStreamSymbol read(
            short number_of_record_segments,
            short number_of_alignedRecordSegments,
            int[][] hardClipsLength
    ) throws IOException {
        read_len = new long[number_of_record_segments];

        if(reads_length == 0){
            for(int segment_i=0; segment_i<number_of_record_segments; segment_i++){
                read_len[segment_i] = (int)(decoders[0].read()+1);
            }
        } else {
            for(int segment_i=0; segment_i<number_of_record_segments; segment_i++){
                if (
                        dataClass == DATA_CLASS.CLASS_I
                                || (dataClass == DATA_CLASS.CLASS_HM && segment_i == 0)
                ){
                    read_len[segment_i] = reads_length - hardClipsLength[segment_i][0] - hardClipsLength[segment_i][1];
                } else {
                    read_len[segment_i] = reads_length;
                }
            }
        }

        long[][] splicedSegLength = new long[MAX_NUM_TEMPLATE_SEGMENTS][1];
        for(int segment_i=0; segment_i<number_of_record_segments; segment_i++){
            splicedSegLength[segment_i][0] = read_len[segment_i];
        }

        if(splicedReadsFlag && (dataClass == DATA_CLASS.CLASS_I || dataClass == DATA_CLASS.CLASS_HM)){
            for(int segment_i=0; segment_i < number_of_alignedRecordSegments; segment_i++){
                if(reads_length == 0) {
                    read_len[segment_i] = decoders[0].read() + 1;
                }
                long remainingLen = read_len[segment_i];
                int number_splices = 0;
                do{
                    long spliceLen = decoders[0].read();
                    remainingLen -= spliceLen;
                    splicedSegLength[segment_i][number_splices] = spliceLen;
                    number_splices++;
                    if(number_splices == splicedSegLength[segment_i].length){
                        splicedSegLength[segment_i] = Arrays.copyOf(
                                splicedSegLength[segment_i],
                                number_splices*2
                        );
                    }
                }while (remainingLen > 0);

                splicedSegLength[segment_i] = Arrays.copyOf(splicedSegLength[segment_i], number_splices);
            }
        }
        return new RlenStreamSymbol(read_len, splicedSegLength);
    }

    public boolean hasNext() throws IOException {
        return decoders[0].hasNext();

    }
}

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
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.decoder.descriptors.S_alphabets;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

import static es.gencom.mpegg.decoder.descriptors.streams.StreamsConstantsParams.MAX_NUM_SOFT_CLIPS;

public class ClipsStream {
    private final DATA_CLASS dataClass;

    private final Payload sub_streams[];
    private final DescriptorDecoder decoders[];


    private long subSequence0;
    private byte[][][] soft_clips;
    private int[][] hard_clips;
    private final ALPHABET_ID alphabet_id;

    public ClipsStream(
            final DataUnitAccessUnit.Block block,
            final DATA_CLASS dataClass,
            final EncodingParameters encodingParameters) {

        CABAC_DescriptorDecoderConfiguration conf = encodingParameters.getDecoderConfiguration(
                DESCRIPTOR_ID.CLIPS,
                dataClass
        );

        this.dataClass = dataClass;
        this.alphabet_id = encodingParameters.getAlphabetId();

        if(block == null){
            sub_streams = null;
            decoders = null;
        }else {
            sub_streams = block.getPayloads();
            decoders = new DescriptorDecoder[sub_streams.length];
            for(int substream_index=0; substream_index < sub_streams.length; substream_index++) {
                decoders[substream_index] = conf.getDescriptorDecoder(
                        sub_streams[substream_index],
                        DESCRIPTOR_ID.CLIPS,
                        substream_index,
                        alphabet_id
                );
            }
        }
        subSequence0 = -1;
    }

    /**
     * Reads the clips stream and sets the variables soft_clips and hard_clips as required
     * @param current_record_count The 0-based index of the current index
     * @param numberOfRecordSegments The number of record segments in the segment, it will be the first
     *      *                                      dimension of the soft_clip and hard_clip attributes
     * @param numberOfAlignedRecordSegments The number of aligned record segments in the record
     * @throws IOException
     */
    public void read(
            long current_record_count,
            int numberOfRecordSegments,
            int numberOfAlignedRecordSegments
    ) throws IOException {
        byte[] s_alphabet_id = S_alphabets.alphabets[alphabet_id.ID];
        soft_clips = new byte[numberOfRecordSegments][MAX_NUM_SOFT_CLIPS][128];
        hard_clips = new int[numberOfRecordSegments][MAX_NUM_SOFT_CLIPS];
        int[][] softClipsLength = new int[numberOfRecordSegments][MAX_NUM_SOFT_CLIPS];

        if((dataClass != DATA_CLASS.CLASS_I && dataClass != DATA_CLASS.CLASS_HM) || decoders==null){
            for(int segment_i = 0; segment_i < numberOfAlignedRecordSegments; segment_i++){
                soft_clips[segment_i][0] = new byte[0];
                soft_clips[segment_i][1] = new byte[0];
            }
            return;
        }

        if(subSequence0 == -1 && decoders[0].hasNext()) {
            subSequence0 = decoders[0].read();
        }

        if (subSequence0 == current_record_count){
            int end = 0;
            do {
                final int subSequence1 = (int) decoders[1].read();
                if(subSequence1 <= 3){
                    long segmentIdx = subSequence1 >> 1;
                    byte template_i = (byte) (subSequence1 & 1);

                    long subSequence2 = decoders[2].read();
                    int terminator = s_alphabet_id.length;
                    do{
                        soft_clips
                                [Math.toIntExact(segmentIdx)]
                                [template_i]
                                [softClipsLength[Math.toIntExact(segmentIdx)][template_i]]
                                =
                                s_alphabet_id[(int)subSequence2];
                        softClipsLength[Math.toIntExact(segmentIdx)][template_i]++;
                        if(
                                soft_clips
                                    [Math.toIntExact(segmentIdx)]
                                    [template_i].length
                            == softClipsLength
                                    [Math.toIntExact(segmentIdx)]
                                    [template_i]){
                            soft_clips[Math.toIntExact(segmentIdx)][template_i]= Arrays.copyOf(
                                    soft_clips[Math.toIntExact(segmentIdx)][template_i],
                                    soft_clips[Math.toIntExact(segmentIdx)][template_i].length*2
                            );
                        }
                        subSequence2 = decoders[2].read();
                    }while(subSequence2 != terminator);
                } else if (subSequence1 <= 7){
                    long segmentIdx = (subSequence1 - 4) >> 1;
                    long template_i = (subSequence1 - 4) & 1;
                    hard_clips[Math.toIntExact(segmentIdx)][Math.toIntExact(template_i)] = (int) decoders[3].read();
                } else if (subSequence1 == 8){
                    end = 1;
                }
            }while (end == 0);
            subSequence0 = -1;
        }

        for(int segment_i = 0; segment_i < numberOfRecordSegments; segment_i++){
            soft_clips[segment_i][0] = Arrays.copyOf(
                    soft_clips[segment_i][0],
                    softClipsLength[segment_i][0]
            );
            soft_clips[segment_i][1] = Arrays.copyOf(
                    soft_clips[segment_i][1],
                    softClipsLength[segment_i][1]
            );
        }

    }

    public byte[][][] getSoft_clips() {
        return soft_clips;
    }

    public int[][] getHard_clips() {
        return hard_clips;
    }
}

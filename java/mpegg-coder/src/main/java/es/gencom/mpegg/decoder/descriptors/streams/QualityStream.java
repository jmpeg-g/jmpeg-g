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

import es.gencom.mpegg.coder.quality.AbstractQualityValueParameterSet;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.encoder.Operation;
import es.gencom.mpegg.coder.compression.*;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.dataunits.AccessUnitBlock;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

public class QualityStream {
    private final DATA_CLASS dataClass;
    private final Payload[] streams;
    private final DescriptorDecoder[] decoders;
    private final AbstractQualityValueParameterSet qualityValueParameterSet;
    private int[] qvCodeBookIds;

    private void decode_qv_codebook_indexes() throws IOException {
        if(qualityValueParameterSet.getNumberQualityBooks() >= 1){
            int pos = 0;
            qvCodeBookIds = new int[1024];
            while(decoders[1].hasNext()){
                qvCodeBookIds[pos] = Math.toIntExact(decoders[1].read());
                pos++;
                if(pos == qvCodeBookIds.length){
                    qvCodeBookIds = Arrays.copyOf(qvCodeBookIds, qvCodeBookIds.length*2);
                }
            }
        }
    }

    private int getCodebookId(
            int[] qvCodeBookIds,
            DATA_CLASS dataClass,
            boolean isAligned,
            int position
    ){
        if(!isAligned){
            if(dataClass == DATA_CLASS.CLASS_I || dataClass == DATA_CLASS.CLASS_HM){
                return qualityValueParameterSet.getNumberQualityBooks() - 1;
            }else{
                throw new IllegalArgumentException();
            }
        }else if(dataClass == DATA_CLASS.CLASS_U){
            return 0;
        }else if(qualityValueParameterSet.getNumberQualityBooks() > 1) { //todo caution the standard says here numCodebooksAligned
            return qvCodeBookIds[position];
        } else {
            return 0;
        }
    }

    public QualityStream(
            EncodingParameters encodingParameters,
            AccessUnitBlock block,
            DATA_CLASS dataClass
    ) throws IOException {
        this.dataClass = dataClass;
        if(block == null){
            this.streams = null;
            decoders = null;
            this.qualityValueParameterSet = null;
        }else {
            this.qualityValueParameterSet = encodingParameters.getQualityValueParameterSet(dataClass);
            this.streams = block.getPayloads();

            DescriptorDecoderConfiguration decoderConfiguration =
                    encodingParameters.getDecoderConfiguration(DESCRIPTOR_ID.QV, dataClass);

            decoders = new DescriptorDecoder[block.getPayloads().length];
            for(int substream_i=0; substream_i < block.getPayloads().length; substream_i++){
                decoders[substream_i] = decoderConfiguration.getDescriptorDecoder(
                        streams[substream_i],
                        DESCRIPTOR_ID.QV,
                        substream_i,
                        encodingParameters.getAlphabetId()
                );
            }
            decode_qv_codebook_indexes();
        }
    }

    public short[] getQualitiesAligned(
            byte[][] operations,
            int[][] operationLength,
            long[] spliceStart,
            long auStart
    ) throws IOException {
        boolean hasValue;
        if(decoders[0].hasNext()){
            hasValue = decoders[0].read() != 0;
        } else {
            hasValue = true;
        }

        if(hasValue){
            return decode_qvs(qualityValueParameterSet, dataClass, operations, operationLength, spliceStart, auStart);
        } else {
            return new short[0];
        }
    }

    public short[] getQualitiesUnaligned(
            int length
    ) throws IOException {
        boolean hasValue;
        if(decoders[0].hasNext()){
            hasValue = decoders[0].read() != 0;
        } else {
            hasValue = true;
        }

        if(hasValue){
            short[] result = new short[length];

            int qualityBookIndex = qualityValueParameterSet.getNumberQualityBooks()-1;

            for(int i=0; i<length; i++){
                result[i] = (short) decoders[qualityBookIndex+2].read();
            }
            return result;
        }else{
            return new short[0];
        }
    }

    private short[] decode_qvs(
            AbstractQualityValueParameterSet qualityValueParameterSet,
            DATA_CLASS dataClass,
            byte[][] operations,
            int[][] operationLength,
            long[] spliceSegmentStart,
            long auStart) throws IOException {

        int length = 0;
        for(int splice_i = 0; splice_i < operations.length; splice_i++) {
            for (int operation_i = 0; operation_i < operations[splice_i].length; operation_i++) {
                if (operations[splice_i][operation_i] != Operation.Delete && 
                    operations[splice_i][operation_i] != Operation.HardClip) {
                    
                    length += operationLength[splice_i][operation_i];
                }
            }
        }

        short[] quality_values = new short[length];

        int baseIdx = 0;
        for(int splice_i=0; splice_i < operations.length; splice_i++) {
            int positionInSplice = 0;
            int offset = Math.toIntExact(spliceSegmentStart[splice_i] - auStart);
            for (int operation_i = 0; operation_i < operations[splice_i].length; operation_i++) {
                final byte operation = operations[splice_i][operation_i];
                for (
                        int inOperationPos = 0;
                        inOperationPos < operationLength[splice_i][operation_i];
                        inOperationPos++
                ) {
                    if (operation == Operation.Delete || operation == Operation.HardClip) {
                        continue;
                    }
                    int codebookId = getCodebookId(
                            qvCodeBookIds,
                            dataClass,
                            operation != Operation.Insert && operation != Operation.SoftClip,
                            positionInSplice + offset);

                    int qvCodeBookSubSeq = codebookId + 2;
                    short entryIndex = (short) decoders[qvCodeBookSubSeq].read();

                    quality_values[baseIdx] = qualityValueParameterSet
                            .getQualityBook((byte) codebookId)
                            .decode(entryIndex);
                    if(operation != Operation.Insert && operation != Operation.SoftClip) {
                        positionInSplice++;
                    }
                    baseIdx++;
                }
            }
        }
        return quality_values;
    }
}

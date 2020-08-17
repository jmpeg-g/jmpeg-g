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

import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DecoderConfiguration;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.Payload;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class AccessUnitBlock {

    public final DESCRIPTOR_ID descriptor_id;
    private final Payload[] payloads;

    public AccessUnitBlock(final DESCRIPTOR_ID descriptor_id, 
                 final Payload originalPayload) {
        this.descriptor_id = descriptor_id;
        this.payloads = new Payload[]{originalPayload};
    }

    public AccessUnitBlock(final DESCRIPTOR_ID descriptor_id, 
                 final Payload[] originalPayloads) {
        this.descriptor_id = descriptor_id;
        this.payloads = originalPayloads;
    }

    public AccessUnitBlock copy() throws EOFException {
        final Payload[] copyPayloads = new Payload[payloads.length];
        for(int i = 0; i < payloads.length; i++){
            copyPayloads[i] = payloads[i];
        }

        return new AccessUnitBlock(descriptor_id, copyPayloads);
    }

    public Payload[] getPayloads() {
        return payloads;
    }

    private long sizeDescriptorSpecificData() {
        long size = 0;
        if(payloads.length == 1) {
            size += payloads[0].size();
        }else{
            for(int i=0; i < payloads.length-1; i++){
                size += 4;
                size += payloads[i].size();
            }
            size += payloads[payloads.length-1].size();
        }
        return size;
    }

    public long size() {
        long size = 5;
        size += sizeDescriptorSpecificData();
        return size;
    }

    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBoolean(false);
        writer.writeBits(descriptor_id.ID, 7);
        writer.writeInt((int) size()-5);
        writer.writePayload(getDescriptorSpecificData());
        writer.flush();
    }

    public Payload getSubstream(int substream_index) {
        return payloads[substream_index];
    }

    public Payload getDescriptorSpecificData() {
        ByteBuffer[] byteBuffers = new ByteBuffer[10];
        int numByteBuffers = 0;

        if(payloads.length == 1){
            ByteBuffer[] subsequenceByteBuffers = payloads[0].getByteBuffers();

            for(int i = 0; i < subsequenceByteBuffers.length; i++){
                int position = subsequenceByteBuffers[i].position();
                subsequenceByteBuffers[i].position(0);
                byteBuffers[numByteBuffers] = subsequenceByteBuffers[i].slice();
                subsequenceByteBuffers[i].position(position);

                numByteBuffers++;
                if(byteBuffers.length == numByteBuffers){
                    byteBuffers = Arrays.copyOf(byteBuffers, byteBuffers.length*2);
                }
            }
        } else {
            for(int i = 0; i < payloads.length - 1; i++) {
                ByteBuffer sizeByteBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
                sizeByteBuffer.putInt((int)payloads[i].size());
                sizeByteBuffer.position(0);
                byteBuffers[numByteBuffers] = sizeByteBuffer;
                numByteBuffers++;
                if(byteBuffers.length == numByteBuffers){
                    byteBuffers = Arrays.copyOf(byteBuffers, byteBuffers.length*2);
                }

                ByteBuffer[] subsequenceByteBuffers = payloads[i].getByteBuffers();
                for(
                        int subsequenceByteBuffers_i=0;
                        subsequenceByteBuffers_i<subsequenceByteBuffers.length;
                        subsequenceByteBuffers_i++
                ){
                    int position = subsequenceByteBuffers[subsequenceByteBuffers_i].position();
                    subsequenceByteBuffers[subsequenceByteBuffers_i].position(0);
                    byteBuffers[numByteBuffers] = subsequenceByteBuffers[subsequenceByteBuffers_i].slice();
                    subsequenceByteBuffers[subsequenceByteBuffers_i].position(position);

                    numByteBuffers++;
                    if(byteBuffers.length == numByteBuffers){
                        byteBuffers = Arrays.copyOf(byteBuffers, byteBuffers.length*2);
                    }
                }
            }

            ByteBuffer[] subsequenceByteBuffers = payloads[payloads.length-1].getByteBuffers();
            for(
                    int subsequenceByteBuffers_i=0;
                    subsequenceByteBuffers_i<subsequenceByteBuffers.length;
                    subsequenceByteBuffers_i++
            ){
                int position = subsequenceByteBuffers[subsequenceByteBuffers_i].position();
                subsequenceByteBuffers[subsequenceByteBuffers_i].position(0);
                byteBuffers[numByteBuffers] = subsequenceByteBuffers[subsequenceByteBuffers_i].slice();
                subsequenceByteBuffers[subsequenceByteBuffers_i].position(position);

                numByteBuffers++;
                if(byteBuffers.length == numByteBuffers){
                    byteBuffers = Arrays.copyOf(byteBuffers, byteBuffers.length*2);
                }
            }
        }
        return new Payload(Arrays.copyOf(byteBuffers, numByteBuffers));
    }

    public static Payload[] readSubsequences(
            final DESCRIPTOR_ID descriptorIdentifier,
            final EncodingParameters encodingParameters,
            final DATA_CLASS dataClass,
            final MPEGReader reader,
            long blockSize) throws IOException {

        DecoderConfiguration decoderConfiguration = encodingParameters.getDecoderConfiguration(
                descriptorIdentifier, dataClass);
        
        // TODO: why the hell decoderConfiguration should return incorrect numberSubstreams for the QV in a first place?
        final int numberSubstreams;
        if (descriptorIdentifier == DESCRIPTOR_ID.QV) {
            numberSubstreams = encodingParameters.getQualityValueParameterSet(dataClass).getNumberQualityBooks() + 2;
        } else {
            numberSubstreams = decoderConfiguration.getNumberSubsequences();
        }

        final Payload[] payloads = new Payload[numberSubstreams];

        for(int i = 0, n = payloads.length - 1; i < n; i++) {
            final long size = reader.readUnsignedInt();
            payloads[i] = reader.readPayload(size);
            blockSize -= size + 4;
        }
        payloads[payloads.length - 1] = reader.readPayload(blockSize);

        return payloads;
    }

    static AccessUnitBlock readBlock(
            final MPEGReader reader,
            final EncodingParameters encodingParameters,
            final DATA_CLASS dataClass) throws IOException {

        reader.readBoolean();
        DESCRIPTOR_ID descriptorIdentifier = DESCRIPTOR_ID.getDescriptorId((byte) reader.readBits(7));

        long blockSize = reader.readBits(32);

        Payload[] payloads = readSubsequences(
                descriptorIdentifier,
                encodingParameters,
                dataClass,
                reader,
                blockSize);

        reader.align();

        return new AccessUnitBlock(descriptorIdentifier, payloads);
    }
}

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

package es.gencom.mpegg.coder.dataunits;

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DataUnitRawReference extends AbstractDataUnit {
    
    private final long data_unit_size;
    private final int[] sequenceIds;
    private final Payload[] sequences;
    private final long[] startPos;
    private final long[] endPos;

    public DataUnitRawReference(
            DataUnits dataUnits,
            int[] sequenceIds,
            Payload[] sequences,
            long[] startPos,
            long[] endPos
    ) {
        super(DATAUNIT_TYPE_ID.RAW_REF, dataUnits);
        if(
                sequenceIds.length != sequences.length
                && sequenceIds.length != startPos.length
                && sequenceIds.length != endPos.length
        ){
            throw new IllegalArgumentException();
        }

        this.sequenceIds = sequenceIds;
        this.sequences = sequences;
        this.startPos = startPos;
        this.endPos = endPos;

        int numSequences = sequences.length;
        long computation_data_unit_size = 9; //data unit header
        computation_data_unit_size += 2;//seq_count
        computation_data_unit_size += 2*numSequences; //sequenceId
        computation_data_unit_size += 5*numSequences; //startPos
        computation_data_unit_size += 5*numSequences; //endPos;

        for(int sequence_i=0; sequence_i < numSequences; sequence_i++){
            computation_data_unit_size += endPos[sequence_i]-startPos[sequence_i];
        }
        data_unit_size = computation_data_unit_size;
    }

    @Override
    public void writeDataUnitContent(final MPEGWriter writer) throws IOException {
        writer.writeLong(data_unit_size);
        writer.writeUnsignedShort(sequenceIds.length);
        for(int sequence_i = 0; sequence_i < sequenceIds.length; sequence_i++){
            writer.writeUnsignedShort(sequenceIds[sequence_i]);
            writer.writeBits(startPos[sequence_i], 40);
            writer.writeBits(endPos[sequence_i], 40);
            sequences[sequence_i].rewind();
            writer.writePayload(sequences[sequence_i]);
        }
        writer.align();
    }
    
    public static DataUnitRawReference read(
            final MPEGReader reader,
            DataUnits dataUnits
    ) throws IOException {
        reader.readLong();
        
        final int seq_count =  reader.readUnsignedShort();

        int[] sequenceIds = new int[seq_count];
        Payload[] sequences = new Payload[seq_count];
        long[] startPos = new long[seq_count];
        long[] endPos = new long[seq_count];

        for(int sequence_i = 0; sequence_i < seq_count; sequence_i++){
            sequenceIds[sequence_i] = reader.readUnsignedShort();
            startPos[sequence_i] = reader.readBits(40);
            endPos[sequence_i] = reader.readBits(40);
            sequences[sequence_i] = reader.readPayload(
                    endPos[sequence_i] - startPos[sequence_i]
            );
        }
        
        return new DataUnitRawReference(
                dataUnits,
                sequenceIds,
                sequences,
                startPos,
                endPos
        );
    }

    public Payload getSequence(int sequenceIdentifier) {
        sequences[sequenceIdentifier].rewind();
        return sequences[sequenceIdentifier];
    }

    public Payload getSubsequence(
            int sequenceIdentifier,
            int startPosSubsequence,
            int endPosSubsequence
    ) throws IOException {

        ByteBuffer[] byteBuffersTmp;

        long requestStart = Long.max(startPos[sequenceIdentifier], startPosSubsequence);
        sequences[sequenceIdentifier].position(requestStart);
        long requestEnd = Long.min(endPos[sequenceIdentifier], endPosSubsequence);
        Payload tmpPayload = sequences[sequenceIdentifier].readPayload(requestEnd - requestStart);

        if(requestStart == startPosSubsequence && requestEnd == endPosSubsequence){
            return tmpPayload;
        }

        byteBuffersTmp = tmpPayload.getByteBuffers();
        boolean needStartPadding = requestStart != startPosSubsequence;
        boolean needEndPadding = requestEnd != endPosSubsequence;
        int sizeResult = byteBuffersTmp.length + (needStartPadding?1:0) + (needEndPadding?1:0);

        ByteBuffer[] result = new ByteBuffer[sizeResult];
        if(needStartPadding){
            byte[] paddingStart = new byte[(int) (requestStart - startPosSubsequence)];
            Arrays.fill(paddingStart, (byte) 'N');
            result[0] = ByteBuffer.wrap(paddingStart);
        }
        for(int i=needStartPadding?1:0, j=0; i<byteBuffersTmp.length; i++, j++){
            result[i] = byteBuffersTmp[j].slice();
        }
        if(needEndPadding){
            byte[] paddingEnd = new byte[(int) (endPosSubsequence - requestEnd)];
            Arrays.fill(paddingEnd, (byte) 'N');
            result[result.length-1] = ByteBuffer.wrap(paddingEnd);
        }


        return new Payload(result);
    }

    public DataUnitRawReference selectSubset(int[] requiredSequences) {
        int[] newSequenceIds = new int[requiredSequences.length];
        Payload[] newsequences = new Payload[requiredSequences.length];
        long[] newstartPos = new long[requiredSequences.length];
        long[] newendPos = new long[requiredSequences.length];

        for(int required_sequence_i = 0; required_sequence_i < requiredSequences.length; required_sequence_i++){
            int required_sequence = requiredSequences[required_sequence_i];
            newSequenceIds[required_sequence_i] = requiredSequences[required_sequence_i];
            newsequences[required_sequence_i] = sequences[required_sequence];
            newstartPos[required_sequence_i] = startPos[required_sequence];
            newendPos[required_sequence_i] = endPos[required_sequence];
        }

        return new DataUnitRawReference(
                getParentStructure(),
                newSequenceIds,
                newsequences,
                newstartPos,
                newendPos
        );
    }
}

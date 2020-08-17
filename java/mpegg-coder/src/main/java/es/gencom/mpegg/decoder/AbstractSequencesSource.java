/*
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

package es.gencom.mpegg.decoder;

import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

public abstract class AbstractSequencesSource {
    public abstract SequenceIdentifier getSequenceIdentifier(String sequenceName);

    /**
     * Obtains an entire portion of the reference and returns it as a payload.
     * @param sequenceIdentifier identifier of the sequence from which information must be obtained
     * @return A payload populated with the nucleotides stored on the sequence identified between the provided positions.
     */
    protected abstract Payload getSequence(SequenceIdentifier sequenceIdentifier);

    /**
     * Obtains a portion of the reference and returns it as a payload.
     * @param sequenceIdentifier identifier of the sequence from which information must be obtained
     * @param startPos first position (0-based) to be retrieved. This position is included in the result.
     * @param endPos last position (0-based) to be retrieved. This position is not included in the result.
     * @return A payload populated with the nucleotides stored on the sequence identified between the provided positions.
     * @throws IOException This exception can be caused by multiple sources.
     */
    public abstract Payload getSubsequence(SequenceIdentifier sequenceIdentifier, int startPos, int endPos) throws IOException;

    /**
     * Returns the provided sequence which is either shorten to the requested size, or padded with 'N' until reaching
     * requested size
     * @param sequence Array of bytes encoding sequences
     * @param startPos first position (0-based) to be retrieved. This position is included in the result.
     * @param endPos last position (0-based) to be retrieved. This position is not included in the result.
     * @return A copy of the provided sequence, either shortened to match size or padded with 'N's.
     */
    public static byte[] obtainSubsequence(byte[] sequence, int startPos, int endPos) {
        if (endPos <= sequence.length){
            return Arrays.copyOfRange(sequence, startPos, endPos);
        }else{
            byte[] result = new byte[endPos-startPos];
            System.arraycopy(sequence, startPos, result, 0, sequence.length-startPos);
            for(int pos_i = sequence.length; pos_i < endPos; pos_i++){
                result[pos_i-startPos] = 'N';
            }
            return result;
        }
    }
}

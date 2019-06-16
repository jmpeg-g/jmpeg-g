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

package es.gencom.mpegg.decoder;

import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.util.Arrays;

public abstract class AbstractSequencesSource {
    public abstract SequenceIdentifier getSequenceIdentifier(String sequenceName);
    public abstract String getSequenceName(SequenceIdentifier sequenceId);

    public final Payload getSubsequenceBytes(SequenceIdentifier sequenceIdentifier, int startPos, int endPos) throws IOException {
        return getSubsequence(sequenceIdentifier, startPos, endPos);
    }

    protected abstract Payload getSequence(SequenceIdentifier sequenceIdentifier);

    public Payload getSubsequence(String sequenceName, int startPos, int endPos) throws IOException {
        return getSubsequence(getSequenceIdentifier(sequenceName), startPos, endPos);
    }

    public abstract Payload getSubsequence(SequenceIdentifier sequenceIdentifier, int startPos, int endPos) throws IOException;

    public class SequenceInformationEntry{
        private String name;
        private int length;

        public SequenceInformationEntry(String name, int length) {
            this.name = name;
            this.length = length;
        }

        public String getName() {
            return name;
        }

        public int getLength() {
            return length;
        }
    }

    protected byte[] obtainSubsequence(byte[] sequence, int startPos, int endPos) {
        if (endPos <= sequence.length){
            return Arrays.copyOfRange(sequence, startPos, endPos);
        }else{
            byte[] result = new byte[endPos-startPos];
            System.arraycopy(sequence, startPos, result, 0, endPos-startPos);
            for(int pos_i = sequence.length - startPos; pos_i < endPos; pos_i++){
                result[pos_i] = 'N';
            }
            return result;
        }
    }
}

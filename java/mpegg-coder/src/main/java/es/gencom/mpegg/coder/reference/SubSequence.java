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

package es.gencom.mpegg.coder.reference;

public class SubSequence {
    private final byte referenceId;
    private final short sequenceId;
    private final long startPosition;
    private final long endPosition;
    private final byte subsequence[];

    public SubSequence(
            byte referenceId,
            short sequenceId,
            long startPosition,
            long endPosition,
            byte subsequence[]) {

        this.referenceId = referenceId;
        this.sequenceId = sequenceId;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.subsequence = subsequence;
    }

    public byte getReferenceId() {
        return referenceId;
    }

    public short getSequenceId() {
        return sequenceId;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public long getEndPosition() {
        return endPosition;
    }

    public byte[] getSubsequence() {
        return subsequence;
    }
}

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

import es.gencom.mpegg.format.SequenceIdentifier;

public class MMapStreamSymbol {
    private final int numberOfAlignments;
    private final int[] numberOfSegmentAlignments;
    private final long[] numberOfAlignmentsPairs;
    private final int[][] alignPtr;
    private final boolean moreAlignments;
    private final SequenceIdentifier moreAlignmentsNextSeqId;
    private final long moreAlignmentsNextPos;

    public MMapStreamSymbol(
            int numberOfAlignments,
            int[] numberOfSegmentAlignments,
            long[] numberOfAlignmentsPairs,
            int[][] alignPtr,
            boolean moreAlignments,
            SequenceIdentifier moreAlignmentsNextSeqId,
            long moreAlignmentsNextPos
    ) {
        this.numberOfAlignments = numberOfAlignments;
        this.numberOfSegmentAlignments = numberOfSegmentAlignments;
        this.numberOfAlignmentsPairs = numberOfAlignmentsPairs;
        this.alignPtr = alignPtr;
        this.moreAlignments = moreAlignments;
        this.moreAlignmentsNextSeqId = moreAlignmentsNextSeqId;
        this.moreAlignmentsNextPos = moreAlignmentsNextPos;
    }

    public int getNumberOfAlignments() {
        return numberOfAlignments;
    }

    public int[] getNumberOfSegmentAlignments() {
        return numberOfSegmentAlignments;
    }

    public long[] getNumberOfAlignmentsPairs() {
        return numberOfAlignmentsPairs;
    }

    public int[][] getAlignPtr() {
        return alignPtr;
    }

    public boolean isMoreAlignments() {
        return moreAlignments;
    }

    public SequenceIdentifier getMoreAlignmentsNextSeqId() {
        return moreAlignmentsNextSeqId;
    }

    public long getMoreAlignmentsNextPos() {
        return moreAlignmentsNextPos;
    }
}

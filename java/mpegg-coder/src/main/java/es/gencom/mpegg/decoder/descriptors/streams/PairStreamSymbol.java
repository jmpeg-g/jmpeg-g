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

import es.gencom.mpegg.SplitType;
import es.gencom.mpegg.format.SequenceIdentifier;

public class PairStreamSymbol {
    private final boolean unpairedRead;
    private final SequenceIdentifier[] mateSeqId;
    private final long[][] mappingPos;
    private final long[] mateAuId;
    private final long[] mateRecordIndex;
    private final SplitType[] splitMate;
    private final boolean read_1_first;

    public PairStreamSymbol(
            boolean unpairedRead,
            SequenceIdentifier[] mateSeqId,
            long[][] mappingPos,
            long[] mateAuId,
            long[] mateRecordIndex,
            SplitType[] splitMate,
            boolean read_1_first
    ) {
        this.unpairedRead = unpairedRead;
        this.mateSeqId = mateSeqId;
        this.mappingPos = mappingPos;
        this.mateAuId = mateAuId;
        this.mateRecordIndex = mateRecordIndex;
        this.splitMate = splitMate;
        this.read_1_first = read_1_first;
    }

    public boolean isUnpairedRead() {
        return unpairedRead;
    }

    public SequenceIdentifier[] getMateSeqId() {
        return mateSeqId;
    }

    public long[][] getMateMappingPos() {
        return mappingPos;
    }

    public long[] getMateAuId() {
        return mateAuId;
    }

    public long[] getMateRecordIndex() {
        return mateRecordIndex;
    }

    public SplitType[] getSplitMate() {
        return splitMate;
    }

    public boolean isRead_1_first() {
        return read_1_first;
    }
}

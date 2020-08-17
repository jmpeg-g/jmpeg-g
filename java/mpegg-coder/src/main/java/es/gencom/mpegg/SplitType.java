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

package es.gencom.mpegg;

public enum SplitType {
    MappedSameRecord((byte)0),
    UnmappedSameRecord((byte)1),
    MappedDifferentRecordSameSequence((byte)2),
    UnmappedDifferentRecordSameAU((byte)3),
    MappedDifferentRecordDifferentSequence((byte)4),
    UnmappedDifferentRecordDifferentAU((byte)5),
    Unpaired((byte)6);

    public final byte id;

    SplitType(final byte id) {
        this.id = id;
    }

    public boolean segmentSequenceInRecord(){
        switch (this){
            case MappedSameRecord:
            case UnmappedSameRecord:
                return true;
            case MappedDifferentRecordSameSequence:
            case UnmappedDifferentRecordSameAU:
            case MappedDifferentRecordDifferentSequence:
            case UnmappedDifferentRecordDifferentAU:
            case Unpaired:
                return false;
            default:
                throw new IllegalArgumentException();
        }
    }
}
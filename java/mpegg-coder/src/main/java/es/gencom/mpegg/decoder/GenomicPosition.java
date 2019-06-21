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

import java.util.Objects;

public class GenomicPosition implements Comparable<GenomicPosition>{
    private final boolean unmapped;
    private final SequenceIdentifier sequenceId;
    private final long position;

    private GenomicPosition(boolean unmapped, SequenceIdentifier sequenceId, long position) {
        if(!unmapped && sequenceId == null){
            throw new IllegalArgumentException();
        }
        this.unmapped = unmapped;
        this.sequenceId = sequenceId;
        this.position = position;
    }

    public GenomicPosition(SequenceIdentifier sequenceId, long position) {
        this(false, sequenceId, position);
    }

    public static GenomicPosition getUnmapped() {
        return new GenomicPosition(true, null, 0);
    }

    public boolean isUnmapped(){
        return unmapped;
    }

    public SequenceIdentifier getSequenceId() {
        return sequenceId;
    }

    public long getPosition() {
        return position;
    }

    public GenomicPosition advance(long distance){
        return new GenomicPosition(
                sequenceId,
                position + distance
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenomicPosition)) return false;
        GenomicPosition that = (GenomicPosition) o;
        return getPosition() == that.getPosition() &&
                Objects.equals(getSequenceId(), that.getSequenceId());
    }

    @Override
    public int compareTo(GenomicPosition genomicPosition) {
        if(isUnmapped() != genomicPosition.isUnmapped()){
            if(!isUnmapped()){
                return -1;
            }else{
                return 1;
            }
        }else{
            if(isUnmapped()){
                return 0;
            }
        }
        if(sequenceId.getSequenceIdentifier() != genomicPosition.getSequenceId().getSequenceIdentifier()){
            return Integer.compare(
                    sequenceId.getSequenceIdentifier(),
                    genomicPosition.getSequenceId().getSequenceIdentifier()
            );
        }else{
            return Long.compare(position, genomicPosition.getPosition());
        }
    }
}

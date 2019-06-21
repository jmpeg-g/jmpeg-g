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

package es.gencom.mpegg.format;

import java.util.Objects;

public class SequenceIdentifier implements Comparable{
    private final int sequenceIdentifier;

    public SequenceIdentifier(int sequenceIdentifier) {
        this.sequenceIdentifier = sequenceIdentifier;
    }

    public int getSequenceIdentifier() {
        return sequenceIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SequenceIdentifier)) return false;
        SequenceIdentifier that = (SequenceIdentifier) o;
        return getSequenceIdentifier() == that.getSequenceIdentifier();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSequenceIdentifier());
    }


    @Override
    public int compareTo(Object anotherObject) throws ClassCastException {
        if (!(anotherObject instanceof SequenceIdentifier))
            throw new ClassCastException("A Sequence Identifier expected.");
        int otherSequence = ((SequenceIdentifier) anotherObject).getSequenceIdentifier();
        return Integer.compare(sequenceIdentifier, otherSequence);
    }

    @Override
    public String toString() {
        return Integer.toString(sequenceIdentifier);
    }
}

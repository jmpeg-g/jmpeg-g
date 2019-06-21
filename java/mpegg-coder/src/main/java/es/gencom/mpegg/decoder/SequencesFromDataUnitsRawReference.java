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
import es.gencom.mpegg.coder.dataunits.DataUnitRawReference;

import java.io.IOException;

public class SequencesFromDataUnitsRawReference extends AbstractSequencesSource {
    private final DataUnitRawReference rawReference;
    private final String[] sequencesNames;

    public SequencesFromDataUnitsRawReference(DataUnitRawReference rawReference, String[] sequencesNames) {
        this.rawReference = rawReference;
        this.sequencesNames = sequencesNames;
    }

    @Override
    public SequenceIdentifier getSequenceIdentifier(String sequenceName) {
        for(short sequence_i=0; sequence_i < sequencesNames.length; sequence_i++){
            if(sequencesNames[sequence_i].equals(sequenceName)){
                return new SequenceIdentifier(sequence_i);
            }
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public String getSequenceName(SequenceIdentifier sequenceId) {
        return sequencesNames[sequenceId.getSequenceIdentifier()];
    }

    @Override
    protected Payload getSequence(SequenceIdentifier sequenceIdentifier) {
        return rawReference.getSequence(sequenceIdentifier.getSequenceIdentifier());
    }

    @Override
    public Payload getSubsequence(SequenceIdentifier sequenceIdentifier, int startPos, int endPos) throws IOException {
        return rawReference.getSubsequence(sequenceIdentifier.getSequenceIdentifier(), startPos, endPos);
    }
}

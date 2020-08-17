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

import es.gencom.mpegg.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.format.DatasetType;
import es.gencom.mpegg.format.SequenceIdentifier;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;

public class Sequence {
    private SequenceIdentifier sequenceIdentifier;
    private long[] subSequences_RefStart;
    private int insertedSubSequences = 0;
    private HashMap<Long, DataUnitAccessUnit> dataUnitAccessUnits;
    private HashMap<DataUnitAccessUnit, WeakReference<SubSequence>> decodedSubsequences;

    public Sequence(SequenceIdentifier sequenceIdentifier) {
        this.sequenceIdentifier = sequenceIdentifier;
        int initial_length = 128;
        subSequences_RefStart = new long[initial_length];
        for(int i=0; i<initial_length; i++){
            subSequences_RefStart[i] = Long.MAX_VALUE;
        }
        dataUnitAccessUnits = new HashMap<>();
        decodedSubsequences = new HashMap<>();
    }

    public void addSubSequence(DataUnitAccessUnit dataUnitAccessUnit){
        if (dataUnitAccessUnit.getParameter().getDatasetType() != DatasetType.REFERENCE){
            throw new InternalError("submitted data unit access unit of a wrong type for sequence");
        }
        if(!dataUnitAccessUnit.header.ref_sequence_id.equals(sequenceIdentifier)){
            throw new InternalError("submitted data unit access unit encodes a sequence different from the current one");
        }

        if(insertedSubSequences == subSequences_RefStart.length){
            subSequences_RefStart = Arrays.copyOf(subSequences_RefStart, 2*subSequences_RefStart.length);
        }

        int posToInsert =
                Arrays.binarySearch(subSequences_RefStart, dataUnitAccessUnit.header.ref_start_position);
        if(posToInsert >= 0){
            throw new InternalError("two data unit access unit with same sequence and sequence start");
        }else{
            posToInsert = ~posToInsert;
            System.arraycopy(
                    subSequences_RefStart,
                    posToInsert,
                    subSequences_RefStart,
                    posToInsert+1,
                    subSequences_RefStart.length-posToInsert-1
            );
            subSequences_RefStart[posToInsert] = dataUnitAccessUnit.header.ref_start_position;
        }

        dataUnitAccessUnits.put(dataUnitAccessUnit.header.ref_start_position, dataUnitAccessUnit);
    }

    public byte[] getSequenceFragment(long start, long end) throws IOException {
        if((end-start) > Integer.MAX_VALUE){
            throw new InternalError("attempting to retrieve an array which exceeds possible size");
        }
        byte result[] = new byte[(int) (end - start)];
        int currentResultSize = 0;

        int posToStartRetrieving =
                Arrays.binarySearch(subSequences_RefStart, start);
        if (posToStartRetrieving < 0){
            posToStartRetrieving = ~posToStartRetrieving -1;
            if (posToStartRetrieving < 0){
                throw new InternalError("missing information to perform query");
            }
        }


        int posRetrievingFrom = posToStartRetrieving;
        int size = (int) (end - start);
        int remainingSize = size;

        while (remainingSize > 0){

            SubSequence subSequence = obtainSubsequence(subSequences_RefStart[posRetrievingFrom]);
            byte obtainedByteSquence[] = subSequence.getSubsequence();
            int startPosition;
            if(start > subSequences_RefStart[posRetrievingFrom]){
                startPosition = (int) (start-subSequences_RefStart[posRetrievingFrom]);
            }else{
                startPosition = 0;
            }
            byte correctedObtainedByteSequence[] = Arrays.copyOfRange(
                obtainedByteSquence,
                startPosition,
                obtainedByteSquence.length
            );

            int sizeCopied;

            if(correctedObtainedByteSequence.length > remainingSize){
                sizeCopied = remainingSize;
            }else{
                sizeCopied = correctedObtainedByteSequence.length;
            }



            System.arraycopy(correctedObtainedByteSequence, 0, result, currentResultSize, sizeCopied);
            currentResultSize += sizeCopied;
            remainingSize -= sizeCopied;
            posRetrievingFrom++;
        }

        return result;
    }

    private SubSequence obtainSubsequence(long startPosition) throws IOException {
        DataUnitAccessUnit dataUnitAccessUnitToRetrieve = dataUnitAccessUnits.get(startPosition);
        WeakReference<SubSequence> subSequenceWeakReference = decodedSubsequences.get(dataUnitAccessUnitToRetrieve);
        if (subSequenceWeakReference == null || subSequenceWeakReference.get() == null){
            SubSequence subSequence = ReferenceSubSequenceAUDecoder.decode(
                    dataUnitAccessUnitToRetrieve.getParameter(),
                    dataUnitAccessUnitToRetrieve,
                    null
            );
            decodedSubsequences.put(dataUnitAccessUnitToRetrieve, new WeakReference<>(subSequence));
            return subSequence;
        }else{
            return subSequenceWeakReference.get();
        }
    }

    public SequenceIdentifier getSequenceIdentifier() {
        return sequenceIdentifier;
    }
}

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

import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;
import es.gencom.mpegg.format.DATA_CLASS;

import java.io.IOException;

public class ReferenceSubSequenceAUDecoder {
    public static SubSequence decode(
            DataUnitParameters dataUnitParameters,
            DataUnitAccessUnit dataUnitAccessUnit,
            Sequence sequence
    ) throws IOException {
        //todo change this
        byte referenceId = 0;

        if(dataUnitAccessUnit.getAUType() == DATA_CLASS.CLASS_U){
            throw new UnsupportedOperationException();
            /*return decodeAU_U(
                    dataUnitAccessUnit,
                    referenceId,
                    dataUnitParameters.getReadLength(),
                    S_alphabets.alphabets[ALPHABET_ID.DNA.ID]
            );*/
        }else{
            throw new UnsupportedOperationException();
        }
    }

    /*private static SubSequence decodeAU_U(
            DataUnitAccessUnit dataUnitAccessUnit,
            byte referenceId,
            long readsLength,
            byte alphabet[]
    ) throws IOException {

        RlenStream rlenStream = null;
        UReadsStream uReadsStream;
        int currentPosition = 0;

        DataUnitAccessUnit.Block rlenBlock = dataUnitAccessUnit.getBlockByDescriptorId(DESCRIPTOR_ID.RLEN);
        if(rlenBlock != null){
            rlenStream = new RlenStream(rlenBlock);
        }

        DataUnitAccessUnit.Block ureadBlock = dataUnitAccessUnit.getBlockByDescriptorId(DESCRIPTOR_ID.UREADS);
        if(ureadBlock == null){
            throw new IllegalArgumentException("required uread block not provided");
        }
        uReadsStream = new UReadsStream(ureadBlock);

        StringBuilder decodedSubSequence = new StringBuilder();

        byte result[] = new byte[1048576];

        while(true){
            long readLength;
            if (rlenStream != null){
                if(!rlenStream.hasNext()){
                    break;
                }else{
                    readLength = rlenStream.read(1)[0];
                }
            }else{
                readLength = readsLength;
            }

            byte decodedbases[] = uReadsStream.read((int)readLength, alphabet);

            if(currentPosition + readLength >= result.length){
                result = Arrays.copyOf(result, 2*result.length);
            }

            for(int i=0; i<readLength; i++){
                result[currentPosition] = decodedbases[i];
                currentPosition++;
            }
        }

        //todo change this:
        short sequenceId = 0;
        long startPosition = 0;
        long endPosition = startPosition + currentPosition;
        return new SubSequence(
                referenceId,
                sequenceId,
                startPosition,
                endPosition,
                Arrays.copyOfRange(result,0,currentPosition)
        );
    }*/
}

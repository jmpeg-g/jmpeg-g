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

package es.gencom.mpegg.coder.tokens;

import es.gencom.mpegg.io.MPEGReader;

import java.io.EOFException;
import java.io.IOException;

public class SubTokenRLEDecoder implements SubTokenSequenceDecoder{
    private final short guard;
    private final MPEGReader reader;
    private final long numOutputSymbols;

    private long readSymbols = 0;
    private short currentValue;
    private long remainingValues = 0;

    public SubTokenRLEDecoder(short guard, MPEGReader reader, long numOutputSymbols) {
        this.guard = guard;
        this.reader = reader;
        this.numOutputSymbols = numOutputSymbols;
    }

    private long getValue() throws IOException {
        if(readSymbols >= numOutputSymbols){
            throw new EOFException();
        }

        if(remainingValues == 0){
            currentValue = reader.readUnsignedByte();
            remainingValues = 1;

            if(currentValue == guard){
                remainingValues = reader.readVarSizedUnsignedInt();
                if(remainingValues == 0){
                    remainingValues = 1;
                    currentValue = guard;
                }else{
                    currentValue = reader.readUnsignedByte();
                }
            }
        }
        remainingValues--;
        readSymbols++;
        return currentValue;

    }

    @Override
    public boolean hasNext() {
        return readSymbols < numOutputSymbols;
    }

    @Override
    public short getSubTokenUnsignedByte() throws IOException {
        return (short) getValue();
    }
}

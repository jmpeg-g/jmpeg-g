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

public class RingBuffer<T>  {
    private final Object[] buffer;
    private int currentBufferSize;
    private int currentPosition;
    private final int maxSize;

    public RingBuffer(int maxSize){
        if(maxSize <= 0){
            throw new IllegalArgumentException();
        }
        buffer = new Object[maxSize];
        currentBufferSize = 0;
        currentPosition = -1;
        this.maxSize = maxSize;
    }

    private int userIndexToBufferIndex(int userIndex){
        if(userIndex >= currentBufferSize){
            throw new IndexOutOfBoundsException();
        }
        int result = (currentPosition - userIndex) % maxSize;
        if (result < 0)
        {
            result += maxSize;
        }
        return result;
    }

    public void addValue(T newValue){
        currentPosition = (currentPosition + 1) % maxSize;
        currentBufferSize = Integer.min(currentBufferSize+1, maxSize);
        buffer[currentPosition] = newValue;
    }

    public T getValue(int pos){
        int bufferIndex = userIndexToBufferIndex(pos);
        return (T)buffer[bufferIndex];
    }

    public int getSize(){
        return currentBufferSize;
    }
}

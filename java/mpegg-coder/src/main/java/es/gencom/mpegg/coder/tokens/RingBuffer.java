/*
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

    /**
     * Creates a new Ring buffer
     * @param maxSize Maximum size of the ring buffer. If more elements than ring buffer are added to the structure,
     *                the oldest element will be overwritten.
     */
    RingBuffer(int maxSize){
        if(maxSize <= 0){
            throw new IllegalArgumentException();
        }
        buffer = new Object[maxSize];
        currentBufferSize = 0;
        currentPosition = -1;
        this.maxSize = maxSize;
    }

    /**
     * This method maps the indexation scheme ordering by inverse order of insertion, to the position in the array
     * storing the information
     * @param userIndex The index  in inverse order of insertion: i.e. 0 corresponds to the last inserted one, 1 to the
     *                  last but one and so on.
     * @return The position on the array which corresponds to the given index.
     */
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

    /**
     * Inserts a new value in the ring buffer. If the maximal size of the ring buffer (defined when calling the
     * constructor is reached), the oldest value is overwritten.
     * @param newValue Value to insert.
     */
    public void addValue(T newValue){
        currentPosition = (currentPosition + 1) % maxSize;
        currentBufferSize = Integer.min(currentBufferSize+1, maxSize);
        buffer[currentPosition] = newValue;
    }

    /**
     * Gets the value from the ring buffer at the indicated position.
     * @param pos The variable indexes the values in inverse order of insertion: i.e. 0 corresponds to the last inserted
     *            one, 1 to the last but one and so on.
     * @return
     */
    public T getValue(int pos){
        int bufferIndex = userIndexToBufferIndex(pos);
        return (T)buffer[bufferIndex];
    }

    /**
     *
     * @return the current size of the ring buffer.
     */
    public int getSize(){
        return currentBufferSize;
    }
}

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

import java.util.HashMap;
import java.util.Objects;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class BidirectionalMap<T1, T2> {
    private final HashMap<T1, T2> mapOne;
    private final HashMap<T2, T1> mapTwo;

    public BidirectionalMap() {
        this.mapOne = new HashMap<>();
        this.mapTwo = new HashMap<>();
    }

    public void put(T1 elem1, T2 elem2){
        mapOne.put(elem1, elem2);
        mapTwo.put(elem2, elem1);
    }

    public T2 getForward(T1 key){
        return mapOne.get(key);
    }

    public T1 getReverse(T2 key){
        return mapTwo.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BidirectionalMap)) return false;
        BidirectionalMap<?, ?> that = (BidirectionalMap<?, ?>) o;
        return Objects.equals(mapOne, that.mapOne) &&
                Objects.equals(mapTwo, that.mapTwo);
    }

    @Override
    public int hashCode() {

        return Objects.hash(mapOne, mapTwo);
    }
}

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

public class OffsetToAccessUnit implements Comparable<OffsetToAccessUnit>{
    private final long offset;
    private final AccessUnitContainer accessUnitContainer;

    public OffsetToAccessUnit(final long offset, final AccessUnitContainer accessUnitContainer) {
        this.offset = offset;
        this.accessUnitContainer = accessUnitContainer;
    }

    public AccessUnitContainer getAccessUnitContainer() {
        return accessUnitContainer;
    }

    @Override
    public int compareTo(OffsetToAccessUnit o) {
        return Long.compare(offset, o.offset);
    }
}
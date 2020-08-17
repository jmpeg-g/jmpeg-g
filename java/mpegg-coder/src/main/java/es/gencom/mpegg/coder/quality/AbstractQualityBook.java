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

package es.gencom.mpegg.coder.quality;

public abstract class AbstractQualityBook {

    /**
     *
     * @return Return the number of entries (i.e. the number of unique qualities which are configured in the quality
     * book
     */
    public abstract byte getNumberEntries();

    /**
     * Returns the entry index which minimizes the distance to the quality given as parameter
     * @param qualitySAM the quality value to encode. It must be a valid quality.
     * @return entry index minimizing distance to the value of qualitySAM
     */
    public abstract short encode(short qualitySAM);

    /**
     *
     * @param encodedQuality the quality book entry for which the associated quality must be returned. The parameter
     *                       value must be between 0 (included) and the number of entries (excluded)
     * @return the quality value associated to the entry index given as input
     */
    public abstract short decode(short encodedQuality);
}

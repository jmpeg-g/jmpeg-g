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

package es.gencom.mpegg.CABAC.encoder;

import java.io.IOException;

/**
 * <p>
 * An abstract EQUALITY encoder that implements 12.6.2.9.1 Equality transformation.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class EqualityCodingEncoder {
    
    private long prevValue;
    
    public void write(final long value) throws IOException {
        if (value == prevValue) {
            encode_flag(1);
        } else {
            encode_symbol(value < prevValue ? value : value - 1);
            prevValue = value;
        }
    }
    
    protected abstract void encode_flag(final long value) throws IOException;
    protected abstract void encode_symbol(final long value) throws IOException;
}

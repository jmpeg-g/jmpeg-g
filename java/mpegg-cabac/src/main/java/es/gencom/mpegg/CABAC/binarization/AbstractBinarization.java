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

package es.gencom.mpegg.CABAC.binarization;

import es.gencom.mpegg.io.BitWriter;
import java.io.IOException;

/**
 * An abstract class which all MPEG-G binarization implementations must extend from.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public abstract class AbstractBinarization {
    
    public final BINARIZATION_ID binarization_id;
    
    public AbstractBinarization(final BINARIZATION_ID binarization) {
        this.binarization_id = binarization;
    }
    
    /**
     * Calculates the number of contexts per subsymbol (12.3.6.2).
     * 
     * @param numAlphaSubsym 
     * @return the number of contexts for sub-symbol for this binarization
     */
    public abstract int getNumContextsSymbol(long numAlphaSubsym);

    /**
     * Encode the symbol using this binarization.
     * 
     * @param writer MCoder writer to write the symbol
     * @param sym_val the symbol to be written
     * @throws IOException 
     */
    public abstract void encode(MCoderBitWriter writer, long sym_val) throws IOException;
    
    /**
     * Decode the symbol using this binarization.
     * 
     * @param reader MDecoder reader to read the symbol from
     * @return decoded symbol
     * @throws IOException 
     */
    public abstract long decode(MCoderBitReader reader) throws IOException;
    
    /**
     * Write CABAC binarizations parameters (12.3.3.1).
     * 
     * @param writer bit stream writer
     * @throws IOException 
     */
    public abstract void write(BitWriter writer) throws IOException;

    public abstract long sizeInBits();
}

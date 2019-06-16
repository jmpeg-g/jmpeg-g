/**
 * *****************************************************************************
 * Copyright (C) 2015 Spanish National Bioinformatics Institute (INB) and
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

package es.gencom.integration.fasta;

/**
 *
 * @author Dmitry Repchevsky
 */
public class FastaSequence extends TFastaSequence {
    public final long position;
    public final byte[] sequence;

    public FastaSequence(final String header, 
                         final long position, 
                         final int length, 
                         final boolean multiline) {
        
        super(header, length, multiline);
        
        this.position = position;
        this.sequence = null;
    }
    
    public FastaSequence(final String header, 
                         final long position, 
                         final byte[] sequence, 
                         final boolean multiline) {
        
        super(header, sequence.length, multiline);
        
        this.position = position;
        this.sequence = sequence;
    }
}

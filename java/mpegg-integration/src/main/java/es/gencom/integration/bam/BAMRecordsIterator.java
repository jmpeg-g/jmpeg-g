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

package es.gencom.integration.bam;

import java.io.IOException;
import java.util.Iterator;
import java.util.zip.DataFormatException;

/**
 * @author Dmitry Repchevsky
 */

public class BAMRecordsIterator implements Iterator<BAMRecord> {
    
    final BAMFileInputStream in;
            
    public BAMRecordsIterator(final BAMFileInputStream in) {
        this.in = in;
    }

    @Override
    public boolean hasNext() {
        try {
            return in.available() >= 0;
        } catch(IOException ex) {}
        return false;
    }

    @Override
    public BAMRecord next() {
        try {
            return BAMRecord.decode(in);
        } catch(IOException | DataFormatException ex) {}
        return null;
    }
}

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

public class UnmappedBAMRecordsIterator implements Iterator<BAMRecord> {

    private final BAI bai;
    private final BAMFileInputStream in;
    
    private int current_ref_id;

    public UnmappedBAMRecordsIterator(
            final BAMFileInputStream in,
            final BAI bai) throws IOException {
        
        this.bai = bai;
        this.in = in;
        
        current_ref_id = 0;
    }
    
    public UnmappedBAMRecordsIterator(
            final BAMFileInputStream in,
            final BAI bai,
            final int idRef) throws IOException {
        
        this.bai = bai;
        this.in = in;
        
        current_ref_id = -idRef;
    }
        
    @Override
    public boolean hasNext() {
        try {
            final long current_chunk_pos = in.index();

            if (current_ref_id < 0) {
                if (Bin.PSEUDO_BIN < bai.indexes[-current_ref_id].length &&
                    bai.indexes[-current_ref_id][Bin.PSEUDO_BIN].chunk_end[1] > 0) {

                    if (current_chunk_pos < bai.indexes[-current_ref_id][Bin.PSEUDO_BIN].chunk_beg[0]) {
                        in.move(bai.indexes[-current_ref_id][Bin.PSEUDO_BIN].chunk_beg[0]);
                        return true;
                    }
                    return current_chunk_pos < bai.indexes[-current_ref_id][Bin.PSEUDO_BIN].chunk_end[0];
                }
                return false;
            }

            for (;current_ref_id < bai.indexes.length; current_ref_id++) {
                if (Bin.PSEUDO_BIN < bai.indexes[current_ref_id].length && 
                    bai.indexes[current_ref_id][Bin.PSEUDO_BIN].chunk_end[1] > 0) {
                    if (current_chunk_pos < bai.indexes[current_ref_id][Bin.PSEUDO_BIN].chunk_beg[0] ||
                        current_chunk_pos > bai.indexes[current_ref_id][Bin.PSEUDO_BIN].chunk_end[0]) {
                        in.move(bai.indexes[current_ref_id][Bin.PSEUDO_BIN].chunk_beg[0]);
                        return true;
                    }
                    if (current_chunk_pos < bai.indexes[current_ref_id][Bin.PSEUDO_BIN].chunk_end[0]) {
                        return true;
                    }
                }
            }
        } catch (IOException ex) {}

        return false;
    }

    @Override
    public BAMRecord next() {
        while (hasNext()) {
            try {
                if (in.index() < bai.indexes[Math.abs(current_ref_id)][Bin.PSEUDO_BIN].chunk_end[0]) {
                    final BAMRecord record = BAMRecord.decode(in);
                    if (record.isUnmappedSegment()) {
                        return BAMRecord.decode(in);
                    }
                }
            } catch (IOException | DataFormatException ex) {}
        }
        return null;
    }
}

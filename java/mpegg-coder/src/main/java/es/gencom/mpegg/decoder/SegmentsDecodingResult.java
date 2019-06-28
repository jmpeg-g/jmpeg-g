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

package es.gencom.mpegg.decoder;

public class SegmentsDecodingResult {
    private final byte[][] decode_sequences;
    private final byte[][][][] operations;
    private final int[][][][] operationLength;
    private final byte[][][][] original_nucleotides;

    public SegmentsDecodingResult(
            byte[][][] decode_sequences_perSplice,
            byte[][][][] operations,
            int[][][][] operationLength,
            byte[][][][] original_nucleotides) {

        decode_sequences = new byte[decode_sequences_perSplice.length][];
        for(int segment_i = 0; segment_i < decode_sequences_perSplice.length; segment_i++) {
            int totalLength = 0;
            for(int splice_i = 0; splice_i < decode_sequences_perSplice[segment_i].length; splice_i++) {
                totalLength += decode_sequences_perSplice[segment_i][splice_i].length;
            }
            decode_sequences[segment_i] = new byte[totalLength];
            int currentLength = 0;
            for(int splice_i=0; splice_i < decode_sequences_perSplice[segment_i].length; splice_i++) {
                System.arraycopy(
                        decode_sequences_perSplice[segment_i][splice_i],
                        0,
                        decode_sequences[segment_i],
                        currentLength,
                        decode_sequences_perSplice[segment_i][splice_i].length
                );
                currentLength += decode_sequences_perSplice[segment_i][splice_i].length;
            }
        }
        this.operations = operations;
        this.operationLength = operationLength;
        this.original_nucleotides = original_nucleotides;
    }

    public byte[][] getDecode_sequences() {
        return decode_sequences;
    }

    public byte[][][][] getOperations() {
        return operations;
    }

    public int[][][][] getOperationLength() {
        return operationLength;
    }

    public byte[][][][] getOriginal_nucleotides() {
        return original_nucleotides;
    }
}

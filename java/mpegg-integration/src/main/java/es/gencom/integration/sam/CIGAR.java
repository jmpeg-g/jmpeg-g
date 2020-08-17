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

package es.gencom.integration.sam;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dmitry Repchevsky
 */

public class CIGAR implements Iterable<Map.Entry<Byte, Long>> {
    private final static String CIGAR = "MIDNSHP=X";
    private final static Pattern PATTERN = Pattern.compile("([0-9]+)([MIDNSHP=X]{1})");

    private int length;
    private long[] cigar;

    public CIGAR() {}
    
    public CIGAR(final String cigar) {
        this.cigar = encode(cigar);
    }
    
    public CIGAR(final long[] cigar) {
        this.cigar = cigar;
    }

    public int getLength() {
        if (cigar != null && length == 0) {
            length = getLength(cigar);
        }
        return length;
    }
    
    @Override
    public String toString() {
        return cigar != null ? parse(cigar) : "";
    }
    
    @Override
    public Iterator<Map.Entry<Byte, Long>> iterator() {
        List<Map.Entry<Byte, Long>> operations = new ArrayList<>();
        if (cigar != null) {
            for (int i = 0, n = cigar.length; i < n; i++) {
                final long cigar_op = cigar[i];
                final int op = (int)(cigar_op & 0x0F);
                final long op_len = cigar_op >>> 4;
                operations.add(new AbstractMap.SimpleImmutableEntry<>((byte)CIGAR.charAt(op), op_len));
            }
        }
        return operations.iterator();
    }

    /**
     * Encodes CIGAR string into binary BAM representation
     * 
     * @param cigar CIGAR string
     * @return CIGAR in BAM binary format
     */
    public static long[] encode(final String cigar) {
        final Matcher m = PATTERN.matcher(cigar);

        long[] arr = null;
        while (m.find()) {
            final String ln = m.group(1);
            final String ch = m.group(2);
            if (ln != null && ch != null) {
                if (arr == null) {
                    arr = new long[1];
                } else {
                    arr = Arrays.copyOf(arr, arr.length + 1);
                }
                arr[arr.length - 1] = Integer.parseInt(ln) << 4 | CIGAR.indexOf(ch);
            }
        }
        return arr;
    }
    
    public static String parse(final long[] cigar) {
        final StringBuilder cigar_str = new StringBuilder();
        for (int i = 0, n = cigar.length; i < n; i++) {
            final long cigar_op = cigar[i];
            final int op = (int)(cigar_op & 0x0F);
            final long op_len = cigar_op >>> 4;
            cigar_str.append(Long.toString(op_len));
            cigar_str.append(CIGAR.charAt(op));
        }
        return cigar_str.toString();
    }
    
    public static int getLength(final long[] cigar) {
        int length = 0;
        for (int i = 0, n = cigar.length; i < n; i++) {
            final long cigar_op = cigar[i];
            final int op = (int)(cigar_op & 0x0F);
            switch(op) {
                case 0: // "M" - match
                case 2: // "D" - delete
                case 3: // "N" - skip
                case 6: // "P" - padding
                case 7: // "=" - seq match
                case 8: length += cigar_op >>> 4; // "X" - substitution
            }
        }
        return length;
    }
}

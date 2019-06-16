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

package es.gencom.mpegg.coder.compression.transform;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 * Large alphabet (long) DC3 algorithm (Difference Cover, size 3) implementation
 * (doi:10.1007/3-540-45061-0_73).
 * 
 * @author Dmitry Repchevsky
 */

public class DC3 {
    
    /**
     * Construct suffix array for long values.
     * 
     * @param s  original data
     * @param sa resulted suffix array
     */
    public static void dc3(final LongBuffer s, final int[] sa) {

        final int n12 = (s.limit() + s.limit()) / 3;
        
        int[] sa12 = new int[n12];

        for(int i = 0, j = 1, n = s.limit(); j < n; i++, j += j % 3) {
            sa12[i] = j;
        }
        
        sa12 = radix(s, sa12, 2);
        sa12 = radix(s, sa12, 1);
        sa12 = radix(s, sa12, 0);
        
        long[] rank12 = new long[s.limit()];
        if (rank(s, sa12, rank12)) {
            int[] s2s = new int[n12 + 2]; // add two zeros after each last suffix

            for (int i = 0, n = (sa12.length + 3) >> 1; i < sa12.length; i++) {
                final int idx = (sa12[i] << 1) / 3;
                s2s[(idx & 1) == 0 ? idx >> 1 : (idx >> 1) + n] = (int)rank12[i * 3 / 2 + 1];
            }
            
            dc3(IntBuffer.wrap(s2s), sa12);

            for(int i = 0, n = (sa12.length + 3) >> 1; i < n12 ; i++) {
                final int idx = sa12[i] < n ? sa12[i] << 1 : (sa12[i] - n) << 1 | 1;
                sa12[i] = idx * 3 / 2 + 1;
            }
        }
        
        for (int i = 0; i < n12; i++) {
            rank12[sa12[i]] = i + 1;
        }

        int[] sa0 = new int[s.limit() - sa12.length];
        for (int i = 0, j = 0, n = s.limit(); i < n; i += 3, j++) {
            sa0[j] = i;
            rank12[i] = s.get(i);
        }
        
        /* sort b0, b12 pairs */
        final LongBuffer buf12 = LongBuffer.wrap(rank12);
        sa0 = radix(buf12, sa0, 1);
        sa0 = radix(buf12, sa0, 0);

        merge(s, sa0, sa12, rank12, sa);
    }

    /**
     * Construct suffix array for integer values.
     * 
     * @param s  original data
     * @param sa resulted suffix array
     */
    public static void dc3(final IntBuffer s, final int[] sa) {

        final int n12 = (s.limit() + s.limit()) / 3;
        
        int[] sa12 = new int[n12];

        for(int i = 0, j = 1, n = s.limit(); j < n; i++, j += j % 3) {
            sa12[i] = j;
        }
        
        sa12 = radix(s, sa12, 2);
        sa12 = radix(s, sa12, 1);
        sa12 = radix(s, sa12, 0);
        
        int[] rank12 = new int[s.limit()];
        if (rank(s, sa12, rank12)) {
            long[] s2s = new long[n12 + 2]; // add two zeros after each last suffix

            for (int i = 0, n = (sa12.length + 3) >> 1; i < sa12.length; i++) {
                final int idx = (sa12[i] << 1) / 3;
                s2s[(idx & 1) == 0 ? idx >> 1 : (idx >> 1) + n] = rank12[i * 3 / 2 + 1];
            }
            
            dc3(LongBuffer.wrap(s2s), sa12);

            for(int i = 0, n = (sa12.length + 3) >> 1; i < n12 ; i++) {
                final int idx = sa12[i] < n ? sa12[i] << 1 : (sa12[i] - n) << 1 | 1;
                sa12[i] = idx * 3 / 2 + 1;
            }
        }
        
        for (int i = 0; i < n12; i++) {
            rank12[sa12[i]] = i + 1;
        }

        int[] sa0 = new int[s.limit() - sa12.length];
        for (int i = 0, j = 0, n = s.limit(); i < n; i += 3, j++) {
            sa0[j] = i;
            rank12[i] = s.get(i);
        }
        
        /* sort b0, b12 pairs */
        final IntBuffer buf12 = IntBuffer.wrap(rank12);
        sa0 = radix(buf12, sa0, 1);
        sa0 = radix(buf12, sa0, 0);

        merge(s, sa0, sa12, rank12, sa);
    }

    /**
     * Radix sort for long values
     * 
     * @param s      sorting data
     * @param sa12   sorting indexes
     * @param offset indexes offset
     * 
     * @return sorted sa12 indexes
     */
    public static int[] radix(LongBuffer s, int[] sa12, int offset) {
        
        int[] _idx12 = new int[sa12.length];
        
        long mask = 0;
        long and = 0xFFFFFFFFFFFFFFFFL;

        int j = 0;
        for (int i = 0; i < sa12.length; i++) {
            final long ch = s(s, sa12[i] + offset);
            mask |= ch; and &= ch;
            
            if ((ch & 1L) == 0) {
                _idx12[j++] = sa12[i];
            }
        }
        for (int i = 0; j < sa12.length; i++) {
            if ((s(s, sa12[i] + offset) & 1L) != 0) {
                _idx12[j++] = sa12[i];
            }
        }
        
        mask ^= and; // mark '1' all columns that are to be sorted
        
        for (long bit = 2; (mask >>>= 1) != 0; bit <<= 1) {
            if ((mask & 1) != 0) {
                j = 0;
                for (int i = 0; i < sa12.length; i++) {
                    if ((s(s, _idx12[i] + offset) & bit) == 0) {
                        sa12[j++] = _idx12[i];
                    }
                }
                for (int i = 0; j < sa12.length; i++) {
                    if ((s(s, _idx12[i] + offset) & bit) != 0) {
                        sa12[j++] = _idx12[i];
                    }
                }

                int[] tmp = sa12;
                sa12 = _idx12;
                _idx12 = tmp;
            }
        }
        return _idx12;
    }

    /**
     * Radix sort for integer values
     * 
     * @param s      sorting data
     * @param sa12   sorting indexes
     * @param offset indexes offset
     * 
     * @return sorted sa12 indexes
     */
    public static int[] radix(IntBuffer s, int[] sa12, int offset) {
        
        int[] _idx12 = new int[sa12.length];
        
        int mask = 0;
        int and = 0xFFFFFFFF;

        int j = 0;
        for (int i = 0; i < sa12.length; i++) {
            final int ch = s(s, sa12[i] + offset);
            mask |= ch; and &= ch;
            
            if ((ch & 1L) == 0) {
                _idx12[j++] = sa12[i];
            }
        }
        for (int i = 0; j < sa12.length; i++) {
            if ((s(s, sa12[i] + offset) & 1) != 0) {
                _idx12[j++] = sa12[i];
            }
        }
        
        mask ^= and; // mark '1' all columns that are to be sorted
        
        for (int bit = 2; (mask >>>= 1) != 0; bit <<= 1) {
            if ((mask & 1) != 0) {
                j = 0;
                for (int i = 0; i < sa12.length; i++) {
                    if ((s(s, _idx12[i] + offset) & bit) == 0) {
                        sa12[j++] = _idx12[i];
                    }
                }
                for (int i = 0; j < sa12.length; i++) {
                    if ((s(s, _idx12[i] + offset) & bit) != 0) {
                        sa12[j++] = _idx12[i];
                    }
                }

                int[] tmp = sa12;
                sa12 = _idx12;
                _idx12 = tmp;
            }
        }
        return _idx12;
    }
    
    /**
     * Generates lexicographic names (ranks) for the suffixes.
     * 
     * @param s    sorting data
     * @param sa12 sorted '12' suffixes
     * @param rank resulted lexicographic names (ranks)
     * 
     * @return true if there are some equal names
     */
    private static boolean rank(
            final LongBuffer s, 
            final int[] sa12, 
            final long[] rank) {

        int r = 1;
        rank[1] = 1;

        for(int i = 1, j = 2, n = s.limit(); j < n; i++, j += j % 3) {
            if (s(s, sa12[i]) == s(s, sa12[i - 1]) &&
                s(s, sa12[i] + 1) == s(s, sa12[i - 1] + 1) &&
                s(s, sa12[i] + 2) == s(s, sa12[i - 1] + 2)) {
                rank[j] = r;
            } else {
                rank[j] = ++r;
            }
        }
        return r != sa12.length;
    }

    /**
     * Generates lexicographic names (ranks) for the suffixes.
     * 
     * @param s    sorting data
     * @param sa12 sorted '12' suffixes
     * @param rank resulted lexicographic names (ranks)
     * 
     * @return true if there are some equal names
     */
    private static boolean rank(
            final IntBuffer s, 
            final int[] sa12, 
            final int[] rank) {

        int r = 1;
        rank[1] = 1;

        for(int i = 1, j = 2, n = s.limit(); j < n; i++, j += j % 3) {
            if (s(s, sa12[i]) == s(s, sa12[i - 1]) &&
                s(s, sa12[i] + 1) == s(s, sa12[i - 1] + 1) &&
                s(s, sa12[i] + 2) == s(s, sa12[i - 1] + 2)) {
                rank[j] = r;
            } else {
                rank[j] = ++r;
            }
        }
        return r != sa12.length;
    }

    /**
     * Merges two suffix arrays sa0 and sa12 into the sa.
     * 
     * @param s     sorting data
     * @param sa0   sorted '0' suffixes
     * @param sa12  sorted '12' suffixes
     * @param r12   '12' ranks
     * @param sa    resulted merged suffixes
     */
    private static void merge(
            final LongBuffer s,
            final int[] sa0, 
            final int[] sa12,
            final long[] r12,
            final int[] sa) {

        int i = 0;
        int j = 0;
        int k = sa.length - r12.length - 1;
        
        while (i < sa12.length && j < sa0.length) {
            
            final int b0 = sa0[j];
            final int b12 = sa12[i];

            final int idx = 
                s.get(b12) < s.get(b0) ||
               (s.get(b12) == s.get(b0) &&
               (b12 % 3 == 1 ? b0 + 1 < r12.length &&
                              (b12 + 1 >= r12.length || r12[b12 + 1] < r12[b0 + 1])
                    : s(s, b12 + 1) < s(s, b0 + 1) ||
                     (s(s, b12 + 1) == s(s, b0 + 1) &&
                      b0 + 2 < r12.length &&
                     (b12 + 2 >= r12.length || r12[b12 + 2] < r12[b0 + 2]))))
                    ? sa12[i++] : sa0[j++];
            if (++k >= 0) {
                sa[k] = idx;
            }
        }
        while (i < sa12.length) {
            sa[++k] = sa12[i++];
        }
        while (j < sa0.length) {
            sa[++k] = sa0[j++];
        }
    }

    /**
     * Merges two suffix arrays sa0 and sa12 into the sa.
     * 
     * @param s     sorting data
     * @param sa0   sorted '0' suffixes
     * @param sa12  sorted '12' suffixes
     * @param r12   '12' ranks
     * @param sa    resulted merged suffixes
     */
    private static void merge(
            final IntBuffer s,
            final int[] sa0, 
            final int[] sa12,
            final int[] r12,
            final int[] sa) {

        int i = 0;
        int j = 0;
        int k = sa.length - r12.length - 1;
        
        while (i < sa12.length && j < sa0.length) {
            
            final int b0 = sa0[j];
            final int b12 = sa12[i];

            final int idx = 
                s.get(b12) < s.get(b0) ||
               (s.get(b12) == s.get(b0) &&
               (b12 % 3 == 1 ? b0 + 1 < r12.length &&
                              (b12 + 1 >= r12.length || r12[b12 + 1] < r12[b0 + 1])
                    : s(s, b12 + 1) < s(s, b0 + 1) ||
                     (s(s, b12 + 1) == s(s, b0 + 1) &&
                      b0 + 2 < r12.length &&
                     (b12 + 2 >= r12.length || r12[b12 + 2] < r12[b0 + 2]))))
                    ? sa12[i++] : sa0[j++];
            if (++k >= 0) {
                sa[k] = idx;
            }
        }
        while (i < sa12.length) {
            sa[++k] = sa12[i++];
        }
        while (j < sa0.length) {
            sa[++k] = sa0[j++];
        }
    }

    private static long s(final LongBuffer s, final int idx) {
        return idx < s.limit() ? s.get(idx) : 0;
    }
    
    private static int s(final IntBuffer s, final int idx) {
        return idx < s.limit() ? s.get(idx) : 0;
    }
}

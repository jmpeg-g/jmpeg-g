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

package es.gencom.mpegg.CABAC.mcoder;

/**
 * <p>
 * Specification of rangeTabLps depending on the values of pStateIdx and qRangeIdx (Table 111).
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class LPSRangeTable {
    
    final static short[][] DATA = new short[][] {
        {128, 128, 128, 123, 116, 111, 105, 100, 95, 90, 85, 81, 77, 73, 69, 66, 
          62,  59,  56,  53,  51,  48,  46,  43, 41, 39, 37, 35, 33, 32, 30, 29, 
          27,  26,  24,  23,  22,  21,  20,  19, 18, 17, 16, 15, 14, 14, 13, 12, 
          12,  11,  11,  10,  10,   9,   9,   8,  8,  7,  7,  7,  6,  6,  6,  2},
        
        {176, 167, 158, 150, 142, 135, 128, 122, 116, 110, 104, 99, 94, 89, 85, 80, 
          76,  72,  69,  65,  62,  59,  56,  53,  50,  48,  45, 43, 41, 39, 37, 35, 
          33,  31,  30,  28,  27,  26,  24,  23,  22,  21,  20, 19, 18, 17, 16, 15,
          14,  14,  13,  12,  12,  11,  11,  10,   9,   9,   9,  8,  8,  7,  7,  2},
        
        {208, 197, 187, 178, 169, 160, 152, 144, 137, 130, 123, 117, 111, 105, 100, 95, 
          90,  86,  81,  77,  73,  69,  66,  63,  59,  56,  54,  51,  48,  46,  43, 41, 
          39,  37,  35,  33,  32,  30,  29,  27,  26,  25,  23,  22,  21,  20,  19, 18, 
          17,  16,  15,  15,  14,  13,  12,  12,  11,  11,  10,  10,   9,   9,   8,  2},
        
        {240, 227, 216, 205, 195, 185, 175, 166, 158, 150, 142, 135, 128, 122, 116, 110,
         104,  99,  94,  89,  85,  80,  76,  72,  69,  65,  62,  59,  56,  53,  50,  48,
          45,  43,  41,  39,  37,  35,  33,  31,  30,  28,  27,  25,  24,  23,  22,  21, 
          20,  19,  18,  17,  16,  15,  14,  14,  13,  12,  12,  11,  11,  10,   9,   2}
    };
}

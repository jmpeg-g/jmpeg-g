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

import es.gencom.mpegg.io.BitReader;
import java.io.IOException;

/**
 * <p>
 * Adapting binary arithmetic decoder implementation.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MDecoder {
    
    private final BitReader reader;
    private final boolean adaptive_mode_flag;
    private final short[][] ctxTable;
    
    private short ivlCurrRange;
    private short ivlOffset;

    public MDecoder(final BitReader reader) throws IOException {
        this(reader, null, false);
    }
    
    /**
     * Initializes M-Decoder
     * 
     * @param reader             encoded bit stream reader
     * @param ctxTable           initial context values.
     * @param adaptive_mode_flag whether the coder works in adapting mode.
     * 
     * @throws IOException 
     */
    public MDecoder(final BitReader reader, 
                    final short[][] ctxTable, 
                    final boolean adaptive_mode_flag) throws IOException {

        this.reader = reader;
        this.ctxTable = ctxTable;
        this.adaptive_mode_flag = adaptive_mode_flag;
        
        ivlCurrRange = 510;
        ivlOffset = (short)reader.readBits(9); // BIG ENDIAN
    }

    public short decode(final int ctxId) throws IOException {
        if (ctxTable == null) {
            return bypass();
        }
//        if (ctxId == 0) { // 276 ??? // && ctxTable == 0 ???
//            return terminate();
//        }
        
        return decode_decision(ctxId);
    }
    
    /*
    * 12.5.2.2 Arithmetic decoding process for a binary decision
    */
    private short decode_decision(final int ctxId) throws IOException {
        short binVal;
        
        short qRangeIdx = (short) ((ivlCurrRange >> 6) & 0x3);
        short ivlLpsRange = LPSRangeTable.DATA[qRangeIdx][ctxTable[0][ctxId]];
        ivlCurrRange -= ivlLpsRange;
        
        if (ivlOffset >= ivlCurrRange) {

            ivlOffset -= ivlCurrRange;
            ivlCurrRange = ivlLpsRange;

            binVal = (short) (1 - ctxTable[1][ctxId]);
            
            if (adaptive_mode_flag) {
                if (ctxTable[0][ctxId] == 0) {
                    // if (pStateIdx == 0) valMps = 1 - valMps;
                    ctxTable[1][ctxId] = (short)(1 - ctxTable[1][ctxId]);
                }

                // pStateIdx = transIdxLps[pStateIdx];
                ctxTable[0][ctxId] = LPSTransTable.DATA[ctxTable[0][ctxId]];
            }
        } else {
            binVal = ctxTable[1][ctxId]; // binVal = valMps
            
            if (adaptive_mode_flag && ctxTable[0][ctxId] < 62) {
                ctxTable[0][ctxId]++;    // pStateIdx = transIdxMps[pStateIdx]
            }
        }
        
        renormalize();

        return binVal;
    }
    
    /*
     * 12.5.2.4 Bypass decoding process for binary decisions
     */
    public short bypass() throws IOException {
        ivlOffset = (short) (ivlOffset << 1 | (short)reader.readBits(1));
        if (ivlOffset >= ivlCurrRange) {
            ivlOffset -= ivlCurrRange;
            return 1;
        }
        return 0;
    }

    /*
    * 12.5.2.5 Decoding process for binary decisions before termination
    */
    private short terminate() throws IOException {
        ivlCurrRange -= 2;
        
        if (ivlOffset >= ivlCurrRange) {
            return 1;
        }
        
        renormalize();
        
        return 0;
    }

    /*
    * 12.5.2.3 Renormalization process in the arithmetic decoding engine.
    */
    private void renormalize() throws IOException {
        while (ivlCurrRange < 256) {
            ivlCurrRange <<= 1;
            ivlOffset = (short) ((ivlOffset << 1) | (short)reader.readBits(1));
        }
    }

}

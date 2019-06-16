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

import es.gencom.mpegg.io.BitWriter;
import java.io.IOException;

/**
 * <p>
 * Adapting binary arithmetic encoder implementation.
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MEncoder {
    
    private final BitWriter writer;
    private final boolean adaptive_mode_flag;
    private final short[][] ctxTable;
    
    private short ivlCurrRange;
    private short ivlLow;
    
    private boolean firstBitFlag;
    private short bitsOutstanding;
    
    public MEncoder(final BitWriter writer, 
                    final short[][] ctxTable, 
                    final boolean adaptive_mode_flag) throws IOException {

        this.writer = writer;
        this.ctxTable = ctxTable;
        this.adaptive_mode_flag = adaptive_mode_flag;
        
        ivlCurrRange = 510;
        firstBitFlag = true;
    }
    
    public void encode(final int ctxId, final short binVal) throws IOException {
        if (ctxTable == null) {
            bypass(binVal);
        } else {
            encode_decision(ctxId, binVal);
        }
    }
            
    private void encode_decision(final int ctxId, final short binVal) throws IOException {
        short qRangeIdx = (short) ((ivlCurrRange >> 6) & 0x3);
        short ivlLpsRange = LPSRangeTable.DATA[qRangeIdx][ctxTable[0][ctxId]];
        ivlCurrRange -= ivlLpsRange;
        
        if (binVal != ctxTable[1][ctxId]) { // binVal != valMps
            ivlLow += ivlCurrRange;
            ivlCurrRange = ivlLpsRange;
            
            if (adaptive_mode_flag) {
                if (ctxTable[0][ctxId] == 0) { // pStateIdx == 0
                    ctxTable[1][ctxId] = (short)(1 - ctxTable[1][ctxId]);
                }

                // pStateIdx = transIdxLps[pStateIdx]
                ctxTable[0][ctxId] = LPSTransTable.DATA[ctxTable[0][ctxId]];
            }
        } else if (adaptive_mode_flag && ctxTable[0][ctxId] < 62) {
            ctxTable[0][ctxId]++;    // pStateIdx = transIdxMps[pStateIdx]
        }
        
        renormalize();
    }
    
    public void bypass(final short binVal) throws IOException {
        ivlLow <<= 1;
        
        if (binVal != 0) {
            ivlLow += ivlCurrRange;
        }
        
        if (ivlLow >= 0x400) {
            put_bit((byte)1);
            ivlLow -= 0x400;
        } else if (ivlLow < 0x200) {
            put_bit((byte)0);
        } else {
            ivlLow -= 0x200;
            bitsOutstanding++;
        }
        
        renormalize();
    }
    
    public void terminate(final short binVal) throws IOException {
        ivlCurrRange -= 2;
        
        if (binVal != 0) {
            ivlLow += ivlCurrRange;
            flush();
        } else {
            renormalize();
        }

        writer.flush();
    }
    
    private void flush() throws IOException {
        ivlCurrRange = 2;
        renormalize();
        
        put_bit((ivlLow >> 9) & 0x1);
        writer.writeBits((ivlLow >> 7) & 3 | 1, 2);
    }

    private void renormalize() throws IOException {
        while (ivlCurrRange < 0x100) {
            if (ivlLow < 0x100) {
                put_bit(0);
            } else if (ivlLow < 0x200) {
                ivlLow -= 0x100;
                bitsOutstanding++;
            } else {
                ivlLow -= 0x200;
                put_bit(1);
            }
            ivlCurrRange <<= 1;
            ivlLow <<= 1;
        }
    }
    
    private void put_bit(final int b) throws IOException {
        if (!firstBitFlag) {
           writer.writeBits(b, 1); 
        } else {
            firstBitFlag = false;
        }
        
        while(bitsOutstanding > 0) {
            writer.writeBits(1 - b, 1);
            bitsOutstanding--;
        }
    }
}

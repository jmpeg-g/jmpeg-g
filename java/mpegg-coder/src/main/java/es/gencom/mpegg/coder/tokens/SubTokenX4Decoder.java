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

package es.gencom.mpegg.coder.tokens;

import es.gencom.mpegg.CABAC.configuration.CABAC_TokentypeDecoderConfiguration;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.TokentypeDecoderConfiguration;
import es.gencom.mpegg.io.MPEGReader;

import java.io.IOException;

public class SubTokenX4Decoder implements SubTokenSequenceDecoder{
    private final short[][] values;
    private int currentIndex = 0;
    private int currentSubsequence = 0;

    public SubTokenX4Decoder(
            final MPEGReader reader,
            final DESCRIPTOR_ID descriptor_id,
            final long numOutputSymbols,
            final TokentypeDecoderConfiguration decoderConfiguration) throws IOException {

        if(numOutputSymbols % 4 != 0) {
            throw new IllegalArgumentException();
        }

        if(! (decoderConfiguration instanceof CABAC_TokentypeDecoderConfiguration)){
            throw new IllegalArgumentException();
        }
        CABAC_TokentypeDecoderConfiguration cabacTokentypeDecoderConfiguration =
                (CABAC_TokentypeDecoderConfiguration)decoderConfiguration;

        values = new short[4][Math.toIntExact(numOutputSymbols)/4];

        for(int i=0; i<4; i++){
            byte type = (byte) reader.readBits(4);
            SubTokenDecoderType decoderType = SubTokenDecoderType.getTokenType(type);

            SubTokenSequenceDecoder sequenceDecoder;

            switch (decoderType){
                case CAT:
                    sequenceDecoder = new SubTokenCATDecoder(reader, numOutputSymbols/4);
                    break;
                case RLE:
                    sequenceDecoder = new SubTokenRLEDecoder(
                            cabacTokentypeDecoderConfiguration.rle_guard_tokentype,
                            reader,
                            numOutputSymbols/4);
                    break;
                case CABAC_METHOD_0:
                    sequenceDecoder = new SubTokenCABACMethod0Decoder(
                            reader,
                            descriptor_id,
                            cabacTokentypeDecoderConfiguration,
                            Math.toIntExact(numOutputSymbols / 4));
                    break;
                case CABAC_METHOD_1:
                    sequenceDecoder = new SubTokenCABACMethod1Decoder(
                            reader,
                            descriptor_id,
                            cabacTokentypeDecoderConfiguration,
                            Math.toIntExact(numOutputSymbols / 4)
                    );
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            for(int j=0; j<numOutputSymbols/4; j++){
                values[i][j] = sequenceDecoder.getSubTokenUnsignedByte();
            }
        }
    }


    @Override
    public boolean hasNext() {
        if(currentSubsequence < 4){
            return true;
        }
        return (currentIndex + 1) < values.length;
    }

    @Override
    public short getSubTokenUnsignedByte() throws IOException {
        if(currentSubsequence == 4){
            currentSubsequence = 0;
            currentIndex++;
        }
        short result = values[currentIndex][currentSubsequence];
        currentSubsequence++;
        return result;
    }
}

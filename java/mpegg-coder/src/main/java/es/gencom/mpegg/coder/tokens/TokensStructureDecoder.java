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

import java.util.Arrays;

public class TokensStructureDecoder {
    private final short[][][] values;
    private final int[][] currentIndex;
    private final RingBuffer<TokensList> tokensListRingBuffer;

    public TokensStructureDecoder(short[][][] values) {
        this.values = values;
        currentIndex = new int[values.length][16];
        tokensListRingBuffer = new RingBuffer<>(65536);
    }

    public boolean hasNext() {
        return currentIndex[0][0] < values[0][0].length;
    }

    private short getValue(int token, int subtoken){
        short value = values[token][subtoken][currentIndex[token][subtoken]];
        currentIndex[token][subtoken]++;
        return value;
    }

    private long getValueInt(int token, int subtoken){
        long value = 0;
        for(int i = 0; i < 4; i++) {
            value = value << 8 | (getValue(token, subtoken));
        }
        return value;
    }

    public String getString() {
        StringBuilder stringBuilder = new StringBuilder();

        short token0 = getValue(0,0);

        if(currentIndex[0][0] == 0 && token0 != TokenType.DiffToken.id){
            throw new IllegalArgumentException("The first of all token must be a Diff to 0");
        }

        TokensList result;
        if(token0 == TokenType.DupToken.id){
            TokensList tokensList = tokensListRingBuffer.getValue((int)getValueInt(0, TokenType.DupToken.id));
            result = tokensList.copy();
        }else if(token0 != TokenType.DiffToken.id){
            throw new IllegalArgumentException("First token must be of type Dup or Diff.");
        } else {
            TokensList tokensList;
            if(
                    tokensListRingBuffer.getSize()==0
            ){
                long distance = getValueInt(0,TokenType.DiffToken.id);
                if(distance == 0){
                    tokensList = null;
                } else {
                    throw new IllegalArgumentException("First token must be of type Diff with distance 0");
                }
            }else {
                tokensList = tokensListRingBuffer.getValue((int)getValueInt(0,TokenType.DiffToken.id));

            }

            Token[] tokens = new Token[values.length];

            int current_token_i = 0;
            short tokenType = getValue(current_token_i+1, TokenType.TypeToken.id);
            while(
                 tokenType != TokenType.EndToken.id
            ){
                if(
                    tokenType == TokenType.MatchToken.id
                ){
                    tokens[current_token_i] = tokensList.getTokens()[current_token_i].copy();
                }else if(
                    tokenType == TokenType.AlphaToken.id
                ){
                    tokens[current_token_i] = decodeStringToken(current_token_i+1);
                }else if(
                    tokenType  == TokenType.CharToken.id
                ){
                    tokens[current_token_i] = decodeCharToken(current_token_i+1);
                }else if(
                    tokenType  == TokenType.DigitsToken.id
                ){
                    tokens[current_token_i] = decodeDigitsToken(current_token_i+1);
                }else if(
                    tokenType  == TokenType.Digits0Token.id
                ){
                    tokens[current_token_i] = decodeDigitsPaddedToken(current_token_i+1);
                }else if(
                    tokenType == TokenType.DeltaDigitsToken.id
                ){
                    tokens[current_token_i] = decodeDeltaDigitsToken(
                            current_token_i+1,
                            tokensList.getTokens()[current_token_i].copy()
                    );
                }else if(
                    tokenType == TokenType.DeltaDigitsPaddedToken.id
                ){
                    tokens[current_token_i] = decodeDeltaDigitsPaddedToken(
                            current_token_i+1,
                            tokensList.getTokens()[current_token_i].copy()
                    );
                }

                current_token_i++;
                tokenType = getValue(current_token_i+1, TokenType.TypeToken.id);
            }
            result = new TokensList(Arrays.copyOf(tokens, current_token_i));

        }

        tokensListRingBuffer.addValue(result);
        for(int token_i=0; token_i < result.getNumberTokens(); token_i++){
            stringBuilder.append(result.getTokens()[token_i].toString());
        }
        return stringBuilder.toString();
    }

    private Token decodeStringToken(int token_i){
        byte[] string = new byte[126];
        int currentLength = 0;

        short currentValue = getValue(token_i, TokenType.AlphaToken.id);
        while(currentValue != 0){
            if(currentLength == string.length){
                string = Arrays.copyOf(string, string.length*2);
            }
            string[currentLength] = (byte) currentValue;
            currentLength++;

            currentValue = getValue(token_i, TokenType.AlphaToken.id);
        }
        string = Arrays.copyOf(string, currentLength);
        return Token.createStringToken(new String(string));
    }

    private Token decodeCharToken(int token_i){
        short charValue = getValue(token_i, TokenType.CharToken.id);
        return Token.createCharToken((char) charValue);
    }

    private Token decodeDigitsToken(int token_i){
        long value = 0;
        for(int i=0; i<4; i++) {
            value = value << 8 | (getValue(token_i, TokenType.DigitsToken.id));
        }
        return Token.createDigitsToken(value);
    }

    private Token decodeDigitsPaddedToken(int token_i){
        long value = 0;
        for(int i=0; i<4; i++) {
            value = value << 8 | (getValue(token_i, TokenType.Digits0Token.id));
        }
        short length = getValue(token_i, TokenType.DZLENToken.id);

        return Token.createDigitsTokenPadded(value, length);
    }

    private Token decodeDeltaDigitsToken(int token_i, Token baseToken) {
        int delta = getValue(token_i, TokenType.DeltaDigitsToken.id);

        return Token.createDigitsToken(delta + baseToken.getNumericValue());
    }

    private Token decodeDeltaDigitsPaddedToken(int token_i, Token baseToken) {
        if(!baseToken.hasFixedLength()){
            throw new IllegalArgumentException();
        }
        short delta = getValue(token_i, TokenType.DeltaDigitsToken.id);
        short length = baseToken.getLength();

        return Token.createDigitsTokenPadded(delta + baseToken.getNumericValue(), length);
    }
}

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

import static es.gencom.mpegg.coder.tokens.TokenType.*;

public abstract class AbstractReadIdentifierEncoder {
    private final int RING_SIZE;
    private short[][][] values;
    private int[][] numberValues;
    private final RingBuffer<TokensList> tokensListRingBuffer;
    private final ReadIdentifierToReadId readIdentifierToReadId;
    private long encoded;
    private long numberEntries = 0;

    /**
     * The function returns a metric of the cost of encoding one token list using the other as reference
     * @param tokensList The token list to be used as base for the encoding
     * @param thatTokensList The token list to encode
     * @return Returns the cost in bytes of encoding the goal token list based on the reference token list, disregarding
     * the effect of compression
     */
    protected long distance(TokensList tokensList, TokensList thatTokensList){
        Token[] tokensFromRing = tokensList.getTokens();
        Token[] tokensToEncode = thatTokensList.getTokens();

        int minNumTokens = Integer.min(tokensFromRing.length, tokensToEncode.length);
        long currentCost = 0;
        for(int i=0; i<minNumTokens; i++){
            currentCost += tokensFromRing[i].distance(tokensToEncode[i]);
        }

        if(tokensFromRing.length == tokensToEncode.length){
            return currentCost;
        }

        Token[] longestTokenList;
        if(tokensFromRing.length < tokensToEncode.length){
            longestTokenList = tokensToEncode;
        }else {
            longestTokenList = tokensFromRing;
        }

        for(int i=minNumTokens; i<longestTokenList.length; i++){
            currentCost += longestTokenList[i].distance(null);
        }

        return currentCost;
    }

    /**
     * The number of subsequences is unknown at the beginning. This methods discovers how many subsequences are
     * currently required.
     * @return The number of subsequences required to encode
     */
    int getNumberSubSequences() {
        int result = 0;
        for(int token_i=0; token_i< numberValues.length; token_i++){
            boolean allEmpty = true;
            boolean hasTypeData = false;
            for(int subsequence_i=0; subsequence_i < numberValues[token_i].length; subsequence_i++){
                if(subsequence_i==0 && numberValues[token_i][subsequence_i] != 0){
                    hasTypeData = true;
                }
                if(numberValues[token_i][subsequence_i] != 0){
                    allEmpty = false;
                    result++;
                }
            }
            if(allEmpty){
                return result;
            } else {
                if(!hasTypeData){
                    throw  new IllegalArgumentException();
                }
            }
        }
        return result;
    }

    /**
     * This method releases any pre-allocated and still unused memory
     * @return the decoded values ready to be reassembled in decoded strings.
     */
    short[][][] resizeAndGetValues() {
        int numberTokens = numberValues.length;
        for(int token_i=0; token_i< numberValues.length; token_i++){
            boolean allEmpty = true;
            for(int subsequence_i=0; subsequence_i < numberValues[token_i].length; subsequence_i++){
                if(numberValues[token_i][subsequence_i] != 0){
                    allEmpty = false;
                    break;
                }
            }
            if(allEmpty){
                numberTokens = token_i;
                break;
            }
        }

        values = Arrays.copyOf(values, numberTokens);
        numberValues = Arrays.copyOf(numberValues, numberTokens);

        for(int token_i=0; token_i< numberValues.length; token_i++) {
            for (int subsequence_i = 0; subsequence_i < numberValues[token_i].length; subsequence_i++) {
                values[token_i][subsequence_i] = Arrays.copyOf(
                        values[token_i][subsequence_i], numberValues[token_i][subsequence_i]);
            }
        }
        return values;
    }

    /**
     * The encoder can be called either in non-greedy mode, or in greedy-mode. In non greedy, it will search for each
     * identifier which prior identifier best matches among the last 32768, while in greedy mode it always encode
     * comparing with the latest identifier.
     * @param greedy Selector of greedy behaviour (true means gready)
     */
    AbstractReadIdentifierEncoder(boolean greedy) {
        if(!greedy) {
            RING_SIZE = 32768;
        }else {
            RING_SIZE = 1;
        }
        tokensListRingBuffer = new RingBuffer<>(RING_SIZE);
        readIdentifierToReadId = new ReadIdentifierToReadId();
        values = new short[32][16][2048];
        numberValues = new int[32][16];
    }

    private void addValue(short value, int token_i, int num_tokentype_sequence_i){
        if(token_i > values.length){
            int oldSize = values.length;
            values = Arrays.copyOf(values, Integer.max(values.length*2, token_i+1));
            for(int token_entry = oldSize; token_entry < values.length; token_entry++){
                values[token_entry] = new short[16][2048];
            }
        }
        if(numberValues[token_i][num_tokentype_sequence_i] == values[token_i][num_tokentype_sequence_i].length){
            values[token_i][num_tokentype_sequence_i] = Arrays.copyOf(
                values[token_i][num_tokentype_sequence_i],
                numberValues[token_i][num_tokentype_sequence_i]*2
            );
        }
        values[token_i][num_tokentype_sequence_i][numberValues[token_i][num_tokentype_sequence_i]] = value;
        numberValues[token_i][num_tokentype_sequence_i]++;
    }

    private void addValueInt(int value, int token_i, int num_tokentype_sequence_i){
        for(int byte_i=0; byte_i<4; byte_i++){
            byte byteValue = (byte)((value & (0xff <<(3 -byte_i)*8)) >> (3-byte_i)*8);
            addValue(byteValue, token_i, num_tokentype_sequence_i);
        }
    }

    /**
     * Searches among all prior token lists which one minimizes the number of bytes to encode.
     * @param thatTokensList The token list to be encoded
     * @return A pointer to the prior token list minimizing the number of bytes to encode for the current one.
     */
    private SearchDiffResult findBestMatch(TokensList thatTokensList) {
        String readIdentifier = thatTokensList.toString();
        Long possibleEqualId = readIdentifierToReadId.getId(readIdentifier);
        if(possibleEqualId != null){
            long equalId = possibleEqualId;
            if(encoded - equalId < RING_SIZE){
                return new SearchDiffResult((int)(encoded - equalId - 1), 0);
            }
        }

        int bestIndex = 0;
        long currentMin = Long.MAX_VALUE;

        for(int index = 0; index < RING_SIZE && index < tokensListRingBuffer.getSize(); index++){
            TokensList tokensList = tokensListRingBuffer.getValue(index);

            if(tokensList.getNumberTokens() != thatTokensList.getNumberTokens()){
                continue;
            }

            long currentCost = distance(tokensList, thatTokensList);

            if(currentCost < currentMin){
                bestIndex = index;
                currentMin = currentCost;
            }

            if(currentCost == tokensList.getNumberTokens()+4){
                break;
            }

        }
        return new SearchDiffResult(bestIndex, currentMin);
    }

    void encode(TokensList tokensList) {
        if(tokensListRingBuffer.getSize()==0){
            encode_without_ref(tokensList);
        }else{
            SearchDiffResult bestMatch = findBestMatch(tokensList);
            if (bestMatch.getCost() == 0){
                addValue(DupToken.id, 0, 0);
                addValueInt(bestMatch.getBestIndex(), 0, DupToken.id);
            }else{
                TokensList bestMatchTokens = tokensListRingBuffer.getValue(bestMatch.getBestIndex());
                addValue(DiffToken.id, 0, 0);
                addValueInt(bestMatch.getBestIndex(), 0, DiffToken.id);
                encode_diff(bestMatchTokens, tokensList);
                addValue(EndToken.id, tokensList.getNumberTokens()+1, 0);
            }
        }
        tokensListRingBuffer.addValue(tokensList);
        readIdentifierToReadId.addReadIdentifier(tokensList.toString(), encoded);
        encoded++;
    }

    /**
     * Encode a token list as a difference to a prior token list
     * @param bestMatchTokens tokens list to be used as reference
     * @param tokensList tokens list to be encoded.
     */
    private void encode_diff(TokensList bestMatchTokens, TokensList tokensList){
        for(int i=0; i<tokensList.getNumberTokens(); i++){
            if(bestMatchTokens.getTokens()[i].equals(tokensList.getTokens()[i])){
                writeToken(i+1, Token.createMatchToken());
            }else{
                if(bestMatchTokens.getTokens()[i].isNumber() && tokensList.getTokens()[i].isNumber()){
                    Token tokenToWrite = tokensList.getTokens()[i];
                    if(bestMatchTokens.getTokens()[i].getNumericValue() < tokensList.getTokens()[i].getNumericValue()){
                        if(
                                tokensList.getTokens()[i].getNumericValue()
                                        - bestMatchTokens.getTokens()[i].getNumericValue() < 255
                        ){
                            if(tokensList.getTokens()[i].hasFixedLength()){
                                tokenToWrite = Token.createDeltaDigitsPadded(
                                        tokensList.getTokens()[i].getNumericValue(),
                                        bestMatchTokens.getTokens()[i],
                                        tokensList.getTokens()[i].getLength()
                                );
                            }else{
                                tokenToWrite= Token.createDeltaDigits(
                                        tokensList.getTokens()[i].getNumericValue(),
                                        bestMatchTokens.getTokens()[i]
                                );
                            }
                        }
                    }
                    writeToken(i+1, tokenToWrite);
                }else {
                    writeToken(i+1, tokensList.getTokens()[i]);
                }
            }
        }
    }

    /**
     * Method to be used for the first tokens list
     * @param tokensList tokens list to be encoded without referencing any other list.
     */
    private void encode_without_ref(
            TokensList tokensList
    ) {
        addValue(DiffToken.id, 0, 0);
        addValueInt(0, 0, DiffToken.id);
        for(int token_i=0; token_i < tokensList.getTokens().length; token_i++){
            writeToken(token_i+1, tokensList.getTokens()[token_i]);
        }
        addValue(EndToken.id, tokensList.getNumberTokens()+1, 0);
    }

    private void writeToken(int token_i, Token token){
        addValue(token.getTokenType().id, token_i, TypeToken.id);
        if(token.getTokenType() == MatchToken){
            return;
        }else if(token.getTokenType() == DigitsToken){
            addValueInt((int) token.getNumericValue(), token_i, DigitsToken.id);
        }else if(token.getTokenType() == Digits0Token){
            addValueInt((int) token.getNumericValue(), token_i, Digits0Token.id);
            addValue(token.getLength(), token_i, DZLENToken.id);
        }else if(token.getTokenType() == AlphaToken){
            byte[] chars = token.getCharValues();
            for(int i=0; i<chars.length; i++){
                addValue(chars[i], token_i, AlphaToken.id);
            }
            addValue((byte)0, token_i, AlphaToken.id);
        }else if(token.getTokenType() == CharToken){
            addValue(token.getCharValues()[0], token_i, CharToken.id);
        }else if(token.getTokenType() == DeltaDigitsToken){
            addValue(token.getDelta(), token_i, DeltaDigitsToken.id);
        }else if(token.getTokenType() == DeltaDigitsPaddedToken){
            addValue(token.getDelta(), token_i, DeltaDigitsToken.id);
        }else{
            throw new InternalError();
        }
    }

    public void encode(String readIdentifier)  {
        numberEntries++;
        encodeSpecific(readIdentifier);
    }

    protected abstract void encodeSpecific(String readIdentifier);

    short[][][] getValues() {
        return values;
    }

    long getNumberEntries() {
        return numberEntries;
    }
}

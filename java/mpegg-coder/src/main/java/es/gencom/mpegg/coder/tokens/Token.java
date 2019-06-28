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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static es.gencom.mpegg.coder.tokens.TokenType.*;

public class Token {
    private final TokenType tokenType;
    private final long dupDifReference;
    private final long numericValue;
    private final short length;
    private final long delta;
    private final byte[] charValues;

    private Token(
            TokenType tokenType,
            long dupDifReference,
            long numericValue,
            short length,
            long delta,
            byte[] charValues
    ){
        this.tokenType = tokenType;
        this.dupDifReference = dupDifReference;
        this.numericValue = numericValue;
        this.length = length;
        this.delta = delta;
        this.charValues = charValues;
    }

    public static Token createMatchToken() {
        return new Token(MatchToken, 0, 0, (byte)0, 0, null);
    }

    public static Token createEndToken() {
        return new Token(EndToken, 0, 0, (byte)0, 0, null);
    }

    public boolean isNumber(){
        if(tokenType == DiffToken || tokenType == DupToken || tokenType == MatchToken || tokenType == EndToken){
            throw new IllegalArgumentException();
        }
        return tokenType==DigitsToken
                || tokenType == Digits0Token
                || tokenType == DeltaDigitsToken
                || tokenType == DeltaDigitsPaddedToken;
    }

    public long getNumericValue() {
        if(!isNumber()){
            throw new IllegalArgumentException("Can only get numericValue for a number token.");
        }
        return numericValue;
    }

    public static Token createCharToken(char value){
        return new Token(CharToken, 0, 0, (byte) 0, 0, new byte[]{(byte) value});
    }

    public static Token createDigitsToken(long value){
        return new Token(DigitsToken, 0, value, (byte) 0, 0, null);
    }

    public static Token createDigitsTokenPadded(long value, short length){
        return new Token(Digits0Token, 0, value, length, 0, new byte[]{});
    }

    public static Token createStringToken(String value){
        return new Token(AlphaToken, 0, 0, (byte) 0, 0, value.getBytes(StandardCharsets.US_ASCII));
    }

    public static Token createStringToken(byte[] value){
        return new Token(AlphaToken, 0, 0, (byte) 0, 0, value);
    }

    public static Token createDeltaDigits(long value, Token baseToken){
        checkConformityForDeltaDigitsToken(value, baseToken);

        long delta = value - baseToken.getNumericValue();
        return new Token(DeltaDigitsToken, 0, value, (byte) 0, delta, null);
    }

    public static Token createDeltaDigitsPadded(long value, Token baseToken, short length){
        checkConformityForDeltaDigitsToken(value, baseToken);

        long delta = value - baseToken.getNumericValue();

        return new Token(
                DeltaDigitsPaddedToken,
                0, value,
                length,
                delta,
                null
        );
    }

    public static Token createDiffToken(long diffReference){
        return new Token(DiffToken, diffReference, 0,(byte)0,0,null);
    }

    public static Token createDupToken(long dupReference){
        return new Token(DupToken, dupReference, 0,(byte)0,0,null);
    }

    public long getDupDifReference() {
        if(!(tokenType == DupToken || tokenType == DiffToken)){
            throw new IllegalArgumentException();
        }
        return dupDifReference;
    }

    private static void checkConformityForDeltaDigitsToken(long value, Token baseToken) {
        if(!baseToken.isNumber()){
            throw new IllegalArgumentException();
        }
        if (value < baseToken.getNumericValue()){
            throw new IllegalArgumentException();
        }
        if(value - baseToken.getNumericValue() > Short.MAX_VALUE){
            throw new IllegalArgumentException();
        }
    }



    @Override
    public boolean equals(Object o) {
        if(tokenType == DupToken || tokenType == DiffToken || tokenType == MatchToken || tokenType == EndToken){
            throw new IllegalArgumentException();
        }
        if (this == o) return true;
        if (!(o instanceof Token)) return false;
        Token token = (Token) o;

        if(
                token.tokenType == DupToken
                        || token.tokenType == DiffToken
                        || token.tokenType == MatchToken
                        || token.tokenType == EndToken
        ){
            throw new IllegalArgumentException();
        }
        if(isNumber() != token.isNumber()){
            return false;
        }
        if(isNumber()){
            return numericValue == token.numericValue && length == token.length;
        }else{
            return Arrays.equals(charValues, token.charValues);
        }
    }

    public long distance(Token token) {

        if(token == null || token.tokenType == EndToken || isNumber() != token.isNumber()){
            if(isNumber()){
                if(length != 0){
                    return 1+4+2;
                }else{
                    return 1+4;
                }
            }else{
                if(tokenType == CharToken){
                    return 2;
                }else{
                    return 1+charValues.length+1;
                }
            }
        }


        if(
                tokenType == DiffToken
                        || tokenType == DupToken
                        || token.tokenType == DiffToken
                        || token.tokenType == DupToken
        ){
            throw new IllegalArgumentException();
        }

        if(tokenType == EndToken){
            return 1;
        }



        if(token.isNumber()){
            byte bytesForType = 1;
            if(token.getNumericValue() == getNumericValue() && token.length == length){
                return bytesForType;
            }
            byte bytesToEncodeValue;
            if(numericValue >= token.getNumericValue()){
                if(numericValue - token.numericValue < Short.MAX_VALUE){
                    bytesToEncodeValue = 2;
                }else{
                    bytesToEncodeValue = 4;
                }
            }else{
                bytesToEncodeValue = 4;
            }

            if(length == 0) {
                return bytesForType + bytesToEncodeValue;
            }else{
                byte bytesToEncodeLength = 2;
                return bytesForType + bytesToEncodeValue + bytesToEncodeLength;
            }
        }else{
            if(Arrays.equals(charValues, token.charValues)){
                return 1;
            }else{
                if (tokenType == CharToken){
                    return 2;
                }else {
                    return 1+charValues.length+1;
                }
            }
        }
    }

    public boolean hasFixedLength() {
        if (!isNumber()){
            throw new IllegalArgumentException("Can only query for fixed length for a number token");
        }
        return length != 0;
    }

    public short getLength(){
        return length;
    }

    public Token copy() {
        return new Token(
                tokenType,
                dupDifReference,
                numericValue,
                length,
                delta,
                charValues != null ? Arrays.copyOf(charValues, charValues.length) : null
        );
    }

    @Override
    public String toString() {
        if(!isNumber()){
            return new String(charValues);
        }else{
            if(length != 0){
                return String.format("%0"+length+"d",numericValue);
            }else{
                return String.format("%d", numericValue);
            }
        }
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public byte getDelta() {
        if(tokenType != DeltaDigitsToken && tokenType != DeltaDigitsPaddedToken){
            throw new IllegalArgumentException("Can only query for the delta for delta number tokens");
        }
        return (byte) delta;
    }

    public byte[] getCharValues() {
        return charValues;
    }

}

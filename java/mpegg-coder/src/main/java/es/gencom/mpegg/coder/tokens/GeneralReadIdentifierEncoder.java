/*
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

public class GeneralReadIdentifierEncoder extends AbstractReadIdentifierEncoder {
    private final static byte[] DELIMITERS = new byte[]{':',',','.',';','/','#','_'};

    public GeneralReadIdentifierEncoder(){
        this(false);
    }

    private GeneralReadIdentifierEncoder(boolean greedy) {
        super(greedy);
    }

    @Override
    protected void encodeSpecific(String readIdentifier) {
        byte[] readIdentifierBytes = readIdentifier.getBytes();
        int[] marks = breakDownIdentifier(readIdentifierBytes);

        Token[] tokens = new Token[32];
        int numTokens = 0;

        for(int i=0; i < marks.length; i++){
            int start = marks[i];
            int end;
            if(i == marks.length-1){
                end = readIdentifierBytes.length;
            } else {
                end = marks[i+1];
            }
            if(isDelimiter(readIdentifierBytes[start])){
                tokens[numTokens] = Token.createCharToken((char) readIdentifierBytes[start]);
            } else {
                if(isDigit(readIdentifierBytes[start])){
                    long value = 0;
                    boolean isPadded = readIdentifierBytes[start] == '0';
                    for(int pos = start; pos < end; pos++){
                        value = value*10 + readIdentifierBytes[pos]-'0';
                    }
                    if(isPadded) {
                        tokens[numTokens] = Token.createDigitsTokenPadded(value, (short) (end-start));
                    }else{
                        tokens[numTokens] = Token.createDigitsToken(value);
                    }
                }else{
                    tokens[numTokens] = Token.createStringToken(Arrays.copyOfRange(readIdentifierBytes, start, end));
                }
            }
            numTokens++;
            if(numTokens == tokens.length){
                tokens = Arrays.copyOf(tokens, tokens.length*2);
            }
        }

        TokensList tokensList = new TokensList(Arrays.copyOf(tokens, numTokens));

        encode(tokensList);
    }

    /**
     * Test whether a character has been marked as a delimiter
     * @param value character which has to be checked
     * @return whether the character is a delimiter
     */
    private static boolean isDelimiter(byte value){
        boolean isDelimiter = false;
        for(int delimiter_i=0; delimiter_i < DELIMITERS.length; delimiter_i++){
            if(value == DELIMITERS[delimiter_i]){
                isDelimiter = true;
                break;
            }
        }
        return isDelimiter;
    }

    /**
     * Test whether a character is a digit
     * @param value character which has to be checked
     * @return whether the character is a digit
     */
    private static boolean isDigit(byte value){
        return value >= '0' && value <= '9';
    }

    /**
     * Marks the beginning of each division within the submitted string.
     * @param bytesInput the string to break down in subdivision
     * @return an array of positions within the string, each position correspond to a new division in the string.
     * Subdivisions are delimited by the delimiters.
     */
    static int[] breakDownIdentifier(byte[] bytesInput){
        boolean isNumber = false;
        boolean multiPositionToken = false;

        int[] marks = new int[32];
        int numMarks = 0;

        for(int i=0; i<bytesInput.length; i++){
            if(isDelimiter(bytesInput[i])){
                multiPositionToken = false;
                marks[numMarks] = i;
                numMarks++;
            } else {
                if(
                    isDigit(bytesInput[i])
                ){
                    if(!multiPositionToken){
                        marks[numMarks] = i;
                        numMarks++;
                        isNumber = true;
                        multiPositionToken = true;
                    } else if(!isNumber){
                        marks[numMarks] = i;
                        numMarks++;
                        isNumber = true;
                        multiPositionToken = true;
                    }
                } else {
                    if(!multiPositionToken){
                        marks[numMarks] = i;
                        numMarks++;
                        isNumber = false;
                        multiPositionToken = true;
                    }else if(isNumber){
                        marks[numMarks] = i;
                        numMarks++;
                        isNumber = false;
                        multiPositionToken = true;
                    }
                }
            }

            if(marks.length == numMarks){
                marks = Arrays.copyOf(marks, marks.length*2);
            }
        }
        return Arrays.copyOf(marks, numMarks);
    }
}

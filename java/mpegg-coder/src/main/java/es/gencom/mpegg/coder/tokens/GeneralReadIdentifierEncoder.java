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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralReadIdentifierEncoder extends AbstractReadIdentifierEncoder {
    final static Pattern PATTERN = Pattern.compile("([a-zA-Z0-9_]*)([:,.;/]{0,1})");

    public GeneralReadIdentifierEncoder(){
        this(false);
    }

    GeneralReadIdentifierEncoder(boolean greedy) {
        super(greedy);
    }

    @Override
    protected void encodeSpecific(String readIdentifier) {
        String[] groups = breakDownIdentifier(readIdentifier);

        Token[] tokens = new Token[32];
        int numTokens = 0;
        for(String group : groups){
            try{
                int value = Integer.parseInt(group);
                if(group.startsWith("0")){
                    tokens[numTokens] = Token.createDigitsTokenPadded(value, (short)group.length());
                }else {
                    tokens[numTokens] = Token.createDigitsToken(Integer.parseInt(group));
                }
            }catch (NumberFormatException e){
                if(group.length() == 1){
                    tokens[numTokens] = Token.createCharToken(group.charAt(0));
                }else{
                    tokens[numTokens] = Token.createStringToken(group);
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

    static String[] breakDownIdentifier(String input){
        Matcher matcher = PATTERN.matcher(input);
        int results = 0;
        String[] output = new String[32];
        while(matcher.find()){
            String group1 = matcher.group(1);
            String group2 = matcher.group(2);
            if(matcher.group(1).length() != 0) {
                output[results] = group1;
                results++;
                if(output.length == results){
                    output = Arrays.copyOf(output, output.length*2);
                }
            }
            if(matcher.group(2).length() != 0) {
                output[results] = group2;
                results++;
                if(output.length == results){
                    output = Arrays.copyOf(output, output.length*2);
                }
            }
        }
        return Arrays.copyOf(output, results);
    }
}

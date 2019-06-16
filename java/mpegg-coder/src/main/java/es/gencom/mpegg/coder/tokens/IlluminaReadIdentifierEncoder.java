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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IlluminaReadIdentifierEncoder extends AbstractReadIdentifierEncoder {

    private final Pattern pattern = Pattern.compile("([a-zA-Z0-9_]+):([0-9]+):([0-9]+):([0-9]+):([0-9]+)#([0-9]+)");

    public IlluminaReadIdentifierEncoder(){
        this(false);
    }

    @Override
    protected long distance(TokensList tokensList, TokensList thatTokensList, int index) {
        Token[] tokensFromRing = tokensList.getTokens();
        Token[] tokensToEncode = thatTokensList.getTokens();

        long currentCost = 5;


        currentCost += tokensFromRing[0].distance(tokensToEncode[0]);
        currentCost += tokensFromRing[2].distance(tokensToEncode[2]);
        currentCost += tokensFromRing[4].distance(tokensToEncode[4]);
        currentCost += tokensFromRing[6].distance(tokensToEncode[6]);
        currentCost += tokensFromRing[8].distance(tokensToEncode[8]);
        currentCost += tokensFromRing[10].distance(tokensToEncode[10]);
        return currentCost;
    }

    public IlluminaReadIdentifierEncoder(boolean gready){
        super(gready);
    }

    @Override
    protected void encodeSpecific(String readIdentifier) {
        Matcher matcher = pattern.matcher(readIdentifier);
        if (!matcher.matches()){
            System.out.println(readIdentifier);
            throw new IllegalArgumentException();
        }

        String elem0 = matcher.group(1);
        int elem1 = Integer.parseInt(matcher.group(2));
        int elem2 = Integer.parseInt(matcher.group(3));
        int elem3 = Integer.parseInt(matcher.group(4));
        int elem4 = Integer.parseInt(matcher.group(5));
        int elem5 = Integer.parseInt(matcher.group(6));

        Token[] tokens = new Token[11];
        tokens[0] = Token.createStringToken(elem0);
        tokens[1] = Token.createCharToken(':');
        tokens[2] = Token.createDigitsToken(elem1);
        tokens[3] = Token.createCharToken(':');
        tokens[4] = Token.createDigitsToken(elem2);
        tokens[5] = Token.createCharToken(':');
        tokens[6] = Token.createDigitsToken(elem3);
        tokens[7] = Token.createCharToken(':');
        tokens[8] = Token.createDigitsToken(elem4);
        tokens[9] = Token.createCharToken('#');
        tokens[10] = Token.createDigitsToken(elem5);

        TokensList tokensList = new TokensList(tokens);

        encode(tokensList);
    }
}

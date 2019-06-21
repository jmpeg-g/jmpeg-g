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

public class TokensList {
    private final Token[] tokens;

    public TokensList(Token[] tokens) {
        this.tokens = tokens;
    }

    public Token[] getTokens() {
        return tokens;
    }

    public int getNumberTokens(){
        return tokens.length;
    }

    public TokensList copy() {
        Token[] newTokens = new Token[tokens.length];

        for(int i=0; i<tokens.length; i++){
            newTokens[i] = tokens[i].copy();
        }
        return new TokensList(newTokens);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0; i<tokens.length; i++){
            stringBuilder.append(tokens[i].toString());
        }
        return stringBuilder.toString();
    }
}

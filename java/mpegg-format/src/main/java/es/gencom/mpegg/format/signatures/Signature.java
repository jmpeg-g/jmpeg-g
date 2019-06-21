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

package es.gencom.mpegg.format.signatures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class Signature {

    private List<SignatureSymbol> symbols;
    final private byte bitsPerSymbol;

    public Signature(byte bitsPerSymbol) {
        this.bitsPerSymbol = bitsPerSymbol;
        symbols = new ArrayList<>();
    }

    /**
     * <p>
     * Constructor for Signature with known symbols.
     * </p>
     * 
     * Creates a symbols with fixed size equal to symbols length.
     * 
     * @param symbols
     * @param bitsPerSymbol
     */
    public Signature(
            final List<SignatureSymbol> symbols, 
            final byte bitsPerSymbol) throws IllegalArgumentException {

        for(SignatureSymbol symbol : symbols){
            if(symbol.getNumberBits() != bitsPerSymbol){
                throw new IllegalArgumentException(
                    String.format("The signature expects symbols of size %s but symbol is %s", 
                            bitsPerSymbol, symbol.getNumberBits()));
            }
        }
        this.symbols = symbols;
        this.bitsPerSymbol = bitsPerSymbol;
    }

    public List<SignatureSymbol> getSymbols() {
        return symbols == null ? Collections.emptyList() : Collections.unmodifiableList(symbols);
    }

    public void setSymbols(final List<SignatureSymbol> symbols) {
        this.symbols = symbols;
    }

    public int getSignatureSize() {
        return symbols.size();
    }

    public Signature addSymbol(final SignatureSymbol symbol) throws IllegalArgumentException {
        if(symbol.getNumberBits() != bitsPerSymbol){
            throw new IllegalArgumentException(
                String.format("The signature expects symbols of size %s but symbol is %s", 
                        bitsPerSymbol, symbol.getNumberBits()));
        }
        symbols.add(symbol);
        return this;
    }

    public byte getBitsPerSymbol() {
        return bitsPerSymbol;
    }
}

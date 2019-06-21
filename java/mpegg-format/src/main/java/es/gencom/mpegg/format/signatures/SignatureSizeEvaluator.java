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

public class SignatureSizeEvaluator {
    private final int signatureSizeInSymbols;
    private final int integerSize;
    private final byte numberBitsPerSymbol;
    private long sizeInBits;

    public SignatureSizeEvaluator(int signatureSizeInSymbols, int integerSize, byte numberBitsPerSymbol) {
        this.signatureSizeInSymbols = signatureSizeInSymbols;
        this.integerSize = integerSize;
        this.numberBitsPerSymbol = numberBitsPerSymbol;
        this.sizeInBits = 0;
    }

    public void simulate(Signature signature) throws IllegalArgumentException {
        if(signatureSizeInSymbols != 0) {
            if (signatureSizeInSymbols != signature.getSignatureSize()) {
                throw new IllegalArgumentException(
                        "The signature writer expects signature of size: " + signatureSizeInSymbols +
                                " but provided signature is of size: " + this.signatureSizeInSymbols
                );
            }
        }

        if(numberBitsPerSymbol != signature.getBitsPerSymbol()){
            throw new IllegalArgumentException(
                    "The signature writer expects signature with " + numberBitsPerSymbol +
                            " per symbols but provided signature has: " + signature.getBitsPerSymbol()
            );
        }

        int remainingSymbols = signature.getSymbols().size();
        if (signatureSizeInSymbols ==0 ){
            remainingSymbols += 1;
        }
        int currentSymbol = 0;

        int integersRequired = (int) Math.ceil(
                (double) remainingSymbols * (double) signature.getBitsPerSymbol() / (double) integerSize
        );

        for(int integer_i = 0; integer_i < integersRequired; integer_i++){
            int symbolsToWrite;
            if(integer_i == integersRequired-1){
                symbolsToWrite = remainingSymbols;
            } else {
                symbolsToWrite = (int) Math.floor(integerSize/signature.getBitsPerSymbol());
            }

            byte bitsToPad = (byte) (integerSize - signature.getBitsPerSymbol() * symbolsToWrite);
            sizeInBits += bitsToPad;

            for(int symbol_i = 0; symbol_i < symbolsToWrite; symbol_i++){
                if (currentSymbol == signature.getSymbols().size()){
                    byte numberBits = signature.getSymbols().get(currentSymbol-1).getNumberBits();
                    sizeInBits += numberBits;
                    break;
                }
                SignatureSymbol signatureSymbol = signature.getSymbols().get(currentSymbol);
                sizeInBits += signatureSymbol.getNumberBits();
                remainingSymbols -= 1;
                currentSymbol += 1;
            }
        }
    }

    public long getSizeInBits(){
        return sizeInBits;
    }

    public void simulateBits(long sizeInBits){
        this.sizeInBits += sizeInBits;
    }
}
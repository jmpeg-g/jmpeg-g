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

import es.gencom.mpegg.io.MPEGReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class SignatureReader {
    private final MPEGReader mpegReader;
    private final byte bitsPerSymbol;
    private final byte integerSize;
    private final short signatureSize;

    public SignatureReader(
            final MPEGReader mpegReader, 
            final byte bitsPerSymbol, 
            final byte integerSize, 
            final short signatureSize) {

        this.mpegReader = mpegReader;
        this.bitsPerSymbol = bitsPerSymbol;
        this.integerSize = integerSize;
        this.signatureSize = signatureSize;
    }

    static private byte symbolsInInteger(byte integerSize, byte bitsPerSymbol){
        return (byte) Math.floor(integerSize / bitsPerSymbol);
    }

    static private byte getPaddingSize(byte integerSize, byte remainingSymbols, byte bitsPerSymbol){
        return (byte) (integerSize - (remainingSymbols * bitsPerSymbol));
    }

    public Signature read(final long firstInteger) throws IOException {
        if (signatureSize == 0){
            return readUnknownSize(firstInteger);
        }else{
            return readKnownSize(firstInteger);
        }
    }

    public Signature read() throws IOException {
        if (signatureSize == 0){
            return readUnknownSize();
        }else{
            return readKnownSize();
        }
    }

    private Signature readUnknownSize(final long firstInteger) throws IOException {
        Signature signature = new Signature(bitsPerSymbol);

        IntegerReader integerReader = new IntegerReader(firstInteger, integerSize, bitsPerSymbol);
        boolean finishedReading;
        do{
            if (integerReader == null) {
                integerReader = new IntegerReader(mpegReader, integerSize, bitsPerSymbol);
            }
            for(Byte value : integerReader.parsedValues){
                signature.addSymbol(new SignatureSymbol(value, bitsPerSymbol));
            }
            finishedReading = integerReader.isTerminated();
            integerReader = null;
        } while (!finishedReading);

        return signature;
    }

    private Signature readUnknownSize() throws IOException {
        Signature signature = new Signature(bitsPerSymbol);

        IntegerReader integerReader;
        do{
            integerReader = new IntegerReader(mpegReader, integerSize, bitsPerSymbol);
            for(Byte value : integerReader.parsedValues){
                signature.addSymbol(new SignatureSymbol(value, bitsPerSymbol));
            }
        } while (!integerReader.isTerminated());

        return signature;
    }

    private Signature readKnownSize(final long firstInteger) throws IOException {
        Signature signature = new Signature(bitsPerSymbol);

        IntegerReader integerReader = new IntegerReader(firstInteger, integerSize, bitsPerSymbol);
        boolean finishedReading;
        do{
            if (integerReader == null) {
                integerReader = new IntegerReader(mpegReader, integerSize, bitsPerSymbol);
            }
            for(Byte value : integerReader.parsedValues){
                signature.addSymbol(new SignatureSymbol(value, bitsPerSymbol));
            }
            finishedReading = signature.getSignatureSize() == signatureSize;
            integerReader = null;
        } while (!finishedReading);

        return signature;
    }

    private Signature readKnownSize() throws IOException {
        Signature signature = new Signature(bitsPerSymbol);

        IntegerReader integerReader;
        do{
            integerReader = new IntegerReader(mpegReader, integerSize, bitsPerSymbol);
            for(Byte value : integerReader.parsedValues){
                signature.addSymbol(new SignatureSymbol(value, bitsPerSymbol));
            }
        } while (signature.getSignatureSize() != signatureSize);

        return signature;
    }
    
    private class IntegerReader {
        final long values;
        final private byte integerSize;
        final private byte currentPosition;
        final private byte bitsPerSymbols;
        List<Byte> parsedValues;
        private boolean terminated;



        IntegerReader(MPEGReader mpegReader, byte integerSize, byte bitsPerSymbols) throws IOException {
            this.integerSize = integerSize;
            this.currentPosition = 0;
            if(integerSize > 64){
                throw new UnsupportedOperationException();
            }

            values = mpegReader.readBits(integerSize);
            parsedValues = new ArrayList<>();
            this.bitsPerSymbols = bitsPerSymbols;
            terminated = false;

            parseValues();
        }

        IntegerReader(long integerValue, byte integerSize, byte bitsPerSymbols){
            this.integerSize = integerSize;
            this.currentPosition = 0;
            if(integerSize > 64){
                throw new UnsupportedOperationException();
            }

            values = integerValue;
            parsedValues = new ArrayList<>();
            this.bitsPerSymbols = bitsPerSymbols;
            terminated = false;

            parseValues();
        }

        boolean isTerminated(){
            return terminated;
        }

        private byte getMask(byte numberBits){
            byte value=0;
            for(byte bit_i=0; bit_i<numberBits; bit_i++){
                value |= 1<<bit_i;
            }
            return value;
        }

        private byte getParsedValue(byte positionFromEnd){
            long shiftedValue = values >>> (positionFromEnd * bitsPerSymbols);
            return (byte) (shiftedValue & getMask(bitsPerSymbols));
        }

        private void parseValues(){
            byte symbolsInInteger = symbolsInInteger(integerSize, bitsPerSymbols);
            Stack<Byte> stack = new Stack<>();
            for(byte symbol_i = 0; (symbol_i<symbolsInInteger); symbol_i++){
                byte parsedValue = getParsedValue(symbol_i);
                if(parsedValue == 0){
                    if(symbol_i == 0){
                        terminated = true;
                    }else{
                        break;
                    }
                } else {
                    stack.push(parsedValue);
                }
            }

            while(!stack.empty()){
                parsedValues.add(stack.pop());
            }
        }
    }
}

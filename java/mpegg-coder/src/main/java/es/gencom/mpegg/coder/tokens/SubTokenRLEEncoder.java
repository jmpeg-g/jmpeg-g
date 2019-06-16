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

import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;

public class SubTokenRLEEncoder implements SubTokenSequenceEncoder {
    private final MPEGWriter writer;
    private final short rleGuard;
    private short currentValue;
    private long currentLength;

    public SubTokenRLEEncoder(MPEGWriter writer, short rleGuard) {
        this.writer = writer;
        this.rleGuard = rleGuard;
        currentValue = rleGuard;
        currentLength = 0;
    }

    @Override
    public void writeValue(short value) throws IOException {
        if(value == rleGuard){
            if(currentValue != rleGuard){
                writeCurrentSymbol();
            }
            writer.writeUnsignedByte(rleGuard);
            writer.writeU7(0);
            currentValue = rleGuard;
            currentLength = 1;

        }
        if(currentValue == value){
            currentLength++;
        }else{
            if(currentValue != rleGuard) {
                writeCurrentSymbol();
            }
            currentValue = value;
            currentLength = 1;
        }
    }

    private void writeCurrentSymbol() throws IOException {
        if(currentLength != 1){
            if(currentLength == 2){
                writer.writeUnsignedByte(currentValue);
                writer.writeUnsignedByte(currentValue);
            }else {
                writer.writeUnsignedByte(rleGuard);
                writer.writeU7(currentLength);
                writer.writeUnsignedByte(currentValue);
            }
        }else{
            writer.writeUnsignedByte(currentValue);
        };
    }

    @Override
    public void close() throws IOException {
        writeCurrentSymbol();
    }
}

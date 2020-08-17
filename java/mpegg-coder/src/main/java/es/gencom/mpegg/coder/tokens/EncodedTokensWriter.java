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

import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.TokentypeDecoderConfiguration;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.MSBitOutputArray;

import java.io.IOException;
import java.util.Arrays;

public class EncodedTokensWriter {

    /**
     * Upon transforming the different tokens list to symbols, these are to be written encoded and compressed into a
     * block using thise function.
     * @param writer MPEGwriter to which the encoded data shall be written to
     * @param readIdentifierEncoder The read identifier encoder which data shall be encoded
     * @param descriptor_id The descriptor id which shall be used for the block created while compressing. Only the
     *                      descriptors for RNAME and MSAR are legitimate here.
     * @param tokentypeDecoderConfiguration Configuration of the entropy encoder
     * @throws IOException Can be thrown at multiple points
     */
    public static void write(
            MPEGWriter writer,
            AbstractReadIdentifierEncoder readIdentifierEncoder,
            DESCRIPTOR_ID descriptor_id,
            TokentypeDecoderConfiguration tokentypeDecoderConfiguration
    ) throws IOException {

        short[][][] values = readIdentifierEncoder.resizeAndGetValues();

        writer.writeUnsignedInt(readIdentifierEncoder.getNumberEntries());
        writer.writeUnsignedShort(readIdentifierEncoder.getNumberSubSequences());

        for(int token_i=0; token_i < values.length; token_i++) {
            for(int subtoken_i=0; subtoken_i < values[token_i].length; subtoken_i++) {
                if(values[token_i][subtoken_i].length == 0){
                    continue;
                }

                writer.writeBits(subtoken_i, 4);

                //**** SEARCH FOR AN IDENTICAL SUBSEQUENCE*****//
                boolean found = false;
                int foundToken;
                int foundSubtoken = 0;
                boolean stop = false;
                for(foundToken=0; foundToken < values.length; foundToken++) {
                    for (foundSubtoken = 0; foundSubtoken < values[token_i].length; foundSubtoken++) {
                        if(foundToken == token_i && foundSubtoken == subtoken_i){
                            stop = true;
                            break;
                        }
                        if(Arrays.equals(values[foundToken][foundSubtoken], values[token_i][subtoken_i])){
                            found = true;
                            stop = true;
                            break;
                        }
                    }
                    if(stop){
                        break;
                    }
                }
                if(found){
                    writer.writeBits(0, 4);
                    writer.writeUnsignedShort(foundToken << 4 | foundSubtoken);
                    continue;
                }


                //**** SEARCH FOR THE BEST METHOD*****//
                int sizeCAT = values[token_i][subtoken_i].length;

                MSBitOutputArray writerRLE = new MSBitOutputArray();
                SubTokenRLEEncoder rleEncoder = new SubTokenRLEEncoder(
                        tokentypeDecoderConfiguration.getTokentypeEncoder(writerRLE, descriptor_id, COMPRESSION_METHOD_ID.RLE));

                MSBitOutputArray writerCABAC0 = new MSBitOutputArray();
                SubTokenCABACMethod0Encoder cabac0Encoder = new SubTokenCABACMethod0Encoder(
                        writerCABAC0,
                        descriptor_id,
                        tokentypeDecoderConfiguration);

                MSBitOutputArray writerCABAC1 = new MSBitOutputArray();
                SubTokenCABACMethod1Encoder cabac1Encoder = new SubTokenCABACMethod1Encoder(
                        writerCABAC1,
                        descriptor_id,
                        tokentypeDecoderConfiguration);

                for(int value_i=0; value_i<values[token_i][subtoken_i].length; value_i++){
                    rleEncoder.writeValue(values[token_i][subtoken_i][value_i]);
                    cabac0Encoder.writeValue(values[token_i][subtoken_i][value_i]);
                    cabac1Encoder.writeValue(values[token_i][subtoken_i][value_i]);
                }
                rleEncoder.close();
                cabac0Encoder.close();
                cabac1Encoder.close();

                writerRLE.flush();
                writerCABAC0.flush();
                writerCABAC1.flush();

                int rleSize = writerRLE.size();
                int cabac0Size = writerCABAC0.size();
                int cabac1Size = writerCABAC1.size();

                //****Decide which is the best method and write to file***//
                int minimalLength = Math.min(Math.min(sizeCAT, rleSize), Math.min(cabac0Size, cabac1Size));
                SubTokenSequenceEncoder bestEncoder;
                if(sizeCAT == minimalLength){
                    writer.writeBits(1, 4);
                    bestEncoder = new SubTokenCATEncoder(writer);
                } else if (rleSize == minimalLength){
                    writer.writeBits(2, 4);
                    bestEncoder = new SubTokenRLEEncoder(
                        tokentypeDecoderConfiguration.getTokentypeEncoder(writer, descriptor_id, COMPRESSION_METHOD_ID.RLE));
                } else if (cabac0Size == minimalLength){
                    writer.writeBits(3, 4);
                    bestEncoder = new SubTokenCABACMethod0Encoder(
                            writer,
                            descriptor_id,
                            tokentypeDecoderConfiguration
                    );
                } else {
                    writer.writeBits(4, 4);
                    bestEncoder = new SubTokenCABACMethod1Encoder(
                            writer,
                            descriptor_id,
                            tokentypeDecoderConfiguration
                    );
                }
                writer.writeU7(values[token_i][subtoken_i].length);
                for(int value_i=0; value_i<values[token_i][subtoken_i].length; value_i++){
                    bestEncoder.writeValue(values[token_i][subtoken_i][value_i]);
                }
                bestEncoder.close();
            }
        }
    }
}

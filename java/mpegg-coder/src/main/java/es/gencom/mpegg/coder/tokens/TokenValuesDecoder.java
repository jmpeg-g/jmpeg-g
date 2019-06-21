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

import es.gencom.mpegg.CABAC.configuration.CABAC_TokentypeDecoderConfiguration;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.TokentypeDecoderConfiguration;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.io.MPEGReader;

import java.io.IOException;
import java.util.Arrays;

public class TokenValuesDecoder {
    public static short[][][] decodeTokenValues(
            MPEGReader reader,
            EncodingParameters encodingParameters,
            DESCRIPTOR_ID descriptor_id,
            DATA_CLASS dataClass) throws IOException {

        long num_output_descriptors = reader.readUnsignedInt();
        int num_tokentype_sequences = reader.readUnsignedShort();
        short[][][] result = new short[16][16][];
        int typeNum = -1;

        TokentypeDecoderConfiguration decoderConfiguration = encodingParameters.getDecoderConfiguration(
                descriptor_id,
                dataClass);

        if(! (decoderConfiguration instanceof CABAC_TokentypeDecoderConfiguration)){
            throw new IllegalArgumentException();
        }
        CABAC_TokentypeDecoderConfiguration cabacTokentypeDecoderConfiguration =
                (CABAC_TokentypeDecoderConfiguration)decoderConfiguration;

        for(int tokentype_sequence_i = 0;
                tokentype_sequence_i < num_tokentype_sequences;
                tokentype_sequence_i++) {

            byte typeId = (byte) reader.readBits(4);
            if(typeId == 0) {
                typeNum++;
            }
            byte methodId = (byte) reader.readBits(4);
            COMPRESSION_METHOD_ID compression_method_id = COMPRESSION_METHOD_ID.getCompressionMethodId(methodId);
            if(compression_method_id == COMPRESSION_METHOD_ID.COP){
                int refMappedtypeID = reader.readUnsignedShort();
                int refTypeNum = refMappedtypeID >> 4;
                int refTypeId = refMappedtypeID & 0xf;

                if(result[refTypeNum][refTypeId] == null){
                    throw new IllegalArgumentException();
                }
                result[typeNum][typeId] = result[refTypeNum][refTypeId];
            } else {
                int numOutputSymbols = Math.toIntExact(reader.readVarSizedUnsignedInt());
                result[typeNum][typeId] = new short[numOutputSymbols];
                SubTokenSequenceDecoder subTokenSequenceDecoder;
                switch (compression_method_id){
                    case CAT:
                        subTokenSequenceDecoder = new SubTokenCATDecoder(reader, numOutputSymbols);
                        break;
                    case RLE:
                        subTokenSequenceDecoder = new SubTokenRLEDecoder(
                                cabacTokentypeDecoderConfiguration.rle_guard_tokentype,
                                reader,
                                numOutputSymbols
                        );
                        break;
                    case CABAC_ORDER_0:
                        subTokenSequenceDecoder = new SubTokenCABACMethod0Decoder(
                                reader,
                                descriptor_id,
                                cabacTokentypeDecoderConfiguration,
                                numOutputSymbols
                        );
                        break;
                    case CABAC_ORDER_1:
                        subTokenSequenceDecoder = new SubTokenCABACMethod1Decoder(
                                reader,
                                descriptor_id,
                                cabacTokentypeDecoderConfiguration,
                                numOutputSymbols
                        );
                        break;
                    case X4:
                        subTokenSequenceDecoder = new SubTokenX4Decoder(
                                reader,
                                descriptor_id,
                                numOutputSymbols,
                                decoderConfiguration
                        );
                        break;
                    default:
                        throw new IllegalArgumentException();
                }

                for(int symbol_i=0; symbol_i < numOutputSymbols; symbol_i++){
                    result[typeNum][typeId][symbol_i] = subTokenSequenceDecoder.getSubTokenUnsignedByte();
                }
            }
        }
        result = Arrays.copyOf(result, typeNum+1);
        return result;
    }
}

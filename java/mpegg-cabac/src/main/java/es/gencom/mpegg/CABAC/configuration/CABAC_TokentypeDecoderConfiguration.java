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

package es.gencom.mpegg.CABAC.configuration;

import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_SubsequenceCoderConfiguration;
import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_SubsequenceConfiguration;
import es.gencom.mpegg.CABAC.decoder.CABAC_TokentypeRleDecoder;
import es.gencom.mpegg.CABAC.decoder.CABAC_TokentypeDecoder;
import es.gencom.mpegg.CABAC.encoder.CABAC_TokentypeEncoder;
import es.gencom.mpegg.CABAC.encoder.CABAC_TokentypeRleEncoder;
import es.gencom.mpegg.coder.compression.AbstractDecoderConfiguration;
import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoder;
import es.gencom.mpegg.coder.compression.DescriptorEncoder;
import es.gencom.mpegg.coder.compression.ENCODING_MODE_ID;
import es.gencom.mpegg.coder.compression.TokentypeDecoderConfiguration;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_TokentypeDecoderConfiguration 
        extends AbstractDecoderConfiguration implements TokentypeDecoderConfiguration {

    public final short rle_guard_tokentype;
    public final CABAC_SubsequenceConfiguration tokentype_order0_config;
    public final CABAC_SubsequenceConfiguration tokentype_order1_config;
    
    public CABAC_TokentypeDecoderConfiguration() {
        super(ENCODING_MODE_ID.CABAC);
        
        this.rle_guard_tokentype = 127;
        this.tokentype_order0_config = new CABAC_SubsequenceConfiguration(
                new CABAC_SubsequenceCoderConfiguration(
                new CABAC_SubsequenceEncodingConfiguration(COMPRESSION_METHOD_ID.CABAC_ORDER_0)));
        this.tokentype_order1_config = new CABAC_SubsequenceConfiguration(
                new CABAC_SubsequenceCoderConfiguration(
                new CABAC_SubsequenceEncodingConfiguration(COMPRESSION_METHOD_ID.CABAC_ORDER_1)));
        
    }
    
    public CABAC_TokentypeDecoderConfiguration(
            final short rle_guard_tokentype,
            final CABAC_SubsequenceConfiguration tokentype_order0_config,
            final CABAC_SubsequenceConfiguration tokentype_order1_config) {

        super(ENCODING_MODE_ID.CABAC);
        
        this.rle_guard_tokentype = rle_guard_tokentype;
        this.tokentype_order0_config = tokentype_order0_config;
        this.tokentype_order1_config = tokentype_order1_config;
    }
    
    @Override
    public DescriptorDecoder getTokentypeDecoder(
            final MPEGReader reader, 
            final DESCRIPTOR_ID descriptor_id,
            final COMPRESSION_METHOD_ID compression_method_id,
            final long numOutputSymbols) throws IOException {
        
        final CABAC_SubsequenceConfiguration tokentype_config;
        switch (compression_method_id) {
            case CABAC_ORDER_0: tokentype_config = tokentype_order0_config; break;
            case CABAC_ORDER_1: tokentype_config = tokentype_order1_config; break;
            case RLE: return new CABAC_TokentypeRleDecoder(reader,
                                                           descriptor_id,
                                                           numOutputSymbols,
                                                           rle_guard_tokentype);

            default: tokentype_config = null;
        }
        
        if (tokentype_config == null) {
            return new CABAC_TokentypeDecoder(reader, numOutputSymbols, descriptor_id, compression_method_id);
        }

        return new CABAC_TokentypeDecoder(reader, numOutputSymbols, descriptor_id, tokentype_config);
    }

    @Override
    public DescriptorEncoder getTokentypeEncoder(
            final MPEGWriter writer, 
            final DESCRIPTOR_ID descriptor_id,
            final COMPRESSION_METHOD_ID compression_method_id) {
        
        final CABAC_SubsequenceConfiguration tokentype_config;
        switch (compression_method_id) {
            case CABAC_ORDER_0: tokentype_config = tokentype_order0_config; break;
            case CABAC_ORDER_1: tokentype_config = tokentype_order1_config; break;
            case RLE: return new CABAC_TokentypeRleEncoder(writer,
                                                           descriptor_id,
                                                           rle_guard_tokentype);
            default: tokentype_config = null;
        }
        
        if (tokentype_config == null) {
            return new CABAC_TokentypeEncoder(writer, descriptor_id, compression_method_id);
        }

        return new CABAC_TokentypeEncoder(
                writer,
                descriptor_id,
                compression_method_id,
                tokentype_config);

    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeUnsignedByte(rle_guard_tokentype);
        tokentype_order0_config.write(writer);
        tokentype_order1_config.write(writer);
    }

    @Override
    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += 8;
        sizeInBits += tokentype_order0_config.sizeInBits();
        sizeInBits += tokentype_order1_config.sizeInBits();
        return sizeInBits;
    }

    public static CABAC_TokentypeDecoderConfiguration read(final MPEGReader reader) throws IOException {
        final short rle_guard_tokentype = reader.readUnsignedByte();
        final CABAC_SubsequenceConfiguration tokentype_order0_config = CABAC_SubsequenceConfiguration.read(reader);
        final CABAC_SubsequenceConfiguration tokentype_order1_config = CABAC_SubsequenceConfiguration.read(reader);
        
        return new CABAC_TokentypeDecoderConfiguration(
                rle_guard_tokentype,
                tokentype_order0_config,
                tokentype_order1_config);
    }
}

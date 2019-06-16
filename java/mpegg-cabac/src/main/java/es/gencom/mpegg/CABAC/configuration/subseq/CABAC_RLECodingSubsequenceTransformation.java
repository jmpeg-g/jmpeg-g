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

package es.gencom.mpegg.CABAC.configuration.subseq;

import es.gencom.mpegg.CABAC.configuration.CABAC_SUBSEQ_TRANSFORM_ID;
import es.gencom.mpegg.CABAC.configuration.CABAC_SubsequenceEncodingConfiguration;
import es.gencom.mpegg.CABAC.decoder.CABAC_RLECodingDecoder;
import es.gencom.mpegg.CABAC.decoder.SubsequenceTransformDecoder;
import es.gencom.mpegg.CABAC.encoder.CABAC_RLECodingEncoder;
import es.gencom.mpegg.CABAC.encoder.SubsequenceTransformEncoder;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_RLECodingSubsequenceTransformation
        extends SubsequenceCoderConfiguration {
    
    public final short rle_coding_guard;
    public final CABAC_SubsequenceEncodingConfiguration lengths_config;
    public final CABAC_SubsequenceEncodingConfiguration symbols_config;
    
    public CABAC_RLECodingSubsequenceTransformation(
            final short rle_coding_guard,
            final CABAC_SubsequenceEncodingConfiguration lengths_config,
            final CABAC_SubsequenceEncodingConfiguration symbols_config) {

        super(CABAC_SUBSEQ_TRANSFORM_ID.RLE_CODING);

        this.rle_coding_guard = rle_coding_guard;
        
        this.lengths_config = lengths_config;
        this.symbols_config = symbols_config;
    }
    
    @Override
    public SubsequenceTransformDecoder getDecoder(
            final MPEGReader reader,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {

        return new CABAC_RLECodingDecoder(
                reader, alphabet_id, descriptor_id, descriptor_subsequence_id,
                rle_coding_guard, lengths_config, symbols_config, ref_source);
    }

    @Override
    public SubsequenceTransformEncoder getEncoder(
            final MPEGWriter writer,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            InputStream ref_source) throws IOException {

        return new CABAC_RLECodingEncoder(
                writer, alphabet_id, descriptor_id, descriptor_subsequence_id, rle_coding_guard,
                lengths_config, symbols_config, ref_source);
    }

    @Override
    public void write(MPEGWriter writer) throws IOException {
        writer.writeBits(rle_coding_guard, 8);
        
        lengths_config.write(writer);
        symbols_config.write(writer);
    }

    @Override
    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += 8; //rle_coding_guard
        sizeInBits += lengths_config.sizeInBits();
        sizeInBits += symbols_config.sizeInBits();
        return sizeInBits;
    }

    public static CABAC_RLECodingSubsequenceTransformation read(
            final MPEGReader reader) throws IOException {
        
        final short rle_coding_guard = reader.readUnsignedByte();
        
        final CABAC_SubsequenceEncodingConfiguration lengths_config 
                = CABAC_SubsequenceEncodingConfiguration.read(reader);

        final CABAC_SubsequenceEncodingConfiguration symbols_config 
                = CABAC_SubsequenceEncodingConfiguration.read(reader);

        return new CABAC_RLECodingSubsequenceTransformation(rle_coding_guard, lengths_config, symbols_config);
    }
}
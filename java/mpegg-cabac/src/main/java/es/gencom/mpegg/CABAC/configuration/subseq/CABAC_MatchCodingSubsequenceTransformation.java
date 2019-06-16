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
import es.gencom.mpegg.CABAC.decoder.CABAC_MatchCodingDecoder;
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

public class CABAC_MatchCodingSubsequenceTransformation
        extends SubsequenceCoderConfiguration {
    
    public final int match_coding_buffer_size;  // u(16)

    public final CABAC_SubsequenceEncodingConfiguration pointers_config;
    public final CABAC_SubsequenceEncodingConfiguration lengths_config;
    public final CABAC_SubsequenceEncodingConfiguration symbols_config;
    
    public CABAC_MatchCodingSubsequenceTransformation(
            final int match_coding_buffer_size, 
            final CABAC_SubsequenceEncodingConfiguration pointers_config,
            final CABAC_SubsequenceEncodingConfiguration lengths_config,
            final CABAC_SubsequenceEncodingConfiguration symbols_config) {
        super(CABAC_SUBSEQ_TRANSFORM_ID.MATCH_CODING);
        
        this.match_coding_buffer_size = match_coding_buffer_size;
        
        this.pointers_config = pointers_config;
        this.lengths_config = lengths_config;
        this.symbols_config = symbols_config;
    }
    
    @Override
    public CABAC_MatchCodingDecoder getDecoder(
            final MPEGReader reader,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {

        return new CABAC_MatchCodingDecoder(
                reader, alphabet_id, descriptor_id, descriptor_subsequence_id, 
                match_coding_buffer_size, 
                pointers_config, lengths_config, symbols_config, ref_source);
    }
    
    @Override
    public SubsequenceTransformEncoder getEncoder(
            final MPEGWriter writer,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {
        
        return null; //return new CABAC_MatchCodingEncoder()
    }

    @Override
    public void write(MPEGWriter writer) throws IOException {
        
        writer.writeShort((short)match_coding_buffer_size);
        
        pointers_config.write(writer);
        lengths_config.write(writer);
        symbols_config.write(writer);
    }

    @Override
    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += 16; //match_coding_buffer_size;
        sizeInBits += pointers_config.sizeInBits();
        sizeInBits += lengths_config.sizeInBits();
        sizeInBits += symbols_config.sizeInBits();
        return sizeInBits;
    }

    public static CABAC_MatchCodingSubsequenceTransformation read(MPEGReader reader) throws IOException {
        
        final int match_coding_buffer_size = reader.readUnsignedShort();
        
        final CABAC_SubsequenceEncodingConfiguration pointers_config
                = CABAC_SubsequenceEncodingConfiguration.read(reader);

        final CABAC_SubsequenceEncodingConfiguration lengths_config
                = CABAC_SubsequenceEncodingConfiguration.read(reader);

        final CABAC_SubsequenceEncodingConfiguration symbols_config
                = CABAC_SubsequenceEncodingConfiguration.read(reader);
        
        return new CABAC_MatchCodingSubsequenceTransformation(
                match_coding_buffer_size,
                pointers_config,
                lengths_config,
                symbols_config);
    }
    
}

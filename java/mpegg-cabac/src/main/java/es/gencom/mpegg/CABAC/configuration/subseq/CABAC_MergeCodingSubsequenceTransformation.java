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

public class CABAC_MergeCodingSubsequenceTransformation
        extends SubsequenceCoderConfiguration {
    
    //private final byte[] merge_coding_shift_size;
    private final CABAC_SubsequenceEncodingConfiguration[] encoding_configurations;
    
    public CABAC_MergeCodingSubsequenceTransformation(
            CABAC_SubsequenceEncodingConfiguration[] encoding_configurations) {
        
        super(CABAC_SUBSEQ_TRANSFORM_ID.MERGE_CODING);
        
        this.encoding_configurations = encoding_configurations;
    }
    
    @Override
    public CABAC_MatchCodingDecoder getDecoder(
            final MPEGReader reader,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {
        return null;
    }
    
    @Override
    public SubsequenceTransformEncoder getEncoder(
            final MPEGWriter writer,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {
        return null;
    }

    @Override
    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(encoding_configurations.length, 4);
        
        for(int i = 0; i < encoding_configurations.length; i++) {
            final byte output_symbol_size = encoding_configurations[i].symbol_encoding_params.output_symbol_size;
            writer.writeBits(output_symbol_size, 5);
        }
        
        for(int i = 0; i < encoding_configurations.length; i++) {
            encoding_configurations[i].write(writer);
        }
    }

    @Override
    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += 4; //encoding_configurations.length
        for(int i = 0; i < encoding_configurations.length; i++) {
            sizeInBits += 5; //output_symbol_size
        }
        for(int i = 0; i < encoding_configurations.length; i++) {
            sizeInBits += encoding_configurations[i].sizeInBits();
        }
        return sizeInBits;
    }

    public static CABAC_MergeCodingSubsequenceTransformation read(final MPEGReader reader) throws IOException {
        
        final int merge_coding_subseq_count = (int)reader.readBits(4); // u(4
        
        final CABAC_SubsequenceEncodingConfiguration[] encoding_configurations = 
                new CABAC_SubsequenceEncodingConfiguration[merge_coding_subseq_count];

        for(int i = 0; i < merge_coding_subseq_count; i++) {
            reader.readBits(5); // u(5) merge_coding_shift_size
        }
        
        for(int i = 0; i < merge_coding_subseq_count; i++) {
            encoding_configurations[i] = CABAC_SubsequenceEncodingConfiguration.read(reader);
        }

        return new CABAC_MergeCodingSubsequenceTransformation(
                encoding_configurations);
    }

}

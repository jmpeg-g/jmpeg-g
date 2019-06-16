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
import es.gencom.mpegg.CABAC.decoder.SubsequenceTransformDecoder;
import es.gencom.mpegg.CABAC.encoder.SubsequenceTransformEncoder;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * CABAC decoding configuration for the descriptor subsequence.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_SubsequenceConfiguration {
    
    public final short descriptor_subsequence_id; // u(10)
    public final SubsequenceCoderConfiguration subsequence_coder_configuration;

    public CABAC_SubsequenceConfiguration(
            final SubsequenceCoderConfiguration subsequence_coder_configuration) {
        this((short)0, subsequence_coder_configuration);
    }
    
    public CABAC_SubsequenceConfiguration(
            final short descriptor_subsequence_id,
            final SubsequenceCoderConfiguration subsequence_coder_configuration) {
        
        this.descriptor_subsequence_id = descriptor_subsequence_id;
        this.subsequence_coder_configuration = subsequence_coder_configuration;
    }

    public SubsequenceTransformDecoder getDecoder(
            final MPEGReader reader,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final InputStream ref_source) throws IOException {
        
        return subsequence_coder_configuration.getDecoder(
                reader, alphabet_id, descriptor_id, descriptor_subsequence_id, ref_source);
    }
    
    public SubsequenceTransformEncoder getEncoder(
            final MPEGWriter writer,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final InputStream ref_source) throws IOException {
        
        return subsequence_coder_configuration.getEncoder(
                writer, alphabet_id, descriptor_id, descriptor_subsequence_id, ref_source);
    }

    public void write(final MPEGWriter writer) throws IOException {
        subsequence_coder_configuration.cabac_transform_id.write(writer);
        subsequence_coder_configuration.write(writer);
    }

    public static CABAC_SubsequenceConfiguration read(
            final MPEGReader reader) throws IOException {
        
        return new CABAC_SubsequenceConfiguration(
                CABAC_SubsequenceConfiguration.readSubsequenceCoderConfiguration(reader));
    }

    protected static SubsequenceCoderConfiguration readSubsequenceCoderConfiguration(
            final MPEGReader reader) throws IOException {
        
        final CABAC_SUBSEQ_TRANSFORM_ID transform_id_subseq = CABAC_SUBSEQ_TRANSFORM_ID.read(reader);
        
        switch(transform_id_subseq) {
            case NO_TRANSFORM:
                return CABAC_SubsequenceCoderConfiguration.read(reader);
            case EQUALITY_CODING:
                return CABAC_EqualityCodingSubsequenceTransformation.read(reader);
            case MATCH_CODING:
                return CABAC_MatchCodingSubsequenceTransformation.read(reader);
            case RLE_CODING:
                return CABAC_RLECodingSubsequenceTransformation.read(reader);
            case MERGE_CODING:
                return CABAC_MergeCodingSubsequenceTransformation.read(reader);
            case RLE_QV_CODING:
                return CABAC_RLEQVCodingSubsequenceTransformation.read(reader);
            default:
                // EXCEPTION !!! ???
        }

        return null;
    }

    public long sizeInBits() {
        long sizeInBits = 0;
        sizeInBits += 8; //subsequence_coder_configuration.cabac_transform_id
        sizeInBits += subsequence_coder_configuration.sizeInBits();
        return sizeInBits;
    }
}

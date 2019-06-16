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

import es.gencom.mpegg.CABAC.configuration.CABAC_SubsequenceEncodingConfiguration;
import es.gencom.mpegg.CABAC.configuration.CABAC_SUBSEQ_TRANSFORM_ID;
import es.gencom.mpegg.CABAC.decoder.CABAC_EqualityCodingDecoder;
import es.gencom.mpegg.CABAC.encoder.CABAC_EqualityCodingEncoder;
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

public class CABAC_EqualityCodingSubsequenceTransformation 
        extends SubsequenceCoderConfiguration {
    
    public final CABAC_SubsequenceEncodingConfiguration flags_configuration;
    public final CABAC_SubsequenceEncodingConfiguration symbols_configuration;

    public CABAC_EqualityCodingSubsequenceTransformation(
            final CABAC_SubsequenceEncodingConfiguration flags_configuration,
            final CABAC_SubsequenceEncodingConfiguration symbols_configuration) {
        
        super(CABAC_SUBSEQ_TRANSFORM_ID.EQUALITY_CODING);
        
        this.flags_configuration = flags_configuration;
        this.symbols_configuration = symbols_configuration;
    }
    
    @Override
    public CABAC_EqualityCodingDecoder getDecoder(
            final MPEGReader reader,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {

        return new CABAC_EqualityCodingDecoder(
                reader, alphabet_id, descriptor_id, descriptor_subsequence_id, 
                flags_configuration, symbols_configuration, ref_source);
    }

    @Override
    public SubsequenceTransformEncoder getEncoder(
            final MPEGWriter writer,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final InputStream ref_source) throws IOException {
        
        return new CABAC_EqualityCodingEncoder(
                writer, alphabet_id, descriptor_id, descriptor_subsequence_id,
                flags_configuration, symbols_configuration, ref_source);
    }

    @Override
    public void write(MPEGWriter writer) throws IOException {
        flags_configuration.write(writer);
        symbols_configuration.write(writer);
    }

    @Override
    public long sizeInBits() {
        long symbolSize = 0;
        symbolSize += flags_configuration.sizeInBits();
        symbolSize += symbols_configuration.sizeInBits();
        return symbolSize;
    }

    public static CABAC_EqualityCodingSubsequenceTransformation read(
            final MPEGReader reader) throws IOException {
        
        final CABAC_SubsequenceEncodingConfiguration flags_configuration 
                = CABAC_SubsequenceEncodingConfiguration.read(reader);

        final CABAC_SubsequenceEncodingConfiguration symbols_configuration
                = CABAC_SubsequenceEncodingConfiguration.read(reader);
        
        return new CABAC_EqualityCodingSubsequenceTransformation(
                                        flags_configuration, symbols_configuration);
    }
}

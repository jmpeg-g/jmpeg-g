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

package es.gencom.mpegg.CABAC.decoder;

import es.gencom.mpegg.CABAC.configuration.CABAC_SubsequenceEncodingConfiguration;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.io.MPEGReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_MatchCodingDecoder extends MatchCodingDecoder {

    public final SyntaxElementDecoder symbols_decoder;
    public final SyntaxElementDecoder lengths_decoder;
    public final SyntaxElementDecoder pointers_decoder;

    public CABAC_MatchCodingDecoder(
            final MPEGReader reader,
            final ALPHABET_ID alphabet_id,
            final DESCRIPTOR_ID descriptor_id,
            final short descriptor_subsequence_id,
            final int match_coding_buffer_size,
            final CABAC_SubsequenceEncodingConfiguration pointers_config,
            final CABAC_SubsequenceEncodingConfiguration lengths_config,
            final CABAC_SubsequenceEncodingConfiguration symbols_config,
            final InputStream ref_source) throws IOException {
        
        super(match_coding_buffer_size);

        pointers_decoder = new SyntaxElementDecoder(
                reader,
                pointers_config.getSubsequenceCodecConfiguration(alphabet_id, descriptor_id, descriptor_subsequence_id),
                pointers_config.binarization,
                pointers_config.context_parameters.adaptive_mode_flag,
                ref_source);

        lengths_decoder = new SyntaxElementDecoder(
                reader,
                lengths_config.getSubsequenceCodecConfiguration(alphabet_id, descriptor_id, descriptor_subsequence_id),
                lengths_config.binarization,
                lengths_config.context_parameters.adaptive_mode_flag,
                ref_source);

        symbols_decoder = new SyntaxElementDecoder(
                reader,
                symbols_config.getSubsequenceCodecConfiguration(alphabet_id, descriptor_id, descriptor_subsequence_id),
                symbols_config.binarization,
                symbols_config.context_parameters.adaptive_mode_flag,
                ref_source);
    }

    @Override
    protected long decode_symbol() throws IOException {
        return symbols_decoder.decode();
    }

    @Override
    protected long decode_length() throws IOException {
        return lengths_decoder.decode();
    }

    @Override
    protected long decode_pointer() throws IOException {
        return pointers_decoder.decode();
    }    
}

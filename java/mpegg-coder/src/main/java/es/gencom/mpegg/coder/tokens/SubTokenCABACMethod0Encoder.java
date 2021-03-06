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

import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorEncoder;
import es.gencom.mpegg.coder.compression.TokentypeDecoderConfiguration;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;

public class SubTokenCABACMethod0Encoder implements SubTokenSequenceEncoder {
    private final DescriptorEncoder tokentypeEncoder;

    public SubTokenCABACMethod0Encoder(
            final MPEGWriter writer,
            final DESCRIPTOR_ID descriptor_id,
            final TokentypeDecoderConfiguration decoderConfiguration) {

        tokentypeEncoder = decoderConfiguration.getTokentypeEncoder(
                        writer,
                        descriptor_id,
                        COMPRESSION_METHOD_ID.CABAC_ORDER_0);
    }

    @Override
    public void writeValue(final short value) throws IOException {
        tokentypeEncoder.write(value);
    }

    @Override
    public void close() throws IOException {
        tokentypeEncoder.close();
    }
}
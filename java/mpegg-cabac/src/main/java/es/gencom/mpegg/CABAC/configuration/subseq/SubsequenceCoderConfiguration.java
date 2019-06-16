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
 * An abstract class for CABAC configuration to encode/decode descriptor`s subsequence.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro 
 */

public abstract class SubsequenceCoderConfiguration {
    
    public final CABAC_SUBSEQ_TRANSFORM_ID cabac_transform_id;
    
    public SubsequenceCoderConfiguration(final CABAC_SUBSEQ_TRANSFORM_ID cabac_transform_id) {
        this.cabac_transform_id = cabac_transform_id;
    }
    
    public abstract SubsequenceTransformDecoder getDecoder(
            MPEGReader reader,
            ALPHABET_ID alphabet_id,
            DESCRIPTOR_ID descriptor_id,
            short descriptor_subsequence_id,
            InputStream ref_source) throws IOException;
    
    public abstract SubsequenceTransformEncoder getEncoder(
            MPEGWriter writer,
            ALPHABET_ID alphabet_id,
            DESCRIPTOR_ID descriptor_id,
            short descriptor_subsequence_id,
            InputStream ref_source) throws IOException;
    
    public abstract void write(MPEGWriter writer) throws IOException;

    public abstract long sizeInBits();
}

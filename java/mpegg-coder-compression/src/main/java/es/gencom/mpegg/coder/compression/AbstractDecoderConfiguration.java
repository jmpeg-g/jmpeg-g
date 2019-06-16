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

package es.gencom.mpegg.coder.compression;

import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * <p>
 * An abstract Decoder Configuration (12.3) class to be extended by any particular 
 * Decoder Configuration implementations. 
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */
        
public abstract class AbstractDecoderConfiguration implements DecoderConfiguration {

    public final ENCODING_MODE_ID encoding_mode_id; // u(8)
    
    public AbstractDecoderConfiguration(
            final ENCODING_MODE_ID encoding_mode_id) {
        
        this.encoding_mode_id = encoding_mode_id;
    }
    
    /**
     * Write the decoder_configuration_type into the MPEG stream
     * 
     * @param writer
     * @throws IOException 
     */
    public abstract void write(MPEGWriter writer) throws IOException;
}

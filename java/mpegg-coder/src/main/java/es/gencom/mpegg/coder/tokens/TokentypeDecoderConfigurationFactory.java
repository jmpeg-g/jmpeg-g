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

import es.gencom.mpegg.CABAC.configuration.CABAC_TokentypeDecoderConfiguration;
import es.gencom.mpegg.coder.compression.AbstractDecoderConfiguration;
import es.gencom.mpegg.coder.compression.ENCODING_MODE_ID;
import es.gencom.mpegg.coder.compression.TokentypeDecoderConfiguration;
import es.gencom.mpegg.io.MPEGReader;
import java.io.IOException;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class TokentypeDecoderConfigurationFactory {

    /**
     * @param encoding_mode_id
     * @return appropriate decoder configuration (CABAC)
     * 
     * @throws IOException 
     */
    public static TokentypeDecoderConfiguration getDefaultDecoderConfiguration(
            final ENCODING_MODE_ID encoding_mode_id) throws IOException {

        switch(encoding_mode_id) {
            case CABAC: return new CABAC_TokentypeDecoderConfiguration();
        }
        return null;
    }
    
    public static AbstractDecoderConfiguration read(MPEGReader reader) throws IOException {
        final ENCODING_MODE_ID encoding_mode_id = ENCODING_MODE_ID.read(reader);
        
        switch(encoding_mode_id) {
            case CABAC: return CABAC_TokentypeDecoderConfiguration.read(reader);
        }
        return null;
    }

}

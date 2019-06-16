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

package es.gencom.mpegg.CABAC.encoder;

import es.gencom.mpegg.CABAC.binarization.AbstractBinarization;
import es.gencom.mpegg.CABAC.configuration.CodecConfiguration;
import es.gencom.mpegg.coder.compression.transform.SAIS;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class CABAC_BWTTransformEncoder extends CABAC_NoTransformEncoder {
    
    private int size;
    private byte[] data;
    private int first_char_index;
    
    public CABAC_BWTTransformEncoder(
            final MPEGWriter writer,
            final CodecConfiguration configuration,
            final AbstractBinarization binarization,
            final boolean adaptive_mode_flag,
            final InputStream ref_source) 
                            throws IOException {

        super(writer, configuration, binarization, adaptive_mode_flag, ref_source);
        
        data = new byte[0xFFFF]; // default size = 64K
    }

    /**
     * EOF-less version of BWT requires the position of the first character
     * (where sa[position] == 0). The value is known after the transformation 
     * is completed.
     * 
     * @return the first character index
     */
    public int getFirstCharIndex() {
        return first_char_index;
    }
    
    @Override
    public void write(final long value) throws IOException {
        if (size == data.length) {
            data = Arrays.copyOf(data, size << 1);
        }
        data[size++] = (byte)(value & 0xFF);
    }

    @Override
    public void close() throws IOException {
        final int[] sa = SAIS.suffix(ByteBuffer.wrap(data));

        first_char_index = -1;
        for (int i = 1; i < sa.length; i++) {
            if (sa[i - 1] == 0) {
                first_char_index = i;
            }
            final byte sym = first_char_index >= 0 ? data[sa[i] - 1] : data[sa[i - 1] - 1];            
            super.write(sym & 0xFF);
        }
        super.close();
    }
}

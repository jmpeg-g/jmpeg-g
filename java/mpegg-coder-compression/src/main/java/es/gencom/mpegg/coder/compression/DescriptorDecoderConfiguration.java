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

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.InputStream;

/**
 * An abstract class all Descritor Decoder Configurations must inherit from.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 * 
 * @param <T>
 * @param <U>
 */

public interface DescriptorDecoderConfiguration<T extends DescriptorDecoder, U extends DescriptorEncoder>
                                                    extends DecoderConfiguration {
    

    /**
     * Get a DescriptorDecoder initialized with this configuration parameters.
     * 
     * @param reader
     * @param descriptor_id descriptor for which the DescriptorDecoder is created
     * @param descriptor_subsequence_id
     * @param alphabet_id
     * 
     * @return configured DescriptorDecoder
     */
    default T getDescriptorDecoder(
            final MPEGReader reader, 
            final DESCRIPTOR_ID descriptor_id,
            final int descriptor_subsequence_id,
            final ALPHABET_ID alphabet_id) {

        return getDescriptorDecoder(
                reader, descriptor_id, descriptor_subsequence_id, alphabet_id, true, null); 
    }

    default T getDescriptorDecoder(
            final MPEGReader reader, 
            final DESCRIPTOR_ID descriptor_id,
            final int descriptor_subsequence_id,
            final ALPHABET_ID alphabet_id,
            final InputStream ref_source) {

        return getDescriptorDecoder(
                reader, descriptor_id, descriptor_subsequence_id, alphabet_id, true, ref_source); 
    }

    /**
     * Get a DescriptorDecoder initialized with this configuration parameters.
     * 
     * @param reader
     * @param descriptor_id descriptor for which the DescriptorDecoder is created
     * @param descriptor_subsequence_id
     * @param alphabet_id
     * @param primary_alignments_only ALIGNED_CONTENT && !multiple_alignments_flag
     * @param ref_source
     * 
     * @return configured DescriptorDecoder
     */
    T getDescriptorDecoder(
            MPEGReader reader,
            DESCRIPTOR_ID descriptor_id,
            int descriptor_subsequence_id,
            ALPHABET_ID alphabet_id,
            boolean primary_alignments_only,
            InputStream ref_source);

    default U getDescriptorEncoder(
            MPEGWriter writer, 
            DESCRIPTOR_ID descriptor_id,
            int descriptor_subsequence_id,
            ALPHABET_ID alphabet_id) {

        return getDescriptorEncoder(
                writer, descriptor_id, descriptor_subsequence_id, alphabet_id, true, null); 
    }


    default U getDescriptorEncoder(
            final MPEGWriter writer, 
            final DESCRIPTOR_ID descriptor_id,
            final int descriptor_subsequence_id,
            final ALPHABET_ID alphabet_id,
            final InputStream ref_source) {

        return getDescriptorEncoder(
                writer, descriptor_id, descriptor_subsequence_id, alphabet_id, true, ref_source); 
    }
    
    U getDescriptorEncoder(
            MPEGWriter writer,
            DESCRIPTOR_ID descriptor_id,
            int descriptor_subsequence_id,
            ALPHABET_ID alphabet_id,
            boolean primary_alignments_only,
            InputStream ref_source);

}

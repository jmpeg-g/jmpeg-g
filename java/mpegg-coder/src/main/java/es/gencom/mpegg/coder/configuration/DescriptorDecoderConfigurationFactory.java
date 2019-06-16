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

package es.gencom.mpegg.coder.configuration;

import es.gencom.mpegg.CABAC.configuration.*;
import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_DescriptorSubsequenceConfiguration;
import es.gencom.mpegg.CABAC.configuration.subseq.CABAC_SubsequenceCoderConfiguration;
import es.gencom.mpegg.coder.compression.*;
import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;

/**
 * The wrapper class used to encode / decode DecoderConfiguration(s).
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DescriptorDecoderConfigurationFactory {
    
    public final AbstractDecoderConfiguration decoder_configuraion;
    
    public DescriptorDecoderConfigurationFactory(final AbstractDecoderConfiguration decoder_configuraion) {
        this.decoder_configuraion = decoder_configuraion;
    }
    
    /**
     * Write DecoderConfiguration into the mpeg stream.
     * 
     * @param writer
     * @throws IOException 
     */
    public void write(final MPEGWriter writer) throws IOException {
        decoder_configuraion.encoding_mode_id.write(writer);
        decoder_configuraion.write(writer);
    }

    /**
     * Temporal solution.The 'dec_cfg_flag' should be after the 'encoding_mode_id'.This way read() method would return either default configuration or read one from the stream.
     *
     * @param encoding_mode_id
     * @param descriptor_id
     * @param numCodebooks
     * @param alphabet_id
     * @param primary_alignments_only
     * @return appropriate decoder configuration (CABAC)
     * 
     * @throws IOException 
     */
    public static DescriptorDecoderConfiguration getDefaultDecoderConfiguration(
            ENCODING_MODE_ID encoding_mode_id,
            final DESCRIPTOR_ID descriptor_id,
            final int numCodebooks,
            final ALPHABET_ID alphabet_id,
            final boolean primary_alignments_only
    ) throws IOException {
        switch(encoding_mode_id) {
            case CABAC: return getCABACDecoderConfiguration(
                    descriptor_id,
                    alphabet_id,
                    primary_alignments_only,
                    numCodebooks
            );
        }
        return null;
        
    }


    private static int getNumberSubsequences(
            DESCRIPTOR_ID descriptor_id,
            int numCodeBooks
    ){
        switch (descriptor_id){
            case POS:
                return 2;
            case RCOMP:
                return 1;
            case FLAGS:
                return 3;
            case MMPOS:
                return 2;
            case MMTYPE:
                return 3;
            case CLIPS:
                return 4;
            case UREADS:
                return 1;
            case RLEN:
                return 1;
            case PAIR:
                return 8;
            case MSCORE:
                return 1;
            case MMAP:
                return 5;
            case MSAR:
                return 2;
            case RTYPE:
                return 1;
            case RGROUP:
                return 1;
            case QV:
                return 2 + numCodeBooks;
            case RNAME:
                return 2;
            case RFTP:
                return 1;
            case RFTT:
                return 1;
            default:
                throw new IllegalArgumentException();
        }
    };

    private static CABAC_DescriptorDecoderConfiguration getCABACDecoderConfiguration(
            final DESCRIPTOR_ID descriptor_id,
            final ALPHABET_ID alphabet_id,
            final boolean primary_alignments_only,
            int numCodebooks
    ){
        int numberSubsequence = getNumberSubsequences(descriptor_id, numCodebooks);

        CABAC_DescriptorSubsequenceConfiguration[] subsequenceConfigurations = new
                CABAC_DescriptorSubsequenceConfiguration[numberSubsequence];

        for(int subsequence_i=0; subsequence_i < numberSubsequence; subsequence_i++){
            subsequenceConfigurations[subsequence_i] = new CABAC_DescriptorSubsequenceConfiguration(
                (short) subsequence_i,
                new CABAC_SubsequenceCoderConfiguration(
                    new CABAC_SubsequenceEncodingConfiguration(
                        descriptor_id,
                        (short) subsequence_i,
                        alphabet_id,
                        primary_alignments_only
                    )
                )
            );
        }

        return new CABAC_DescriptorDecoderConfiguration(subsequenceConfigurations);
    }

    private static CABAC_DescriptorDecoderConfiguration getCABACDecoderConfigurationCorrect(
            final DESCRIPTOR_ID descriptor_id,
            final ALPHABET_ID alphabet_id,
            final boolean primary_alignments_only,
            final int numCodebooks) {

        int numberSubsequence = getNumberSubsequences(descriptor_id, numCodebooks);

        CABAC_DescriptorSubsequenceConfiguration[] subsequenceConfigurations = new
                CABAC_DescriptorSubsequenceConfiguration[numberSubsequence];

        for(int subsequence_i=0; subsequence_i < numberSubsequence; subsequence_i++){
            subsequenceConfigurations[subsequence_i] = new CABAC_DescriptorSubsequenceConfiguration(
                    (short) subsequence_i,
                    new CABAC_SubsequenceCoderConfiguration(
                            new CABAC_SubsequenceEncodingConfiguration(
                                    descriptor_id,
                                    (short) subsequence_i,
                                    alphabet_id,
                                    primary_alignments_only
                            )
                    )
            );
        }

        return new CABAC_DescriptorDecoderConfiguration(subsequenceConfigurations);
    }
    
    public static AbstractDecoderConfiguration read(final MPEGReader reader) throws IOException {
        
        final ENCODING_MODE_ID encoding_mode_id = ENCODING_MODE_ID.read(reader);
        
        switch(encoding_mode_id) {
            case CABAC: return CABAC_DescriptorDecoderConfiguration.read(reader);
        }
        return null;
    }
}

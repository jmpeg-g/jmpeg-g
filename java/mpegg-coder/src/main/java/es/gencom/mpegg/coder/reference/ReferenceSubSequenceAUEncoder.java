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

package es.gencom.mpegg.coder.reference;

import es.gencom.mpegg.CABAC.configuration.CABAC_DescriptorDecoderConfiguration;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;
import es.gencom.mpegg.coder.dataunits.DataUnits;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorEncoder;
import es.gencom.mpegg.io.MSBitOutputArray;
import es.gencom.mpegg.io.Payload;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ReferenceSubSequenceAUEncoder {
    private static final int MAX_RLEN = 100;

    private static DataUnitAccessUnit encodeSubsequenceWithClassU_rlen0(
            DataUnitParameters dataUnitParameters,
            long access_unit_ID,
            long ref_start_position,
            long ref_end_position,
            SequenceIdentifier ref_sequence_id,
            byte[] subsequence,
            DataUnits dataUnits) throws IOException {

        DataUnitAccessUnit.Block blocks[] = new DataUnitAccessUnit.Block[2];

        int rlenSymbolsNeeded = (int) Math.ceil((double)subsequence.length / MAX_RLEN);

        MSBitOutputArray outRlen = new MSBitOutputArray();
        outRlen.writeInt(Integer.reverseBytes(rlenSymbolsNeeded));
        CABAC_DescriptorDecoderConfiguration conf = new CABAC_DescriptorDecoderConfiguration();
        DescriptorEncoder encoderRlen
                = conf.getDescriptorEncoder(outRlen, DESCRIPTOR_ID.RLEN, 0, ALPHABET_ID.DNA);

        MSBitOutputArray outUreads = new MSBitOutputArray();
        outUreads.writeInt(Integer.reverseBytes(subsequence.length));
        DescriptorEncoder encoderUreads
                = conf.getDescriptorEncoder(outUreads, DESCRIPTOR_ID.UREADS, 0, ALPHABET_ID.DNA);

        int toEncode = subsequence.length;
        int encoded = 0;

        while (toEncode > 0) {
            int sizeCurrentEncoding = Math.min(MAX_RLEN, toEncode);

            encoderRlen.write(sizeCurrentEncoding);
            for(int i=0; i<sizeCurrentEncoding; i++) {
                encoderUreads.write(subsequence[encoded + i]);
            }

            toEncode -= sizeCurrentEncoding;
            encoded += sizeCurrentEncoding;
        }

        encoderRlen.close();
        encoderUreads.close();

        outRlen.flush();
        outUreads.flush();


        ByteBuffer rlenByteBuffer = outRlen.toByteBuffer();
        ByteBuffer uReadsByteBuffer = outUreads.toByteBuffer();
        rlenByteBuffer.rewind();
        uReadsByteBuffer.rewind();

        blocks[0] = new DataUnitAccessUnit.Block(DESCRIPTOR_ID.RLEN, new Payload(rlenByteBuffer));
        blocks[1] = new DataUnitAccessUnit.Block(DESCRIPTOR_ID.UREADS, new Payload(uReadsByteBuffer));

        SequenceIdentifier sequence_ID = new SequenceIdentifier((short) 0);
        long au_start_position = 0;
        long au_end_position = 0;
        long extended_start_position = 0;
        long extended_end_position = 0;

        long read_count = 0;
        DataUnitAccessUnit.DataUnitAccessUnitHeader accessUnitHeader = new DataUnitAccessUnit.DataUnitAccessUnitHeader(
            access_unit_ID,
            (short)blocks.length,
            dataUnitParameters.getParameter_set_ID(),
            DATA_CLASS.CLASS_U,
            read_count,
            0,
            0,
            ref_sequence_id,
            ref_start_position,
            ref_end_position,
            sequence_ID,
            au_start_position,
            au_end_position,
            extended_start_position,
            extended_end_position
        );
        return new DataUnitAccessUnit(accessUnitHeader, dataUnits, blocks);
    }

    public static DataUnitAccessUnit encodeSubsequenceWithClassU(
        DataUnitParameters dataUnitParameters,
        long access_unit_ID,
        long ref_start_position,
        long ref_end_position,
        SequenceIdentifier ref_sequence_id,
        byte[] subsequence,
        DataUnits dataUnits
    ) throws IOException {
        if(dataUnitParameters.getReadLength() == 0){
            return encodeSubsequenceWithClassU_rlen0(
                    dataUnitParameters,
                    access_unit_ID,
                    ref_start_position,
                    ref_end_position,
                    ref_sequence_id,
                    subsequence,
                    dataUnits
            );
        }else{
            throw new UnsupportedOperationException();
        }
    }
}

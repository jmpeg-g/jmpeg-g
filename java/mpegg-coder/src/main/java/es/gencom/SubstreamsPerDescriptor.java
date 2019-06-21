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

package es.gencom;

import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.configuration.EncodingParameters;

import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SubstreamsPerDescriptor {
    private final static Map<DESCRIPTOR_ID, Byte> SUBSTREAMS_PER_DESCRIPTOR =
        Stream.of(
        new SimpleEntry<>(DESCRIPTOR_ID.POS, (byte) 2),
        new SimpleEntry<>(DESCRIPTOR_ID.RCOMP, (byte) 1),
        new SimpleEntry<>(DESCRIPTOR_ID.FLAGS, (byte) 3),
        new SimpleEntry<>(DESCRIPTOR_ID.MMPOS, (byte) 2),
        new SimpleEntry<>(DESCRIPTOR_ID.MMTYPE, (byte) 3),
        new SimpleEntry<>(DESCRIPTOR_ID.CLIPS, (byte) 4),
        new SimpleEntry<>(DESCRIPTOR_ID.UREADS, (byte) 1),
        new SimpleEntry<>(DESCRIPTOR_ID.RLEN, (byte) 1),
        new SimpleEntry<>(DESCRIPTOR_ID.PAIR, (byte) 8),
        new SimpleEntry<>(DESCRIPTOR_ID.MSCORE, (byte)1),
        new SimpleEntry<>(DESCRIPTOR_ID.MMAP, (byte)5),
        new SimpleEntry<>(DESCRIPTOR_ID.MSAR, (byte)2),
        new SimpleEntry<>(DESCRIPTOR_ID.RTYPE, (byte)1),
        new SimpleEntry<>(DESCRIPTOR_ID.RGROUP, (byte)1),
        new SimpleEntry<>(DESCRIPTOR_ID.RNAME, (byte)2),
        new SimpleEntry<>(DESCRIPTOR_ID.RFTP, (byte)1),
        new SimpleEntry<>(DESCRIPTOR_ID.RFTT, (byte)1)
    ).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    public static byte getNumberSubstreams(
            DESCRIPTOR_ID descriptorIdentifier,
            EncodingParameters encodingParameters,
            DATA_CLASS dataClass
    ){
        if(descriptorIdentifier == DESCRIPTOR_ID.QV){
            return (byte) (encodingParameters.getQualityValueParameterSet(dataClass).getNumberQualityBooks()+2);
        }else {
            return SUBSTREAMS_PER_DESCRIPTOR.getOrDefault(descriptorIdentifier, (byte) 1);
        }
    }
}

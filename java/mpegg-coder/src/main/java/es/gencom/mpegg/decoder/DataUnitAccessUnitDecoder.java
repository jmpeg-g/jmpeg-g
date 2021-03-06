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

package es.gencom.mpegg.decoder;

import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;
import es.gencom.mpegg.coder.quality.AbstractQualityValueParameterSet;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.tokens.TokenValuesDecoder;
import es.gencom.mpegg.decoder.descriptors.streams.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataUnitAccessUnitDecoder {
    public static List<Record> decode(
            DataUnitAccessUnit dataUnitToDecode,
            AbstractSequencesSource sequencesSource

    ) throws IOException {
        DATA_CLASS dataUnitClass = dataUnitToDecode.getAUType();

        long auId = dataUnitToDecode.getHeader().getAccess_unit_ID();


        DataUnitParameters dataUnitParameters = dataUnitToDecode.getParameter();
        AbstractQualityValueParameterSet qualityValueParameterSet =
                dataUnitParameters.getQualityValueParameterSet(dataUnitClass);

        GenomicPosition genomicPosition = new GenomicPosition(
                dataUnitToDecode.getHeader().getSequence_ID(),
                dataUnitToDecode.getHeader().getAu_start_position()
        );

        AbstractAccessUnitDecoder abstractAccessUnitDecoder;


        short[][][] tokensReadIdentifiers = null;
        DataUnitAccessUnit.Block rnameBlock = dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.RNAME);
        if(rnameBlock != null){
            tokensReadIdentifiers = TokenValuesDecoder.decodeTokenValues(
                    rnameBlock.getDescriptorSpecificData(),
                    dataUnitParameters.getEncodingParameters(),
                    DESCRIPTOR_ID.RNAME,
                    dataUnitClass
            );
        }


        if(dataUnitClass.ID < DATA_CLASS.CLASS_HM.ID) {
            abstractAccessUnitDecoder = new MappedAccessUnitDecoder(
                    auId,
                    genomicPosition,
                    new PosStream(
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.POS),
                            genomicPosition,
                            dataUnitClass,
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new PairStream(
                            dataUnitClass,
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.PAIR),
                            dataUnitParameters.getNumberTemplateSegments(),
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new MMapStream(
                            dataUnitClass,
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.MMAP),
                            dataUnitParameters.isMultiple_alignments_flag(),
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new RCompStream(
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.RCOMP),
                            dataUnitClass,
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new FlagsStream(
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.FLAGS),
                            dataUnitClass,
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new RlenStream(
                            dataUnitClass,
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.RLEN),
                            (int)dataUnitParameters.getReadLength(),
                            dataUnitParameters.isSplicedReadsFlag(),
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new MMposStream(
                            dataUnitClass,
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.MMPOS),
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new MMTypeStream(
                            dataUnitClass,
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.MMTYPE),
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new ClipsStream(
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.CLIPS),
                            dataUnitClass,
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new QualityStream(
                            dataUnitParameters.getEncodingParameters(),
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.QV),
                            dataUnitClass
                    ),
                    sequencesSource,
                    tokensReadIdentifiers,
                    dataUnitParameters.getEncodingParameters().getAlphabetId(),
                    qualityValueParameterSet
            );
        }else if(dataUnitClass == DATA_CLASS.CLASS_HM){
            abstractAccessUnitDecoder = new HalfMappedAccessUnitDecoder(
                    auId,
                    dataUnitParameters.getEncodingParameters().getAlphabetId(),
                    genomicPosition,
                    sequencesSource,
                    new PosStream(
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.POS),
                            genomicPosition,
                            dataUnitClass,
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new RlenStream(
                            dataUnitClass,
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.RLEN),
                            (int)dataUnitParameters.getReadLength(),
                            dataUnitParameters.isSplicedReadsFlag(),
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new RCompStream(
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.RCOMP),
                            dataUnitClass,
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new MMposStream(
                            dataUnitClass,
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.MMPOS),
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new MMTypeStream(
                            dataUnitClass,
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.MMTYPE),
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new ClipsStream(
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.CLIPS),
                            dataUnitClass,
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new UReadsStream(
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.UREADS),
                            dataUnitClass,
                            dataUnitParameters.getEncodingParameters()
                    ),
                    new QualityStream(
                            dataUnitParameters.getEncodingParameters(),
                            dataUnitToDecode.getBlockByDescriptorId(DESCRIPTOR_ID.QV),
                            dataUnitClass
                    ),
                    tokensReadIdentifiers
            );
        }else{
            throw new IllegalArgumentException();
        }

        List<Record> result = new ArrayList<>(10000);
        while (abstractAccessUnitDecoder.hasNext()){
            try {
                result.add(abstractAccessUnitDecoder.getRecord());
            }catch (Exception e){
                e.printStackTrace();
                break;
            }
        }

        return result;
    }
}

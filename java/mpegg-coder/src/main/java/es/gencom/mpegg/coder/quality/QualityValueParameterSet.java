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

package es.gencom.mpegg.coder.quality;

import es.gencom.mpegg.coder.configuration.Parameter_set_qvps_format;

public class QualityValueParameterSet extends AbstractQualityValueParameterSet {

    private static class QualityBook extends AbstractQualityBook{
        private final short[] entries;

        QualityBook(short[] entries) {
            this.entries = entries;
        }


        @Override
        public byte getNumberEntries() {
            return (byte)entries.length;
        }

        @Override
        public short encode(short qualitySAM) {
            if(qualitySAM < 33 || qualitySAM > 126){
                throw new IllegalArgumentException();
            }
            for(byte index = 1; index<entries.length; index++){
                if(qualitySAM < entries[index]){
                    return (byte) (index-1);
                }
            }
            return (byte)(entries.length-1);
        }

        @Override
        public short decode(short encodedQuality) {
            return entries[encodedQuality];
        }
    }


    private final AbstractQualityBook[] qualityBooks;

    public QualityValueParameterSet(Parameter_set_qvps_format parameter_set_qvps_format){
        qualityBooks = new AbstractQualityBook[parameter_set_qvps_format.getNumCodebooks()];
        for(byte book_i = 0; book_i < qualityBooks.length; book_i++){
            qualityBooks[book_i] = new QualityBook(parameter_set_qvps_format.getEntries(book_i));
        }
    }

    @Override
    public int getNumberQualityBooks() {
        return 0;
    }

    @Override
    public AbstractQualityBook getQualityBook(int qualityBookIndex) throws IndexOutOfBoundsException {
        return null;
    }
}

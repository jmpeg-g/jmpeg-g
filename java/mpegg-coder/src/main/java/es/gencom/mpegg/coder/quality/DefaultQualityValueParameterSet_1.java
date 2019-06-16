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

public class DefaultQualityValueParameterSet_1 extends AbstractQualityValueParameterSet{
    private static AbstractQualityBook qualityBook
            = new Default_qvps_ID_1_codebook();
    @Override
    public int getNumberQualityBooks() {
        return 1;
    }

    @Override
    public AbstractQualityBook getQualityBook(int qualityBookIndex) throws IndexOutOfBoundsException {
        if(qualityBookIndex != 0){
            throw new IndexOutOfBoundsException();
        }
        return qualityBook;
    }

    private static class Default_qvps_ID_1_codebook extends AbstractQualityBook{
        @Override
        public byte getNumberEntries() {
            return 8;
        }

        @Override
        public short encode(short qualitySAM) {
            if(qualitySAM < 33 || qualitySAM > 126){
                throw new IllegalArgumentException();
            }
            if (qualitySAM < 41){
                return 0;
            }else if (qualitySAM < 46){
                return 1;
            }else if (qualitySAM < 51){
                return 2;
            }else if (qualitySAM < 56){
                return 3;
            }else if (qualitySAM < 61){
                return 4;
            }else if (qualitySAM < 66){
                return 5;
            }else if (qualitySAM < 74){
                return 6;
            }else{
                return 7;
            }
        }

        @Override
        public short decode(short encodedQuality) {
            if(encodedQuality < 0 || encodedQuality > 7){
                throw new IllegalArgumentException();
            }
            if(encodedQuality == 0){
                return 33;
            }else if (encodedQuality == 1){
                return 41;
            }else if (encodedQuality == 2){
                return 46;
            }else if (encodedQuality == 3){
                return 51;
            }else if (encodedQuality == 4){
                return 56;
            }else if (encodedQuality == 5){
                return 61;
            }else if (encodedQuality == 6){
                return 66;
            }else {
                return 74;
            }
        }
    }
}

/*
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

import es.gencom.mpegg.coder.configuration.QualityValuesParameterSet;

public class QualityValueParameterSet extends AbstractQualityValueParameterSet {

    private final AbstractQualityBook[] qualityBooks;

    public QualityValueParameterSet(QualityValuesParameterSet parameter_set_qvps_format){
        qualityBooks = new AbstractQualityBook[parameter_set_qvps_format.getNumCodebooks()];
        for(byte book_i = 0; book_i < qualityBooks.length; book_i++){
            qualityBooks[book_i] = new QualityBookParameterSet(parameter_set_qvps_format.getEntries(book_i));
        }
    }

    @Override
    public int getNumberQualityBooks() {
        return qualityBooks.length;
    }

    @Override
    public AbstractQualityBook getQualityBook(int qualityBookIndex) throws IndexOutOfBoundsException {
        return qualityBooks[qualityBookIndex];
    }
}

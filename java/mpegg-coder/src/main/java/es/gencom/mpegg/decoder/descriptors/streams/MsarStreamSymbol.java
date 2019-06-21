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

package es.gencom.mpegg.decoder.descriptors.streams;

public class MsarStreamSymbol {
    final private String value;

    public MsarStreamSymbol(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    private int countSplices() {
        int count = 0;
        for (int i = 0, n = value.length(); i < n; i++) {
            final char ch = value.charAt(i);
            if (ch == '/') {
                count++;
            }
        }
        return count;
    }

    public boolean equalToPrimary(){
        return value.equals("*");
    }

    public int countSplices(int primarySplices){
        if(equalToPrimary()){
            return primarySplices;
        }else{
            return countSplices();
        }
    }
}

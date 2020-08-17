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

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;

import java.io.IOException;

/**
 * <p>
 * Quality Values Parameter Set (qvps) class (ISO/IEC DIS 23092-2 7.3.2.2)
 * </p>
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */
public class QualityValuesParameterSet {

    private short[][] qv_recon;

    public QualityValuesParameterSet() {
        qv_recon = new short[0][];
    }

    public QualityValuesParameterSet(final short[][] qv_recon) {
        if(qv_recon.length > 16) {
            throw new IllegalArgumentException("qv_recon cannot have more than 15 codebooks.");
        }

        for(int qv_codebook_i = 0; qv_codebook_i < qv_recon.length; qv_codebook_i++) {
            if(qv_recon[qv_codebook_i].length > 255){
                throw new IllegalArgumentException("qv codebook cannot have more than 255 entries.");
            }
        }
        this.qv_recon = qv_recon;
    }

    public QualityValuesParameterSet(byte[] qualityHistogram) {
        qv_recon = new short[1][qualityHistogram.length];
        for(int i=0; i<qualityHistogram.length; i++){
            qv_recon[0][i] = qualityHistogram[i];
        }
    }

    public void write(final MPEGWriter writer) throws IOException {
        writer.writeBits(qv_recon.length, 4);
        for(int qv_codebook_i=0; qv_codebook_i < qv_recon.length; qv_codebook_i++){
            writer.writeBits(qv_recon[qv_codebook_i].length, 8);
            for(int qv_entry_i = 0; qv_entry_i < qv_recon[qv_codebook_i].length; qv_entry_i++){
                writer.writeBits(qv_recon[qv_codebook_i][qv_entry_i], 8);
            }
        }
    }

    public void read(final MPEGReader reader) throws IOException {
        final int qv_num_codebooks_total = (int)reader.readBits(4);
        qv_recon = new short[qv_num_codebooks_total][];
        for(int qv_codebook_i=0; qv_codebook_i < qv_recon.length; qv_codebook_i++){
            final int num_codebook_entries = (int)reader.readBits(8);
            qv_recon[qv_codebook_i] = new short[num_codebook_entries];
            for(int qv_entry_i = 0; qv_entry_i < qv_recon[qv_codebook_i].length; qv_entry_i++){
                qv_recon[qv_codebook_i][qv_entry_i] = reader.readUnsignedByte();
            }
        }
    }

    public long size() {
        return (long) Math.ceil((double)sizeInBits()/8);
    }

    public byte getNumCodebooks() {
        return (byte) qv_recon.length;
    }

    public short getNumEntries(final byte codebook_i) {
        return (short) qv_recon[codebook_i].length;
    }

    public short[] getEntries(final byte codebook_i) {
        return qv_recon[codebook_i];
    }

    public long sizeInBits() {
        long resultInBits = 0;
        resultInBits += 4;
        for(int qv_codebook_i = 0; qv_codebook_i < qv_recon.length; qv_codebook_i++) {
            resultInBits += 8;
            resultInBits += qv_recon[qv_codebook_i].length * 8;
        }
        return resultInBits;
    }
}

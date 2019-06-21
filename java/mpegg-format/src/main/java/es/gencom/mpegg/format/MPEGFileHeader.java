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

package es.gencom.mpegg.format;

import es.gencom.mpegg.io.MPEGReader;
import es.gencom.mpegg.io.MPEGWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MPEGFileHeader extends GenInfo<MPEGFileHeader> {
    
    public final static String KEY = "flhd";
    
    private String major_brand;             // c(6)
    private String minor_version;           // c(4)
    private List<String> compatible_brands; // c(4)[]
    
    public MPEGFileHeader() {
        this("MPEG-G", "1900", new ArrayList<>());
    }

    public MPEGFileHeader(final List<String> compatible_brands){
        this("MPEG-G", "1900", compatible_brands);
    }

    private MPEGFileHeader(final String major_brand, 
                           final String minor_version, 
                           final List<String> compatible_brands) {
        super(KEY);

        this.major_brand = major_brand;
        this.minor_version = minor_version;
        this.compatible_brands = compatible_brands;
    }
    
    public String getMajorBrand() {
        return major_brand;
    }

    public String getMinorVersion() {
        return minor_version;
    }
    
    public List<String> getCompatibleBrands() {
        return compatible_brands;
    }
    
    public void addCompatibleBrand(final String compatibleBrand) {
        compatible_brands.add(compatibleBrand);
    }
    
    @Override
    protected long size() {
        return 10 + 4 * compatible_brands.size();
    }

    @Override
    public void write(MPEGWriter writer) throws IOException {
        writer.writeString(major_brand);
        writer.writeString(minor_version);
        
        for (String s : compatible_brands) {
            writer.writeString(s);
        }
    }

    @Override
    public MPEGFileHeader read(final MPEGReader reader, final long size) throws IOException {
        major_brand = new String(reader.readChars(6));
        minor_version = new String(reader.readChars(4));
        
        compatible_brands = new ArrayList<>();

        if((size - 10)%4 != 0){
            throw new IllegalArgumentException();
        }
        
        int num_compatible_brands = (int) (size - 10)/4;
        while (num_compatible_brands-- > 0) {
            compatible_brands.add(new String(reader.readChars(4)));
        }
        
        return this;
    }

    public static MPEGFileHeader readAndCreate(final ReadableByteChannel channel) throws IOException {
        return read(Header.read(channel), channel);
    }
    
    public static MPEGFileHeader read(final Header header, final ReadableByteChannel channel) throws IOException {
        if (!MPEGFileHeader.KEY.equals(header.key)) {
            throw new IOException(String.format("Invalid MPEGFileHeader KEY: %s (must be %s)", header.key, MPEGFileHeader.KEY));
        }
        
        final byte[] arr = new byte[(int)header.length - Header.SIZE];
        final ByteBuffer buf = ByteBuffer.wrap(arr).order(BYTE_ORDER);
        while (buf.hasRemaining() && channel.read(buf) >= 0){}

        final MPEGFileHeader file_header = new MPEGFileHeader();
        file_header.major_brand = new String(arr, 0, 6, "UTF-8");
        file_header.minor_version = new String(arr, 6, 4, "UTF-8");
        
        file_header.compatible_brands = new ArrayList<>();
        for (int i = 10, n = arr.length; i < n; i += 4) {
            file_header.compatible_brands.add(new String(arr, i, 4, "UTF-8"));
        }

        return file_header;
    }
}

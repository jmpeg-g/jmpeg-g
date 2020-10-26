/**
 * *****************************************************************************
 * Copyright (C) 2015 Spanish National Bioinformatics Institute (INB) and
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

package es.gencom.integration.bam;

import es.gencom.integration.io.DataReaderHelper;
import es.gencom.integration.io.DataWriterHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;

/**
 * @author Dmitry Repchevsky
 */

public class BAMHeader {

    public final static byte MAGIC[] = {'B', 'A', 'M', 0x01};

    public final String text;
    public final Reference[] refs;

    public BAMHeader(final String text, final Reference[] refs) {
        this.text = text;
        this.refs = refs;
    }
            
    public BAMHeader(final InputStream in) throws IOException, DataFormatException {
        if (in.read() != MAGIC[0] ||
            in.read() != MAGIC[1] ||
            in.read() != MAGIC[2] ||
            in.read() != MAGIC[3]) {
            throw new DataFormatException("not BAM file");
        }

        final int l_text = (int)DataReaderHelper.readUnsignedInt(in);
        final byte[] txt = new byte[l_text];
        for (int i = 0, n; i < l_text && (n = in.read(txt, i, l_text - i)) >= 0; i += n) {}

        text = new String(txt, StandardCharsets.US_ASCII);
        
        final int n_ref = (int)DataReaderHelper.readUnsignedInt(in);
        refs = new Reference[n_ref];
        
        for (int i = 0; i < n_ref; i++) {
            final int l_name = (int)DataReaderHelper.readUnsignedInt(in);
            final byte[] name = new byte[l_name];
            in.read(name);
            
            final int l_ref = (int)DataReaderHelper.readUnsignedInt(in);
            
            refs[i] = new Reference(
                    new String(name, 0, l_name - 1, StandardCharsets.US_ASCII), l_ref);
        }
    }
    
    public void write(final OutputStream out) throws IOException {
        
        out.write(MAGIC[0]);
        out.write(MAGIC[1]);
        out.write(MAGIC[2]);
        out.write(MAGIC[3]);

        final byte[] txt = text.getBytes(StandardCharsets.US_ASCII);
        DataWriterHelper.writeUnsignedInt(out, txt.length);
        out.write(txt);
        
        DataWriterHelper.writeUnsignedInt(out, refs.length);
        for (int i = 0; i < refs.length; i++) {
            final byte[] name = refs[i].name.getBytes(StandardCharsets.US_ASCII);
            DataWriterHelper.writeUnsignedInt(out, name.length + 1);
            out.write(name);
            out.write(0); // NUL-terminated
            DataWriterHelper.writeUnsignedInt(out, refs[i].length);
        }
    }
    
    public static class Reference {
        public final String name;
        public final int length;
        public Reference(final String name, final int length) {
            this.name = name;
            this.length = length;
        }
    }

    public Reference getReference(int ref_index){
        return refs[ref_index];
    }
}


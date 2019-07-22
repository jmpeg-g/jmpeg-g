package es.gencom.mpegg.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MSBitOutputArrayTest {
   
    public final static String DATA = "Barcelona es bona si la bossa sona";
    
    @Test
    public void test() throws IOException {
        
        final ByteBuffer buf = ByteBuffer.wrap(DATA.getBytes());
        final MPEGReader in = new Payload(buf);
        final MSBitOutputArray out = new MSBitOutputArray();
        
        for (int i = DATA.length() * 8, read; i > 0; i -= read) {
            read = Math.min(i, 17);
            final long bit = in.readBits(read);
            out.writeBits(bit, read);
        }

        Assert.assertArrayEquals(DATA.getBytes(), Arrays.copyOf(out.toByteBuffer().array(), out.toByteBuffer().limit()));
    }
}

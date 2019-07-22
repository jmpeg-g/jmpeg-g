package es.gencom.mpegg.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MSBitBufferTest {
    public final static String DATA = "Barcelona es bona si la bossa sona";
    
    @Test
    public void test() throws IOException {
    
        // BitSet is LSF - reverse bits in all bytes.
        final byte[] arr = DATA.getBytes();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte)(Integer.reverse(arr[i]) >>> 24);
        }
        BitSet bitset = BitSet.valueOf(arr);
        
        MSBitBuffer bits = new MSBitBuffer(ByteBuffer.wrap(DATA.getBytes()));
        
        for (int i = 0, n = bitset.length(); i < n;) {
            final int read = Math.min(17, n - i);
            long bit = bits.readBits(read);
            for (int j = 1; j <= read; j++, i++) {
                Assert.assertEquals(bitset.get(i), ((bit >> (read - j)) & 1) == 1);
            }
        }
    }
}

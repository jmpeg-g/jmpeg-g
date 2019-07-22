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

package es.gencom.mpegg.CABAC.binarization;

import es.gencom.mpegg.io.Payload;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class BinarizationTest {
    
    @Test
    public void bi() throws IOException {
        final byte[][] data = { 
            //cLength, symVal, bitStr
            {1, 0, (byte)0b0_0000000},
            {1, 1, (byte)0b1_0000000},
            {3, 1, (byte)0b001_00000},
            {3, 2, (byte)0b010_00000},
            {2, 3, (byte)0b11_000000},
        };
        
        // encode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final BinaryCodingBinarization bi = new BinaryCodingBinarization(data[i][0]);

            bi.encode(buf, data[i][1]);
            buf.buf.flush();

            Assert.assertEquals(data[i][2], array[0]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][2]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final BinaryCodingBinarization bi = new BinaryCodingBinarization(data[i][0]);

            Assert.assertEquals(data[i][1], bi.decode(buf));
        }
    }
    
    @Test
    public void tu() throws IOException {
        final byte[][] data = {
            // cMax, symVal, bitStr
            {3, 0, (byte)0b0_0000000},
            {3, 1, (byte)0b10_000000},
            {3, 2, (byte)0b110_00000},
            {3, 3, (byte)0b111_00000}
        };
        
        // encode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final TruncatedUnaryBinarization tu = new TruncatedUnaryBinarization(data[i][0]);

            tu.encode(buf, data[i][1]);
            buf.buf.flush();

            Assert.assertEquals(data[i][2], array[0]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][2]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final TruncatedUnaryBinarization tu = new TruncatedUnaryBinarization(data[i][0]);

            Assert.assertEquals(data[i][1], tu.decode(buf));
        }
    }
    
    @Test
    public void eg() throws IOException {
        final byte[][] data = {
            // symVal, bitStr
            {0, (byte)0b1_0000000}, // '1'
            {1, (byte)0b010_00000}, // '010'
            {2, (byte)0b011_00000}, // '011'
            {3, (byte)0b00100_000}, // '00100'
            {4, (byte)0b00101_000}, // '00101'
            {5, (byte)0b00110_000}, // '00110'
            {6, (byte)0b00111_000}, // '00111'
            {7, (byte)0b0001000_0}, // '0001000'
            {8, (byte)0b0001001_0}, // '0001001'
            {9, (byte)0b0001010_0}, // '0001010'
        };
                
        // encode test
        for (int i = 0; i < data.length; i++) {     
            final byte[] array = {0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final ExponentialGolombBinarization eg = new ExponentialGolombBinarization();

            eg.encode(buf, data[i][0]);
            buf.buf.flush();

            Assert.assertEquals(data[i][1], array[0]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][1]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final ExponentialGolombBinarization eg = new ExponentialGolombBinarization();

            Assert.assertEquals(data[i][0], eg.decode(buf));
        }
    }
    
    //@Test
    public void seg() throws IOException {
        final byte[][] data = {
            // symVal, bitStr
            {0,  (byte)0b1_0000000}, // '1'
            {1,  (byte)0b010_00000}, // '010'
            {-1, (byte)0b011_00000}, // '011'
            {2,  (byte)0b00100_000}, // '00100'
            {-2, (byte)0b00101_000}, // '00101'
            {3,  (byte)0b00110_000}, // '00110'
            {-3, (byte)0b00111_000}  // '00111'
        };
                
        // encode test
        for (int i = 0; i < data.length; i++) {     
            final byte[] array = {0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final SignedExponentialGolombBinarization seg = new SignedExponentialGolombBinarization();

            seg.encode(buf, data[i][0]);
            buf.buf.flush();

            Assert.assertEquals(data[i][1], array[0]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][1]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final SignedExponentialGolombBinarization seg = new SignedExponentialGolombBinarization();

            Assert.assertEquals(data[i][0], seg.decode(buf));
        }
    }
    
    @Test
    public void teg() throws IOException {
        final byte[][] data = {
            // cTruncExpGolParam, symVal, bitStr
            {2, 0, (byte)0b0_0000000}, // '0'
            {2, 1, (byte)0b10_000000}, // '10'
            {2, 2, (byte)0b111_00000}, // '111'
            {2, 3, (byte)0b11010_000}, // '11010'
            {2, 4, (byte)0b11011_000}  // '11011'
        };
        
        // encode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final TruncatedExponentialGolombBinarization teg = new TruncatedExponentialGolombBinarization(data[i][0]);

            teg.encode(buf, data[i][1]);
            buf.buf.flush();

            Assert.assertEquals(data[i][2], array[0]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][2]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final TruncatedExponentialGolombBinarization teg = new TruncatedExponentialGolombBinarization(data[i][0]);

            Assert.assertEquals(data[i][1], teg.decode(buf));
        }
    }
    
    @Test
    public void steg() throws IOException {
        final byte[][] data = {
            // cSignedTruncExpGolParam, symVal, bitStr
            {2, -4, (byte)0b110111_00}, // '110111'
            {2, -3, (byte)0b110101_00}, // '110101'
            {2, -2, (byte)0b1111_0000}, // '1111'
            {2, -1, (byte)0b101_00000}, // '101'
            {2,  0, (byte)0b0_0000000}, // '0'
            {2,  1, (byte)0b100_00000}, // '100'
            {2,  2, (byte)0b1110_0000}, // '1110'
            {2,  3, (byte)0b110100_00}, // '110100'
            {2,  4, (byte)0b110110_00}, // '110110'
        };
        
        // encode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final SignedTruncatedExponentialGolombBinarization steg = new SignedTruncatedExponentialGolombBinarization(data[i][0]);

            steg.encode(buf, data[i][1]);
            buf.buf.flush();

            Assert.assertEquals(data[i][2], array[0]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][2]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final SignedTruncatedExponentialGolombBinarization steg = new SignedTruncatedExponentialGolombBinarization(data[i][0]);

            Assert.assertEquals(data[i][1], steg.decode(buf));
        }
    }
    
    @Test
    public void sutu() throws IOException {
        final byte[][] data = {
            // splitUnitSize, outputSymSize, symVal, bitStr
            {2, 8,  0, (byte)0b0000_0000, (byte)0b00000000}, // '0000'
            {2, 8,  1, (byte)0b10000_000, (byte)0b00000000}, // '10000'
            {2, 8,  3, (byte)0b111000_00, (byte)0b00000000}, // '111000'
            {2, 8, 15, (byte)0b111111_00, (byte)0b00000000}, // '11111100'
            {2, 8, 31, (byte)0b11111110, (byte)0b0_0000000}, // '111111100'
            {2, 8, 63, (byte)0b11111111, (byte)0b10_000000}, // '1111111110'
        };
        
        // encode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {0,0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final SplitUnitWiseTruncatedUnaryBinarization sutu = 
                    new SplitUnitWiseTruncatedUnaryBinarization(data[i][0], data[i][1]);

            sutu.encode(buf, data[i][2]);
            buf.buf.flush();

            Assert.assertEquals(data[i][3], array[0]);
            Assert.assertEquals(data[i][4], array[1]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][3], data[i][4]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final SplitUnitWiseTruncatedUnaryBinarization sutu = 
                    new SplitUnitWiseTruncatedUnaryBinarization(data[i][0], data[i][1]);
            
            Assert.assertEquals(data[i][2], sutu.decode(buf));
        }
    }
    
    @Test
    public void ssutu() throws IOException {
        final byte[][] data = {
            // splitUnitSize, outputSymSize, symVal, bitStr
            {2, 8, -3, (byte)0b1110001_0}, // '1110001'
            {2, 8, -1, (byte)0b100001_00}, // '100001'
            {2, 8,  0, (byte)0b0000_0000}, // '0000'
            {2, 8,  1, (byte)0b100000_00}, // '100000'
            {2, 8,  3, (byte)0b1110000_0}, // '1110000'
        };
        
        // encode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final SignedSplitUnitWiseTruncatedUnaryBinarization ssutu = 
                    new SignedSplitUnitWiseTruncatedUnaryBinarization(data[i][0], data[i][1]);

            ssutu.encode(buf, data[i][2]);
            buf.buf.flush();

            Assert.assertEquals(data[i][3], array[0]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][3]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final SignedSplitUnitWiseTruncatedUnaryBinarization ssutu = 
                    new SignedSplitUnitWiseTruncatedUnaryBinarization(data[i][0], data[i][1]);
            
            Assert.assertEquals(data[i][2], ssutu.decode(buf));
        }
    }

    @Test
    public void dtu() throws IOException {
        final byte[][] data = {
            // cMax, splitUnitSize, outputSymSize, symVal, bitStr
            {1, 2, 8, 0, (byte)0b0_0000000, (byte)0b00000000}, // '0'
            {1, 2, 8, 1, (byte)0b10000_000, (byte)0b00000000}, // '10000'
            {1, 2, 8, 3, (byte)0b1110000_0, (byte)0b00000000}, // '1110000'
            {1, 2, 8, 15,(byte)0b11101110, (byte)0b0_0000000}, // '111011100'
            {1, 2, 8, 31,(byte)0b11101111, (byte)0b00_000000}, // '1110111100'
            {1, 2, 8, 63,(byte)0b11101111, (byte)0b110_00000}  // '11101111110'
        };
        
        // encode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {0,0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final DoubleTruncatedUnaryBinarization dtu = 
                    new DoubleTruncatedUnaryBinarization(data[i][0], data[i][1], data[i][2]);

            dtu.encode(buf, data[i][3]);
            buf.buf.flush();

            Assert.assertEquals(data[i][4], array[0]);
            Assert.assertEquals(data[i][5], array[1]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][4], data[i][5]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final DoubleTruncatedUnaryBinarization dtu = 
                    new DoubleTruncatedUnaryBinarization(data[i][0], data[i][1], data[i][2]);
            
            Assert.assertEquals(data[i][3], dtu.decode(buf));
        }
    }

    @Test
    public void sdtu() throws IOException {
        final byte[][] data = {
            // cMax, splitUnitSize, outputSymSize, symVal, bitStr
            {1, 2, 8, -3, (byte)0b11100001},  // '11100001'
            {1, 2, 8, -1, (byte)0b100001_00}, // '100001'
            {1, 2, 8,  0, (byte)0b00_000000}, // '0'
            {1, 2, 8,  1, (byte)0b100000_00}, // '100000'
            {1, 2, 8,  3, (byte)0b11100000}   // '11100000'
        };
        
        // encode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {0};
            final BinarizatinBitWriter buf = new BinarizatinBitWriter(
                    new Payload(ByteBuffer.wrap(array)));
            final SignedDoubleTruncatedUnaryBinarization sdtu = 
                    new SignedDoubleTruncatedUnaryBinarization(data[i][0], data[i][1], data[i][2]);

            sdtu.encode(buf, data[i][3]);
            buf.buf.flush();

            Assert.assertEquals(data[i][4], array[0]);
        }
        
        // decode test
        for (int i = 0; i < data.length; i++) {
            final byte[] array = {data[i][4]};
            final BinarizatinBitReader buf = new BinarizatinBitReader(
                    new Payload(ByteBuffer.wrap(array)));

            final SignedDoubleTruncatedUnaryBinarization sdtu = 
                    new SignedDoubleTruncatedUnaryBinarization(data[i][0], data[i][1], data[i][2]);
            
            Assert.assertEquals(data[i][3], sdtu.decode(buf));
        }
    }
    
    public class BinarizatinBitReader extends MCoderBitReader {

        private final Payload buf;
        
        public BinarizatinBitReader(final Payload buf) {
            this.buf = buf;
        }
        
        @Override
        public long readBits(int ctxIdx, int nbits, boolean bypass) throws IOException {
            return buf.readBits(nbits);
        }        
    }
    
    public class BinarizatinBitWriter extends MCoderBitWriter {

        private final Payload buf;
        
        public BinarizatinBitWriter(final Payload buf) {
            this.buf = buf;
        }

        @Override
        public void bypass(short bit) throws IOException {
            buf.writeBits(bit, 1);
        }

        @Override
        public void writeBit(int ctxIdx, short bit) throws IOException {
            buf.writeBits(bit, 1);
        }
    }
}

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

package es.gencom.mpegg.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>
 * MPEG-G Payload based on an array of byte buffers.
 * </p>
 *
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class Payload implements MPEGReader, MPEGWriter {

    private int idx;
    private final ByteBuffer[] buf;

    private long value;
    private byte bits_left;

    public Payload(final ByteBuffer buf) {
        this.buf = new ByteBuffer[1];
        this.buf[0] = buf.slice().order(ByteOrder.BIG_ENDIAN);
        idx = 0;
    }

    public Payload(final ByteBuffer[] buf) {
        this.buf = new ByteBuffer[buf.length];
        for (int i = 0; i < buf.length; i++) {
            this.buf[i] = buf[i].slice().order(ByteOrder.BIG_ENDIAN);
        }
        idx = 0;
    }

    public Payload(byte[] bytes) {
        this(ByteBuffer.wrap(bytes));
    }

    public void position(long newPosition){
        long currentPosition = 0;
        idx = 0;
        bits_left = 0;

        for(ByteBuffer buffer : buf) {
            buffer.rewind();
        }

        while(currentPosition < newPosition) {
            long toAdvance = newPosition - currentPosition;
            if(buf[idx].remaining() > toAdvance) {
                buf[idx].position((int) toAdvance);
                return;
            } else {
                currentPosition += buf[idx].remaining();
                idx++;
                if(idx == buf.length) {
                    return;
                }
            }
        }
    }

    public long remaining() {
        long result = 0;
        for(int bufferIndex = idx; bufferIndex < buf.length; bufferIndex++) {
            result += buf[bufferIndex].remaining();
        }
        return result;
    }

    @Override
    public void align() {
        if(idx == buf.length) return;
        buf[idx].position(buf[idx].position() - (bits_left >>> 3));
        bits_left = 0;
    }

    private ByteBuffer[] slicePayloads(long size) throws EOFException {
        align();
        final int x = idx;
        int slice_idx = idx;
        while(slice_idx < buf.length && size > buf[slice_idx].remaining()) {
            size -= buf[slice_idx++].remaining();
        }

        if (slice_idx == buf.length) {
            if(size == 0){
                return new ByteBuffer[]{ByteBuffer.allocate(0)};
            }
            throw new EOFException();
        }

        final int n = slice_idx - x;
        final ByteBuffer[] bb = new ByteBuffer[n + 1];
        for (int i = 0; i < bb.length; i++) {
            bb[i] = buf[x + i].slice();
        }

        bb[n].limit((int)(size));

        return bb;
    }

    @Override
    public Payload readPayload(final long size) throws IOException {
        Payload result = new Payload(slicePayloads(size));

        position(getPosition() + size);

        return result;
    }

    @Override
    public ByteBuffer readByteBuffer(final int size) throws IOException {
        if(size < 0){
            throw new IllegalArgumentException();
        }
        if(idx == buf.length){
            throw new EOFException();
        }
        align();
        if(size < buf[idx].remaining()){
            ByteBuffer result = buf[idx].slice();
            result.limit(size);
        }
        if(size > remaining()){
            throw new EOFException();
        }
        byte arrayBuffer[]= new byte[size];
        int copied = 0;
        while(copied < size){
            int toCopy = Math.min(buf[idx].remaining(), size-copied);
            buf[idx].get(arrayBuffer, copied, toCopy);
            idx++;
        }
        return ByteBuffer.wrap(arrayBuffer);
    }

    @Override
    public long readBits(int nbits) throws EOFException {
        final long mask = -1L >>> (64 - nbits);

        if (bits_left == 0) {
            value = get();
            if(bits_left < nbits){
                throw new EOFException();
            }
        } else if (bits_left < nbits) {
            nbits -= bits_left;
            long bits = value << nbits;
            value = get();
            if(bits_left < nbits){
                throw new EOFException();
            }
            bits_left -= nbits;
            return (value >>> bits_left | bits) & mask;
        }

        bits_left -= nbits;
        return (value >>> bits_left) & mask;
    }

    @Override
    public void writeByteBuffer(final ByteBuffer buffer) throws IOException {
        while(buffer.remaining() > 0){
            if(idx == buf.length) {
                throw new EOFException();
            }

            final int toCopy = Math.min(buf[idx].remaining(), buffer.remaining());
            byte[] copyBuffer = new byte[toCopy];
            buffer.get(copyBuffer);
            buf[idx].put(copyBuffer);
            if(buf[idx].remaining()==0){
                idx++;
                if(idx != buf.length){
                    buf[idx].rewind();
                }
            }
        }
    }

    @Override
    public void writeBits(long bits, int nbits) {
        
        bits &= -1L >>> (64 - nbits);

        if (bits_left > nbits) {
            bits_left -= nbits;
            value |= bits << bits_left;
        } else if (bits_left == 0) {
            bits_left = (byte)(64 - nbits);
            value = bits << bits_left;
            if(bits_left == 0){
                put(value);
            }
        } else if (bits_left < nbits) {
            nbits -= bits_left;
            put(value | (bits >> nbits));
            bits_left = (byte)(64 - nbits);
            value = bits << bits_left;
        } else {
            put(value | bits);
            bits_left = 0;
        }
    }

    private long get() throws EOFException {
        if(idx == buf.length){
            throw new EOFException();
        }

        if (buf[idx].remaining() >= Long.BYTES) {
            bits_left = 64;
            return buf[idx].getLong();
        }

        bits_left = 0;
        long val = 0;
        do {
            for (;buf[idx].remaining() > 0 && bits_left < 64; bits_left += 8) {
                val = (val << 8) | (0xFFL & buf[idx].get());
            }
        } while (bits_left < 64 && ++idx < buf.length);

        return val;
    }

    protected void put(long value) {
        if (buf[idx].remaining() >= Long.BYTES) {
            buf[idx].putLong(value);
        }

        for (int i = 56; i >= 0 && idx < buf.length; idx++) {
            for (;i >= 0 && buf[idx].remaining() > 0; i -= 8) {
                buf[idx].put((byte)((value >>> i) & 0xFF));
            }
        }
    }

    @Override
    public void flush() throws IOException {
        if (bits_left > 0) {
            int len = (71 - bits_left) >>> 3;
            put(value);
            
            // check if payload is completely full
            if (idx < buf.length) {
                // also check an absurd case when buf[idx - 1].length < len;
                while (idx > 0 && buf[idx].position() < len) {
                    len -= buf[idx].position();
                    buf[idx--].position(0);
                }
                buf[idx].position(buf[idx].position() - len);
            }
            bits_left = 0;
        }
    }

    public void rewind() {
        position(0);
    }

    public ByteBuffer[] getByteBuffers() {
        return buf;
    }

    @Override
    public long getPosition() {
        long position = 0;
        for(int i = 0; i < idx; i++){
            ByteBuffer byteBuffer = buf[i];
            final int byteBufferPosition = byteBuffer.position();
            byteBuffer.rewind();
            position += byteBuffer.remaining();
            byteBuffer.position(byteBufferPosition);
        }
        if(idx != buf.length) {
            ByteBuffer byteBuffer = buf[idx];
            int byteBufferPosition = byteBuffer.position();
            byteBuffer.rewind();
            int startPosition = byteBuffer.position();
            position += (byteBufferPosition - startPosition);
            byteBuffer.position(byteBufferPosition);
        }

        if(bits_left != 0 && bits_left != 64){
            position -= bits_left / 8;
        }

        return position;
    }

    /**
     *
     * @param payload
     * @return
     */
    public Payload addPayload(final Payload payload) {
        final ByteBuffer newBuf[] = new ByteBuffer[buf.length + payload.buf.length];
        System.arraycopy(buf, 0, newBuf, 0, buf.length);

        for(int i = 0, j = buf.length; i < payload.buf.length; i++, j++) {
            final int position = payload.buf[i].position();
            payload.buf[i].rewind();
            newBuf[j] = payload.buf[i].slice();
            payload.buf[i].position(position);
        }

        return new Payload(newBuf);
    }

    public long size() {
        final long position = getPosition();
        position(0);
        final long size = remaining();
        position(position);
        return size;
    }

    public byte[] getAllBytes() throws IndexOutOfBoundsException {
        final long lsize = size();
        if(lsize > Integer.MAX_VALUE){
            throw new IndexOutOfBoundsException();
        }
        rewind();

        int size = (int)lsize;
        byte[] outputArray = new byte[size];

        for(int i = 0, offset = 0; i < buf.length; i++) {
            final int toCopy = buf[i].remaining();
            buf[i].get(outputArray, offset, toCopy);
            offset += toCopy;
        }

        return outputArray;
    }

    public Payload cloneAndRewind(){
        ByteBuffer[] byteBuffers = new ByteBuffer[buf.length];
        for(int i = 0; i < buf.length; i++) {
            byteBuffers[i] = buf[i].asReadOnlyBuffer();
            byteBuffers[i].rewind();
        }
        return new Payload(byteBuffers);
    }

    public Payload createCopy() {
        ByteBuffer[] byteBuffers = new ByteBuffer[this.buf.length];
        for(int i = 0; i<this.buf.length; i++){
            byteBuffers[i] = this.buf[i].slice();
        }
        Payload cloned = new Payload(byteBuffers);

        cloned.idx = this.idx;
        cloned.bits_left = this.bits_left;
        cloned.value = this.value;

        return cloned;
    }
}

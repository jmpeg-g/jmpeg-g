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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * <p>
 * File Channel based MPEGReader implementation.
 * </p>
 * 
 * @author @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class ReadableMSBitFileChannel extends ReadableMSBitChannel<FileChannel> {
    private final long mask;
    private final long chunkSize;

    /**
     * Last chunk start position.
     * Because chunks are of 2G size it's always x * 2G.
     */
    private long chunk_start;

    /**
     * The actual chunckSize is equal to 2^logChunckSize;
     */
    private final byte logChunckSize;
    private WeakReference<ByteBuffer> weak;

    public ReadableMSBitFileChannel(final FileChannel channel, byte logChunckSize) {
        super(channel);
        this.logChunckSize = logChunckSize;
        mask = 0x7fffffffffffffffL << logChunckSize;
        chunkSize = (1L << logChunckSize)-1;
    }

    public ReadableMSBitFileChannel(final FileChannel channel){
        this(channel, (byte) 31);
    }

    @Override
    public ByteBuffer readByteBuffer(final int size) throws IOException {
        align();

        final FileChannel ch = (FileChannel)channel;
        final ByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, ch.position(), size);
        ch.position(channel.position() + size);
        return byteBuffer;
    }

    /**
     * Constructs a payload based on an array of file mapped buffers.
     * 
     * @param size the size of payload to be obtained
     * 
     * @return payload backed by memory mapped buffers
     * 
     * @throws IOException
     */
    @Override
    public Payload readPayload(final long size) throws IOException {

        /*
         * Image drawn for the default case logChunkSize = 31
         *              2G               2G               2G
         *      +----------------+----------------+----------------+-----
         *      ^            | 0 |         1      | 2 | n = 2
         * chank_start       |<---------------------->|
         *      |            |           size     |   |
         *      |<---------->|                    |   ^
         *            pre    |                    |  new_channel_pos
         *                   ^                    ^
         *             ch.position()       new_chunk_start
         */

        long new_chunk_start = channel.position() & mask;

        final int pre = (int) (channel.position() - new_chunk_start);
        final int n = (int)((pre + size - 1) >> logChunckSize);
        final ByteBuffer[] buf = new ByteBuffer[n + 1];

        int i = 0;
        if (weak != null && new_chunk_start == chunk_start) {
            final ByteBuffer chunk = weak.get();
            if (chunk != null) {
                buf[0] = chunk.slice();
                i = 1;
                new_chunk_start += chunkSize;
            }
        }

        for (; i < n; i++, new_chunk_start += chunkSize) {
            buf[i] = channel.map(FileChannel.MapMode.READ_ONLY, new_chunk_start, chunkSize).slice();
            channel.position(new_chunk_start + chunkSize);
        }

        if (buf[n] == null) {
            final long mapSize = Math.min(chunkSize, channel.size()-new_chunk_start);
            final ByteBuffer chunk = channel.map(FileChannel.MapMode.READ_ONLY, new_chunk_start, mapSize);
            buf[n] = chunk.slice();
            chunk_start = new_chunk_start;
            weak = new WeakReference(chunk);
        }

        final long new_channel_pos = channel.position() + size;
        channel.position(new_channel_pos);

        buf[0].position(pre);
        buf[n].limit((int) (new_channel_pos - chunk_start));

        return new Payload(buf);
    }

    @Override
    public long getPosition() throws IOException {
        return channel.position();
    }
}

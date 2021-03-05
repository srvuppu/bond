// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.bondlib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides implementation for the Bond blob type as a simple wrapper around {@code byte []}.
 */
public final class Blob implements Comparable<Blob>, Cloneable {

    public final static ByteBuffer[] EMPTY_BUFFER = new ByteBuffer[0];

    private ByteBuffer[] buffers;
    private int size = 0;
    private int hash = 0;

    /**
     * Creates a new Blob instance
     *
     * @return new Blob instance with an empty {@code byte[]}
     */
    public Blob() {
        this.buffers = EMPTY_BUFFER;
    }

    /**
     * Creates a new Blob instance wrapping an existing {@code byte[]}
     * @param data
     * @return new Blob instance wrapping the data array
     * @throws IllegalArgumentException if the argument is null
     */
    public Blob(byte[] data) {
        ArgumentHelper.ensureNotNull(data, "data");
        ByteBuffer buffer = ByteBuffer.wrap(data);

        this.buffers = new ByteBuffer[]{buffer};
        this.size = data.length;
    }

    public Blob(byte[] data, int offset, int length) {
        ArgumentHelper.ensureNotNull(data, "data");
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);

        this.buffers = new ByteBuffer[]{buffer};
        this.size = length;
    }

    public Blob(ByteBuffer buffer) {
        ArgumentHelper.ensureNotNull(buffer, "buffer");
        this.buffers = new ByteBuffer[]{buffer};
        this.size = buffer.limit() - buffer.position();
    }

    public Blob(ByteBuffer[] buffers) {
        ArgumentHelper.ensureNotNull(buffers, "buffers");

        this.buffers = buffers;
        for (int i = 0; i < buffers.length; ++i) {
            ByteBuffer buffer = buffers[i];
            this.size += buffer.limit() - buffer.position();
        }
    }

    public InputStream toInputStream() {
        int remaining = size;
        List<ByteArrayInputStream> inputStreams = new ArrayList<>(this.buffers.length);
        for (int i = 0; i < buffers.length; ++i) {
            ByteBuffer buffer = this.buffers[i];
            int length = Math.min(buffer.limit() - buffer.position(), remaining);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.array(), buffer.position(), length);
            remaining -= length;
            inputStreams.add(inputStream);
        }

        return new SequenceInputStream(Collections.enumeration(inputStreams));
    }

    /**
     * Compare for equality by contents of underlying {@code byte[]}
     *
     * @param other object to compare against
     * @return {@code true} if object is also a Blob and the underlying arrays have equivalent contents
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Blob)) {
            return false;
        }

        Blob otherBlob = (Blob) other;
        if (otherBlob.size != this.size) {
            return false;
        }

        if (otherBlob.buffers.length != this.buffers.length) {
            return false;
        }

        for (int i = 0; i < buffers.length; ++i) {
            ByteBuffer left = otherBlob.buffers[i];
            ByteBuffer right = this.buffers[i];

            if (!left.equals(right)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Blob clone() {
        return new Blob(this.buffers);
    }

    /**
     * Generate hash code based on contents of underlying {@code byte[]}
     *
     * @return hash code of underlying array contents
     */
    @Override
    public int hashCode() {
        if (hash == 0 && buffers.length > 0) {
            generateHashCode();
        }
        return hash;
    }

    private void generateHashCode() {
        int hashCode = 0;
        for (int i = 0; i < buffers.length; ++i) {
            hashCode <<= 2;
            hashCode += this.buffers[i].hashCode();
        }

        this.hash = hashCode;
    }

    public int size() {
        return size;
    }

    public ByteBuffer[] getByteBuffers() {
        return this.buffers;
    }

    public byte[] toByteArray() {
        // If this Blob contains only one ByteBuffer
        // and the buffer length is exactly same as blob length,
        // we can just return the internal buffer without data copy.
        if (buffers.length == 1) {
            ByteBuffer firstBuffer = buffers[0];
            if (firstBuffer.hasArray() && firstBuffer.position() == 0
                    && firstBuffer.limit() - firstBuffer.position() == size && firstBuffer.array().length == size) {
                return firstBuffer.array();
            }
        }

        // For other case, just carefully allocate buffer and copy data out.
        int remaining = size;
        ByteBuffer result = ByteBuffer.allocate(remaining);

        int offset = 0;
        while (remaining > 0) {
            ByteBuffer buffer = this.buffers[offset];
            int length = Math.min(buffer.limit() - buffer.position(), remaining);
            result.put(buffer.array(), buffer.position(), length);

            remaining -= length;
        }

        return result.array();
    }

    public void dispose() {
        this.buffers = EMPTY_BUFFER;
    }

    /**
     * Provide access to underlying {@code byte[]}
     *
     * @return the underying {@code byte[]}
     */
    public byte[] getData() {
        return toByteArray();
    }

    @Override
    public int compareTo(Blob blob) {
        int cmpSize = Math.min(this.size(), blob.size());

        InputStream thisStream = this.toInputStream();
        InputStream thatStream = blob.toInputStream();

        for (int i = 0; i < cmpSize; ++i) {
            try {
                Integer leftValue = thisStream.read();
                Integer rightValue = thatStream.read();

                if (leftValue == rightValue) {
                    continue;
                }

                return leftValue.compareTo(rightValue);
            } catch (IOException ioEx) {
                return -1;
            }
        }

        return Integer.compare(this.size(), blob.size());
    }
}

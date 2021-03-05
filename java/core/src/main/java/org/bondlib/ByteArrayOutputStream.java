/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The file is modified from Apache commons-io library.
 */
package org.bondlib;

import stormpot.Poolable;
import stormpot.Slot;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * This class implements an output stream in which the data is
 * written into a byte array. The buffer automatically grows as data
 * is written to it.
 * <p>
 * The data can be retrieved using <code>toByteArray()</code> and
 * <code>toString()</code>.
 * <p>
 * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 * <p>
 * This is an alternative implementation of the {@link java.io.ByteArrayOutputStream}
 * class. The original implementation only allocates 32 bytes at the beginning.
 * As this class is designed for heavy duty it starts at 1024 bytes. In contrast
 * to the original it doesn't reallocate the whole memory block but allocates
 * additional buffers. This way no buffers need to be garbage collected and
 * the contents don't have to be copied to the new buffer. This class is
 * designed to behave exactly like the original. The only exception is the
 * deprecated toString(int) method that has been ignored.
 */
public class ByteArrayOutputStream extends OutputStream implements Poolable {

    public static final int EOF = -1;
    static final int DEFAULT_SIZE = 1024;
    /**
     * A singleton empty byte array.
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    /**
     * The list of buffers, which grows and never reduces.
     */
    private final List<byte[]> buffers = new ArrayList<>();
    private final Slot slot;
    /**
     * The index of the current buffer.
     */
    private int currentBufferIndex;
    /**
     * The total count of bytes in all the filled buffers.
     */
    private int filledBufferSum;
    /**
     * The current buffer.
     */
    private byte[] currentBuffer;
    /**
     * The total count of bytes written.
     */
    private int count;
    /**
     * Flag to indicate if the buffers can be reused after reset
     */
    private boolean reuseBuffers = true;

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 1024 bytes, though its size increases if necessary.
     */
    public ByteArrayOutputStream(Slot slot) {
        this(DEFAULT_SIZE, slot);
    }

    public ByteArrayOutputStream() {
        this(null);
    }

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param size the initial size
     * @throws IllegalArgumentException if size is negative
     */
    public ByteArrayOutputStream(final int size, Slot slot) {

        this.slot = slot;
        if (size < 0) {
            throw new IllegalArgumentException(
                    "Negative initial size: " + size);
        }
        needNewBuffer(size);
    }

    /**
     * Makes a new buffer available either by allocating
     * a new one or re-cycling an existing one.
     *
     * @param newcount the size of the buffer if one is created
     */
    private void needNewBuffer(final int newcount) {
        if (currentBufferIndex < buffers.size() - 1) {
            //Recycling old buffer
            filledBufferSum += currentBuffer.length;

            currentBufferIndex++;
            currentBuffer = buffers.get(currentBufferIndex);
        } else {
            //Creating new buffer
            int newBufferSize;
            if (currentBuffer == null) {
                newBufferSize = newcount;
                filledBufferSum = 0;
            } else {
                newBufferSize = Math.max(
                        currentBuffer.length << 1,
                        newcount - filledBufferSum);
                filledBufferSum += currentBuffer.length;
            }

            currentBufferIndex++;
            currentBuffer = new byte[newBufferSize];
            buffers.add(currentBuffer);
        }
    }

    /**
     * Write the bytes to byte array.
     *
     * @param b   the bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     */
    @Override
    public void write(final byte[] b, final int off, final int len) {
        if ((off < 0)
                || (off > b.length)
                || (len < 0)
                || ((off + len) > b.length)
                || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        final int newcount = count + len;
        int remaining = len;
        int inBufferPos = count - filledBufferSum;
        while (remaining > 0) {
            final int part = Math.min(remaining, currentBuffer.length - inBufferPos);
            System.arraycopy(b, off + len - remaining, currentBuffer, inBufferPos, part);
            remaining -= part;
            if (remaining > 0) {
                needNewBuffer(newcount);
                inBufferPos = 0;
            }
        }
        count = newcount;
    }

    /**
     * Write a byte to byte array.
     *
     * @param b the byte to write
     */
    @Override
    public void write(final int b) {
        int inBufferPos = count - filledBufferSum;
        if (inBufferPos == currentBuffer.length) {
            needNewBuffer(count + 1);
            inBufferPos = 0;
        }
        currentBuffer[inBufferPos] = (byte) b;
        count++;
    }

    /**
     * Writes the entire contents of the specified input stream to this
     * byte stream. Bytes from the input stream are read directly into the
     * internal buffers of this streams.
     *
     * @param in the input stream to read from
     * @return total number of bytes read from the input stream
     * (and written to this stream)
     * @throws IOException if an I/O error occurs while reading the input stream
     * @since 1.4
     */
    public int write(final InputStream in) throws IOException {
        int readCount = 0;
        int inBufferPos = count - filledBufferSum;
        int n = in.read(currentBuffer, inBufferPos, currentBuffer.length - inBufferPos);
        while (n != EOF) {
            readCount += n;
            inBufferPos += n;
            count += n;
            if (inBufferPos == currentBuffer.length) {
                needNewBuffer(currentBuffer.length);
                inBufferPos = 0;
            }
            n = in.read(currentBuffer, inBufferPos, currentBuffer.length - inBufferPos);
        }
        return readCount;
    }

    /**
     * Return the current size of the byte array.
     *
     * @return the current size of the byte array
     */
    public int size() {
        return count;
    }

    /**
     * Close is same as release.
     *
     * @throws IOException never (this method should not declare this exception
     *                     but it has to now due to backwards compatibility)
     */
    @Override
    public void close() throws IOException {
        //nop
        this.release();
    }

    /**
     * @see java.io.ByteArrayOutputStream#reset()
     */
    public void reset() {
        count = 0;
        filledBufferSum = 0;
        currentBufferIndex = 0;
        if (reuseBuffers) {
            currentBuffer = buffers.get(currentBufferIndex);
        } else {
            //Throw away old buffers
            currentBuffer = null;
            final int size = buffers.get(0).length;
            buffers.clear();
            needNewBuffer(size);
            reuseBuffers = true;
        }
    }

    /**
     * Writes the entire contents of this byte stream to the
     * specified output stream.
     *
     * @param out the output stream to write to
     * @throws IOException if an I/O error occurs, such as if the stream is closed
     * @see java.io.ByteArrayOutputStream#writeTo(OutputStream)
     */
    public void writeTo(final OutputStream out) throws IOException {
        int remaining = count;
        for (final byte[] buf : buffers) {
            final int c = Math.min(buf.length, remaining);
            out.write(buf, 0, c);
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
    }

    /**
     * Writes the entire contents of this byte stream to the
     * specified output ObjectOutput.
     *
     * @param out the output stream to write to
     * @throws IOException if an I/O error occurs, such as if the stream is closed
     * @see java.io.ByteArrayOutputStream#writeTo(OutputStream)
     */
    public void writeTo(final ObjectOutput out) throws IOException {
        int remaining = count;
        for (final byte[] buf : buffers) {
            final int c = Math.min(buf.length, remaining);
            out.write(buf, 0, c);
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
    }

    /**
     * Gets the current contents of this byte stream as a Input Stream. The
     * returned stream is backed by buffers of <code>this</code> stream,
     * avoiding memory allocation and copy, thus saving space and time.<br>
     *
     * @return the current contents of this output stream.
     * @see java.io.ByteArrayOutputStream#toByteArray()
     * @see #reset()
     * @since 2.5
     */
    public InputStream toInputStream() {
        int remaining = count;
        if (remaining == 0) {
            return new InputStream() {
                @Override
                public int read() {
                    return EOF;
                }
            };
        }
        final List<ByteArrayInputStream> list = new ArrayList<>(buffers.size());
        for (final byte[] buf : buffers) {
            final int c = Math.min(buf.length, remaining);
            list.add(new ByteArrayInputStream(buf, 0, c));
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
        reuseBuffers = false;
        return new SequenceInputStreamEx(Collections.enumeration(list));
    }

    public Blob toBlob() {
        int remaining = count;
        if (remaining == 0) {
            return new Blob();
        }

        List<ByteBuffer> byteBuffers = new ArrayList<>(buffers.size());
        for (int i = 0; i < buffers.size(); ++i) {
            byte[] buf = buffers.get(i);
            final int c = Math.min(buf.length, remaining);
            ByteBuffer buffer = ByteBuffer.wrap(buf, 0, c);
            byteBuffers.add(buffer);

            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }

        reuseBuffers = false;
        return new Blob(byteBuffers.toArray(Blob.EMPTY_BUFFER));
    }

    /**
     * Gets the current contents of this byte stream as a byte array.
     * The result is independent of this stream.
     *
     * @return the current contents of this output stream, as a byte array
     * @see java.io.ByteArrayOutputStream#toByteArray()
     */
    public byte[] toByteArray() {
        int remaining = count;
        if (remaining == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte newbuf[] = new byte[remaining];
        int pos = 0;
        for (final byte[] buf : buffers) {
            final int c = Math.min(buf.length, remaining);
            System.arraycopy(buf, 0, newbuf, pos, c);
            pos += c;
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
        return newbuf;
    }

    /**
     * Gets the current contents of this byte stream as a string
     * using the platform default charset.
     *
     * @return the contents of the byte array as a String
     * @see java.io.ByteArrayOutputStream#toString()
     * @deprecated 2.5 use {@link #toString(String)} instead
     */
    @Override
    @Deprecated
    public String toString() {
        // make explicit the use of the default charset
        return new String(toByteArray(), Charset.defaultCharset());
    }

    /**
     * Gets the current contents of this byte stream as a string
     * using the specified encoding.
     *
     * @param enc the name of the character encoding
     * @return the string converted from the byte array
     * @throws UnsupportedEncodingException if the encoding is not supported
     * @see java.io.ByteArrayOutputStream#toString(String)
     */
    public String toString(final String enc) throws UnsupportedEncodingException {
        return new String(toByteArray(), enc);
    }

    /**
     * Gets the current contents of this byte stream as a string
     * using the specified encoding.
     *
     * @param charset the character encoding
     * @return the string converted from the byte array
     * @see java.io.ByteArrayOutputStream#toString(String)
     * @since 2.5
     */
    public String toString(final Charset charset) {
        return new String(toByteArray(), charset);
    }

    @Override
    public void release() {
        if (slot != null) {

            this.reuseBuffers = true;
            this.reset();
            slot.release(this);
        }
    }

    public void destory() {
        this.reuseBuffers = false;
        this.reset();

        if (slot != null) {
            slot.release(this);
        }
    }

    public class SequenceInputStreamEx extends SequenceInputStream {

        private int totalBytesRead = 0;

        public SequenceInputStreamEx(Enumeration<? extends InputStream> e) {
            super(e);
        }

        @Override
        public void close() throws IOException {
            super.close();

            // Release parent outputstream.
            release();
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            int bytesRead = super.read(b, off, len);
            totalBytesRead += bytesRead;
            return bytesRead;
        }

        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                ++totalBytesRead;
            }

            return value;
        }

        public InputStream cloneStream() throws IOException {
            InputStream stream = new ByteArrayInputStream(toByteArray());
            stream.skip(totalBytesRead);

            return stream;
        }
    }

}

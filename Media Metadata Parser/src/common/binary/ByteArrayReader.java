package common.binary;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Performs in-memory reading of primitive data types from a byte array.
 * 
 * <p>
 * Supports reading of signed and unsigned integers, floating-point numbers, and byte sequences with
 * configurable byte order (big-endian or little-endian).
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 12 December 2025
 */
public final class ByteArrayReader extends AbstractBinaryStream implements BinaryInput
{
    private final byte[] buffer;
    private final int baseIndex;
    private long bufferIndex;

    /**
     * Constructs a reader for the input byte array, starting at the given offset and using the
     * specified byte order.
     *
     * @param buf
     *        the source byte array
     * @param offset
     *        the starting position within the byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     *
     * @throws IllegalArgumentException
     *         if the byte array is {@code null} or empty
     * @throws IndexOutOfBoundsException
     *         if {@code offset} is negative or greater than the length of the byte array
     * @throws NullPointerException
     *         if {@code order} is {@code null}
     */
    public ByteArrayReader(byte[] buf, int offset, ByteOrder order)
    {
        super(order);

        if (buf == null || buf.length == 0)
        {
            throw new IllegalArgumentException("Input buffer cannot be null or empty");
        }

        if (offset < 0 || offset > buf.length)
        {
            throw new IndexOutOfBoundsException("Start index [" + offset + "] out of bounds [0.." + buf.length + "]");
        }

        this.buffer = buf;
        this.baseIndex = offset;
        this.bufferIndex = 0;
    }

    /**
     * Constructs a reader for the input byte array using big-endian byte order.
     *
     * @param buf
     *        the source byte array
     */
    public ByteArrayReader(byte[] buf)
    {
        this(buf, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs a reader for the input byte array using the specified byte order.
     *
     * @param buf
     *        the source byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public ByteArrayReader(byte[] buf, ByteOrder order)
    {
        this(buf, 0, order);
    }

    /**
     * Constructs a reader for the input byte array, starting at the given offset and using
     * big-endian byte order.
     *
     * @param buf
     *        the source byte array
     * @param offset
     *        the starting position within the byte array
     */
    public ByteArrayReader(byte[] buf, int offset)
    {
        this(buf, offset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Closes the stream. For {@code ByteArrayReader}, this is a no-op operation since no underlying
     * native OS assets or file handles are bound to it.
     */
    @Override
    public void close() throws IOException
    {
        // No-op: No OS assets or native file handles are bound to this array reader.
    }

    /**
     * Returns the readable length of this reader.
     *
     * @return the number of bytes from the configured base offset to the end of the byte array
     */
    @Override
    public long length()
    {
        return buffer.length - baseIndex;
    }

    /**
     * Returns the current read position.
     *
     * @return the current position relative to the configured base offset
     */
    @Override
    public long getCurrentPosition()
    {
        return bufferIndex;
    }

    /**
     * Moves to the specified position within the byte array.
     *
     * @param pos
     *        the position to move to
     *
     * @throws IndexOutOfBoundsException
     *         if the position is invalid
     */
    @Override
    public void seek(long pos)
    {
        if (pos < 0 || pos > length())
        {
            throw new IndexOutOfBoundsException("Position [" + pos + "] out of bounds. Valid range is [0.." + length() + "].");
        }

        bufferIndex = pos;
    }

    /**
     * Reads a single signed byte from the current position and advances the reader pointer by 1.
     *
     * @return the signed 8-bit byte value (-128 to 127)
     * 
     * @throws EOFException
     *         if the reader index has reached the end of the readable buffer bounds
     */
    @Override
    public byte readByte() throws IOException
    {
        if (!hasRemaining(1))
        {
            throw new EOFException("End of buffer reached. Cannot read beyond position [" + length() + "]");
        }

        return getByte(bufferIndex++);
    }

    /**
     * Reads an 8-bit unsigned integer from the current position and advances the reader pointer by
     * 1.
     *
     * @return the unsigned 8-bit value (0-255) represented as an integer
     * 
     * @throws EOFException
     *         if the reader index has reached the end of the readable buffer bounds
     */
    @Override
    public int readUnsignedByte() throws IOException
    {
        return (readByte() & 0xFF);
    }

    /**
     * Reads a sequence of bytes from the current position and advances the reader pointer by the
     * requested length.
     *
     * @param length
     *        the number of bytes to read
     * @return a newly allocated byte array containing the copied data payload
     * 
     * @throws EOFException
     *         if fewer bytes remain in the stream than the requested length specifies
     */
    @Override
    public byte[] readBytes(int length) throws IOException
    {
        if (!hasRemaining(length))
        {
            throw new EOFException("Cannot read [" + length + "] bytes. Only [" + remaining() + "] remaining");
        }

        byte[] bytes = getBytes(bufferIndex, length);
        bufferIndex += length;
        return bytes;
    }

    /**
     * Reads a 16-bit signed short value from the current position using the configured byte order
     * and advances the reader pointer by 2.
     *
     * @return the signed short value (-32768 to 32767)
     * 
     * @throws EOFException
     *         if fewer than 2 bytes remain available in the stream buffer
     */
    @Override
    public short readShort() throws IOException
    {
        return (short) readValue(2);
    }

    /**
     * Reads a 16-bit unsigned short from the current position using the configured byte order and
     * advances the reader pointer by 2.
     *
     * @return the unsigned short value (0 to 65535) represented as an integer
     * 
     * @throws EOFException
     *         if fewer than 2 bytes remain available in the stream buffer
     */
    @Override
    public int readUnsignedShort() throws IOException
    {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads a 32-bit signed integer value from the current position using the configured byte order
     * and advances the reader pointer by 4.
     *
     * @return the signed 32-bit integer value
     * 
     * @throws EOFException
     *         if fewer than 4 bytes remain available in the stream buffer
     */
    @Override
    public int readInteger() throws IOException
    {
        return (int) readValue(4);
    }

    /**
     * Reads a 32-bit unsigned integer value from the current position using the configured byte
     * order and advances the reader pointer by 4.
     *
     * @return the unsigned 32-bit integer value represented as a long (0 to 4294967295)
     * 
     * @throws EOFException
     *         if fewer than 4 bytes remain available in the stream buffer
     */
    @Override
    public long readUnsignedInteger() throws IOException
    {
        return readInteger() & 0xFFFFFFFFL;
    }

    /**
     * Reads a 24-bit unsigned integer (3 bytes) from the current position using the configured byte
     * order and advances the reader pointer by 3.
     *
     * @return the 24-bit unsigned value represented as an integer (0 to 16777215)
     * 
     * @throws EOFException
     *         if fewer than 3 bytes remain available in the stream buffer
     */
    @Override
    public int readUnsignedInteger24() throws IOException
    {
        return (int) readValue(3);
    }

    /**
     * Reads a 64-bit signed long value from the current position using the configured byte order
     * and advances the reader pointer by 8.
     *
     * @return the signed 64-bit long value
     * 
     * @throws EOFException
     *         if fewer than 8 bytes remain available in the stream buffer
     */
    @Override
    public long readLong() throws IOException
    {
        return readValue(8);
    }

    /**
     * Reads a 32-bit IEEE 754 single-precision floating-point value from the current position using
     * the configured byte order and advances the reader pointer by 4.
     *
     * @return the single-precision float value
     * 
     * @throws EOFException
     *         if fewer than 4 bytes remain available in the stream buffer
     */
    @Override
    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Reads a 64-bit IEEE 754 double-precision floating-point value from the current position using
     * the configured byte order and advances the reader pointer by 8.
     *
     * @return the double-precision double value
     * 
     * @throws EOFException
     *         if fewer than 8 bytes remain available in the stream buffer
     */
    @Override
    public double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Retrieves the data at the specified offset within the byte array without advancing the
     * current position.
     *
     * @param offset
     *        the offset (relative to baseIndex)
     * @return the byte of data fetched from the offset
     * 
     * @throws IndexOutOfBoundsException
     *         if the targeted offset location falls outside of the buffer bounds
     */
    @Override
    public byte peek(long offset)
    {
        return getByte(offset);
    }

    /**
     * Retrieves a sub-array of bytes from the specified absolute offset without advancing the
     * current position.
     *
     * @param offset
     *        the position relative to the configured base offset
     * @param length
     *        the number of bytes to read
     * @return a new byte array containing the requested data block segment
     * 
     * @throws IndexOutOfBoundsException
     *         if the targeted offset slice window falls outside of the buffer bounds
     */
    @Override
    public byte[] peek(long offset, int length)
    {
        return getBytes(offset, length);
    }

    /**
     * Reads a null-terminated string using the default ISO-8859-1 character encoding.
     *
     * @return the decoded string excluding the terminating null character
     * 
     * @throws IllegalStateException
     *         if the end of the buffer space is reached before discovering a null terminator
     */
    @Override
    public String readString()
    {
        return readString(StandardCharsets.ISO_8859_1);
    }

    /**
     * Reads a null-terminated string decoded via the specified text character set.
     * 
     * <p>
     * Scans for {@code 0x00} starting from the current position. The pointer is advanced past the
     * null terminator.
     * </p>
     *
     * @param charset
     *        the text character encoding configuration to use for string decoding
     * @return the decoded string excluding the null terminator
     *
     * @throws IllegalStateException
     *         if the end of the buffer is reached before a null terminator
     * @throws NullPointerException
     *         if the given {@code charset} parameter is {@code null}
     */
    @Override
    public String readString(Charset charset)
    {
        Objects.requireNonNull(charset, "Charset cannot be null");

        int absStart = baseIndex + (int) bufferIndex;
        int absEnd = absStart;
        int max = baseIndex + buffer.length;

        while (absEnd < max && buffer[absEnd] != 0)
        {
            absEnd++;
        }

        if (absEnd == max)
        {
            throw new IllegalStateException("Unterminated string at position " + bufferIndex);
        }

        String value = new String(buffer, absStart, absEnd - absStart, charset);
        bufferIndex += (absEnd - absStart) + 1;

        return value;
    }

    /**
     * Returns a single byte from the array at the specified relative position.
     *
     * @param position
     *        the index (relative to base offset) in the byte array
     * @return the byte fetched from the position
     */
    private byte getByte(long position)
    {
        validateByteIndex(position, 1);
        return buffer[baseIndex + (int) position];
    }

    /**
     * Copies and returns a sub-array from the byte array, starting from the specified position.
     *
     * @param position
     *        the index (relative to base offset) in the byte array
     * @param length
     *        the total number of bytes to include in the sub-array (must be {@literal <=}
     *        Integer.MAX_VALUE)
     * @return a new byte array containing the subset of the original array
     */
    private byte[] getBytes(long position, int length)
    {
        byte[] bytes = new byte[length];

        validateByteIndex(position, length);
        System.arraycopy(buffer, baseIndex + (int) position, bytes, 0, length);

        return bytes;
    }

    /**
     * Retrieves a value based on the number of bytes from the current position.
     *
     * @param numBytes
     *        the number of bytes to read
     * @return the computed value as a long
     * 
     * @throws IOException
     *         if an I/O error occurs or the requested number of bytes is beyond the file's bounds
     */
    private long readValue(int numBytes) throws IOException
    {
        checkBounds(numBytes);

        long value = 0;
        int start = baseIndex + (int) bufferIndex;

        if (byteOrder == ByteOrder.BIG_ENDIAN)
        {
            for (int i = 0; i < numBytes; i++)
            {
                value = (value << 8) | (buffer[start + i] & 0xFF);
            }
        }

        else
        {
            for (int i = 0; i < numBytes; i++)
            {
                value |= ((long) (buffer[start + i] & 0xFF)) << (i * 8);
            }
        }

        bufferIndex += numBytes;

        return value;
    }

    /**
     * Checks whether the specified position is within the byte array’s bounds.
     *
     * @param position
     *        the position relative to the configured base offset
     * @param length
     *        the total number of bytes to check
     *
     * @throws IndexOutOfBoundsException
     *         if the position or target sequence range falls outside of bounds
     */
    private void validateByteIndex(long position, int length)
    {
        if (position < 0L)
        {
            throw new IndexOutOfBoundsException("Cannot read the buffer with a negative index [" + position + "]");
        }

        if (length < 0)
        {
            throw new IndexOutOfBoundsException("Length of requested bytes cannot be negative [" + length + "]");
        }

        if (length > length() - position)
        {
            throw new IndexOutOfBoundsException("Attempt to read beyond end of buffer. position=" + position + ", requested=" + length + ", remaining=" + (length() - position));
        }
    }
}
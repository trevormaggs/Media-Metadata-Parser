package common.binary;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Performs in-memory reading of primitive data types from a byte array.
 * 
 * <p>
 * Supports reading of signed and unsigned integers, floating-point numbers, and byte sequences with
 * configurable byte order (big-endian or little-endian). All bounds checking restrictions throw a
 * clean runtime {@link IllegalStateException} to keep consumer read operations elegant.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 8 June 2026
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
     *        the starting offset within the byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     *
     * @throws IllegalArgumentException
     *         if the byte array is {@code null} or empty
     * @throws IndexOutOfBoundsException
     *         if {@code offset} is negative or out of bounds
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
     *        the starting offset within the byte array
     */
    public ByteArrayReader(byte[] buf, int offset)
    {
        this(buf, offset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Closes the reader.
     *
     * <p>
     * This implementation performs no action because the reader operates entirely on an in-memory
     * byte array.
     * </p>
     */
    @Override
    public void close()
    {
        // No-op: No OS resources are tied to an in-memory byte array.
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
            throw new IndexOutOfBoundsException("Position [" + pos + "] out of bounds. Valid range is [0.." + length() + "]");
        }

        bufferIndex = pos;
    }

    /**
     * Reads a single signed byte from the current position and advances the current position by 1.
     *
     * @return the signed 8-bit byte value (-128 to 127)
     * @throws IllegalStateException
     *         if the buffer capacity has been reached
     */
    @Override
    public byte readByte()
    {
        byte value = getByte(bufferIndex);

        bufferIndex++;

        return value;
    }

    /**
     * Reads an 8-bit unsigned integer from the current position and advances the current position
     * by 1.
     *
     * @return the unsigned 8-bit value (0-255) represented as an integer
     * 
     * @throws IllegalStateException
     *         if the buffer capacity has been reached
     */
    @Override
    public int readUnsignedByte()
    {
        return (readByte() & 0xFF);
    }

    /**
     * Reads a sequence of bytes from the current position and advances the current position by the
     * requested length.
     *
     * @param length
     *        the number of bytes to read
     * @return a newly allocated byte array containing the copied data payload
     * 
     * @throws IndexOutOfBoundsException
     *         if the length falls outside of bounds
     */
    @Override
    public byte[] readBytes(int length)
    {
        byte[] bytes = getBytes(bufferIndex, length);

        bufferIndex += length;

        return bytes;
    }

    /**
     * Reads a 16-bit signed short value from the current position using the configured byte order
     * and advances the current position by 2.
     *
     * @return the signed short value (-32768 to 32767)
     * 
     * @throws IllegalStateException
     *         if fewer than 2 bytes remain in the buffer
     */
    @Override
    public short readShort()
    {
        return (short) readValue(2);
    }

    /**
     * Reads a 16-bit unsigned short from the current position using the configured byte order and
     * advances the current position by 2.
     *
     * @return the unsigned short value (0 to 65535) represented as an integer
     * 
     * @throws IllegalStateException
     *         if fewer than 2 bytes remain in the buffer
     */
    @Override
    public int readUnsignedShort()
    {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads a 32-bit signed integer value from the current position using the configured byte order
     * and advances the current position by 4.
     *
     * @return the signed 32-bit integer value
     * 
     * @throws IllegalStateException
     *         if fewer than 4 bytes remain in the buffer
     */
    @Override
    public int readInteger()
    {
        return (int) readValue(4);
    }

    /**
     * Reads a 32-bit unsigned integer value from the current position using the configured byte
     * order and advances the current position by 4.
     *
     * @return the unsigned 32-bit integer value represented as a long (0 to 4,294,967,295)
     * 
     * @throws IllegalStateException
     *         if fewer than 4 bytes remain in the buffer
     */
    @Override
    public long readUnsignedInteger()
    {
        return readInteger() & 0xFFFFFFFFL;
    }

    /**
     * Reads a 24-bit unsigned integer (3 bytes) from the current position using the configured byte
     * order and advances the current position by 3.
     *
     * @return the 24-bit unsigned value represented as an integer (0 to 16,777,215)
     * 
     * @throws IllegalStateException
     *         if fewer than 3 bytes remain in the buffer
     */
    @Override
    public int readUnsignedInteger24()
    {
        return (int) readValue(3);
    }

    /**
     * Reads a 64-bit signed long value from the current position using the configured byte order
     * and advances the current position by 8.
     *
     * @return the signed 64-bit long value
     * 
     * @throws IllegalStateException
     *         if fewer than 8 bytes remain in the buffer
     */
    @Override
    public long readLong()
    {
        return readValue(8);
    }

    /**
     * Reads a 32-bit IEEE 754 single-precision floating-point value from the current position using
     * the configured byte order and advances the current position by 4.
     *
     * @return the single-precision float value
     * 
     * @throws IllegalStateException
     *         if fewer than 4 bytes remain in the buffer
     */
    @Override
    public float readFloat()
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Reads a 64-bit IEEE 754 double-precision floating-point value from the current position using
     * the configured byte order and advances the current position by 8.
     *
     * @return the double value
     * 
     * @throws IllegalStateException
     *         if fewer than 8 bytes remain in the buffer
     */
    @Override
    public double readDouble()
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Retrieves the data at the specified offset within the byte array without advancing the
     * current position.
     *
     * @param offset
     *        the offset relative to the start of the readable data
     * @return the byte of data fetched from the offset
     * 
     * @throws IndexOutOfBoundsException
     *         if the targeted offset slice window falls outside of the buffer bounds
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
     *        the offset relative to the start of the readable data
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
     * Reads a null-terminated string using the specified character set.
     *
     * <p>
     * Bytes are read starting at the current position until a null byte ({@code 0x00}) is
     * found. The returned string does not include the null terminator, and the current
     * position is advanced past it.
     * </p>
     *
     * @param charset
     *        the character set used to decode the string
     * @return the decoded string, excluding the null terminator
     *
     * @throws NullPointerException
     *         if {@code charset} is {@code null}
     */
    @Override
    public String readString(Charset charset)
    {
        Objects.requireNonNull(charset, "Charset cannot be null");

        int absStart = baseIndex + (int) bufferIndex;
        int absEnd = absStart;
        int max = buffer.length;

        while (absEnd < max && buffer[absEnd] != 0)
        {
            absEnd++;
        }

        // If absEnd = absStart, then it will return "" (empty)
        String value = new String(buffer, absStart, absEnd - absStart, charset);

        bufferIndex += (absEnd - absStart);

        // Move past the null terminator if needed
        if (absEnd < max)
        {
            bufferIndex++;
        }

        return value;
    }

    /**
     * Returns a single byte from the array at the specified relative position.
     *
     * @param offset
     *        the index (relative to base offset) in the byte array
     * @return the byte fetched from the position
     * 
     * @throws IllegalArgumentException
     *         if {@code offset} is negative
     * @throws IndexOutOfBoundsException
     *         if the requested range is outside the underlying buffer boundary
     */
    private byte getByte(long offset)
    {
        validateByteIndex(offset, 1);
        return buffer[baseIndex + (int) offset];
    }

    /**
     * Copies and returns a sub-array from the byte array, starting from the specified position.
     *
     * @param offset
     *        the index (relative to base offset) in the byte array
     * @param length
     *        the number of bytes to copy
     * @return a new byte array containing the subset of the original array
     * 
     * @throws IllegalArgumentException
     *         if {@code offset} or {@code length} is negative
     * @throws IndexOutOfBoundsException
     *         if the requested range is outside the underlying buffer boundary
     */
    private byte[] getBytes(long offset, int length)
    {
        validateByteIndex(offset, length);

        byte[] bytes = new byte[length];
        System.arraycopy(buffer, baseIndex + (int) offset, bytes, 0, length);

        return bytes;
    }

    /**
     * Reads a value composed of the specified number of bytes from the current position using the
     * configured byte order.
     *
     * <p>
     * The current position is advanced by {@code numBytes} after the value has been read.
     * </p>
     *
     * @param numBytes
     *        the number of bytes to read
     * @return the assembled value represented as a {@code long}
     *
     * @throws IllegalStateException
     *         if fewer than {@code numBytes} bytes remain available
     */
    private long readValue(int numBytes)
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
     * Validates that the specified byte range is within the readable bounds.
     *
     * @param offset
     *        the position relative to the configured base offset
     * @param length
     *        the total number of bytes to check
     *
     * @throws IndexOutOfBoundsException
     *         if either offset, length or target sequence range falls outside of bounds
     */
    private void validateByteIndex(long offset, int length)
    {
        long fileLength = length();
        long endOffset = offset + length;

        if (offset < 0L)
        {
            throw new IndexOutOfBoundsException("Offset index cannot be negative [" + offset + "]");
        }

        if (length < 0)
        {
            throw new IndexOutOfBoundsException("Requested read length cannot be negative [" + length + "]");
        }

        if (endOffset < offset || endOffset > fileLength)
        {
            throw new IndexOutOfBoundsException(String.format("Peek out of bounds: offset [%d], length [%d], file length [%d]", offset, length, fileLength));
        }
    }
}
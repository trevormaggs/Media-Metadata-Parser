package common;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Performs sequential reading of primitive data types from a byte array.
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
public class SequentialByteArrayReader implements ByteStreamReader
{
    private final byte[] buffer;
    private final int baseIndex;
    private final Deque<Long> markPosition;
    private long bufferIndex;
    private ByteOrder byteOrder;

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
    public SequentialByteArrayReader(byte[] buf, int offset, ByteOrder order)
    {
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
        this.byteOrder = Objects.requireNonNull(order, "Byte order cannot be null");
        this.bufferIndex = 0;
        this.markPosition = new ArrayDeque<>();
    }

    /**
     * Constructs a reader for the input byte array using big-endian byte order.
     *
     * @param buf
     *        the source byte array
     */
    public SequentialByteArrayReader(byte[] buf)
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
    public SequentialByteArrayReader(byte[] buf, ByteOrder order)
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
    public SequentialByteArrayReader(byte[] buf, int offset)
    {
        this(buf, offset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Closes this reader. No action is performed because this reader operates entirely on an
     * in-memory byte array.
     */
    @Override
    public void close()
    {
        // No resources to release.
    }

    /**
     * Currently not used and returns {@code null}. This method is implemented solely to satisfy the
     * {@link ByteStreamReader} interface contract.
     *
     * @return {@code null}
     */
    @Override
    public Path getPath()
    {
        return null;
    }

    /**
     * Sets the byte order for interpreting the input bytes.
     *
     * @param order
     *        the byte order for interpreting the input bytes
     */
    @Override
    public void setByteOrder(ByteOrder order)
    {
        byteOrder = Objects.requireNonNull(order, "Byte order cannot be null");
    }

    /**
     * Returns the byte order used when interpreting multi-byte values.
     *
     * @return either {@link ByteOrder#BIG_ENDIAN} or {@link ByteOrder#LITTLE_ENDIAN}
     */
    @Override
    public ByteOrder getByteOrder()
    {
        return byteOrder;
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
     * Skips forward by the given number of bytes.
     *
     * @param n
     *        the number of bytes to skip
     *
     * @throws IndexOutOfBoundsException
     *         if the position is outside the valid range [0..length()]
     */
    @Override
    public void skip(long n)
    {
        long newPosition = bufferIndex + n;

        if (newPosition < 0 || newPosition > length())
        {
            throw new IndexOutOfBoundsException("Cannot skip by [" + n + "] bytes. New position [" + newPosition + "] is out of bounds [0, " + length() + "].");
        }

        bufferIndex = newPosition;
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
     * Marks the current position in the buffer, allowing a subsequent {@link #reset()} call to
     * return to this position.
     */
    @Override
    public void mark()
    {
        markPosition.push(bufferIndex);
    }

    /**
     * Resets the reader's position to the last marked position.
     *
     * @throws IllegalStateException
     *         if the mark stack is empty
     */
    @Override
    public void reset()
    {
        if (markPosition.isEmpty())
        {
            throw new IllegalStateException("Cannot reset position: mark stack is empty");
        }

        bufferIndex = markPosition.pop();
    }

    /**
     * Retrieves the data at the specified offset within the byte array without advancing the
     * current position.
     *
     * @param offset
     *        the offset (relative to baseIndex)
     * @return the byte of data
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
     * @return a new byte array containing the data
     */
    @Override
    public byte[] peek(long offset, int length)
    {
        return getBytes(offset, length);
    }

    /**
     * Reads a single byte from the current position and advances the reader.
     *
     * @return the byte value
     */
    @Override
    public byte readByte()
    {
        if (!hasRemaining(1))
        {
            throw new IndexOutOfBoundsException("End of buffer reached. Cannot read beyond position [" + length() + "]");
        }

        return getByte(bufferIndex++);
    }

    /**
     * Reads a sequence of bytes from the current position and advances the reader.
     *
     * @param length
     *        the number of bytes to read
     * @return a new byte array containing the data
     */
    @Override
    public byte[] readBytes(int length)
    {
        if (!hasRemaining(length))
        {
            throw new IndexOutOfBoundsException("Cannot read [" + length + "] bytes. Only [" + remaining() + "] remaining");
        }

        byte[] bytes = getBytes(bufferIndex, length);

        bufferIndex += length;

        return bytes;
    }

    /**
     * Reads an 8-bit unsigned integer from the current position and advances the reader.
     *
     * @return the unsigned 8-bit value (0-255)
     */
    @Override
    public int readUnsignedByte()
    {
        return (readByte() & 0xFF);
    }

    /**
     * Reads a 16-bit signed short value from the current position and advances the reader.
     *
     * @return the short value
     */
    @Override
    public short readShort()
    {
        return (short) readValue(2);
    }

    /**
     * Reads a 16-bit unsigned short from the current position and advances the reader.
     *
     * @return the unsigned short value (0-65535) represented by an integer
     */
    @Override
    public int readUnsignedShort()
    {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads a 32-bit signed integer from the current position and advances the reader.
     *
     * @return the signed integer value
     */
    @Override
    public int readInteger()
    {
        return (int) readValue(4);
    }

    /**
     * Reads a 32-bit unsigned integer from the current position and advances the reader.
     *
     * @return the unsigned integer value represented as a long
     */
    @Override
    public long readUnsignedInteger()
    {
        return readInteger() & 0xFFFFFFFFL;
    }

    /**
     * Reads a 24-bit unsigned integer (3 bytes) from the current position.
     *
     * @return the 24-bit value as an integer
     */
    @Override
    public int readUnsignedInt24()
    {
        return (int) readValue(3);
    }

    /**
     * Reads a 64-bit signed long from the current position and advances the reader.
     *
     * @return the long value
     */
    @Override
    public long readLong()
    {
        return readValue(8);
    }

    /**
     * Reads a 32-bit IEEE 754 single-precision floating-point value from the current position and
     * advances the reader.
     *
     * @return the floating-point value
     */
    @Override
    public float readFloat()
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Reads a 64-bit IEEE 754 double-precision floating-point value from the current position and
     * advances the reader.
     *
     * @return the floating-point value
     */
    @Override
    public double readDouble()
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads a null-terminated string (ISO-8859-1).
     *
     * <p>
     * Scans for {@code 0x00} starting from the current position. The pointer is advanced past the
     * null terminator.
     * </p>
     *
     * @return the decoded string excluding the null terminator
     *
     * @throws IllegalStateException
     *         if the end of the buffer is reached before a null terminator
     */
    @Override
    public String readString()
    {
        int start = (int) bufferIndex;
        int limit = (int) length();
        int end = start;

        while (end < limit)
        {
            if (buffer[baseIndex + end] == 0x00)
            {
                break;
            }

            end++;
        }

        if (end == limit)
        {
            throw new IllegalStateException("Null terminator not found starting at position [" + start + "]");
        }

        int strLength = end - start;
        byte[] stringBytes = new byte[strLength];

        System.arraycopy(buffer, baseIndex + start, stringBytes, 0, strLength);

        bufferIndex = end + 1;

        return new String(stringBytes, StandardCharsets.ISO_8859_1);
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
     * Checks whether the specified position is within the byte array’s bounds. If the position is
     * out of range, an {@code IndexOutOfBoundsException} is thrown.
     *
     * @param position
     *        the position relative to the configured base offset (0 represents the first readable
     *        byte)
     * @param length
     *        the total number of bytes to check (must be {@literal <=} Integer.MAX_VALUE)
     *
     * @throws IndexOutOfBoundsException
     *         if the position is out of bounds
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

        if (position + length > length())
        {
            throw new IndexOutOfBoundsException("Attempt to read beyond end of buffer. Relative index [" + position + "], Requested length [" + length + "], Readable length [" + length() + "]");
        }

        if (position > Integer.MAX_VALUE)
        {
            throw new IndexOutOfBoundsException("Position exceeds Java's maximum array index limit");
        }
    }

    /**
     * Returns the number of unread bytes remaining in the buffer, relative to the current read
     * position.
     *
     * @return the number of bytes still available for reading
     */
    public long remaining()
    {
        return length() - bufferIndex;
    }

    /**
     * Checks if at least the specified number of bytes are available to read.
     *
     * @param n
     *        the number of bytes to check for
     *
     * @return true if {@code n} bytes or more remain, otherwise false
     */
    public boolean hasRemaining(int n)
    {
        return remaining() >= n;
    }

    /**
     * Retrieves a value based on the number of bytes from the current position.
     *
     * @param numBytes
     *        the number of bytes to read
     * @return the computed value as a long
     */
    private long readValue(int numBytes)
    {
        if (!hasRemaining(numBytes))
        {
            throw new IndexOutOfBoundsException("Insufficient bytes remaining");
        }

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
}
package common.binary;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Provides a universal state and positioning framework for binary streams.
 * 
 * @author Trevor Maggs
 * @version 1.6
 */
public abstract class AbstractBinaryStream implements AutoCloseable
{
    protected final Deque<Long> positionStack;
    protected ByteOrder byteOrder;

    protected AbstractBinaryStream(ByteOrder order)
    {
        this.positionStack = new ArrayDeque<>();
        this.byteOrder = Objects.requireNonNull(order, "Byte order cannot be null");
    }

    public abstract long length() throws IOException;
    public abstract long getCurrentPosition() throws IOException;
    public abstract void seek(long position) throws IOException;

    @Override
    public abstract void close() throws IOException;

    /**
     * Moves the file pointer by a relative offset.
     *
     * @param n
     *        the number of bytes to skip (positive to move forward, negative for backward)
     *
     * @throws IndexOutOfBoundsException
     *         if the resulting position is out of file bounds
     * @throws IOException
     *         if an I/O error occurs or the stream ends prematurely
     */
    public void skip(long n) throws IOException
    {
        long length = length();
        long target = getCurrentPosition() + n;

        if (target < 0 || target > length)
        {
            throw new IndexOutOfBoundsException("Skip target [" + target + "] out of bounds [0-" + length + "]");
        }

        seek(target);
    }

    /**
     * Pushes the current file pointer onto the internal mark stack. A subsequent call to
     * {@link #reset()} will pop this position and return the reader to it.
     */
    public void mark()
    {
        try
        {
            positionStack.push(getCurrentPosition());
        }

        catch (IOException exc)
        {
            throw new RuntimeException("Failed to mark stream position", exc);
        }
    }

    /**
     * Returns to the position recorded by the most recent {@link #mark()}. This operation pops the
     * position from the stack.
     *
     * @throws IllegalStateException
     *         if the mark stack is empty
     */
    public void reset()
    {
        if (positionStack.isEmpty())
        {
            throw new IllegalStateException("Mark stack is empty");
        }

        try
        {
            seek(positionStack.pop());
        }

        catch (IOException exc)
        {
            throw new RuntimeException("Failed to reset stream position", exc);
        }
    }

    /**
     * Updates the byte order for subsequent multi-byte read operations.
     *
     * @param order
     *        the new byte order
     *
     * @throws NullPointerException
     *         if {@code order} is {@code null}
     */
    public void setByteOrder(ByteOrder order)
    {
        byteOrder = Objects.requireNonNull(order, "Byte order cannot be null");
    }

    /**
     * Returns the current byte order used for interpreting data.
     *
     * @return the active {@link ByteOrder}, either {@link ByteOrder#BIG_ENDIAN} or
     *         {@link ByteOrder#LITTLE_ENDIAN}
     */
    public ByteOrder getByteOrder()
    {
        return byteOrder;
    }

    /**
     * Returns the number of unread bytes remaining between the current position and the end of the
     * stream.
     *
     * @return the number of bytes remaining
     * 
     * @throws IOException
     *         if an I/O error occurs while obtaining the current position or stream length
     */
    public long remaining() throws IOException
    {
        return length() - getCurrentPosition();
    }

    /**
     * Checks whether at least one byte remains available for reading.
     *
     * @return {@code true} if at least one byte remains, otherwise {@code false}
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    public boolean hasRemaining() throws IOException
    {
        return hasRemaining(1);
    }

    /**
     * Checks if at least the specified number of bytes are available to read.
     *
     * @param n
     *        the number of bytes to check for
     * @return true if {@code n} bytes or more remain, otherwise false
     *
     * @throws IllegalArgumentException
     *         if the number of bytes is negative
     * @throws IOException
     *         if an I/O error occurs
     */
    public boolean hasRemaining(int n) throws IOException
    {
        if (n < 0)
        {
            throw new IllegalArgumentException("Byte count cannot be negative");
        }

        return remaining() >= n;
    }

    /**
     * Validates that enough bytes remain in the file for the subsequent operation.
     *
     * @param byteLen
     *        the number of bytes required
     *
     * @throws EOFException
     *         if the requested number of bytes is beyond the file's bounds
     * @throws IOException
     *         if an I/O error occurs while determining the current position
     */
    protected void checkBounds(long byteLen) throws IOException
    {
        long remaining = remaining();

        if (byteLen < 0)
        {
            throw new IllegalArgumentException("Byte count cannot be negative");
        }

        if (byteLen > remaining)
        {
            throw new EOFException(String.format("Requested %d bytes, but only %d remain", byteLen, remaining));
        }
    }
}
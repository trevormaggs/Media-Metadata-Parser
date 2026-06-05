package common;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public abstract class AbstractBinaryStream 
{
    protected final Deque<Long> positionStack;
    protected ByteOrder byteOrder;

    public AbstractBinaryStream(ByteOrder order)
    {
        this.positionStack = new ArrayDeque<>();
        this.byteOrder = Objects.requireNonNull(order, "Byte order cannot be null");
    }

    /**
     * Returns the number of unread bytes remaining between the current position and the end of the
     * stream.
     *
     * @return the number of bytes remaining
     *
     * @throws IOException
     *         if an I/O error occurs while obtaining the current position
     */
    public long remaining() throws IOException
    {
        return (length() - getCurrentPosition());
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
    public boolean hasRemaining(long n) throws IOException
    {
        if (n < 0)
        {
            throw new IllegalArgumentException("Byte count cannot be negative");
        }

        return remaining() >= n;
    }

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
        long fileLength = length();
        long target = getCurrentPosition() + n;

        if (target < 0 || target > fileLength)
        {
            throw new IndexOutOfBoundsException("Skip target [" + target + "] out of bounds [0-" + fileLength + "]");
        }

        seek(target);
    }

    /**
     * Pushes the current file pointer onto the internal mark stack. A subsequent call to
     * {@link #reset()} will pop this position and return the reader to it.
     *
     * @throws IOException
     *         if an I/O error occurs while retrieving the file pointer
     */
    public void mark() throws IOException
    {
        positionStack.push(getCurrentPosition());
    }

    /**
     * Returns to the position recorded by the most recent {@link #mark()}. This operation pops the
     * position from the stack.
     *
     * @throws IllegalStateException
     *         if the mark stack is empty
     * @throws IOException
     *         if an I/O error occurs
     */
    public void reset() throws IOException
    {
        if (positionStack.isEmpty())
        {
            throw new IllegalStateException("Mark stack is empty");
        }

        seek(positionStack.pop());
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
        byteOrder = Objects.requireNonNull(order);
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

    public abstract long length() throws IOException;
    public abstract long getCurrentPosition() throws IOException;
    public abstract void seek(long position) throws IOException;
}
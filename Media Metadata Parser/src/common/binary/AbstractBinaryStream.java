package common.binary;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Base class for binary input and output streams that provides common position management,
 * byte-order handling, and mark/reset support.
 *
 * <p>
 * Subclasses are responsible for implementing the actual I/O operations, while this class supplies
 * shared navigation and stream-state behaviour.
 * </p>
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
     * Moves the current stream position by the specified offset.
     *
     * @param n
     *        the number of bytes to move. Positive values move forward and negative values move
     *        backward
     *
     * @throws IndexOutOfBoundsException
     *         if the resulting position is outside the stream bounds
     * @throws IOException
     *         if an I/O error occurs
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
     * Saves the current stream position on the internal mark stack.
     *
     * <p>
     * A subsequent call to {@link #reset()} restores the most recently saved position.
     * </p>
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
     * Restores the stream position recorded by the most recent {@link #mark()}.
     *
     * <p>
     * The restored position is removed from the mark stack.
     * </p>
     *
     * @throws IllegalStateException
     *         if no marked position exists
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
     * Sets the byte order used when reading or writing multi-byte values.
     *
     * @param order
     *        the byte order to use
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
     * Returns the number of bytes remaining between the current position and the end of the stream.
     *
     * @return the number of remaining bytes
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    public long remaining() throws IOException
    {
        return length() - getCurrentPosition();
    }

    /**
     * Determines whether at least one byte remains in the stream.
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
     * Determines whether at least the specified number of bytes remain in the stream.
     *
     * @param n
     *        the required number of bytes
     *
     * @return {@code true} if at least {@code n} bytes remain, otherwise {@code false}
     *
     * @throws IllegalArgumentException
     *         if {@code n} is negative
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
     * Verifies that the specified number of bytes remain available.
     *
     * @param byteLen
     *        the number of bytes required
     *
     * @throws IllegalArgumentException
     *         if {@code byteLen} is negative
     * @throws EOFException
     *         if insufficient bytes remain
     * @throws IOException
     *         if an I/O error occurs
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
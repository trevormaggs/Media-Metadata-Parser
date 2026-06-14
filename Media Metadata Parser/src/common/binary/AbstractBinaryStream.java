package common.binary;

import java.io.IOException;
import java.io.UncheckedIOException;
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
 * The following diagram shows the class hierarchy.
 * 
 * <pre>
 * {@literal 
 * <interface>} AutoCloseable
 * ├── {@literal <interface>} BinaryInput
 * └── {@literal <interface>} BinaryOutput
 *
 * AbstractBinaryStream (Root Base)
 * ├── ByteArrayReader [implements BinaryInput]
 * └── AbstractRandomAccessStream [implements AutoCloseable]
 *     ├── RandomAccessReader [implements BinaryInput]
 *     └── RandomAccessWriter [implements BinaryOutput]
 * </pre>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 9 June 2026
 */
public abstract class AbstractBinaryStream
{
    protected final Deque<Long> positionStack;
    protected ByteOrder byteOrder;

    /**
     * Constructs a binary stream with the specified byte order.
     *
     * @param order
     *        the byte order to use
     *
     * @throws NullPointerException
     *         if {@code order} is {@code null}
     */
    protected AbstractBinaryStream(ByteOrder order)
    {
        this.positionStack = new ArrayDeque<>();
        this.byteOrder = Objects.requireNonNull(order, "Byte order cannot be null");
    }

    public abstract long length() throws IOException;
    public abstract long getCurrentPosition() throws IOException;
    public abstract void seek(long position) throws IOException;

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
     * 
     * @throws UncheckedIOException
     *         if the stream position cannot be queried due to an underlying I/O error
     */
    public void mark()
    {
        try
        {
            positionStack.push(getCurrentPosition());
        }

        catch (IOException exc)
        {
            throw new UncheckedIOException("Failed to mark stream position", exc);
        }
    }

    /**
     * Restores the stream position registered by the most recent {@link #mark()}.
     *
     * <p>
     * The restored position is removed from the mark stack.
     * </p>
     *
     * @throws IllegalStateException
     *         if no marked position exists
     * @throws UncheckedIOException
     *         if the stream position cannot be restored due to an underlying I/O error
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
            throw new UncheckedIOException("Failed to reset stream position", exc);
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
     * @return the non-negative number of remaining bytes or zero if the current position is beyond
     *         the end of the stream
     * 
     * @throws UncheckedIOException
     *         if an I/O error occurs querying the stream capacity
     */
    public long remaining()
    {
        try
        {
            long len = length();
            long pos = getCurrentPosition();

            return pos > len ? 0L : len - pos;
        }

        catch (IOException exc)
        {
            throw new UncheckedIOException("Failed to evaluate stream capacity due to an underlying I/O error", exc);
        }
    }

    /**
     * Determines whether at least one byte remains in the stream.
     *
     * @return {@code true} if at least one byte remains, otherwise {@code false}
     */
    public boolean hasRemaining()
    {
        return hasRemaining(1);
    }

    /**
     * Determines whether at least the specified number of bytes remain in the stream.
     *
     * @param n
     *        the number of bytes required
     * @return {@code true} if at least {@code n} bytes remain, otherwise {@code false}
     *
     * @throws IllegalArgumentException
     *         if {@code n} is negative
     */
    public boolean hasRemaining(int n)
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
     * @param byteCount
     *        the number of bytes required
     * 
     * @throws IllegalArgumentException
     *         if {@code byteCount} is negative
     * @throws IllegalStateException
     *         if insufficient bytes remain
     */
    protected void checkBounds(long byteCount)
    {
        if (byteCount < 0)
        {
            throw new IllegalArgumentException("Byte count cannot be negative");
        }

        long remaining = remaining();

        if (byteCount > remaining)
        {
            throw new IllegalStateException(String.format("Requested %d bytes, but only %d remain", byteCount, remaining));
        }
    }
}
package common.binary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Base class for file-backed binary streams that use a {@link RandomAccessFile} for random-access
 * I/O.
 *
 * <p>
 * This class provides common file positioning and length operations while delegating
 * stream-specific reading and writing operations to subclasses.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 */
public abstract class AbstractRandomAccessStream extends AbstractBinaryStream implements AutoCloseable
{
    protected final Path fpath;
    protected final RandomAccessFile raf;

    /**
     * Creates a file-backed binary stream.
     *
     * @param fpath
     *        the file path
     * @param order
     *        the byte order used for multi-byte values
     * @param mode
     *        the {@link RandomAccessFile} access mode, such as {@code "r"} or {@code "rw"}
     *
     * @throws NullPointerException
     *         if any argument is {@code null}
     * @throws IOException
     *         if the file cannot be opened
     */
    public AbstractRandomAccessStream(Path fpath, ByteOrder order, String mode) throws IOException
    {
        super(order);

        Objects.requireNonNull(mode, "Mode cannot be null");
        this.fpath = Objects.requireNonNull(fpath, "Path cannot be null");
        this.raf = new RandomAccessFile(fpath.toFile(), mode);
    }

    /**
     * Closes the underlying {@link RandomAccessFile}.
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public void close() throws IOException
    {
        raf.close();
    }

    /**
     * Returns the length of the underlying file in bytes.
     *
     * @return the file length in bytes
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public long length() throws IOException
    {
        return raf.length();
    }

    /**
     * Returns the current file-pointer position.
     *
     * @return the current byte offset from the beginning of the file
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public long getCurrentPosition() throws IOException
    {
        return raf.getFilePointer();
    }

    /**
     * Moves the file pointer to the specified absolute position.
     *
     * @param n
     *        the byte offset from the beginning of the file
     *
     * @throws IndexOutOfBoundsException
     *         if the position is outside the file bounds
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public void seek(long n) throws IOException
    {
        long fileLength = length();

        if (n < 0 || n > fileLength)
        {
            throw new IndexOutOfBoundsException("Seek position [" + n + "] out of bounds [0-" + fileLength + "]");
        }

        raf.seek(n);
    }

    /**
     * Returns the path of the file backing this stream.
     *
     * @return the file path
     */
    public Path getPath()
    {
        return fpath;
    }
}
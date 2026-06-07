package common.binary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Acts as a middle-tier base class providing native OS file-system binding capabilities via
 * {@link RandomAccessFile}. It bridges absolute hardware interactions with the logical tracking
 * layer.
 *
 * @author Trevor Maggs
 * @version 1.1
 */
public abstract class AbstractRandomAccessStream extends AbstractBinaryStream
{
    protected final Path fpath;
    protected final RandomAccessFile raf;

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
     *         if an I/O error occurs while closing the file
     */
    @Override
    public void close() throws IOException
    {
        if (raf != null)
        {
            raf.close();
        }
    }

    /**
     * Returns the current length of the underlying file.
     *
     * @return the file size in bytes
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
     * Returns the current absolute byte offset of the file pointer.
     *
     * @return the current position
     * 
     * @throws IOException
     *         if an I/O error occurs while reading the file pointer state
     */
    @Override
    public long getCurrentPosition() throws IOException
    {
        return raf.getFilePointer();
    }

    /**
     * Moves the file pointer to an absolute offset.
     *
     * @param n
     *        the absolute byte offset from the beginning of the file
     *
     * @throws IndexOutOfBoundsException
     *         if the resulting position is out of file bounds
     * @throws IOException
     *         if an I/O error occurs or the stream ends prematurely
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
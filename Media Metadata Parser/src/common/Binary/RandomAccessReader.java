package common.Binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A seekable binary reader backed by a {@link java.io.RandomAccessFile}.
 *
 * <p>
 * Supports random-access reading, peeking, mark/reset operations, configurable {@link ByteOrder},
 * and reading of primitive values from arbitrary file positions.
 * </p>
 *
 * <p>
 * This class is suitable for parsing binary file formats such as TIFF, PNG, JPEG, HEIF, RIFF, and
 * other structured binary containers.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 5 June 2026
 */
public final class RandomAccessReader extends AbstractRandomAccessStream implements BinaryInput
{
    /**
     * Creates a reader using the specified byte order.
     *
     * @param fpath
     *        the file to read
     * @param order
     *        the byte order used when reading multi-byte values
     *
     * @throws IOException
     *         if the file cannot be opened
     */
    public RandomAccessReader(Path fpath, ByteOrder order) throws IOException
    {
        super(fpath, order, "r");
    }

    /**
     * Creates a reader using {@link ByteOrder#BIG_ENDIAN}.
     *
     * @param fpath
     *        the file to read
     *
     * @throws IOException
     *         if the file cannot be opened
     */
    public RandomAccessReader(Path fpath) throws IOException
    {
        this(fpath, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a signed byte and advances the current position by one byte.
     *
     * @return the signed byte value
     *
     * @throws IOException
     *         if an I/O error occurs or the end of file is reached
     */
    @Override
    public byte readByte() throws IOException
    {
        checkBounds(1);
        return raf.readByte();
    }

    /**
     * Reads an unsigned 8-bit value.
     *
     * @return the value in the range {@code 0-255}
     *
     * @throws IOException
     *         if an I/O error occurs or the end of file is reached
     */
    @Override
    public int readUnsignedByte() throws IOException
    {
        checkBounds(1);
        return raf.readUnsignedByte();
    }

    /**
     * Reads a sequence of bytes and advances the pointer by the specified length.
     *
     * @param length
     *        the number of bytes to read
     * @return a new array containing the read bytes
     *
     * @throws IllegalArgumentException
     *         if {@code length} is negative
     * @throws IOException
     *         if an I/O error occurs or insufficient bytes remain
     */
    @Override
    public byte[] readBytes(int length) throws IOException
    {
        if (length == 0)
        {
            return new byte[0];
        }

        checkBounds(length);
        
        byte[] bytes = new byte[length];
        raf.readFully(bytes);

        return bytes;
    }

    /**
     * Reads a 16-bit short value respecting the current byte order.
     *
     * @return the signed short value
     *
     * @throws IOException
     *         if an I/O error occurs or the end of file is reached
     */
    @Override
    public short readShort() throws IOException
    {
        checkBounds(2);
        short value = raf.readShort();

        return (byteOrder == ByteOrder.LITTLE_ENDIAN) ? Short.reverseBytes(value) : value;
    }

    /**
     * Reads an unsigned 16-bit value.
     *
     * @return the value in the range {@code 0-65535}
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public int readUnsignedShort() throws IOException
    {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads a 32-bit integer respecting the current byte order.
     *
     * @return the signed integer value
     *
     * @throws IOException
     *         if an I/O error occurs or the end of file is reached
     */
    @Override
    public int readInteger() throws IOException
    {
        checkBounds(4);
        int value = raf.readInt();

        return (byteOrder == ByteOrder.LITTLE_ENDIAN) ? Integer.reverseBytes(value) : value;
    }

    /**
     * Reads an unsigned 32-bit integer.
     *
     * @return the value in the range {@code 0-4294967295}
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public long readUnsignedInteger() throws IOException
    {
        return readInteger() & 0xFFFFFFFFL;
    }

    /**
     * Reads an unsigned 24-bit integer respecting the current byte order.
     *
     * @return the value in the range {@code 0-16777215}
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public int readUnsignedInteger24() throws IOException
    {
        byte[] b = readBytes(3);

        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            return ((b[2] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
        }

        else
        {
            return ((b[0] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[2] & 0xFF);
        }
    }

    /**
     * Reads a 64-bit long respecting the current byte order.
     *
     * @return the signed long value
     *
     * @throws IOException
     *         if an I/O error occurs or the end of file is reached
     */
    @Override
    public long readLong() throws IOException
    {
        checkBounds(8);
        long value = raf.readLong();

        return (byteOrder == ByteOrder.LITTLE_ENDIAN) ? Long.reverseBytes(value) : value;
    }

    /**
     * Reads a 32-bit float respecting the current byte order.
     *
     * @return the float value
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Reads a 64-bit double respecting the current byte order.
     *
     * @return the double value
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads a single byte at an absolute offset without advancing the current file pointer.
     *
     * @param offset
     *        the absolute position to read from
     * @return the signed byte value
     *
     * @throws IndexOutOfBoundsException
     *         if the resulting position is out of file bounds
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public byte peek(long offset) throws IOException
    {
        long fileLength = length();
        long currentPosition = getCurrentPosition();

        if (offset < 0 || offset >= fileLength)
        {
            throw new IndexOutOfBoundsException("Peek target [" + offset + "] out of bounds [0-" + fileLength + "]");
        }

        try
        {
            raf.seek(offset);
            return raf.readByte();
        }

        finally
        {
            raf.seek(currentPosition);
        }
    }

    /**
     * Reads a sequence of bytes at an absolute offset without moving the current file pointer.
     *
     * @param offset
     *        the absolute position to start reading from
     * @param length
     *        the number of bytes to read
     * @return a new byte array containing the requested data
     *
     * @throws IndexOutOfBoundsException
     *         if the requested range extends beyond the file bounds
     * @throws IllegalArgumentException
     *         if either {@code offset} or {@code length} is negative
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public byte[] peek(long offset, int length) throws IOException
    {
        long fileLength = length();

        if (offset < 0 || length < 0)
        {
            throw new IllegalArgumentException("Offset or Length cannot be negative");
        }

        if (offset > fileLength - length)
        {
            throw new IndexOutOfBoundsException("Peek request exceeds file length");
        }

        long originalPos = getCurrentPosition();

        try
        {
            byte[] data = new byte[length];
            raf.seek(offset);
            raf.readFully(data);

            return data;
        }

        finally
        {
            raf.seek(originalPos);
        }
    }

    /**
     * Reads a null-terminated string using ISO-8859-1 encoding.
     *
     * @see #readString(Charset)
     */
    @Override
    public String readString() throws IOException
    {
        return readString(StandardCharsets.ISO_8859_1);
    }

    /**
     * Reads a null-terminated (C-style) string using the specified character set.
     *
     * <p>
     * If the current position already points to a null terminator, an empty string is returned
     * and the file pointer advances by one byte. The string bytes are decoded using the supplied
     * charset and the current position is advanced past both the string data and the null
     * terminator.
     * </p>
     *
     * @param charset
     *        the character set used to decode the string bytes
     * @return the decoded string, excluding the null terminator
     *
     * @throws NullPointerException
     *         if {@code charset} is {@code null}
     * @throws UnsupportedOperationException
     *         if the detected string length exceeds {@link Integer#MAX_VALUE}
     * @throws IOException
     *         if a null terminator cannot be found or an I/O error occurs
     */
    @Override
    public String readString(Charset charset) throws IOException
    {
        Objects.requireNonNull(charset, "Charset cannot be null");

        int b;
        boolean foundNull = false;
        ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(32);
     
        while ((b = raf.read()) != -1)
        {
            /*
             * Just advances the pointer to reach
             * the null terminator if present
             */
            if (b == 0x00)
            {
                foundNull = true;
                break;
            }
            
            bos.write(b);
        }

        if (!foundNull)
        {
            throw new IOException("Null terminator not found before end of stream.");
        }

        byte[] data = bos.toByteArray();
        
        return new String(data, charset);
    }

    /**
     * Determines whether the file pointer is positioned at the end of the file.
     *
     * @return {@code true} if no bytes remain to be read, otherwise {@code false}
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    public boolean isEOF() throws IOException
    {
        return remaining() == 0;
    }
}
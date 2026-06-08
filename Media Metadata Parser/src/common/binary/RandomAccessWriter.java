package common.binary;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides a seekable binary stream writer backed by {@link java.io.RandomAccessFile}.
 *
 * <p>
 * Supports in-place file modification and writing primitive data types using a configurable byte
 * order (endianness).
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 8 June 2026
 */
public final class RandomAccessWriter extends AbstractRandomAccessStream implements BinaryOutput
{
    /**
     * Constructs a random-access writer with a specific byte order configuration operating under
     * read-write ("rw") mode.
     *
     * @param fpath
     *        the path to the target file on disk
     * @param order
     *        the byte order used when writing multi-byte values
     *
     * @throws IOException
     *         if an I/O error occurs while opening the file access channel
     */
    public RandomAccessWriter(Path fpath, ByteOrder order) throws IOException
    {
        super(fpath, order, "rw");
    }

    /**
     * Constructs a random-access writer using {@link ByteOrder#BIG_ENDIAN} by default operating
     * under read-write ("rw") mode.
     *
     * @param fpath
     *        the path to the target file on disk
     *
     * @throws IOException
     *         if an I/O error occurs while opening the file access channel
     */
    public RandomAccessWriter(Path fpath) throws IOException
    {
        this(fpath, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Writes a single signed byte at the current position.
     *
     * @param value
     *        the byte value to write
     *
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    @Override
    public void writeByte(byte value) throws IOException
    {
        raf.writeByte(value);
    }

    /**
     * Writes an array of raw bytes sequentially at the current position.
     *
     * @param data
     *        the byte array to write
     *
     * @throws NullPointerException
     *         if {@code data} is {@code null}
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    @Override
    public void writeBytes(byte[] data) throws IOException
    {
        Objects.requireNonNull(data, "Data array payload cannot be null");
        raf.write(data);
    }

    /**
     * Writes an array of bytes, ensuring the file pointer advances exactly by the specified length.
     * 
     * <p>
     * If the input array is shorter than {@code length}, it is padded with trailing zeros (0x00).
     * If the input array is longer than {@code length}, only the first {@code length} bytes are
     * written into the stream.
     * </p>
     *
     * @param data
     *        the data payload block to write (may be null, treated as all zeros)
     * @param length
     *        the exact number of bytes to write
     * 
     * @throws IllegalArgumentException
     *         if {@code length} is negative
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    public void writeBytes(byte[] data, int length) throws IOException
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("Target byte stream length cannot be negative");
        }

        int maxLength = (data == null) ? 0 : data.length;
        int realLength = Math.min(maxLength, length);

        if (realLength > 0)
        {
            raf.write(data, 0, realLength);
        }

        // Add padding efficiently up to the requested layout size boundary
        int paddingRequired = length - realLength;

        if (paddingRequired > 0)
        {
            byte[] padding = new byte[Math.min(paddingRequired, 4096)];

            while (paddingRequired > 0)
            {
                int bytesToWrite = Math.min(paddingRequired, padding.length);
                raf.write(padding, 0, bytesToWrite);
                paddingRequired -= bytesToWrite;
            }
        }
    }

    /**
     * Writes a 16-bit short value matching the active byte endianness rules.
     *
     * @param value
     *        the short integer value to write
     *
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    @Override
    public void writeShort(short value) throws IOException
    {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Short.reverseBytes(value);
        }

        raf.writeShort(value);
    }

    /**
     * Writes a 32-bit signed integer value matching the active byte endianness rules.
     *
     * @param value
     *        the integer value to write
     *
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    @Override
    public void writeInteger(int value) throws IOException
    {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Integer.reverseBytes(value);
        }

        raf.writeInt(value);
    }

    /**
     * Writes an unsigned 32-bit integer value matching the active byte endianness rules.
     *
     * @param value
     *        the unsigned integer value represented as a long
     *
     * @throws IllegalArgumentException
     *         if the value falls outside of valid unsigned 32-bit limits
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    public void writeUnsignedInteger(long value) throws IOException
    {
        if (value < 0L || value > 0xFFFFFFFFL)
        {
            throw new IllegalArgumentException("Value [" + value + "] out of unsigned 32-bit integer range [0..4294967295]");
        }

        writeInteger((int) value);
    }

    /**
     * Writes an unsigned 24-bit integer value matching the active byte endianness rules. Only the
     * lowest 24 bits of the supplied value are written.
     *
     * @param value
     *        the integer value containing the 24-bit pattern to write
     *
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    public void writeUnsignedInteger24(int value) throws IOException
    {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            raf.write(value & 0xFF);
            raf.write((value >> 8) & 0xFF);
            raf.write((value >> 16) & 0xFF);
        }

        else
        {
            raf.write((value >> 16) & 0xFF);
            raf.write((value >> 8) & 0xFF);
            raf.write(value & 0xFF);
        }
    }

    /**
     * Writes a 64-bit signed long value matching the active byte endianness rules.
     *
     * @param value
     *        the long value to write
     *
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    @Override
    public void writeLong(long value) throws IOException
    {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Long.reverseBytes(value);
        }

        raf.writeLong(value);
    }

    /**
     * Writes a 32-bit IEEE 754 single-precision floating-point value matching the active byte
     * endianness rules.
     *
     * @param value
     *        the float value to write
     *
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    @Override
    public void writeFloat(float value) throws IOException
    {
        writeInteger(Float.floatToIntBits(value));
    }

    /**
     * Writes a 64-bit IEEE 754 double-precision floating-point value matching the active byte
     * endianness rules.
     *
     * @param value
     *        the double value to write
     *
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    @Override
    public void writeDouble(double value) throws IOException
    {
        writeLong(Double.doubleToLongBits(value));
    }

    /**
     * Writes a C-style null-terminated string using the specified character set.
     *
     * <p>
     * A trailing {@code 0x00} byte is always written. If the string is {@code null}, only the
     * terminator is written.
     * </p>
     * 
     * @param s
     *        the string to write, or {@code null} to write only the terminator
     * @param charset
     *        the character set used to encode the string
     *
     * @throws NullPointerException
     *         if {@code charset} is {@code null}
     * @throws IOException
     *         if an I/O error occurs on the storage channel
     */
    public void writeNullTerminatedString(String s, Charset charset) throws IOException
    {
        Objects.requireNonNull(charset, "Charset encoding configuration cannot be null");

        if (s != null)
        {
            writeBytes(s.getBytes(charset));
        }

        writeByte((byte) 0);
    }
}
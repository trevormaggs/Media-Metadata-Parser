package common;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A seekable binary stream writer backed by {@link java.io.RandomAccessFile}.
 *
 * <p>
 * Extends the reader to provide in-place file modification capabilities. Supports writing primitive types with configurable byte order, concerning the endian-ness. This is particularly useful for surgical metadata patching where only specific segments of an image file need to be updated.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 3 February 2026
 */
public class ImageRandomAccessWriter extends ImageRandomAccessReader
{
    /**
     * Instantiates a writer with a specific byte order.
     *
     * @param fpath the path to the file
     * @param order the {@link ByteOrder} for multi-byte interpretation
     *
     * @throws IOException if the file cannot be opened in {@code rw} mode
     */
    public ImageRandomAccessWriter(Path fpath, ByteOrder order) throws IOException
    {
        super(fpath, order, "rw");
    }

    /**
     * Instantiates a writer using {@link ByteOrder#BIG_ENDIAN} by default.
     *
     * @param fpath the path to the file
     *
     * @throws IOException if the file cannot be opened in {@code rw} mode
     */
    public ImageRandomAccessWriter(Path fpath) throws IOException
    {
        this(fpath, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Writes a single signed byte at the current position.
     *
     * @param v the byte value to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeByte(byte v) throws IOException
    {
        raf.writeByte(v);
    }

    /**
     * Writes a single unsigned byte (0-255) at the current position.
     *
     * @param v the value to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeUnsignedByte(int v) throws IOException
    {
        raf.write(v & 0xFF);
    }

    /**
     * Writes an array of bytes at the current position and advances the pointer.
     *
     * @param bytes the data to write, must not be null
     *
     * @throws NullPointerException if bytes is null
     * @throws IOException if an I/O error occurs
     */
    public void writeBytes(byte[] bytes) throws IOException
    {
        Objects.requireNonNull(bytes, "Bytes cannot be null");
        raf.write(bytes);
    }

    /**
     * Writes an array of bytes, ensuring the file pointer advances exactly by the specified length.
     * 
     * <p>
     * If the input array is shorter than {@code length}, it is padded with trailing zeros (0x00). If the input array is longer than {@code length}, only the first {@code length} bytes are
     * written.
     * </p>
     *
     * @param bytes the data to write (may be null, treated as all zeros)
     * @param length the exact number of bytes to occupy in the file
     * 
     * @throws IllegalArgumentException if {@code length} is negative
     * @throws IOException if an I/O error occurs
     */
    public void writeBytes(byte[] bytes, int length) throws IOException
    {
        if(length < 0)
        {
            throw new IllegalArgumentException("Length cannot be negative");
        }

        int maxLength = (bytes == null) ? 0 : bytes.length;
        int realLength = Math.min(maxLength, length);

        if(realLength > 0)
        {
            raf.write(bytes, 0, realLength);
        }

        // Add padding if required
        for(int i = realLength; i < length; i++)
        {
            raf.write(0);
        }
    }

    /**
     * Writes a 32-bit integer according to the current byte order.
     *
     * @param value the integer value to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeInteger(int value) throws IOException
    {
        if(byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Integer.reverseBytes(value);
        }

        raf.writeInt(value);
    }

    /**
     * Writes a 16-bit short according to the current byte order.
     *
     * @param value the short value to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeShort(short value) throws IOException
    {
        if(byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Short.reverseBytes(value);
        }

        raf.writeShort(value);
    }

    /**
     * Writes a 24-bit integer according to the current byte order.
     *
     * @param value the integer to write (only the lower 24 bits are used)
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeUnsignedInteger24(int value) throws IOException
    {
        if(byteOrder == ByteOrder.LITTLE_ENDIAN)
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
     * Writes a C-style string followed by a null terminator (0x00).
     *
     * @param s the string to write, may be null
     * @param charset the character set to use for encoding
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeNullTerminatedString(String s, Charset charset) throws IOException
    {
        Objects.requireNonNull(charset, "Charset cannot be null");

        if(s != null)
        {
            writeBytes(s.getBytes(charset));
        }

        writeByte((byte) 0);
    }

    /**
     * Writes a 64-bit long according to the current byte order.
     *
     * @param value the long value to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeLong(long value) throws IOException
    {
        if(byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Long.reverseBytes(value);
        }

        raf.writeLong(value);
    }

   /**
     * Writes a 32-bit floating-point value according to the current byte order.
     *
     * @param value the 32-bit floating-point value to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeFloat(float value) throws IOException
    {
        writeInteger(Float.floatToIntBits(value));
    }


   /**
     * Writes a 64-bit double-precision value according to the current byte order.
     *
     * @param value the double value to write
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeDouble(double value) throws IOException
    {
        writeLong(Double.doubleToLongBits(value));
    }

    public void writeUnsignedInteger(long value) throws IOException
    {
        if(value < 0 || value > 0xFFFFFFFFL)
        {
            throw new IllegalArgumentException("Value out of unsigned 32-bit range");
        }

        writeInteger((int) value);
    }
}
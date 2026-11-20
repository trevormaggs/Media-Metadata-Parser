package common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import tif.RationalNumber;

/**
 * ByteBuffer-backed utility for decoding primitive values, arrays, and structures from byte arrays.
 * 
 * <p>
 * This utility simplifies binary parsing (such as image metadata formats like TIFF, EXIF, and PNG)
 * by delegating primitive decoding to a shared, endian-configured {@link ByteBuffer} wrapper.
 * </p>
 */
public final class ByteValueConverterBB
{
    /**
     * Default constructor is unsupported and will always throw an exception.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    private ByteValueConverterBB()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Reads a 16-bit signed short from the byte array at the given offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded short value
     */
    public static short toShort(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getShort();
    }

    /**
     * Reads a 16-bit unsigned short from the byte array at the given offset, returned as an
     * integer.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the unsigned short value as an integer (0 to 65535)
     */
    public static int toUnsignedShort(byte[] data, int offset, ByteOrder order)
    {
        return Short.toUnsignedInt(toShort(data, offset, order));
    }

    /**
     * Reads a 32-bit signed integer from the byte array at the given offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded integer value
     */
    public static int toInteger(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getInt();
    }

    /**
     * Reads a 32-bit unsigned integer from the byte array at the given offset, returned as a long.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the unsigned integer value as a long (0 to 4294967295)
     */
    public static long toUnsignedInteger(byte[] data, int offset, ByteOrder order)
    {
        return Integer.toUnsignedLong(toInteger(data, offset, order));
    }

    /**
     * Reads a 64-bit signed long from the byte array at the given offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded long value
     */
    public static long toLong(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getLong();
    }

    /**
     * Reads a 32-bit floating-point value from the byte array at the given offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded float value
     */
    public static float toFloat(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getFloat();
    }

    /**
     * Reads a 64-bit double-precision floating-point value from the byte array at the given offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded double value
     */
    public static double toDouble(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getDouble();
    }

    /**
     * Decodes a sequence of 16-bit signed shorts into an array, starting at the specified offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded short values
     * 
     * @throws IllegalArgumentException
     *         if the remaining byte count is not a multiple of 2
     */
    public static short[] toShortArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 2 != 0)
        {
            throw new IllegalArgumentException("Remaining bytes (" + remaining + ") must be a multiple of 2 to convert to short array.");
        }

        short[] out = new short[remaining / 2];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getShort();
        }

        return out;
    }

    /**
     * Decodes a sequence of 16-bit unsigned shorts into an integer array, starting at the specified
     * offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded unsigned short values represented as integers
     * 
     * @throws IllegalArgumentException
     *         if the remaining byte count is not a multiple of 2
     */
    public static int[] toUnsignedShortArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 2 != 0)
        {
            throw new IllegalArgumentException("Remaining bytes (" + remaining + ") must be a multiple of 2 to convert to unsigned short array.");
        }

        int[] out = new int[remaining / 2];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = Short.toUnsignedInt(buf.getShort());
        }

        return out;
    }

    /**
     * Decodes a sequence of 32-bit signed integers into an array, starting at the specified offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded integer values
     * 
     * @throws IllegalArgumentException
     *         if the remaining byte count is not a multiple of 4
     */
    public static int[] toIntegerArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 4 != 0)
        {
            throw new IllegalArgumentException("Remaining bytes (" + remaining + ") must be a multiple of 4.");
        }

        int[] out = new int[remaining / 4];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getInt();
        }

        return out;
    }

    /**
     * Decodes a sequence of 32-bit unsigned integers into a 64-bit long array, starting at the
     * specified offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded unsigned integer values represented as longs (0 to 4294967295)
     * @throws IllegalArgumentException
     *         if the remaining byte count is not a multiple of 4
     */
    public static long[] toUnsignedIntegerArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 4 != 0)
        {
            throw new IllegalArgumentException("Remaining bytes (" + remaining + ") must be a multiple of 4.");
        }

        long[] out = new long[remaining / 4];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = Integer.toUnsignedLong(buf.getInt());
        }

        return out;
    }

    /**
     * Decodes a sequence of 64-bit signed longs into an array, starting at the specified offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded long values
     * 
     * @throws IllegalArgumentException
     *         if the remaining byte count is not a multiple of 8
     */
    public static long[] toLongArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 8 != 0)
        {
            throw new IllegalArgumentException("Remaining bytes (" + remaining + ") must be a multiple of 8.");
        }

        long[] out = new long[remaining / 8];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getLong();
        }

        return out;
    }

    /**
     * Decodes a sequence of 32-bit floats into an array, starting at the specified offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded float values
     * 
     * @throws IllegalArgumentException
     *         if the remaining byte count is not a multiple of 4
     */
    public static float[] toFloatArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 4 != 0)
        {
            throw new IllegalArgumentException("Remaining bytes (" + remaining + ") must be a multiple of 4.");
        }

        float[] out = new float[remaining / 4];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getFloat();
        }

        return out;
    }

    /**
     * Decodes a sequence of 64-bit doubles into an array, starting at the specified offset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded double values
     * 
     * @throws IllegalArgumentException
     *         if the remaining byte count is not a multiple of 8
     */
    public static double[] toDoubleArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 8 != 0)
        {
            throw new IllegalArgumentException("Remaining bytes (" + remaining + ") must be a multiple of 8.");
        }

        double[] out = new double[remaining / 8];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getDouble();
        }

        return out;
    }

    /**
     * Decodes an 8-byte segment into a {@link RationalNumber} (numerator and denominator pair).
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @param type
     *        the numerical data type definition (signed or unsigned)
     * @return the decoded RationalNumber object
     */
    public static RationalNumber toRational(byte[] data, int offset, ByteOrder order, RationalNumber.DataType type)
    {
        ByteBuffer buf = buffer(data, offset, order);

        int num = buf.getInt();
        int den = buf.getInt();

        return new RationalNumber(num, den, type);
    }

    /**
     * Decodes a byte sequence into an array of {@link RationalNumber} objects.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position within the array
     * @param order
     *        the byte order {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @param type
     *        the numerical data type definition (signed or unsigned)
     * @return an array of decoded RationalNumber objects
     * 
     * @throws IllegalArgumentException
     *         if the remaining byte count is not a multiple of 8
     */
    public static RationalNumber[] toRationalArray(byte[] data, int offset, ByteOrder order, RationalNumber.DataType type)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 8 != 0)
        {
            throw new IllegalArgumentException("Remaining bytes (" + remaining + ") must be a multiple of 8.");
        }

        RationalNumber[] out = new RationalNumber[remaining / 8];

        for (int i = 0; i < out.length; i++)
        {
            int num = buf.getInt();
            int den = buf.getInt();
            out[i] = new RationalNumber(num, den, type);
        }

        return out;
    }

    /**
     * Reads the entire contents of a file path into a byte array.
     *
     * @path filePath
     *       the path to the file
     * @return a byte array containing the full file contents
     * 
     * @throws IOException
     *         if an I/O error occurs reading from the file
     */
    public static byte[] readAllBytes(Path filePath) throws IOException
    {
        try (InputStream in = Files.newInputStream(filePath))
        {
            return readAllBytes(in);
        }
    }

    /**
     * Reads all bytes from an input stream until the end of the stream is reached.
     * Note: This method does not close the input stream.
     *
     * @param stream
     *        the input stream to read from
     * @return a byte array containing all read bytes
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public static byte[] readAllBytes(InputStream stream) throws IOException
    {
        Objects.requireNonNull(stream, "InputStream cannot be null");

        byte[] buf = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int n;

        while ((n = stream.read(buf)) != -1)
        {
            out.write(buf, 0, n);
        }

        return out.toByteArray();
    }

    /**
     * Reads a null-terminated (0x00) string from the byte array using the specified charset.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the start position to begin reading
     * @param cs
     *        the character set to decode with
     * @return the decoded string up to the first null byte or array end
     */
    public static String readNullTerminatedString(byte[] data, int offset, Charset cs)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(cs, "Charset cannot be null");

        int end = offset;

        while (end < data.length && data[end] != 0)
        {
            end++;
        }

        return new String(data, offset, end - offset, cs);
    }

    /**
     * Checks whether a byte array contains at least one null byte (0x00).
     *
     * @param data
     *        the byte array to check
     * @return true if a null byte is found, false otherwise
     */
    public static boolean containsNullByte(byte[] data)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");

        for (byte b : data)
        {
            if (b == 0)
            {
                return true;
            }
        }

        return false;
    }

    private static ByteBuffer buffer(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset out of bounds: " + offset);
        }

        return ByteBuffer.wrap(data, offset, data.length - offset).order(order);
    }
}
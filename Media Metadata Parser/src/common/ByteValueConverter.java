package common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import tif.RationalNumber;

/**
 * Utility class providing static methods for converting raw byte arrays to primitive values,
 * arrays, strings, and {@link RationalNumber} objects, particularly for image metadata parsing
 * formats such as TIFF, Exif, PNG, HEIF, and WebP.
 *
 * <p>
 * This class is non-instantiable and thread-safe due to its stateless and static design.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public final class ByteValueConverter
{
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Default constructor is unsupported and will always throw an exception.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    private ByteValueConverter()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /* --- Byte reading utilities --- */

    /**
     * Decodes the first two bytes of a byte array into a 16-bit signed short value using the
     * specified byte order.
     *
     * @param data
     *        an array of 2 bytes or more
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded short value
     */
    public static short toShort(byte[] data, ByteOrder order)
    {
        return toShort(data, 0, order);
    }

    /**
     * Decodes two bytes starting at the specified offset into a 16-bit signed short value using the
     * specified byte order.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded signed short value
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     */
    public static short toShort(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset + 2 > data.length)
        {
            throw new IndexOutOfBoundsException("Offset is out of bounds");
        }

        int byte0 = data[offset + 0] & 0xFF;
        int byte1 = data[offset + 1] & 0xFF;

        if (order == ByteOrder.BIG_ENDIAN)
        {
            return (short) (byte0 << 8 | byte1);
        }

        return (short) (byte1 << 8 | byte0);
    }

    /**
     * Decodes the first two bytes of a byte array into a 16-bit unsigned short value using the
     * specified byte order.
     *
     * @param data
     *        an array of 2 bytes or more
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded unsigned short value, in the range {@code 0x0000} to {@code 0xFFFF} (0 to
     *         65535)
     */
    public static int toUnsignedShort(byte[] data, ByteOrder order)
    {
        return toUnsignedShort(data, 0, order);
    }

    /**
     * Decodes two bytes starting at the specified offset into a 16-bit unsigned short value using
     * the specified byte order.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded unsigned short value as an {@code int}, in the range {@code 0x0000} to
     *         {@code 0xFFFF} (0 to 65535)
     */
    public static int toUnsignedShort(byte[] data, int offset, ByteOrder order)
    {
        return toShort(data, offset, order) & 0xFFFF;
    }

    /**
     * Decodes the first four bytes of a byte array into a 32-bit signed integer value using the
     * specified byte order.
     *
     * @param data
     *        an array of 4 bytes or more
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded signed integer value
     */
    public static int toInteger(byte[] data, ByteOrder order)
    {
        return toInteger(data, 0, order);
    }

    /**
     * Decodes four bytes starting at the specified offset into a 32-bit signed integer value using
     * the specified byte order.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded signed integer value
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     */
    public static int toInteger(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset + 4 > data.length)
        {
            throw new IndexOutOfBoundsException("Offset is out of bounds");
        }

        int byte0 = data[offset + 0] & 0xFF;
        int byte1 = data[offset + 1] & 0xFF;
        int byte2 = data[offset + 2] & 0xFF;
        int byte3 = data[offset + 3] & 0xFF;

        if (order == ByteOrder.BIG_ENDIAN)
        {
            return byte0 << 24 | byte1 << 16 | byte2 << 8 | byte3;
        }

        else
        {
            return byte3 << 24 | byte2 << 16 | byte1 << 8 | byte0;
        }
    }

    /**
     * Decodes the first four bytes of a byte array into a 32-bit unsigned integer value using the
     * specified byte order.
     *
     * @param data
     *        an array of 4 bytes or more
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded unsigned integer value represented as a {@code long}, in the range of
     *         {@code 0x00000000} to {@code 0xFFFFFFFF} (0 to 4294967295)
     */
    public static long toUnsignedInteger(byte[] data, ByteOrder order)
    {
        return toUnsignedInteger(data, 0, order);
    }

    /**
     * Decodes four bytes starting at the specified offset into a 32-bit unsigned integer value
     * using the specified byte order.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded unsigned integer value represented as a {@code long}, in the range of
     *         {@code 0x00000000} to {@code 0xFFFFFFFF} (0 to 4294967295)
     */
    public static long toUnsignedInteger(byte[] data, int offset, ByteOrder order)
    {
        return Integer.toUnsignedLong(toInteger(data, offset, order));
    }

    /**
     * Decodes the first eight bytes of a byte array into a 64-bit signed long value using the
     * specified byte order.
     *
     * @param data
     *        an array of 8 bytes or more
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded signed long value
     */
    public static long toLong(byte[] data, ByteOrder order)
    {
        return toLong(data, 0, order);
    }

    /**
     * Decodes eight bytes starting at the specified offset into a 64-bit signed long value using
     * the specified byte order.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded long value
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     */
    public static long toLong(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset + 8 > data.length)
        {
            throw new IndexOutOfBoundsException("Invalid input for toLong: byte offset is out of bounds");
        }

        long byte0 = data[offset + 0] & 0xFFL;
        long byte1 = data[offset + 1] & 0xFFL;
        long byte2 = data[offset + 2] & 0xFFL;
        long byte3 = data[offset + 3] & 0xFFL;
        long byte4 = data[offset + 4] & 0xFFL;
        long byte5 = data[offset + 5] & 0xFFL;
        long byte6 = data[offset + 6] & 0xFFL;
        long byte7 = data[offset + 7] & 0xFFL;

        if (order == ByteOrder.BIG_ENDIAN)
        {
            return byte0 << 56 | byte1 << 48 | byte2 << 40 | byte3 << 32 | byte4 << 24 | byte5 << 16 | byte6 << 8 | byte7;
        }

        return byte7 << 56 | byte6 << 48 | byte5 << 40 | byte4 << 32 | byte3 << 24 | byte2 << 16 | byte1 << 8 | byte0;
    }

    /**
     * Decodes the first four bytes of a byte array into a 32-bit floating-point value using the
     * specified byte order.
     *
     * @param data
     *        an array of 4 bytes or more
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded floating-point value
     */
    public static float toFloat(byte[] data, ByteOrder order)
    {
        return toFloat(data, 0, order);
    }

    /**
     * Decodes four bytes starting at the specified offset into a 32-bit floating-point value using
     * the specified byte order.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded floating-point value
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     */
    public static float toFloat(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset + 4 > data.length)
        {
            throw new IndexOutOfBoundsException("Invalid input for toFloat: byte offset is out of bounds");
        }

        return Float.intBitsToFloat(toInteger(data, offset, order));
    }

    /**
     * Decodes the first eight bytes of a byte array into a 64-bit IEEE-754 double value using the
     * specified byte order.
     *
     * @param data
     *        an array of 8 bytes or more
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded double-precision value
     */
    public static double toDouble(byte[] data, ByteOrder order)
    {
        return toDouble(data, 0, order);
    }

    /**
     * Decodes eight bytes starting at the specified offset into a 64-bit IEEE-754 double value
     * using the specified byte order.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return the decoded double-precision value
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     */
    public static double toDouble(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset + 8 > data.length)
        {
            throw new IndexOutOfBoundsException("Invalid input for toDouble: byte offset is out of bounds");
        }

        return Double.longBitsToDouble(toLong(data, offset, order));
    }

    /**
     * Decodes the first 8 bytes of the input array into a {@link RationalNumber} object. The first
     * four bytes represent the numerator and the next four bytes represent the denominator.
     *
     * @param data
     *        the byte array containing the numerator and denominator values
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @param type
     *        whether the numerator and denominator should be interpreted as signed or unsigned
     *        values
     * @return a {@link RationalNumber} whose numerator and denominator are decoded from the input
     *         bytes
     */
    public static RationalNumber toRational(byte[] data, ByteOrder order, RationalNumber.DataType type)
    {
        return toRational(data, 0, order, type);
    }

    /**
     * Decodes an 8-byte segment from the input array into a {@link RationalNumber}, starting at the
     * specified offset. The first four bytes represent the numerator and the next four bytes
     * represent the denominator, interpreted according to the specified byte order and data type
     * (signed or unsigned).
     *
     * @param data
     *        the input byte array containing the numerator and denominator values
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @param type
     *        whether the numerator and denominator should be interpreted as signed or unsigned
     *        values
     * @return a {@link RationalNumber} whose numerator and denominator are decoded from the input
     *         bytes starting at the specified offset
     *
     * @throws NullPointerException
     *         if the input byte array, byte order, or rational data type is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     */
    public static RationalNumber toRational(byte[] data, int offset, ByteOrder order, RationalNumber.DataType type)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");
        Objects.requireNonNull(type, "Rational data type cannot be null");

        if (offset < 0 || offset + 8 > data.length)
        {
            throw new IndexOutOfBoundsException("Offset is out of bounds, or array is too short for 8 bytes starting at offset");
        }

        int numeratorRaw = toInteger(data, offset, order);
        int denominatorRaw = toInteger(data, offset + 4, order);

        return new RationalNumber(numeratorRaw, denominatorRaw, type);
    }

    /**
     * Convenience method for decoding an entire byte array into an array of 16-bit unsigned short
     * values.
     * 
     * @param data
     *        the input byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded unsigned short values
     */
    public static int[] toUnsignedShortArray(byte[] data, ByteOrder order)
    {
        return toUnsignedShortArray(data, 0, order);
    }

    /**
     * Convenience method for decoding a byte array into an array of 16-bit unsigned short values
     * starting at the specified offset.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the starting offset within the byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an {@code int} array containing the decoded unsigned short values in the range of
     *         {@code 0x0000} to {@code 0xFFFF} (0 to 65535)
     * 
     * @throws NullPointerException
     *         if the input byte array or byte order is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     * @throws IllegalArgumentException
     *         if the remaining byte count after the offset is not a multiple of 2
     */
    public static int[] toUnsignedShortArray(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset [" + offset + "] is out of bounds for array of length [" + data.length + "]");
        }

        if ((data.length - offset) % 2 != 0)
        {
            throw new IllegalArgumentException("Byte array length minus offset must be even to convert to unsigned short array");
        }

        int count = (data.length - offset) / 2;
        int[] result = new int[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = toUnsignedShort(data, offset + (i * 2), order);
        }

        return result;
    }

    /**
     * Convenience method for decoding an entire byte array into an array of 32-bit signed integer
     * values.
     *
     * @param data
     *        the input byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded signed integer values
     */
    public static int[] toIntegerArray(byte[] data, ByteOrder order)
    {
        return toIntegerArray(data, 0, order);
    }

    /**
     * Convenience method for decoding a byte array into an array of 32-bit signed integer values
     * starting at the specified offset.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded signed integer values
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is null
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     * @throws IllegalArgumentException
     *         if the remaining byte count after the offset is not a multiple of 4
     */
    public static int[] toIntegerArray(byte[] data, int offset, ByteOrder order)
    {
        final int dataSize = 4;

        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset [" + offset + "] is out of bounds for array of length [" + data.length + "]");
        }

        final int remainingLength = data.length - offset;

        if (remainingLength % dataSize != 0)
        {
            throw new IllegalArgumentException("Byte array length minus offset (" + remainingLength + ") must be a multiple of [" + dataSize + "] to convert to integer array");
        }

        int count = remainingLength / dataSize;
        int[] result = new int[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = toInteger(data, offset + (i * dataSize), order);
        }

        return result;
    }

    /**
     * Convenience method for decoding an entire byte array into an array of 64-bit long values.
     *
     * @param data
     *        the input byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded long values
     */
    public static long[] toLongArray(byte[] data, ByteOrder order)
    {
        return toLongArray(data, 0, order);
    }

    /**
     * Convenience method for decoding a byte array into an array of 64-bit signed long values
     * starting at the specified offset.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded long values
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is null
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     * @throws IllegalArgumentException
     *         if the remaining byte count after the offset is not a multiple of 8
     */
    public static long[] toLongArray(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset [" + offset + "] is out of bounds for array of length " + data.length);
        }

        int remaining = data.length - offset;

        if (remaining % 8 != 0)
        {
            throw new IllegalArgumentException("Byte array length minus offset [" + remaining + "] must be a multiple of [8] to convert to long array");
        }

        long[] result = new long[remaining / 8];

        for (int i = 0; i < result.length; i++)
        {
            result[i] = toLong(data, offset + (i * 8), order);
        }

        return result;
    }

    /**
     * Convenience method for decoding an entire byte array into an array of 32-bit floating-point
     * values.
     *
     * @param data
     *        the input byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded floating-point values
     */
    public static float[] toFloatArray(byte[] data, ByteOrder order)
    {
        return toFloatArray(data, 0, order);
    }

    /**
     * Convenience method for decoding a byte array into an array of 32-bit floating-point values
     * starting at the specified offset.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded floating-point values
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is null
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     * @throws IllegalArgumentException
     *         if the remaining byte count after the offset is not a multiple of 4
     */
    public static float[] toFloatArray(byte[] data, int offset, ByteOrder order)
    {
        final int dataSize = 4;

        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset [" + offset + "] is out of bounds for array of length [" + data.length + "]");
        }

        int remaining = data.length - offset;

        if (remaining % dataSize != 0)
        {
            throw new IllegalArgumentException("Byte array length minus offset [" + remaining + "] must be a multiple of [" + dataSize + "] to convert to float array");
        }

        int count = remaining / dataSize;
        float[] result = new float[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = toFloat(data, offset + (i * dataSize), order);
        }

        return result;
    }

    /**
     * Convenience method for decoding an entire byte array into an array of 64-bit double values.
     *
     * @param data
     *        the input byte array
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded double values
     */
    public static double[] toDoubleArray(byte[] data, ByteOrder order)
    {
        return toDoubleArray(data, 0, order);
    }

    /**
     * Convenience method for decoding a byte array into an array of 64-bit double values starting
     * from the specified offset.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @return an array of decoded double values
     *
     * @throws NullPointerException
     *         if the input byte array or byte order is null
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     * @throws IllegalArgumentException
     *         if the remaining byte count after the offset is not a multiple of 8
     */
    public static double[] toDoubleArray(byte[] data, int offset, ByteOrder order)
    {
        final int dataSize = 8;

        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset [" + offset + "] is out of bounds for array of length [" + data.length + "]");
        }

        int remaining = data.length - offset;

        if (remaining % dataSize != 0)
        {
            throw new IllegalArgumentException("Byte array length minus offset [" + remaining + "] must be a multiple of [" + dataSize + "] to convert to double array");
        }

        int count = remaining / dataSize;
        double[] result = new double[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = toDouble(data, offset + (i * dataSize), order);
        }

        return result;
    }

    /**
     * Converts a byte array into an array of {@link RationalNumber} objects, interpreting each
     * 8-byte segment as a numerator-denominator pair starting at the specified offset.
     *
     * @param data
     *        the input byte array
     * @param offset
     *        the offset within the byte array at which to start reading
     * @param order
     *        the byte order for interpreting the input bytes, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @param type
     *        whether the numerator and denominator values should be interpreted as signed or
     *        unsigned
     * @return an array of decoded {@link RationalNumber} objects
     *
     * @throws NullPointerException
     *         if the input byte array, byte order, or rational data type is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is out of bounds
     * @throws IllegalArgumentException
     *         if the remaining byte count after the offset is not a multiple of 8
     */
    public static RationalNumber[] toRationalArray(byte[] data, int offset, ByteOrder order, RationalNumber.DataType type)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");
        Objects.requireNonNull(type, "Rational data type cannot be null");

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset [" + offset + "] is out of bounds for array of length [" + data.length + "]");
        }

        if ((data.length - offset) % 8 != 0)
        {
            throw new IllegalArgumentException("Byte array length minus offset must be divisible by 8");
        }

        int count = (data.length - offset) / 8;
        RationalNumber[] result = new RationalNumber[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = toRational(data, offset + (i * 8), order, type);
        }

        return result;
    }

    /**
     * Packs a numerator and denominator into an 8-byte segment of a byte array as a TIFF RATIONAL,
     * honouring the specified byte order. The numerator occupies the first four bytes and the
     * denominator occupies the following four bytes.
     *
     * @param buf
     *        the target byte array
     * @param offset
     *        the offset within the byte array at which to start writing
     * @param num
     *        the numerator
     * @param div
     *        the denominator or divisor
     * @param order
     *        the byte order to use (BIG_ENDIAN or LITTLE_ENDIAN)
     * 
     * @throws NullPointerException
     *         if the input byte array or byte order is null
     * @throws IndexOutOfBoundsException
     *         if the offset is invalid or there is insufficient space in the buffer
     */
    public static void packRational(byte[] buf, int offset, int num, int div, ByteOrder order)
    {
        Objects.requireNonNull(buf, "Target buffer cannot be null");
        Objects.requireNonNull(order, "ByteOrder cannot be null");

        if (offset < 0 || offset + 8 > buf.length)
        {
            throw new IndexOutOfBoundsException("Offset is out of bounds for an 8-byte RATIONAL");
        }

        if (order == ByteOrder.BIG_ENDIAN)
        {
            // Numerator (Bytes 0-3)
            buf[offset + 0] = (byte) (num >> 24);
            buf[offset + 1] = (byte) (num >> 16);
            buf[offset + 2] = (byte) (num >> 8);
            buf[offset + 3] = (byte) num;

            // Denominator (Bytes 4-7)
            buf[offset + 4] = (byte) (div >> 24);
            buf[offset + 5] = (byte) (div >> 16);
            buf[offset + 6] = (byte) (div >> 8);
            buf[offset + 7] = (byte) div;
        }

        else
        {
            // Numerator (Bytes 0-3, Little Endian)
            buf[offset + 0] = (byte) num;
            buf[offset + 1] = (byte) (num >> 8);
            buf[offset + 2] = (byte) (num >> 16);
            buf[offset + 3] = (byte) (num >> 24);

            // Denominator (Bytes 4-7, Little Endian)
            buf[offset + 4] = (byte) div;
            buf[offset + 5] = (byte) (div >> 8);
            buf[offset + 6] = (byte) (div >> 16);
            buf[offset + 7] = (byte) (div >> 24);
        }
    }

    /* --- Stream/file utilities --- */

    /**
     * Converts a byte array to a hexadecimal string representation.
     *
     * @param data
     *        the input byte array
     * @return a hexadecimal string
     * 
     * @throws NullPointerException
     *         if the input byte array is null
     */
    public static String toHex(byte[] data)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");

        StringBuilder sb = new StringBuilder(data.length * 5);

        for (int j = 0; j < data.length; j++)
        {
            int v = data[j] & 0xFF;

            sb.append("0x").append(HEX_ARRAY[v >>> 4]).append(HEX_ARRAY[v & 0x0F]);

            if (j < data.length - 1)
            {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * Generates a hexadecimal table for debugging byte arrays.
     * 
     * <p>
     * Bytes are rendered as two-digit hexadecimal values and displayed in rows of sixteen bytes,
     * with a separator between the first and second group of eight bytes.
     * </p>
     *
     * @param values
     *        the byte array to format
     * @return a formatted hexadecimal table string
     *
     * @throws NullPointerException
     *         if the input byte array is {@code null}
     */
    public static String toHexTable(byte[] values)
    {
        int columns = 16;
        StringBuilder sb = new StringBuilder();

        Objects.requireNonNull(values, "Input data array cannot be null");

        for (int i = 0; i < values.length; i++)
        {
            if (i % columns == 0)
            {
                if (i > 0)
                {
                    sb.append(System.lineSeparator());
                }

                sb.append(String.format(Locale.ROOT, "%08X: ", i));
            }

            if (i % columns == 8)
            {
                sb.append("| ");
            }

            sb.append(String.format(Locale.ROOT, "%02X ", values[i] & 0xFF));
        }

        return sb.toString();
    }

    /**
     * Converts an array of integers into a raw byte array by casting each element to a single byte.
     * 
     * <p>
     * This conversion is appropriate for restoring 8-bit data, such as TIFF {@code TYPE_BYTE_U} or
     * {@code TYPE_BYTE_S}, that was previously expanded to integers. Values outside the range
     * representable by a byte are truncated to their low-order 8 bits through Java's standard
     * narrowing conversion.
     * </p>
     * 
     * @param values
     *        the source array of integers to convert
     * @return a byte array containing the cast values
     * 
     * @throws NullPointerException
     *         if the input integer array is null
     */
    public static byte[] castToByteArray(int[] values)
    {
        Objects.requireNonNull(values, "Input data array cannot be null");

        byte[] b = new byte[values.length];

        for (int i = 0; i < values.length; i++)
        {
            b[i] = (byte) values[i];
        }

        return b;
    }

    /**
     * Reads the entire contents of the file at the given {@link Path} into a byte array.
     * 
     * @param filePath
     *        a Path instance encapsulating the image file
     * @return a byte array of the file's raw contents, or empty if file is zero-length
     * 
     * @throws IOException
     *         if the file cannot be read
     */
    public static byte[] readAllBytes(Path filePath) throws IOException
    {
        try (InputStream inputStream = Files.newInputStream(filePath))
        {
            return readAllBytes(inputStream);
        }
    }

    /**
     * Reads all bytes from the specified {@link InputStream} resource and returns as a byte array.
     *
     * <p>
     * Internally, it uses an 8 KB buffer for efficient reading, making it suitable for large
     * streams such as files or network input.
     * </p>
     *
     * <p>
     * This method does not close the opened InputStream resource. The caller is responsible for
     * closing it.
     * </p>
     *
     *
     * @param stream
     *        the input stream to read from
     * @return a byte array containing all bytes read from the stream
     * 
     * @throws NullPointerException
     *         if the input stream is null
     * @throws IOException
     *         if an I/O error occurs while reading
     */
    public static byte[] readAllBytes(InputStream stream) throws IOException
    {
        int bytesRead;
        byte[] buffer = new byte[8192];

        Objects.requireNonNull(stream, "InputStream cannot be null");

        // TODO: Be aware of excessively high memory which could trigger an OOM
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            while ((bytesRead = stream.read(buffer)) != -1)
            {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }
    }

    /* String utilities */

    /**
     * Checks if the specified byte array contains a null {@code 0x00}) byte.
     * 
     * @param data
     *        the byte array to examine
     * @return {@code true} if a null byte ({@code 0x00}) is found, otherwise {@code false}
     * 
     * @throws NullPointerException
     *         if the input byte array is null
     */
    public static boolean containsNullByte(byte[] data)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");

        for (int i = 0; i < data.length; i++)
        {
            if (data[i] == 0)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts a byte array segment terminated by the first null ({@code 0x00}) byte.
     *
     * <p>
     * This method searches for the first occurrence of a null byte ({@code 0x00}) in the specified
     * {@code data} array. If a null byte is found, it returns a new array containing all bytes from
     * the beginning of {@code data} up to, but not including, the first null byte. Any bytes after
     * the first null terminator are ignored.
     * </p>
     *
     * If no null byte is found in the {@code data} array, a copy of the entire original
     * {@code data} array is returned.
     *
     * @param data
     *        the input byte array to be searched for a null terminator
     * @return a new byte array containing the segment before the first null terminator, or a copy
     *         of the entire original array if no null terminator is found
     * 
     * @throws NullPointerException
     *         if the input byte array is null
     */
    public static byte[] readFirstNullTerminatedByteArray(byte[] data)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");

        int nullIndex = data.length;

        for (int i = 0; i < data.length; i++)
        {
            if (data[i] == 0)
            {
                nullIndex = i;
                break;
            }
        }

        return Arrays.copyOf(data, nullIndex);
    }

    /**
     * Reads a string from a byte array starting at the specified offset. The string is terminated
     * by the first null ({@code 0x00}) byte encountered, or by the end of the array if no NUL byte
     * is present.
     *
     * @param data
     *        the source byte array
     * @param offset
     *        the starting index from which to read
     * @param charset
     *        the {@link Charset} used to decode the string
     * @return the decoded string consisting of the bytes between the specified offset and the first
     *         null byte (exclusive), or the end of the array if no null byte is present. Returns an
     *         empty string if the offset is equal to the array length
     *
     * @throws NullPointerException
     *         if the input byte array or charset is {@code null}
     * @throws IndexOutOfBoundsException
     *         if the offset is less than {@code 0} or greater than the array length
     */
    public static String readNullTerminatedString(byte[] data, int offset, Charset charset)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(charset, "Charset cannot be null");

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset out of bounds [" + offset + "]. Must be between 0 and [" + data.length + "]");
        }

        if (offset == data.length)
        {
            return "";
        }

        int pos = offset;

        while (pos < data.length && data[pos] != 0)
        {
            pos++;
        }

        return new String(Arrays.copyOfRange(data, offset, pos), charset);
    }

    /**
     * Splits a byte array of null-delimited strings into individual strings using UTF-8.
     *
     * @param data
     *        the byte array to split
     * @return an array containing each null-delimited string decoded using UTF-8. Empty strings are
     *         preserved when consecutive null bytes occur
     */
    public static String[] splitNullDelimitedStrings(byte[] data)
    {
        return splitNullDelimitedStrings(data, StandardCharsets.UTF_8);
    }

    /**
     * Splits a byte array of null-delimited strings into individual strings using the specified
     * charset. Consecutive null bytes produce empty string elements. If the byte array ends with a
     * null terminator, no additional trailing empty string is added.
     *
     * @param data
     *        the byte array to split
     * @param charset
     *        the charset used to decode each string
     * @return an array of strings
     * 
     * @throws NullPointerException
     *         if the input byte array or charset is null
     */
    public static String[] splitNullDelimitedStrings(byte[] data, Charset charset)
    {
        Objects.requireNonNull(data, "Input data array cannot be null");
        Objects.requireNonNull(charset, "Charset cannot be null");

        int start = 0;
        List<String> result = new ArrayList<>();

        for (int i = 0; i < data.length; i++)
        {
            if (data[i] == 0)
            {
                result.add(new String(data, start, i - start, charset));
                start = i + 1;
            }
        }

        /*
         * Capture remaining data only if the array didn't end with a null.
         * This prevents adding an extra "" if the array was properly terminated.
         */
        if (start < data.length)
        {
            result.add(new String(data, start, data.length - start, charset));
        }

        return result.toArray(new String[0]);
    }

    @Deprecated
    public static RationalNumber[] toRationalArray(byte[] data, ByteOrder order, RationalNumber.DataType type)
    {
        return toRationalArray(data, 0, order, type);
    }

    // TODO: Maybe add public static long[] toUnsignedIntegerArray(byte[] data, int offset,
    // ByteOrder order)
}
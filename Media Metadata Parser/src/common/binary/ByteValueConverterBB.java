package common.binary;

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
 * ByteBuffer-backed utility for decoding primitive values and structures from byte arrays.
 *
 * This version reduces duplication by delegating all primitive conversions to a shared
 * ByteBuffer wrapper with configured byte order.
 */
public final class ByteValueConverterBB
{

    private ByteValueConverterBB()
    {
        throw new UnsupportedOperationException();
    }

    /*
     * =========================================================
     * Internal helper
     * =========================================================
     */

    private static ByteBuffer buffer(byte[] data, int offset, ByteOrder order)
    {
        Objects.requireNonNull(data);
        Objects.requireNonNull(order);

        if (offset < 0 || offset > data.length)
        {
            throw new IndexOutOfBoundsException("Offset out of bounds: " + offset);
        }

        return ByteBuffer.wrap(data, offset, data.length - offset).order(order);
    }

    /*
     * =========================================================
     * Primitive decoding (no manual bit ops anymore)
     * =========================================================
     */

    public static short toShort(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getShort();
    }

    public static int toUnsignedShort(byte[] data, int offset, ByteOrder order)
    {
        return Short.toUnsignedInt(toShort(data, offset, order));
    }

    public static int toInteger(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getInt();
    }

    public static long toUnsignedInteger(byte[] data, int offset, ByteOrder order)
    {
        return Integer.toUnsignedLong(toInteger(data, offset, order));
    }

    public static long toLong(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getLong();
    }

    public static float toFloat(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getFloat();
    }

    public static double toDouble(byte[] data, int offset, ByteOrder order)
    {
        return buffer(data, offset, order).getDouble();
    }

    /*
     * =========================================================
     * Array decoding (now much smaller + uniform)
     * =========================================================
     */

    public static int[] toIntegerArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 4 != 0)
        {
            throw new IllegalArgumentException();
        }

        int[] out = new int[remaining / 4];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getInt();
        }

        return out;
    }

    public static long[] toLongArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 8 != 0)
        {
            throw new IllegalArgumentException();
        }

        long[] out = new long[remaining / 8];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getLong();
        }

        return out;
    }

    public static float[] toFloatArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 4 != 0)
        {
            throw new IllegalArgumentException();
        }

        float[] out = new float[remaining / 4];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getFloat();
        }

        return out;
    }

    public static double[] toDoubleArray(byte[] data, int offset, ByteOrder order)
    {
        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();
        if (remaining % 8 != 0)
        {
            throw new IllegalArgumentException();
        }

        double[] out = new double[remaining / 8];

        for (int i = 0; i < out.length; i++)
        {
            out[i] = buf.getDouble();
        }

        return out;
    }

    /*
     * =========================================================
     * Rational (cleaner than manual shifting)
     * =========================================================
     */

    public static RationalNumber toRational(byte[] data, int offset, ByteOrder order, RationalNumber.DataType type)
    {
        ByteBuffer buf = buffer(data, offset, order);

        int num = buf.getInt();
        int den = buf.getInt();

        return new RationalNumber(num, den, type);
    }

    public static RationalNumber[] toRationalArray(byte[] data, int offset, ByteOrder order, RationalNumber.DataType type)
    {

        ByteBuffer buf = buffer(data, offset, order);
        int remaining = buf.remaining();

        if (remaining % 8 != 0)
        {
            throw new IllegalArgumentException();
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

    /*
     * =========================================================
     * Remaining logic (unchanged, still useful as-is)
     * =========================================================
     */

    public static byte[] readAllBytes(Path filePath) throws IOException
    {
        try (InputStream in = Files.newInputStream(filePath))
        {
            return readAllBytes(in);
        }
    }

    public static byte[] readAllBytes(InputStream stream) throws IOException
    {
        Objects.requireNonNull(stream);

        byte[] buf = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int n;

        while ((n = stream.read(buf)) != -1)
        {
            out.write(buf, 0, n);
        }

        return out.toByteArray();
    }

    /*
     * =========================================================
     * String utilities unchanged (ByteBuffer doesn't help much here)
     * =========================================================
     */

    public static String readNullTerminatedString(byte[] data, int offset, Charset cs)
    {
        Objects.requireNonNull(data);
        Objects.requireNonNull(cs);

        int end = offset;

        while (end < data.length && data[end] != 0)
        {
            end++;
        }

        return new String(data, offset, end - offset, cs);
    }

    public static boolean containsNullByte(byte[] data)
    {
        for (byte b : data)
        {
            if (b == 0)
            {
                return true;
            }
        }

        return false;
    }
}
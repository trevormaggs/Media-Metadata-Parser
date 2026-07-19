package tif;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import common.ByteValueConverter;
import tif.DirectoryIFD.EntryIFD;
import util.SmartDateParser;

/**
 * Utility methods for converting parsed TIFF metadata values into strongly-typed Java
 * representations.
 *
 * <p>
 * Supports conversion of numeric, textual, and rational metadata values commonly found in TIFF,
 * EXIF, DNG, and related image formats.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 20 June 2026
 */
public final class TagValueTranslator
{
    private static final int ENCODING_HEADER_LENGTH = 8;
    private static final Map<String, Charset> ENCODING_MAP;

    static
    {
        ENCODING_MAP = new HashMap<>();
        ENCODING_MAP.put("ASCII\0\0\0", StandardCharsets.US_ASCII);
        ENCODING_MAP.put("UTF-8\0\0\0", StandardCharsets.UTF_8);
        ENCODING_MAP.put("\0\0\0\0\0\0\0\0", StandardCharsets.UTF_8);
        ENCODING_MAP.put("JIS\0\0\0\0\0", Charset.forName("Shift_JIS"));
        ENCODING_MAP.put("UNICODE\0", StandardCharsets.UTF_16LE);
    }

    /**
     * Default constructor will always throw an exception.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    private TagValueTranslator()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Returns the value as a byte.
     *
     * @param data
     *        the value to convert
     * @return the byte value, or {@code 0} if the value is not a {@code Byte}
     */
    public static byte toByteValue(Object data)
    {
        if (data instanceof Byte)
        {
            return (byte) data;
        }

        return 0;
    }

    /**
     * Returns the value as a byte array.
     *
     * @param data
     *        the value to convert
     * @return the byte array, or an empty array if the value is not a {@code byte[]}
     */
    public static byte[] toByteArray(Object data)
    {
        if (data instanceof byte[])
        {
            return (byte[]) data;
        }

        return new byte[0];
    }

    /**
     * Returns the first value contained in an IFD entry as an integer.
     *
     * @param entry
     *        the entry to evaluate
     * @return the first integer value represented by the entry
     *
     * @throws IllegalArgumentException
     *         if the entry is null, exceeds the 32-bit integer range, or does not contain a usable
     *         integer value
     */
    public static int getIntValue(EntryIFD entry)
    {
        if (entry != null)
        {
            if (!canConvertToInt(entry.getFieldType()))
            {
                throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] exceeds 32-bit signed range in directory [%s]",
                        entry.getTag(),
                        entry.getTagID(),
                        entry.getTag().getDirectoryType().getDescription()));
            }

            int[] arr = toIntArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable integer data elements");
            }

            return arr[0];
        }

        throw new IllegalArgumentException("Cannot convert to an integer value from a null EntryIFD");
    }

    /**
     * Returns all values contained in an IFD entry as an integer array.
     *
     * @param entry
     *        the entry to evaluate
     * @return the integer values represented by the entry
     *
     * @throws IllegalArgumentException
     *         if the entry is null, exceeds the 32-bit integer range, or does not contain usable
     *         integer values
     */
    public static int[] getIntArray(EntryIFD entry)
    {
        if (entry != null)
        {
            if (!canConvertToInt(entry.getFieldType()))
            {
                throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] exceeds 32-bit signed range in directory [%s]",
                        entry.getTag(),
                        entry.getTagID(),
                        entry.getTag().getDirectoryType().getDescription()));
            }

            int[] arr = toIntArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable integer data elements");
            }

            return arr;
        }

        throw new IllegalArgumentException("Cannot convert to an integer array from a null EntryIFD");

    }

    /**
     * Converts a value to an integer array.
     *
     * <p>
     * Supports TIFF integer field types that can be represented as 32-bit integers. Unsigned BYTE
     * and SHORT values are widened to preserve their unsigned values.
     * </p>
     *
     * <p>
     * Values that require more than 32 bits, such as {@code Long} and {@code long[]}, are not
     * converted.
     * </p>
     *
     * @param data
     *        the value to convert
     * @return an integer array, an empty array if the value is {@code null} or cannot be
     *         represented as an integer, or {@code null} if the type is unsupported
     */
    public static int[] toIntArray(Object data)
    {
        int[] result = null;

        if (data == null || data instanceof Long || data instanceof long[])
        {
            result = new int[0];
        }

        else if (data instanceof Byte)
        {
            result = new int[]{Byte.toUnsignedInt((byte) data)};
        }

        else if (data instanceof byte[])
        {
            byte[] val = (byte[]) data;

            result = new int[val.length];

            for (int i = 0; i < result.length; i++)
            {
                result[i] = Byte.toUnsignedInt(val[i]);
            }
        }

        else if (data instanceof Short)
        {
            result = new int[]{((Short) data).intValue() & 0xFFFF};
        }

        else if (data instanceof short[])
        {
            short[] val = (short[]) data;

            result = new int[val.length];

            for (int i = 0; i < result.length; i++)
            {
                result[i] = val[i] & 0xFFFF;
            }
        }

        else if (data instanceof Integer)
        {
            result = new int[]{(int) data};
        }

        else if (data instanceof int[])
        {
            result = (int[]) data;
        }

        return result;
    }

    /**
     * Returns the first numeric value contained in an IFD entry as a {@code long}.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the first value represented by the entry as a {@code long}
     *
     * @throws IllegalArgumentException
     *         if {@code entry} is {@code null}, or if the entry does not contain any convertible
     *         numeric values
     */
    public static long getLongValue(EntryIFD entry)
    {
        if (entry != null)
        {
            long[] arr = toLongArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable long data elements");
            }

            return arr[0];
        }

        throw new IllegalArgumentException("Cannot convert to a long value from a null EntryIFD");
    }

    /**
     * Returns all 64-bit numerical values contained within an IFD entry as a long array.
     *
     * @param entry
     *        the entry to evaluate
     * @return all values represented by the entry as a long array
     *
     * @throws IllegalArgumentException
     *         if {@code entry} is {@code null}, or if the entry does not contain any convertible
     *         numeric values
     */
    public static long[] getLongArray(EntryIFD entry)
    {
        if (entry != null)
        {
            long[] arr = toLongArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable long data elements");
            }

            return arr;
        }

        throw new IllegalArgumentException("Cannot convert to a long array from a null EntryIFD");
    }

    /**
     * Converts a value to a long array.
     *
     * <p>
     * Supports TIFF integer values, numeric types, and rational values that can be represented as
     * {@code long}.
     * </p>
     *
     * @param data
     *        the value to convert
     * @return a long array, an empty array if {@code data} is {@code null}, or {@code null} if the
     *         type is unsupported
     */
    public static long[] toLongArray(Object data)
    {
        long[] result = null;

        if (data == null)
        {
            result = new long[0];
        }

        else if (data instanceof Byte)
        {
            result = new long[]{Byte.toUnsignedLong((byte) data)};
        }

        else if (data instanceof byte[])
        {
            byte[] val = (byte[]) data;

            result = new long[val.length];

            for (int i = 0; i < val.length; i++)
            {
                result[i] = Byte.toUnsignedLong(val[i]);
            }
        }

        else if (data instanceof Short)
        {
            result = new long[]{((Short) data).intValue() & 0xFFFFL};
        }

        else if (data instanceof short[])
        {
            short[] val = (short[]) data;

            result = new long[val.length];

            for (int i = 0; i < val.length; i++)
            {
                result[i] = val[i] & 0xFFFFL;
            }
        }

        else if (data instanceof Integer)
        {
            result = new long[]{Integer.toUnsignedLong((int) data)};
        }

        else if (data instanceof int[])
        {
            int[] val = (int[]) data;

            result = new long[val.length];

            for (int i = 0; i < val.length; i++)
            {
                result[i] = Integer.toUnsignedLong(val[i]);
            }
        }

        else if (data instanceof Long)
        {
            result = new long[]{(long) data};
        }

        else if (data instanceof long[])
        {
            result = (long[]) data;
        }

        else if (data instanceof RationalNumber)
        {
            result = new long[]{((RationalNumber) data).longValue()};
        }

        else if (data instanceof RationalNumber[])
        {
            RationalNumber[] rn = (RationalNumber[]) data;

            result = new long[rn.length];

            for (int i = 0; i < rn.length; i++)
            {
                result[i] = rn[i].longValue();
            }
        }

        else if (data instanceof Number)
        {
            result = new long[]{((Number) data).longValue()};
        }

        return result;
    }

    /**
     * Returns the first value contained in an IFD entry as a scalar float.
     *
     * @param entry
     *        the entry to evaluate
     * @return the first value represented by the entry as a {@code float}
     *
     * @throws IllegalArgumentException
     *         if {@code entry} is {@code null}, or if the entry does not contain any convertible
     *         numeric values
     */
    public static float getFloatValue(EntryIFD entry)
    {
        if (entry != null)
        {
            float[] arr = toFloatArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable float data elements");
            }

            return arr[0];
        }

        throw new IllegalArgumentException("Cannot convert to a float value from a null EntryIFD");
    }

    /**
     * Extracts all single-precision decimal values contained within an IFD entry as a float array.
     *
     * @param entry
     *        the entry to evaluate
     * @return the array of floats containing the entry data elements
     *
     * @throws IllegalArgumentException
     *         if the entry is null, or contains a value that cannot be converted
     */
    public static float[] getFloatArray(EntryIFD entry)
    {

        if (entry != null)
        {
            float[] arr = toFloatArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable float data elements");
            }

            return arr;
        }

        throw new IllegalArgumentException("Cannot convert to a float array from a null EntryIFD");
    }

    /**
     * Converts a value to a float array.
     *
     * <p>
     * Supports floating-point, rational, and other numeric values that can be represented as
     * {@code float}.
     * </p>
     *
     * @param data
     *        the value to convert
     * @return a float array, an empty array if {@code data} is {@code null}, or {@code null} if the
     *         type is unsupported
     */
    public static float[] toFloatArray(Object data)
    {
        float[] result = null;

        if (data == null)
        {
            result = new float[0];
        }

        else if (data instanceof Float)
        {
            result = new float[]{(float) data};
        }

        else if (data instanceof float[])
        {
            result = (float[]) data;
        }

        else if (data instanceof RationalNumber)
        {
            result = new float[]{((RationalNumber) data).floatValue()};
        }

        else if (data instanceof RationalNumber[])
        {
            RationalNumber[] rn = (RationalNumber[]) data;

            result = new float[rn.length];

            for (int i = 0; i < rn.length; i++)
            {
                result[i] = rn[i].floatValue();
            }
        }

        else if (data instanceof Number)
        {
            result = new float[]{((Number) data).floatValue()};
        }

        return result;
    }

    /**
     * Returns the first value contained in an IFD entry as a scalar double.
     *
     * @param entry
     *        the entry to evaluate
     * @return the first double value represented by the entry
     *
     * @throws IllegalArgumentException
     *         if {@code entry} is {@code null}, or if the entry does not contain any convertible
     *         numeric values
     */
    public static double getDoubleValue(EntryIFD entry)
    {

        if (entry != null)
        {
            double[] arr = toDoubleArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable double data elements");
            }

            return arr[0];
        }

        throw new IllegalArgumentException("Cannot convert to a double value from a null EntryIFD");
    }

    /**
     * Extracts all double-precision decimal values contained within an IFD entry as a double array.
     *
     * @param entry
     *        the entry to evaluate
     * @return the array of doubles containing the entry data elements
     *
     * @throws IllegalArgumentException
     *         if the entry is null, or contains a value that cannot be converted
     */
    public static double[] getDoubleArray(EntryIFD entry)
    {
        if (entry != null)
        {
            double[] arr = toDoubleArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable double data elements");
            }

            return arr;
        }

        throw new IllegalArgumentException("Cannot convert to a double value from a null EntryIFD");
    }

    /**
     * Converts the specified value to an array of doubles.
     *
     * <p>
     * Supports floating-point values, rational values, and other numeric types that can be
     * represented as {@code double}.
     * </p>
     *
     * @param data
     *        the value to convert
     * @return a double array, an empty array if {@code data} is {@code null}, or {@code null} if
     *         the value type is unsupported
     */
    public static double[] toDoubleArray(Object data)
    {
        double[] result = null;

        if (data == null)
        {
            result = new double[0];
        }

        else if (data instanceof Double)
        {
            result = new double[]{(double) data};
        }

        else if (data instanceof double[])
        {
            result = (double[]) data;
        }

        else if (data instanceof RationalNumber)
        {
            result = new double[]{((RationalNumber) data).doubleValue()};
        }

        else if (data instanceof RationalNumber[])
        {
            RationalNumber[] rn = (RationalNumber[]) data;

            result = new double[rn.length];

            for (int i = 0; i < rn.length; i++)
            {
                result[i] = rn[i].doubleValue();
            }
        }

        else if (data instanceof Number)
        {
            result = new double[]{((Number) data).doubleValue()};
        }

        return result;
    }

    /**
     * Returns the first value contained in an IFD entry as a {@link RationalNumber}.
     *
     * @param entry
     *        the entry to evaluate
     * @return the first rational value represented by the entry
     *
     * @throws IllegalArgumentException
     *         if {@code entry} is {@code null}, or if the entry does not contain any usable
     *         rational values
     */
    public static RationalNumber getRationalValue(EntryIFD entry)
    {
        if (entry != null)
        {
            RationalNumber[] arr = toRationalArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable rational data elements");
            }

            return arr[0];
        }

        throw new IllegalArgumentException("Cannot convert to a rational value from a null EntryIFD");
    }

    /**
     * Extracts all fractional objects contained within an IFD entry as a RationalNumber array.
     *
     * @param entry
     *        the entry to evaluate
     * @return the rational values represented by the entry
     *
     * @throws IllegalArgumentException
     *         if the entry is null, or does not contain any usable rational values
     */
    public static RationalNumber[] getRationalArray(EntryIFD entry)
    {
        if (entry != null)
        {
            RationalNumber[] arr = toRationalArray(entry.getData());

            if (arr == null || arr.length == 0)
            {
                throw new IllegalArgumentException("Tag [" + entry.getTag() + "] contains no usable rational data elements");
            }

            return arr;
        }

        throw new IllegalArgumentException("Cannot convert to a rational array from a null EntryIFD");
    }

    /**
     * Converts a value to a rational number array.
     *
     * <p>
     * Supports {@link RationalNumber} instances and arrays of rational numbers.
     * </p>
     *
     * @param data
     *        the value to convert
     * @return a rational number array, an empty array if {@code data} is {@code null}, or
     *         {@code null} if the type is unsupported
     */
    public static RationalNumber[] toRationalArray(Object data)
    {
        RationalNumber[] result = null;

        if (data == null)
        {
            result = new RationalNumber[0];
        }

        else if (data instanceof RationalNumber)
        {
            result = new RationalNumber[]{(RationalNumber) data};
        }

        else if (data instanceof RationalNumber[])
        {
            result = (RationalNumber[]) data;
        }

        return result;
    }

    /**
     * Returns an IFD entry's value as a readable text representation.
     *
     * @param entry
     *        the entry to convert
     * @return a formatted string representation of the entry value
     */
    public static String toStringValue(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot convert string value from a null EntryIFD");
        }

        return toStringValue(entry.getData(), entry.getTag().getHint());
    }

    /**
     * Converts a raw metadata payload value into a trimmed human-readable string representation.
     *
     * @param data
     *        the raw metadata object value to evaluate
     * @param hint
     *        the structural formatting hint applied to this tag, or {@code null} if none
     * @return a formatted text string representation of the data payload
     */
    public static String toStringValue(Object data, TagHint hint)
    {
        String result = "";

        if (data == null)
        {
            result = "";
        }

        else if (hint == TagHint.HINT_BYTE_STREAM)
        {
            result = "Binary Data Stream";

            if (data != null)
            {
                int byteLength = (data instanceof byte[] ? ((byte[]) data).length : data instanceof int[] ? ((int[]) data).length * 4 : 0);

                if (byteLength > 0)
                {
                    result = String.format(Locale.ROOT, "Binary Data: %d bytes", byteLength);
                }
            }
        }

        else if (hint == TagHint.HINT_UCS2)
        {
            int[] val = toIntArray(data);

            if (val == null || val.length == 0)
            {
                result = "";
            }

            else
            {
                byte[] arr = ByteValueConverter.castToByteArray(val);
                result = new String(arr, StandardCharsets.UTF_16LE).replace("\u0000", "").trim();
            }
        }

        else if (hint == TagHint.HINT_DATE)
        {
            String dt = ((String) data).trim();
            ZonedDateTime zdt = toZonedDateTime(data);

            // zdt.format(DateTimeFormatter.ofPattern("dd MMM yyyy @ HH:mm:ss z"))
            result = (zdt != null) ? SmartDateParser.convertToLocalisedDateTime(dt, Locale.getDefault()) : dt;
        }

        else if (hint == TagHint.HINT_MASK)
        {
            result = "[Masked items]";
        }

        else if (data instanceof byte[])
        {
            byte[] arr = (byte[]) data;

            if (hint == TagHint.HINT_BYTE)
            {
                result = ByteValueConverter.toHex(arr);
            }

            else if (hint == TagHint.HINT_ENCODED_STRING)
            {
                result = decodeUserComment(arr);
            }

            else
            {
                result = new String(ByteValueConverter.readFirstNullTerminatedByteArray(arr), StandardCharsets.UTF_8).trim();
            }
        }

        else if (data instanceof Short || data instanceof short[] || data instanceof Integer || data instanceof int[])
        {
            result = decodeIntArray(data);
        }

        else if (data instanceof Long || data instanceof long[])
        {
            long[] arr = toLongArray(data);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < arr.length; i++)
            {
                sb.append(arr[i]).append(i < arr.length - 1 ? " " : "");
            }

            result = sb.toString();
        }

        else if (data instanceof RationalNumber || data instanceof RationalNumber[])
        {
            StringBuilder sb = new StringBuilder();
            RationalNumber[] arr = toRationalArray(data);

            for (int i = 0; i < arr.length; i++)
            {
                RationalNumber r = arr[i];

                if (r == null)
                {
                    sb.append("0.0");
                }

                else if (r.divisor == 0)
                {
                    sb.append("Unknown");
                }

                else
                {
                    /*
                     * Make numbers look like standard camera settings. Tiny decimal numbers, such
                     * as 0.004, are flipped into fractions, such as 1/250, so they read like normal
                     * shutter speeds. Likewise for a lens aperture of f1.6, just print normally as
                     * decimals (1.6) without conversion.
                     */
                    double value = r.doubleValue();
                    double reciprocal = (value > 0.0 ? 1.0 / value : 0.0);
                    boolean flip = (value > 0.0 && value < 1.0
                            && Math.abs(reciprocal - Math.rint(reciprocal)) < 0.0001);

                    if (flip)
                    {
                        sb.append("1/").append((long) Math.rint(reciprocal));
                    }

                    else
                    {
                        sb.append(r.toSimpleString(true));
                    }
                }

                if (i < arr.length - 1)
                {
                    sb.append(" ");
                }
            }

            result = sb.toString();
        }

        else if (data instanceof Float || data instanceof float[])
        {
            float[] arr = toFloatArray(data);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < arr.length; i++)
            {
                sb.append(formatNumericValue(arr[i])).append(i < arr.length - 1 ? " " : "");
            }

            result = sb.toString();
        }

        else if (data instanceof Double || data instanceof double[])
        {
            double[] arr = toDoubleArray(data);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < arr.length; i++)
            {
                sb.append(formatNumericValue(arr[i])).append(i < arr.length - 1 ? " " : "");
            }

            result = sb.toString();
        }

        else if (data instanceof String)
        {
            result = ((String) data).trim();
        }

        else
        {
            result = data.toString().trim();
        }

        return result;
    }

    /**
     * Parses the entry data into a {@link ZonedDateTime} object. This method supports ISO-8601 and
     * various regional patterns via the {@link SmartDateParser}.
     *
     * @param entry
     *        the entry to parse
     * @return a parsed {@link ZonedDateTime} object
     *
     * @throws IllegalArgumentException
     *         if the entry does not contain a valid date hint, the format is invalid, the entry is
     *         null or not convertible to a valid date representation
     */
    public static ZonedDateTime getZonedDateTime(EntryIFD entry)
    {
        if (entry != null)
        {
            if (entry.getTag() == null || entry.getTag().getHint() != TagHint.HINT_DATE)
            {
                throw new IllegalArgumentException("Mismatched date format or missing HINT_DATE in entry [" + entry.getTag() + "]");
            }

            return toZonedDateTime(entry.getData());
        }

        throw new IllegalArgumentException("Cannot convert to ZonedDateTime value from a null EntryIFD");
    }

    /**
     * Converts a value to a {@link ZonedDateTime}.
     *
     * @param data
     *        the value to convert
     * @return the parsed {@link ZonedDateTime}, or {@code null} if the data is not a string or
     *         cannot be parsed as a date/time
     */
    public static ZonedDateTime toZonedDateTime(Object data)
    {
        if (data instanceof String)
        {
            return SmartDateParser.convertToZonedDateTime((String) data);
        }

        return null;
    }

    /**
     * Decodes text from an Exif tag, such as UserComment, that begins with an 8-byte character
     * encoding header.
     *
     * <p>
     * For UNICODE comments, the Exif specification stores text as UCS-2/UTF-16 using the file's
     * byte order and does not require a Byte Order Mark (BOM). However, some software, including
     * Adobe Photoshop and Windows Explorer, writes a BOM immediately after the encoding header.
     * </p>
     *
     * <p>
     * Java does not automatically remove a BOM when decoding with a fixed UTF-16 charset. To avoid
     * a leading {@code '\uFEFF'} phantom character appearing in the decoded text, this method
     * detects and skips BOM markers ({@code 0xFEFF} or {@code 0xFFFE}) and adjusts the character
     * encoding accordingly.
     * </p>
     *
     * <p>
     * If no BOM is present, the payload is decoded as UTF-16LE for compatibility with commonly
     * encountered Exif files.
     * </p>
     *
     * @param data
     *        the raw byte array from the Exif metadata tag
     * @return the decoded and trimmed text string, or an empty string if the input is invalid
     */
    public static String decodeUserComment(byte[] data)
    {
        if (data != null && data.length >= ENCODING_HEADER_LENGTH)
        {
            String headerKey = new String(data, 0, ENCODING_HEADER_LENGTH, StandardCharsets.US_ASCII);
            Charset charset = ENCODING_MAP.get(headerKey);

            if (charset == null)
            {
                charset = StandardCharsets.UTF_8;
            }

            int payloadOffset = ENCODING_HEADER_LENGTH;
            int payloadLength = data.length - ENCODING_HEADER_LENGTH;

            if (charset.equals(StandardCharsets.UTF_16LE) && payloadLength >= 2)
            {
                int b1 = data[ENCODING_HEADER_LENGTH] & 0xFF;
                int b2 = data[ENCODING_HEADER_LENGTH + 1] & 0xFF;

                /* Detect and skip an optional UTF-16 BOM. */
                if (b1 == 0xFE && b2 == 0xFF)
                {
                    charset = StandardCharsets.UTF_16BE;
                    payloadOffset += 2;
                    payloadLength -= 2;
                }

                else if (b1 == 0xFF && b2 == 0xFE)
                {
                    charset = StandardCharsets.UTF_16LE;
                    payloadOffset += 2;
                    payloadLength -= 2;
                }
            }

            String decoded = new String(data, payloadOffset, payloadLength, charset);

            return decoded.replace("\0", "").trim();
        }

        return "";
    }

    /**
     * Determines whether values stored using the specified TIFF field type can be represented
     * without loss in a 32-bit signed integer.
     *
     * @param type
     *        the TIFF field type to evaluate
     * @return {@code true} if the field type can be safely converted to an {@code int}, otherwise
     *         {@code false}
     */
    public static boolean canConvertToInt(TifFieldType type)
    {
        if (type != null)
        {
            switch (type)
            {
                case TYPE_BYTE_U:
                case TYPE_BYTE_S:
                case TYPE_SHORT_U:
                case TYPE_SHORT_S:
                case TYPE_LONG_S:
                    return true;

                default:
                    return false;
            }
        }

        return false;
    }

    /**
     * Formats a numeric value as a readable string.
     *
     * <p>
     * Whole numbers are displayed without a decimal point. Decimal values are rounded to four
     * decimal places, with trailing zeros removed. Special values such as {@code NaN} and infinity
     * are returned unchanged.
     * </p>
     *
     * @param d
     *        the value to format
     * @return the formatted string representation
     */
    private static String formatNumericValue(double d)
    {
        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            return String.valueOf(d);
        }

        if (d == (long) d)
        {
            return String.format(Locale.ROOT, "%d", (long) d);
        }

        return String.format(Locale.ROOT, "%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /**
     * Converts an integer value or integer array into a space-separated string.
     *
     * @param val
     *        the value to convert
     * @return a space-separated string of integer values, or an empty string if no values are
     *         available
     */
    private static String decodeIntArray(Object val)
    {
        int[] arr = toIntArray(val);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++)
        {
            sb.append(arr[i]);

            if (i < arr.length - 1)
            {
                sb.append(" ");
            }
        }

        return sb.toString();
    }
}
package tif;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import common.ByteValueConverter;
import logger.LogFactory;
import tif.DirectoryIFD.EntryIFD;
import tif.tagspecs.Taggable;
import util.SmartDateParser;

/**
 * Utility class for converting {@link EntryIFD} values into human-readable or numeric forms.
 * Hardened against UTF-16 premature truncation and heap allocation thrashing.
 *
 * @author Trevor Maggs
 * @version 1.4
 * @since 20 May 2026
 */
public final class TagValueConverter
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TagValueConverter.class);
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
    private TagValueConverter()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Checks if a TIFF field type can be losslessly converted to a 32-bit signed integer.
     *
     * @param type
     *        the TIFF field type to evaluate
     * @return {@code true} if the type is compatible with 32-bit signed integers
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
     * Returns the value as an integer.
     *
     * <p>
     * <b>Note:</b> It also explicitly handles unsigned promotion to prevent sign-extension bugs.
     * </p>
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the integer value
     *
     * @throws IllegalArgumentException
     *         if the field type is incompatible with 32-bit integers or the data is not numeric
     */
    public static int getIntValue(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract integer value from a null EntryIFD");
        }

        if (!canConvertToInt(entry.getFieldType()))
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] exceeds 32-bit signed range in directory [%s]",
                    entry.getTag(),
                    entry.getTagID(),
                    entry.getTag().getDirectoryType().getDescription()));
        }

        int[] arr = getIntArray(entry);

        if (arr.length == 0)
        {
            throw new IllegalArgumentException("Entry contains no usable integer data elements or encountered unexpected 64-bit value conversion");
        }

        return arr[0];
    }

    /**
     * Returns the value as an integer array.
     *
     * <p>
     * This method provides resilient handling by returning direct {@code int[]} representations,
     * promoting single {@code Integer}, {@code Short}, or {@code Byte} scalars to arrays, and
     * performing unsigned promotion for 8-bit and 16-bit types.
     * </p>
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return an array of integers, or an empty array if the data is incompatible
     *
     * @throws IllegalArgumentException
     *         if the entry is null or does not contain compatible data
     */
    public static int[] getIntArray(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract integer array from a null EntryIFD");
        }

        Taggable tag = entry.getTag();
        Object data = entry.getData();

        if (data == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    tag.getDescription(),
                    tag.getNumberID(),
                    tag.getDirectoryType().getDescription()));
        }

        if (data instanceof int[])
        {
            return (int[]) data;
        }

        if (data instanceof Integer)
        {
            return new int[]{(int) data};
        }

        if (data instanceof short[])
        {
            short[] s = (short[]) data;
            int[] result = new int[s.length];

            for (int i = 0; i < s.length; i++)
            {
                result[i] = s[i] & 0xFFFF;
            }

            return result;
        }

        if (data instanceof Short)
        {
            return new int[]{((Short) data).intValue() & 0xFFFF};
        }

        if (data instanceof byte[])
        {
            byte[] b = (byte[]) data;
            int[] result = new int[b.length];

            for (int i = 0; i < b.length; i++)
            {
                result[i] = Byte.toUnsignedInt(b[i]);
            }

            return result;
        }

        if (data instanceof Byte)
        {
            return new int[]{Byte.toUnsignedInt((byte) data)};
        }

        if (data instanceof long[] || data instanceof Long)
        {
            LOGGER.warn("Incompatible 64-bit data for Tag [" + tag + "]. Returning empty array to prevent downcasting corruption");
            return new int[0];
        }

        throw new IllegalArgumentException("Tag [" + tag + "] contains incompatible type [" + data.getClass().getSimpleName() + "] for integer array conversion");
    }

    /**
     * Returns the value as a long.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the long value
     *
     * @throws IllegalArgumentException
     *         if the data is not numeric
     */
    public static long getLongValue(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract long value from a null EntryIFD");
        }

        long[] arr = getLongArray(entry);

        if (arr.length == 0)
        {
            throw new IllegalArgumentException("Entry contains no usable long data elements");
        }

        return arr[0];
    }

    /**
     * Returns the value as a long array.
     *
     * Since long is 64-bit, it safely holds all TIFF integer types (1, 3, 4, 6, 8, 9) as per TIFF
     * specification 6.0.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return an array of longs, or an empty array if data is incompatible
     *
     * @throws IllegalArgumentException
     *         if the entry is null or does not contain compatible data
     */
    public static long[] getLongArray(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract long array from a null EntryIFD");
        }

        Taggable tag = entry.getTag();
        Object data = entry.getData();

        if (data == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    tag.getDescription(),
                    tag.getNumberID(),
                    tag.getDirectoryType().getDescription()));
        }

        if (data instanceof long[])
        {
            return (long[]) data;
        }

        if (data instanceof int[])
        {
            int[] ints = (int[]) data;
            long[] result = new long[ints.length];

            for (int i = 0; i < ints.length; i++)
            {
                // Protect unsigned promotion
                result[i] = ints[i] & 0xFFFFFFFFL;
            }

            return result;
        }

        if (data instanceof short[])
        {
            short[] s = (short[]) data;
            long[] result = new long[s.length];

            for (int i = 0; i < s.length; i++)
            {
                result[i] = s[i] & 0xFFFFL;
            }

            return result;
        }

        if (data instanceof byte[])
        {
            byte[] b = (byte[]) data;
            long[] result = new long[b.length];

            for (int i = 0; i < b.length; i++)
            {
                result[i] = Byte.toUnsignedLong(b[i]);
            }

            return result;
        }

        if (data instanceof RationalNumber[])
        {
            RationalNumber[] rn = (RationalNumber[]) data;
            long[] result = new long[rn.length];

            for (int i = 0; i < rn.length; i++)
            {
                result[i] = rn[i].longValue();
            }

            return result;
        }

        if (data instanceof Number)
        {
            return new long[]{((Number) data).longValue()};
        }

        LOGGER.warn("Incompatible type [" + data.getClass().getSimpleName() + "] for Tag [" + tag + "]. Returning empty array");

        return new long[0];
    }

    /**
     * Returns the value as a scalar float.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the float value
     *
     * @throws IllegalArgumentException
     *         if the data is not numeric
     */
    public static float getFloatValue(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract float value from a null EntryIFD");
        }

        float[] arr = getFloatArray(entry);

        if (arr.length == 0)
        {
            throw new IllegalArgumentException("Entry contains no usable float data elements");
        }

        return arr[0];
    }

    /**
     * Returns the value as a float array.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the float array
     *
     * @throws IllegalArgumentException
     *         if the entry is null or does not contain compatible data
     */
    public static float[] getFloatArray(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract float array from a null EntryIFD");
        }

        Object data = entry.getData();

        if (data == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    entry.getTag().getDescription(),
                    entry.getTag().getNumberID(),
                    entry.getTag().getDirectoryType().getDescription()));
        }

        if (data instanceof float[])
        {
            return (float[]) data;
        }

        if (data instanceof Float)
        {
            return new float[]{(float) data};
        }

        if (data instanceof Number)
        {
            return new float[]{((Number) data).floatValue()};
        }

        return new float[0];
    }

    /**
     * Returns the value as a scalar double.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the double value
     *
     * @throws IllegalArgumentException
     *         if the data is not numeric
     */
    public static double getDoubleValue(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract double value from a null EntryIFD");
        }

        double[] arr = getDoubleArray(entry);

        if (arr.length == 0)
        {
            throw new IllegalArgumentException("Entry contains no usable double data elements");
        }

        return arr[0];
    }

    /**
     * Returns the value as a double array.
     *
     * <p>
     * It also acts as a "Bridge" between TIFF {@link RationalNumber} objects and Java primitives.
     * </p>
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the double array, or an empty array if incompatible
     *
     * @throws IllegalArgumentException
     *         if the entry is null or does not contain compatible data
     */
    public static double[] getDoubleArray(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract double array from a null EntryIFD");
        }

        Object data = entry.getData();

        if (data == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    entry.getTag().getDescription(),
                    entry.getTag().getNumberID(),
                    entry.getTag().getDirectoryType().getDescription()));
        }

        if (data instanceof double[])
        {
            return (double[]) data;
        }

        if (data instanceof Double)
        {
            return new double[]{(double) data};
        }

        if (data instanceof RationalNumber[])
        {
            RationalNumber[] rn = (RationalNumber[]) data;
            double[] result = new double[rn.length];

            for (int i = 0; i < rn.length; i++)
            {
                result[i] = rn[i].doubleValue();
            }

            return result;
        }

        if (data instanceof RationalNumber)
        {
            return new double[]{((RationalNumber) data).doubleValue()};
        }

        if (data instanceof Number)
        {
            return new double[]{((Number) data).doubleValue()};
        }

        return new double[0];
    }

    /**
     * Returns the value as a single RationalNumber instance.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the RationalNumber instance
     *
     * @throws IllegalArgumentException
     *         if the entry is null
     */
    public static RationalNumber getRationalValue(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract rational value from a null EntryIFD");
        }

        Object data = entry.getData();

        if (data == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] contains no data", entry.getTag().getDescription(), entry.getTagID()));
        }

        if (data instanceof RationalNumber)
        {
            return (RationalNumber) data;
        }

        RationalNumber[] arr = getRationalArray(entry);

        if (arr.length > 0)
        {
            return arr[0];
        }

        LOGGER.warn("Tag [" + entry.getTag() + "] expected RationalNumber but found [" + data.getClass().getSimpleName() + "]. Returning default 0/1");

        return new RationalNumber(0, 1);
    }

    /**
     * Returns the value as an array of {@link RationalNumber} objects.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the rational array, or an empty array if not rational
     *
     * @throws IllegalArgumentException
     *         if the entry is null or does not contain compatible data
     */
    public static RationalNumber[] getRationalArray(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract rational array from a null EntryIFD");
        }

        Object data = entry.getData();

        if (data == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    entry.getTag().getDescription(),
                    entry.getTag().getNumberID(),
                    entry.getTag().getDirectoryType().getDescription()));
        }

        if (data instanceof RationalNumber[])
        {
            return (RationalNumber[]) data;
        }

        if (data instanceof RationalNumber)
        {
            return new RationalNumber[]{(RationalNumber) data};
        }

        return new RationalNumber[0];
    }

    /**
     * Converts an {@link EntryIFD} entry into a human-readable string.
     *
     * <p>
     * This method handles standard TIFF field types including Rationals, encoded byte arrays, and
     * formatted hex tables for long arrays.
     * </p>
     *
     * @param entry
     *        the entry to format
     * @return a formatted string representation of the entry data
     */
    public static String toStringValue(EntryIFD entry)
    {
        if (entry != null && entry.getData() != null)
        {
            Object obj = entry.getData();
            Taggable tag = entry.getTag();
            TagHint hint = tag != null ? tag.getHint() : null;

            if (hint == TagHint.HINT_UCS2)
            {
                try
                {
                    byte[] b = ByteValueConverter.castToByteArray(getIntArray(entry));
                    return new String(b, StandardCharsets.UTF_16LE).replace("\u0000", "").trim();
                }

                catch (IllegalArgumentException exc)
                {
                    LOGGER.warn("Failed to parse UCS2 string for tag [" + tag + "] " + exc.getMessage());
                    return "";
                }
            }

            if (obj instanceof RationalNumber)
            {
                return ((RationalNumber) obj).toSimpleString(true);
            }

            if (obj instanceof RationalNumber[])
            {
                StringBuilder sb = new StringBuilder();
                RationalNumber[] result = (RationalNumber[]) obj;

                for (int i = 0; i < result.length; i++)
                {
                    sb.append(result[i].toSimpleString(true));

                    if (i < result.length - 1)
                    {
                        sb.append(", ");
                    }
                }

                return sb.toString();
            }

            if (obj instanceof byte[])
            {
                return decodeByteArray(tag, (byte[]) obj);
            }

            if (obj instanceof String)
            {
                return decodeString(tag, (String) obj);
            }

            if (obj instanceof Number || obj.getClass().isArray())
            {
                if (hint == TagHint.HINT_BYTE_STREAM)
                {
                    String firstHex = "N/A";

                    if (obj instanceof byte[] && ((byte[]) obj).length > 0)
                    {
                        firstHex = Integer.toHexString(Byte.toUnsignedInt(((byte[]) obj)[0]));
                    }

                    else if (obj instanceof int[] && ((int[]) obj).length > 0)
                    {
                        firstHex = Long.toHexString(((int[]) obj)[0] & 0xFFFFFFFFL);
                    }

                    else if (obj instanceof long[] && ((long[]) obj).length > 0)
                    {
                        firstHex = Long.toHexString(((long[]) obj)[0]);
                    }

                    return "Truncated stream. Count [" + entry.getCount() + "]. Starts [" + firstHex + "]";
                }

                if (obj instanceof float[] || obj instanceof Float)
                {
                    float[] result = getFloatArray(entry);
                    return (result.length == 1 ? String.valueOf(result[0]) : Arrays.toString(result));
                }

                if (obj instanceof double[] || obj instanceof Double)
                {
                    double[] result = getDoubleArray(entry);
                    return (result.length == 1 ? String.valueOf(result[0]) : Arrays.toString(result));
                }

                long[] result = getLongArray(entry);
                return (result.length == 1 ? String.valueOf(result[0]) : Arrays.toString(result));
            }

            LOGGER.warn("Unsupported field structure [" + entry.getFieldType() + "] for tag [" + tag + "]");
        }

        return "";
    }

    /**
     * Extracts a {@link Date} object from an entry if a date hint is present.
     *
     * @param entry
     *        the entry to parse
     * @return a parsed Date object
     *
     * @throws IllegalArgumentException
     *         if the hint is missing or the format is invalid
     */
    public static Date getDate(EntryIFD entry)
    {
        if (entry != null)
        {
            if (entry.getTag() == null || entry.getTag().getHint() != TagHint.HINT_DATE)
            {
                throw new IllegalArgumentException("Tag does not contain a valid date hint [" + entry.getTag() + "]");
            }

            Object data = entry.getData();

            if (data instanceof String)
            {
                Date d = SmartDateParser.convertToDate((String) data);

                if (d != null)
                {
                    return d;
                }
            }

            throw new IllegalArgumentException("Invalid or missing date data payload in entry [" + entry.getTag() + "]");
        }

        else
        {
            throw new IllegalArgumentException("Cannot extract Date from a null EntryIFD");
        }
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
     *         if the entry does not contain a date hint or the format is invalid
     */
    public static ZonedDateTime getZonedDateTime(EntryIFD entry)
    {
        if (entry != null)
        {
            if (entry.getTag() == null || entry.getTag().getHint() != TagHint.HINT_DATE)
            {
                throw new IllegalArgumentException("Mismatched date format or missing HINT_DATE in entry [" + entry.getTag() + "]");
            }

            Object data = entry.getData();

            if (data instanceof String)
            {
                ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime((String) data);

                if (zdt != null)
                {
                    return zdt;
                }
            }

            throw new IllegalArgumentException("Invalid timezone payload format in entry [" + entry.getTag() + "]");
        }

        else
        {
            throw new IllegalArgumentException("Cannot extract ZonedDateTime from a null EntryIFD");
        }
    }

    /**
     * Decodes a string value. If the input matches a recognised date pattern, it returns the value
     * as a formatted date string.
     *
     * @param tag
     *        the tag descriptor
     * @param data
     *        the raw string value to decode
     * @return a decoded string, potentially formatted as a date
     */
    private static String decodeString(Taggable tag, String data)
    {
        String val = data.trim();

        if (tag != null && tag.getHint() == TagHint.HINT_DATE)
        {
            Date d = SmartDateParser.convertToDate(val);
            return (d != null) ? d.toString() : val;
        }

        return val;
    }

    /**
     * Decodes a raw byte array into a human-readable string based on the tag's hint.
     *
     * @param tag
     *        the tag descriptor
     * @param bytes
     *        the raw binary data
     * @return a formatted string representation
     */
    private static String decodeByteArray(Taggable tag, byte[] bytes)
    {
        TagHint hint = tag != null ? tag.getHint() : null;

        if (hint == TagHint.HINT_MASK)
        {
            return "[Masked items]";
        }

        if (hint == TagHint.HINT_BYTE)
        {
            return ByteValueConverter.toHex(bytes);
        }

        if (hint == TagHint.HINT_ENCODED_STRING)
        {
            return decodeUserComment(bytes);
        }

        return new String(bytes, StandardCharsets.UTF_8).replace("\u0000", "").trim();
    }

    /**
     * Decodes encoded strings containing an 8-byte charset identifier.
     *
     * @param data
     *        the raw byte array
     * @return a decoded, trimmed string
     */
    private static String decodeUserComment(byte[] data)
    {
        if (data != null && data.length >= ENCODING_HEADER_LENGTH)
        {
            String headerKey = new String(data, 0, ENCODING_HEADER_LENGTH, StandardCharsets.US_ASCII);
            byte[] payloadBytes = Arrays.copyOfRange(data, ENCODING_HEADER_LENGTH, data.length);
            Charset charset = ENCODING_MAP.get(headerKey);

            if (charset == null)
            {
                charset = StandardCharsets.UTF_8;
            }

            String decoded = new String(payloadBytes, charset);
            int nullIndex = decoded.indexOf('\0');

            if (nullIndex != -1)
            {
                decoded = decoded.substring(0, nullIndex);
            }

            return decoded.trim();
        }

        return "";
    }
}
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
 * Utility class for converting {@link EntryIFD} values into human-readable or numeric forms. This
 * class applies transformation rules based on TIFF field types and tag hints.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2025
 */
public final class TagValueConverter
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TagValueConverter.class);
    private static final int ENCODING_HEADER_LENGTH = 8;
    private static final Map<String, Charset> ENCODING_MAP = new HashMap<>();

    static
    {
        ENCODING_MAP.put("ASCII\0\0\0", StandardCharsets.US_ASCII);
        ENCODING_MAP.put("UTF-8\0\0\0", StandardCharsets.UTF_8);
        ENCODING_MAP.put("\0\0\0\0\0\0\0\0", StandardCharsets.UTF_8);
        ENCODING_MAP.put("JIS\0\0\0\0\0", Charset.forName("Shift_JIS"));
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
                    entry.getTag().getNumberID(),
                    entry.getTag().getDirectoryType().getDescription()));
        }

        return toNumericValue(entry).intValue();
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

        int[] result;
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
            result = (int[]) data;
        }

        else if (data instanceof Integer)
        {
            result = new int[]{(int) data};
        }

        else if (data instanceof short[])
        {
            short[] s = (short[]) data;
            result = new int[s.length];

            for (int i = 0; i < s.length; i++)
            {
                result[i] = s[i] & 0xFFFF;
            }
        }

        else if (data instanceof Short)
        {
            result = new int[]{((Short) data).intValue() & 0xFFFF};
        }

        else if (data instanceof byte[])
        {
            byte[] b = (byte[]) data;
            result = new int[b.length];

            for (int i = 0; i < b.length; i++)
            {
                result[i] = Byte.toUnsignedInt(b[i]);
            }
        }

        else if (data instanceof Byte)
        {
            result = new int[]{Byte.toUnsignedInt((byte) data)};
        }

        else if (data instanceof long[] || data instanceof Long)
        {
            result = new int[0];
            LOGGER.warn("Incompatible 64-bit data for Tag [" + tag + "]. Returning empty array");
        }

        else
        {
            throw new IllegalArgumentException("Tag [" + tag + "] contains incompatible type [" + data.getClass().getSimpleName() + "] for integer array conversion");
        }

        return result;
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

        return toNumericValue(entry).longValue();
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

        long[] result;
        Taggable tag = entry.getTag();
        Object data = entry.getData();

        if (data == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    tag.getDescription(), tag.getNumberID(), tag.getDirectoryType().getDescription()));
        }

        if (data instanceof long[])
        {
            result = (long[]) data;
        }

        else if (data instanceof int[])
        {
            int[] ints = (int[]) data;
            result = new long[ints.length];

            for (int i = 0; i < ints.length; i++)
            {
                // Protect unsigned promotion
                result[i] = ints[i] & 0xFFFFFFFFL;
            }
        }

        else if (data instanceof short[])
        {
            short[] s = (short[]) data;
            result = new long[s.length];

            for (int i = 0; i < s.length; i++)
            {
                result[i] = s[i] & 0xFFFFL;
            }
        }

        else if (data instanceof byte[])
        {
            byte[] b = (byte[]) data;
            result = new long[b.length];

            for (int i = 0; i < b.length; i++)
            {
                result[i] = Byte.toUnsignedLong(b[i]);
            }
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

        else
        {
            result = new long[0];
            LOGGER.warn("Incompatible type [" + data.getClass().getSimpleName() + "] for Tag [" + tag + "]. Returning empty array.");
        }

        return result;
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

        return toNumericValue(entry).floatValue();
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

        float[] result;
        Taggable tag = entry.getTag();

        if (entry.getData() != null)
        {
            Object data = entry.getData();

            if (data instanceof float[])
            {
                result = (float[]) data;
            }

            else if (data instanceof Float)
            {
                result = new float[]{(float) data};
            }

            else
            {
                result = new float[0];
            }
        }

        else
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    tag.getDescription(),
                    tag.getNumberID(),
                    tag.getDirectoryType().getDescription()));
        }

        return result;
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

        return toNumericValue(entry).doubleValue();
    }

    /**
     * Returns the value as a double array.
     * *
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

        double[] result;
        Taggable tag = entry.getTag();

        if (entry.getData() != null)
        {
            Object data = entry.getData();

            if (data instanceof double[])
            {
                result = (double[]) data;
            }

            else if (data instanceof Double)
            {
                return new double[]{(double) data};
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

            else if (data instanceof RationalNumber)
            {
                result = new double[]{((RationalNumber) data).doubleValue()};
            }

            else
            {
                result = new double[0];
            }
        }

        else
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    tag.getDescription(),
                    tag.getNumberID(),
                    tag.getDirectoryType().getDescription()));
        }

        return result;
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
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] contains no data",
                    entry.getTag().getDescription(), entry.getTagID()));
        }

        if (data instanceof RationalNumber)
        {
            return (RationalNumber) data;
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

        RationalNumber[] result;
        Taggable tag = entry.getTag();

        if (entry.getData() != null)
        {
            Object data = entry.getData();

            if (data instanceof RationalNumber[])
            {
                result = (RationalNumber[]) data;
            }

            else if (data instanceof RationalNumber)
            {
                result = new RationalNumber[]{(RationalNumber) data};
            }

            else
            {
                result = new RationalNumber[0];
            }
        }

        else
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] in [%s] contains no data",
                    tag.getDescription(),
                    tag.getNumberID(),
                    tag.getDirectoryType().getDescription()));
        }

        return result;
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
        String str = "";

        if (entry != null && entry.getData() != null)
        {
            Object obj = entry.getData();
            Taggable tag = entry.getTag();
            TagHint hint = tag.getHint();

            if (hint == TagHint.HINT_UCS2)
            {
                byte[] b = ByteValueConverter.castToByteArray(getIntArray(entry));

                str = new String(b, StandardCharsets.UTF_16LE).replace("\u0000", "").trim();
            }

            else if (obj instanceof RationalNumber || obj instanceof RationalNumber[])
            {
                StringBuilder sb = new StringBuilder();
                RationalNumber[] result = getRationalArray(entry);

                for (int i = 0; i < result.length; i++)
                {
                    sb.append(result[i].toSimpleString(true));

                    if (i < result.length - 1)
                    {
                        sb.append(", ");
                    }
                }

                str = sb.toString();
            }

            else if (obj instanceof byte[])
            {
                str = decodeByteArray(tag, (byte[]) obj);
            }

            else if (obj instanceof String)
            {
                str = decodeString(tag, (String) obj);
            }

            else if (obj instanceof Number || obj.getClass().isArray())
            {
                if (hint == TagHint.HINT_BYTE_STREAM)
                {
                    long[] result = getLongArray(entry);
                    str = "Truncated stream. Count [" + entry.getCount() + "]. Starts [" + (result.length > 0 ? Long.toHexString(result[0]) : "N/A") + "]";
                }

                else if (obj instanceof float[] || obj instanceof Float)
                {
                    float[] result = getFloatArray(entry);
                    str = (result.length == 1 ? String.valueOf(result[0]) : Arrays.toString(result));
                }

                else if (obj instanceof double[] || obj instanceof Double)
                {
                    double[] result = getDoubleArray(entry);
                    str = (result.length == 1 ? String.valueOf(result[0]) : Arrays.toString(result));
                }

                else
                {
                    long[] result = getLongArray(entry);
                    str = (result.length == 1 ? String.valueOf(result[0]) : Arrays.toString(result));
                }
            }

            else
            {
                LOGGER.warn("Unsupported field [" + entry.getFieldType() + "] for tag [" + tag + "]");
            }
        }

        return str;
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
        if (entry.getTag().getHint() != TagHint.HINT_DATE)
        {
            throw new IllegalArgumentException("Tag does not contain a date hint: " + entry.getTag());
        }

        if (entry.getData() instanceof String)
        {
            Date d = SmartDateParser.convertToDate((String) entry.getData());

            if (d != null)
            {
                return d;
            }
        }

        throw new IllegalArgumentException("Invalid or null date format in entry [" + entry.getTag() + "]");
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
        if (entry != null && entry.getData() instanceof String && entry.getTag().getHint() == TagHint.HINT_DATE)
        {
            return SmartDateParser.convertToZonedDateTime((String) entry.getData());
        }

        throw new IllegalArgumentException("Mismatched date format or missing HINT_DATE in entry [" + entry.getTag() + "]");
    }

    /**
     * Validates and returns the entry data as a {@link Number}.
     *
     * @param entry
     *        the {@link EntryIFD} to evaluate
     * @return the data cast as a Number
     *
     * @throws IllegalArgumentException
     *         if the data is not numeric
     */
    private static Number toNumericValue(EntryIFD entry)
    {
        Object obj = entry.getData();

        if (obj instanceof Number)
        {
            return (Number) obj;
        }

        throw new IllegalArgumentException("Tag [" + entry.getTag() + "] is not a numeric type");
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

        if (tag.getHint() == TagHint.HINT_DATE)
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
        TagHint hint = tag.getHint();

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

        return new String(ByteValueConverter.readFirstNullTerminatedByteArray(bytes), StandardCharsets.UTF_8);
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
        if (data == null || data.length < ENCODING_HEADER_LENGTH)
        {
            return "";
        }

        String header = new String(data, 0, ENCODING_HEADER_LENGTH, StandardCharsets.US_ASCII);
        Charset charset = ENCODING_MAP.get(header);
        byte[] cleaned = ByteValueConverter.readFirstNullTerminatedByteArray(Arrays.copyOfRange(data, ENCODING_HEADER_LENGTH, data.length));

        if (charset == null)
        {
            charset = StandardCharsets.UTF_8;
        }

        return new String(cleaned, charset).trim();
    }

    @Deprecated
    public static int[] getIntArray2(EntryIFD entry)
    {
        int[] result = new int[0];

        if (entry != null)
        {
            Object data = entry.getData();

            if (data instanceof int[])
            {
                result = (int[]) data;
            }

            else if (data instanceof Integer)
            {
                result = new int[]{(int) data};
            }

            else if (data instanceof short[])
            {
                short[] s = (short[]) data;
                result = new int[s.length];

                for (int i = 0; i < s.length; i++)
                {
                    result[i] = s[i] & 0xFFFF;
                }
            }

            else if (data instanceof Short)
            {
                result = new int[]{((Short) data).intValue() & 0xFFFF};
            }

            else if (data instanceof byte[])
            {
                byte[] b = (byte[]) data;
                result = new int[b.length];

                for (int i = 0; i < b.length; i++)
                {
                    result[i] = Byte.toUnsignedInt(b[i]);
                }
            }

            else if (data instanceof Byte)
            {
                result = new int[]{Byte.toUnsignedInt((byte) data)};
            }

            else if (data instanceof long[] || data instanceof Long)
            {
                LOGGER.warn("Cannot convert to an integer array from 64-bit Long data. Empty array returned");
            }
        }

        return result;
    }
}
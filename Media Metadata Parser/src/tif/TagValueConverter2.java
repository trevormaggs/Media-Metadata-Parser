package tif;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import common.ByteValueConverter;
import logger.LogFactory;
import tif.DirectoryIFD.EntryIFD;
import tif.tagspecs.Taggable;
import util.SmartDateParser;

/**
 * Single source of truth for converting and formatting TIFF entry payloads
 * into numeric arrays, primitives, and clean human-readable text.
 *
 * @author Trevor Maggs
 * @version 1.5
 * @since 20 May 2026
 */
public final class TagValueConverter2
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TagValueConverter2.class);
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

    private TagValueConverter2()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    public static boolean canConvertToInt(TifFieldType type)
    {
        if (type == null) return false;
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
            throw new IllegalArgumentException("Entry contains no usable integer data elements");
        }

        return arr[0];
    }

    public static int[] getIntArray(EntryIFD entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot extract integer array from a null EntryIFD");
        }

        return toIntArray(entry.getTag(), entry.getData());
    }

    private static int[] toIntArray(Taggable tag, Object data)
    {
        if (data == null) return new int[0];

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

        if (data instanceof int[])
        {
            return (int[]) data;
        }

        if (data instanceof Integer)
        {
            return new int[]{(int) data};
        }

        if (data instanceof long[] || data instanceof Long)
        {
            LOGGER.warn("Incompatible 64-bit data for Tag [" + tag + "]. Returning empty array to prevent downcasting corruption");
            return new int[0];
        }

        throw new IllegalArgumentException("Tag [" + tag + "] contains incompatible type [" + data.getClass().getSimpleName() + "] for integer array conversion");
    }

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

    public static long[] getLongArray(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract long array from a null EntryIFD");
        return toLongArray(entry.getTag(), entry.getData());
    }

    private static long[] toLongArray(Taggable tag, Object data)
    {
        if (data == null) return new long[0];
        if (data instanceof long[]) return (long[]) data;

        if (data instanceof int[])
        {
            int[] ints = (int[]) data;
            long[] result = new long[ints.length];
            for (int i = 0; i < ints.length; i++)
                result[i] = ints[i] & 0xFFFFFFFFL;
            return result;
        }
        if (data instanceof short[])
        {
            short[] s = (short[]) data;
            long[] result = new long[s.length];
            for (int i = 0; i < s.length; i++)
                result[i] = s[i] & 0xFFFFL;
            return result;
        }
        if (data instanceof byte[])
        {
            byte[] b = (byte[]) data;
            long[] result = new long[b.length];
            for (int i = 0; i < b.length; i++)
                result[i] = Byte.toUnsignedLong(b[i]);
            return result;
        }
        if (data instanceof RationalNumber[])
        {
            RationalNumber[] rn = (RationalNumber[]) data;
            long[] result = new long[rn.length];
            for (int i = 0; i < rn.length; i++)
                result[i] = rn[i].longValue();
            return result;
        }
        if (data instanceof Number)
        {
            return new long[]{((Number) data).longValue()};
        }
        LOGGER.warn("Incompatible type [" + data.getClass().getSimpleName() + "] for Tag [" + tag + "]. Returning empty array");
        return new long[0];
    }

    public static float getFloatValue(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract float value from a null EntryIFD");
        float[] arr = getFloatArray(entry);
        if (arr.length == 0) throw new IllegalArgumentException("Entry contains no usable float data elements");
        return arr[0];
    }

    public static float[] getFloatArray(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract float array from a null EntryIFD");
        Object data = entry.getData();
        if (data instanceof float[]) return (float[]) data;
        if (data instanceof Float) return new float[]{(float) data};
        if (data instanceof Number) return new float[]{((Number) data).floatValue()};
        return new float[0];
    }

    public static double getDoubleValue(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract double value from a null EntryIFD");
        double[] arr = getDoubleArray(entry);
        if (arr.length == 0) throw new IllegalArgumentException("Entry contains no usable double data elements");
        return arr[0];
    }

    public static double[] getDoubleArray(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract double array from a null EntryIFD");
        return toDoubleArray(entry.getData());
    }

    private static double[] toDoubleArray(Object data)
    {
        if (data == null) return new double[0];
        if (data instanceof double[]) return (double[]) data;
        if (data instanceof Double) return new double[]{(double) data};

        if (data instanceof RationalNumber[])
        {
            RationalNumber[] rn = (RationalNumber[]) data;
            double[] result = new double[rn.length];
            for (int i = 0; i < rn.length; i++)
                result[i] = rn[i].doubleValue();
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

    public static RationalNumber getRationalValue(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract rational value from a null EntryIFD");
        Object data = entry.getData();
        if (data instanceof RationalNumber) return (RationalNumber) data;

        RationalNumber[] arr = getRationalArray(entry);
        if (arr.length > 0) return arr[0];

        LOGGER.warn("Tag [" + entry.getTag() + "] expected RationalNumber but found [" + data.getClass().getSimpleName() + "]. Returning default 0/1");
        return new RationalNumber(0, 1);
    }

    public static RationalNumber[] getRationalArray(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract rational array from a null EntryIFD");
        Object data = entry.getData();
        if (data instanceof RationalNumber[]) return (RationalNumber[]) data;
        if (data instanceof RationalNumber) return new RationalNumber[]{(RationalNumber) data};
        return new RationalNumber[0];
    }

    public static Date getDate(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract Date from a null EntryIFD");
        if (entry.getTag() == null || entry.getTag().getHint() != TagHint.HINT_DATE)
        {
            throw new IllegalArgumentException("Tag does not contain a valid date hint [" + entry.getTag() + "]");
        }
        if (entry.getData() instanceof String)
        {
            Date d = SmartDateParser.convertToDate((String) entry.getData());
            if (d != null) return d;
        }
        throw new IllegalArgumentException("Invalid or missing date data payload in entry [" + entry.getTag() + "]");
    }

    public static ZonedDateTime getZonedDateTime(EntryIFD entry)
    {
        if (entry == null) throw new IllegalArgumentException("Cannot extract ZonedDateTime from a null EntryIFD");
        if (entry.getTag() == null || entry.getTag().getHint() != TagHint.HINT_DATE)
        {
            throw new IllegalArgumentException("Mismatched date format or missing HINT_DATE in entry [" + entry.getTag() + "]");
        }
        if (entry.getData() instanceof String)
        {
            ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime((String) entry.getData());
            if (zdt != null) return zdt;
        }
        throw new IllegalArgumentException("Invalid timezone payload format in entry [" + entry.getTag() + "]");
    }

    /**
     * Unified master string rendering engine used by both EntryIFD containers
     * and Taggable schema fallback printers.
     */
    public static String toHumanReadableString(Taggable tag, Object val)
    {
        if (val == null) return "";

        TagHint hint = tag != null ? tag.getHint() : null;

        // 1. Handle special string context formatting mappings
        if (hint == TagHint.HINT_UCS2)
        {
            try
            {
                byte[] b = ByteValueConverter.castToByteArray(toIntArray(tag, val));
                return new String(b, StandardCharsets.UTF_16LE).replace("\u0000", "").trim();
            }
            catch (IllegalArgumentException exc)
            {
                LOGGER.warn("Failed to parse UCS2 string for tag [" + tag + "] " + exc.getMessage());
                return "";
            }
        }

        if (hint == TagHint.HINT_BYTE_STREAM)
        {
            if (val instanceof byte[] || val instanceof int[] || val instanceof long[])
            {
                int length = Array.getLength(val);
                if (length > 0)
                {
                    String firstHex = "N/A";
                    if (val instanceof byte[]) firstHex = Integer.toHexString(Byte.toUnsignedInt(((byte[]) val)[0]));
                    else if (val instanceof int[]) firstHex = Long.toHexString(((int[]) val)[0] & 0xFFFFFFFFL);
                    else if (val instanceof long[]) firstHex = Long.toHexString(((long[]) val)[0]);

                    return String.format(Locale.ROOT, "Truncated stream. Count [%d]. Starts [%s]", length, firstHex);
                }
            }
            return "[Binary Data]";
        }

        // 2. Handle Object Array Types
        if (val instanceof RationalNumber[])
        {
            StringBuilder sb = new StringBuilder();
            RationalNumber[] arr = (RationalNumber[]) val;
            for (int i = 0; i < arr.length; i++)
            {
                sb.append(arr[i] != null ? formatRational(arr[i]) : "0");
                if (i < arr.length - 1) sb.append(", ");
            }
            return sb.toString();
        }

        if (val instanceof RationalNumber)
        {
            return ((RationalNumber) val).toSimpleString(true);
        }

        if (val instanceof byte[])
        {
            return decodeByteArray(tag, (byte[]) val);
        }

        if (val instanceof String)
        {
            return decodeString(tag, (String) val);
        }

        // 3. Handle Primitive Array and Scalar Primitives natively
        if (val instanceof float[] || val instanceof Float)
        {
            float[] arr = (val instanceof float[]) ? (float[]) val : new float[]{(Float) val};
            return arr.length == 1 ? formatNumericValue(arr[0]) : Arrays.toString(arr);
        }

        if (val instanceof double[] || val instanceof Double)
        {
            double[] arr = toDoubleArray(val);
            if (arr.length == 1) return formatNumericValue(arr[0]);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length; i++)
            {
                sb.append(formatNumericValue(arr[i]));
                if (i < arr.length - 1) sb.append(" ");
            }
            return sb.toString();
        }

        if (val.getClass().isArray())
        {
            long[] arr = toLongArray(tag, val);
            return arr.length == 1 ? String.valueOf(arr[0]) : Arrays.toString(arr);
        }

        return val.toString().trim();
    }

    public static String toStringValue(EntryIFD entry)
    {
        if (entry == null || entry.getData() == null) return "";
        return toHumanReadableString(entry.getTag(), entry.getData());
    }

    public static String formatRational(RationalNumber r)
    {
        if (r == null) return "";
        if (r.hasIntegerValue()) return String.valueOf(r.longValue());

        double d = r.doubleValue();
        if (d < 0.1) return r.toString(); // Keeps shutter speeds like 1/125 readable
        return formatNumericValue(d);
    }

    public static String formatNumericValue(double d)
    {
        if (Double.isNaN(d) || Double.isInfinite(d)) return String.valueOf(d);
        if (d == (long) d) return String.format(Locale.ROOT, "%d", (long) d);
        return String.format(Locale.ROOT, "%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static String decodeUserComment(byte[] data)
    {
        if (data == null || data.length < ENCODING_HEADER_LENGTH) return "";

        String headerKey = new String(data, 0, ENCODING_HEADER_LENGTH, StandardCharsets.US_ASCII);
        Charset charset = ENCODING_MAP.getOrDefault(headerKey, StandardCharsets.UTF_8);

        int payloadOffset = ENCODING_HEADER_LENGTH;
        int payloadLength = data.length - ENCODING_HEADER_LENGTH;

        if (charset.equals(StandardCharsets.UTF_16LE) && payloadLength >= 2)
        {
            int b1 = data[ENCODING_HEADER_LENGTH] & 0xFF;
            int b2 = data[ENCODING_HEADER_LENGTH + 1] & 0xFF;

            if (b1 == 0xFE && b2 == 0xFF)
            {
                charset = StandardCharsets.UTF_16BE;
                payloadOffset += 2;
                payloadLength -= 2;
            }
            else if (b1 == 0xFF && b2 == 0xFE)
            {
                payloadOffset += 2;
                payloadLength -= 2;
            }
        }
        return new String(data, payloadOffset, payloadLength, charset).replace("\0", "").trim();
    }

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

    private static String decodeByteArray(Taggable tag, byte[] bytes)
    {
        TagHint hint = tag != null ? tag.getHint() : null;
        if (hint == TagHint.HINT_MASK) return "[Masked items]";
        if (hint == TagHint.HINT_BYTE) return ByteValueConverter.toHex(bytes);
        if (hint == TagHint.HINT_ENCODED_STRING) return decodeUserComment(bytes);

        return new String(bytes, StandardCharsets.UTF_8).replace("\u0000", "").trim();
    }
}
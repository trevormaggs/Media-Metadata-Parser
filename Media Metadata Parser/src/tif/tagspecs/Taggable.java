package tif.tagspecs;

import tif.TagHint;
import tif.TagValueFormatter;
import java.lang.reflect.Array;
import java.util.Locale;
import tif.DirectoryIdentifier;
import tif.RationalNumber;

public interface Taggable
{
    /**
     * @return the 16-bit numerical Tag ID (e.g., 0x0100 for ImageWidth)
     */
    int getNumberID();

    /**
     * @return the expected data type or format hint for this tag
     */
    TagHint getHint();

    /**
     * @return the IFD where this tag is valid
     */
    DirectoryIdentifier getDirectoryType();

    /**
     * @return a human-readable name or description of the tag
     */
    String getDescription();

    /**
     * @return true if this represents a tag not explicitly defined in the specification
     */
    default boolean isUnknown()
    {
        return false;
    }

    default String translate2(Object val)
    {
        if (val == null)
        {
            return "";
        }

        if (val instanceof RationalNumber)
        {
            return TagValueFormatter.toStringValue(val, getHint());
        }

        return val.toString().trim();
    }

    default String translate(Object val)
    {
        if (val == null)
        {
            return "";
        }

        if (getHint() == TagHint.HINT_BYTE_STREAM)
        {
            if (val instanceof byte[] || val instanceof int[])
            {
                int length = Array.getLength(val);

                if (length > 0)
                {
                    return String.format(Locale.ROOT, "[Binary Data: %d bytes]", length);
                }
            }

            return "[Binary Data]";
        }

        if (val instanceof RationalNumber[])
        {
            StringBuilder sb = new StringBuilder();
            RationalNumber[] arr = (RationalNumber[]) val;

            for (int i = 0; i < arr.length; i++)
            {
                if (arr[i] != null)
                {
                    sb.append(formatNumericValue(arr[i].doubleValue()));
                }

                else
                {
                    sb.append("0");
                }

                if (i < arr.length - 1)
                {
                    sb.append(" ");
                }
            }

            return sb.toString();
        }

        if (val instanceof RationalNumber)
        {
            return formatRational((RationalNumber) val);
        }

        if (val instanceof Float)
        {
            return formatNumericValue(((Float) val).doubleValue());
        }

        if (val instanceof Double)
        {
            return formatNumericValue((Double) val);
        }

        if (val instanceof int[])
        {
            return formatIntArray((int[]) val);
        }

        if (val instanceof double[])
        {
            return formatDoubleArray((double[]) val);
        }

        return val.toString().trim();
    }

    static int convertToInt(Object val)
    {
        if (val instanceof Number)
        {
            if (val instanceof Byte)
            {
                return ((Byte) val).intValue() & 0xFF; // Preserve unsigned layout byte range
            }

            return ((Number) val).intValue();
        }

        if (val instanceof int[] && ((int[]) val).length > 0)
        {
            return ((int[]) val)[0];
        }

        if (val instanceof byte[] && ((byte[]) val).length > 0)
        {
            return ((byte[]) val)[0] & 0xFF;
        }

        return -1;
    }

    static String formatRational(RationalNumber r)
    {
        if (r == null) return "";

        if (r.hasIntegerValue())
        {
            return String.valueOf(r.longValue());
        }

        double d = r.doubleValue();

        // Keeps small fractional values like shutter speeds (e.g. 1/125) readable
        if (d < 0.1)
        {
            return r.toString();
        }

        return formatNumericValue(d);
    }

    static String formatNumericValue(double d)
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

    static String formatDoubleArray(double[] arr)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++)
        {
            sb.append(formatNumericValue(arr[i]));

            if (i < arr.length - 1)
            {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    static String formatIntArray(int[] arr)
    {
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

    static String formatIntArray2(Object val)
    {
        StringBuilder sb = new StringBuilder();
        int[] arr = TagValueFormatter.toIntArray(val);

        for (int i = 0; i < arr.length; i++)
        {
            sb.append(arr[i]);

            if (i < arr.length - 1)
            {
                sb.append(" ");
            }

            System.out.printf("LOOK: %s\n", sb.toString());
        }

        return sb.toString();
    }
}
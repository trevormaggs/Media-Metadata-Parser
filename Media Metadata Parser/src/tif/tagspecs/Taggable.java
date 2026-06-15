package tif.tagspecs;

import tif.TagHint;
import java.util.Locale;
import tif.DirectoryIdentifier;
import tif.RationalNumber;

public interface Taggable
{
    /**
     * @return the 16-bit numerical Tag ID (e.g., 0x0100 for ImageWidth)
     */
    public int getNumberID();

    /**
     * @return the expected data type or format hint for this tag
     */
    public TagHint getHint();

    /**
     * @return the IFD where this tag is valid
     */
    public DirectoryIdentifier getDirectoryType();

    /**
     * @return a human-readable name or description of the tag
     */
    public String getDescription();

    /**
     * @return true if this represents a tag not explicitly defined in the specification
     */
    public default boolean isUnknown()
    {
        return false;
    }

    default String translate(Object val)
    {
        if (val == null)
        {
            return "";
        }

        // 1. Intercept Custom Rational Object Wrappers
        if (val instanceof RationalNumber)
        {
            RationalNumber r = (RationalNumber) val;

            if (r.hasIntegerValue())
            {
                return String.valueOf(r.longValue());
            }

            double d = r.doubleValue();

            if (d < 0.1)
            {
                return r.toString();
            }

            return formatNumericValue(d);
        }

        // 2. Intercept Standard Decimal Primitives
        if (val instanceof Double)
        {
            return formatNumericValue((Double) val);
        }

        if (val instanceof Float)
        {
            return formatNumericValue(((Float) val).doubleValue());
        }

        // 3. Global Byte Stream Interception Footprint
        if (getHint() == TagHint.HINT_BYTE_STREAM)
        {
            if (val instanceof byte[])
            {
                return String.format(Locale.ROOT, "[Binary Data: %d bytes]", ((byte[]) val).length);
            }

            if (val instanceof int[])
            {
                return String.format(Locale.ROOT, "[Binary Data: %d bytes]", ((int[]) val).length);
            }

            return "[Binary Data]";
        }

        // 4. Clean Fallback for Primitive Array Blocks & Unmapped Objects
        if (val instanceof double[])
        {
            return formatDoubleArray((double[]) val);
        }

        if (val instanceof int[])
        {
            return formatIntArray((int[]) val);
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
}
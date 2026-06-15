package tif;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import common.ByteValueConverter;
import tif.tagspecs.TagIFD_Baseline;
import tif.tagspecs.TagIFD_Extension;
import tif.tagspecs.TagIFD_Private;
import tif.tagspecs.Taggable;

/**
 * Utility class responsible for translating raw TIFF/EXIF tag values into human-readable strings.
 * 
 * @author Trevor Maggs
 * @version 1.4
 * @since 15 June 2026
 */
public class TagTranslator
{
    public static String getDisplayName(DirectoryIdentifier dir, Taggable tag)
    {
        if (dir == DirectoryIdentifier.IFD_DIRECTORY_IFD1)
        {
            if (tag == TagIFD_Baseline.IFD_JPEG_INTERCHANGE_FORMAT)
            {
                return "ThumbnailOffset";
            }

            else if (tag == TagIFD_Baseline.IFD_JPEG_INTERCHANGE_FORMAT_LENGTH)
            {
                return "ThumbnailLength";
            }
        }

        return tag.getDescription();
    }

    public static String translate(Taggable tag, Object value)
    {
        if (value == null)
        {
            return "";
        }

        if (tag.getHint() == TagHint.HINT_RATIONAL)
        {
            return formatRational(value);
        }

        else if (tag.getHint() == TagHint.HINT_BYTE_STREAM)
        {
            if (value instanceof byte[])
            {
                return String.format(Locale.ROOT, "[Binary Data: %d bytes]", ((byte[]) value).length);
            }

            return "[Binary Data]";
        }

        if (tag instanceof TagIFD_Baseline)
        {
            return translateBaseline((TagIFD_Baseline) tag, value);
        }

        else if (tag instanceof TagIFD_Extension)
        {
            return translateExtension((TagIFD_Extension) tag, value);
        }

        else if (tag instanceof TagIFD_Private)
        {
            return translatePrivate((TagIFD_Private) tag, value);
        }

        return translateFallback(tag, value);
    }

    private static String translateBaseline(TagIFD_Baseline tag, Object val)
    {
        switch (tag)
        {
            case IFD_ORIENTATION:
                return translateOrientation(val);

            case IFD_RESOLUTION_UNIT:
                return translateResolutionUnit(val);

            case IFD_COMPRESSION:
                return translateCompression(val);

            case IFD_YCB_CR_POSITIONING:
                return translateYCbCr(val);

            case IFD_PHOTOMETRIC_INTERPRETATION:
                return translatePhotometric(val);

            case IFD_PLANAR_CONFIGURATION:
                return translatePlanarConfig(val);

            default:
            break;
        }

        return translateFallback(tag, val);
    }

    private static String translateOrientation(Object val)
    {
        int num = convertToInt(val);

        switch (num)
        {
            case 1:
                return "Horizontal (normal)";

            case 2:
                return "Mirror horizontal";

            case 3:
                return "Rotate 180";

            case 4:
                return "Mirror vertical";

            case 5:
                return "Mirror horizontal and rotate 270 CW";

            case 6:
                return "Rotate 90 CW";

            case 7:
                return "Mirror horizontal and rotate 90 CW";

            case 8:
                return "Rotate 270 CW";

            default:
                return translateFallback(null, val);
        }
    }

    private static String translateResolutionUnit(Object val)
    {
        int num = convertToInt(val);

        switch (num)
        {
            case 1:
                return "None";

            case 2:
                return "inches";

            case 3:
                return "cm";

            default:
                return translateFallback(null, val);
        }
    }

    private static String translateCompression(Object val)
    {
        int num = convertToInt(val);

        switch (num)
        {
            case 1:
                return "Uncompressed";

            case 2:
                return "CCITT 1D";

            case 3:
                return "T4/Group 3 Fax";

            case 4:
                return "T6/Group 4 Fax";

            case 5:
                return "LZW";

            case 6:
                return "JPEG (old-style)";

            case 7:
                return "JPEG";

            case 8:
                return "Adobe Deflate";

            case 32773:
                return "PackBits";

            default:
                return translateFallback(null, val);
        }
    }

    private static String translateYCbCr(Object val)
    {
        int num = convertToInt(val);
        return (num == 1) ? "Centered" : (num == 2 ? "Co-sited" : translateFallback(null, val));
    }

    private static String translatePhotometric(Object val)
    {
        int num = convertToInt(val);

        switch (num)
        {
            case 0:
                return "WhiteIsZero";

            case 1:
                return "BlackIsZero";

            case 2:
                return "RGB";

            case 3:
                return "Palette";

            case 4:
                return "Transparency Mask";

            case 5:
                return "CMYK";

            case 6:
                return "YCbCr";

            case 8:
                return "CIELab";

            case 9:
                return "ICCLab";

            case 10:
                return "ITULab";

            case 32803:
                return "Color Filter Array";

            case 32844:
                return "Pixar LogL";

            case 32845:
                return "Pixar LogLuv";

            case 34892:
                return "Linear Raw";

            case 51177:
                return "Depth Map";

            case 52527:
                return "Semantic Mask";

            default:
                return translateFallback(null, val);
        }
    }

    private static String translatePlanarConfig(Object val)
    {
        int num = convertToInt(val);
        return (num == 1) ? "Chunky" : (num == 2 ? "Planar" : translateFallback(null, val));
    }

    private static String translateExtension(TagIFD_Extension tag, Object val)
    {
        switch (tag)
        {
            case IFD_CLEAN_FAX_DATA:
                return translateCleanFax(val);

            case IFD_INDEXED:
                return translateIndexed(val);

            case IFD_XP_TITLE:
            case IFD_XP_SUBJECT:
            case IFD_XP_COMMENT:
            case IFD_XP_KEYWORDS:
            case IFD_XP_AUTHOR:
                return translateXPString(val);

            default:
            break;
        }

        return translateFallback(tag, val);
    }

    private static String translateCleanFax(Object val)
    {
        int num = convertToInt(val);

        switch (num)
        {
            case 0:
                return "Clean";

            case 1:
                return "Regenerated";

            case 2:
                return "Unclean";

            default:
                return translateFallback(null, val);
        }
    }

    private static String translateIndexed(Object val)
    {
        int num = convertToInt(val);
        return (num == 1 ? "Indexed" : "Not indexed");
    }

    private static String translateXPString(Object val)
    {
        byte[] bytes;

        if (val instanceof byte[])
        {
            bytes = (byte[]) val;
        }

        else if (val instanceof int[])
        {
            bytes = ByteValueConverter.castToByteArray((int[]) val);
        }

        else
        {
            return String.valueOf(val);
        }

        int length = bytes.length;

        if (length % 2 != 0)
        {
            length--;
        }

        String decoded = new String(bytes, 0, length, StandardCharsets.UTF_16LE);
        int nullIdx = decoded.indexOf('\0');

        if (nullIdx != -1)
        {
            decoded = decoded.substring(0, nullIdx);
        }

        return decoded.trim();
    }

    private static String translatePrivate(TagIFD_Private tag, Object val)
    {
        switch (tag)
        {
            case IFD_DNG_VERSION:
            case IFD_DNG_BACKWARD_VERSION:
                return translateDngVersion(val);

            case IFD_PROFILE_EMBED_POLICY:
                return translateProfileEmbedPolicy(val);

            case IFD_PREVIEW_COLOR_SPACE:
                return translatePreviewColorSpace(val);

            case IFD_DEPTH_UNITS:
                return translateDepthUnits(val);

            case IFD_DEPTH_MEASURE_TYPE:
                return translateDepthMeasureType(val);

            default:
            break;
        }

        return translateFallback(tag, val);
    }

    private static String translateDngVersion(Object val)
    {
        if (val instanceof byte[])
        {
            byte[] b = (byte[]) val;

            if (b.length >= 4)
            {
                return String.format(Locale.ROOT, "%d.%d.%d.%d", b[0] & 0xFF, b[1] & 0xFF, b[2] & 0xFF, b[3] & 0xFF);
            }
        }

        if (val instanceof int[])
        {
            int[] i = (int[]) val;

            if (i.length >= 4)
            {
                return String.format(Locale.ROOT, "%d.%d.%d.%d", i[0], i[1], i[2], i[3]);
            }
        }

        return translateFallback(null, val);
    }

    private static String translateProfileEmbedPolicy(Object val)
    {
        switch (convertToInt(val))
        {
            case 0:
                return "Allow Copying";

            case 1:
                return "Embed if Used";

            case 2:
                return "Never Embed";

            case 3:
                return "No Restrictions";

            default:
                return translateFallback(null, val);
        }
    }

    private static String translatePreviewColorSpace(Object val)
    {
        switch (convertToInt(val))
        {
            case 1:
                return "sRGB";

            case 2:
                return "Adobe RGB";

            case 3:
                return "ProPhoto RGB";

            case 4:
                return "ColorMatch RGB";

            default:
                return translateFallback(null, val);
        }
    }

    private static String translateDepthUnits(Object val)
    {
        switch (convertToInt(val))
        {
            case 1:
                return "Meters";

            case 2:
                return "Inches";

            default:
                return translateFallback(null, val);
        }
    }

    private static String translateDepthMeasureType(Object val)
    {
        switch (convertToInt(val))
        {
            case 0:
                return "Unknown";

            case 1:
                return "Optical Depth";

            case 2:
                return "Inverse Optical Depth";

            default:
                return translateFallback(null, val);
        }
    }

    /**
     * Centralised fallback handler that replaces raw 'String.valueOf(Object)' calls. Keeps
     * primitive array pointers out of the UI stream and decodes ASCII version strings.
     */
    private static String translateFallback(Taggable tag, Object val)
    {
        if (tag != null && val instanceof byte[] && tag.toString().contains("VERSION"))
        {
            byte[] versionBytes = (byte[]) val;

            if (versionBytes.length >= 4)
            {
                return new String(versionBytes, 0, 4, StandardCharsets.US_ASCII).trim();
            }
        }

        if (val instanceof int[])
        {
            int[] arr = (int[]) val;
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

        if (val instanceof byte[])
        {
            byte[] arr = (byte[]) val;
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < arr.length; i++)
            {
                sb.append(arr[i] & 0xFF);

                if (i < arr.length - 1)
                {
                    sb.append(" ");
                }
            }

            return sb.toString();
        }

        return String.valueOf(val);
    }

    private static String formatRational(Object val)
    {
        if (val == null) return "";

        if (val instanceof Object[])
        {
            Object[] arr = (Object[]) val;
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < arr.length; i++)
            {
                sb.append(formatRational(arr[i]));

                if (i < arr.length - 1)
                {
                    sb.append(" ");
                }
            }

            return sb.toString();
        }

        if (val instanceof double[])
        {
            double[] arr = (double[]) val;
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

        if (val instanceof int[])
        {
            int[] arr = (int[]) val;
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

        else if (val instanceof Number)
        {
            return formatNumericValue(((Number) val).doubleValue());
        }

        return String.valueOf(val).replace("/1", "");
    }

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

    private static int convertToInt(Object val)
    {
        if (val instanceof Number)
        {
            if (val instanceof Byte)
            {
                return ((Byte) val).intValue() & 0xFF;
            }

            return ((Number) val).intValue();
        }

        else if (val instanceof int[] && ((int[]) val).length > 0)
        {
            return ((int[]) val)[0];
        }

        else if (val instanceof byte[] && ((byte[]) val).length > 0)
        {
            return ((byte[]) val)[0] & 0xFF;
        }

        return -1;
    }

    @SuppressWarnings("unused")
    private static double convertToDouble(Object val)
    {
        if (val instanceof Number)
        {
            return ((Number) val).doubleValue();
        }

        else if (val instanceof RationalNumber)
        {
            return ((RationalNumber) val).doubleValue();
        }

        else if (val instanceof double[] && ((double[]) val).length > 0)
        {
            return ((double[]) val)[0];
        }

        else if (val instanceof float[] && ((float[]) val).length > 0)
        {
            return ((float[]) val)[0];
        }

        else if (val instanceof int[] && ((int[]) val).length > 0)
        {
            return ((int[]) val)[0];
        }

        else if (val instanceof byte[] && ((byte[]) val).length > 0)
        {
            return ((byte[]) val)[0] & 0xFF;
        }

        return Double.NaN;
    }
}
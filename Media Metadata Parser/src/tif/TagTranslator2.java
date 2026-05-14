package tif;

import java.nio.charset.StandardCharsets;
import common.ByteValueConverter;
import tif.tagspecs.*;

public class TagTranslator2
{
    public static String getDisplayName(DirectoryIdentifier dir, Taggable tag)
    {
        // IFD1 is almost always the thumbnail directory
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
        if (value == null) return "";

        // 1. Global Hint-based handling (High Priority)
        if (tag.getHint() == TagHint.HINT_UCS2)
        {
            return translateXPString(value);
        }

        if (tag.getHint() == TagHint.HINT_RATIONAL)
        {
            return formatRational(value);
        }

        if (tag.isUnknown() || tag.getHint() == TagHint.HINT_BYTE_STREAM)
        {
            return formatBinarySummary(value);
        }

        // 2. Directory-specific translations
        if (tag instanceof TagIFD_Baseline)
        {
            return translateBaseline((TagIFD_Baseline) tag, value);
        }

        if (tag instanceof TagIFD_Exif)
        {
            return translateExif((TagIFD_Exif) tag, value);
        }

        if (tag instanceof TagIFD_GPS)
        {
            return translateGPS((TagIFD_GPS) tag, value);
        }

        // Fallback for everything else (Strings, Shorts, Longs)
        if (value instanceof byte[] || value instanceof int[])
        {
            return formatBinarySummary(value);
        }

        return String.valueOf(value);
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
            case IFD_XRESOLUTION:
            case IFD_YRESOLUTION:
                return formatRational(val);
            default:
                return String.valueOf(val);
        }
    }

    private static String translateExif(TagIFD_Exif tag, Object val)
    {
        switch (tag)
        {
            case EXIF_EXPOSURE_PROGRAM:
                return translateExposureProgram(val);
            case EXIF_METERING_MODE:
                return translateMeteringMode(val);
            case EXIF_COLOR_SPACE:
                return translateColorSpace(val);
            case EXIF_XPTITLE:
            case EXIF_XPSUBJECT:
            case EXIF_XPCOMMENT:
            case EXIF_XPKEYWORDS:
                return translateXPString(val);
            default:
                return String.valueOf(val);
        }
    }

    private static String translateGPS(TagIFD_GPS tag, Object val)
    {
        switch (tag)
        {
            case GPS_LATITUDE_REF:
            case GPS_LONGITUDE_REF:
                return String.valueOf(val).trim(); // E.g., "N" or "W"
            case GPS_ALTITUDE_REF:
                if (!(val instanceof Number)) return String.valueOf(val);
                return ((Number) val).intValue() == 0 ? "Above Sea Level" : "Below Sea Level";
            default:
                return String.valueOf(val);
        }
    }

    /* --- Helper Methods --- */
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

        /*
         * Windows XP tags (Title, Author, etc.) are explicitly encoded in UTF-16LE. The .trim()
         * effectively removes the trailing null terminators (0x00 0x00) common in these TIFF
         * fields.
         */
        return new String(bytes, StandardCharsets.UTF_16LE).trim();
    }

    private static String formatRational(Object val)
    {
        if (val == null)
        {
            return "";
        }

        // Handle Arrays (Crucial for GPS/Lens data)
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

        else if (val instanceof RationalNumber)
        {
            RationalNumber r = (RationalNumber) val;

            if (r.hasIntegerValue())
            {
                return String.valueOf(r.longValue());
            }

            return formatNumericValue(r.doubleValue());
        }

        else if (val instanceof Number)
        {
            return formatNumericValue(((Number) val).doubleValue());
        }

        else
        {
            return String.valueOf(val).replace("/1", "");
        }
    }

    private static String formatBinarySummary(Object val)
    {
        if (val != null)
        {
            if (val instanceof byte[])
            {
                return String.format("(Binary data %d bytes)", ((byte[]) val).length);
            }

            else if (val instanceof int[])
            {
                return String.format("(Binary data %d elements)", ((int[]) val).length);
            }

            else if (val instanceof Object[])
            {
                return String.format("(Binary data %d elements)", ((Object[]) val).length);
            }

            else
            {
                return String.valueOf(val);
            }
        }

        return "";
    }

    private static String translatePhotometric(Object val)
    {
        if (!(val instanceof Number)) return String.valueOf(val);
        switch (((Number) val).intValue())
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
            default:
                return String.valueOf(val);
        }
    }

    private static String translatePlanarConfig(Object val)
    {
        if (!(val instanceof Number)) return String.valueOf(val);
        switch (((Number) val).intValue())
        {
            case 1:
                return "Chunky";
            case 2:
                return "Planar";
            default:
                return String.valueOf(val);
        }
    }

    private static String translateOrientation(Object val)
    {
        switch (convertToInt(val))
        {
            case 1:
                return "Horizontal (normal)";
            case 3:
                return "Rotate 180";
            case 6:
                return "Rotate 90 CW";
            case 8:
                return "Rotate 270 CW";
            default:
                return String.valueOf(val);
        }
    }

    private static String translateYCbCr(Object val)
    {
        switch (convertToInt(val))
        {
            case 1:
                return "Centered";
            case 2:
                return "Co-sited";
            default:
                return String.valueOf(val);
        }
    }

    private static int convertToInt(Object val)
    {
        if (val == null)
        {
            return -1;
        }

        if (val instanceof Number)
        {
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

    private static String translateColorSpace(Object val)
    {
        if (!(val instanceof Number)) return String.valueOf(val);
        int i = ((Number) val).intValue();
        return (i == 1) ? "sRGB" : (i == 0xFFFF ? "Uncalibrated" : "Unknown (" + i + ")");
    }

    private static String translateCompression(Object val)
    {
        if (!(val instanceof Number))
        {
            return String.valueOf(val);
        }

        switch (((Number) val).intValue())
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
                return String.valueOf(val);
        }
    }

    private static String translateResolutionUnit(Object val)
    {
        if (!(val instanceof Number))
        {
            return String.valueOf(val);
        }

        switch (((Number) val).intValue())
        {
            case 1:
                return "None";
            case 2:
                return "inches";
            case 3:
                return "cm";
            default:
                return String.valueOf(val);
        }
    }

    private static String translateExposureProgram(Object val)
    {
        if (!(val instanceof Number)) return String.valueOf(val);
        switch (((Number) val).intValue())
        {
            case 1:
                return "Manual";
            case 2:
                return "Normal program";
            case 3:
                return "Aperture priority";
            case 4:
                return "Shutter priority";
            default:
                return String.valueOf(val);
        }
    }

    private static String translateMeteringMode(Object val)
    {
        if (!(val instanceof Number)) return String.valueOf(val);

        switch (((Number) val).intValue())
        {
            case 1:
                return "Average";
            case 2:
                return "Center-weighted average";
            case 3:
                return "Spot";
            case 4:
                return "Multi-spot";
            case 5:
                return "Pattern";
            default:
                return String.valueOf(val);
        }
    }

    /**
     * Separate the "Aesthetics" from the "Logic". This ensures 72.0000 becomes 72, matching
     * ExifTool.
     */
    private static String formatNumericValue(double d)
    {
        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            return String.valueOf(d);
        }

        if (d == (long) d)
        {
            return String.format("%d", (long) d);
        }

        return String.format("%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}

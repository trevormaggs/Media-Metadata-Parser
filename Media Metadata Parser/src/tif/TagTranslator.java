package tif;

import java.nio.charset.StandardCharsets;
import common.ByteValueConverter;
import tif.tagspecs.*;

/**
 * Utility class responsible for translating raw TIFF/EXIF tag values into human-readable strings.
 * *
 * <p>
 * This class handles various data types including Windows XP UCS-2 strings, rational numbers, and
 * enumerated constants for standard tags like Orientation, Compression, and GPS data. The output is
 * designed to closely match the formatting of industry-standard tools like ExifTool.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 14 May 2026
 */
public class TagTranslator
{
    /**
     * Determines the appropriate display name for a tag based on its directory context. For
     * example, JPEG offsets in IFD1 are traditionally labelled as {@code Thumbnail} attributes.
     * 
     * @param dir
     *        The directory identifier where the tag resides
     * @param tag
     *        the tag definition
     * @return a localised or descriptive string name for the tag
     */
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

    /**
     * The primary entry point for value translation. Routes values to specific handlers based on
     * tag hints or directory-specific logic.
     * 
     * @param tag
     *        the tag definition containing metadata hints
     * @param value
     *        the raw object value extracted from the image file
     * @return a formatted string representation of the value
     */
    public static String translate(Taggable tag, Object value)
    {
        if (value == null)
        {
            return "";
        }

        // 1. HIGH PRIORITY: Directory-Specific Logic
        // We must check these first so specialized math (like APEX)
        // is applied before generic rational formatting.
        if (tag instanceof TagIFD_Baseline)
        {
            return translateBaseline((TagIFD_Baseline) tag, value);
        }

        else if (tag instanceof TagIFD_Exif)
        {
            return translateExif((TagIFD_Exif) tag, value);
        }

        else if (tag instanceof TagIFD_GPS)
        {
            return translateGPS((TagIFD_GPS) tag, value);
        }

        // 2. MEDIUM PRIORITY: Data Encoding Hints
        else if (tag.getHint() == TagHint.HINT_UCS2)
        {
            return translateXPString(value);
        }

        else if (tag.getHint() == TagHint.HINT_UNDEFINED)
        {
            byte[] bytes = null;

            if (value instanceof byte[])
            {
                bytes = (byte[]) value;
            }

            else if (value instanceof int[])
            {
                bytes = ByteValueConverter.castToByteArray((int[]) value);
            }

            if (bytes != null)
            {
                String s = new String(bytes, StandardCharsets.US_ASCII).trim();
                // If it's a version tag (like 0232), return it.
                // If it's random binary junk, the trim() might result in an empty string.
                return s.isEmpty() ? formatBinarySummary(value) : s;
            }
        }

        // 3. LOW PRIORITY: Generic Type Fallbacks
        else if (tag.getHint() == TagHint.HINT_RATIONAL)
        {
            return formatRational(value);
        }

        else if (tag.isUnknown() || tag.getHint() == TagHint.HINT_BYTE_STREAM || value instanceof byte[] || value instanceof int[])
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
            case EXIF_TAG_EXIF_VERSION:
            case EXIF_FLASHPIX_VERSION:
                // Explicitly handle these version strings
                if (val instanceof byte[])
                {
                    return new String((byte[]) val, StandardCharsets.US_ASCII).trim();
                }
                else if (val instanceof int[])
                {
                    return new String(ByteValueConverter.castToByteArray((int[]) val), StandardCharsets.US_ASCII).trim();
                }
                return String.valueOf(val);
            case EXIF_APERTURE_VALUE:
            case EXIF_MAX_APERTURE_VALUE:
                double apexAperture = convertToDouble(val);
                if (Double.isNaN(apexAperture)) return String.valueOf(val);
                double fNumber = Math.pow(2, apexAperture / 2.0);
                return formatNumericValue(fNumber);

            case EXIF_SHUTTER_SPEED_VALUE:
                double apexShutter = convertToDouble(val);
                // Formula: shutter speed = 1 / (2 ^ apex)
                double exposureTime = 1.0 / Math.pow(2, apexShutter);
                // Return as fraction if very small
                if (exposureTime < 1.0)
                {
                    return "1/" + Math.round(1.0 / exposureTime);
                }
                return formatNumericValue(exposureTime);
            case EXIF_COMPONENTS_CONFIGURATION:
                return translateComponentsConfiguration(val);
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
                return String.valueOf(val).trim();
            case GPS_ALTITUDE_REF:
                int i = convertToInt(val);
                return (i == 0) ? "Above Sea Level" : (i == 1 ? "Below Sea Level" : String.valueOf(val));
            default:
                return String.valueOf(val);
        }
    }

    /* --- Helper Methods --- */

    /**
     * Decodes Windows-specific {@code XP} tags, for example: {@code XPTitle, XPAuthor, XPSubject}.
     * 
     * <p>
     * These tags are unique outliers in the TIFF specification. While standard TIFF strings use
     * ASCII, Microsoft encodes these as <b>UTF-16LE</b>. Furthermore, they are stored as
     * {@code BYTE} types rather than {@code ASCII} types, which often results in binary-level
     * quirks handled here:
     * </p>
     * 
     * <ul>
     * <li><b>Type Casting:</b> Converts {@code int[]} back to {@code byte[]} using
     * {@link ByteValueConverter#castToByteArray}, reversing the expansion performed by the IFD
     * parser for unsigned bytes.</li>
     * <li><b>The Dangling Byte:</b> Because UTF-16LE requires 2 bytes per character, an
     * odd-lengthed array (often caused by single-byte padding) is malformed. This method truncates
     * the "dangling" byte to prevent the insertion of replacement characters ({@code \uFFFD}).</li>
     * <li><b>Null Termination:</b> Unlike standard Java strings, these decoded buffers often
     * contain trailing null characters ({@code \0}) followed by random padding data. This method
     * performs a double-cleanup by truncating at the first null and then trimming whitespace.</li>
     * </ul>
     * 
     * @param val
     *        the raw tag value (expected as {@code byte[]} or {@code int[]})
     * @return a sanitised, human-readable string, or {@code String.valueOf(val)} if the input is
     *         not a byte-based array
     */
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

        // Windows often leaves multiple null terminators or junk after the null.
        // Standard String.trim() ignores characters > U+0020, so we manually
        // find the first logical null character.
        int nullIdx = decoded.indexOf('\0');

        if (nullIdx != -1)
        {
            decoded = decoded.substring(0, nullIdx);
        }

        return decoded.trim();
    }

    /**
     * Formats RationalNumbers and arrays of Rational Numbers (common in GPS data) into a clean,
     * human-readable string format.
     * 
     * @param val
     *        the RationalNumber, RationalNumber[], or other Number types
     * @return a formatted string, such as "72" or "51 30 12.5"
     */
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

        else if (val instanceof RationalNumber)
        {
            RationalNumber r = (RationalNumber) val;

            if (r.hasIntegerValue())
            {
                // Whole number, for example: show 72/1 as "72"
                return String.valueOf(r.longValue());
            }

            double d = r.doubleValue();

            // if value too small (i.e., shutter speeds), keep fraction,
            // otherwise decimal number, for example: 18/10 -> 1.8
            if (d < 0.1)
            {
                // Fraction is more clearer, i.e., Returns "1/1297"
                return r.toString();
            }

            // Otherwise, decimal number is good here, like 1.8 or 2.5
            return formatNumericValue(d);
        }

        else if (val instanceof Number)
        {
            return formatNumericValue(((Number) val).doubleValue());
        }

        return String.valueOf(val).replace("/1", "");
    }

    /**
     * Provides a brief summary for large binary data blocks (like ICC Profiles) to prevent
     * polluting the output with raw memory addresses or large hex dumps.
     * 
     * @param val
     *        the binary array (byte[], int[], or Object[])
     * @return a summary string indicating the size and type of data
     */
    private static String formatBinarySummary(Object val)
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

        return (val == null) ? "" : String.valueOf(val);
    }

    /**
     * Translates the PhotometricInterpretation tag (0x0106). This tag describes the color space of
     * the image data.
     * 
     * @param val
     *        the raw value (Number, int[], or byte[])
     * @return the human-readable color space name, for example: "RGB", "YCbCr"
     */
    private static String translatePhotometric(Object val)
    {
        switch (convertToInt(val))
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

    /**
     * Translates the PlanarConfiguration tag (0x011C). Indicates if color components are stored
     * interleaved or in separate planes.
     * 
     * * @param val the raw value (Number, int[], or byte[])
     * 
     * @return "Chunky", "Planar", or the raw value if unknown
     */
    private static String translatePlanarConfig(Object val)
    {
        int i = convertToInt(val);
        return (i == 1) ? "Chunky" : (i == 2 ? "Planar" : String.valueOf(val));
    }

    /**
     * Translates the Orientation tag (0x0112). Maps numeric values to descriptions of image
     * rotation and mirroring.
     * 
     * @param val
     *        the raw value (Number, int[], or byte[])
     * @return a description of the orientation, for example: "Rotate 90 CW"
     */
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

    /**
     * Translates the YCbCrPositioning tag (0x0213). Defines the position of chroma components
     * relative to luma samples.
     * 
     * @param val
     *        the raw value (Number, int[], or byte[])
     * @return "Centered", "Co-sited", or the raw value
     */
    private static String translateYCbCr(Object val)
    {
        int i = convertToInt(val);
        return (i == 1) ? "Centered" : (i == 2 ? "Co-sited" : String.valueOf(val));
    }

    /**
     * Translates the ColorSpace tag (0xA001). Identifies the color space used for the image data.
     * 
     * @param val
     *        the raw value (Number, int[], or byte[])
     * @return "sRGB", "Uncalibrated", or "Unknown" with the ID
     */
    private static String translateColorSpace(Object val)
    {
        int i = convertToInt(val);
        return (i == 1) ? "sRGB" : (i == 0xFFFF ? "Uncalibrated" : "Unknown (" + i + ")");
    }

    /**
     * Translates the Compression tag (0x0103). Maps TIFF compression scheme identifiers to their
     * technical names.
     * 
     * @param val
     *        the raw value (Number, int[], or byte[])
     * @return the name of the compression scheme, such as "LZW", "PackBits"
     */
    private static String translateCompression(Object val)
    {
        switch (convertToInt(val))
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

    /**
     * Translates the ResolutionUnit tag (0x0128). Defines the measurement unit for XResolution and
     * YResolution.
     * 
     * @param val
     *        the raw value (Number, int[], or byte[])
     * @return "None", "inches", or "cm"
     */
    private static String translateResolutionUnit(Object val)
    {
        switch (convertToInt(val))
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

    /**
     * Translates the ExposureProgram tag (0x8822). Describes the camera's mode for setting
     * exposure.
     * 
     * @param val
     *        the raw value (Number, int[], or byte[])
     * @return the exposure mode, for example: {@code Aperture priority}
     */
    private static String translateExposureProgram(Object val)
    {
        switch (convertToInt(val))
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

    /**
     * Translates the MeteringMode tag (0x9207). Describes how the camera measures light to
     * determine exposure.
     * 
     * @param val
     *        the raw value (Number, int[], or byte[])
     * @return the metering mode, such as "Spot", "Pattern"
     */
    private static String translateMeteringMode(Object val)
    {
        switch (convertToInt(val))
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

    private static String translateComponentsConfiguration(Object val)
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

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytes.length; i++)
        {
            int component = bytes[i] & 0xFF;

            switch (component)
            {
                case 0:
                    sb.append("-");
                break;
                case 1:
                    sb.append("Y");
                break;
                case 2:
                    sb.append("Cb");
                break;
                case 3:
                    sb.append("Cr");
                break;
                case 4:
                    sb.append("R");
                break;
                case 5:
                    sb.append("G");
                break;
                case 6:
                    sb.append("B");
                break;
                default:
                    sb.append("Unknown");
            }

            if (i < bytes.length - 1)
            {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    /**
     * Extracts an integer from various object types (Number, int[], byte[]) to simplify switch
     * statements and handle parser "boxing" quirks.
     * 
     * @param val
     *        the object to convert
     * @return the integer value, or -1 if the conversion is not possible
     */
    private static int convertToInt(Object val)
    {
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

    /**
     * Extracts a double from various object types (Number, RationalNumber, arrays).
     * Useful for tags that require decimal precision like FNumber or GPS coordinates.
     */
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

        // Fallback for single-element integer/byte arrays
        int i = convertToInt(val);

        return (i != -1) ? (double) i : Double.NaN;
    }

    /**
     * Formats floating-point values to a maximum of 4 decimal places, stripping trailing zeros and
     * decimal points to match professional metadata tools like ExifTool.
     * 
     * @param d
     *        the double-precision value to format
     * @return a clean string representation of the number
     */
    private static String formatNumericValue(Object val)
    {
        double d = convertToDouble(val);

        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            return String.valueOf(val);
        }

        // Check if it's effectively an integer (e.g., 5.0)
        if (d == (long) d)
        {
            return String.format("%d", (long) d);
        }

        // Clean formatting to 4 decimal places, stripping trailing zeros
        return String.format("%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
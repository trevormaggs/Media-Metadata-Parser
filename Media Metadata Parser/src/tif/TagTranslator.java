package tif;

import java.nio.charset.StandardCharsets;
import common.ByteValueConverter;
import tif.tagspecs.TagIFD_Baseline;
import tif.tagspecs.TagIFD_Extension;
import tif.tagspecs.TagIFD_Private;
import tif.tagspecs.Taggable;

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
        if (value != null)
        {
            // TODO: NEED TO TEST ITS LOGICAL PLACEMENT FOR BLOB DATA BEFORE BASELINE
            if (tag.getHint() == TagHint.HINT_RATIONAL)
            {
                return formatRational(value);
            }

            else if (tag.getHint() == TagHint.HINT_BYTE_STREAM)
            {
                if (value instanceof byte[])
                {
                    return String.format("[Binary Data: %d bytes]", ((byte[]) value).length);
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

            return String.valueOf(value);
        }

        return "";
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

        return String.valueOf(val);
    }

    /**
     * Translates the Orientation tag (0x0112). Maps numeric values to descriptions of image
     * rotation and mirroring.
     *
     * @param val
     *        the raw value as a Number
     * @return a description of the orientation, for example: "Rotate 90 CW"
     */
    private static String translateOrientation(Object val)
    {
        int num = (val instanceof Number ? ((Number) val).intValue() : convertToInt(val));

        switch (num)
        {
            case 1:
                return "Horizontal (normal)"; // default
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
                return String.valueOf(val);
        }
    }

    /**
     * Translates the Resolution Unit tag (0x0128). Defines the measurement unit for XResolution and
     * YResolution.
     *
     * @param val
     *        the raw value as a Number
     * @return "None", "inches" or "cm"
     */
    private static String translateResolutionUnit(Object val)
    {
        int num = (val instanceof Number ? ((Number) val).intValue() : convertToInt(val));

        switch (num)
        {
            case 1:
                return "None";
            case 2:
                return "inches"; // default
            case 3:
                return "cm";
            default:
                return String.valueOf(val);
        }
    }

    /**
     * Translates the Compression tag (0x0103). Maps TIFF compression scheme identifiers to their
     * technical names.
     *
     * @param val
     *        the raw value as a Number
     * @return the name of the compression scheme, such as "LZW", "PackBits"
     */
    private static String translateCompression(Object val)
    {
        int num = (val instanceof Number ? ((Number) val).intValue() : convertToInt(val));

        switch (num)
        {
            case 1:
                return "Uncompressed";// default
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
     * Translates the YCbCr Positioning tag (0x0213). Defines the position of chroma components
     * relative to luma samples.
     *
     * @param val
     *        the raw value as a Number
     * @return "Centered" (default), "Co-sited", or the raw value
     */
    private static String translateYCbCr(Object val)
    {
        int num = (val instanceof Number ? ((Number) val).intValue() : convertToInt(val));

        return (num == 1) ? "Centered" : (num == 2 ? "Co-sited" : String.valueOf(val));
    }

    /**
     * Translates the Photometric Interpretation tag (0x0106). This tag describes the color space of
     * the image data.
     *
     * @param val
     *        the raw value as a Number
     * @return the human-readable colour space name, for example: "RGB", "YCbCr"
     */
    private static String translatePhotometric(Object val)
    {
        int num = (val instanceof Number ? ((Number) val).intValue() : convertToInt(val));

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
                return String.valueOf(val);
        }
    }

    /**
     * Translates the PlanarConfiguration tag (0x011C). Indicates if colour components are stored
     * interleaved or in separate planes.
     * 
     * @param val
     *        the raw value as a Number
     * @return either "Chunky" (default), "Planar", or the raw value if unknown. Note "Chunky" is
     *         Default
     */
    private static String translatePlanarConfig(Object val)
    {
        int num = (val instanceof Number ? ((Number) val).intValue() : convertToInt(val));

        return (num == 1) ? "Chunky" : (num == 2 ? "Planar" : String.valueOf(val));
    }

    /* ---------- IFD Extension Tags ---------- */

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

        if (tag.getHint() == TagHint.HINT_RATIONAL)
        {
            return formatRational(val);
        }

        return String.valueOf(val);
    }

    private static String translateCleanFax(Object val)
    {
        int num = (val instanceof Number ? ((Number) val).intValue() : convertToInt(val));

        switch (num)
        {
            case 0:
                return "Clean";
            case 1:
                return "Regenerated";
            case 2:
                return "Unclean";
            default:
                return String.valueOf(val);
        }
    }

    /**
     * Translates the Image Indexed tag (0x015A). Defines whether the image should be indexed or
     * not.
     *
     * @param val
     *        the raw value as a Number
     * @return "Indexed", "Not Indexed", or the raw value
     */
    private static String translateIndexed(Object val)
    {
        int num = (val instanceof Number ? ((Number) val).intValue() : convertToInt(val));

        return (num == 1 ? "Indexed" : "Not indexed");
    }

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

    /* ---------- IFD Private Tags ---------- */

    private static String translatePrivate(TagIFD_Private tag, Object val)
    {
        switch (tag)
        {
            case IFD_DNG_VERSION:
            case IFD_DNG_BACKWARD_VERSION:
                //return translateDngVersion(val);
            case IFD_PROFILE_EMBED_POLICY:
                //return translateProfileEmbedPolicy(val);
            case IFD_PREVIEW_COLOR_SPACE:
                //return translatePreviewColorSpace(val);
            case IFD_DEPTH_UNITS:
                //return translateDepthUnits(val);
            case IFD_DEPTH_MEASURE_TYPE:
                //return translateDepthMeasureType(val);
            default:
            break;
        }

        return String.valueOf(val);
    }

    /* ---------- Helper Utilities ---------- */

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
        if (val != null)
        {
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
                    // Whole number, for example: 72/1 -> 72
                    return String.valueOf(r.longValue());
                }

                double d = r.doubleValue();

                // if value too small (i.e., shutter speeds), keep fraction,
                // otherwise decimal number, for example: 18/10 -> 1.8
                if (d < 0.1)
                {
                    // Fraction is more clearer, i.e., 1/1297
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

        return "";
    }

    /**
     * Formats floating-point values to a maximum of 4 decimal places, stripping trailing zeros and
     * decimal points to match professional metadata tools like ExifTool.
     *
     * @param val
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

        // Check if it is a clean integer
        if (d == (long) d)
        {
            return String.format("%d", (long) d);
        }

        return String.format("%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /**
     * Normalises data embedded in the specified Object type (such as Number, int[], or byte[]) into
     * a primary integer value.
     * 
     * <p>
     * This method handles "boxed" single-element arrays—specifically {@code int[]} or
     * {@code byte[]}, which are often produced by parsers when a tag has a count of one (scalar
     * type).
     * </p>
     * 
     * @param val
     *        the raw object value to resolve
     * @return the resolved integer value, or {@code -1} if the type is null or unsupported
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
     * 
     * @param val
     *        the raw object value to resolve
     * @return the resolved double value 
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
}
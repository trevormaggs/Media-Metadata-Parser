package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;
import tif.TagValueTranslator;

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

    /**
     * Converts a raw tag value into a display string.
     *
     * @param val
     *        the value to convert
     * @return a formatted string representation of the value, or an empty string if the value is
     *         {@code null}
     */
    default String translate(Object val)
    {
        return (val == null ? "" : TagValueTranslator.toStringValue(val, getHint()));
    }

    /**
     * Extracts the first numeric value from a metadata payload.
     *
     * <p>
     * Supports numeric wrapper types and primitive numeric arrays. Unsigned byte and short values
     * are widened to preserve their full range. If the value cannot be converted, {@code -1} is
     * returned.
     * </p>
     *
     * @param val
     *        the value to evaluate
     * @return the converted integer value, or {@code -1} if unavailable
     */
    static int convertToInt(Object val)
    {
        if (val == null)
        {
            return -1;
        }

        if (val instanceof Number)
        {
            if (val instanceof Byte)
            {
                // Preserve unsigned byte range
                return ((Byte) val).intValue() & 0xFF;
            }

            return ((Number) val).intValue();
        }

        if (val instanceof byte[] && ((byte[]) val).length > 0)
        {
            return ((byte[]) val)[0] & 0xFF;
        }

        if (val instanceof short[] && ((short[]) val).length > 0)
        {
            return ((short[]) val)[0] & 0xFFFF;
        }

        if (val instanceof int[] && ((int[]) val).length > 0)
        {
            return ((int[]) val)[0];
        }

        if (val instanceof long[] && ((long[]) val).length > 0)
        {
            return (int) ((long[]) val)[0];
        }

        return -1;
    }

    /**
     * Translates standard resolution units shared between baseline TIFF and EXIF focal plane
     * configurations.
     * 
     * <p>
     * This utility is shared by {@code IFD_RESOLUTION_UNIT} (0x0128) in baseline directories, as
     * well as {@code EXIF_FOCAL_PLANE_RESOLUTION_UNIT} (0xA210) within EXIF metadata
     * sub-directories.
     * </p>
     * 
     * @param val
     *        the raw unit tag value container
     * @return the human-readable unit string, or a default fallback if unmapped
     */
    static String translateResolutionUnit(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 1:
                return "None";

            case 2:
                return "inch";

            case 3:
                return "cm";

            case 4:
                return "mm";

            case 5:
                return "um";

            default:
                return String.valueOf(val).trim();
        }
    }

    /**
     * Translates a standard light source or calibration illuminant code into a human-readable
     * string.
     * 
     * <p>
     * This utility is also shared by {@code EXIF_LIGHT_SOURCE}, which indicates the specific
     * capture white balance setting, as well as the DNG {@code IFD_CALIBRATION_ILLUMINANT1} through
     * to {@code IFD_CALIBRATION_ILLUMINANT4} tags, which define the reference lighting environments
     * used for factory sensor color profiling.
     * </p>
     * 
     * @param val
     *        the raw metadata value
     * @return the translated description, or the raw value if the code is unknown
     */
    static String translateLightSource(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Unknown";

            case 1:
                return "Daylight";

            case 2:
                return "Fluorescent";

            case 3:
                return "Tungsten (Incandescent)";

            case 4:
                return "Flash";

            case 9:
                return "Fine Weather";

            case 10:
                return "Cloudy";

            case 11:
                return "Shade";

            case 12:
                return "Daylight Fluorescent (D 5700 - 7100K)";

            case 13:
                return "Day White Fluorescent (N 4600 - 5400K)";

            case 14:
                return "Cool White Fluorescent (W 3900 - 4500K)";

            case 15:
                return "White Fluorescent (WW 3200 - 3700K)";

            case 17:
                return "Standard Light A";

            case 18:
                return "Standard Light B";

            case 19:
                return "Standard Light C";

            case 20:
                return "D55";

            case 21:
                return "D65";

            case 22:
                return "D75";

            case 23:
                return "D50";

            case 24:
                return "ISO Studio Tungsten";

            case 25:
                return "Camera view finder flash";

            case 255:
                return "Other";

            default:
                return String.valueOf(val).trim();
        }
    }
}
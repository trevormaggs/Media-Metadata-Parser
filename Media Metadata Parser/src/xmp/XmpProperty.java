package xmp;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tif.DirectoryIdentifier;
import tif.TagHint;
import tif.tagspecs.Taggable;
import util.SmartDateParser;

/**
 * Represents a fixed set of known XMP properties and their associated namespaces. This enumeration
 * provides a type-safe mechanism for identifying XMP properties, enabling efficient lookups while
 * avoiding errors caused by misspelled property names.
 *
 * <p>
 * This registry covers the most common schemas used in digital imaging, including:
 * </p>
 *
 * <ul>
 * <li><b>DC:</b> Dublin Core descriptive metadata.</li>
 * <li><b>XMP:</b> XMP Basic workflow metadata.</li>
 * <li><b>TIFF/EXIF:</b> Binary image metadata mirrored into XMP.</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 21 January 2026
 */
public enum XmpProperty implements Taggable
{
    DC_CONTRIBUTOR("contributor", NameSpace.DC),
    DC_COVERAGE("coverage", NameSpace.DC),
    DC_CREATOR("creator", NameSpace.DC),
    DC_DATE("date", NameSpace.DC),
    DC_DESCRIPTION("description", NameSpace.DC),
    DC_FORMAT("format", NameSpace.DC),
    DC_IDENTIFIER("identifier", NameSpace.DC),
    DC_LANGUAGE("language", NameSpace.DC),
    DC_PUBLISHER("publisher", NameSpace.DC),
    DC_RELATION("relation", NameSpace.DC),
    DC_RIGHTS("rights", NameSpace.DC),
    DC_SOURCE("source", NameSpace.DC),
    DC_SUBJECT("subject", NameSpace.DC),
    DC_TITLE("title", NameSpace.DC),
    DC_TYPE("type", NameSpace.DC),

    XMP_CREATEDATE("CreateDate", NameSpace.XMP),
    XMP_CREATORTOOL("CreatorTool", NameSpace.XMP),
    XMP_METADATADATE("MetadataDate", NameSpace.XMP),
    XMP_MODIFYDATE("ModifyDate", NameSpace.XMP),
    XMP_ADVISORY("Advisory", NameSpace.XMP),
    XMP_BASEURL("BaseURL", NameSpace.XMP),
    XMP_IDENTIFIER("Identifier", NameSpace.XMP),
    XMP_LABEL("Label", NameSpace.XMP),
    XMP_NICKNAME("Nickname", NameSpace.XMP),
    XMP_RATING("Rating", NameSpace.XMP),
    XMP_THUMBNAILS("Thumbnails", NameSpace.XMP),

    XMPMM_DOCUMENTID("DocumentID", NameSpace.XMPMM),
    XMPMM_INSTANCEID("InstanceID", NameSpace.XMPMM),
    XMPMM_ORIGINALDOCUMENTID("OriginalDocumentID", NameSpace.XMPMM),
    XMPMM_HISTORY("History", NameSpace.XMPMM),
    XMPMM_DERIVEDFROM("DerivedFrom", NameSpace.XMPMM),
    XMPMM_RENDITIONCLASS("RenditionClass", NameSpace.XMPMM),
    XMPMM_VERSIONID("VersionID", NameSpace.XMPMM),
    XMPMM_VERSIONS("Versions", NameSpace.XMPMM),
    XMPMM_INGREDIENTS("Ingredients", NameSpace.XMPMM),

    // Often used to store basic image data like resolution and orientation
    TIFF_ORIENTATION("Orientation", NameSpace.TIFF),
    TIFF_XRESOLUTION("XResolution", NameSpace.TIFF),
    TIFF_YRESOLUTION("YResolution", NameSpace.TIFF),
    TIFF_RESOLUTION_UNIT("ResolutionUnit", NameSpace.TIFF),
    TIFF_DATE_TIME("DateTime", NameSpace.TIFF),
    TIFF_IMAGE_DESCRIPTION("ImageDescription", NameSpace.TIFF),
    TIFF_MAKE("Make", NameSpace.TIFF),
    TIFF_MODEL("Model", NameSpace.TIFF),
    TIFF_SOFTWARE("Software", NameSpace.TIFF),

    // Used for camera and capture settings
    EXIF_DATE_TIME_ORIGINAL("DateTimeOriginal", NameSpace.EXIF),
    EXIF_DATE_TIME_DIGITIZED("DateTimeDigitized", NameSpace.EXIF),
    EXIF_EXPOSURE_TIME("ExposureTime", NameSpace.EXIF),
    EXIF_FNUMBER("FNumber", NameSpace.EXIF),
    EXIF_ISO_SPEED_RATINGS("ISOSpeedRatings", NameSpace.EXIF),
    EXIF_SHUTTER_SPEED_VALUE("ShutterSpeedValue", NameSpace.EXIF),
    EXIF_APERTURE_VALUE("ApertureValue", NameSpace.EXIF),
    EXIF_BRIGHTNESS_VALUE("BrightnessValue", NameSpace.EXIF),
    EXIF_FLASH("Flash", NameSpace.EXIF),
    EXIF_FOCAL_LENGTH("FocalLength", NameSpace.EXIF),
    EXIF_COLOUR_SPACE("ColorSpace", NameSpace.EXIF),
    EXIF_PIXEL_XDIMENSION("PixelXDimension", NameSpace.EXIF),
    EXIF_PIXEL_YDIMENSION("PixelYDimension", NameSpace.EXIF),

    EXIF_EXPOSURE_PROGRAM("ExposureProgram", NameSpace.EXIF),
    EXIF_METERING_MODE("MeteringMode", NameSpace.EXIF),
    EXIF_SENSING_METHOD("SensingMethod", NameSpace.EXIF),
    EXIF_SCENE_TYPE("SceneType", NameSpace.EXIF),
    EXIF_EXPOSURE_MODE("ExposureMode", NameSpace.EXIF),
    EXIF_WHITE_BALANCE("WhiteBalance", NameSpace.EXIF),
    EXIF_SCENE_CAPTURE_TYPE("SceneCaptureType", NameSpace.EXIF),

    EXIF_GPS_ALTITUDE_REF("GPSAltitudeRef", NameSpace.EXIF),
    EXIF_GPS_IMG_DIRECTION_REF("GPSImgDirectionRef", NameSpace.EXIF),
    EXIF_GPS_DEST_BEARING_REF("GPSDestBearingRef", NameSpace.EXIF),
    EXIF_GPS_SPEED_REF("GPSSpeedRef", NameSpace.EXIF),

    UNKNOWN("unknown", NameSpace.UNKNOWN);

    private static final Pattern RATIONAL_PATTERN = Pattern.compile("^(-?\\d+)/(\\d+)$");
    private static final Map<String, XmpProperty> NAME_LOOKUP = new HashMap<>();
    private final String propName;
    private final NameSpace schema;

    static
    {
        for (XmpProperty type : values())
        {
            if (type.schema != NameSpace.UNKNOWN)
            {
                String key = (type.getSchemaPrefix() + ":" + type.getPropertyName()).toLowerCase(Locale.ROOT);
                NAME_LOOKUP.put(key, type);
            }
        }
    }

    private XmpProperty(String propName, NameSpace schema)
    {
        this.propName = propName;
        this.schema = schema;
    }

    /**
     * Returns the local property name within the schema, for example: {@code creator} or
     * {@code CreateDate}.
     *
     * @return the local property name string
     */
    public String getPropertyName()
    {
        return propName;
    }

    /**
     * Returns the namespace prefix for this property.
     *
     * @return the namespace prefix (for example, {@code dc} or {@code xmp})
     */
    public NameSpace getNameSpaceConstant()
    {
        return schema;
    }

    /**
     * Returns the abbreviated prefix name of the schema.
     *
     * @return the abbreviated schema name, for example: dc, xmp, etc
     */
    public String getSchemaPrefix()
    {
        return schema.getPrefix();
    }

    /**
     * Returns the full URI of the associated XMP namespace.
     *
     * @return the full namespace URI
     */
    public String getNamespaceURI()
    {
        return schema.getURI();
    }

    /**
     * Returns the fully qualified property name consisting of the namespace prefix and local
     * property name.
     *
     * <p>
     * For example: {@code dc:creator} or {@code xmp:CreateDate}.
     * </p>
     *
     * @return the qualified property name, or an empty string if this constant is {@link #UNKNOWN}
     */
    public String getQualifiedPath()
    {
        if (this == UNKNOWN)
        {
            return "";
        }

        return getSchemaPrefix() + ":" + getPropertyName();
    }

    /**
     * Resolves the enumeration constant corresponding to the specified qualified property name.
     *
     * @param qualifiedPath
     *        the qualified property name (for example, {@code dc:format}); comparison is
     *        case-insensitive
     * @return the matching property, or {@link #UNKNOWN} if no match exists
     */
    public static XmpProperty fromQualifiedPath(String qualifiedPath)
    {
        if (qualifiedPath == null)
        {
            return UNKNOWN;
        }

        return NAME_LOOKUP.getOrDefault(qualifiedPath.toLowerCase(Locale.ROOT), UNKNOWN);
    }

    /**
     * Returns a hash-based numeric identifier derived from the qualified property name.
     *
     * @return the hash code of the qualified property name
     */
    @Override
    public int getNumberID()
    {
        return getQualifiedPath().hashCode();
    }

    /**
     * Returns the display hint associated with this property.
     *
     * @return the display hint
     */
    @Override
    public TagHint getHint()
    {
        return TagHint.HINT_DEFAULT;
    }

    /**
     * Returns the metadata directory to which this property belongs.
     *
     * @return the directory identifier
     */
    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return DirectoryIdentifier.IFD_ROOT_DIRECTORY;
    }

    /**
     * Returns a human-readable description of this property.
     *
     * @return the formatted property description
     */
    @Override
    public String getDescription()
    {
        return format(getPropertyName());
    }

    /**
     * Translates the supplied metadata value into a more human-readable representation where
     * applicable.
     *
     * @param val
     *        the raw metadata value
     * @return the translated value, or an empty string if {@code val} is {@code null}
     */
    @Override
    public String translate(Object val)
    {
        if (val != null && String.valueOf(val).length() > 0)
        {
            String rawStr = String.valueOf(val).trim();

            switch (this)
            {
                case TIFF_ORIENTATION:
                case TIFF_RESOLUTION_UNIT:
                    return translateTiffSchema(rawStr);

                case EXIF_EXPOSURE_PROGRAM:
                case EXIF_METERING_MODE:
                case EXIF_SENSING_METHOD:
                case EXIF_SCENE_TYPE:
                case EXIF_EXPOSURE_MODE:
                case EXIF_WHITE_BALANCE:
                case EXIF_SCENE_CAPTURE_TYPE:
                    return translateExifSchema(rawStr);

                case EXIF_GPS_ALTITUDE_REF:
                case EXIF_GPS_IMG_DIRECTION_REF:
                case EXIF_GPS_DEST_BEARING_REF:
                case EXIF_GPS_SPEED_REF:
                    return translateGpsSchema(rawStr);

                case EXIF_DATE_TIME_ORIGINAL:
                case XMP_CREATEDATE:
                case XMP_MODIFYDATE:
                case XMP_METADATADATE:
                    return translateTimeSchema(rawStr);

                default:
                break;
            }

            return evaluateGenericValue(rawStr);
        }

        return "";
    }

    /**
     * Converts a camel-case or Pascal-case property name into a human-readable form by inserting
     * spaces between words.
     *
     * <p>
     * Examples:
     * </p>
     *
     * <ul>
     * <li>{@code GPSAltitude} → {@code GPS Altitude}</li>
     * <li>{@code CreateDate} → {@code Create Date}</li>
     * <li>{@code Y2K} → {@code Y2 K}</li>
     * </ul>
     *
     * @param rawData
     *        the property name to format
     *
     * @return the formatted display name
     */
    public static String format(String rawData)
    {
        if (rawData == null || rawData.isEmpty() || rawData.equalsIgnoreCase("unknown"))
        {
            return "Unknown";
        }

        String name = rawData;

        // Example: "GPSAltitude" -> "GPS Altitude"
        name = name.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ");

        // Example: "CreateDate" -> "Create Date"
        name = name.replaceAll("(?<=[a-z])(?=[A-Z])", " ");

        // Example: "Y2K" -> "Y2 K"
        name = name.replaceAll("(?<=[0-9])(?=[A-Z])", " ");

        // Capitalise the absolute first character cleanly for custom unmapped entries
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    /**
     * Translates TIFF property values into human-readable descriptions.
     *
     * @param rawData
     *        the raw TIFF value
     * @return the translated value
     */
    private String translateTiffSchema(String rawData)
    {
        switch (this)
        {
            case TIFF_ORIENTATION:
                return rawData.equals("1") ? "Horizontal (normal)" : rawData;

            case TIFF_RESOLUTION_UNIT:
                return rawData.equals("2") ? "inches" : rawData.equals("3") ? "cm" : rawData;

            default:
                return rawData;
        }
    }

    /**
     * Translates EXIF enumeration values into descriptive text.
     *
     * @param rawData
     *        the raw EXIF value
     * @return the translated value
     */
    private String translateExifSchema(String rawData)
    {
        switch (this)
        {
            case EXIF_EXPOSURE_PROGRAM:
                return rawData.equals("2") ? "Program AE" : rawData;

            case EXIF_METERING_MODE:
                return rawData.equals("5") ? "Multi-segment" : rawData;

            case EXIF_SENSING_METHOD:
                return rawData.equals("2") ? "One-chip color area" : rawData;

            case EXIF_SCENE_TYPE:
                return rawData.equals("1") ? "Directly photographed" : rawData;

            case EXIF_EXPOSURE_MODE:
            case EXIF_WHITE_BALANCE:
                return rawData.equals("0") ? "Auto" : rawData;

            case EXIF_SCENE_CAPTURE_TYPE:
                return rawData.equals("0") ? "Standard" : rawData;

            default:
                return rawData;
        }
    }

    /**
     * Translates GPS-related enumeration values into descriptive text.
     *
     * @param rawData
     *        the raw GPS value
     * @return the translated value
     */
    private String translateGpsSchema(String rawData)
    {
        switch (this)
        {
            case EXIF_GPS_ALTITUDE_REF:
                return rawData.equals("0") ? "Above Sea Level" : rawData.equals("1") ? "Below Sea Level" : rawData;

            case EXIF_GPS_IMG_DIRECTION_REF:
            case EXIF_GPS_DEST_BEARING_REF:
                return rawData.equalsIgnoreCase("T") ? "True North" : rawData.equalsIgnoreCase("M") ? "Magnetic North" : rawData;

            case EXIF_GPS_SPEED_REF:
                return rawData.equalsIgnoreCase("K") ? "km/h" : rawData;

            default:
                return rawData;
        }
    }

    /**
     * Converts an XMP or EXIF date/time value into a localised display format.
     *
     * @param rawData
     *        the raw date/time value
     * @return the localised date/time string, or the original value if parsing fails
     */
    private String translateTimeSchema(String rawData)
    {
        ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(rawData);

        if (zdt != null)
        {
            return SmartDateParser.convertToLocalisedDateTime(zdt, Locale.getDefault());
        }

        return rawData;
    }

    /**
     * Evaluates simple rational numbers expressed as {@code numerator/denominator} and returns
     * their decimal representation.
     *
     * <p>
     * If the value is not a valid rational number, the original string is returned unchanged.
     * </p>
     *
     * @param rawData
     *        the raw metadata value
     * @return the evaluated decimal value, or the original value
     */
    private static String evaluateGenericValue(String rawData)
    {
        Matcher matcher = RATIONAL_PATTERN.matcher(rawData);

        if (matcher.matches())
        {
            try
            {
                double num = Double.parseDouble(matcher.group(1));
                double div = Double.parseDouble(matcher.group(2));

                if (div == 0)
                {
                    return rawData;
                }

                double val = num / div;

                if (val == (long) val)
                {
                    return String.format(Locale.ROOT, "%d", (long) val);
                }

                return String.valueOf(Math.round(val * 1000000.0) / 1000000.0);
            }
            catch (NumberFormatException exc)
            {
                return rawData;
            }
        }

        return rawData;
    }
}
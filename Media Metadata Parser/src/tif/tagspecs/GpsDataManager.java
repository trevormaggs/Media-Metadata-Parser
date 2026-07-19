package tif.tagspecs;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZonedDateTime;
import java.util.Locale;
import tif.RationalNumber;
import tif.TagValueTranslator;
import util.SmartDateParser;

/**
 * Utility class that converts GPS metadata into ExifTool-style human-readable representations.
 *
 * <p>
 * This class formats GPS coordinates, altitude, timestamps, bearings, and other GPS-specific
 * metadata into descriptive text suitable for display.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 27 June 2026
 */
public final class GpsDataManager
{
    private GpsDataManager()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Converts raw positional GPS tag attributes into normalised descriptive display text.
     *
     * @param val
     *        the raw GPS tag value
     * @param tag
     *        the GPS tag definition
     * @return a localised formatted descriptive text representation, or an empty string
     */
    public static String getDisplayValue(Object val, TagIFD_GPS tag)
    {
        if (val == null || tag == null)
        {
            return "";
        }

        switch (tag)
        {
            case GPS_VERSION_ID: // 0x0000
                return Taggable.translateVersion(val);

            case GPS_LATITUDE_REF: // 0x0001
                return translateLatitudeRef(val);

            case GPS_LATITUDE: // 0x0002
            case GPS_DEST_LATITUDE: // 0x0014
                return formatCoordinate(val);

            case GPS_LONGITUDE_REF: // 0x0003
                return translateLongitudeRef(val);

            case GPS_LONGITUDE: // 0x0004
            case GPS_DEST_LONGITUDE: // 0x0016
                return formatCoordinate(val);

            case GPS_ALTITUDE_REF: // 0x0005
                return translateAltitudeRef(val);

            case GPS_ALTITUDE: // 0x0006
            case GPS_HPOSITIONING_ERROR: // 0x001F
                return formatAltitude(val);

            case GPS_TIME_STAMP: // 0x0007
                return formatTimeStamp(val);

            case GPS_STATUS: // 0x0009
                return translateStatus(val);

            case GPS_MEASURE_MODE: // 0x000A
                return translateMeasureMode(val);

            case GPS_SPEED_REF: // 0x000C
                return translateSpeedRef(val);

            case GPS_TRACK_REF: // 0x000E
            case GPS_IMG_DIRECTION_REF: // 0x0010
            case GPS_DEST_BEARING_REF: // 0x0017
                return translateDirectionRef(val);

            case GPS_TRACK: // 0x000F
            case GPS_IMG_DIRECTION: // 0x0011
            case GPS_DEST_BEARING: // 0x0018
                return formatBearing(val);

            case GPS_DEST_DISTANCE_REF: // 0x0019
                return translateDestDistanceRef(val);

            case GPS_DATE_STAMP: // 0x001D
                return formatDateStamp(val);

            case GPS_DIFFERENTIAL: // 0x001E
                return translateDifferential(val);

            default:
                return String.valueOf(val).trim();
        }
    }

    /**
     * Translates a GPS latitude reference.
     *
     * @param val
     *        the raw tag value
     * @return the hemisphere name
     */
    private static String translateLatitudeRef(Object val)
    {
        String ref = String.valueOf(val).trim();

        if ("S".equalsIgnoreCase(ref))
        {
            return "South";
        }

        if ("N".equalsIgnoreCase(ref))
        {
            return "North";
        }

        return ref;
    }

    /**
     * Formats a GPS coordinate stored as degrees, minutes, and seconds.
     *
     * @param val
     *        the raw tag value
     * @return the formatted coordinate expressed as degrees, minutes, and seconds, or an empty
     *         string if unavailable
     */
    private static String formatCoordinate(Object val)
    {
        RationalNumber[] arr = TagValueTranslator.toRationalArray(val);

        if (arr != null && arr.length >= 3 && arr[0] != null && arr[1] != null && arr[2] != null)
        {
            // Degrees and minutes are whole units
            double degrees = arr[0].doubleValue();
            double minutes = arr[1].doubleValue();
            double seconds = arr[2].doubleValue();

            return String.format(Locale.ROOT, "%.0f deg %.0f' %.2f\"", degrees, minutes, seconds);
        }

        return "";
    }

    /**
     * Translates a GPS longitude reference.
     *
     * @param val
     *        the raw tag value
     * @return the hemisphere name
     */
    private static String translateLongitudeRef(Object val)
    {
        String ref = String.valueOf(val).trim();

        if ("E".equalsIgnoreCase(ref))
        {
            return "East";
        }

        if ("W".equalsIgnoreCase(ref))
        {
            return "West";
        }

        return ref;
    }

    /**
     * Translates a GPS altitude reference.
     *
     * @param val
     *        the raw tag value
     * @return the altitude reference description
     */
    private static String translateAltitudeRef(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Above Sea Level";

            case 1:
                return "Below Sea Level";

            default:
                return String.valueOf(val).trim();
        }
    }

    /**
     * Formats an altitude value in metres.
     *
     * @param val
     *        the raw tag value
     * @return the formatted altitude in metres
     */
    private static String formatAltitude(Object val)
    {
        if (val instanceof Number)
        {
            double alt = ((Number) val).doubleValue();

            if (alt == (long) alt)
            {
                return String.format(Locale.ROOT, "%d m", (long) alt);
            }

            DecimalFormat df = new DecimalFormat("0.#######", DecimalFormatSymbols.getInstance(Locale.ROOT));

            return df.format(alt) + " m";
        }

        return String.valueOf(val).trim() + " m";
    }

    /**
     * Formats a GPS timestamp into {@code HH:mm:ss}.
     *
     * @param val
     *        the raw tag value
     * @return the formatted time, or an empty string if unavailable
     */
    private static String formatTimeStamp(Object val)
    {
        RationalNumber[] arr = TagValueTranslator.toRationalArray(val);

        if (arr != null && arr.length >= 3 && arr[0] != null && arr[1] != null && arr[2] != null)
        {
            int hours = (int) Math.round(arr[0].doubleValue());
            int minutes = (int) Math.round(arr[1].doubleValue());
            int seconds = (int) Math.round(arr[2].doubleValue());

            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        }

        return "";
    }

    /**
     * Translates a GPS receiver status value.
     *
     * @param val
     *        the raw tag value
     * @return the receiver status description
     */
    private static String translateStatus(Object val)
    {
        String status = String.valueOf(val).trim();

        if ("A".equalsIgnoreCase(status))
        {
            return "Measurement Active";
        }

        if ("V".equalsIgnoreCase(status))
        {
            return "Void";
        }

        return status;
    }

    /**
     * Translates a GPS measurement mode.
     *
     * @param val
     *        the raw tag value
     * @return the measurement mode description
     */
    private static String translateMeasureMode(Object val)
    {
        String mode = String.valueOf(val).trim();

        if ("2".equals(mode))
        {
            return "2-Dimensional Measurement";
        }

        if ("3".equals(mode))
        {
            return "3-Dimensional Measurement";
        }

        return mode;
    }

    /**
     * Translates a GPS speed reference.
     *
     * @param val
     *        the raw tag value
     * @return the speed unit
     */
    private static String translateSpeedRef(Object val)
    {
        String speedRef = String.valueOf(val).trim();

        if ("K".equalsIgnoreCase(speedRef))
        {
            return "km/h";
        }

        if ("M".equalsIgnoreCase(speedRef))
        {
            return "mph";
        }

        if ("N".equalsIgnoreCase(speedRef))
        {
            return "knots";
        }

        return speedRef;
    }

    /**
     * Translates a GPS direction reference.
     *
     * @param val
     *        the raw tag value
     * @return the direction reference description
     */
    private static String translateDirectionRef(Object val)
    {
        String dirRef = String.valueOf(val).trim();

        if ("T".equalsIgnoreCase(dirRef))
        {
            return "True North";
        }

        if ("M".equalsIgnoreCase(dirRef))
        {
            return "Magnetic North";
        }

        return dirRef;
    }

    /**
     * Formats a bearing or direction value.
     *
     * @param val
     *        the raw tag value
     * @return the formatted bearing, or an empty string if no valid bearing is available
     */
    private static String formatBearing(Object val)
    {
        double bearing = Double.NaN;
        RationalNumber[] arr = TagValueTranslator.toRationalArray(val);

        if (arr != null && arr.length > 0 && arr[0] != null)
        {
            bearing = arr[0].doubleValue();
        }

        else if (val instanceof Number)
        {
            bearing = ((Number) val).doubleValue();
        }

        if (Double.isFinite(bearing) && bearing >= 0.0)
        {
            if (bearing == (long) bearing)
            {
                return String.format(Locale.ROOT, "%d", (long) bearing);
            }

            DecimalFormat df = new DecimalFormat("0.#######", DecimalFormatSymbols.getInstance(Locale.ROOT));

            return df.format(bearing);
        }

        String fallback = String.valueOf(val).trim();

        return fallback.startsWith("[") ? "" : fallback;
    }

    /**
     * Translates a GPS destination distance reference.
     *
     * @param val
     *        the raw tag value
     * @return the destination distance unit
     */
    private static String translateDestDistanceRef(Object val)
    {
        String distRef = String.valueOf(val).trim();

        if ("K".equalsIgnoreCase(distRef))
        {
            return "km";
        }

        if ("M".equalsIgnoreCase(distRef))
        {
            return "miles";
        }

        if ("N".equalsIgnoreCase(distRef))
        {
            return "nautical miles";
        }

        return distRef;
    }

    /**
     * Formats a GPS date stamp using the default locale.
     *
     * @param val
     *        the raw tag value
     * @return the formatted localised date, the original value if it cannot be parsed, or an empty
     *         string if unavailable
     */
    private static String formatDateStamp(Object val)
    {
        String rawDate = String.valueOf(val).trim();

        if (!rawDate.isEmpty())
        {
            ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(rawDate);

            if (zdt != null)
            {
                return SmartDateParser.convertToLocalisedDateTime(zdt, Locale.getDefault());
            }

            return rawDate;
        }

        return "";
    }

    /**
     * Translates a GPS differential correction status.
     *
     * @param val
     *        the raw tag value
     * @return the differential correction description
     */
    private static String translateDifferential(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "No Correction";

            case 1:
                return "Differential Corrected";

            default:
                return String.valueOf(val).trim();
        }
    }
}
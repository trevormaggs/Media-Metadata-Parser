package tif;

import java.time.ZonedDateTime;
import java.util.Locale;
import tif.tagspecs.TagIFD_GPS;
import tif.tagspecs.Taggable;
import util.SmartDateParser;

public final class GpsDataManager
{
    private GpsDataManager()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    public static String getDisplayValue(Object val, TagIFD_GPS tag)
    {
        if (val != null && tag != null)
        {
            switch (tag)
            {
                case GPS_VERSION_ID: // 0x0000
                    return Taggable.translateVersion(val);

                case GPS_LATITUDE_REF: // 0x0001
                    return translateLatitudeRef(val);

                case GPS_LATITUDE: // 0x0002
                    return formatCoordinate(val);

                case GPS_LONGITUDE_REF: // 0x0003
                    return translateLongitudeRef(val);

                case GPS_LONGITUDE: // 0x0004
                    return formatCoordinate(val);

                case GPS_ALTITUDE_REF: // 0x0005
                    return translateAltitudeRef(val);

                case GPS_ALTITUDE: // 0x0006
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
                    return translateDirectionRef(val);

                case GPS_TRACK: // 0x000F
                    return formatBearing(val);

                case GPS_IMG_DIRECTION_REF: // 0x0010
                    return translateDirectionRef(val);

                case GPS_IMG_DIRECTION: // 0x0011
                    return formatBearing(val);

                case GPS_DEST_LATITUDE: // 0x0014
                    return formatCoordinate(val);

                case GPS_DEST_LONGITUDE: // 0x0016
                    return formatCoordinate(val);

                case GPS_DEST_BEARING_REF: // 0x0017
                    return translateDirectionRef(val);

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

        return "";
    }

    /**
     * Translates the GPS LatitudeRef string indicator.
     * 
     * @param val
     *        the raw latitude reference tag value
     * @return the full string representation of the hemisphere direction
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

    private static String formatCoordinate(Object val)
    {
        RationalNumber[] arr = TagValueTranslator.toRationalArray(val);

        if (arr != null && arr.length >= 3)
        {
            RationalNumber d = arr[0];
            RationalNumber m = arr[1];
            RationalNumber s = arr[2];

            if (d != null && m != null && s != null)
            {
                // Degrees and minutes are whole units
                double degrees = d.doubleValue();
                double minutes = m.doubleValue();
                double seconds = s.doubleValue();

                return String.format(Locale.ROOT, "%.0f deg %.0f' %.2f\"", degrees, minutes, seconds);
            }
        }

        return "";
    }

    /**
     * Translates the GPS LongitudeRef string indicator.
     * 
     * @param val
     *        the raw longitude reference tag value
     * @return the full string representation of the hemisphere direction
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
     * Translates the GPS AltitudeRef byte parameter.
     * 
     * @param val
     *        the raw altitude reference tag value
     * @return the altitude positioning reference context label
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

    private static String formatAltitude(Object val)
    {
        // This also catches RationalNumber
        if (val instanceof Number)
        {
            double alt = ((Number) val).doubleValue();
            return alt == (long) alt ? String.format(Locale.ROOT, "%d m", (long) alt) : alt + " m";
        }

        return String.valueOf(val).trim() + " m";
    }

    private static String formatTimeStamp(Object val)
    {
        RationalNumber[] arr = TagValueTranslator.toRationalArray(val);

        if (arr != null && arr.length >= 3)
        {
            RationalNumber h = arr[0];
            RationalNumber m = arr[1];
            RationalNumber s = arr[2];

            if (h != null && m != null && s != null)
            {
                int hours = (int) Math.round(h.doubleValue());
                int minutes = (int) Math.round(m.doubleValue());
                int seconds = (int) Math.round(s.doubleValue());

                return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
            }
        }

        return "";
    }

    /**
     * Translates the GPS Status enumeration code string.
     * 
     * @param val
     *        the raw GPS status tag value
     * @return the operational tracking marker
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
     * Translates the dimensional measurement tracking configuration layout mode.
     * 
     * @param val
     *        the raw measurement mode tag value
     * @return the structural calculation layout description
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
     * Translates the receiver operational metrics speed code unit mapping.
     * 
     * @param val
     *        the raw speed reference tag value
     * @return the short unit representation token matches Exiftool standard
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
     * Translates true vs magnetic North references for directions and bearing coordinates.
     * 
     * @param val
     *        the raw direction reference tag value
     * @return the unadorned angular frame label
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

    private static String formatBearing(Object val)
    {
        String str = String.valueOf(val).trim();
        RationalNumber[] arr = TagValueTranslator.toRationalArray(val);

        if (arr != null && arr.length > 0 && arr[0] != null)
        {
            double bearing = arr[0].doubleValue();

            if (Double.isFinite(bearing) && bearing >= 0.0)
            {
                if (bearing == (long) bearing)
                {
                    str = String.format(Locale.ROOT, "%d", (long) bearing);
                }

                else
                {
                    str = String.format(Locale.ROOT, "%.4f", bearing).replaceAll("0+$", "").replaceAll("\\.$", "");
                }
            }
        }

        else if (val instanceof Number)
        {
            double bearing = ((Number) val).doubleValue();

            if (Double.isFinite(bearing) && bearing >= 0.0)
            {
                if (bearing == (long) bearing)
                {
                    str = String.format(Locale.ROOT, "%d", (long) bearing);
                }

                else
                {
                    str = String.format(Locale.ROOT, "%.4f", bearing).replaceAll("0+$", "").replaceAll("\\.$", "");
                }
            }
        }

        else
        {
            str = str.startsWith("[") ? "" : str;
        }

        return str;
    }

    /**
     * Translates operational metric destination distance units components shorthand mappings.
     * 
     * @param val
     *        the raw destination distance reference tag value
     * @return the localised string description label configuration
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

    private static String formatDateStamp(Object val)
    {
        if (val != null && String.valueOf(val).trim().length() > 0)
        {
            String rawDate = String.valueOf(val).trim();

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
     * Translates GPS Differential correction processing status bits.
     * 
     * @param val
     *        the raw differential correction tag value
     * @return the correction translation text
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
package tif;

import java.util.Locale;
import tif.DirectoryIFD.EntryIFD;
import tif.tagspecs.TagIFD_GPS;
import tif.tagspecs.Taggable;

public final class GpsDataManager2
{
    private GpsDataManager2()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    public static String getDisplayValue(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir != null && tag != null)
        {
            switch (tag)
            {
                case GPS_LATITUDE_REF: // 0x0001
                    return translateLatitudeRef(gpsDir, tag);

                case GPS_LATITUDE: // 0x0002
                    return formatCoordinate(gpsDir, tag);

                case GPS_LONGITUDE_REF: // 0x0003
                    return translateLongitudeRef(gpsDir, tag);

                case GPS_LONGITUDE: // 0x0004
                    return formatCoordinate(gpsDir, tag);

                case GPS_ALTITUDE_REF: // 0x0005
                    return translateAltitudeRef(gpsDir, tag);

                case GPS_ALTITUDE: // 0x0006
                    return formatAltitude(gpsDir, tag);

                case GPS_TIME_STAMP: // 0x0007
                    return formatTimeStamp(gpsDir, tag);

                case GPS_STATUS: // 0x0009
                    return translateStatus(gpsDir, tag);

                case GPS_MEASURE_MODE: // 0x000A
                    return translateMeasureMode(gpsDir, tag);

                case GPS_SPEED_REF: // 0x000C
                    return translateSpeedRef(gpsDir, tag);

                case GPS_TRACK_REF: // 0x000E
                    return translateDirectionRef(gpsDir, tag);

                case GPS_TRACK: // 0x000F
                    return formatBearing(gpsDir, tag);

                case GPS_IMG_DIRECTION_REF: // 0x0010
                    return translateDirectionRef(gpsDir, tag);

                case GPS_IMG_DIRECTION: // 0x0011
                    return formatBearing(gpsDir, tag);

                case GPS_DEST_LATITUDE: // 0x0014
                    return formatCoordinate(gpsDir, tag);

                case GPS_DEST_LONGITUDE: // 0x0016
                    return formatCoordinate(gpsDir, tag);

                case GPS_DEST_BEARING_REF: // 0x0017
                    return translateDirectionRef(gpsDir, tag);

                case GPS_DEST_BEARING: // 0x0018
                    return formatBearing(gpsDir, tag);

                case GPS_DEST_DISTANCE_REF: // 0x0019
                    return translateDestDistanceRef(gpsDir, tag);

                case GPS_DATE_STAMP: // 0x001D
                    return formatDateStamp(gpsDir, tag);

                case GPS_DIFFERENTIAL: // 0x001E
                    return translateDifferential(gpsDir, tag);

                default:
                    if (gpsDir.hasTag(tag))
                    {
                        EntryIFD entry = gpsDir.getTagEntry(tag);
                        Object rawValue = (entry != null) ? entry.getData() : null;
                        return rawValue != null ? String.valueOf(rawValue).trim() : "";
                    }
                break;
            }
        }

        return "";
    }

    /**
     * Translates the GPS LatitudeRef string indicator.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS latitude reference tag definition
     * @return the full string representation of the hemisphere direction
     */
    private static String translateLatitudeRef(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                String ref = String.valueOf(entry.getData()).trim();

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
        }

        return "";
    }

    private static String formatCoordinate(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        EntryIFD entry = gpsDir.getTagEntry(tag);

        if (entry != null && gpsDir.hasTag(tag))
        {
            RationalNumber[] rationals = TagValueTranslator.getRationalArray(entry);

            if (rationals != null && rationals.length == 3 && rationals[0] != null && rationals[1] != null && rationals[2] != null)
            {
                double degrees = rationals[0].doubleValue();
                double minutes = rationals[1].doubleValue();
                double seconds = rationals[2].doubleValue();

                return String.format(Locale.ROOT, "%.0f deg %.0f' %.2f\"", degrees, minutes, seconds);
            }
        }

        return "";
    }

    /**
     * Translates the GPS LongitudeRef string indicator.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS longitude reference tag definition
     * @return the full string representation of the hemisphere direction
     */
    private static String translateLongitudeRef(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                String ref = String.valueOf(entry.getData()).trim();

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
        }

        return "";
    }

    /**
     * Translates the GPS AltitudeRef byte parameter.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS altitude reference tag definition
     * @return the altitude positioning reference context label
     */
    private static String translateAltitudeRef(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                switch (Taggable.convertToInt(entry.getData()))
                {
                    case 0:
                        return "Above Sea Level";

                    case 1:
                        return "Below Sea Level";

                    default:
                        return String.valueOf(entry.getData()).trim();
                }
            }
        }

        return "";
    }

    private static String formatAltitude(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            double alt = gpsDir.getDoubleValue(tag);
            return alt == (long) alt ? String.format(Locale.ROOT, "%d m", (long) alt) : String.valueOf(alt) + " m";
        }

        return "";
    }

    private static String formatTimeStamp(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD timeEntry = gpsDir.getTagEntry(tag);
            RationalNumber[] timeArray = (timeEntry != null) ? TagValueTranslator.getRationalArray(timeEntry) : null;

            if (timeArray != null && timeArray.length >= 3 && timeArray[0] != null && timeArray[1] != null && timeArray[2] != null)
            {
                try
                {
                    int hours = (int) Math.round(timeArray[0].doubleValue());
                    int minutes = (int) Math.round(timeArray[1].doubleValue());
                    int seconds = (int) Math.round(timeArray[2].doubleValue());

                    return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
                }
                catch (Exception exc)
                {
                    // Do nothing - fallback
                }
            }
        }

        return "";
    }

    /**
     * Translates the GPS Status enumeration code string.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS status tag definition
     * @return the operational tracking marker
     */
    private static String translateStatus(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                String status = String.valueOf(entry.getData()).trim();

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
        }

        return "";
    }

    /**
     * Translates the dimensional measurement tracking configuration layout mode.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS measurement mode tag definition
     * @return the structural calculation layout description
     */
    private static String translateMeasureMode(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                String mode = String.valueOf(entry.getData()).trim();

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
        }

        return "";
    }

    /**
     * Translates the receiver operational metrics speed code unit mapping.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS speed reference tag definition
     * @return the short unit representation token matches Exiftool standard
     */
    private static String translateSpeedRef(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                String speedRef = String.valueOf(entry.getData()).trim();

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
        }

        return "";
    }

    /**
     * Translates true vs magnetic North references for directions and bearing coordinates.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS direction reference tag definition
     * @return the unadorned angular frame label
     */
    private static String translateDirectionRef(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                String dirRef = String.valueOf(entry.getData()).trim();

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
        }

        return "";
    }

    private static String formatBearing(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        EntryIFD entry = gpsDir.getTagEntry(tag);

        if (entry != null && gpsDir.hasTag(tag))
        {
            RationalNumber[] bearingArray = TagValueTranslator.getRationalArray(entry);

            if (bearingArray != null && bearingArray.length > 0 && bearingArray[0] != null)
            {
                return String.valueOf(bearingArray[0].doubleValue());
            }
        }

        return "";
    }

    /**
     * Translates operational metric destination distance units components shorthand mappings.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS destination distance reference tag definition
     * @return the localised string description label configuration
     */
    private static String translateDestDistanceRef(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                String distRef = String.valueOf(entry.getData()).trim();

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
        }

        return "";
    }

    private static String formatDateStamp(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        String rawDate = gpsDir.getString(tag);

        if (rawDate != null && gpsDir.hasTag(tag))
        {
            String cleanDate = rawDate.trim().replace('-', ':').replace('/', ':').split(" ")[0];
            String[] parts = cleanDate.split(":");

            if (parts.length == 3 && parts[0].length() == 4)
            {
                return cleanDate;
            }

            if (parts.length == 3 && parts[2].length() == 4)
            {
                return parts[2] + ":" + parts[1] + ":" + parts[0];
            }

            return cleanDate;
        }

        return "";
    }

    /**
     * Translates GPS Differential correction processing status bits.
     * 
     * @param gpsDir
     *        the GPS directory containing the metadata tag
     * @param tag
     *        the specific GPS differential correction tag definition
     * @return the correction translation text
     */
    private static String translateDifferential(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir.hasTag(tag))
        {
            EntryIFD entry = gpsDir.getTagEntry(tag);

            if (entry != null && entry.getData() != null)
            {
                switch (Taggable.convertToInt(entry.getData()))
                {
                    case 0:
                        return "No Correction";

                    case 1:
                        return "Differential Corrected";

                    default:
                        return String.valueOf(entry.getData()).trim();
                }
            }
        }

        return "";
    }
}
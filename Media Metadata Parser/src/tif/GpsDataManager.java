package tif;

import java.util.Locale;
import tif.DirectoryIFD.EntryIFD;
import tif.tagspecs.TagIFD_GPS;
import tif.tagspecs.Taggable;

public final class GpsDataManager
{
    private GpsDataManager()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    public static String getDisplayValue(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (gpsDir == null || tag == null)
        {
            return "";
        }

        switch (tag)
        {
            case GPS_LATITUDE:
            case GPS_DEST_LATITUDE:
            case GPS_LONGITUDE:
            case GPS_DEST_LONGITUDE:
                return formatCoordinate(gpsDir, tag);

            case GPS_ALTITUDE:
                return formatAltitude(gpsDir, tag);

            case GPS_TRACK:
            case GPS_IMG_DIRECTION:
            case GPS_DEST_BEARING:
                return formatBearing(gpsDir, tag);

            case GPS_TIME_STAMP:
                return formatTimeStamp(gpsDir, tag);

            case GPS_STATUS:
            case GPS_MEASURE_MODE:
            case GPS_SPEED_REF:
            case GPS_DEST_DISTANCE_REF:
            case GPS_DIFFERENTIAL:
                return translateGpsMetadata(gpsDir, tag);

            default:
                return formatFallback(gpsDir, tag);
        }
    }

    private static String formatAltitude(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (!gpsDir.hasTag(tag))
        {
            return "";
        }

        // Defensive check: Default to 0 (Above Sea Level) if the reference tag is omitted
        int altRef = gpsDir.hasTag(TagIFD_GPS.GPS_ALTITUDE_REF) ? gpsDir.getIntValue(TagIFD_GPS.GPS_ALTITUDE_REF) : 0;
        double alt = gpsDir.getDoubleValue(tag);
        String reference = (altRef == 1) ? "Below" : "Above";

        return String.format(Locale.ROOT, "%.2f meters (%s Sea Level)", alt, reference);
    }

    private static String formatTimeStamp(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (!gpsDir.hasTag(tag))
        {
            return "";
        }

        EntryIFD timeEntry = gpsDir.getTagEntry(tag);
        RationalNumber[] timeArray = (timeEntry != null) ? TagValueFormatter.getRationalArray(timeEntry) : null;

        if (timeArray != null && timeArray.length >= 3 && timeArray[0] != null && timeArray[1] != null && timeArray[2] != null)
        {
            try
            {
                // Protected against direct truncation floating-point precision loss bugs
                int hours = (int) Math.round(timeArray[0].doubleValue());
                int minutes = (int) Math.round(timeArray[1].doubleValue());
                double seconds = timeArray[2].doubleValue();

                return String.format(Locale.ROOT, "%02d:%02d:%05.2fZ", hours, minutes, seconds);
            }

            catch (Exception e)
            {
                // Defensive fallback to block stack trace leaks
            }
        }

        return "";
    }

    private static String translateGpsMetadata(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (!gpsDir.hasTag(tag))
        {
            return "";

        }
        String val = gpsDir.getString(tag);

        if (val == null)
        {
            return "";
        }

        val = val.trim();

        switch (tag)
        {
            case GPS_STATUS:
                if ("A".equalsIgnoreCase(val)) return "Measurement Active";
                if ("V".equalsIgnoreCase(val)) return "Measurement Void";

                return val;

            case GPS_MEASURE_MODE:
                if ("2".equals(val)) return "2-Dimensional Measurement";
                if ("3".equals(val)) return "3-Dimensional Measurement";

                return val;

            case GPS_SPEED_REF:
                if ("K".equalsIgnoreCase(val)) return "Kilometers per hour (km/h)";
                if ("M".equalsIgnoreCase(val)) return "Miles per hour (mph)";
                if ("N".equalsIgnoreCase(val)) return "Knots";

                return val;

            case GPS_DEST_DISTANCE_REF:
                if ("K".equalsIgnoreCase(val)) return "Kilometers";
                if ("M".equalsIgnoreCase(val)) return "Miles";
                if ("N".equalsIgnoreCase(val)) return "Nautical Miles";

                return val;

            case GPS_DIFFERENTIAL:
                try
                {
                    int diff = Integer.parseInt(val);
                    if (diff == 1) return "Differential Corrected";
                    if (diff == 0) return "No Correction";
                }

                catch (NumberFormatException e)
                {
                    // Fall through to raw value presentation safely
                }
                return val;

            default:
                return val;
        }
    }

    private static String formatFallback(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (!gpsDir.hasTag(tag))
        {
            return "";
        }

        if (tag.getHint() == TagHint.HINT_RATIONAL)
        {
            EntryIFD fallbackEntry = gpsDir.getTagEntry(tag);
            RationalNumber[] ratArray = (fallbackEntry != null) ? TagValueFormatter.getRationalArray(fallbackEntry) : null;

            if (ratArray != null && ratArray.length > 0 && ratArray[0] != null)
            {
                return ratArray[0].toSimpleString(true);
            }
        }

        String genericValue = gpsDir.getString(tag);

        return (genericValue != null) ? genericValue.trim() : "";
    }

    public static String formatCoordinate(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (!gpsDir.hasTag(tag))
        {
            return "";
        }

        EntryIFD entry = gpsDir.getTagEntry(tag);

        if (entry == null)
        {
            return "";
        }

        RationalNumber[] rationals = TagValueFormatter.getRationalArray(entry);

        return decodeGpsArray(rationals, tag, gpsDir);
    }

    private static String formatBearing(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        if (!gpsDir.hasTag(tag))
        {
            return "";
        }

        Taggable dirRefTag = lookupDirectionRefTag(tag);
        String dirRef = (dirRefTag != null && gpsDir.hasTag(dirRefTag)) ? gpsDir.getString(dirRefTag) : "";

        EntryIFD tagEntry = gpsDir.getTagEntry(tag);

        if (tagEntry == null)
        {
            return "";
        }

        RationalNumber[] bearingArray = TagValueFormatter.getRationalArray(tagEntry);

        if (bearingArray == null || bearingArray.length == 0 || bearingArray[0] == null)
        {
            return "";
        }

        // Keep decimal readability clean for angles/bearings
        double bearingVal = bearingArray[0].doubleValue();
        String refLabel = (dirRef == null || dirRef.trim().isEmpty()) ? "Unknown Ref"
                : ("M".equalsIgnoreCase(dirRef.trim()) ? "Magnetic North" : "True North");

        return String.format(Locale.ROOT, "%.4f° %s", bearingVal, refLabel);
    }

    private static String decodeGpsArray(RationalNumber[] rationals, Taggable tag, DirectoryIFD dir)
    {
        if (rationals == null || rationals.length < 3 || rationals[0] == null || rationals[1] == null || rationals[2] == null)
        {
            return "";
        }

        // Direct mathematical evaluation protects against raw ratio leaks (like 2981/100)
        double degrees = rationals[0].doubleValue();
        double minutes = rationals[1].doubleValue();
        double seconds = rationals[2].doubleValue();

        Taggable refTag = lookupRefTag(tag);
        String ref = "";

        if (dir != null && refTag != null && dir.hasTag(refTag))
        {
            String rawRef = dir.getString(refTag);
            if (rawRef != null)
            {
                ref = rawRef.trim();
            }
        }

        // Standard ExifTool format matching output
        String formattedDms = String.format(Locale.ROOT, "%.0f deg %.0f' %.2f\"", degrees, minutes, seconds);

        return ref.isEmpty() ? formattedDms : formattedDms + " " + ref;
    }

    private static Taggable lookupDirectionRefTag(Taggable tag)
    {
        if (tag == TagIFD_GPS.GPS_TRACK) return TagIFD_GPS.GPS_TRACK_REF;
        if (tag == TagIFD_GPS.GPS_IMG_DIRECTION) return TagIFD_GPS.GPS_IMG_DIRECTION_REF;
        if (tag == TagIFD_GPS.GPS_DEST_BEARING) return TagIFD_GPS.GPS_DEST_BEARING_REF;

        return null;
    }

    private static Taggable lookupRefTag(Taggable tag)
    {
        if (tag == TagIFD_GPS.GPS_LATITUDE) return TagIFD_GPS.GPS_LATITUDE_REF;
        if (tag == TagIFD_GPS.GPS_LONGITUDE) return TagIFD_GPS.GPS_LONGITUDE_REF;
        if (tag == TagIFD_GPS.GPS_DEST_LATITUDE) return TagIFD_GPS.GPS_DEST_LATITUDE_REF;
        if (tag == TagIFD_GPS.GPS_DEST_LONGITUDE) return TagIFD_GPS.GPS_DEST_LONGITUDE_REF;

        return null;
    }
}
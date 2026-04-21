package tif;

import tif.DirectoryIFD.EntryIFD;
import tif.tagspecs.TagIFD_GPS;
import tif.tagspecs.Taggable;

public final class GpsDataManager
{
    /**
     * Default constructor will always throw an exception.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    private GpsDataManager()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    public static String getDisplayValue(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        switch (tag)
        {
            // Coordinate Group
            case GPS_LATITUDE:
            case GPS_DEST_LATITUDE:
            case GPS_LONGITUDE:
            case GPS_DEST_LONGITUDE:
                // Matches the new signature: (DirectoryIFD, TagIFD_GPS)
                return formatCoordinate(gpsDir, tag);

            // Altitude Group
            case GPS_ALTITUDE:
                String altRef = gpsDir.getString(TagIFD_GPS.GPS_ALTITUDE_REF);
                double alt = gpsDir.getDoubleValue(tag);
                return String.format("%.2f meters (%s sea level)", alt, "1".equals(altRef) ? "Below" : "Above");

            // Directional Group (Bearing/Track)
            case GPS_TRACK:
            case GPS_IMG_DIRECTION:
            case GPS_DEST_BEARING:
                // 1. Get the correct reference tag (T or M) safely
                Taggable dirRefTag = lookupDirectionRefTag(tag);
                String dirRef = "";

                // Check if the directory actually contains the reference tag before calling
                // getString
                if (gpsDir != null && dirRefTag != null && gpsDir.hasTag(dirRefTag))
                {
                    dirRef = gpsDir.getString(dirRefTag);
                }

                // 2. Extract the bearing value
                RationalNumber[] bearingArray = TagValueConverter.getRationalArray(gpsDir.getTagEntry(tag));
                String bearingVal = (bearingArray.length > 0) ? bearingArray[0].toSimpleString(true) : "0";

                // 3. Format the output
                return bearingVal + "° " + ("M".equals(dirRef) ? "Magnetic" : "True");

            default:
                // Fall back to general conversion for simple tags (Satellites, Version, etc.)
                return TagValueConverter.toStringValue(gpsDir.getTagEntry(tag));
        }
    }

    /**
     * Formats GPS-specific rational arrays into human-readable DMS strings.
     * 
     * @param rationals
     *        the coordinate components (degrees, minutes, seconds)
     * @param tag
     *        the specific GPS tag (Latitude or Longitude)
     * @param parentDir
     *        the directory context for reference lookups (e.g., 'N', 'S', 'E', 'W')
     * @return a formatted coordinate string
     */
    public static String formatCoordinate(DirectoryIFD gpsDir, TagIFD_GPS tag)
    {
        EntryIFD entry = gpsDir.getTagEntry(tag);

        if (entry == null) return "";

        // Use the array getter instead of the single value getter
        RationalNumber[] rationals = TagValueConverter.getRationalArray(entry);

        return decodeGpsArray(rationals, tag, gpsDir);
    }

    private static String decodeGpsArray(RationalNumber[] rationals, Taggable tag, DirectoryIFD dir)
    {
        if (rationals == null || rationals.length < 3)
        {
            return "";
        }

        String d = rationals[0].toSimpleString(true);
        String m = rationals[1].toSimpleString(true);
        String s = rationals[2].toSimpleString(true);

        // Map Value Tag to its Ref Tag
        Taggable refTag = lookupRefTag(tag);

        String ref = "";

        if (dir != null && refTag != null && dir.hasTag(refTag))
        {
            ref = dir.getString(refTag);
        }

        return String.format("%s° %s' %s\" %s", d, m, s, ref).trim();
    }

    private static Taggable lookupDirectionRefTag(Taggable tag)
    {
        if (tag == TagIFD_GPS.GPS_TRACK)
        {
            return TagIFD_GPS.GPS_TRACK_REF;
        }

        if (tag == TagIFD_GPS.GPS_IMG_DIRECTION)
        {
            return TagIFD_GPS.GPS_IMG_DIRECTION_REF;
        }

        if (tag == TagIFD_GPS.GPS_DEST_BEARING)
        {
            return TagIFD_GPS.GPS_DEST_BEARING_REF;
        }

        return null;
    }

    private static Taggable lookupRefTag(Taggable tag)
    {
        if (tag == TagIFD_GPS.GPS_LATITUDE)
        {
            return TagIFD_GPS.GPS_LATITUDE_REF;
        }

        if (tag == TagIFD_GPS.GPS_LONGITUDE)
        {
            return TagIFD_GPS.GPS_LONGITUDE_REF;
        }

        if (tag == TagIFD_GPS.GPS_DEST_LATITUDE)
        {
            return TagIFD_GPS.GPS_DEST_LATITUDE_REF;
        }

        if (tag == TagIFD_GPS.GPS_DEST_LONGITUDE)
        {
            return TagIFD_GPS.GPS_DEST_LONGITUDE_REF;
        }

        return null;
    }
}
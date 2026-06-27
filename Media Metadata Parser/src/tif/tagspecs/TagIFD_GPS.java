package tif.tagspecs;

import static tif.DirectoryIdentifier.*;
import tif.DirectoryIdentifier;
import tif.GpsDataManager;
import tif.TagHint;

/**
 * Global Positioning System (GPS) sub-directory metadata tags within the
 * {@link DirectoryIdentifier#IFD_GPS_SUBIFD_DIRECTORY} scope.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 27 June 2026
 */
public enum TagIFD_GPS implements Taggable
{
    GPS_VERSION_ID(0x0000, "GPS Tag Version", TagHint.HINT_BYTE),
    GPS_LATITUDE_REF(0x0001, "North or South Latitude", TagHint.HINT_STRING),
    GPS_LATITUDE(0x0002, "Latitude (Degrees, Minutes, Seconds)", TagHint.HINT_RATIONAL),
    GPS_LONGITUDE_REF(0x0003, "East or West Longitude", TagHint.HINT_STRING),
    GPS_LONGITUDE(0x0004, "Longitude (Degrees, Minutes, Seconds)", TagHint.HINT_RATIONAL),
    GPS_ALTITUDE_REF(0x0005, "Altitude Reference (Sea Level)", TagHint.HINT_BYTE),
    GPS_ALTITUDE(0x0006, "Altitude (Metres)", TagHint.HINT_RATIONAL),
    GPS_TIME_STAMP(0x0007, "GPS Time (Atomic Clock)", TagHint.HINT_RATIONAL),
    GPS_SATELLITES(0x0008, "GPS Satellites Used for Measurement", TagHint.HINT_STRING),
    GPS_STATUS(0x0009, "GPS Receiver Status", TagHint.HINT_STRING),
    GPS_MEASURE_MODE(0x000A, "GPS Measurement Mode", TagHint.HINT_STRING),
    GPS_DOP(0x000B, "Measurement Precision (DOP)", TagHint.HINT_RATIONAL),
    GPS_SPEED_REF(0x000C, "Speed Unit (K, M, N)", TagHint.HINT_STRING),
    GPS_SPEED(0x000D, "Speed of GPS Receiver", TagHint.HINT_RATIONAL),
    GPS_TRACK_REF(0x000E, "Reference for Direction of Movement", TagHint.HINT_STRING),
    GPS_TRACK(0x000F, "Direction of Movement", TagHint.HINT_RATIONAL),
    GPS_IMG_DIRECTION_REF(0x0010, "Reference for Direction of Image", TagHint.HINT_STRING),
    GPS_IMG_DIRECTION(0x0011, "Direction of Image", TagHint.HINT_RATIONAL),
    GPS_MAP_DATUM(0x0012, "Geodetic Survey Data Used", TagHint.HINT_STRING),
    GPS_DEST_LATITUDE_REF(0x0013, "Reference for Latitude of Destination", TagHint.HINT_STRING),
    GPS_DEST_LATITUDE(0x0014, "Latitude of Destination", TagHint.HINT_RATIONAL),
    GPS_DEST_LONGITUDE_REF(0x0015, "Reference for Longitude of Destination", TagHint.HINT_STRING),
    GPS_DEST_LONGITUDE(0x0016, "Longitude of Destination", TagHint.HINT_RATIONAL),
    GPS_DEST_BEARING_REF(0x0017, "Reference for Bearing of Destination", TagHint.HINT_STRING),
    GPS_DEST_BEARING(0x0018, "Bearing of Destination", TagHint.HINT_RATIONAL),
    GPS_DEST_DISTANCE_REF(0x0019, "Reference for Distance to Destination", TagHint.HINT_STRING),
    GPS_DEST_DISTANCE(0x001A, "Distance to Destination", TagHint.HINT_RATIONAL),
    GPS_PROCESSING_METHOD(0x001B, "Name of GPS Processing Method", TagHint.HINT_ENCODED_STRING),
    GPS_AREA_INFORMATION(0x001C, "Name of GPS Area", TagHint.HINT_ENCODED_STRING),
    GPS_DATE_STAMP(0x001D, "GPS Date Stamp", TagHint.HINT_DATE),
    GPS_DIFFERENTIAL(0x001E, "GPS Differential Correction", TagHint.HINT_SHORT),
    GPS_HPOSITIONING_ERROR(0x001F, "Horizontal Positioning Error (Metres)", TagHint.HINT_RATIONAL);

    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagIFD_GPS(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    private TagIFD_GPS(int id, String desc, TagHint clue)
    {
        this.numID = id;
        this.desc = desc;
        this.hint = clue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberID()
    {
        return numID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return IFD_GPS_SUBIFD_DIRECTORY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TagHint getHint()
    {
        return hint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription()
    {
        return desc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String translate(Object val)
    {
        return GpsDataManager.getDisplayValue(val, this);
    }
}
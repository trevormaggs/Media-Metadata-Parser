package tif.tagspecs;

import static tif.DirectoryIdentifier.*;
import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Global Positioning System (GPS) sub-directory metadata tags within the
 * {@link DirectoryIdentifier#IFD_GPS_SUBIFD_DIRECTORY} scope.
 * 
 * @author Trevor Maggs
 * @version 1.2
 */
public enum TagIFD_GPS implements Taggable
{
    GPS_VERSION_ID(0X0000, "GPS Tag Version", TagHint.HINT_BYTE),
    GPS_LATITUDE_REF(0X0001, "North or South Latitude", TagHint.HINT_STRING),
    GPS_LATITUDE(0X0002, "Latitude (Degrees, Minutes, Seconds)", TagHint.HINT_RATIONAL),
    GPS_LONGITUDE_REF(0X0003, "East or West Longitude", TagHint.HINT_STRING),
    GPS_LONGITUDE(0X0004, "Longitude (Degrees, Minutes, Seconds)", TagHint.HINT_RATIONAL),
    GPS_ALTITUDE_REF(0X0005, "Altitude Reference (Sea Level)", TagHint.HINT_BYTE),
    GPS_ALTITUDE(0X0006, "Altitude (Metres)", TagHint.HINT_RATIONAL),
    GPS_TIME_STAMP(0X0007, "GPS Time (Atomic Clock)", TagHint.HINT_RATIONAL),
    GPS_SATELLITES(0X0008, "GPS Satellites Used for Measurement", TagHint.HINT_STRING),
    GPS_STATUS(0X0009, "GPS Receiver Status", TagHint.HINT_STRING),
    GPS_MEASURE_MODE(0X000A, "GPS Measurement Mode", TagHint.HINT_STRING),
    GPS_DOP(0X000B, "Measurement Precision (DOP)", TagHint.HINT_RATIONAL),
    GPS_SPEED_REF(0X000C, "Speed Unit (K, M, N)", TagHint.HINT_STRING),
    GPS_SPEED(0X000D, "Speed of GPS Receiver", TagHint.HINT_RATIONAL),
    GPS_TRACK_REF(0X000E, "Reference for Direction of Movement", TagHint.HINT_STRING),
    GPS_TRACK(0X000F, "Direction of Movement", TagHint.HINT_RATIONAL),
    GPS_IMG_DIRECTION_REF(0X0010, "Reference for Direction of Image", TagHint.HINT_STRING),
    GPS_IMG_DIRECTION(0X0011, "Direction of Image", TagHint.HINT_RATIONAL),
    GPS_MAP_DATUM(0X0012, "Geodetic Survey Data Used", TagHint.HINT_STRING),
    GPS_DEST_LATITUDE_REF(0X0013, "Reference for Latitude of Destination", TagHint.HINT_STRING),
    GPS_DEST_LATITUDE(0X0014, "Latitude of Destination", TagHint.HINT_RATIONAL),
    GPS_DEST_LONGITUDE_REF(0X0015, "Reference for Longitude of Destination", TagHint.HINT_STRING),
    GPS_DEST_LONGITUDE(0X0016, "Longitude of Destination", TagHint.HINT_RATIONAL),
    GPS_DEST_BEARING_REF(0X0017, "Reference for Bearing of Destination", TagHint.HINT_STRING),
    GPS_DEST_BEARING(0X0018, "Bearing of Destination", TagHint.HINT_RATIONAL),
    GPS_DEST_DISTANCE_REF(0X0019, "Reference for Distance to Destination", TagHint.HINT_STRING),
    GPS_DEST_DISTANCE(0X001A, "Distance to Destination", TagHint.HINT_RATIONAL),
    GPS_PROCESSING_METHOD(0X001B, "Name of GPS Processing Method", TagHint.HINT_ENCODED_STRING),
    GPS_AREA_INFORMATION(0X001C, "Name of GPS Area", TagHint.HINT_ENCODED_STRING),
    GPS_DATE_STAMP(0X001D, "GPS Date Stamp", TagHint.HINT_DATE),
    GPS_DIFFERENTIAL(0X001E, "GPS Differential Correction", TagHint.HINT_SHORT),
    GPS_HPOSITIONING_ERROR(0X001F, "Horizontal Positioning Error (Metres)", TagHint.HINT_RATIONAL);

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

    @Override
    public int getNumberID()
    {
        return numID;
    }

    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return IFD_GPS_SUBIFD_DIRECTORY;
    }

    @Override
    public TagHint getHint()
    {
        return hint;
    }

    @Override
    public String getDescription()
    {
        return desc;
    }
}
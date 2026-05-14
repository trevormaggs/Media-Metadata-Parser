package tif.tagspecs;

import static tif.DirectoryIdentifier.*;
import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagIFD_GPS implements Taggable
{
    GPS_VERSION_ID(0X0000,  "GPS Tag Version"),
    GPS_LATITUDE_REF(0X0001,  "North or South Latitude"),
    GPS_LATITUDE(0X0002,  "Latitude (Degrees, Minutes, Seconds)"),
    GPS_LONGITUDE_REF(0X0003,  "East or West Longitude"),
    GPS_LONGITUDE(0X0004,  "Longitude (Degrees, Minutes, Seconds)"),
    GPS_ALTITUDE_REF(0X0005,  "Altitude Reference (Sea Level)"),
    GPS_ALTITUDE(0X0006,  "Altitude (Meters)"),
    GPS_TIME_STAMP(0X0007,  "GPS Time (Atomic Clock)", TagHint.HINT_DATE),
    GPS_SATELLITES(0X0008,  "GPS Satellites Used for Measurement"),
    GPS_STATUS(0X0009,  "GPS Receiver Status"),
    GPS_MEASURE_MODE(0X000A,  "GPS Measurement Mode"),
    GPS_DOP(0X000B,  "Measurement Precision (DOP)"),
    GPS_SPEED_REF(0X000C,  "Speed Unit (K, M, N)"),
    GPS_SPEED(0X000D,  "Speed of GPS Receiver"),
    GPS_TRACK_REF(0X000E,  "Reference for Direction of Movement"),
    GPS_TRACK(0X000F,  "Direction of Movement"),
    GPS_IMG_DIRECTION_REF(0X0010,  "Reference for Direction of Image"),
    GPS_IMG_DIRECTION(0X0011,  "Direction of Image"),
    GPS_MAP_DATUM(0X0012,  "Geodetic Survey Data Used"),
    GPS_DEST_LATITUDE_REF(0X0013,  "Reference for Latitude of Destination"),
    GPS_DEST_LATITUDE(0X0014,  "Latitude of Destination"),
    GPS_DEST_LONGITUDE_REF(0X0015,  "Reference for Longitude of Destination"),
    GPS_DEST_LONGITUDE(0X0016,  "Longitude of Destination"),
    GPS_DEST_BEARING_REF(0X0017,  "Reference for Bearing of Destination"),
    GPS_DEST_BEARING(0X0018,  "Bearing of Destination"),
    GPS_DEST_DISTANCE_REF(0X0019,  "Reference for Distance to Destination"),
    GPS_DEST_DISTANCE(0X001A,  "Distance to Destination"),
    GPS_PROCESSING_METHOD(0X001B,  "Name of GPS Processing Method", TagHint.HINT_ENCODED_STRING),
    GPS_AREA_INFORMATION(0X001C,  "Name of GPS Area"),
    GPS_DATE_STAMP(0X001D,  "GPS Date Stamp", TagHint.HINT_DATE),
    GPS_DIFFERENTIAL(0X001E,  "GPS Differential Correction"),
    GPS_HPOSITIONING_ERROR(0X001F,  "Horizontal Positioning Error (Metres)");

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
        return IFD_GPS_DIRECTORY;
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
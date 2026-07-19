package tif.tagspecs;

import static tif.DirectoryIdentifier.*;
import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Global Positioning System (GPS) sub-directory metadata tags within the
 * {@link DirectoryIdentifier#IFD_GPS_SUBIFD_DIRECTORY} scope.
 * 
 * <p>
 * This enumeration handles the structural decoding tags defined by the EXIF/TIFF specification for
 * geospatial telemetry, including coordinates, altitude reference benchmarks, velocity indicators,
 * and directional vectors.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 27 June 2026
 */
public enum TagIFD_GPS implements Taggable
{
    GPS_VERSION_ID(0x0000, "GPS Version ID", TagHint.HINT_BYTE),
    GPS_LATITUDE_REF(0x0001, "GPS Latitude Ref", TagHint.HINT_STRING),
    GPS_LATITUDE(0x0002, "GPS Latitude", TagHint.HINT_RATIONAL),
    GPS_LONGITUDE_REF(0x0003, "GPS Longitude Ref", TagHint.HINT_STRING),
    GPS_LONGITUDE(0x0004, "GPS Longitude", TagHint.HINT_RATIONAL),
    GPS_ALTITUDE_REF(0x0005, "GPS Altitude Ref", TagHint.HINT_BYTE),
    GPS_ALTITUDE(0x0006, "GPS Altitude", TagHint.HINT_RATIONAL),
    GPS_TIME_STAMP(0x0007, "GPS Time Stamp", TagHint.HINT_RATIONAL),
    GPS_SATELLITES(0x0008, "GPS Satellites", TagHint.HINT_STRING),
    GPS_STATUS(0x0009, "GPS Status", TagHint.HINT_STRING),
    GPS_MEASURE_MODE(0x000A, "GPS Measure Mode", TagHint.HINT_STRING),
    GPS_DOP(0x000B, "GPS DOP", TagHint.HINT_RATIONAL),
    GPS_SPEED_REF(0x000C, "GPS Speed Ref", TagHint.HINT_STRING),
    GPS_SPEED(0x000D, "GPS Speed", TagHint.HINT_RATIONAL),
    GPS_TRACK_REF(0x000E, "GPS Track Ref", TagHint.HINT_STRING),
    GPS_TRACK(0x000F, "GPS Track", TagHint.HINT_RATIONAL),
    GPS_IMG_DIRECTION_REF(0x0010, "GPS Image Direction Ref", TagHint.HINT_STRING),
    GPS_IMG_DIRECTION(0x0011, "GPS Image Direction", TagHint.HINT_RATIONAL),
    GPS_MAP_DATUM(0x0012, "GPS Map Datum", TagHint.HINT_STRING),
    GPS_DEST_LATITUDE_REF(0x0013, "GPS Dest Latitude Ref", TagHint.HINT_STRING),
    GPS_DEST_LATITUDE(0x0014, "GPS Dest Latitude", TagHint.HINT_RATIONAL),
    GPS_DEST_LONGITUDE_REF(0x0015, "GPS Dest Longitude Ref", TagHint.HINT_STRING),
    GPS_DEST_LONGITUDE(0x0016, "GPS Dest Longitude", TagHint.HINT_RATIONAL),
    GPS_DEST_BEARING_REF(0x0017, "GPS Dest Bearing Ref", TagHint.HINT_STRING),
    GPS_DEST_BEARING(0x0018, "GPS Dest Bearing", TagHint.HINT_RATIONAL),
    GPS_DEST_DISTANCE_REF(0x0019, "GPS Dest Distance Ref", TagHint.HINT_STRING),
    GPS_DEST_DISTANCE(0x001A, "GPS Dest Distance", TagHint.HINT_RATIONAL),
    GPS_PROCESSING_METHOD(0x001B, "GPS Processing Method", TagHint.HINT_ENCODED_STRING),
    GPS_AREA_INFORMATION(0x001C, "GPS Area Information", TagHint.HINT_ENCODED_STRING),
    GPS_DATE_STAMP(0x001D, "GPS Date Stamp", TagHint.HINT_DATE),
    GPS_DIFFERENTIAL(0x001E, "GPS Differential", TagHint.HINT_SHORT),
    GPS_HPOSITIONING_ERROR(0x001F, "GPS H Positioning Error", TagHint.HINT_RATIONAL);

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
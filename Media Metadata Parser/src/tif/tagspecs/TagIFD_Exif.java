package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagIFD_Exif implements Taggable
{
    /* --- Capture & Exposure Settings --- */
    EXIF_EXPOSURE_TIME(0x829A, "Exposure Time", TagHint.HINT_RATIONAL),
    EXIF_FNUMBER(0x829D, "F-Number", TagHint.HINT_RATIONAL),
    EXIF_EXPOSURE_PROGRAM(0x8822, "Exposure Program"),
    EXIF_SPECTRAL_SENSITIVITY(0x8824, "Spectral Sensitivity"),
    EXIF_ISOSPEED_RATINGS(0x8827, "ISO Speed Ratings"),
    EXIF_OECF(0x8828, "Optoelectronic Conversion Function"),
    EXIF_SENSITIVITY_TYPE(0x8830, "Sensitivity Type"),
    EXIF_STANDARD_OUTPUT_SENSITIVITY(0x8831, "Standard Output Sensitivity"),
    EXIF_RECOMMENDED_EXPOSURE_INDEX(0x8832, "Recommended Exposure Index"),
    EXIF_ISOSPEED(0x8833, "ISO Speed"),
    EXIF_ISOSPEED_LATITUDEYYY(0x8834, "ISO Speed Latitude yyy"),
    EXIF_ISOSPEED_LATITUDEZZZ(0x8835, "ISO Speed Latitude zzz"),
    EXIF_TAG_EXIF_VERSION(0x9000, "Exif Version", TagHint.HINT_UNDEFINED),
    EXIF_SHUTTER_SPEED_VALUE(0x9201, "Shutter Speed Value", TagHint.HINT_RATIONAL),
    EXIF_APERTURE_VALUE(0x9202, "Aperture Value", TagHint.HINT_RATIONAL),
    EXIF_BRIGHTNESS_VALUE(0x9203, "Brightness Value", TagHint.HINT_RATIONAL),
    EXIF_EXPOSURE_BIAS_VALUE(0x9204, "Exposure Bias Value", TagHint.HINT_RATIONAL),
    EXIF_MAX_APERTURE_VALUE(0x9205, "Max Aperture Value"),
    EXIF_METERING_MODE(0x9207, "Metering Mode"),
    EXIF_LIGHT_SOURCE(0x9208, "Light Source"),
    EXIF_FLASH(0x9209, "Flash"),
    EXIF_FOCAL_LENGTH(0x920A, "Focal Length", TagHint.HINT_RATIONAL),
    EXIF_SUBJECT_AREA(0x9214, "Subject Area"),
    EXIF_FLASHPIX_VERSION(0xA000, "Flashpix Version", TagHint.HINT_UNDEFINED),
    EXIF_FLASH_ENERGY(0xA20B, "Flash Energy"),
    EXIF_SCENE_TYPE(0xA301, "Scene Type"),
    EXIF_EXPOSURE_INDEX(0xA215, "Exposure Index"),
    EXIF_SENSING_METHOD(0xA217, "Sensing Method"),
    EXIF_EXPOSURE_MODE(0xA402, "Exposure Mode"),
    EXIF_WHITE_BALANCE(0xA403, "White Balance"),
    EXIF_FOCAL_LENGTH_IN_35MM_FILM(0xA405, "Focal Length In 35mm Film"),
    EXIF_SCENE_CAPTURE_TYPE(0xA406, "Scene Capture Type"),
    EXIF_GAIN_CONTROL(0xA407, "Gain Control"),
    EXIF_OFFSET_SCHEMA(0xEA1D, "Offset Schema"),
    EXIF_PADDING(0xEA1C, "Microsoft Padding", TagHint.HINT_BYTE_STREAM),

    /* --- Date & Time --- */
    EXIF_DATE_TIME_ORIGINAL(0x9003, "Date/Time Original", TagHint.HINT_DATE),
    EXIF_DATE_TIME_DIGITIZED(0x9004, "Date/Time Digitized", TagHint.HINT_DATE),
    EXIF_OFFSET_TIME(0x9010, "Offset Time (Time Zone)"),
    EXIF_OFFSET_TIME_ORIGINAL(0x9011, "Offset Time Original"),
    EXIF_OFFSET_TIME_DIGITIZED(0x9012, "Offset Time Digitized"),
    EXIF_SUBSEC_TIME(0x9290, "Sub-second Time", TagHint.HINT_STRING),
    EXIF_SUBSEC_TIME_ORIGINAL(0x9291, "Sub-second Time Original", TagHint.HINT_STRING),
    EXIF_SUBSEC_TIME_DIGITIZED(0x9292, "Sub-second Time Digitized", TagHint.HINT_STRING),

    /* --- Image Characterisation --- */
    EXIF_COLOR_SPACE(0xA001, "Color Space"),
    EXIF_PIXEL_XDIMENSION(0xA002, "Pixel X Dimension"),
    EXIF_PIXEL_YDIMENSION(0xA003, "Pixel Y Dimension"),
    EXIF_COMPONENTS_CONFIGURATION(0x9101, "Components Configuration", TagHint.HINT_BYTE),
    EXIF_COMPRESSED_BITS_PER_PIXEL(0x9102, "Compressed Bits Per Pixel"),
    EXIF_CONTRAST(0xA408, "Contrast"),
    EXIF_SATURATION(0xA409, "Saturation"),
    EXIF_SHARPNESS(0xA40A, "Sharpness"),
    EXIF_LENS_SPECIFICATION(0xA432, "Lens Specification", TagHint.HINT_RATIONAL),
    EXIF_GAMMA(0xA500, "Gamma"),

    /* --- Hardware & Lens --- */
    EXIF_MAKER_NOTE(0x927C, "Maker Note", TagHint.HINT_BYTE_STREAM),
    EXIF_USER_COMMENT(0x9286, "User Comment", TagHint.HINT_ENCODED_STRING),
    EXIF_LENS_MAKE(0xA433, "Lens Make"),
    EXIF_LENS_MODEL(0xA434, "Lens Model"),
    EXIF_LENS_SERIAL_NUMBER(0xA435, "Lens Serial Number"),
    EXIF_BODY_SERIAL_NUMBER(0xA431, "Body Serial Number"),
    EXIF_CAMERA_OWNER_NAME(0xA430, "Camera Owner Name");

    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagIFD_Exif(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    private TagIFD_Exif(int id, String desc, TagHint clue)
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
        return DirectoryIdentifier.IFD_EXIF_SUBIFD_DIRECTORY;
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
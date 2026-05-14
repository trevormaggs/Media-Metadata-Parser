package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagIFD_Exif implements Taggable
{
    /* --- Capture & Exposure Settings --- */
    EXIF_EXPOSURE_TIME(0x829A, "Exposure Time", TagHint.HINT_RATIONAL),
    EXIF_FNUMBER(0x829D, "F-Number", TagHint.HINT_RATIONAL),
    EXIF_EXPOSURE_PROGRAM(0x8822, "Exposure Program", TagHint.HINT_DEFAULT),
    EXIF_SPECTRAL_SENSITIVITY(0x8824, "Spectral Sensitivity", TagHint.HINT_DEFAULT),
    EXIF_ISOSPEED_RATINGS(0x8827, "ISO Speed Ratings", TagHint.HINT_DEFAULT),
    EXIF_OECF(0x8828, "Optoelectronic Conversion Function", TagHint.HINT_DEFAULT),
    EXIF_SENSITIVITY_TYPE(0x8830, "Sensitivity Type", TagHint.HINT_DEFAULT),
    EXIF_STANDARD_OUTPUT_SENSITIVITY(0x8831, "Standard Output Sensitivity", TagHint.HINT_DEFAULT),
    EXIF_RECOMMENDED_EXPOSURE_INDEX(0x8832, "Recommended Exposure Index", TagHint.HINT_DEFAULT),
    EXIF_ISOSPEED(0x8833, "ISO Speed", TagHint.HINT_DEFAULT),
    EXIF_ISOSPEED_LATITUDEYYY(0x8834, "ISO Speed Latitude yyy", TagHint.HINT_DEFAULT),
    EXIF_ISOSPEED_LATITUDEZZZ(0x8835, "ISO Speed Latitude zzz", TagHint.HINT_DEFAULT),
    EXIF_TAG_EXIF_VERSION(0x9000, "Exif Version", TagHint.HINT_UNDEFINED),
    EXIF_SHUTTER_SPEED_VALUE(0x9201, "Shutter Speed Value", TagHint.HINT_RATIONAL),
    EXIF_APERTURE_VALUE(0x9202, "Aperture Value", TagHint.HINT_RATIONAL),
    EXIF_BRIGHTNESS_VALUE(0x9203, "Brightness Value", TagHint.HINT_RATIONAL),
    EXIF_EXPOSURE_BIAS_VALUE(0x9204, "Exposure Bias Value", TagHint.HINT_RATIONAL),
    EXIF_MAX_APERTURE_VALUE(0x9205, "Max Aperture Value", TagHint.HINT_DEFAULT),
    EXIF_METERING_MODE(0x9207, "Metering Mode", TagHint.HINT_DEFAULT),
    EXIF_LIGHT_SOURCE(0x9208, "Light Source", TagHint.HINT_DEFAULT),
    EXIF_FLASH(0x9209, "Flash", TagHint.HINT_DEFAULT),
    EXIF_FOCAL_LENGTH(0x920A, "Focal Length", TagHint.HINT_RATIONAL),    
    EXIF_FLASHPIX_VERSION(0xA000, "Flashpix Version", TagHint.HINT_UNDEFINED),    
    EXIF_FLASH_ENERGY(0xA20B, "Flash Energy", TagHint.HINT_DEFAULT),
    EXIF_EXPOSURE_INDEX(0xA215, "Exposure Index", TagHint.HINT_DEFAULT),
    EXIF_EXPOSURE_MODE(0xA402, "Exposure Mode", TagHint.HINT_DEFAULT),
    EXIF_WHITE_BALANCE(0xA403, "White Balance", TagHint.HINT_DEFAULT),
    EXIF_FOCAL_LENGTH_IN_35MM_FILM(0xA405, "Focal Length In 35mm Film", TagHint.HINT_DEFAULT),
    EXIF_SCENE_CAPTURE_TYPE(0xA406, "Scene Capture Type", TagHint.HINT_DEFAULT),
    EXIF_GAIN_CONTROL(0xA407, "Gain Control", TagHint.HINT_DEFAULT),

    /* --- Date & Time --- */
    EXIF_DATE_TIME_ORIGINAL(0x9003, "Date/Time Original", TagHint.HINT_DATE),
    EXIF_DATE_TIME_DIGITIZED(0x9004, "Date/Time Digitized", TagHint.HINT_DATE),
    EXIF_OFFSET_TIME(0x9010, "Offset Time (Time Zone)", TagHint.HINT_DEFAULT),
    EXIF_OFFSET_TIME_ORIGINAL(0x9011, "Offset Time Original", TagHint.HINT_DEFAULT),
    EXIF_OFFSET_TIME_DIGITIZED(0x9012, "Offset Time Digitized", TagHint.HINT_DEFAULT),
    EXIF_SUBSEC_TIME(0x9290, "Sub-second Time", TagHint.HINT_STRING),
    EXIF_SUBSEC_TIME_ORIGINAL(0x9291, "Sub-second Time Original", TagHint.HINT_STRING),
    EXIF_SUBSEC_TIME_DIGITIZED(0x9292, "Sub-second Time Digitized", TagHint.HINT_STRING),

    /* --- Image Characterization --- */
    EXIF_COLOR_SPACE(0xA001, "Color Space", TagHint.HINT_DEFAULT),
    EXIF_PIXEL_XDIMENSION(0xA002, "Pixel X Dimension", TagHint.HINT_DEFAULT),
    EXIF_PIXEL_YDIMENSION(0xA003, "Pixel Y Dimension", TagHint.HINT_DEFAULT),
    EXIF_COMPONENTS_CONFIGURATION(0x9101, "Components Configuration", TagHint.HINT_BYTE),
    EXIF_COMPRESSED_BITS_PER_PIXEL(0x9102, "Compressed Bits Per Pixel", TagHint.HINT_DEFAULT),
    EXIF_CONTRAST(0xA408, "Contrast", TagHint.HINT_DEFAULT),
    EXIF_SATURATION(0xA409, "Saturation", TagHint.HINT_DEFAULT),
    EXIF_SHARPNESS(0xA40A, "Sharpness", TagHint.HINT_DEFAULT),
    EXIF_LENS_SPECIFICATION(0xA432, "Lens Specification", TagHint.HINT_RATIONAL),
    EXIF_GAMMA(0xA500, "Gamma", TagHint.HINT_DEFAULT),

    /* --- Hardware & Lens --- */
    EXIF_MAKER_NOTE(0x927C, "Maker Note", TagHint.HINT_BYTE_STREAM),
    EXIF_USER_COMMENT(0x9286, "User Comment", TagHint.HINT_ENCODED_STRING),
    EXIF_LENS_MAKE(0xA433, "Lens Make", TagHint.HINT_DEFAULT),
    EXIF_LENS_MODEL(0xA434, "Lens Model", TagHint.HINT_DEFAULT),
    EXIF_LENS_SERIAL_NUMBER(0xA435, "Lens Serial Number", TagHint.HINT_DEFAULT),
    EXIF_BODY_SERIAL_NUMBER(0xA431, "Body Serial Number", TagHint.HINT_DEFAULT),
    EXIF_CAMERA_OWNER_NAME(0xA430, "Camera Owner Name", TagHint.HINT_DEFAULT),

    /* --- DNG Specific (Digital Negative) --- */
    EXIF_DNGVERSION(0xC612, "DNG Version", TagHint.HINT_DEFAULT),
    EXIF_DNGBACKWARD_VERSION(0xC613, "DNG Backward Version", TagHint.HINT_DEFAULT),
    EXIF_UNIQUE_CAMERA_MODEL(0xC614, "Unique Camera Model", TagHint.HINT_DEFAULT),
    EXIF_BLACK_LEVEL(0xC61A, "Black Level", TagHint.HINT_DEFAULT),
    EXIF_WHITE_LEVEL(0xC61D, "White Level", TagHint.HINT_DEFAULT),
    EXIF_AS_SHOT_NEUTRAL(0xC628, "As Shot Neutral", TagHint.HINT_DEFAULT),

    /*
     * Note, these tags can appear in the Exif Sub-IFD, they are almost exclusively written by
     * Windows and Adobe software into the Root IFD (IFD0).
     */

    // TODO: CHECK IF TagHint.HINT_UCS2 SHOULD BE USED INSTEAD?

    /*
     * These tags are written by Windows (UCS-2 encoded) and Adobe
     * typically into the Root IFD (IFD0).
     */
    EXIF_XPTITLE(0x9C9B, "Windows XP Title", TagHint.HINT_UCS2),
    EXIF_XPCOMMENT(0x9C9C, "Windows XP Comment", TagHint.HINT_UCS2),
    EXIF_XPKEYWORDS(0x9C9E, "Windows XP Keywords", TagHint.HINT_UCS2),
    EXIF_XPSUBJECT(0x9C9F, "Windows XP Subject", TagHint.HINT_UCS2),
    EXIF_IMAGE_RATING(0x4746, "Rating (0-5)", TagHint.HINT_DEFAULT),
    EXIF_IMAGE_RATING_PERCENT(0x4749, "Rating Percent", TagHint.HINT_DEFAULT),
    EXIF_INTEROPERABILITY_POINTER(0xA005, "Interoperability Offset", TagHint.HINT_DEFAULT);

    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagIFD_Exif(int id, String desc, TagHint clue)
    {
        this.numID = id;
        this.desc = desc;
        this.hint = clue;
    }

    private TagIFD_Exif(int id)
    {
        this(id, "", TagHint.HINT_DEFAULT);
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
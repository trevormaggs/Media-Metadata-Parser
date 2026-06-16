package tif.tagspecs;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Exchangeable Image File Format (Exif) sub-directory metadata tags within the
 * {@link DirectoryIdentifier#IFD_EXIF_SUBIFD_DIRECTORY} scope.
 * 
 * @author Trevor Maggs
 * @version 1.2
 */
public enum TagIFD_Exif implements Taggable
{
    /* --- Capture & Exposure Settings --- */
    EXIF_EXPOSURE_TIME(0x829A, "Exposure Time", TagHint.HINT_RATIONAL),
    EXIF_FNUMBER(0x829D, "F-Number", TagHint.HINT_RATIONAL),
    EXIF_EXPOSURE_PROGRAM(0x8822, "Exposure Program", TagHint.HINT_SHORT),
    EXIF_SPECTRAL_SENSITIVITY(0x8824, "Spectral Sensitivity", TagHint.HINT_STRING),
    EXIF_ISOSPEED_RATINGS(0x8827, "ISO Speed Ratings", TagHint.HINT_SHORT),
    EXIF_OECF(0x8828, "Optoelectronic Conversion Function", TagHint.HINT_UNDEFINED),
    EXIF_SENSITIVITY_TYPE(0x8830, "Sensitivity Type", TagHint.HINT_BYTE),
    EXIF_STANDARD_OUTPUT_SENSITIVITY(0x8831, "Standard Output Sensitivity", TagHint.HINT_INTEGER),
    EXIF_RECOMMENDED_EXPOSURE_INDEX(0x8832, "Recommended Exposure Index", TagHint.HINT_INTEGER),
    EXIF_ISOSPEED(0x8833, "ISO Speed", TagHint.HINT_INTEGER),
    EXIF_ISOSPEED_LATITUDE_YYY(0x8834, "ISO Speed Latitude yyy", TagHint.HINT_INTEGER),
    EXIF_ISOSPEED_LATITUDE_ZZZ(0x8835, "ISO Speed Latitude zzz", TagHint.HINT_INTEGER),
    EXIF_TAG_EXIF_VERSION(0x9000, "Exif Version", TagHint.HINT_VERSION),
    EXIF_SHUTTER_SPEED_VALUE(0x9201, "Shutter Speed Value", TagHint.HINT_SRATIONAL),
    EXIF_APERTURE_VALUE(0x9202, "Aperture Value", TagHint.HINT_RATIONAL),
    EXIF_BRIGHTNESS_VALUE(0x9203, "Brightness Value", TagHint.HINT_SRATIONAL),
    EXIF_EXPOSURE_BIAS_VALUE(0x9204, "Exposure Bias Value", TagHint.HINT_SRATIONAL),
    EXIF_MAX_APERTURE_VALUE(0x9205, "Max Aperture Value", TagHint.HINT_RATIONAL),
    EXIF_METERING_MODE(0x9207, "Metering Mode", TagHint.HINT_SHORT),
    EXIF_LIGHT_SOURCE(0x9208, "Light Source", TagHint.HINT_SHORT),
    EXIF_FLASH(0x9209, "Flash", TagHint.HINT_SHORT),
    EXIF_FOCAL_LENGTH(0x920A, "Focal Length", TagHint.HINT_RATIONAL),
    EXIF_SUBJECT_AREA(0x9214, "Subject Area", TagHint.HINT_SHORT),
    EXIF_FLASHPIX_VERSION(0xA000, "Flashpix Version", TagHint.HINT_VERSION),
    EXIF_FLASH_ENERGY(0xA20B, "Flash Energy", TagHint.HINT_RATIONAL),
    EXIF_SCENE_TYPE(0xA301, "Scene Type", TagHint.HINT_UNDEFINED),
    EXIF_EXPOSURE_INDEX(0xA215, "Exposure Index", TagHint.HINT_RATIONAL),
    EXIF_SENSING_METHOD(0xA217, "Sensing Method", TagHint.HINT_SHORT),
    EXIF_EXPOSURE_MODE(0xA402, "Exposure Mode", TagHint.HINT_SHORT),
    EXIF_WHITE_BALANCE(0xA403, "White Balance", TagHint.HINT_SHORT),
    EXIF_FOCAL_LENGTH_IN_35MM_FILM(0xA405, "Focal Length In 35mm Film", TagHint.HINT_SHORT),
    EXIF_SCENE_CAPTURE_TYPE(0xA406, "Scene Capture Type", TagHint.HINT_SHORT),
    EXIF_GAIN_CONTROL(0xA407, "Gain Control", TagHint.HINT_SHORT),
    EXIF_OFFSET_SCHEMA(0xEA1D, "Offset Schema", TagHint.HINT_INTEGER),
    EXIF_PADDING(0xEA1C, "Microsoft Padding", TagHint.HINT_BYTE_STREAM),

    /* --- Date & Time --- */
    EXIF_DATE_TIME_ORIGINAL(0x9003, "Date/Time Original", TagHint.HINT_DATE),
    EXIF_DATE_TIME_DIGITIZED(0x9004, "Date/Time Digitized", TagHint.HINT_DATE),
    EXIF_OFFSET_TIME(0x9010, "Offset Time (Time Zone)", TagHint.HINT_STRING),
    EXIF_OFFSET_TIME_ORIGINAL(0x9011, "Offset Time Original", TagHint.HINT_STRING),
    EXIF_OFFSET_TIME_DIGITIZED(0x9012, "Offset Time Digitized", TagHint.HINT_STRING),
    EXIF_SUBSEC_TIME(0x9290, "Sub-second Time", TagHint.HINT_STRING),
    EXIF_SUBSEC_TIME_ORIGINAL(0x9291, "Sub-second Time Original", TagHint.HINT_STRING),
    EXIF_SUBSEC_TIME_DIGITIZED(0x9292, "Sub-second Time Digitized", TagHint.HINT_STRING),

    /* --- Image Characterisation --- */
    EXIF_COLOR_SPACE(0xA001, "Color Space", TagHint.HINT_SHORT),
    EXIF_PIXEL_XDIMENSION(0xA002, "Pixel X Dimension", TagHint.HINT_INTEGER),
    EXIF_PIXEL_YDIMENSION(0xA003, "Pixel Y Dimension", TagHint.HINT_INTEGER),
    EXIF_COMPONENTS_CONFIGURATION(0x9101, "Components Configuration", TagHint.HINT_UNDEFINED),
    EXIF_COMPRESSED_BITS_PER_PIXEL(0x9102, "Compressed Bits Per Pixel", TagHint.HINT_RATIONAL),
    EXIF_CONTRAST(0xA408, "Contrast", TagHint.HINT_SHORT),
    EXIF_SATURATION(0xA409, "Saturation", TagHint.HINT_SHORT),
    EXIF_SHARPNESS(0xA40A, "Sharpness", TagHint.HINT_SHORT),
    EXIF_LENS_SPECIFICATION(0xA432, "Lens Specification", TagHint.HINT_RATIONAL),
    EXIF_GAMMA(0xA500, "Gamma", TagHint.HINT_RATIONAL),

    /* --- Hardware & Lens --- */
    EXIF_MAKER_NOTE(0x927C, "Maker Note", TagHint.HINT_BYTE_STREAM),
    EXIF_USER_COMMENT(0x9286, "User Comment", TagHint.HINT_ENCODED_STRING),
    EXIF_LENS_MAKE(0xA433, "Lens Make", TagHint.HINT_STRING),
    EXIF_LENS_MODEL(0xA434, "Lens Model", TagHint.HINT_STRING),
    EXIF_LENS_SERIAL_NUMBER(0xA435, "Lens Serial Number", TagHint.HINT_STRING),
    EXIF_BODY_SERIAL_NUMBER(0xA431, "Body Serial Number", TagHint.HINT_STRING),
    EXIF_CAMERA_OWNER_NAME(0xA430, "Camera Owner Name", TagHint.HINT_STRING);

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

    @Override
    public String translate(Object val)
    {
        switch (this)
        {
            case EXIF_TAG_EXIF_VERSION:
            case EXIF_FLASHPIX_VERSION:
                return translateVersionBytes(val);

            case EXIF_USER_COMMENT:
                return translateUserComment(val);

            case EXIF_EXPOSURE_MODE:
                return translateExposureMode(val);

            case EXIF_EXPOSURE_PROGRAM:
                return translateExposureProgram(val);

            case EXIF_METERING_MODE:
                // return translateMeteringMode(num);

            case EXIF_LIGHT_SOURCE:
                // return translateLightSource(num);

            case EXIF_FLASH:
                // return translateFlash(num);

            case EXIF_COLOR_SPACE:
                // return translateColorSpace(num);

            case EXIF_WHITE_BALANCE:
                // return translateWhiteBalance(num);

            case EXIF_SCENE_CAPTURE_TYPE:
                // return translateSceneCaptureType(num);

            case EXIF_GAIN_CONTROL:
                // return translateGainControl(num);

            case EXIF_CONTRAST:
            case EXIF_SATURATION:
            case EXIF_SHARPNESS:
                // return translateSoftnessMetric(num);

            case EXIF_SENSITIVITY_TYPE:
                // return translateSensitivityType(num);

            case EXIF_SENSING_METHOD:
                // return translateSensingMethod(num);

            case EXIF_COMPONENTS_CONFIGURATION:
                // return translateComponentsConfiguration(val);

            default:
            break;
        }

        return Taggable.super.translate(val);
    }

    private String translateVersionBytes(Object val)
    {
        if (val instanceof byte[])
        {
            byte[] bytes = (byte[]) val;

            if (bytes.length >= 4)
            {
                return new String(bytes, 0, 4, StandardCharsets.US_ASCII);
            }
        }

        return String.valueOf(val);
    }

    private String translateUserComment(Object val)
    {
        if (val instanceof byte[])
        {
            byte[] bytes = (byte[]) val;

            if (bytes.length <= 8)
            {
                return "";
            }

            // Read the first 8 bytes for character encoding designation
            String prefix = new String(bytes, 0, 8, StandardCharsets.US_ASCII).trim();
            Charset charset = StandardCharsets.UTF_8; // Default fallback

            if (prefix.startsWith("ASCII"))
            {
                charset = StandardCharsets.US_ASCII;
            }

            else if (prefix.startsWith("UNICODE"))
            {
                charset = StandardCharsets.UTF_16; // Standard default (looks for BOM)

                // If there are at least 2 bytes of text payload, check for an explicit BOM
                if (bytes.length >= 10)
                {
                    int b1 = bytes[8] & 0xFF;
                    int b2 = bytes[9] & 0xFF;

                    if (b1 == 0xFF && b2 == 0xFE)
                    {
                        charset = StandardCharsets.UTF_16LE;
                    }

                    else if (b1 == 0xFE && b2 == 0xFF)
                    {
                        charset = StandardCharsets.UTF_16BE;
                    }

                    else
                    {
                        // No BOM found. EXIF spec technically defaults to big-endian,
                        // but many cameras write little-endian. If you notice gibberish
                        // on certain devices, swap this default to UTF_16LE.
                        charset = StandardCharsets.UTF_16BE;
                    }
                }
            }

            else if (prefix.startsWith("JIS"))
            {
                charset = Charset.forName("Shift_JIS");
            }

            // Decode the remainder of the payload using the matched character set
            String decoded = new String(bytes, 8, bytes.length - 8, charset);

            // Safety step: Replace hidden structural null padding bytes (\0) before trimming
            // whitespace
            return decoded.replace("\0", "").trim();
        }

        return String.valueOf(val);
    }

    private String translateExposureMode(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0:
                return "Auto exposure";

            case 1:
                return "Manual exposure";

            case 2:
                return "Auto bracket";

            default:
                return Taggable.super.translate(val);
        }
    }

    private String translateExposureProgram(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0:
                return "Not defined";

            case 1:
                return "Manual";

            case 2:
                return "Normal program";

            case 3:
                return "Aperture priority";

            case 4:
                return "Shutter priority";

            case 5:
                return "Creative program (biased toward depth of field)";

            case 6:
                return "Action program (biased toward fast shutter speed)";

            case 7:
                return "Portrait mode (for closeup photos with the background out of focus)";

            case 8:
                return "Landscape mode (for landscape photos with the background in focus)";

            default:
                return Taggable.super.translate(val);
        }
    }
}
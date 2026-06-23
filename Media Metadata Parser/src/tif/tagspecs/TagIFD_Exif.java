package tif.tagspecs;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import tif.DirectoryIdentifier;
import tif.RationalNumber;
import tif.TagHint;
import tif.TagValueFormatter;

/**
 * Exchangeable Image File Format (Exif) sub-directory metadata tags within the
 * {@link DirectoryIdentifier#IFD_EXIF_SUBIFD_DIRECTORY} scope.
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 18 June 2026
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
    EXIF_FOCAL_PLANE_X_RESOLUTION(0xA20E, "FocalPlaneXResolution", TagHint.HINT_RATIONAL),
    EXIF_FOCAL_PLANE_Y_RESOLUTION(0xA20F, "FocalPlaneYResolution", TagHint.HINT_RATIONAL),
    EXIF_FOCAL_PLANE_RESOLUTION_UNIT(0xA210, "FocalPlaneResolutionUnit", TagHint.HINT_SHORT),
    EXIF_CUSTOM_RENDERED(0xA401, "CustomRendered", TagHint.HINT_SHORT),
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
    EXIF_CAMERA_OWNER_NAME(0xA430, "Camera Owner Name", TagHint.HINT_STRING),
    EXIF_COMPOSITE_IMAGE(0xA460, "Composite Image", TagHint.HINT_SHORT);

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

            case EXIF_EXPOSURE_MODE:
                return translateExposureMode(val);

            case EXIF_EXPOSURE_PROGRAM:
                return translateExposureProgram(val);

            case EXIF_FOCAL_PLANE_RESOLUTION_UNIT:
                return translateFocalPlaneResolutionUnit(val);

            case EXIF_CUSTOM_RENDERED:
                return translateCustomRendered(val);

            case EXIF_FOCAL_PLANE_X_RESOLUTION:
            case EXIF_FOCAL_PLANE_Y_RESOLUTION:

                if (val instanceof RationalNumber)
                {
                    double resolutionValue = ((RationalNumber) val).doubleValue();
                    return (resolutionValue % 1 == 0) ? String.format(Locale.US, "%.0f", resolutionValue)
                            : String.format(Locale.US, "%.2f", resolutionValue);
                }

            break;

            case EXIF_WHITE_BALANCE:
                return translateWhiteBalance(val);

            case EXIF_SCENE_CAPTURE_TYPE:
                return translateSceneCaptureType(val);

            case EXIF_COLOR_SPACE:
                return translateColorSpace(val);

            case EXIF_FLASH:
                return translateFlash(val);

            case EXIF_METERING_MODE:
                return translateMeteringMode(val);

            case EXIF_SCENE_TYPE:
                return translateSceneType(val);

            case EXIF_APERTURE_VALUE:
                return translateApexAperture(val);

            case EXIF_SHUTTER_SPEED_VALUE:
                return translateApexShutterSpeed(val);

            case EXIF_LIGHT_SOURCE:
                return Taggable.translateLightSource(val);

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
            byte[] bytes = TagValueFormatter.toByteArray(val);

            if (bytes != null && bytes.length >= 4)
            {
                return new String(bytes, 0, 4, StandardCharsets.US_ASCII);
            }
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
                return "Creative program";

            case 6:
                return "Action program";

            case 7:
                return "Portrait";

            case 8:
                return "Landscape";

            default:
                return Taggable.super.translate(val);
        }
    }

    private String translateFocalPlaneResolutionUnit(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 1:
                return "None";

            case 2:
                return "inches";

            case 3:
                return "cm";

            case 4:
                return "mm";

            case 5:
                return "um";

            default:
                return Taggable.super.translate(val);
        }
    }

    private String translateCustomRendered(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0:
                return "Normal process";

            case 1:
                return "Custom process";

            default:
                return String.valueOf(val);
        }
    }

    private String translateWhiteBalance(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0:
                return "Auto white balance";

            case 1:
                return "Manual white balance";
        }

        return String.valueOf(val);
    }

    private String translateSceneCaptureType(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0:
                return "Standard";

            case 1:
                return "Landscape";

            case 2:
                return "Portrait";

            case 3:
                return "Night scene";
        }

        return String.valueOf(val);
    }

    private String translateColorSpace(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 1:
                return "sRGB";

            case 2:
                return "Adobe RGB";

            case 0xFFFF:
                return "Uncalibrated";

            default:
                return Taggable.super.translate(val);
        }
    }

    private String translateFlash(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0x0000:
                return "Did not fire";

            case 0x0001:
                return "Fired";

            case 0x0005:
                return "Fired, Return not detected";

            case 0x0007:
                return "Fired, Return detected";

            case 0x0009:
                return "Fired, Compulsory Flash Mode";

            case 0x000D:
                return "Fired, Compulsory Flash Mode, Return not detected";

            case 0x000F:
                return "Fired, Compulsory Flash Mode, Return detected";

            case 0x0010:
                return "Did not fire, Compulsory";

            case 0x0018:
                return "Did not fire, Auto mode";

            case 0x0019:
                return "Fired, Auto Mode";

            case 0x001D:
                return "Fired, Auto Mode, Return not detected";

            case 0x001F:
                return "Fired, Auto Mode, Return detected";

            case 0x0020:
                return "No Flash Function";

            case 0x0041:
                return "Fired, Red-eye Reduction";

            case 0x0045:
                return "Fired, Red-eye Reduction, Return not detected";

            case 0x0047:
                return "Fired, Red-eye Reduction, Return detected";

            case 0x0049:
                return "Fired, Compulsory, Red-eye Reduction";

            case 0x004D:
                return "Fired, Compulsory, Red-eye Reduction, Return not detected";

            case 0x004F:
                return "Fired, Compulsory, Red-eye Reduction, Return detected";

            case 0x0059:
                return "Fired, Auto, Red-eye Reduction";

            case 0x005D:
                return "Fired, Auto, Red-eye Reduction, Return not detected";

            case 0x005F:
                return "Fired, Auto, Red-eye Reduction, Return detected";

            default:
                return Taggable.super.translate(val);
        }
    }

    private String translateMeteringMode(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0:
                return "Unknown";

            case 1:
                return "Average";

            case 2:
                return "CenterWeightedAverage";

            case 3:
                return "Spot";

            case 4:
                return "MultiSpot";

            case 5:
                return "Pattern";

            case 6:
                return "Partial";

            case 255:
                return "other";

            default:
                return Taggable.super.translate(val);
        }
    }

    private String translateSceneType(Object val)
    {
        int num = Taggable.convertToInt(val);
        return num == 1 ? "Directly photographed" : "Unknown (" + num + ")";
    }

    /*
     * For formula details, refer to Annex C. [Informative] APEX Units official documentation,
     * "CIPA DC-008-Translation-2024 - Exchangeable image file format for digital still cameras: Exif Version 3.0"
     * and look for APEX (Additive System of Photographic Exposure).
     */
    private String translateApexAperture(Object val)
    {
        if (val instanceof RationalNumber)
        {
            double apertureValue = ((RationalNumber) val).doubleValue();

            if (!Double.isNaN(apertureValue) && !Double.isInfinite(apertureValue))
            {
                // Formula for f-number = 2^(Av / 2) or (sqrt(2))^Av
                double fNumber = Math.pow(2.0, apertureValue / 2.0);
                return String.format(Locale.US, "%.1f", fNumber);
            }
        }

        return Taggable.super.translate(val);
    }

    /*
     * For formula details, refer to Annex C. [Informative] APEX Units official documentation,
     * "CIPA DC-008-Translation-2024 - Exchangeable image file format for digital still cameras: Exif Version 3.0"
     * and look for APEX (Additive System of Photographic Exposure).
     */
    private String translateApexShutterSpeed(Object val)
    {
        if (val instanceof RationalNumber)
        {
            double shutterSpeedValue = ((RationalNumber) val).doubleValue();

            if (!Double.isNaN(shutterSpeedValue) && !Double.isInfinite(shutterSpeedValue))
            {
                // Formula for Exposure Time: time = 1 / (2^Tv) where Tv is the Shutter Speed Value
                double denominator = Math.pow(2.0, shutterSpeedValue);

                if (denominator >= 1.0)
                {
                    // Fast shutter speed (e.g., 1/2488)
                    return String.format(Locale.US, "1/%d", Math.round(denominator));
                }

                else
                {
                    // Long exposure (longer than 1 second)
                    double seconds = 1.0 / denominator;
                    return String.format(Locale.US, "%.1f s", seconds).replaceAll("\\.0 s$", " s");
                }
            }
        }

        return Taggable.super.translate(val);
    }
}
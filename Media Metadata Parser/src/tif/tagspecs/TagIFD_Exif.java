package tif.tagspecs;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import tif.DirectoryIdentifier;
import tif.RationalNumber;
import tif.TagHint;
import tif.TagValueTranslator;

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
        return DirectoryIdentifier.IFD_EXIF_SUBIFD_DIRECTORY;
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

    @Override
    public String translate(Object val)
    {
        switch (this)
        {
            case EXIF_EXPOSURE_PROGRAM: // 0x8822
                return translateExposureProgram(val);

            case EXIF_SENSITIVITY_TYPE: // 0x8830
                return translateSensitivityType(val);

            case EXIF_TAG_EXIF_VERSION: // 0x9000
                return translateVersionBytes(val);

            case EXIF_COMPONENTS_CONFIGURATION: // 0x9101
                return translateComponentsConfiguration(val);

            case EXIF_SHUTTER_SPEED_VALUE: // 0x9201
                return translateApexShutterSpeed(val);

            case EXIF_APERTURE_VALUE: // 0x9202
                return translateApexAperture(val);

            case EXIF_METERING_MODE: // 0x9207
                return translateMeteringMode(val);

            case EXIF_LIGHT_SOURCE: // 0x9208
                return Taggable.translateLightSource(val);

            case EXIF_FLASH: // 0x9209
                return translateFlash(val);

            case EXIF_FLASHPIX_VERSION: // 0xA000
                return translateVersionBytes(val);

            case EXIF_COLOR_SPACE: // 0xA001
                return translateColorSpace(val);

            case EXIF_FOCAL_PLANE_RESOLUTION_UNIT: // 0xA210
                return Taggable.translateResolutionUnit(val);

            case EXIF_SENSING_METHOD: // 0xA217
                return translateSensingMethod(val);

            case EXIF_SCENE_TYPE: // 0xA301
                return translateSceneType(val);

            case EXIF_CUSTOM_RENDERED: // 0xA401
                return translateCustomRendered(val);

            case EXIF_EXPOSURE_MODE: // 0xA402
                return translateExposureMode(val);

            case EXIF_WHITE_BALANCE: // 0xA403
                return translateWhiteBalance(val);

            case EXIF_SCENE_CAPTURE_TYPE: // 0xA406
                return translateSceneCaptureType(val);

            case EXIF_GAIN_CONTROL: // 0xA407
                return translateGainControl(val);

            case EXIF_CONTRAST: // 0xA408
            case EXIF_SHARPNESS: // 0xA40A
                return translateSoftHardScale(val);

            case EXIF_SATURATION: // 0xA409
                return translateSaturation(val);

            case EXIF_LENS_SPECIFICATION: // 0xA432
                return translateLensSpecification(val);

            case EXIF_COMPOSITE_IMAGE: // 0xA460
                return translateCompositeImage(val);

            default:
            break;
        }

        return Taggable.super.translate(val);
    }

    /**
     * Translates the EXIF ExposureProgram enumeration.
     * 
     * <p>
     * The ExposureProgram tag identifies the camera program used to determine exposure settings
     * when the image was captured.
     * </p>
     * 
     * @param val
     *        the raw tag value
     * @return the corresponding exposure program description
     */
    private String translateExposureProgram(Object val)
    {
        switch (Taggable.convertToInt(val))
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
                return "Portrait mode";

            case 8:
                return "Landscape mode";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Translates the EXIF SensitivityType enumeration.
     * 
     * <p>
     * The SensitivityType tag indicates which electronic sensitivity parameters (such as Standard
     * Output Sensitivity, Recommended Exposure Index, or ISO Speed) are designated by the camera
     * configuration.
     * </p>
     * 
     * @param val
     *        the raw SensitivityType tag value
     * @return a descriptive sensitivity parameter mode string
     */
    private String translateSensitivityType(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Unknown";

            case 1:
                return "Standard output sensitivity (SOS)";

            case 2:
                return "Recommended exposure index (REI)";

            case 3:
                return "ISO speed";

            case 4:
                return "SOS and REI";

            case 5:
                return "SOS and ISO speed";

            case 6:
                return "REI and ISO speed";

            case 7:
                return "SOS, REI and ISO speed";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Converts an EXIF version byte sequence into a readable version string.
     * 
     * <p>
     * EXIF version tags such as ExifVersion and FlashpixVersion are stored as four ASCII bytes.
     * This method converts those bytes into a standard version string.
     * </p>
     * 
     * <p>
     * Example:
     * </p>
     * 
     * <pre>
     * {'0','2','3','2'} -&gt; "0232"
     * </pre>
     *
     * @param val
     *        the raw version tag value
     * @return the decoded version string, or the original translated value if the input cannot be
     *         interpreted as a version byte sequence
     */
    private String translateVersionBytes(Object val)
    {
        if (val instanceof byte[])
        {
            byte[] bytes = TagValueTranslator.toByteArray(val);

            if (bytes != null && bytes.length >= 4)
            {
                return new String(bytes, 0, 4, StandardCharsets.US_ASCII);
            }
        }

        return Taggable.super.translate(val);
    }

    /**
     * Translates the layout configuration of channels for UNDEFINED values.
     * 
     * <p>
     * The ComponentsConfiguration tag records the structural sequence of channels (such as Y, Cb,
     * Cr, or R, G, B) to allow processing components without a complex dependency check on color
     * model spaces.
     * </p>
     * 
     * @param val
     *        the raw metadata channel sequence
     * @return a string concatenation of active image component identifiers
     */
    private String translateComponentsConfiguration(Object val)
    {
        if (val instanceof byte[] && ((byte[]) val).length == 4)
        {
            byte[] bytes = (byte[]) val;
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < 4; i++)
            {
                int component = bytes[i] & 0xFF;

                switch (component)
                {
                    case 1:
                        sb.append("Y");
                    break;

                    case 2:
                        sb.append("Cb");
                    break;

                    case 3:
                        sb.append("Cr");
                    break;

                    case 4:
                        sb.append("R");
                    break;

                    case 5:
                        sb.append("G");
                    break;

                    case 6:
                        sb.append("B");
                    break;

                    default:
                    break;
                }
            }

            return sb.length() == 0 ? "Unknown" : sb.toString();
        }

        return Taggable.super.translate(val);
    }

    /**
     * Converts an APEX shutter speed value into an exposure time.
     * 
     * <p>
     * The conversion uses the formula {@code time = 1 / 2^Tv} defined by the Exif specification.
     * </p>
     * 
     * <p>
     * For details of the APEX system, refer to Annex C (Informative) of the Exif specification:
     * "CIPA DC-008-Translation-2024 - Exchangeable image file format for digital still cameras:
     * Exif Version 3.0".
     * </p>
     *
     * @param val
     *        the raw APEX shutter speed value
     * @return the formatted exposure time
     */
    private String translateApexShutterSpeed(Object val)
    {
        if (val instanceof RationalNumber)
        {
            double shutterSpeedValue = ((RationalNumber) val).doubleValue();

            if (!Double.isNaN(shutterSpeedValue) && !Double.isInfinite(shutterSpeedValue))
            {
                double denominator = Math.pow(2.0, shutterSpeedValue);

                if (denominator >= 1.0)
                {
                    return String.format(Locale.US, "1/%d", Math.round(denominator));
                }

                else
                {
                    double seconds = 1.0 / denominator;
                    return String.format(Locale.US, "%.1f s", seconds).replaceAll("\\.0 s$", " s");
                }
            }
        }

        return Taggable.super.translate(val);
    }

    /**
     * Converts an APEX aperture value into an f-number.
     * 
     * <p>
     * The conversion uses the formula {@code f = 2^(Av / 2)} defined by the Exif specification.
     * </p>
     * 
     * <p>
     * For details of the APEX system, refer to Annex C (Informative) of the Exif specification:
     * "CIPA DC-008-Translation-2024 - Exchangeable image file format for digital still cameras:
     * Exif Version 3.0".
     * </p>
     *
     * @param val
     *        the raw APEX aperture value
     * @return the calculated f-number
     */
    private String translateApexAperture(Object val)
    {
        if (val instanceof RationalNumber)
        {
            double apertureValue = ((RationalNumber) val).doubleValue();

            if (!Double.isNaN(apertureValue) && !Double.isInfinite(apertureValue))
            {
                double fNumber = Math.pow(2.0, apertureValue / 2.0);
                return String.format(Locale.US, "%.1f", fNumber);
            }
        }

        return Taggable.super.translate(val);
    }

    /**
     * Converts an EXIF MeteringMode value into a human-readable description.
     * 
     * <p>
     * The MeteringMode tag identifies the method used by the camera to measure scene brightness
     * when determining exposure.
     * </p>
     *
     * @param val
     *        the raw MeteringMode tag value
     * @return a descriptive metering mode, or the default translated value if the value is not
     *         recognised
     */
    private String translateMeteringMode(Object val)
    {
        switch (Taggable.convertToInt(val))
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
                return "Multi-spot";

            case 5:
                return "Pattern";

            case 6:
                return "Partial";

            case 255:
                return "Other";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Translates the EXIF Flash bit-field into a descriptive string.
     * 
     * <p>
     * The EXIF Flash tag stores a coded value that describes whether the flash fired and may also
     * indicate additional information such as automatic flash mode, red-eye reduction, compulsory
     * flash operation, and return-light detection.
     * </p>
     *
     * @param val
     *        the raw flash value
     * @return the corresponding flash description
     */
    private String translateFlash(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Did not fire";

            case 1:
                return "Fired";

            case 5:
                return "Fired, Return not detected";

            case 7:
                return "Fired, Return detected";

            case 9:
                return "Fired, Compulsory Flash Mode";

            case 13:
                return "Fired, Compulsory Flash Mode, Return not detected";

            case 15:
                return "Fired, Compulsory Flash Mode, Return detected";

            case 16:
                return "Off, Did not fire";

            case 24:
                return "Did not fire, Auto mode";

            case 25:
                return "Fired, Auto Mode";

            case 29:
                return "Fired, Auto Mode, Return not detected";

            case 31:
                return "Fired, Auto Mode, Return detected";

            case 32:
                return "No Flash Function";

            case 65:
                return "Fired, Red-eye Reduction";

            case 69:
                return "Fired, Red-eye Reduction, Return not detected";

            case 71:
                return "Fired, Red-eye Reduction, Return detected";

            case 73:
                return "Fired, Compulsory, Red-eye Reduction";

            case 77:
                return "Fired, Compulsory, Red-eye Reduction, Return not detected";

            case 79:
                return "Fired, Compulsory, Red-eye Reduction, Return detected";

            case 89:
                return "Fired, Auto, Red-eye Reduction";

            case 93:
                return "Fired, Auto, Red-eye Reduction, Return not detected";

            case 95:
                return "Fired, Auto, Red-eye Reduction, Return detected";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Converts an EXIF ColorSpace value into a colour-space description.
     * 
     * <p>
     * The ColorSpace tag identifies the colour space used to encode image data. Common values
     * include sRGB and Adobe RGB.
     * </p>
     *
     * @param val
     *        the raw ColorSpace tag value
     * @return a colour-space description, or the default translated value if the value is not
     *         recognised
     */
    private String translateColorSpace(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 1:
                return "sRGB";

            case 2:
                return "Adobe RGB"; // Non-standard CIPA DC-008 (DCF 2.0 extension)

            case 0xFFFF:
                return "Uncalibrated";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Translates the sensor structure types.
     * 
     * <p>
     * The SensingMethod tag identifies the type of image sensor configuration layout on the camera
     * or capturing hardware module.
     * </p>
     * 
     * @param val
     *        the raw structural enumeration value
     * @return the text mapping representation of the hardware layout architecture
     */
    private String translateSensingMethod(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 1:
                return "Not defined";

            case 2:
                return "One-chip color area sensor";

            case 3:
                return "Two-chip color area sensor";

            case 4:
                return "Three-chip color area sensor";

            case 5:
                return "Color sequential area sensor";

            case 7:
                return "Trilinear sensor";

            case 8:
                return "Color sequential linear sensor";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Converts an EXIF SceneType value into a human-readable description.
     * 
     * <p>
     * The SceneType tag indicates whether the image was directly photographed by a digital camera.
     * </p>
     *
     * @param val
     *        the raw SceneType tag value
     * @return a scene type description, or an unknown value description if the value is not
     *         recognised
     */
    private String translateSceneType(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 1:
                return "Directly photographed";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Converts an EXIF CustomRendered value into a human-readable description.
     * 
     * <p>
     * The CustomRendered tag indicates whether the image was processed normally by the camera or
     * subjected to special image-processing operations.
     * </p>
     *
     * @param val
     *        the raw CustomRendered tag value
     * @return a descriptive rendering mode, or the default translated value if the value is not
     *         recognised
     */
    private String translateCustomRendered(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Normal process";

            case 1:
                return "Custom process";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Converts an EXIF ExposureMode value into a human-readable description.
     * 
     * <p>
     * The ExposureMode tag indicates whether exposure settings were selected automatically by the
     * camera, manually by the photographer, or as part of an automatic exposure bracketing
     * sequence.
     * </p>
     *
     * @param val
     *        the raw ExposureMode tag value
     * @return a descriptive exposure mode, or the default translated value if the value is not
     *         recognised
     */
    private String translateExposureMode(Object val)
    {
        switch (Taggable.convertToInt(val))
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

    /**
     * Converts an EXIF WhiteBalance value into a human-readable description.
     * 
     * <p>
     * The WhiteBalance tag indicates whether white balance was determined automatically by the
     * camera or manually selected by the photographer.
     * </p>
     *
     * @param val
     *        the raw WhiteBalance tag value
     * @return a descriptive white balance mode, or the default translated value if the value is not
     *         recognised
     */
    private String translateWhiteBalance(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Auto white balance";

            case 1:
                return "Manual white balance";
        }

        return Taggable.super.translate(val);
    }

    /**
     * Converts an EXIF SceneCaptureType value into a scene description.
     * 
     * <p>
     * The SceneCaptureType tag identifies the type of scene for which the camera was optimised,
     * such as portrait, landscape or night photography.
     * </p>
     *
     * @param val
     *        the raw SceneCaptureType tag value
     * @return a descriptive scene type, or the default translated value if the value is not
     *         recognised
     */
    private String translateSceneCaptureType(Object val)
    {
        switch (Taggable.convertToInt(val))
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

        return Taggable.super.translate(val);
    }

    /**
     * Converts an EXIF GainControl value into a human-readable description.
     * 
     * <p>
     * The GainControl tag indicates the degree of overall image gain adjustment applied to the
     * sensor signal prior to digitisation (closely related to ISO behaviour).
     * </p>
     *
     * @param val
     *        the raw GainControl tag value
     * @return a descriptive gain control adjustment string, or the default translated value if the
     *         value is not recognised
     */
    private String translateGainControl(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "None";

            case 1:
                return "Low gain up";

            case 2:
                return "High gain up";

            case 3:
                return "Low gain down";

            case 4:
                return "High gain down";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Translates three-state camera rendering adjustments that use the Soft/Hard scale.
     * 
     * <p>
     * This utility is shared by {@code EXIF_CONTRAST} (0xA408) and {@code EXIF_SHARPNESS} (0xA40A)
     * configurations.
     * </p>
     * 
     * @param val
     *        the raw metadata processing modifier
     * @return a description of the processing intensity ("Normal", "Soft", or "Hard")
     */
    private String translateSoftHardScale(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Normal";

            case 1:
                return "Soft";

            case 2:
                return "Hard";

            default:
                return String.valueOf(val).trim();
        }
    }

    /**
     * Translates camera saturation rendering settings.
     * 
     * <p>
     * The Saturation tag indicates the direction of saturation processing applied by the camera,
     * using standard baseline parameters.
     * </p>
     * 
     * @param val
     *        the raw metadata saturation parameter modifier
     * @return a description of the processing scale intensity string
     */
    private String translateSaturation(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Normal";

            case 1:
                return "Low saturation";

            case 2:
                return "High saturation";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Translates the EXIF LensSpecification tag into a human-readable lens description.
     * *
     * <p>
     * The value is expected to contain four {@link RationalNumber} elements representing the
     * minimum and maximum focal lengths followed by the minimum F-numbers (maximum apertures)
     * at those focal lengths.
     * </p>
     * *
     * <p>
     * Examples: {@code 24-70mm f/2.8}, {@code 18-55mm f/3.5-5.6}, {@code 50mm f/1.4}
     * </p>
     *
     * @param val
     *        the raw LensSpecification value
     * @return the formatted lens description, or the default translation if the value cannot be
     *         interpreted
     */
    private String translateLensSpecification(Object val)
    {
        RationalNumber[] arr = TagValueTranslator.toRationalArray(val);

        if (arr != null && arr.length == 4)
        {
            double minFocal = arr[0] != null ? arr[0].doubleValue() : 0.0;
            double maxFocal = arr[1] != null ? arr[1].doubleValue() : 0.0;
            double shortFStop = arr[2] != null ? arr[2].doubleValue() : 0.0;
            double longFStop = arr[3] != null ? arr[3].doubleValue() : 0.0;

            if (!Double.isFinite(minFocal) || !Double.isFinite(maxFocal) || !Double.isFinite(shortFStop) || !Double.isFinite(longFStop) || minFocal <= 0.0 || shortFStop <= 0.0)
            {
                return Taggable.super.translate(val);
            }

            StringBuilder sb = new StringBuilder();
            DecimalFormat lensFormat = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));

            // Format Focal Length Range
            if (Double.compare(minFocal, maxFocal) == 0 || maxFocal <= 0.0)
            {
                sb.append(lensFormat.format(minFocal)).append("mm");
            }

            else
            {
                sb.append(lensFormat.format(minFocal)).append("-").append(lensFormat.format(maxFocal)).append("mm");
            }

            sb.append(" f/");

            // Format Max Aperture Range (F-number)
            if (Double.compare(shortFStop, longFStop) == 0 || longFStop <= 0.0)
            {
                sb.append(lensFormat.format(shortFStop));
            }

            else
            {
                sb.append(lensFormat.format(shortFStop)).append("-").append(lensFormat.format(longFStop));
            }

            return sb.toString();
        }

        return Taggable.super.translate(val);
    }

    /**
     * Translates the CompositeImage enumeration integers into reader-friendly strings.
     * 
     * <p>
     * The CompositeImage tag records whether an image is a collection of composite multi-exposures
     * rendered at capture time or composed of a single baseline processing exposure sequence.
     * </p>
     * 
     * @param val
     *        the raw specification identifier
     * @return the corresponding Composite Image description layout string
     */
    private String translateCompositeImage(Object val)
    {
        switch (Taggable.convertToInt(val))
        {
            case 0:
                return "Unknown";

            case 1:
                return "Non-composite image";

            case 2:
                return "General composite image";

            case 3:
                return "Composite image captured when shooting";

            default:
                return Taggable.super.translate(val);
        }
    }
}
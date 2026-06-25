package tif.tagspecs;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

            case EXIF_WHITE_BALANCE:
                return translateWhiteBalance(val);

            case EXIF_SCENE_CAPTURE_TYPE:
                return translateSceneCaptureType(val);

            case EXIF_COLOR_SPACE:
                return translateColorSpace(val);

            case EXIF_FLASH:
                return translateFlash(val);

            case EXIF_COMPOSITE_IMAGE:
                return translateCompositeImage(val);

            case EXIF_METERING_MODE:
                return translateMeteringMode(val);

            case EXIF_SCENE_TYPE:
                return translateSceneType(val);

            case EXIF_APERTURE_VALUE:
                return translateApexAperture(val);

            case EXIF_SHUTTER_SPEED_VALUE:
                return translateApexShutterSpeed(val);

            case EXIF_LENS_SPECIFICATION:
                return translateLensSpecification(val);

            case EXIF_LIGHT_SOURCE:
                return Taggable.translateLightSource(val);

            case EXIF_GAIN_CONTROL:
                return translateGainControl(val);

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
     * {'0','2','3','2'} -> "0232"
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
            byte[] bytes = TagValueFormatter.toByteArray(val);

            if (bytes != null && bytes.length >= 4)
            {
                return new String(bytes, 0, 4, StandardCharsets.US_ASCII);
            }
        }

        return Taggable.super.translate(val);
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

    /**
     * Converts an EXIF ExposureProgram value into a human-readable description.
     *
     * <p>
     * The ExposureProgram tag identifies the camera program used to determine exposure settings
     * when the image was captured.
     * </p>
     *
     * <p>
     * Examples include Manual, Aperture Priority and Shutter Priority modes.
     * </p>
     *
     * @param val
     *        the raw ExposureProgram tag value
     * @return a descriptive exposure programme name, or the default translated value if the value
     *         is not recognised
     */
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

    /**
     * Converts an EXIF FocalPlaneResolutionUnit value into a measurement unit.
     *
     * <p>
     * The FocalPlaneResolutionUnit tag specifies the unit used by the FocalPlaneXResolution and
     * FocalPlaneYResolution tags.
     * </p>
     *
     * @param val
     *        the raw resolution unit value
     * @return the corresponding measurement unit, or the default translated value if the value is
     *         not recognised
     */
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
        int num = Taggable.convertToInt(val);

        switch (num)
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
        int num = Taggable.convertToInt(val);

        switch (num)
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

        return Taggable.super.translate(val);
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

    /**
     * Converts an EXIF Flash tag value into a human-readable description.
     *
     * <p>
     * The EXIF Flash tag stores a coded value that describes whether the flash fired and may also
     * indicate additional information such as automatic flash mode, red-eye reduction, compulsory
     * flash operation, and return-light detection.
     * </p>
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * 0  -> Did not fire
     * 1  -> Fired
     * 25 -> Fired, Auto Mode
     * 89 -> Fired, Auto, Red-eye Reduction
     * </pre>
     *
     * <p>
     * Only the standard EXIF flash values defined by the specification are translated. Unrecognised
     * values are passed to the default formatter.
     * </p>
     *
     * @param val
     *        the raw EXIF Flash tag value
     * @return a human-readable flash description, or the default translated value
     *         if the flash value is not recognized
     */
    private String translateFlash(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
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
     * Translates the CompositeImage enumeration integers into reader-friendly strings.
     */
    private String translateCompositeImage(Object val)
    {
        int num = Taggable.convertToInt(val);
        switch (num)
        {
            case 1:
                return "Not a Composite Image";

            case 2:
                return "General Composite Image";

            case 3:
                return "Composite Image Captured When Shooting";

            default:
                return Taggable.super.translate(val);
        }
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
                return "Multi-segment";

            case 6:
                return "Partial";

            case 255:
                return "other";

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
        int num = Taggable.convertToInt(val);
        return num == 1 ? "Directly photographed" : "Unknown (" + num + ")";
    }

    /**
     * Converts an EXIF ApertureValue (APEX aperture value) into an f-number.
     *
     * <p>
     * The EXIF ApertureValue tag stores the aperture using the APEX (Additive System of
     * Photographic Exposure) scale rather than as a direct f-number. This method converts the
     * stored APEX value into the familiar aperture notation used by photographers.
     * </p>
     *
     * <p>
     * The conversion is performed using the formula:
     * </p>
     *
     * <pre>
     * f-number = 2^(Av / 2)
     * </pre>
     *
     * <p>
     * For example:
     * </p>
     *
     * <pre>
     * Av = 5  -> f/5.7
     * Av = 6  -> f/8.0
     * </pre>
     *
     * <p>
     * For details of the APEX system, refer to Annex C (Informative) of the Exif specification:
     * "CIPA DC-008-Translation-2024 - Exchangeable image file format for digital still cameras:
     * Exif Version 3.0".
     * </p>
     *
     * @param val
     *        the raw EXIF ApertureValue tag value
     * @return the calculated f-number, or the default translated value if the supplied value cannot
     *         be interpreted as a valid APEX aperture value
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

    /**
     * Converts an EXIF ShutterSpeedValue (APEX shutter speed value) into a human-readable exposure
     * time.
     *
     * <p>
     * The EXIF ShutterSpeedValue tag stores shutter speed using the APEX (Additive System of
     * Photographic Exposure) scale rather than as a direct exposure time. This method converts the
     * stored value into a familiar shutter speed representation.
     * </p>
     *
     * <p>
     * The conversion is performed using the formula:
     * </p>
     *
     * <pre>
     * exposure time = 1 / (2^Tv)
     * </pre>
     *
     * <p>
     * Fast shutter speeds are displayed as fractions, whilst longer exposures are displayed in
     * seconds.
     * </p>
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * Tv = 8   -> 1/256
     * Tv = 12  -> 1/4096
     * Tv = -1  -> 2 s
     * </pre>
     *
     * <p>
     * For details of the APEX system, refer to Annex C (Informative) of the Exif specification:
     * "CIPA DC-008-Translation-2024 - Exchangeable image file format for digital still cameras:
     * Exif Version 3.0".
     * </p>
     *
     * @param val
     *        the raw EXIF ShutterSpeedValue tag value
     * @return a formatted exposure time, or the default translated value if the supplied value
     *         cannot be interpreted as a valid APEX shutter speed value
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

    /**
     * Converts an EXIF LensSpecification value into a human-readable lens description.
     *
     * <p>
     * The EXIF LensSpecification tag is expected to contain an array of four {@link RationalNumber}
     * values representing:
     * </p>
     *
     * <ol>
     * <li>Minimum focal length</li>
     * <li>Maximum focal length</li>
     * <li>Minimum aperture (f-number)</li>
     * <li>Maximum aperture (f-number)</li>
     * </ol>
     *
     * <p>
     * For example, a value describing a 24-70 mm f/2.8 zoom lens is formatted as:
     * </p>
     *
     * <pre>
     * 24-70mm f/2.8
     * </pre>
     *
     * @param val
     *        the raw LensSpecification tag value
     * @return a formatted lens description, or the original string value if the value cannot be
     *         interpreted as a LensSpecification
     */
    private String translateLensSpecification(Object val)
    {
        // The EXIF specification defines LensSpecification as four rational values
        if (val instanceof RationalNumber[] && ((RationalNumber[]) val).length == 4)
        {
            RationalNumber[] arr = (RationalNumber[]) val;

            // Extract focal length and aperture values
            double minFocal = arr[0] != null ? arr[0].doubleValue() : 0.0;
            double maxFocal = arr[1] != null ? arr[1].doubleValue() : 0.0;
            double minFStop = arr[2] != null ? arr[2].doubleValue() : 0.0;
            double maxFStop = arr[3] != null ? arr[3].doubleValue() : 0.0;

            if (!Double.isFinite(minFocal) || !Double.isFinite(maxFocal) || !Double.isFinite(minFStop) || !Double.isFinite(maxFStop))
            {
                return Taggable.super.translate(val);
            }

            StringBuilder sb = new StringBuilder();
            DecimalFormat lensFormat = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));

            // 1. Format Focal Length Range (e.g., "24-70mm" or "50mm")
            if (Double.compare(minFocal, maxFocal) == 0)
            {
                sb.append(lensFormat.format(minFocal)).append("mm");
            }

            else
            {
                sb.append(lensFormat.format(minFocal)).append("-");
                sb.append(lensFormat.format(maxFocal)).append("mm");
            }

            sb.append(" ");

            // Format aperture (for example, "f/2.8" or "f/4-5.6")
            if (Double.compare(minFStop, maxFStop) == 0)
            {
                sb.append("f/").append(lensFormat.format(minFStop));
            }

            else
            {
                sb.append("f/").append(lensFormat.format(minFStop)).append("-").append(lensFormat.format(maxFStop));
            }

            return sb.toString();
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
        int num = Taggable.convertToInt(val);

        switch (num)
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
}
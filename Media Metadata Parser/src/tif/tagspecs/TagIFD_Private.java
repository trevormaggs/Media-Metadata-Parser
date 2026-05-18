package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagIFD_Private implements Taggable
{
    /* --- Non-Standard Application Tags --- */
    IFD_PROCESSING_SOFTWARE(0x000B, "Processing Software"),
    IFD_RATING(0x4746, "Rating"),
    IFD_RATING_PERCENT(0x4749, "Rating Percent"),
    IFD_PHOTOSHOP_SETTINGS(0x8649, "Photoshop Tags", TagHint.HINT_BYTE_STREAM),
    IFD_ICC_PROFILE(0x8773, "ICC Profile Tags", TagHint.HINT_BYTE_STREAM),
    IFD_IMAGE_SOURCE_DATA(0x935C, "Image Source Data"),
    IFD_PRINT_IM(0xC4A5, "Print IM"),
    IFD_SEAL(0xCEA1, "SEAL"),

    /* --- Mapping & GeoTIFF Extensions --- */
    IFD_PIXEL_SCALE(0x830E, "Pixel Scale"),
    IFD_INTERGRAPH_MATRIX(0x8480, "IntergraphMatrix"),
    IFD_MODEL_TIE_POINT(0x8482, "Model Tie Point"),
    IFD_SEMINFO(0x8546, "SEM Info"),
    IFD_MODEL_TRANSFORM(0x85D8, "Model Transform"),
    IFD_GEO_TIFF_DIRECTORY(0x87AF, "Geo Tiff Directory", TagHint.HINT_SHORT),
    IFD_GEO_TIFF_DOUBLE_PARAMS(0x87B0, "Geo Tiff Double Params"),
    IFD_GEO_TIFF_ASCII_PARAMS(0x87B1, "Geo Tiff Ascii Params"),
    IFD_GDAL_METADATA(0xA480, "GDAL Metadata"),
    IFD_GDAL_NO_DATA(0xA481, "GDAL NoData"),

    /* --- Adobe DNG / Raw Sensor Calibration Extensions --- */
    IFD_DNG_VERSION(0xC612, "DNG Version", TagHint.HINT_BYTE),
    IFD_DNG_BACKWARD_VERSION(0xC613, "DNG Backward Version"),
    IFD_UNIQUE_CAMERA_MODEL(0xC614, "Unique Camera Model"),
    IFD_LOCALIZED_CAMERA_MODEL(0xC615, "Localized Camera Model"),
    IFD_BLACK_LEVEL(0xC61A, "Black Level"),
    IFD_WHITE_LEVEL(0xC61D, "White Level"),
    IFD_COLOR_MATRIX1(0xC621, "Color Matrix1", TagHint.HINT_RATIONAL),
    IFD_COLOR_MATRIX2(0xC622, "Color Matrix2", TagHint.HINT_RATIONAL),
    IFD_CAMERA_CALIBRATION1(0xC623, "Camera Calibration1"),
    IFD_CAMERA_CALIBRATION2(0xC624, "Camera Calibration2"),
    IFD_REDUCTION_MATRIX1(0xC625, "Reduction Matrix1"),
    IFD_REDUCTION_MATRIX2(0xC626, "Reduction Matrix2"),
    IFD_ANALOG_BALANCE(0xC627, "Analog Balance"),
    IFD_AS_SHOT_NEUTRAL(0xC628, "As Shot Neutral", TagHint.HINT_RATIONAL),
    IFD_AS_SHOT_WHITE_XY(0xC629, "As Shot White XY"),
    IFD_BASELINE_EXPOSURE(0xC62A, "Baseline Exposure"),
    IFD_BASELINE_NOISE(0xC62B, "Baseline Noise"),
    IFD_BASELINE_SHARPNESS(0xC62C, "Baseline Sharpness"),
    IFD_LINEAR_RESPONSE_LIMIT(0xC62E, "Linear Response Limit"),
    IFD_CAMERA_SERIAL_NUMBER(0xC62F, "Camera Serial Number"),
    IFD_DNG_LENS_INFO(0xC630, "DNG Lens Info"),
    IFD_SHADOW_SCALE(0xC633, "Shadow Scale"),
    IFD_SR2_PRIVATE(0xC634, "SR2 Private"),
    IFD_MAKER_NOTE_SAFETY(0xC635, "Maker Note Safety"),
    IFD_CALIBRATION_ILLUMINANT1(0xC65A, "Calibration Illuminant1"),
    IFD_CALIBRATION_ILLUMINANT2(0xC65B, "Calibration Illuminant2"),
    IFD_RAW_DATA_UNIQUE_ID(0xC65D, "Raw Data Unique ID"),
    IFD_ORIGINAL_RAW_FILE_NAME(0xC68B, "Original Raw File Name"),
    IFD_ORIGINAL_RAW_FILE_DATA(0xC68C, "Original Raw File Data"),
    IFD_AS_SHOT_ICCPROFILE(0xC68F, "As Shot ICC Profile", TagHint.HINT_BYTE_STREAM),
    IFD_AS_SHOT_PRE_PROFILE_MATRIX(0xC690, "As Shot Pre Profile Matrix"),
    IFD_CURRENT_ICCPROFILE(0xC691, "Current ICC Profile", TagHint.HINT_BYTE_STREAM),
    IFD_CURRENT_PRE_PROFILE_MATRIX(0xC692, "Current Pre Profile Matrix"),
    IFD_COLORIMETRIC_REFERENCE(0xC6BF, "Colorimetric Reference"),
    IFD_SRAW_TYPE(0xC6C5, "SRaw Type"),
    IFD_PANASONIC_TITLE(0xC6D2, "Panasonic Title"),
    IFD_PANASONIC_TITLE2(0xC6D3, "Panasonic Title2"),
    IFD_CAMERA_CALIBRATION_SIG(0xC6F3, "Camera Calibration Sig"),
    IFD_PROFILE_CALIBRATION_SIG(0xC6F4, "Profile Calibration Sig"),
    IFD_PROFILE_IFD(0xC6F5, "Profile IFD"),
    IFD_AS_SHOT_PROFILE_NAME(0xC6F6, "As Shot Profile Name"),
    IFD_PROFILE_NAME(0xC6F8, "Profile Name"),
    IFD_PROFILE_HUE_SAT_MAP_DIMS(0xC6F9, "Profile Hue Sat Map Dims"),
    IFD_PROFILE_HUE_SAT_MAP_DATA1(0xC6FA, "Profile Hue Sat Map Data1"),
    IFD_PROFILE_HUE_SAT_MAP_DATA2(0xC6FB, "Profile Hue Sat Map Data2"),
    IFD_PROFILE_TONE_CURVE(0xC6FC, "Profile Tone Curve"),
    IFD_PROFILE_EMBED_POLICY(0xC6FD, "Profile Embed Policy"),
    IFD_PROFILE_COPYRIGHT(0xC6FE, "Profile Copyright"),
    IFD_FORWARD_MATRIX1(0xC714, "Forward Matrix1", TagHint.HINT_RATIONAL),
    IFD_FORWARD_MATRIX2(0xC715, "Forward Matrix2", TagHint.HINT_RATIONAL),
    IFD_PREVIEW_APPLICATION_NAME(0xC716, "Preview Application Name"),
    IFD_PREVIEW_APPLICATION_VERSION(0xC717, "Preview Application Version"),
    IFD_PREVIEW_SETTINGS_NAME(0xC718, "Preview Settings Name"),
    IFD_PREVIEW_SETTINGS_DIGEST(0xC719, "Preview Settings Digest"),
    IFD_PREVIEW_COLOR_SPACE(0xC71A, "Preview Color Space"),
    IFD_PREVIEW_DATE_TIME(0xC71B, "Preview Date Time", TagHint.HINT_DATE),
    IFD_RAW_IMAGE_DIGEST(0xC71C, "Raw Image Digest"),
    IFD_ORIGINAL_RAW_FILE_DIGEST(0xC71D, "Original Raw File Digest"),
    IFD_PROFILE_LOOK_TABLE_DIMS(0xC725, "Profile Look Table Dims"),
    IFD_PROFILE_LOOK_TABLE_DATA(0xC726, "Profile Look Table Data"),
    IFD_TIME_CODES(0xC763, "Time Codes"),
    IFD_FRAME_RATE(0xC764, "Frame Rate"),
    IFD_TSTOP(0xC772, "TStop"),
    IFD_REEL_NAME(0xC789, "Reel Name"),
    IFD_ORIGINAL_DEFAULT_FINAL_SIZE(0xC791, "Original Default Final Size"),
    IFD_ORIGINAL_BEST_QUALITY_SIZE(0xC792, "Original Best Quality Size"),
    IFD_ORIGINAL_DEFAULT_CROP_SIZE(0xC793, "Original Default Crop Size"),
    IFD_CAMERA_LABEL(0xC7A1, "Camera Label"),
    IFD_PROFILE_HUE_SAT_MAP_ENCODING(0xC7A3, "Profile Hue Sat Map Encoding"),
    IFD_PROFILE_LOOK_TABLE_ENCODING(0xC7A4, "Profile Look Table Encoding"),
    IFD_BASELINE_EXPOSURE_OFFSET(0xC7A5, "Baseline Exposure Offset"),
    IFD_DEFAULT_BLACK_RENDER(0xC7A6, "Default Black Render"),
    IFD_NEW_RAW_IMAGE_DIGEST(0xC7A7, "New Raw Image Digest"),
    IFD_RAW_TO_PREVIEW_GAIN(0xC7A8, "Raw To Preview Gain"),
    IFD_DEPTH_FORMAT(0xC7E9, "Depth Format"),
    IFD_DEPTH_NEAR(0xC7EA, "Depth Near"),
    IFD_DEPTH_FAR(0xC7EB, "Depth Far"),
    IFD_DEPTH_UNITS(0xC7EC, "Depth Units"),
    IFD_DEPTH_MEASURE_TYPE(0xC7ED, "Depth Measure Type"),
    IFD_ENHANCE_PARAMS(0xC7EE, "Enhance Params"),
    IFD_CALIBRATION_ILLUMINANT3(0xCD31, "Calibration Illuminant3"),
    IFD_CAMERA_CALIBRATION3(0xCD32, "Camera Calibration3"),
    IFD_COLOR_MATRIX3(0xCD33, "Color Matrix3", TagHint.HINT_RATIONAL),
    IFD_FORWARD_MATRIX3(0xCD34, "Forward Matrix3", TagHint.HINT_RATIONAL),
    IFD_ILLUMINANT_DATA1(0xCD35, "Illuminant Data1"),
    IFD_ILLUMINANT_DATA2(0xCD36, "Illuminant Data2"),
    IFD_ILLUMINANT_DATA3(0xCD37, "Illuminant Data3"),
    IFD_PROFILE_HUE_SAT_MAP_DATA3(0xCD39, "Profile Hue Sat Map Data3"),
    IFD_REDUCTION_MATRIX3(0xCD3A, "Reduction Matrix3"),
    IFD_RGB_TABLES(0xCD3F, "RGB Tables"),
    IFD_PROFILE_GAIN_TABLE_MAP2(0xCD40, "Profile Gain Table Map2"),
    IFD_IMAGE_SEQUENCE_INFO(0xCD44, "Image Sequence Info"),
    IFD_IMAGE_STATS(0xCD46, "Image Stats"),
    IFD_PROFILE_DYNAMIC_RANGE(0xCD47, "Profile Dynamic Range"),
    IFD_PROFILE_GROUP_NAME(0xCD48, "Profile Group Name"),
    IFD_JXL_DISTANCE(0xCD49, "JXL Distance"),
    IFD_JXL_EFFORT(0xCD4A, "JXL Effort"),
    IFD_JXL_DECODE_SPEED(0xCD4B, "JXL Decode Speed");

    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagIFD_Private(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    private TagIFD_Private(int id, String desc, TagHint clue)
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
        return DirectoryIdentifier.IFD_ROOT_DIRECTORY;
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
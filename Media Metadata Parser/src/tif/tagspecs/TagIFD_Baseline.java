package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagIFD_Baseline implements Taggable
{
    IFD_NEW_SUBFILE_TYPE(0x00FE, "New Subfile Type"),
    IFD_SUBFILE_TYPE(0x00FF, "Subfile Type"),
    IFD_IMAGE_WIDTH(0x0100, "Image Width"),
    IFD_IMAGE_LENGTH(0x0101, "Image Length"),
    IFD_BITS_PER_SAMPLE(0x0102, "Bits Per Sample"),
    IFD_COMPRESSION(0x0103, "Compression"),
    IFD_PHOTOMETRIC_INTERPRETATION(0x0106, "Photometric Interpretation"),
    IFD_THRESHOLDING(0x0107, "Thresholding"),
    IFD_CELL_WIDTH(0x0108, "Cell Width"),
    IFD_CELL_LENGTH(0x0109, "Cell Length"),
    IFD_FILL_ORDER(0x010A, "Fill Order"),
    IFD_DOCUMENT_NAME(0x010D, "Document Name"),
    IFD_IMAGE_DESCRIPTION(0x010E, "Image Description"),
    IFD_MAKE(0x010F, "Make"),
    IFD_MODEL(0x0110, "Model"),
    IFD_STRIP_OFFSETS(0x0111, "Strip Offsets", TagHint.HINT_BYTE_STREAM),
    IFD_ORIENTATION(0x0112, "Orientation", TagHint.HINT_SHORT),
    IFD_SAMPLES_PER_PIXEL(0x0115, "Samples Per Pixel"),
    IFD_ROWS_PER_STRIP(0x0116, "Rows Per Strip"),
    IFD_STRIP_BYTE_COUNTS(0x0117, "Strip Byte Counts", TagHint.HINT_BYTE_STREAM),
    IFD_MIN_SAMPLE_VALUE(0x0118, "Min Sample Value"),
    IFD_MAX_SAMPLE_VALUE(0x0119, "Max Sample Value"),
    IFD_XRESOLUTION(0x011A, "XResolution", TagHint.HINT_RATIONAL),
    IFD_YRESOLUTION(0x011B, "YResolution", TagHint.HINT_RATIONAL),
    IFD_PLANAR_CONFIGURATION(0x011C, "Planar Configuration", TagHint.HINT_SHORT),
    IFD_PAGE_NAME(0x011D, "Page Name"),
    IFD_XPOSITION(0x011E, "XPosition"),
    IFD_YPOSITION(0x011F, "YPosition"),
    IFD_FREE_OFFSETS(0x0120, "Free Offsets"),
    IFD_FREE_BYTE_COUNTS(0x0121, "Free Byte Counts"),
    IFD_GRAY_RESPONSE_UNIT(0x0122, "Gray Response Unit"),
    IFD_GRAY_RESPONSE_CURVE(0x0123, "Gray Response Curve"),
    IFD_T4_OPTIONS(0x0124, "T4Options"),
    IFD_T6_OPTIONS(0x0125, "T6Options"),
    IFD_RESOLUTION_UNIT(0x0128, "Resolution Unit"),
    IFD_PAGE_NUMBER(0x0129, "Page Number"),
    IFD_TRANSFER_FUNCTION(0x012D, "Transfer Function"),
    IFD_SOFTWARE(0x0131, "Software"),
    IFD_DATE_TIME(0x0132, "Date Time", TagHint.HINT_DATE),
    IFD_ARTIST(0x013B, "Artist"),
    IFD_HOST_COMPUTER(0x013C, "Host Computer"),
    IFD_PREDICTOR(0x013D, "Predictor"),
    IFD_WHITE_POINT(0x013E, "White Point"),
    IFD_PRIMARY_CHROMATICITIES(0x013F, "Primary Chromaticities"),
    IFD_COLOR_MAP(0x0140, "Color Map", TagHint.HINT_SHORT),
    IFD_HALFTONE_HINTS(0x0141, "Halftone Hints"),
    IFD_TILE_WIDTH(0x0142, "Tile Width"),
    IFD_TILE_LENGTH(0x0143, "Tile Length"),
    IFD_TILE_OFFSETS(0x0144, "Tile Offsets"),
    IFD_TILE_BYTE_COUNTS(0x0145, "Tile Byte Counts"),
    IFD_INK_SET(0x014C, "Ink Set"),
    IFD_INK_NAMES(0x014D, "Ink Names"),
    IFD_NUMBER_OF_INKS(0x014E, "Number Of Inks"),
    IFD_DOT_RANGE(0x0150, "Dot Range", TagHint.HINT_SHORT),
    IFD_TARGET_PRINTER(0x0151, "Target Printer"),
    IFD_EXTRA_SAMPLES(0x0152, "Extra Samples"),
    IFD_SAMPLE_FORMAT(0x0153, "Sample Format"),
    IFD_SMIN_SAMPLE_VALUE(0x0154, "SMin Sample Value"),
    IFD_SMAX_SAMPLE_VALUE(0x0155, "SMax Sample Value"),
    IFD_TRANSFER_RANGE(0x0156, "Transfer Range"),
    IFD_JPEG_PROC(0x0200, "JPEG Proc"),
    IFD_JPEG_INTERCHANGE_FORMAT(0x0201, "JPEG Interchange Format"),
    IFD_JPEG_INTERCHANGE_FORMAT_LENGTH(0x0202, "JPEG Interchange Format Length"),
    IFD_JPEG_RESTART_INTERVAL(0x0203, "JPEG Restart Interval"),
    IFD_JPEG_LOSSLESS_PREDICTORS(0x0205, "JPEG Lossless Predictors"),
    IFD_JPEG_POINT_TRANSFORMS(0x0206, "JPEG Point Transforms"),
    IFD_JPEG_QTABLES(0x0207, "JPEG QTables"),
    IFD_JPEG_DC_TABLES(0x0208, "JPEG DC Tables"),
    IFD_JPEG_AC_TABLES(0x0209, "JPEG AC Tables"),
    IFD_YCBCR_COEFFICIENTS(0x0211, "YCbCr Coefficients"),
    IFD_YCBCR_SUB_SAMPLING(0x0212, "YCbCr Sub Sampling", TagHint.HINT_SHORT),
    IFD_YCB_CR_POSITIONING(0x0213, "YCbCr Positioning"),
    IFD_REFERENCE_BLACK_WHITE(0x0214, "Reference Black White"),
    IFD_COPYRIGHT(0x8298, "Copyright");

    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagIFD_Baseline(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    private TagIFD_Baseline(int id, String desc, TagHint clue)
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
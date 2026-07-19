package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Private, vendor-specific, and industry-standard TIFF tags defined within the
 * {@link DirectoryIdentifier#IFD_ROOT_DIRECTORY} scope.
 *
 * @author Trevor Maggs
 * @version 2.2
 * @since 02 July 2026
 */
public enum TagIFD_Private implements Taggable
{
    /* --- 0x0000 - 0x84FF Range --- */
    IFD_PROCESSING_SOFTWARE(0x000B, "Processing Software", TagHint.HINT_STRING),
    IFD_RATING(0x4746, "Rating", TagHint.HINT_SHORT),
    IFD_RATING_PERCENT(0x4749, "Rating Percent", TagHint.HINT_SHORT),
    IFD_PIXEL_SCALE(0x830E, "Pixel Scale", TagHint.HINT_DOUBLE),
    IFD_INTERGRAPH_MATRIX(0x8480, "Intergraph Matrix", TagHint.HINT_DOUBLE),
    IFD_MODEL_TIE_POINT(0x8482, "Model Tie Point", TagHint.HINT_DOUBLE),

    /* --- 0x8500 - 0x8FFF Range --- */
    IFD_SEMINFO(0x8546, "SEM Info"),
    IFD_MODEL_TRANSFORM(0x85D8, "Model Transform", TagHint.HINT_DOUBLE),
    IFD_PHOTOSHOP_SETTINGS(0x8649, "Photoshop Tags", TagHint.HINT_BYTE_STREAM),
    IFD_ICC_PROFILE(0x8773, "ICC Profile Tags", TagHint.HINT_BYTE_STREAM),
    IFD_GEO_TIFF_DIRECTORY(0x87AF, "GeoKey Directory", TagHint.HINT_SHORT),
    IFD_GEO_TIFF_DOUBLE_PARAMS(0x87B0, "GeoDouble Params", TagHint.HINT_DOUBLE),
    IFD_GEO_TIFF_ASCII_PARAMS(0x87B1, "GeoAscii Params", TagHint.HINT_STRING),

    /* --- 0x9000 - 0xFFFF Range --- */
    IFD_IMAGE_SOURCE_DATA(0x935C, "Image Source Data", TagHint.HINT_BYTE_STREAM),
    IFD_GDAL_METADATA(0xA480, "GDAL Metadata", TagHint.HINT_STRING),
    IFD_GDAL_NO_DATA(0xA481, "GDAL NoData", TagHint.HINT_STRING),
    IFD_PRINT_IM(0xC4A5, "Print IM", TagHint.HINT_BYTE_STREAM),
    IFD_SEAL(0xCEA1, "SEAL");

    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagIFD_Private(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    private TagIFD_Private(int id, String desc, TagHint hint)
    {
        this.numID = id;
        this.desc = desc;
        this.hint = hint;
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
        return DirectoryIdentifier.IFD_ROOT_DIRECTORY;
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
        if (this == IFD_PHOTOSHOP_SETTINGS)
        {
            /*
             * Return an empty string to avoid mixing Photoshop properties with the
             * normal [IFD0] output. The properties are displayed separately by
             * PhotoshopManager.
             */
            return "";
        }

        return Taggable.super.translate(val);
    }
}
package tif.tagspecs;

import java.nio.charset.StandardCharsets;
import common.ByteValueConverter;
import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Defines TIFF extension tags specified by TIFF 6.0 and commonly encountered platform-specific
 * extensions.
 *
 * <p>
 * These tags supplement the baseline TIFF tag set with support for advanced document properties,
 * embedded metadata (such as XMP), fax-related information, image layering, and Windows XP metadata
 * fields. All tags in this enum belong to the root IFD.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 18 June 2026
 */
public enum TagIFD_Extension implements Taggable
{
    IFD_BAD_FAX_LINES(0x0146, "Bad Fax Lines"),
    IFD_CLEAN_FAX_DATA(0x0147, "Clean Fax Data", TagHint.HINT_SHORT),
    IFD_CONSECUTIVE_BAD_FAX_LINES(0x0148, "Consecutive Bad Fax Lines"),
    IFD_CLIP_PATH(0x0157, "Clip Path", TagHint.HINT_BYTE),
    IFD_XCLIP_PATH_UNITS(0x0158, "XClip Path Units"),
    IFD_YCLIP_PATH_UNITS(0x0159, "YClip Path Units"),
    IFD_INDEXED(0x015A, "Indexed"),
    IFD_JPEG_TABLES(0x015B, "JPEG Tables", TagHint.HINT_BYTE_STREAM),
    IFD_OPIPROXY(0x015F, "OPI Proxy"),
    IFD_GLOBAL_PARAMETERS_IFD(0x0190, "Global Parameters IFD"),
    IFD_PROFILE_TYPE(0x0191, "Profile Type"),
    IFD_FAX_PROFILE(0x0192, "Fax Profile"),
    IFD_CODING_METHODS(0x0193, "Coding Methods"),
    IFD_VERSION_YEAR(0x0194, "Version Year"),
    IFD_MODE_NUMBER(0x0195, "Mode Number"),
    IFD_DECODE(0x01B1, "Decode"),
    IFD_DEFAULT_IMAGE_COLOR(0x01B2, "Default Image Color"),
    IFD_STRIP_ROW_COUNTS(0x022F, "Strip Row Counts"),
    //IFD_XML_PACKET(0x02BC, "XMP Metadata", TagHint.HINT_STRING),
    IFD_XML_PACKET(0x02BC, "XMP Metadata", TagHint.HINT_BYTE_STREAM),
    IFD_IMAGE_ID(0x800D, "Image ID"),
    IFD_IMAGE_LAYER(0x87AC, "Image Layer"),
    IFD_PADDING(0xEA1C, "Microsoft Padding", TagHint.HINT_BYTE_STREAM),

    /* --- Windows XP Metadata Tags --- */
    IFD_XP_TITLE(0x9C9B, "Windows XP Title", TagHint.HINT_UCS2),
    IFD_XP_COMMENT(0x9C9C, "Windows XP Comment", TagHint.HINT_UCS2),
    IFD_XP_AUTHOR(0x9C9D, "Windows XP Author", TagHint.HINT_UCS2),
    IFD_XP_KEYWORDS(0x9C9E, "Windows XP Keywords", TagHint.HINT_UCS2),
    IFD_XP_SUBJECT(0x9C9F, "Windows XP Subject", TagHint.HINT_UCS2);

    private final int numID;
    private final TagHint hint;
    private final String desc;

    /**
     * Constructs an extension TIFF tag using the default hint.
     *
     * @param id
     *        the TIFF tag identifier
     * @param desc
     *        the tag description
     */
    private TagIFD_Extension(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    /**
     * Constructs an extension TIFF tag with an explicit value hint.
     *
     * @param id
     *        the TIFF tag identifier
     * @param desc
     *        the tag description
     * @param clue
     *        the value interpretation hint
     */
    private TagIFD_Extension(int id, String desc, TagHint clue)
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
     * Converts known TIFF extension tag values into human-readable text. Tags without custom
     * translations use the default {@link Taggable#translate(Object)} implementation.
     *
     * @param val
     *        the tag value
     *
     * @return a translated description of the value
     */
    @Override
    public String translate(Object val)
    {
        switch (this)
        {
            case IFD_CLEAN_FAX_DATA:
                return translateCleanFax(val);

            case IFD_INDEXED:
                return translateIndexed(val);

            case IFD_XP_TITLE:
            case IFD_XP_SUBJECT:
            case IFD_XP_COMMENT:
            case IFD_XP_KEYWORDS:
            case IFD_XP_AUTHOR:
                return translateXPString(val);

            case IFD_FAX_PROFILE:
                return translateFaxProfile(val);

            default:
            break;
        }

        return Taggable.super.translate(val);
    }

    /**
     * Translates CleanFaxData values into descriptive status labels.
     *
     * @param num
     *        the raw CleanFaxData value
     *
     * @return the corresponding status description
     */
    private String translateCleanFax(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0:
                return "Clean";

            case 1:
                return "Regenerated";

            case 2:
                return "Unclean";

            default:
                return Taggable.super.translate(val);
        }
    }

    /**
     * Translates Indexed values into palette indexing descriptions.
     *
     * @param num
     *        the raw Indexed value
     *
     * @return "Indexed" if the image uses a color lookup table, otherwise "Not indexed"
     */
    private String translateIndexed(Object val)
    {
        int num = Taggable.convertToInt(val);

        return (num == 1) ? "Indexed" : "Not indexed";
    }

    /**
     * Decodes a Windows XP metadata string stored as UTF-16LE.
     *
     * <p>
     * TIFF XP tags store text as UCS-2/UTF-16LE byte sequences, typically terminated by a null
     * character. This method accepts either a {@code byte[]} or {@code int[]} representation and
     * converts the value into a Java {@link String}.
     * </p>
     *
     * @param val
     *        the raw tag value
     *
     * @return the decoded text, or the default translation if the value cannot be interpreted as a
     *         Windows XP string
     */
    private String translateXPString(Object val)
    {
        byte[] bytes;

        if (val instanceof byte[])
        {
            bytes = (byte[]) val;
        }

        else if (val instanceof int[])
        {
            bytes = ByteValueConverter.castToByteArray((int[]) val);
        }

        else
        {
            return Taggable.super.translate(val);
        }

        int length = bytes.length;

        /* UTF-16 requires an even number of bytes. */
        length &= ~1;

        String decoded = new String(bytes, 0, length, StandardCharsets.UTF_16LE);
        int nullIdx = decoded.indexOf('\0');

        if (nullIdx != -1)
        {
            decoded = decoded.substring(0, nullIdx);
        }

        return decoded.trim();
    }

    /**
     * Translates FaxProfile values into their standardized ITU-T profile names.
     *
     * @param val
     *        the raw fax profile data container
     * @return the profile name string
     */
    private String translateFaxProfile(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (num)
        {
            case 0:
                return "Unknown (Generic)";

            case 1:
                return "Minimal B&W lossless (Profile S)";

            case 2:
                return "Extended B&W lossless (Profile F)";

            case 3:
                return "Lossless progressive B&W (Profile J)";

            case 4:
                return "Lossless Color (Profile C)";

            case 5:
                return "Mixed Raster Content (Profile M)";

            default:
                return Taggable.super.translate(val);
        }
    }
}
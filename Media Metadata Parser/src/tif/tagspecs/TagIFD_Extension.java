package tif.tagspecs;

import java.nio.charset.StandardCharsets;
import common.ByteValueConverter;
import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Extension TIFF tags defined by the TIFF 6.0 specification and common platform extensions.
 * 
 * <p>
 * These tags build upon the baseline architecture to handle advanced document properties, embedded
 * metadata packets (such as XMP), and legacy operating system descriptors. All extension elements
 * map to the root directory structure to ensure universal availability across sequential image
 * streams.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.1
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
    IFD_XML_PACKET(0x02BC, "XMP Metadata", TagHint.HINT_STRING),
    IFD_IMAGE_ID(0x800D, "Image ID"),
    IFD_IMAGE_LAYER(0x87AC, "Image Layer"),
    IFD_PADDING(0xEA1C, "Microsoft Padding", TagHint.HINT_BYTE_STREAM),

    /* --- Windows Legacy Custom Tags --- */
    IFD_XP_TITLE(0x9C9B, "Windows XP Title", TagHint.HINT_UCS2),
    IFD_XP_COMMENT(0x9C9C, "Windows XP Comment", TagHint.HINT_UCS2),
    IFD_XP_AUTHOR(0x9C9D, "Windows XP Author", TagHint.HINT_UCS2),
    IFD_XP_KEYWORDS(0x9C9E, "Windows XP Keywords", TagHint.HINT_UCS2),
    IFD_XP_SUBJECT(0x9C9F, "Windows XP Subject", TagHint.HINT_UCS2);

    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagIFD_Extension(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    private TagIFD_Extension(int id, String desc, TagHint clue)
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

    @Override
    public String translate(Object val)
    {
        int num = Taggable.convertToInt(val);

        switch (this)
        {
            case IFD_CLEAN_FAX_DATA:
                return translateCleanFax(num);

            case IFD_INDEXED:
                return translateIndexed(num);

            case IFD_XP_TITLE:
            case IFD_XP_SUBJECT:
            case IFD_XP_COMMENT:
            case IFD_XP_KEYWORDS:
            case IFD_XP_AUTHOR:
                return translateXPString(val);

            default:
            break;
        }

        return Taggable.super.translate(val);
    }

    private String translateCleanFax(int num)
    {
        switch (num)
        {
            case 0:
                return "Clean";

            case 1:
                return "Regenerated";

            case 2:
                return "Unclean";

            default:
                return Taggable.super.translate(num);
        }
    }

    private String translateIndexed(int num)
    {
        // Preserving your exact logic, falling back to interface trimming if not explicitly 1
        return (num == 1) ? "Indexed" : "Not indexed";
    }

    private String translateXPString(Object val)
    {
        byte[] bytes;

        if (val instanceof byte[])
        {
            bytes = (byte[]) val;
        }
        else if (val instanceof int[])
        {
            // Leverages your custom data packet translator utility
            bytes = ByteValueConverter.castToByteArray((int[]) val);
        }
        else
        {
            return Taggable.super.translate(val);
        }

        int length = bytes.length;
        if (length % 2 != 0)
        {
            length--; // Keep alignment boundaries safely evened out
        }

        // Decode using your standard explicit character set identifier
        String decoded = new String(bytes, 0, length, StandardCharsets.UTF_16LE);
        int nullIdx = decoded.indexOf('\0');

        if (nullIdx != -1)
        {
            decoded = decoded.substring(0, nullIdx);
        }

        return decoded.trim();
    }
}
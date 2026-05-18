package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagIFD_Extension implements Taggable
{
    IFD_BAD_FAX_LINES(0x0146, "Bad Fax Lines"),
    IFD_CLEAN_FAX_DATA(0x0147, "Clean Fax Data", TagHint.HINT_SHORT),
    IFD_CONSECUTIVE_BAD_FAX_LINES(0x0148, "Consecutive Bad Fax Lines"),
    IFD_CLIP_PATH(0x0157, "Clip Path", TagHint.HINT_BYTE_STREAM),
    IFD_XCLIP_PATH_UNITS(0x0158, "XClip Path Units"),
    IFD_YCLIP_PATH_UNITS(0x0159, "YClip Path Units"),
    IFD_INDEXED(0x015A, "Indexed"),
    IFD_JPEG_TABLES(0x015B, "JPEG Tables"),
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
    IFD_XML_PACKET(0x02BC, "XMP Metadata", TagHint.HINT_BYTE_STREAM),
    IFD_IMAGE_ID(0x800D, "Image ID"),
    IFD_IMAGE_LAYER(0x87AC, "Image Layer"),

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
}
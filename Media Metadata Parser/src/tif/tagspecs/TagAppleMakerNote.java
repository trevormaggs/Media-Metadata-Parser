package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Apple-specific MakerNote metadata tags.
 */
public enum TagAppleMakerNote implements TagExif_MakerNote
{
    MAKER_NOTE_VERSION(0x0001, "MakerNote Version", TagHint.HINT_SHORT),
    RUN_TIME_FLAGS(0x0003, "Run Time Flags", TagHint.HINT_INTEGER),
    RUN_TIME_VALUE(0x0004, "Run Time Value", TagHint.HINT_INTEGER),
    COLOR_TEMPERATURE(0x0005, "Color Temperature", TagHint.HINT_INTEGER),
    CAMERA_TYPE(0x0006, "Camera Type", TagHint.HINT_STRING);

    private final int numID;
    private final String desc;
    private final TagHint hint;

    TagAppleMakerNote(int id, String desc, TagHint hint)
    {
        this.numID = id;
        this.desc = desc;
        this.hint = hint;
    }

    @Override
    public int getNumberID()
    {
        return numID;
    }

    @Override
    public String getDescription()
    {
        return desc;
    }

    @Override
    public TagHint getHint()
    {
        return hint;
    }

    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return DirectoryIdentifier.IFD_DIRECTORY_UNKNOWN;
    }

    @Override
    public String getCameraMake()
    {
        return "Apple";
    }
}
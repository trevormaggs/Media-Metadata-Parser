package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Exif Interoperability sub-directory metadata tags identified within the
 * {@link DirectoryIdentifier#EXIF_INTEROP_DIRECTORY} scope.
 * 
 * @author Trevor Maggs
 * @version 1.1
 */
public enum TagExif_Interop implements Taggable
{
    INTEROP_INDEX(0x0001, "Interop Index", TagHint.HINT_STRING),
    INTEROP_VERSION(0x0002, "Interop Version", TagHint.HINT_VERSION),
    INTEROP_RELATED_IMAGE_FILE_FORMAT(0x1000, "Related Image File Format", TagHint.HINT_STRING),
    INTEROP_RELATED_IMAGE_WIDTH(0x1001, "Related Image Width", TagHint.HINT_SHORT),
    INTEROP_RELATED_IMAGE_HEIGHT(0x1002, "Related Image Height", TagHint.HINT_SHORT);

    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagExif_Interop(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    private TagExif_Interop(int id, String desc, TagHint clue)
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
        return DirectoryIdentifier.EXIF_INTEROP_DIRECTORY;
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
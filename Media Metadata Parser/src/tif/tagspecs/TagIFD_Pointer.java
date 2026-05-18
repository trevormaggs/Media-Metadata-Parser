package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagIFD_Pointer implements Taggable
{
    IFD_SUBIFD_POINTER(0x014A, "SubIFD DataOffset", DirectoryIdentifier.IFD_SUBIFD_DIRECTORY),
    IFD_EXIF_POINTER(0x8769, "Exif Metadata Pointer", DirectoryIdentifier.IFD_EXIF_SUBIFD_DIRECTORY),
    IFD_GPS_INFO_POINTER(0x8825, "GPS Metadata Pointer", DirectoryIdentifier.IFD_GPS_SUBIFD_DIRECTORY),
    EXIF_INTEROPERABILITY_POINTER(0xA005, "Interoperability Offset", DirectoryIdentifier.EXIF_INTEROP_DIRECTORY);

    private final int numID;
    private final DirectoryIdentifier targetDir;
    private final String desc;

    private TagIFD_Pointer(int id, String desc, DirectoryIdentifier targetDir)
    {
        this.numID = id;
        this.desc = desc;
        this.targetDir = targetDir;
    }

    @Override
    public int getNumberID()
    {
        return numID;
    }

    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return targetDir;
    }

    @Override
    public TagHint getHint()
    {
        return TagHint.HINT_DEFAULT;
    }

    @Override
    public String getDescription()
    {
        return desc;
    }
}
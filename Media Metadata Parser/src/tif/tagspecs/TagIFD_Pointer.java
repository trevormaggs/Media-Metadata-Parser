package tif.tagspecs;

import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Structural offset pointer tags used to navigate between TIFF sub-directories.
 *
 * <p>
 * These tags act as file offset descriptors containing 32-bit addresses. Instead of holding direct
 * metadata values, they instruct the parsing pipeline to jump to a completely separate Image File
 * Directory (IFD) segment in the byte stream, such as Exif or GPS segments.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 18 June 2026
 */
public enum TagIFD_Pointer implements Taggable
{
    IFD_SUBIFD_POINTER(0x014A, "SubIFD Data Offset", DirectoryIdentifier.IFD_ROOT_DIRECTORY, DirectoryIdentifier.IFD_SUBIFD_DIRECTORY),
    IFD_EXIF_POINTER(0x8769, "Exif Metadata Pointer", DirectoryIdentifier.IFD_ROOT_DIRECTORY, DirectoryIdentifier.IFD_EXIF_SUBIFD_DIRECTORY),
    IFD_GPS_INFO_POINTER(0x8825, "GPS Metadata Pointer", DirectoryIdentifier.IFD_ROOT_DIRECTORY, DirectoryIdentifier.IFD_GPS_SUBIFD_DIRECTORY),
    IFD_INTEROPERABILITY_POINTER(0xA005, "Interoperability Offset", DirectoryIdentifier.IFD_EXIF_SUBIFD_DIRECTORY, DirectoryIdentifier.EXIF_INTEROP_DIRECTORY);

    private final int numID;
    private final String desc;
    private final DirectoryIdentifier hostDir;
    private final DirectoryIdentifier targetDir;

    private TagIFD_Pointer(int id, String desc, DirectoryIdentifier hostDir, DirectoryIdentifier targetDir)
    {
        this.numID = id;
        this.desc = desc;
        this.hostDir = hostDir;
        this.targetDir = targetDir;
    }

    /**
     * Retrieves the low-level binary tag identifier used during raw file translation.
     *
     * @return the numerical integer ID
     */
    @Override
    public int getNumberID()
    {
        return numID;
    }

    /**
     * Retrieves the directory block classification context where this tag is hosted.
     *
     * @return the hosting {@link DirectoryIdentifier} key
     */
    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return hostDir;
    }

    /**
     * Indicates the expected parsing primitive format strategy applied to read this element.
     *
     * @return always {@link TagHint#HINT_INTEGER} as pointers represent 32-bit unsigned offset
     *         positions
     */
    @Override
    public TagHint getHint()
    {
        return TagHint.HINT_INTEGER;
    }

    /**
     * Retrieves the descriptive text title assigned to this pointer tag element.
     *
     * @return the text {@link String} label
     */
    @Override
    public String getDescription()
    {
        return desc;
    }

    /**
     * Determines if the offset pointer originates from a primary root-level directory.
     *
     * @return {@code true} if the hosting directory is the root image file chain, otherwise
     *         {@code false}
     */
    public boolean isMainChainPointer()
    {
        return (this.hostDir == DirectoryIdentifier.IFD_ROOT_DIRECTORY);
    }

    /**
     * Retrieves the structural directory block where the parsed offset pointer takes effect.
     *
     * @return the destination {@link DirectoryIdentifier} metadata container target
     */
    public DirectoryIdentifier getTargetDirectory()
    {
        return targetDir;
    }
}
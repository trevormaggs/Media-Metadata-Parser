package tif;

import common.Metadata;
import xmp.XmpDirectory;

/**
 * A dedicated interface for accessing and managing TIFF-based metadata structures.
 *
 * <p>
 * This interface extends the base {@link Metadata} contract to navigate the hierarchical nature of
 * TIFF files. It provides direct access to specific Image File Directories (IFDs), including EXIF,
 * GPS, and Interoperability sub-directories.
 * </p>
 *
 * <p>
 * Additionally, this interface serves as a container for {@link XmpDirectory} segments,
 * consolidating binary IFD tags and XML-based metadata packets into a single management context.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 13 August 2025
 * @see DirectoryIFD
 * @see DirectoryIdentifier
 */
public interface TifMetadataProvider extends Metadata<DirectoryIFD>
{
    /**
     * Retrieves a specific Image File Directory (IFD) by its identifier.
     *
     * @param dirKey
     *        the identity of the directory to retrieve, such as IFD0, EXIF_SUBIFD, or GPS
     * @return the matching {@link DirectoryIFD}, or {@code null} if the directory is not present
     */
    public DirectoryIFD getDirectory(DirectoryIdentifier dirKey);

    /**
     * Integrates an XMP metadata directory for unified access.
     *
     * <p>
     * XMP is often embedded in TIFF-based formats within a global tag (ID 0x02BC). This method
     * allows the parsed XMP object to be managed alongside standard IFD tags.
     * </p>
     *
     * @param dir
     *        the {@link XmpDirectory} containing the parsed XML metadata
     */
    public void addXmpDirectory(XmpDirectory dir);

    /**
     * Retrieves the attached XMP metadata directory.
     *
     * @return the {@link XmpDirectory} instance, or {@code null} if no XMP data exists
     */
    public XmpDirectory getXmpDirectory();

}
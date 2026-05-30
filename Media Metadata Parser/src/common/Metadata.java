package common;

import java.nio.ByteOrder;
import java.time.ZonedDateTime;

/**
 * A unified top-level container for image metadata structured into accessible {@link Directory}
 * blocks.
 *
 * <p>
 * This interface serves as the foundational contract for managing, traversing, and extracting
 * metadata from diverse imaging formats, such as TIFF, JPEG, PNG. It models metadata as an iterable
 * collection of domain-specific directories, where each directory encapsulates related metadata
 * properties or tags (such as camera settings, geolocation data, or text blocks).
 * </p>
 *
 * <p>
 * Beyond structural management, this interface acts as a high-level facade for harvesting standard,
 * cross-format information—such as temporal markers—by abstracting format-specific parsing
 * complexities behind a unified API.
 * </p>
 *
 * @param <D>
 *        the specific type of {@link Directory} compiled within this metadata container, such as
 *        {@code DirectoryIFD} for TIFF or {@code PngDirectory} for PNG images
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2025
 */
public interface Metadata<D extends Directory<?>> extends Iterable<D>
{
    /**
     * Adds a metadata directory to the collection.
     *
     * @param directory
     *        the directory to add
     */
    void addDirectory(D directory);

    /**
     * Removes a metadata directory from the collection.
     *
     * @param directory
     *        the directory to remove
     * @return {@code true} if the directory was found and removed, otherwise {@code false}
     */
    boolean removeDirectory(D directory);

    /**
     * Checks if the metadata collection is empty.
     *
     * @return {@code true} if the collection contains no directories, otherwise {@code false}
     */
    boolean isEmpty();

    /**
     * Returns the global byte order used natively by the metadata container of the image format.
     * 
     * <p>
     * Implementations must explicitly return the definitive endian-ness of the target image
     * wrapper.
     * </p>
     *
     * @return either {@link java.nio.ByteOrder#BIG_ENDIAN} or
     *         {@link java.nio.ByteOrder#LITTLE_ENDIAN}
     */
    ByteOrder getByteOrder();

    /**
     * Checks if the metadata collection contains any metadata entries.
     *
     * @return {@code true} if at least one directory is present, otherwise {@code false}
     */
    boolean hasMetadata();
 
    /**
     * Indicates whether the container holds EXIF metadata.
     * 
     * @return {@code true} if EXIF metadata is present, otherwise {@code false}
     */
    default boolean hasExifData()
    {
        return false;
    }

    /**
     * Indicates whether the container holds XMP (Adobe Extensible Metadata Platform) metadata.
     * 
     * @return {@code true} if XMP metadata is present, otherwise {@code false}
     */
    default boolean hasXmpData()
    {
        return false;
    }

    /**
     * Extracts the most authoritative creation date available as a modern ZonedDateTime instance.
     * 
     * <p>
     * This method preserves timezone and offset information often found in XMP segments.
     * </p>
     * 
     * @return the extracted {@link ZonedDateTime}, or {@code null} if no valid timestamp is found
     */
    ZonedDateTime extractZonedDateTime();
}
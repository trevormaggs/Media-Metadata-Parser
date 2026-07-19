package common;

import java.nio.ByteOrder;
import java.time.ZonedDateTime;

/**
 * Represents a collection of image metadata organised into {@link Directory} instances.
 *
 * <p>
 * Implementations provide a unified view of metadata extracted from image formats such as TIFF,
 * JPEG, PNG, HEIF, and WebP. Each directory contains a related set of metadata entries, such as
 * EXIF tags, text values, or format-specific properties.
 * </p>
 *
 * <p>
 * This interface also provides access to common cross-format metadata, including byte order
 * information and image creation timestamps.
 * </p>
 *
 * @param <D>
 *        the directory type contained within this metadata collection
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
     * Resets the metadata container to an entirely empty state.
     */
    void clear();

    /**
     * Checks if the metadata collection is empty.
     *
     * @return {@code true} if the collection contains no directories, otherwise {@code false}
     */
    boolean isEmpty();

    /**
     * Returns the native byte order used by the metadata structure.
     *
     * @return either {@link java.nio.ByteOrder#BIG_ENDIAN} or
     *         {@link java.nio.ByteOrder#LITTLE_ENDIAN}
     */
    ByteOrder getByteOrder();

    /**
     * Returns the detected media format signature associated with this metadata container.
     *
     * @return a {@link DigitalSignature} enum variant identifying the true structure, or
     *         {@link DigitalSignature#UNKNOWN} if undefined or generic
     */
    DigitalSignature getImageFormat();

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
     * Returns the best available creation timestamp.
     *
     * <p>
     * Implementations may derive this value from EXIF, XMP, or other format-specific metadata
     * sources.
     * </p>
     *
     * @return the creation timestamp, or {@code null} if unavailable
     */
    ZonedDateTime extractZonedDateTime();
}
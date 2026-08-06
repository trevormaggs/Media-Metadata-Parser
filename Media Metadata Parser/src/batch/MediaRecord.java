package batch;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import common.DigitalSignature;
import common.Metadata;

/**
 * Represents an immutable snapshot of a media file captured during a scan.
 *
 * <p>
 * This package-private class encapsulates the media file's location, extracted metadata, detected
 * digital format, file system attributes, and the file's chronological "truth" (Natural Date).
 * </p>
 *
 * <p>
 * It is intended solely for internal use within the {@code batch} package as a transport object
 * between the scanning and processing stages.
 * </p>
 *
 * <p>
 * Instances are immutable, provided that the supplied {@link Metadata} implementation is itself
 * immutable.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.1
 * @since 1 May 2026
 */
final class MediaRecord
{
    private final Path mediaFile;
    private final Metadata<?> metadata;
    private final DigitalSignature mediaFormat;
    private final long fileSize;
    private final FileTime fileSystemDate;
    private final boolean hasMetadataContainer;

    /**
     * Constructs an immutable media record from the specified scan results.
     *
     * @param fpath
     *        the path to the media file
     * @param attr
     *        the file system attributes recorded during scanning
     * @param record
     *        the extracted metadata container, or {@code null} if the file contains no recognised
     *        metadata
     */
    MediaRecord(Path fpath, BasicFileAttributes attr, Metadata<?> record)
    {
        this.mediaFile = fpath;
        this.metadata = record;
        this.mediaFormat = (record != null ? record.getImageFormat() : null);
        this.fileSize = attr.size();
        this.fileSystemDate = attr.lastModifiedTime();
        this.hasMetadataContainer = (record != null && record.hasMetadata());
    }

    /**
     * Returns the file system path to the media file.
     *
     * @return the media file path
     */
    Path getPath()
    {
        return mediaFile;
    }

    /**
     * Returns the metadata container extracted from the media file.
     *
     * @return the metadata instance, or {@code null} if no metadata was found
     */
    Metadata<?> getMetadata()
    {
        return metadata;
    }

    /**
     * Returns the digital signature identifying the media format, as determined from the file's
     * signature (magic number).
     *
     * @return the detected media format
     */
    DigitalSignature getMediaFormat()
    {
        return mediaFormat;
    }

    /**
     * Returns the size of the media file in bytes recorded during the scan.
     *
     * @return the file size in bytes
     */
    long getFileSize()
    {
        return fileSize;
    }

    /**
     * Returns the file system's last modified time recorded during the scan.
     * 
     * @return the file system date
     */
    FileTime getFileSystemDate()
    {
        return fileSystemDate;
    }

    /**
     * Indicates whether the media file lacks recognised embedded metadata.
     *
     * @return {@code true} if no metadata container was found, otherwise {@code false}
     */
    boolean isMetadataEmpty()
    {
        return !hasMetadataContainer;
    }

    /**
     * Checks if the media file's digital signature matches the expected JPG standard.
     *
     * @return true if JPG, otherwise false
     */
    boolean isJPG()
    {
        return mediaFormat == DigitalSignature.JPG;
    }

    /**
     * Checks if the media file's digital signature matches the expected PNG standard.
     * 
     * @return true if PNG, otherwise false
     */
    boolean isPNG()
    {
        return mediaFormat == DigitalSignature.PNG;
    }

    /**
     * Checks if the media file's digital signature matches the expected TIFF standard.
     * 
     * @return true if TIFF, otherwise false
     */
    boolean isTIF()
    {
        return mediaFormat == DigitalSignature.TIF;
    }

    /**
     * Checks if the media file's digital signature matches the expected HEIC standard.
     *
     * @return true if HEIC, otherwise false
     */
    boolean isHEIC()
    {
        return mediaFormat == DigitalSignature.HEIF;
    }

    /**
     * Checks if the media file's digital signature matches the expected WebP standard.
     *
     * @return true if WebP, otherwise false
     */
    boolean isWebP()
    {
        return mediaFormat == DigitalSignature.WEBP;
    }

    /**
     * Returns whether this media file represents a recognised video format, such as MP4, MOV, or
     * AVI.
     *
     * @return true if the media is a video, otherwise false
     */
    boolean isVideoFormat()
    {
        return mediaFormat != null && mediaFormat.isVideo();
    }

    /**
     * Resolves the "chronological truth" for this file. Prioritises embedded metadata timestamps
     * before falling back to the file system's last modified time.
     * 
     * @return the most accurate timestamp available for this media
     */
    FileTime getNaturalDate()
    {
        if (hasMetadataContainer)
        {
            ZonedDateTime metaDate = metadata.extractZonedDateTime();

            if (metaDate != null)
            {
                return FileTime.from(metaDate.toInstant());
            }
        }

        return getFileSystemDate();
    }

    /**
     * Compares this media record with another object.
     *
     * <p>
     * Two media records are considered equal if all immutable state used to describe the scanned
     * file is equal.
     * </p>
     *
     * @param other
     *        the object to compare
     * @return {@code true} if the objects are equal, otherwise {@code false}
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof MediaRecord))
        {
            return false;
        }

        MediaRecord record = (MediaRecord) other;

        return fileSize == record.fileSize
                && hasMetadataContainer == record.hasMetadataContainer
                && Objects.equals(fileSystemDate, record.fileSystemDate)
                && mediaFormat == record.mediaFormat
                && Objects.equals(metadata, record.metadata)
                && Objects.equals(mediaFile, record.mediaFile);
    }

    /**
     * Computes a hash code based on the record's immutable state to ensure stable behaviour in
     * hashed collections.
     *
     * @return the hash code for this object
     */
    @Override
    public int hashCode()
    {
        int result = 17;

        result = 31 * result + Objects.hashCode(mediaFile);
        result = 31 * result + Objects.hashCode(metadata);
        result = 31 * result + Objects.hashCode(mediaFormat);
        result = 31 * result + Objects.hashCode(fileSystemDate);
        result = 31 * result + Long.hashCode(fileSize);
        result = 31 * result + Boolean.hashCode(hasMetadataContainer);

        return result;
    }

    /**
     * Returns a formatted string representation of the media record, suitable for CLI display or
     * debug logging.
     *
     * @return a multi-line formatted string containing file details
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(String.format("  %-30s %s%n", "[Media File]", getPath()));
        line.append(String.format("  %-30s %d bytes%n", "[File Size]", getFileSize()));
        line.append(String.format("  %-30s %s%n", "[Metadata]", getMetadata()));
        line.append(String.format("  %-30s %s%n", "[Format]", getMediaFormat()));
        line.append(String.format("  %-30s %s%n", "[Empty Metadata]", isMetadataEmpty()));
        line.append(String.format("  %-30s %s%n", "[Natural Date]", getNaturalDate()));
        line.append(String.format("  %-30s %s%n", "[File System Date]", getFileSystemDate()));

        return line.toString();
    }
}
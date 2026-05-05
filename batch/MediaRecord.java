package batch;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import common.DigitalSignature;
import common.Metadata;

/**
 * Represents an immutable snapshot of a media file's properties read during a scan.
 * 
 * <p>
 * This record encapsulates the physical location, raw metadata container, verified digital format,
 * and chronological "truth" (Natural Date) of the file.
 * </p>
 * 
 * <p>
 * This class is designed to be immutable, ensuring thread safety when shared across batch
 * processing tasks.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.1
 * @since 1 May 2026
 */
public final class MediaRecord
{
    private final Path mediaFile;
    private final Metadata<?> metadata;
    private final DigitalSignature mediaFormat;
    private final FileTime fileSystemDate;
    private final boolean hasMetadataContainer;

    public MediaRecord(Path fpath, Metadata<?> meta, DigitalSignature sig, FileTime ft)
    {
        this.mediaFile = fpath;
        this.metadata = meta;
        this.mediaFormat = sig;
        this.fileSystemDate = ft;
        this.hasMetadataContainer = (meta != null && meta.hasMetadata());
    }

    /**
     * Returns the file system path to the media file.
     *
     * @return the media file path
     */
    public Path getPath()
    {
        return mediaFile;
    }

    public Metadata<?> getMetadata()
    {
        return metadata;
    }

    /**
     * Returns the media format signature used to identify the file type, normally via magic
     * numbers.
     *
     * @return the media format type
     */
    public DigitalSignature getMediaFormat()
    {
        return mediaFormat;
    }

    /**
     * Indicates whether this media file lacks a valid embedded metadata container, such as EXIF or
     * XMP.
     *
     * @return true if the file lacks metadata, otherwise false
     */
    public boolean isMetadataEmpty()
    {
        return !hasMetadataContainer;
    }

    /**
     * Checks if the media file's digital signature matches the expected JPG standard.
     *
     * @return true if JPG, otherwise false
     */
    public boolean isJPG()
    {
        return mediaFormat == DigitalSignature.JPG;
    }

    /**
     * Checks if the media file's digital signature matches the expected PNG standard.
     * 
     * @return true if PNG, otherwise false
     */
    public boolean isPNG()
    {
        return mediaFormat == DigitalSignature.PNG;
    }

    /**
     * Checks if the media file's digital signature matches the expected TIFF standard.
     * 
     * @return true if TIFF, otherwise false
     */
    public boolean isTIF()
    {
        return mediaFormat == DigitalSignature.TIF;
    }

    /**
     * Checks if the media file's digital signature matches the expected HEIC standard.
     *
     * @return true if HEIC, otherwise false
     */
    public boolean isHEIC()
    {
        return mediaFormat == DigitalSignature.HEIF;
    }

    /**
     * Checks if the media file's digital signature matches the expected WebP standard.
     *
     * @return true if isWebP, otherwise false
     */
    public boolean isWebP()
    {
        return mediaFormat == DigitalSignature.WEBP;
    }

    /**
     * Returns whether this media file is of a known video format, for example: MP4, MOV, AVI, etc.
     *
     * @return true if the media is a video, otherwise false
     */
    public boolean isVideoFormat()
    {
        return mediaFormat.isVideo();
    }

    /**
     * Returns the file system's last modified time recorded during the scan.
     * 
     * @return the file system date
     */
    public FileTime getFileSystemDate()
    {
        return fileSystemDate;
    }

    /**
     * Resolves the "chronological truth" for this file. Prioritises embedded metadata timestamps
     * before falling back to the file system's last modified time.
     * 
     * @return the most accurate timestamp available for this media
     */
    public FileTime getNaturalDate()
    {
        if (hasMetadataContainer)
        {
            ZonedDateTime metaDate = metadata.extractZonedDateTime();

            if (metaDate != null)
            {
                return FileTime.from(metaDate.toInstant());
            }
        }

        return this.fileSystemDate;
    }

    /**
     * Compares this record with another object. Two records are considered equal if they point to
     * the same path and share the same metadata and format state.
     *
     * @param other
     *        the object to compare
     * 
     * @return true if the objects are equal, otherwise false
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

        MediaRecord meta = (MediaRecord) other;

        return hasMetadataContainer == meta.hasMetadataContainer
                && mediaFormat == meta.mediaFormat
                && Objects.equals(metadata, meta.metadata)
                && Objects.equals(mediaFile, meta.mediaFile);
    }

    /**
     * Computes a hash code based on the file path, metadata state, and digital signature to ensure
     * stable behaviour in hashed collections.
     *
     * @return the hash code for this object
     */
    @Override
    public int hashCode()
    {
        int result = 17;

        result = 31 * result + mediaFile.hashCode();
        result = 31 * result + metadata.hashCode();
        result = 31 * result + mediaFormat.hashCode();
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
        line.append(String.format("  %-30s %s%n", "[Media File]", mediaFile));
        line.append(String.format("  %-30s %s%n", "[Metadata]", metadata));
        line.append(String.format("  %-30s %s%n", "[Format]", mediaFormat));
        line.append(String.format("  %-30s %s%n", "[Empty Metadata]", !hasMetadataContainer));

        return line.toString();
    }
}
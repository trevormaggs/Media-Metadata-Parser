package batch;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import common.DigitalSignature;
import common.Metadata;

/**
 * Represents metadata for a single media file, including the file path, capture date, file format,
 * and metadata availability.
 * 
 * <p>
 * This class is designed to be immutable, ensuring thread safety when shared across batch
 * processing tasks.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public final class MediaFile2
{
    private final Path mediaFile;
    private final Metadata<?> metadata;
    private final boolean hasNoMetadata;
    private final DigitalSignature mediaFormat;
    private final FileTime fileSystemDate;

    public MediaFile2(Path fpath, Metadata<?> meta, DigitalSignature sig, FileTime ft)
    {
        this.mediaFile = fpath;
        this.metadata = meta;
        this.mediaFormat = sig;
        this.hasNoMetadata = (meta == null || meta.extractZonedDateTime() == null);
        this.fileSystemDate = ft;
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
     * Indicates whether this media file lacks embedded metadata (for example, EXIF). This is
     * used to determine whether metadata should be added or inferred from the file system.
     *
     * @return true if the file lacks metadata, otherwise false
     */
    public boolean isMetadataEmpty()
    {
        return hasNoMetadata;
    }

    /**
     * Returns whether this media file is in JPG format.
     *
     * @return true if JPG, otherwise false
     */
    public boolean isJPG()
    {
        return mediaFormat == DigitalSignature.JPG;
    }

    /**
     * Returns whether this media file is in PNG format.
     *
     * @return true if PNG, otherwise false
     */
    public boolean isPNG()
    {
        return mediaFormat == DigitalSignature.PNG;
    }

    /**
     * Returns whether this media file is in TIFF format.
     *
     * @return true if TIFF, otherwise false
     */
    public boolean isTIF()
    {
        return mediaFormat == DigitalSignature.TIF;
    }

    /**
     * Returns whether this media file is in HEIC format.
     *
     * @return true if HEIC, otherwise false
     */
    public boolean isHEIC()
    {
        return mediaFormat == DigitalSignature.HEIF;
    }

    /**
     * Returns whether this media file is in WebP format.
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
    
    /*
     * long offset = 0;
     * for (MediaFile2 media : scanner) {
     * FileTime targetDate = media.getEffectiveDate(config, offset);
     * // ... execute copy/rename
     * offset += 10_000; // Increment for next file
     * }
     */
    public FileTime getEffectiveDate(BatchConfiguration config, long offsetMillis)
    {
        // Priority 1: User explicitly wants to override everything
        if (config.isForceDateChange() && config.getUserDate() != null)
        {
            return FileTime.from(config.getUserDate().toInstant().plusMillis(offsetMillis));
        }

        // Priority 2: Trust the internal metadata if it exists
        ZonedDateTime metaDate = (metadata != null ? metadata.extractZonedDateTime() : null);
        
        if (metaDate != null)
        {
            return FileTime.from(metaDate.toInstant());
        }

        // Priority 3: Metadata is missing; use the User Date as a baseline + sequence offset
        if (config.getUserDate() != null)
        {
            return FileTime.from(config.getUserDate().toInstant().plusMillis(offsetMillis));
        }

        // Ultimate fallback: The OS file system time
        return this.fileSystemDate;
    }

    /**
     * Compares this MediaFile instance with another for equality.
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

        if (!(other instanceof MediaFile2))
        {
            return false;
        }

        MediaFile2 meta = (MediaFile2) other;

        return hasNoMetadata == meta.hasNoMetadata && mediaFormat == meta.mediaFormat
                && Objects.equals(metadata, meta.metadata) && Objects.equals(mediaFile, meta.mediaFile);
    }

    /**
     * Computes a hash code based on the media path, capture date, format, and metadata status.
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
        result = 31 * result + Boolean.hashCode(hasNoMetadata);

        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();
        line.append(String.format("  %-30s %s%n", "[Media File]", mediaFile));
        line.append(String.format("  %-30s %s%n", "[Metadata]", metadata));
        line.append(String.format("  %-30s %s%n", "[Format]", mediaFormat));
        line.append(String.format("  %-30s %s%n", "[Empty Metadata]", hasNoMetadata));

        return line.toString();
    }
}
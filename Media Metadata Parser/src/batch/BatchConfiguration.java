package batch;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An immutable configuration object representing the validated parameters for a batch processing
 * operation.
 *
 * <p>
 * This class provides a snapshot of settings produced by {@link BatchBuilder} after validation has
 * been completed. Once created, the configuration cannot be modified.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 2 June 2026
 */
public final class BatchConfiguration
{
    private final Path source;
    private final Path target;
    private final String prefix;
    private final ZonedDateTime userDate;
    private final Set<String> fileSet;
    private final boolean forceDateChange;
    private final boolean embedDateTime;
    private final boolean skipVideo;
    private final boolean showMetadata;
    private final boolean descending;
    private final boolean debug;

    /**
     * Creates an immutable configuration from validated builder state.
     *
     * <p>
     * This constructor is intended for use by {@link BatchBuilder} after all configuration values
     * have been validated.
     * </p>
     * 
     * @param source
     *        the directory containing the original media files
     * @param target
     *        the destination directory for processed copies
     * @param prefix
     *        a user-defined string prepended to new filenames
     * @param userDate
     *        a user-defined timestamp for metadata updates, or {@code null}
     * @param fileSet
     *        an optional array containing the filenames to process
     * @param forceDateChange
     *        flag to overwrite existing metadata tags
     * @param embedDateTime
     *        flag to include timestamps in the generated filename
     * @param skipVideo
     *        flag to ignore video formats during processing
     * @param showMetadata
     *        flag to display detailed metadata information
     * @param descending
     *        flag to sort media in reverse chronological order
     * @param debug
     *        flag to enable verbose diagnostic output
     */
    BatchConfiguration(Path source, Path target, String prefix, ZonedDateTime userDate, String[] fileSet, boolean forceDateChange, boolean embedDateTime, boolean skipVideo, boolean showMetadata, boolean descending, boolean debug)
    {
        this.source = source;
        this.target = target;
        this.prefix = (prefix == null ? "" : prefix);
        this.userDate = userDate;
        this.fileSet = (fileSet == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(Arrays.asList(fileSet))));
        this.forceDateChange = forceDateChange;
        this.embedDateTime = embedDateTime;
        this.skipVideo = skipVideo;
        this.showMetadata = showMetadata;
        this.descending = descending;
        this.debug = debug;
    }

    /**
     * Returns the source directory containing the media files to be processed.
     *
     * @return the source directory
     */
    public Path getSource()
    {
        return source;
    }

    /**
     * Returns the destination directory for processed files.
     *
     * @return the target directory
     */
    public Path getTarget()
    {
        return target;
    }

    /**
     * Returns the filename prefix to prepend to generated filenames.
     *
     * @return the configured filename prefix, or an empty string if no prefix was specified
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * Returns the user-defined timestamp used for metadata updates.
     *
     * @return the configured timestamp, or {@code null} if original file dates should be used
     */
    public ZonedDateTime getUserDate()
    {
        return userDate;
    }

    /**
     * Returns the set of filenames explicitly selected for processing.
     *
     * @return an unmodifiable set of filenames, or an empty set if no file filter was specified
     */
    public Set<String> getFileSet()
    {
        return fileSet;
    }

    /**
     * Indicates whether existing metadata date values should be overwritten.
     *
     * @return {@code true} if existing metadata dates should be replaced, {@code false} otherwise
     */
    public boolean isForceDateChange()
    {
        return forceDateChange;
    }

    /**
     * Indicates whether timestamps should be embedded in generated filenames.
     *
     * @return {@code true} if timestamps should be included in filenames, {@code false} otherwise
     */
    public boolean isEmbedDateTime()
    {
        return embedDateTime;
    }

    /**
     * Indicates whether video files should be excluded from processing.
     *
     * @return {@code true} if video files should be skipped, {@code false} otherwise
     */
    public boolean isSkipVideo()
    {
        return skipVideo;
    }

    /**
     * Indicates whether detailed metadata information should be displayed.
     *
     * @return {@code true} if detailed metadata information should be displayed, {@code false}
     *         otherwise
     */
    public boolean isShowMetadata()
    {
        return showMetadata;
    }

    /**
     * Indicates whether media files should be processed in descending chronological order.
     *
     * @return {@code true} if descending ordering is enabled, {@code false} otherwise
     */
    public boolean isDescending()
    {
        return descending;
    }

    /**
     * Indicates whether diagnostic output is enabled.
     *
     * @return {@code true} if debug mode is enabled, {@code false} otherwise
     */
    public boolean isDebug()
    {
        return debug;
    }
}
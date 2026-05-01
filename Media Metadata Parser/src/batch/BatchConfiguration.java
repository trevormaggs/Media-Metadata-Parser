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
 * Designed to provide a "frozen" snapshot of settings, this class ensures that parameters remain
 * constant once the {@link BatchBuilder} has completed validation and instantiation.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 23 April 2026
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
     * Internal constructor used by the {@link BatchBuilder} to encapsulate validated state.
     * 
     * @param source
     *        the directory containing the original media files
     * @param target
     *        the destination directory for processed copies
     * @param prefix
     *        a user-defined string prepended to new filenames
     * @param userDate
     *        a forced timestamp for metadata patching (may be null)
     * @param fileSet
     *        an optional array of specific filenames to process
     * @param forceDateChange
     *        flag to overwrite existing metadata tags
     * @param embedDateTime
     *        flag to include timestamps in the generated filename
     * @param skipVideo
     *        flag to ignore video formats during processing
     * @param showMetadata
     *        flag to trigger detailed metadata logging
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

    public Path getSource()
    {
        return source;
    }

    public Path getTarget()
    {
        return target;
    }

    public String getPrefix()
    {
        return prefix;
    }

    /**
     * @return the user-defined time-stamp, or {@code null} if using original file dates
     */
    public ZonedDateTime getUserDate()
    {
        return userDate;
    }

    public Set<String> getFileSet()
    {
        return fileSet;
    }

    public boolean isForceDateChange()
    {
        return forceDateChange;
    }

    public boolean isEmbedDateTime()
    {
        return embedDateTime;
    }

    public boolean isSkipVideo()
    {
        return skipVideo;
    }

    public boolean isShowMetadata()
    {
        return showMetadata;
    }

    public boolean isDescending()
    {
        return descending;
    }

    public boolean isDebug()
    {
        return debug;
    }
}
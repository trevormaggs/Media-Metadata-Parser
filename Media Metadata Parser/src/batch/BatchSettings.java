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
 * *
 * <p>
 * In Java 8, this is implemented as a final class with final fields to ensure that once the
 * {@link BatchBuilder} produces this object, the settings remain constant throughout the execution
 * of the {@link BatchExecutor}.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 22 April 2026
 */
public final class BatchSettings
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
     * Internal constructor used by the Builder to "freeze" the state.
     */
    public BatchSettings(Path source, Path target, String prefix, ZonedDateTime userDate, String[] fileSet, boolean forceDateChange, boolean embedDateTime, boolean skipVideo, boolean showMetadata, boolean descending, boolean debug)
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

    /**
     * Utility method to check if a specific file name is part of the restricted processing set.
     */
    public boolean shouldProcessFile(String fileName)
    {
        return fileSet.isEmpty() || fileSet.contains(fileName);
    }
}
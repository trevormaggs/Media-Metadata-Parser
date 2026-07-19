package batch;

import java.nio.file.Paths;
import java.time.ZonedDateTime;

/**
 * <p>
 * Implements the Builder design pattern for constructing and validating {@link BatchConfiguration}
 * instances used by batch-processing operations.
 * </p>
 * 
 * <p>
 * This class serves as the central entry point for defining and validating batch-processing
 * parameters. It can produce a self-initiating {@link MediaMetadataConsole} via {@link #build()}
 * for CLI applications, or an immutable {@link BatchConfiguration} via {@link #buildConfig()} for
 * custom implementations such as GUI.
 * </p>
 * 
 * <p>
 * <b>Example (CLI):</b>
 * </p>
 * 
 * <pre>
 * <code>
 * MediaMetadataConsole console = new BatchBuilder()
 * .source("D:\\Media\\Photos")
 * .target("local\\images")
 * .prefix("holiday")
 * .descending(true)
 * .userDate("26 04 2006")
 * .build();
 * </code>
 * </pre>
 * 
 * <p>
 * <b>Example (GUI):</b>
 * </p>
 * 
 * <pre>
 * <code>
 * MediaMetadataConsole config = new BatchBuilder()
 * .source(txtField.getText())
 * .buildConfig();
 * </code>
 * </pre>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 24 April 2026
 */
public final class BatchBuilder
{
    /** Source directory containing media files to process. */
    private String bd_sourceDir = MediaBatchProcessor.DEFAULT_SOURCE_DIRECTORY;

    /** Filename prefix applied to generated output files. */
    private String bd_prefix = MediaBatchProcessor.DEFAULT_IMAGE_PREFIX;

    /** Target directory for processed files. */
    private String bd_target = MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY;

    /** Determines whether the media date should be embedded in generated filenames. */
    private boolean bd_embedDateTime = false;

    /** User-supplied date string prior to parsing and validation. */
    private String bd_userDate = "";

    /** Indicates whether existing metadata dates should be overwritten. */
    private boolean bd_force = false;

    /** Optional list of filenames explicitly selected for processing. */
    private String[] bd_files = new String[0];

    /** Indicates whether video files should be excluded from processing. */
    private boolean bd_skipVideoFiles = false;

    /** Indicates whether metadata information should be displayed instead of processing files. */
    private boolean bd_displayMetadata = false;

    /** Indicates whether chronological ordering should be descending. */
    private boolean bd_descending = false;

    /** Indicates whether diagnostic logging should be enabled. */
    private boolean bd_debug = false;

    /**
     * Sets the source directory containing the original media files.
     *
     * @param s
     *        the source directory
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder source(String s)
    {
        if (s != null)
        {
            bd_sourceDir = s;
        }

        return this;
    }

    /**
     * Sets the target directory where the processed files will be saved.
     *
     * @param t
     *        the target directory
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder target(String t)
    {
        if (t != null)
        {
            bd_target = t;
        }

        return this;
    }

    /**
     * Sets a prefix to be prepended uniformly to each output file name.
     *
     * @param p
     *        the string to be prepended to every copied file
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder prefix(String p)
    {
        if (p != null)
        {
            bd_prefix = p;
        }

        return this;
    }

    /**
     * Sets a specific date used to override both the EXIF {@code Date Taken} metadata and the file
     * system modification timestamp.
     *
     * @param d
     *        the date and time string to be used for processing
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder userDate(String d)
    {
        if (d != null)
        {
            bd_userDate = d;
        }

        return this;
    }

    /**
     * Specifies a defined set of individual files to copy, rather than processing the entire source
     * directory.
     *
     * @param f
     *        a string array of specific filenames
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder fileSet(String[] f)
    {
        if (f != null)
        {
            bd_files = f;
        }

        return this;
    }

    /**
     * Forces the system to prioritise the user-defined date, regardless of any existing metadata
     * present in the file.
     *
     * @param b
     *        {@code true} to force the user-defined date to replace existing metadata dates
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder forceDateChange(boolean b)
    {
        bd_force = b;
        return this;
    }

    /**
     * Indicates whether media date information should be embedded in output filenames.
     *
     * @param b
     *        {@code true} to include the date in generated filenames, otherwise {@code false}
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder embedDateTime(boolean b)
    {
        bd_embedDateTime = b;
        return this;
    }

    /**
     * Defines whether video files should be skipped or included in the batch.
     *
     * @param b
     *        {@code true} to skip video files, otherwise copy them
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder skipVideo(boolean b)
    {
        bd_skipVideoFiles = b;
        return this;
    }

    /**
     * Configures the builder to display a list of metadata entries for the processed files.
     *
     * @param b
     *        {@code true} to enable metadata display
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder showMetadata(boolean b)
    {
        bd_displayMetadata = b;
        return this;
    }

    /**
     * Configures the sorting order of processed media records.
     *
     * @param b
     *        {@code true} to sort in descending order, otherwise ascending
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder descending(boolean b)
    {
        bd_descending = b;
        return this;
    }

    /**
     * Enables or disables debug mode for more verbose logging.
     *
     * @param b
     *        {@code true} to enable debugging
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder debug(boolean b)
    {
        bd_debug = b;
        return this;
    }

    /**
     * Validates the current builder state and creates a new {@link MediaMetadataConsole} instance.
     *
     * <p>
     * This is the preferred entry point for command-line applications, where the resulting console
     * instance manages execution of the configured batch operation.
     * </p>
     *
     * @return a new console instance configured using the validated builder state
     *
     * @throws IllegalStateException
     *         if the configuration fails validation
     */
    public MediaMetadataConsole build()
    {
        return new MediaMetadataConsole(buildConfig());
    }

    /**
     * Validates the current configuration and returns an immutable {@link BatchConfiguration}
     * snapshot.
     *
     * @return a validated immutable configuration object
     *
     * @throws IllegalStateException
     *         if one or more configuration constraints are violated
     */
    public BatchConfiguration buildConfig()
    {
        validate();

        ZonedDateTime parsedDate = util.SmartDateParser.convertToZonedDateTime(bd_userDate);

        return new BatchConfiguration(Paths.get(bd_sourceDir), Paths.get(bd_target),
                bd_prefix, parsedDate,
                bd_files, bd_force,
                bd_embedDateTime, bd_skipVideoFiles,
                bd_displayMetadata, bd_descending,
                bd_debug);
    }

    /**
     * Validates the builder's state against internal constraint rules to maintain configuration
     * integrity.
     * 
     * <p>
     * This validation ensures that a user-defined date is supplied whenever forced date
     * modification is enabled.
     * </p>
     * 
     * @throws IllegalStateException
     *         if forced date modification is enabled without a user-defined date
     */
    private void validate()
    {
        if (bd_force && (bd_userDate == null || bd_userDate.trim().isEmpty()))
        {
            throw new IllegalStateException("Force flag (-f) requires a target date (-m)");
        }
    }
}
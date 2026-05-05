package batch;

import java.nio.file.Paths;
import java.time.ZonedDateTime;

/**
 * <p>
 * Implements the Builder design pattern for the construction and validation of
 * {@link BatchExecutor} configurations.
 * </p>
 * 
 * <p>
 * This class serves as the central gateway for defining batch parameters. It can produce a
 * self-initiating {@link BatchConsole} via {@link #build()} for CLI applications, or an immutable
 * {@link BatchConfiguration} via {@link #buildConfig()} for custom implementations such as GUI.
 * </p>
 * 
 * <p>
 * <b>Example (CLI):</b>
 * </p>
 * 
 * <pre>
 * <code>
 * BatchConsole batch = new BatchBuilder()
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
 * BatchConfiguration config = new BatchBuilder()
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
    private String bd_sourceDir = BatchExecutor.DEFAULT_SOURCE_DIRECTORY;
    private String bd_prefix = BatchExecutor.DEFAULT_IMAGE_PREFIX;
    private String bd_target = BatchExecutor.DEFAULT_TARGET_DIRECTORY;
    private boolean bd_embedDateTime = false;
    private String bd_userDate = "";
    private boolean bd_force = false;
    private String[] bd_files = new String[0];
    private boolean bd_skipVideoFiles = false;
    private boolean bd_displayMetadata = false;
    private boolean bd_descending = false;
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
     * Sets a specific date and time used to override the {@code Date Taken} attribute.
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
     *        {@code true} to enable forced user date/time in the filename
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder forceDateChange(boolean b)
    {
        bd_force = b;
        return this;
    }

    /**
     * Determines whether the date and time attribute should be prepended to each output file name.
     *
     * @param b
     *        {@code true} to include the date/time in the filename, or {@code false} to omit it
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
     * Configures the sorting order of the processed images to be descending.
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
     * Validates configuration constraints and returns a new {@link BatchConsole} object.
     * 
     * @return a new {@code BatchConsole} instance
     * 
     * @throws IllegalStateException
     *         if validation rules are violated
     * @throws BatchErrorException
     *         if there is an I/O error during the initial source directory scan
     */
    public BatchConsole build() throws BatchErrorException
    {
        BatchConsole console = new BatchConsole(buildConfig());

        console.start();
        return console;
    }

    public MediaMetadataConsole newBuild() throws BatchErrorException
    {
        MediaMetadataConsole console = new MediaMetadataConsole(buildConfig());

        console.run();

        return console;
    }

    /**
     * Validates the current configuration and returns an immutable {@link BatchConfiguration}
     * snapshot.
     * 
     * @return a validated, immutable configuration object
     *
     * @throws IllegalStateException
     *         if validation rules are violated
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
     * This validation ensures that:
     * </p>
     * 
     * <ul>
     * <li>A user-defined date is present when the force flag is enabled.</li>
     * <li>A source directory has been explicitly specified.</li>
     * </ul>
     * 
     * @throws IllegalStateException
     *         if the force flag is enabled without a user date, or if the source directory is
     *         missing
     */
    private void validate()
    {
        if (bd_force && (bd_userDate == null || bd_userDate.trim().isEmpty()))
        {
            throw new IllegalStateException("Force flag (-f) requires a target date (-m)");
        }

        if (bd_sourceDir == null || bd_sourceDir.trim().isEmpty())
        {
            throw new IllegalStateException("Source directory must be specified");
        }
    }
}
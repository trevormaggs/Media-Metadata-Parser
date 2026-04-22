package batch;

/**
 * <p>
 * Implements the Builder design pattern for the step-by-step construction of a
 * {@link BatchExecutor} instance. This class provides setter methods to configure batch parameters,
 * which are subsequently used to create a new {@code BatchExecutor} via the {@link #build()}
 * method.
 * </p>
 *
 * <p>
 * <b>Example:</b>
 * </p>
 * 
 * <pre>
 * <code>
 * BatchExecutor batch = new BatchBuilder()
 * .source("D:\\Media\\Photos")
 * .target("local\\images")
 * .prefix("holiday")
 * .descending(true)
 * .userDate("26 04 2006")
 * .build();
 * </code>
 * </pre>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public final class BatchBuilder2
{
    /* All field variables have a visibility of package-private */
    String bd_sourceDir = BatchExecutor.DEFAULT_SOURCE_DIRECTORY;
    String bd_prefix = BatchExecutor.DEFAULT_IMAGE_PREFIX;
    String bd_target = BatchExecutor.DEFAULT_TARGET_DIRECTORY;
    boolean bd_embedDateTime = false;
    String bd_userDate = "";
    boolean bd_force = false;
    String[] bd_files = new String[0];
    boolean bd_skipVideoFiles = false;
    boolean bd_displayMetadata = false;
    boolean bd_descending = false;
    boolean bd_cleanTargetDir = false;
    boolean bd_debug = false;

    /**
     * Sets the source directory containing the original media files.
     *
     * @param src
     *        the source directory path; must not be null or empty
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 source(final String src)
    {
        if (src == null || src.isEmpty())
        {
            throw new IllegalArgumentException("Source directory cannot be null or empty");
        }

        bd_sourceDir = src;
        return this;
    }

    /**
     * Sets a prefix to be prepended to each output file name.
     *
     * @param prefix
     *        the string to be prepended to every copied file
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 prefix(final String prefix)
    {
        bd_prefix = prefix;
        return this;
    }

    /**
     * Sets the target directory where the processed files will be saved.
     *
     * @param target
     *        the target directory path
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 target(final String target)
    {
        bd_target = target;
        return this;
    }

    /**
     * Determines whether the date and time attribute should be prepended
     * to each output file name.
     *
     * @param emb
     *        {@code true} to include the date/time in the filename, or {@code false} to exclude it
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 embedDateTime(final boolean emb)
    {
        bd_embedDateTime = emb;
        return this;
    }

    /**
     * Sets a specific date and time to initialise or override the {@code Date Taken} attribute.
     *
     * @param dt
     *        the date and time string to be used for processing
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 userDate(final String dt)
    {
        bd_userDate = dt;
        return this;
    }

    /**
     * Forces the system to prioritise the user-defined date, even if existing metadata is present
     * in the file.
     * 
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 forceDateChange()
    {
        bd_force = true;
        return this;
    }

    /**
     * Specifies a defined set of individual files to copy, rather than processing the entire source
     * directory.
     *
     * @param files
     *        a string array of specific filenames to be processed
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 fileSet(final String[] files)
    {
        bd_files = (files == null) ? new String[0] : (String[]) files.clone();
        return this;
    }

    /**
     * Defines whether video files should be ignored or included in the batch.
     *
     * @param video
     *        {@code true} to skip video files, otherwise copy them
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 skipVideo(final boolean video)
    {
        bd_skipVideoFiles = video;
        return this;
    }

    /**
     * Configures the builder to display a list of metadata entries for the processed files.
     *
     * @param meta
     *        {@code true} to enable metadata display
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 showMetadata(final boolean meta)
    {
        bd_displayMetadata = meta;
        return this;
    }

    /**
     * Configures the sorting order of the processed images to be descending.
     *
     * @param desc
     *        {@code true} to sort in descending order, otherwise ascending
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 descending(final boolean desc)
    {
        bd_descending = desc;
        return this;
    }

    /**
     * Enables or disables debug mode for more verbose logging.
     *
     * @param debug
     *        {@code true} to enable debugging
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder2 debug(final boolean debug)
    {
        bd_debug = debug;
        return this;
    }

    /**
     * Constructs the {@link BatchExecutor} with the configured parameters.
     * 
     * @return a new {@code BatchExecutor} instance
     * 
     * @throws BatchErrorException
     *         if the configuration is invalid or an error occurs during initialisation
     */
    public BatchExecutor build() throws BatchErrorException
    {
        if (bd_sourceDir == null)
        {
            throw new BatchErrorException("Source directory must be configured");
        }

        //return new BatchExecutor(this);
        return null;
    }
}
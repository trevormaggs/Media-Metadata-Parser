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
 * parameters. After validation, {@link #build()} returns an immutable {@link BatchConfiguration}
 * that can be used by command-line, GUI, or other application front ends.
 * </p>
 * 
 * <p>
 * <b>Example (CLI):</b>
 * </p>
 * 
 * <pre>
 * BatchConfiguration config = new BatchBuilder()
 *         .source("D:\\Media\\Photos")
 *         .target("local\\images")
 *         .prefix("holiday")
 *         .descending(true)
 *         .userDate("26 04 2006")
 *         .build();
 * </pre>
 * 
 * <p>
 * <b>Example (GUI):</b>
 * </p>
 * 
 * <pre>
 * BatchConfiguration config = new BatchBuilder()
 *         .source(txtField.getText())
 *         .build();
 * </pre>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 24 April 2026
 */
public final class BatchBuilder
{
    private String bd_sourceDir = MediaBatchProcessor.DEFAULT_SOURCE_DIRECTORY;
    private String bd_prefix = MediaBatchProcessor.DEFAULT_IMAGE_PREFIX;
    private String bd_target = MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY;
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
        if (s != null && !s.trim().isEmpty())
        {
            bd_sourceDir = s.trim();
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
        if (t != null && !t.trim().isEmpty())
        {
            bd_target = t.trim();
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
            bd_prefix = p.trim();
        }

        return this;
    }

    /**
     * Sets the user-supplied date to be used during processing.
     *
     * @param d
     *        the date and time string to be used for processing
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder userDate(String d)
    {
        if (d != null)
        {
            bd_userDate = d.trim();
        }

        return this;
    }

    /**
     * Specifies the set of files to process instead of processing every supported file in the
     * source directory.
     *
     * @param f
     *        a string array of specific filenames
     * @return this builder instance to allow method chaining
     */
    public BatchBuilder fileSet(String[] f)
    {
        if (f != null)
        {
            bd_files = new String[f.length];

            for (int i = 0; i < f.length; i++)
            {
                bd_files[i] = (f[i] != null ? f[i].trim() : "");
            }
        }

        return this;
    }

    /**
     * Specifies whether the user-supplied date should replace any existing metadata dates.
     * 
     * @param b
     *        {@code true} to replace existing metadata dates with the user-supplied date
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
     * Specifies whether metadata information should be displayed instead of processing the files.
     *
     * @param b
     *        {@code true} to display metadata instead of processing files
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
     * Enables or disables diagnostic logging.
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
     * Constructs and returns a validated BatchConfiguration.
     *
     * @return the fully initialised BatchConfiguration instance
     * 
     * @throws BatchErrorException
     *         if one or more configuration values fail validation
     */
    public BatchConfiguration build() throws BatchErrorException
    {
        if (bd_force && (bd_userDate == null || bd_userDate.isEmpty()))
        {
            throw new BatchErrorException("Force flag (-f) requires a target date (-m)");
        }

        ZonedDateTime parsedDate = util.SmartDateParser.convertToZonedDateTime(bd_userDate);

        return new BatchConfiguration(
                Paths.get(bd_sourceDir),
                Paths.get(bd_target),
                bd_prefix,
                parsedDate,
                bd_files,
                bd_force,
                bd_embedDateTime,
                bd_skipVideoFiles,
                bd_displayMetadata,
                bd_descending,
                bd_debug);
    }
}
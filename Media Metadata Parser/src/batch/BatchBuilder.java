package batch;

import java.nio.file.Paths;
import java.time.ZonedDateTime;
import util.SmartDateParser;

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
 * @author Trevor Maggs
 * @version 1.0
 * @since 23 April 2026
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

    public BatchBuilder source(String s)
    {
        if (s != null)
        {
            bd_sourceDir = s;
        }

        return this;
    }

    public BatchBuilder target(String t)
    {
        if (t != null)
        {
            bd_target = t;
        }

        return this;
    }

    public BatchBuilder prefix(String p)
    {
        if (p != null)
        {
            bd_prefix = p;
        }

        return this;
    }

    public BatchBuilder userDate(String d)
    {
        if (d != null)
        {
            bd_userDate = d;
        }

        return this;
    }

    public BatchBuilder forceDateChange(boolean f)
    {
        bd_force = f;
        return this;
    }

    public BatchBuilder embedDateTime(boolean e)
    {
        bd_embedDateTime = e;
        return this;
    }

    public BatchBuilder fileSet(String[] f)
    {
        if (f != null)
        {
            bd_files = f;
        }

        return this;
    }

    public BatchBuilder skipVideo(boolean s)
    {
        bd_skipVideoFiles = s;
        return this;
    }

    public BatchBuilder showMetadata(boolean s)
    {
        bd_displayMetadata = s;
        return this;
    }

    public BatchBuilder descending(boolean d)
    {
        bd_descending = d;
        return this;
    }

    public BatchBuilder debug(boolean d)
    {
        bd_debug = d;
        return this;
    }

    /**
     * Validates configuration constraints and returns a new {@link BatchConsole} object.
     * 
     * @return a new {@code BatchConsole} instance
     * 
     * @throws IllegalStateException
     *         if validation rules are violated
     * @throws RuntimeException
     *         if the initial source directory scan fails
     */
    public BatchConsole build()
    {
        validate();

        ZonedDateTime parsedDate = SmartDateParser.convertToZonedDateTime(bd_userDate);
        BatchConfiguration settings = new BatchConfiguration(Paths.get(bd_sourceDir), Paths.get(bd_target), bd_prefix,
                parsedDate, bd_files, bd_force, bd_embedDateTime, bd_skipVideoFiles,
                bd_displayMetadata, bd_descending, bd_debug);

        try
        {
            BatchConsole console = new BatchConsole(settings);
            console.start();

            return console;
        }

        catch (Exception exc)
        {
            throw new RuntimeException("Failed to scan source directory [" + exc.getMessage() + "]", exc);
        }
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
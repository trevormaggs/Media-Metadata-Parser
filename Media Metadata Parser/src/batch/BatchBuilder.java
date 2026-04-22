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
 * @since 13 August 2025
 */
public final class BatchBuilder
{
    /* All field variables have a visibility of package-private */
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

    public BatchBuilder source(String src)
    {
        if (src != null)
        {
            bd_sourceDir = src;
        }

        return this;
    }

    public BatchBuilder target(String tgt)
    {
        if (tgt != null)
        {
            bd_target = tgt;
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

    public BatchBuilder userDate(String datestr)
    {
        if (datestr != null)
        {
            bd_userDate = datestr;
        }

        return this;
    }

    public BatchBuilder forceDateChange()
    {
        bd_force = true;
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

    public BatchConsole build()
    {
        validate();

        ZonedDateTime parsedDate = SmartDateParser.convertToZonedDateTime(bd_userDate);
        BatchSettings settings = new BatchSettings(Paths.get(bd_sourceDir), Paths.get(bd_target), bd_prefix,
                parsedDate, bd_files, bd_force, bd_embedDateTime, bd_skipVideoFiles,
                bd_displayMetadata, bd_descending, bd_debug);

        try
        {
            return new BatchConsole(settings);
        }

        catch (BatchErrorException e)
        {
            throw new RuntimeException("Initialisation failed: " + e.getMessage(), e);
        }
    }

    private void validate()
    {
        // Check if force is requested without a valid date string
        if (bd_force && (bd_userDate == null || bd_userDate.trim().isEmpty()))
        {
            throw new IllegalStateException("Force flag (-f) requires a target date (-m)");
        }

        // Optional: Ensure source directory actually exists before starting the batch
        if (bd_sourceDir == null || bd_sourceDir.trim().isEmpty())
        {
            throw new IllegalStateException("Source directory must be specified");
        }
    }
}
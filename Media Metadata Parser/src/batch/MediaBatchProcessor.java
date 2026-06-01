package batch;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import heif.HeifDatePatcher;
import jpg.JpgDatePatcher;
import logger.LogFactory;
import png.PngDatePatcher;
import progressbar.ProgressListener;
import tif.TiffDatePatcher;
import util.SystemInfo;
import webp.WebPDatePatcher;

/**
 * Automates the batch processing of image files by copying, renaming, and chronologically sorting
 * them, typically based on their {@code DateTimeOriginal} EXIF metadata.
 * 
 * <p>
 * This processor implements a "surgical" strategy: it never modifies source files. Instead, it
 * creates a renamed copy in the target directory and applies binary patches to the metadata
 * segments of the copy to ensure chronological integrity across JPEG, TIFF, PNG, WebP, and HEIF
 * formats.
 * </p>
 * 
 * <p>
 * A built-in 10-second offset is applied to user-defined dates to prevent metadata collisions and
 * ensure stable sorting in downstream applications (like Windows Photos or Apple Photos).
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 5 May 2026
 */
public final class MediaBatchProcessor
{
    private static final LogFactory LOGGER = LogFactory.getLogger(MediaBatchProcessor.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("ddMMMyyyy");
    private static final long TEN_SECOND_OFFSET = 10L;
    private static final FileVisitor<Path> DELETE_VISITOR;
    private final List<ProgressListener> listeners;
    private final BatchConfiguration config;
    private final MetadataScanner scanner;

    public static final String DEFAULT_SOURCE_DIRECTORY = ".";
    public static final String DEFAULT_TARGET_DIRECTORY = "IMAGEDIR";
    public static final String DEFAULT_IMAGE_PREFIX = "image";

    static
    {
        DELETE_VISITOR = new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                if (exc == null)
                {
                    Files.delete(dir);
                }

                else
                {
                    throw exc;
                }

                return FileVisitResult.CONTINUE;
            }
        };
    }

    /**
     * Constructs a new Processor using the specified configuration needed for scan.
     *
     * @param config
     *        the configuration settings used to initialise the executor
     */
    public MediaBatchProcessor(BatchConfiguration config)
    {
        this.config = config;
        this.scanner = new MetadataScanner(config);
        this.listeners = new ArrayList<ProgressListener>();
    }

    /**
     * Registers a listener used to respond to progress updates during the batch execution.
     *
     * @param listener
     *        the progress listener to add
     */
    public void addProgressListener(ProgressListener listener)
    {
        if (listener != null)
        {
            this.listeners.add(listener);
        }
    }

    /**
     * Begins the batch processing workflow by preparing the target directory, setting up logging,
     * and processing the specified source files or directory.
     * 
     * <p>
     * Note, this method is final to ensure no subclass accidentally overrides the defined critical
     * logic.
     * </p>
     *
     * @throws BatchErrorException
     *         if an I/O error has occurred during directory preparation or file processing
     */
    public final void execute() throws BatchErrorException
    {
        prepareTargetDirectory();
        startLogging();
        scanner.start();

        int index = 1;
        int total = scanner.getRecordCount();

        LOGGER.info("Starting batch process for [" + total + "] files...");

        for (MediaRecord record : scanner)
        {
            if (record.isVideoFormat() && config.isSkipVideo())
            {
                LOGGER.info("File [" + record.getPath() + "] skipped");
                continue;
            }

            processRecord(record, index, total);

            /* Iterates through registered listeners to broadcast the current progress. */
            for (ProgressListener listener : listeners)
            {
                listener.onProgressUpdate(index, total);
            }

            index++;
        }

        LOGGER.info("Batch processing completed successfully");
    }

    /**
     * Handles the end-to-end processing of a single media record.
     * 
     * <p>
     * The process follows a strict "copy-then-patch" sequence:
     * </p>
     * 
     * <ol>
     * <li>Calculate the effective timestamp (Natural vs. User-defined).</li>
     * <li>Generate a new filename based on configuration.</li>
     * <li>Copy the source file to the target location.</li>
     * <li>Apply binary metadata patches to the <b>copy</b> if forced.</li>
     * <li>Update file-system attributes (Last Modified Time).</li>
     * </ol>
     * 
     * @param record
     *        the media file record to process
     * @param index
     *        the current position in the batch
     * @param total
     *        the total number of files in the batch
     * @throws BatchErrorException
     *         if file I/O or metadata patching fails
     */
    private void processRecord(MediaRecord record, int index, int total) throws BatchErrorException
    {
        try
        {
            FileTime effectiveTime = calculateEffectiveTime(record, index);
            String newName = generateTargetName(record, index, effectiveTime);
            Path targetPath = config.getTarget().resolve(newName);

            Files.copy(record.getPath(), targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

            if (record.isMetadataEmpty())
            {
                LOGGER.warn("File [" + record.getPath() + "] contains no metadata. Only file dates were updated");
            }

            else if (config.isForceDateChange())
            {
                if (record.isTIF())
                {
                    TiffDatePatcher.patchAllDates(targetPath, effectiveTime, true);
                }

                else if (record.isJPG())
                {
                    JpgDatePatcher.patchAllDates(targetPath, effectiveTime, false);
                }

                else if (record.isPNG())
                {
                    PngDatePatcher.patchAllDates(targetPath, effectiveTime, false);
                }

                else if (record.isWebP())
                {
                    WebPDatePatcher.patchAllDates(targetPath, effectiveTime, false);
                }

                else if (record.isHEIC())
                {
                    HeifDatePatcher.patchAllDates(targetPath, effectiveTime, false);
                }
            }

            BasicFileAttributeView attr = Files.getFileAttributeView(targetPath, BasicFileAttributeView.class);
            attr.setTimes(effectiveTime, effectiveTime, effectiveTime);

            LOGGER.info(String.format("[%d/%d] Processed: %s -> %s", index, total, record.getPath().getFileName(), newName));
        }

        catch (IOException exc)
        {
            String msg = "I/O error detected with [" + record.getPath().getFileName() + "]";
            LOGGER.error(msg);

            throw new BatchErrorException(msg, exc);
        }
    }

    /**
     * Determines the final timestamp for the record, applying increments if the date is
     * user-defined.
     * 
     * @param record
     *        the media record being processed
     * @param index
     *        the current index used to calculate the 10-second offset
     * @return the calculated FileTime for metadata and file-system updates
     */
    private FileTime calculateEffectiveTime(MediaRecord record, int index)
    {
        if (config.isForceDateChange() && config.getUserDate() != null)
        {
            long secondsToAdd = (index - 1) * TEN_SECOND_OFFSET;
            return FileTime.from(config.getUserDate().plusSeconds(secondsToAdd).toInstant());
        }

        return record.getNaturalDate();
    }

    /**
     * Constructs the target filename using prefix, date/time embedding, and index padding.
     * 
     * @param record
     *        the media record
     * @param index
     *        the batch index for numerical padding, such as 001, 002, etc
     * @param time
     *        the timestamp to embed if enabled
     * @return a formatted string representing the new filename
     */
    private String generateTargetName(MediaRecord record, int index, FileTime time)
    {
        StringBuilder sb = new StringBuilder();

        if (config.getPrefix() != null && !config.getPrefix().isEmpty())
        {
            sb.append(config.getPrefix()).append("_");
        }

        if (config.isEmbedDateTime())
        {
            ZonedDateTime zdt = time.toInstant().atZone(ZoneId.systemDefault());
            sb.append(zdt.format(DTF)).append("_");
        }

        sb.append(String.format("%03d", index));

        String ext = record.getMediaFormat().getFileExtensionName();

        if (!ext.startsWith("."))
        {
            sb.append(".");
        }

        sb.append(ext);

        return sb.toString();
    }

    /**
     * Prepares the target directory by ensuring it exists and is empty.
     * 
     * <p>
     * Safety Check: Throws an exception if the target is identical to the source to prevent
     * accidental data deletion during the destructive clean phase.
     * </p>
     * 
     * @throws BatchErrorException
     *         if source/target are identical or directory creation fails
     */
    private void prepareTargetDirectory() throws BatchErrorException
    {
        try
        {
            if (Files.exists(config.getTarget()))
            {
                if (Files.isSameFile(config.getSource(), config.getTarget()))
                {
                    throw new BatchErrorException("Target directory cannot be the same as source directory");
                }

                // Destructive clean to ensure a fresh batch environment
                Files.walkFileTree(config.getTarget(), DELETE_VISITOR);
            }

            Files.createDirectories(config.getTarget());
        }

        catch (IOException exc)
        {
            throw new BatchErrorException("Cannot prepare target directory [" + config.getTarget() + "] due to an I/O error", exc);
        }
    }

    /**
     * Begins the logging system and writes configuration details to a log file. This method is for
     * internal setup and is not intended for external use.
     *
     * @throws BatchErrorException
     *         if the logging service cannot be established
     */
    private void startLogging() throws BatchErrorException
    {
        try
        {
            String logName = "batchlog_" + SystemInfo.getHostname() + ".log";
            Path logPath = config.getTarget().resolve(logName);

            LOGGER.configure(logPath.toString());
            LOGGER.setDebug(config.isDebug());
            LOGGER.setTrace(false);

            LOGGER.info("MediaBatchProcessor Initialised.");
            LOGGER.info("Source: " + config.getSource().toAbsolutePath());
            LOGGER.info("Target: " + config.getTarget().toAbsolutePath());

            String sortOrder = config.isDescending() ? "descending" : "ascending";
            LOGGER.info("Sorted scanned images in " + sortOrder + " order");
        }

        catch (IOException exc)
        {
            throw new BatchErrorException("Unable to start logging", exc);
        }
    }
}
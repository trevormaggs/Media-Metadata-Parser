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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * Automates the batch processing of media files by copying, renaming, and chronologically sorting
 * them according to metadata timestamps.
 *
 * <p>
 * This processor implements a "surgical" strategy. It never modifies source files. Instead, it
 * creates a renamed copy in the target directory and applies binary patches to the metadata
 * segments of the copy to ensure chronological integrity across JPEG, TIFF, DNG, PNG, WebP, and
 * HEIF formats.
 * </p>
 *
 * <p>
 * A built-in 10-second offset is applied to user-defined dates to prevent metadata collisions and
 * ensure stable sorting in downstream applications, such as Windows Photos or Apple Photos.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 5 May 2026
 */
public final class MediaBatchProcessor
{
    public static final String DEFAULT_SOURCE_DIRECTORY = ".";
    public static final String DEFAULT_TARGET_DIRECTORY = "IMAGEDIR";
    public static final String DEFAULT_IMAGE_PREFIX = "image";
    private static final LogFactory LOGGER = LogFactory.getLogger(MediaBatchProcessor.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("ddMMMyyyy");
    private static final long TEN_SECOND_OFFSET = 10L;
    private volatile boolean cancelled = false;
    private final List<ProgressListener> listeners;
    private final BatchConfiguration config;
    private final MetadataScanner scanner;
    private BatchStatistics stats;

    /**
     * Constructs a batch processor using the specified configuration.
     *
     * @param config
     *        the validated configuration used for batch execution
     */
    public MediaBatchProcessor(BatchConfiguration config)
    {
        this.config = config;
        this.scanner = new MetadataScanner(config);
        this.listeners = new ArrayList<>();
    }

    /**
     * Registers a progress listener to receive updates during both scanning and processing
     * execution phases.
     *
     * @param listener
     *        the progress listener to register
     */
    public void addProgressListener(ProgressListener listener)
    {
        if (listener != null)
        {
            this.listeners.add(listener);
            this.scanner.addProgressListener(listener);
        }
    }

    /**
     * Signals the scanner to abort execution at the earliest opportunity.
     */
    public void cancel()
    {
        cancelled = true;

        if (scanner != null)
        {
            scanner.cancel();
        }
    }

    /**
     * Returns whether execution cancellation was requested.
     *
     * @return {@code true} if cancellation has been requested or the current thread has been
     *         interrupted, otherwise {@code false}
     */
    public boolean isCancelled()
    {
        return (cancelled || Thread.currentThread().isInterrupted());
    }

    public BatchStatistics getStatistics()
    {
        return (stats == null ? new BatchStatistics(0, 0, 0L) : stats);
    }

    /**
     * Begins the batch-processing workflow by preparing the target directory, initialising logging,
     * and processing the configured media files.
     *
     * <p>
     * By design, this method is final to ensure that subclasses cannot accidentally override the
     * core processing workflow.
     * </p>
     *
     * <p>
     * <b>Note:</b> Call {@link #addProgressListener} prior to calling this method if you wish to
     * monitor execution progress via a progress bar or status label.
     * </p>
     *
     * @throws BatchErrorException
     *         if an I/O error occurs during directory preparation or file processing
     */
    public final void execute() throws BatchErrorException
    {
        try
        {
            prepareTargetDirectory();
            startLogging();

            scanner.start();
            resetListeners();

            int count = 1;
            int processedCount = 1;
            long totalTargetSize = 0L;
            int totalSourceFiles = scanner.getRecordCount();

            if (totalSourceFiles > 0)
            {
                LOGGER.info("Starting batch process for [" + totalSourceFiles + "] files...");

                for (MediaRecord record : scanner)
                {
                    if (isCancelled())
                    {
                        LOGGER.warn("Batch process was cancelled by user after processing " + (count - 1) + " files.");
                        return;
                    }

                    if (record.isVideoFormat() && config.isSkipVideo())
                    {
                        LOGGER.info("File [" + record.getPath().getFileName() + "] skipped");
                    }

                    else
                    {
                        Path pfile = processRecord(record, processedCount++, totalSourceFiles);
                        totalTargetSize += Files.size(pfile);
                    }

                    /* Notify all registered listeners */
                    for (ProgressListener listener : listeners)
                    {
                        listener.onProgressUpdate(count, totalSourceFiles);
                    }

                    count++;
                }

                LOGGER.info("Batch processing completed successfully");
            }

            else
            {
                LOGGER.info("No valid media files found in [" + config.getSource() + "]");
            }

            stats = new BatchStatistics(totalSourceFiles, processedCount - 1, totalTargetSize);
        }

        catch (IOException exc)
        {
            LOGGER.error("I/O error during batch execution", exc);
            throw new BatchErrorException("Failed to complete batch processing", exc);
        }

        finally
        {
            LogFactory.close();
        }
    }

    /**
     * Resets internal progress state across all registered listeners.
     */
    private void resetListeners()
    {
        for (ProgressListener listener : listeners)
        {
            listener.reset();
        }
    }

    /**
     * Handles the end-to-end processing of a single media record.
     *
     * <p>
     * The process follows a strict "copy-then-patch" sequence:
     * </p>
     *
     * <ol>
     * <li>Calculate the effective timestamp (natural or user-defined).</li>
     * <li>Generate a new filename based on the configuration.</li>
     * <li>Copy the source file to the target location.</li>
     * <li>Apply binary metadata patches to the copied file, if required.</li>
     * <li>Update file-system timestamps.</li>
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
    private Path processRecord(MediaRecord record, int index, int total) throws BatchErrorException
    {
        Path targetPath = null;

        try
        {
            FileTime effectiveTime = calculateEffectiveTime(record, index);
            String newName = generateTargetName(record, index, effectiveTime);
            targetPath = config.getTarget().resolve(newName);

            Files.copy(record.getPath(), targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

            if (!isCancelled())
            {
                if (record.isMetadataEmpty())
                {
                    LOGGER.warn("File [" + record.getPath() + "] contains no metadata. Only file dates were updated");
                }

                else if (config.isForceDateChange())
                {
                    // TODO: May need to add one for DNG

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

            else
            {
                Files.deleteIfExists(targetPath);
                LOGGER.warn("Processing interrupted for [" + record.getPath().getFileName() + "]. Cleaned up temporary target file.");
            }
        }

        catch (IOException exc)
        {
            /* Clean up partial or corrupt file if an error occurred during copy or patch */
            if (targetPath != null)
            {
                try
                {
                    Files.deleteIfExists(targetPath);
                }

                catch (IOException exc2)
                {
                    // Just pass through
                }
            }

            String msg = "I/O error detected with [" + record.getPath().getFileName() + "]";
            LOGGER.error(msg, exc);

            throw new BatchErrorException(msg, exc);
        }

        return targetPath;
    }

    /**
     * Determines the effective timestamp for a media record.
     *
     * <p>
     * If a user-defined date is configured, a fixed offset is applied based on the record position
     * to ensure unique chronological ordering.
     * </p>
     *
     * @param record
     *        the media record being processed
     * @param index
     *        the current index used to calculate the 10-second offset
     * @return the calculated {@link FileTime} used for metadata and file-system updates
     */
    private FileTime calculateEffectiveTime(MediaRecord record, int index)
    {
        if (config.isForceDateChange() && config.getUserDate() != null)
        {
            long extraSeconds = (index - 1) * TEN_SECOND_OFFSET;
            return FileTime.from(config.getUserDate().plusSeconds(extraSeconds).toInstant());
        }

        return record.getNaturalDate();
    }

    /**
     * Constructs the target filename using prefix, date/time embedding, and index padding.
     *
     * @param record
     *        the media record
     * @param index
     *        the batch index used for numerical padding (for example, 001, 002, and so on)
     * @param time
     *        the timestamp to embed if enabled
     * @return the generated filename for the copied media file
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
     * An exception is thrown if the target directory is identical to the source directory to
     * prevent accidental data loss during cleanup.
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

                FileVisitor<Path> deleteVisitor = new SimpleFileVisitor<Path>()
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
                        if (exc != null)
                        {
                            throw exc;
                        }

                        /*
                         * No need to delete the target directory since we will need it anyway.
                         * Old sub-directotroes within this target directory are deleted.
                         */
                        else if (!dir.equals(config.getTarget()))
                        {
                            Files.delete(dir);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                };

                Files.walkFileTree(config.getTarget(), deleteVisitor);
            }

            else
            {
                Files.createDirectories(config.getTarget());
            }
        }

        catch (IOException exc)
        {
            throw new BatchErrorException("Cannot prepare target directory [" + config.getTarget() + "] due to an I/O error", exc);
        }
    }

    /**
     * Initialises the logging system and records the active configuration.
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

            LogFactory.configure(logPath.toString());
            LogFactory.setDebug(config.isDebug());
            LogFactory.setTrace(true);

            LOGGER.info("MediaBatchProcessor initialised");
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
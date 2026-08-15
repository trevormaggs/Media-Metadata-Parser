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
import common.PropertyListener;
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
 * them according to metadata timestamps. This is the engine core of the batch processing
 * functionality.
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
    private static final LogFactory LOGGER = LogFactory.getLogger(MediaBatchProcessor.class);
    private static final DateTimeFormatter EMBED_DTF = DateTimeFormatter.ofPattern("ddMMMyyyy");
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final long TEN_SECOND_OFFSET = 10L;
    private volatile boolean cancelled = false;
    private final List<ProgressListener> listeners;
    private final MetadataScanner scanner;
    private final BatchConfiguration config;
    private PropertyListener fileUpdateListener;
    public static final String DEFAULT_IMAGE_PREFIX = "image";
    public static final String DEFAULT_SOURCE_DIRECTORY = ".";
    public static final String DEFAULT_TARGET_DIRECTORY = "IMAGEDIR";

    /**
     * Constructs a batch processor using the specified configuration.
     *
     * @param config
     *        the validated configuration used for batch execution
     */
    public MediaBatchProcessor(BatchConfiguration config)
    {
        this.config = config;
        this.listeners = new ArrayList<>();
        this.scanner = new MetadataScanner(config);
    }

    /**
     * Registers a progress listener to receive updates during both scanning and processing
     * execution phases. You may add multiple listeners.
     *
     * @param listener
     *        the progress listener to register
     */
    public void addProgressListener(ProgressListener listener)
    {
        if (listener != null)
        {
            listeners.add(listener);
            scanner.addProgressListener(listener);
        }
    }

    /**
     * Sets the property listener to receive individual file processing metrics. There can only be
     * one listener.
     *
     * @param listener
     *        the property listener to set
     */
    public void setPropertyListener(PropertyListener listener)
    {
        fileUpdateListener = listener;
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

    /**
     * Begins the batch-processing workflow by preparing the target directory, initialising logging,
     * and processing the configured media files.
     *
     * <p>
     * <b>Note:</b> Call {@link #addProgressListener} prior to calling this method if you wish to
     * monitor execution progress via a progress bar or a type of console update indicator.
     * </p>
     *
     * @return the {@link BatchMetrics} summarising files scanned, files processed, and total bytes
     *         (cumulative)
     *
     * @throws BatchErrorException
     *         if an I/O error occurs during directory preparation or file processing
     */
    public BatchMetrics execute() throws BatchErrorException
    {
        int count = 1;
        int processedCount = 0;
        int totalSourceFiles = 0;
        long totalTargetSize = 0L;

        try
        {
            prepareTargetDirectory();
            startLogging();
            scanner.start();
            resetListeners();// Reset progress bar for the next task: processing

            totalSourceFiles = scanner.getRecordCount();
            LOGGER.info("Total number of source files scanned [" + totalSourceFiles + "]");

            if (totalSourceFiles > 0)
            {
                for (MediaRecord record : scanner)
                {
                    LOGGER.info("Processing file: " + record.getPath().getFileName());

                    if (isCancelled())
                    {
                        LOGGER.warn("Batch process was cancelled by user after processing " + processedCount + " files");
                        break;
                    }

                    int index = processedCount + 1;
                    FileTime effectiveTime = calculateEffectiveTime(record, index);
                    String targetName = generateTargetName(record, index, effectiveTime);
                    long targetSize = processRecord(record, effectiveTime, targetName);

                    if (targetSize != -1L)
                    {
                        String formattedDate = effectiveTime.toInstant().atZone(ZoneId.systemDefault()).format(DTF);

                        processedCount++;
                        totalTargetSize += targetSize;

                        LOGGER.info(String.format("[File %d/%d] Processed: %s -> %s [Effective date/time: %s]", index, totalSourceFiles, record.getPath().getFileName(), targetName, formattedDate));
                    }

                    if (fileUpdateListener != null)
                    {
                        fileUpdateListener.accept(BatchEventType.FILE_PROCESSED.getKey(), new BatchProcessEvent(record, targetName, targetSize));
                    }

                    /* Notify progress listeners based on overall loop count */
                    for (ProgressListener listener : listeners)
                    {
                        listener.onProgressUpdate(count, totalSourceFiles);
                    }

                    count++;
                }

                LOGGER.info("Batch processing completed");
            }

            else
            {
                LOGGER.info("No valid media files found in [" + config.getSource() + "]");
            }
        }

        finally
        {
            LogFactory.close();
        }

        return new BatchMetrics(scanner.getTotalScannedCount(), processedCount, totalTargetSize);
    }

    /**
     * Handles the end-to-end processing of a single media record.
     *
     * <p>
     * The process follows a strict "copy-then-patch" sequence:
     * </p>
     *
     * <ol>
     * <li>Copy the source file to the target location.</li>
     * <li>Apply binary metadata patches to the copied file using the calculated timestamp, if
     * required.</li>
     * <li>Update file-system timestamps.</li>
     * </ol>
     *
     * @param record
     *        the media file record to process
     * @param effectiveTime
     *        the calculated effective timestamp
     * @param newName
     *        the target filename to use for output
     * @return the file size of the newly generated file, or {@code -1} if processing was cancelled
     *         before completion
     * 
     * @throws BatchErrorException
     *         if file I/O or metadata patching fails
     */
    private long processRecord(MediaRecord record, FileTime effectiveTime, String newName) throws BatchErrorException
    {
        Path targetPath = null;

        try
        {
            targetPath = config.getTarget().resolve(newName);
            Files.copy(record.getPath(), targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

            if (!isCancelled())
            {
                if (record.isMetadataEmpty())
                {
                    LOGGER.warn("File [" + record.getPath() + "] contains no embedded metadata. Only system file dates were updated");
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

                BasicFileAttributeView view = Files.getFileAttributeView(targetPath, BasicFileAttributeView.class);

                if (view != null)
                {
                    view.setTimes(effectiveTime, effectiveTime, effectiveTime);
                }

                return view.readAttributes().size();
            }

            else
            {
                Files.deleteIfExists(targetPath);
                LOGGER.warn("Processing interrupted for [" + record.getPath().getFileName() + "]. Cleaned up temporary target file");
            }
        }

        catch (IOException exc1)
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
            throw new BatchErrorException(msg, exc1);
        }

        return -1L;
    }

    /**
     * Determines the effective timestamp for a media record.
     *
     * <p>
     * If date changes are forced, the batch configuration guarantees that a user-defined date is
     * available. A fixed 10-second offset is then applied based on the record position to ensure
     * unique chronological ordering.
     * </p>
     *
     * <p>
     * Otherwise, the media record's natural timestamp is returned. The natural timestamp is
     * determined by the metadata date when available, if no metadata date exists, the user-defined
     * date is used when configured, otherwise the file's last modified time is used.
     * </p>
     *
     * @param record
     *        the media record being processed
     * @param index
     *        the current index used to calculate the 10-second offset
     * @return the effective {@link FileTime} used for metadata and file-system updates
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
            sb.append(zdt.format(EMBED_DTF)).append("_");
        }

        sb.append(String.format("%04d", index));

        String ext = record.getMediaFormat().getFileExtensionName();

        if (!ext.startsWith("."))
        {
            sb.append(".");
        }

        sb.append(ext);

        return sb.toString();
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
                         * Old sub-directories within this target directory are deleted.
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
            LogFactory.setTrace(config.isTrace());

            LOGGER.info(this.getClass().getSimpleName() + " initialised");
            LOGGER.info("Source: " + config.getSource().toAbsolutePath());
            LOGGER.info("Target: " + config.getTarget().toAbsolutePath());
            LOGGER.info("Scanned images will be sorted in " + (config.isDescending() ? "descending" : "ascending") + " order");

            if (config.isForceDateChange() && config.getUserDate() != null)
            {
                String dtf = config.getUserDate().format(DTF);
                LOGGER.info("User-defined date override received [" + dtf + "]");
            }

            if (config.isDebug())
            {
                LOGGER.info("Debugging is enabled");
            }

            if (config.isTrace())
            {
                LOGGER.info("Trace logging is enabled");
            }
        }

        catch (IOException exc)
        {
            throw new BatchErrorException("Unable to start logging", exc);
        }
    }
}
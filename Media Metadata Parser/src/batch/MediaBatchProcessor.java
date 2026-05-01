package batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import logger.LogFactory;
import util.SystemInfo;

/**
 * Executes the "surgical" batch processing of media files.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 1 May 2026
 */
public final class MediaBatchProcessor
{
    private static final LogFactory LOGGER = LogFactory.getLogger(MediaBatchProcessor.class);
    private final BatchConfiguration config;
    private final MetadataScanner scanner;
    private final DateTimeFormatter nameFormatter;

    public MediaBatchProcessor(BatchConfiguration config, MetadataScanner scanner)
    {
        this.config = config;
        this.scanner = scanner;
        this.nameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    }

    public void execute() throws BatchErrorException
    {
        prepareTargetDirectory();
        startLogging();

        int index = 1;
        int total = scanner.getRecordCount();

        LOGGER.info("Starting batch process for " + total + " files...");

        for (MediaRecord record : scanner)
        {
            processRecord(record, index++, total);
        }

        LOGGER.info("Batch processing completed successfully.");
    }

    /**
     * Unifies the processing logic for a single file.
     */
    private void processRecord(MediaRecord record, int index, int total)
    {
        try
        {
            // 1. Determine Effective Time (Natural or Forced)
            FileTime effectiveTime = calculateEffectiveTime(record, index);

            // 2. Generate target path
            String newName = generateTargetName(record, index, effectiveTime);
            Path targetPath = config.getTarget().resolve(newName);

            // 3. I/O Operations
            Files.copy(record.getPath(), targetPath);

            // 4. TODO: Surgical Binary Patching
            // (e.g., using a separate utility to write effectiveTime into EXIF)

            // 5. Update File System Attributes
            Files.setLastModifiedTime(targetPath, effectiveTime);

            LOGGER.info(String.format("[%d/%d] Processed: %s -> %s", index, total, record.getPath(), newName));
        }
        catch (IOException exc)
        {
            LOGGER.error("Processing failed: " + record.getPath().getFileName() + " -> " + exc.getMessage());
        }
    }

    /**
     * Determines if we use the file's natural date or the user-forced sequence.
     */
    private FileTime calculateEffectiveTime(MediaRecord record, int index)
    {
        if (config.isForceDateChange() && config.getUserDate() != null)
        {
            // 1. Take the User's ZonedDateTime
            // 2. Add (index - 1) seconds to create the sequence
            // 3. Convert directly to an Instant, then to FileTime
            return FileTime.from(config.getUserDate().plusSeconds(index - 1).toInstant());
        }

        return record.getNaturalDate();
    }

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
            sb.append(zdt.format(nameFormatter)).append("_");
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

    private void prepareTargetDirectory() throws BatchErrorException
    {
        try
        {
            if (Files.exists(config.getTarget()) && Files.isSameFile(config.getSource(), config.getTarget()))
            {
                throw new BatchErrorException("Target directory cannot be the same as Source directory.");
            }
            Files.createDirectories(config.getTarget());
        }
        catch (IOException exc)
        {
            throw new BatchErrorException("Target directory preparation failed: " + config.getTarget(), exc);
        }
    }

    private void startLogging() throws BatchErrorException
    {
        try
        {
            String logName = "batchlog_" + SystemInfo.getHostname() + ".log";
            Path logPath = config.getTarget().resolve(logName);
            LOGGER.configure(logPath.toString());

            LOGGER.info("MediaBatchProcessor Initialised.");
            LOGGER.info("Source: " + config.getSource().toAbsolutePath());
            LOGGER.info("Target: " + config.getTarget().toAbsolutePath());
        }
        catch (IOException exc)
        {
            throw new BatchErrorException("Logging system failed to start.", exc);
        }
    }
}
package batch;

/**
 * Stores a snapshot of numbers summarising a single batch run.
 *
 * <p>
 * This class is immutable and thread-safe. It keeps track of how many files were discovered during
 * scanning, how many were successfully processed, and the total disk space occupied by the created
 * target files.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 12 August 2026
 */
public final class BatchMetrics
{
    private final int scanned;
    private final int processed;
    private final long targetBytes;

    /**
     * Creates a new summary of batch operations.
     *
     * @param filesScannedCount
     *        the total number of files found while scanning the source folder
     * @param filesProcessedCount
     *        the number of files successfully converted or created
     * @param totalTargetSizeBytes
     *        the combined size of all newly generated target files, in bytes
     */
    public BatchMetrics(int filesScannedCount, int filesProcessedCount, long totalTargetSizeBytes)
    {
        this.scanned = filesScannedCount;
        this.processed = filesProcessedCount;
        this.targetBytes = totalTargetSizeBytes;
    }

    /**
     * Gets the total number of source files discovered during the initial scan.
     *
     * @return the count of scanned files
     */
    public int getScanned()
    {
        return scanned;
    }

    /**
     * Gets the number of files that were successfully processed.
     *
     * @return the count of completed files
     */
    public int getProcessed()
    {
        return processed;
    }

    /**
     * Gets the combined size of all generated target files in bytes.
     *
     * @return the total target size in bytes
     */
    public long getTargetBytes()
    {
        return targetBytes;
    }

    /**
     * Converts the total target size from bytes to megabytes (MB) for easier display.
     *
     * @return the total target size in megabytes
     */
    public double getTotalTargetSizeMB()
    {
        return targetBytes / (1024.0 * 1024.0);
    }

    /**
     * Calculates how many files were skipped or failed by subtracting the processed count from the
     * scanned count.
     *
     * @return the number of unprocessed or failed files, guaranteed not to be negative
     */
    public int getFilesSkippedCount()
    {
        return Math.max(0, scanned - processed);
    }
}
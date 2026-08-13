package batch;

/**
 * Stores summary statistics produced during a batch processing operation.
 *
 * <p>
 * This immutable class records the number of source files processed, the number of target files
 * created, and the total size of the target files.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 4 August 2026
 */
public final class BatchStatistics
{
    private final int sourceFilesCount;
    private final int targetFilesCount;
    private final long totalTargetSizeBytes;

    /**
     * Creates a new set of batch processing statistics.
     *
     * @param sourceFilesCount
     *        the number of source files processed
     * @param targetFilesCount
     *        the number of target files created
     * @param totalTargetSizeBytes
     *        the combined size of all target files, in bytes
     */
    public BatchStatistics(int sourceFilesCount, int targetFilesCount, long totalTargetSizeBytes)
    {
        this.sourceFilesCount = sourceFilesCount;
        this.targetFilesCount = targetFilesCount;
        this.totalTargetSizeBytes = totalTargetSizeBytes;
    }

    /**
     * Returns the number of source files processed.
     *
     * @return the source file count
     */
    public int getSourceFilesCount()
    {
        return sourceFilesCount;
    }

    /**
     * Returns the number of target files created.
     *
     * @return the target file count
     */
    public int getTargetFilesCount()
    {
        return targetFilesCount;
    }

    /**
     * Returns the combined size of all target files in bytes.
     *
     * @return the total target file size, in bytes
     */
    public long getTotalTargetSizeBytes()
    {
        return totalTargetSizeBytes;
    }

    /**
     * Returns the combined size of all target files in megabytes.
     *
     * @return the total target file size, in megabytes
     */
    public double getTotalTargetSizeMB()
    {
        return totalTargetSizeBytes / (1024.0 * 1024.0);
    }
}
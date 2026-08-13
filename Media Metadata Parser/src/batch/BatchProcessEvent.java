package batch;

/**
 * Immutable domain event representing the execution outcome of processing a single media record.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2026
 */
public final class BatchProcessEvent
{
    private final MediaRecord record;
    private final String targetName;
    private final long targetSize;

    /**
     * Constructs a batch process event containing processing details for a record.
     *
     * @param record
     *        the source media record being processed
     * @param targetName
     *        the generated target file name
     * @param targetSize
     *        the size of the generated target file in bytes, or {@code -1} if processing failed or
     *        was skipped
     */
    public BatchProcessEvent(MediaRecord record, String targetName, long targetSize)
    {
        this.record = record;
        this.targetName = targetName;
        this.targetSize = targetSize;
    }

    /**
     * Returns the simple file name of the source media file.
     *
     * @return the source file name
     */
    public String getSourceName()
    {
        return record.getPath().getFileName().toString();
    }

    /**
     * Returns the name assigned to the generated target file.
     *
     * @return the target file name
     */
    public String getTargetName()
    {
        return targetName;
    }

    /**
     * Returns the size of the target file in bytes.
     *
     * @return the target file size in bytes, or {@code -1} if processing was unsuccessful
     */
    public long getTargetSize()
    {
        return targetSize;
    }

    /**
     * Returns whether the processing operation completed successfully.
     *
     * @return {@code true} if the target file was created successfully, otherwise {@code false}
     */
    public boolean isSuccess()
    {
        return targetSize != -1;
    }
}
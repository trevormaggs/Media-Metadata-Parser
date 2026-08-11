package batch;

/**
 * Domain event representing the outcome of processing a single media record.
 */
public final class BatchProcessEvent
{
    private final MediaRecord record;
    private final String targetName;
    private final long targetSize;

    public BatchProcessEvent(MediaRecord record, String targetName, long targetSize)
    {
        this.record = record;
        this.targetName = targetName;
        this.targetSize = targetSize;
    }

    public MediaRecord getRecord()
    {
        return record;
    }

    public String getTargetName()
    {
        return targetName;
    }

    public long getTargetSize()
    {
        return targetSize;
    }

    public boolean isSuccess()
    {
        return targetSize != -1;
    }
}
package batch;

/**
 * Identifies domain events emitted during batch processing operations.
 */
public enum BatchEventType
{
    FILE_PROCESSED, SCAN_COMPLETED, BATCH_FAILED;

    /**
     * Returns the name of the event type.
     */
    public String getKey()
    {
        return name();
    }
}
package batch;

/**
 * Identifies domain events emitted during batch processing operations.
 */
public enum BatchEventType
{
    FILE_PROCESSED("FILE_PROCESSED"),
    SCAN_COMPLETED("SCAN_COMPLETED"),
    BATCH_FAILED("BATCH_FAILED");

    private final String key;

    BatchEventType(String key)
    {
        this.key = key;
    }

    public String getKey()
    {
        return key;
    }
}
package batch;

/**
 * Identifies domain events emitted during batch processing operations.
 */
enum BatchEventType
{
    FILE_PROCESSED, SCAN_COMPLETED, BATCH_FAILED;

    /**
     * Returns the name of the event type.
     * 
     * @return the string corresponding to this enum constant value
     */
    String getKey()
    {
        return name();
    }
}
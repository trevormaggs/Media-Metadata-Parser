package batch;

import common.DigitalSignature;

/**
 * Immutable domain event representing the outcome of processing a single media record.
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
     * Creates a batch process event containing the processing result for a media record.
     *
     * @param record
     *        the source media record being processed
     * @param targetName
     *        the name assigned to the generated target file
     * @param targetSize
     *        the size of the generated target file in bytes, or {@code -1} if processing failed
     *        or the record was skipped
     */
    public BatchProcessEvent(MediaRecord record, String targetName, long targetSize)
    {
        this.record = record;
        this.targetName = targetName;
        this.targetSize = targetSize;
    }

    /**
     * Returns the simple file name of the source media record.
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
     * Returns the size of the generated target file.
     *
     * @return the target file size in bytes, or {@code -1} if processing failed or the record was
     *         skipped
     */
    public long getTargetSize()
    {
        return targetSize;
    }

    /**
     * Returns the signature identifying the media file format, such as TIF, JPG, or PNG.
     *
     * @return the detected media format signature
     */
    public DigitalSignature getDigitalSignature()
    {
        return record.getMediaFormat();
    }

    /**
     * Indicates whether processing produced a valid target file size.
     *
     * @return {@code true} if {@link #getTargetSize()} is not {@code -1}, otherwise {@code false}
     */
    public boolean isSuccess()
    {
        return targetSize != -1;
    }
}
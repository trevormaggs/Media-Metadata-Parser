package batch;

public class BatchStatistics
{
    private final int sourceFilesCount;
    private final int targetFilesCount;
    private final long totalTargetSizeBytes;

    public BatchStatistics(int sourceFilesCount, int targetFilesCount, long totalTargetSizeBytes)
    {
        this.sourceFilesCount = sourceFilesCount;
        this.targetFilesCount = targetFilesCount;
        this.totalTargetSizeBytes = totalTargetSizeBytes;
    }

    public int getSourceFilesCount()
    {
        return sourceFilesCount;
    }

    public int getTargetFilesCount()
    {
        return targetFilesCount;
    }

    public long getTotalTargetSizeBytes()
    {
        return totalTargetSizeBytes;
    }

    public double getTotalTargetSizeMB()
    {
        return totalTargetSizeBytes / (1024.0 * 1024.0);
    }
}
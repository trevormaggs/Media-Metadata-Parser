package batch;

/**
 * Utility class to print media metadata in a format emulating ExifTool's -G1 -a -s -u output style.
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 5 May 2026
 */
public final class DisplayMetadata
{
    @SuppressWarnings("unused")
    private static final String COLUMN_FORMAT = "[%-13s] %-31s : %s%n";

    /**
     * Prevents direct instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that direct instantiation is not supported
     */
    private DisplayMetadata()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }
}
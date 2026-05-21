package common;

import java.io.IOException;

/**
 * Defines the contract for a handler that processes image files and extracts structured metadata.
 *
 * @author Trevor Maggs
 * @version 1.1
 */
public interface ImageHandler extends AutoCloseable
{
    /**
     * Parses the image data and extracts metadata into internal structures.
     * 
     * @throws IOException
     *         if low-level I/O errors occur, or if the binary data is corrupt
     */
    boolean parseMetadata() throws IOException;
}
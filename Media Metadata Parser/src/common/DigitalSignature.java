package common;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

/**
 * Enumerates known media formats by identifying their magic-number signatures within the beginning
 * of a media file.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2026
 */
public enum DigitalSignature
{
    JPG("jpg", new int[][]{{0xFF, 0xD8, 0xFF}}),
    PNG("png", new int[][]{{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}}),
    TIF("tif", new int[][]{{0x4D, 0x4D}, {0x49, 0x49}}), // Check for Motorola then Intel
                                                         // endian-ness
    DNG("dng", new int[0][]), // TIFF and DNG share signatures. DNG is identified by its DNGVersion
                              // TIF tag
    WEBP("webp", new int[][]{{0x52, 0x49, 0x46, 0x46, -1, -1, -1, -1, 0x57, 0x45, 0x42, 0x50}}),
    AVI("avi", new int[][]{{0x52, 0x49, 0x46, 0x46, -1, -1, -1, -1, 0x41, 0x56, 0x49, 0x20}}),
    HEIF("heic", new int[][]{{0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63}}),
    MOV("mov", new int[][]{{0x66, 0x74, 0x79, 0x70, 0x71, 0x74}, {0x6D, 0x6F, 0x6F, 0x76}}),
    MP4("mp4", new int[][]{{0x66, 0x74, 0x79, 0x70}}),
    UNKNOWN("", new int[0][]);

    /**
     * Sentinel value representing a wildcard byte that matches any value (0x00 to 0xFF). This is
     * necessary for correctly identifying RIFF containers.
     */
    private static final int WILDCARD = -1;
    private static final int MAX_MAGIC_LENGTH;
    private static final EnumSet<DigitalSignature> VIDEO_FORMATS = EnumSet.of(AVI, MOV, MP4);
    private static final EnumSet<DigitalSignature> KNOWN_FORMATS = EnumSet.complementOf(EnumSet.of(UNKNOWN));
    private final String extension;
    private final int[][] magicNumbers;

    static
    {
        int max = 0;

        /* Find the longest magic number sequence needed for buffer sizing */
        for (DigitalSignature sig : KNOWN_FORMATS)
        {
            for (int[] magic : sig.magicNumbers)
            {
                max = Math.max(max, magic.length);
            }
        }

        /*
         * Allocate a buffer large enough to detect signatures
         * that may occur beyond the start of the file.
         */
        MAX_MAGIC_LENGTH = max * 2;
    }

    DigitalSignature(String extension, int[][] magicNumbers)
    {
        this.extension = extension;
        this.magicNumbers = magicNumbers;
    }

    /**
     * Returns the standard file extension associated with this media format, excluding the leading
     * dot.
     *
     * <p>
     * If the file format is unknown, an empty string is returned.
     * </p>
     *
     * @return the file extension (e.g., {@code "jpg"}, {@code "png"}), or an empty string if
     *         unknown
     */
    public String getFileExtensionName()
    {
        return extension;
    }

    /**
     * Returns a byte array of the primary (first) magic numbers for this format.
     *
     * @return an array of bytes containing the magic numbers
     */
    public byte[] getFirstMagicNumberBytes()
    {
        return magicNumbers.length == 0 ? new byte[0] : getMagicNumberBytes(0);
    }

    /**
     * Returns a byte array containing the magic-number pattern at the specified index.
     *
     * <p>
     * Wildcard values ({@code -1}) are converted to bytes with the bit pattern {@code 0xFF}.
     * </p>
     *
     * @param index
     *        the index of the magic-number pattern to retrieve
     * @return a byte array containing the magic-number pattern
     * 
     * @throws IndexOutOfBoundsException
     *         if the index is out of bounds
     */
    public byte[] getMagicNumberBytes(int index)
    {
        if (index < 0 || index >= magicNumbers.length)
        {
            throw new IndexOutOfBoundsException("Index [" + index + "] is out of bounds for magicNumbers array");
        }

        int[] intArray = magicNumbers[index];
        byte[] byteArray = new byte[intArray.length];

        for (int i = 0; i < intArray.length; i++)
        {
            byteArray[i] = (byte) intArray[i];
        }

        return byteArray;
    }

    /**
     * Returns whether this signature represents a known video format (e.g., MP4, MOV, AVI).
     *
     * @return {@code true} if the media format is a video; {@code false} otherwise
     */
    public boolean isVideo()
    {
        return VIDEO_FORMATS.contains(this);
    }

    /**
     * Detects the media format signature based on magic numbers.
     *
     * @param file
     *        the file path as a String
     * @return the matching {@link DigitalSignature}, or {@link #UNKNOWN} if no signature matches
     * @throws IOException
     *         if an I/O error occurs while opening or reading the file
     */
    public static DigitalSignature detectFormat(String file) throws IOException
    {
        return detectFormat(Paths.get(file));
    }

    /**
     * Detects the media format signature based on magic numbers.
     *
     * @param path
     *        the file path
     * @return the matching {@link DigitalSignature}, or {@link #UNKNOWN} if no signature matches
     * @throws IOException
     *         if an I/O error occurs while opening or reading the file
     */
    public static DigitalSignature detectFormat(Path path) throws IOException
    {
        byte[] buffer = new byte[MAX_MAGIC_LENGTH];

        try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(path)))
        {
            int totalRead = 0;

            /*
             * Continue reading until the buffer is full or end-of-file is reached.
             */
            while (totalRead < buffer.length)
            {
                int bytesRead = input.read(buffer, totalRead, buffer.length - totalRead);

                if (bytesRead == -1)
                {
                    break;
                }

                totalRead += bytesRead;
            }

            for (DigitalSignature sig : KNOWN_FORMATS)
            {
                for (int[] magic : sig.magicNumbers)
                {
                    if (containsMagicNumbers(buffer, totalRead, magic))
                    {
                        return sig;
                    }
                }
            }
        }

        return UNKNOWN;
    }

    /**
     * Checks whether the given header buffer contains the specified magic number sequence. Array
     * elements equal to {@link #WILDCARD} ({@code -1}) match any byte value.
     *
     * @param fileHeader
     *        the buffer containing the initial bytes of the file
     * @param bytesRead
     *        the number of actual bytes that have been read into the buffer
     * @param magic
     *        the magic number sequence to search for, including wildcards
     * @return {@code true} if the magic number pattern occurs within the bytes read, otherwise
     *         {@code false}
     */
    private static boolean containsMagicNumbers(byte[] fileHeader, int bytesRead, int[] magic)
    {
        if (magic.length <= bytesRead)
        {
            OUTER:
            for (int i = 0; i <= bytesRead - magic.length; i++)
            {
                for (int j = 0; j < magic.length; j++)
                {
                    if (magic[j] != WILDCARD && (fileHeader[i + j] & 0xFF) != magic[j])
                    {
                        continue OUTER;
                    }
                }

                // Sub-array sequence matched
                return true;
            }
        }

        return false;
    }
}
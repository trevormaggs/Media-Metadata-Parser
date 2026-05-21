package common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * An abstract base for image file parsers. Subclasses implement decoding logic for specific formats
 * (e.g. JPEG, PNG, TIFF) to extract metadata structures.
 *
 * <p>
 * This class handles file validation and provides utilities for retrieving basic file system
 * attributes and diagnostic information.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public abstract class AbstractImageParser2
{
    private final Path imageFile;

    /**
     * Constructs an image parser and validates the target file.
     *
     * @param fpath
     *        the path to the image file
     * 
     * @throws NullPointerException
     *         if {@code fpath} is null
     * @throws IOException
     *         if the file does not exist or is not a regular file
     */
    public AbstractImageParser2(Path fpath) throws IOException
    {
        this.imageFile = Objects.requireNonNull(fpath);

        if (Files.notExists(imageFile) || !Files.isRegularFile(imageFile))
        {
            throw new IOException("File [" + imageFile + "] does not exist or is not a regular file");
        }
    }

    /**
     * Gets the image file path used for parsing.
     *
     * @return the image file {@link Path}
     */
    public Path getImageFile()
    {
        return imageFile;
    }

    /**
     * Summarises basic file attributes and metadata status for diagnostics.
     *
     * @return a formatted string containing file metrics and detected formats
     * 
     * @throws IOException
     *         if the file attributes cannot be read
     */
    public String formatDiagnosticString() throws IOException
    {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

        sb.append("File Attributes").append(System.lineSeparator());
        sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());

        BasicFileAttributes attr = Files.readAttributes(getImageFile(), BasicFileAttributes.class);

        sb.append(String.format(MetadataConstants.FORMATTER, "File", getImageFile()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Creation Time", df.format(attr.creationTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Last Access Time", df.format(attr.lastAccessTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Last Modified Time", df.format(attr.lastModifiedTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Image Format Type", getImageFormat().getFileExtensionName()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Byte Order", getMetadata().getByteOrder()));
        sb.append(System.lineSeparator());

        return sb.toString();
    }

    /**
     * Formats an error message describing the incorrect extension of the file.
     *
     * @return the error message in a form of string
     */
    protected String formatExtensionErrorMessage()
    {
        String filename = imageFile.getFileName().toString();
        int lastDot = filename.lastIndexOf('.');
        String baseName = (lastDot > 0) ? filename.substring(0, lastDot) : filename;
        String targetExt = getImageFormat().getFileExtensionName();

        /*
         * Special Case: If the filename is the extension itself (e.g., file named "tif")
         * or if it's an extension-only hidden file (e.g., ".tif").
         */
        if (baseName.equalsIgnoreCase(targetExt) || filename.equalsIgnoreCase("." + targetExt))
        {
            return String.format("File [%s] has no proper extension, but contains [%s] data", filename, targetExt.toUpperCase());
        }

        return String.format("Mismatched extension in [%s] detected. Should be [%s]", filename, String.format("%s.%s", baseName, targetExt));
    }

    /**
     * Extracts metadata from the image file.
     *
     * @throws IOException
     *         if a file reading error occurs during parsing
     */
    public abstract void readMetadata() throws IOException;

    /**
     * Retrieves the extracted metadata.
     * 
     * @return the extracted {@link Metadata} container
     */
    public abstract Metadata<?> getMetadata();

    /**
     * Returns the detected image format, such as {@code TIFF}, {@code PNG}, or {@code JPG}.
     * 
     * @return the {@link DigitalSignature} representing the image format
     */
    public abstract DigitalSignature getImageFormat();
}
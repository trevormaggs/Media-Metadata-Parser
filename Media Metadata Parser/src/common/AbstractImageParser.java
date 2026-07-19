package common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * An abstract base for image file parsers. Subclasses implement decoding logic for specific
 * formats, such as JPEG, PNG, TIFF, to extract metadata structures.
 *
 * <p>
 * This class handles file validation configurations and provides utilities for retrieving basic
 * file system attributes and generic diagnostic information.
 * </p>
 * 
 * @param <T>
 *        the specific type of {@link Metadata} container returned by this parser
 * 
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2025
 */
public abstract class AbstractImageParser<T extends Metadata<?>>
{
    private final Path imageFile;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Constructs an image parser. File validation checks are deferred to execution runtime to avoid
     * constructor-level blocking I/O and race conditions.
     *
     * @param fpath
     *        the path to the image file
     *
     * @throws NullPointerException
     *         if {@code fpath} is null
     */
    protected AbstractImageParser(Path fpath)
    {
        this.imageFile = Objects.requireNonNull(fpath, "Image file path cannot be null");
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
     * Summarises basic OS-level file attributes for diagnostics. Internal binary attributes (such
     * as byte order) are handled directly by subclasses.
     *
     * @return a formatted string containing general file metrics
     *
     * @throws IOException
     *         if the file attributes cannot be read from disk
     */
    public String formatDiagnosticString() throws IOException
    {
        StringBuilder sb = new StringBuilder();

        sb.append("File Attributes").append(System.lineSeparator());
        sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());

        BasicFileAttributes attr = Files.readAttributes(getImageFile(), BasicFileAttributes.class);

        // sb.append(String.format(MetadataConstants.FORMATTER, "File", getImageFile()));
        sb.append(String.format(MetadataConstants.FORMATTER, "File", getImageFile().getFileName()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Creation Time", DATE_FORMATTER.format(attr.creationTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Last Access Time", DATE_FORMATTER.format(attr.lastAccessTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Last Modified Time", DATE_FORMATTER.format(attr.lastModifiedTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Detected Format", getMetadata().getImageFormat().getFileExtensionName()));

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
        String baseName = (lastDot == 0 ? filename : (lastDot > 0 ? filename.substring(0, lastDot) : filename));
        String targetExt = getMetadata().getImageFormat().getFileExtensionName();

        /*
         * Special Case: If the filename is the extension itself (e.g., file named "tif")
         * or if it's an extension-only hidden file (e.g., ".tif").
         */
        if (filename.equalsIgnoreCase("." + targetExt) || filename.equalsIgnoreCase(targetExt))
        {
            return String.format("File [%s] has no proper extension, but contains [%s] data", filename, targetExt.toUpperCase());
        }

        return String.format("File [%s] contains [%s] data but uses an incorrect extension. Expected [%s.%s]", filename, targetExt.toUpperCase(), baseName, targetExt);
    }

    /**
     * Parses the image file and extracts any known metadata.
     *
     * <p>
     * Implementations are responsible for validating the file state and populating the parser's
     * metadata container. Also, by careful design, subclasses should apply a soft-landing exception
     * strategy by logging any recoverable errors to prevent from crashing mid-way during the
     * processing.
     * </p>
     */
    public abstract void readMetadata();

    /**
     * Retrieves the extracted metadata.
     * 
     * @return the extracted {@link Metadata} container execution context
     */
    public abstract T getMetadata();
}
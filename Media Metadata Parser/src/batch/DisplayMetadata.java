package batch;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import common.AbstractImageParser;
import common.ImageParserFactory;
import common.Metadata;
import filesystem.AbstractFileNode;
import filesystem.FileInspector;
import png.PngMetadataProvider;
import tif.DirectoryIFD;
import tif.TagTranslator;
import tif.TifMetadataProvider;

/**
 * Utility class to print media metadata in a format emulating ExifTool's {@code -G1 -a -s -u}
 * output style.
 *
 * <p>
 * This class coordinates file discovery through a {@link MetadataScanner}, displays file system
 * attributes under the standard {@code [System]} group, and renders metadata from supported image
 * formats in a column-aligned view.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 2 June 2026
 */
public final class DisplayMetadata
{
    private static final String COLUMN_FORMAT = "%-16s%-32s: %s%n";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private final MetadataScanner scanner;

    /**
     * Creates an instance for displaying metadata name/value attributes, similar to
     * the output format produced by {@code exiftool -G1 -a -s -u}.
     *
     * @param config
     *        the configuration containing the validated source parameters and
     *        filters supplied on the command line
     */
    public DisplayMetadata(BatchConfiguration config)
    {
        this.scanner = new MetadataScanner(config);
    }

    /**
     * Executes the metadata extraction pipeline for all matching records discovered by the scanner.
     *
     * <p>
     * For each media file, this method extracts file system attributes and then evaluates
     * format-specific metadata containers, specifically TIFF, JPEG, PNG, WebP, and HEIC. Exceptions
     * that occur during parsing or stream reading are caught and suppressed to ensure that a single
     * corrupted or unsupported file does not terminate processing of the remaining files.
     * </p>
     */
    public void execute()
    {
        try
        {
            scanner.start();

            for (MediaRecord record : scanner)
            {
                Path fpath = record.getPath();
                AbstractImageParser<?> parser = ImageParserFactory.getParser(fpath);

                parser.readMetadata();
                Metadata<?> meta = parser.getMetadata();

                displaySystemMetadata(fpath);

                if (meta.hasMetadata())
                {
                    if (record.isTIF() && meta instanceof TifMetadataProvider)
                    {
                        displayTifMetadata((TifMetadataProvider) meta);
                    }

                    else if (record.isJPG() && meta instanceof TifMetadataProvider)
                    {
                        displayTifMetadata((TifMetadataProvider) meta);
                    }

                    else if (record.isPNG() && meta instanceof PngMetadataProvider)
                    {
                        // displayPngMetadata(pngMeta);
                    }

                    else if (record.isWebP() && meta instanceof TifMetadataProvider)
                    {
                    }

                    else if (record.isHEIC() && meta instanceof TifMetadataProvider)
                    {
                        displayTifMetadata((TifMetadataProvider) meta);
                    }
                }
            }
        }

        catch (Exception exc)
        {
            /*
             * Possible exceptions may be received:
             *
             * IOException
             * ImageReadErrorException <-- Apache Commons Imaging
             * NoSuchFileException
             * UnsupportedOperationException (RuntimeException)
             * IndexOutOfBoundsException (RuntimeException)
             * IllegalStateException (RuntimeException)
             * NullPointerException (RuntimeException)
             * IllegalArgumentException (RuntimeException)
             */
        }
    }

    /**
     * Displays file system attributes for the specified path using the standard {@code [System]}
     * metadata group.
     *
     * @param path
     *        the file whose attributes are to be displayed
     *
     * @throws IOException
     *         if the file system attributes cannot be read
     */
    private void displaySystemMetadata(Path path) throws IOException
    {
        String group = "[System]";
        StringBuilder sb = new StringBuilder();
        AbstractFileNode node = FileInspector.inspect(path, true);

        sb.append(String.format(COLUMN_FORMAT, group, "FileName", node.getName()));
        sb.append(String.format(COLUMN_FORMAT, group, "Directory", "."));
        sb.append(String.format(COLUMN_FORMAT, group, "FileSize", (node.size() / 1024) + " KB"));
        sb.append(String.format(COLUMN_FORMAT, group, "FileModifyDate", formatTimestamp(node.lastModifiedTime())));
        sb.append(String.format(COLUMN_FORMAT, group, "FileAccessDate", formatTimestamp(node.lastAccessTime())));
        sb.append(String.format(COLUMN_FORMAT, group, "FileCreateDate", formatTimestamp(node.creationTime())));
        sb.append(String.format(COLUMN_FORMAT, group, "FilePermissions", node.getPermissionsString()));

        System.out.print(sb);
    }

    /**
     * Formats an epoch timestamp as an ExifTool-style date/time string.
     *
     * @param millis
     *        the timestamp in milliseconds since the Unix epoch
     *
     * @return a string in the format
     *         {@code yyyy:MM:dd HH:mm:ss±HH:mm}, using the system default time zone
     */
    private String formatTimestamp(long millis)
    {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(DTF);
    }

    /**
     * Displays metadata contained within a TIFF-based metadata structure.
     *
     * <p>
     * This method iterates through each IFD directory, translates tag identifiers and values into
     * human-readable form, and prints them using ExifTool-style group names. The metadata may
     * originate from a native TIFF file or from TIFF-based EXIF data embedded in formats such as
     * JPEG or HEIC.
     * </p>
     *
     * @param tif
     *        the metadata provider supplying TIFF directories and associated data
     */
    private void displayTifMetadata(TifMetadataProvider tif)
    {
        for (DirectoryIFD ifd : tif)
        {
            String groupName = "[" + ifd.getDirectoryType().getDescription() + "]";

            for (DirectoryIFD.EntryIFD entry : ifd)
            {
                String name = TagTranslator.getDisplayName(ifd.getDirectoryType(), entry.getTag());
                String val = TagTranslator.translate(entry.getTag(), entry.getData());

                System.out.printf(COLUMN_FORMAT, groupName, name, val);
            }
        }

        // Handle XMP separately if it exists
        if (tif.getXmpDirectory() != null)
        {
            // Logic to iterate XMP key/value pairs
        }
    }
}
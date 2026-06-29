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
import tif.TifMetadataProvider;
import tif.tagspecs.Taggable;
import xmp.XmpDirectory;
import xmp.XmpDirectory.XmpRecord;
import xmp.XmpProperty;

/**
 * Utility class to print media metadata in a format emulating the output style of
 * {@code exiftool -G1 -a -s -u}.
 *
 * This class coordinates file discovery through a {@link MetadataScanner}, displays file system
 * attributes under the standard {@code [System]} group, and renders metadata from supported image
 * formats in a column-aligned view.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 29 June 2026
 */
public final class DisplayMetadata
{
    private static final String COLUMN_FORMAT = "%-20s%-32s: %s%n";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private final MetadataScanner scanner;

    /**
     * Creates an instance for displaying metadata name/value attributes, similar to the output
     * format produced by {@code exiftool -G1 -a -s -u}.
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
     */
    public void execute()
    {
        try
        {
            scanner.start();
        }

        catch (Exception exc)
        {
            System.err.println("Fatal: Failed to initialise metadata scanner: " + exc.getMessage());
            return;
        }

        for (MediaRecord record : scanner)
        {
            Path fpath = record.getPath();

            try
            {
                AbstractImageParser<?> parser = ImageParserFactory.getParser(fpath);

                if (parser != null)
                {
                    parser.readMetadata();
                    Metadata<?> meta = parser.getMetadata();

                    System.out.printf("======== %s ========%n", fpath);

                    displaySystemMetadata(fpath);

                    if (meta != null && meta.hasMetadata())
                    {
                        if (meta instanceof TifMetadataProvider)
                        {
                            displayTifMetadata((TifMetadataProvider) meta);
                        }

                        else if (meta instanceof PngMetadataProvider)
                        {
                            displayPngMetadata((PngMetadataProvider) meta);
                        }
                    }

                    System.out.println();
                }
            }

            catch (Exception exc)
            {
                System.err.printf("Warning: Skipping metadata display for [%s] due to error: %s%n", fpath.getFileName(), exc.getMessage());
            }
        }
    }

    /**
     * Displays file system attributes for the specified path using the standard {@code [System]}
     * metadata group.
     *
     * @param path
     *        the file whose attributes are to be displayed
     * @throws IOException
     *         if the file system attributes cannot be read
     */
    private void displaySystemMetadata(Path path) throws IOException
    {
        String group = "[System]";
        StringBuilder sb = new StringBuilder();
        AbstractFileNode node = FileInspector.inspect(path, true);

        sb.append(String.format(COLUMN_FORMAT, group, "FileName", node.getName()));
        sb.append(String.format(COLUMN_FORMAT, group, "Directory", path.getParent() != null ? path.getParent().toString() : "."));
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
     * @param tif
     *        the metadata provider supplying TIFF directories and associated data
     */
    private void displayTifMetadata(TifMetadataProvider tif)
    {
        for (DirectoryIFD ifd : tif)
        {
            tif.DirectoryIdentifier dirType = ifd.getDirectoryType();
            String groupName = "[" + dirType.getDescription() + "]";

            for (DirectoryIFD.EntryIFD entry : ifd)
            {
                Taggable tag = entry.getTag();
                Object rawData = entry.getData();
                String name = getDisplayName(dirType, tag);
                String value = (tag == null || rawData == null) ? "" : tag.translate(rawData);

                System.out.printf(COLUMN_FORMAT, groupName, name, value);
            }
        }

        if (tif.hasXmpData())
        {
            XmpDirectory xmp = tif.getXmpDirectory();

            for (XmpRecord record : xmp)
            {
                String displayName;
                String translatedValue;
                String prefix = record.getPrefix();
                String rawName = record.getName();
                XmpProperty xmpProp = XmpProperty.fromQualifiedPath(record.getQualifierPath());

                // Skip structural language metadata fields that Exiftool suppresses implicitly
                if (rawName == null || rawName.contains("/xml:lang") || rawName.contains("exif:Fired") || rawName.contains("exif:Mode"))
                {
                    continue;
                }

                if (xmpProp == XmpProperty.UNKNOWN)
                {
                    displayName = XmpProperty.format(rawName);
                    translatedValue = XmpProperty.UNKNOWN.translate(record.getValue());
                }

                else
                {
                    displayName = xmpProp.getDescription();
                    translatedValue = xmpProp.translate(record.getValue());
                }

                String groupName = (!prefix.isEmpty() ? "[XMP-" + prefix + "]" : "[XMP]");

                System.out.printf(COLUMN_FORMAT, groupName, displayName, translatedValue);
            }
        }
    }

    private void displayPngMetadata(PngMetadataProvider pngMeta)
    {
        // Placeholder for native text chunk printing
    }

    /**
     * Applies cosmetic display label transformations to match unique ExifTool output styles.
     */
    private String getDisplayName(tif.DirectoryIdentifier dir, Taggable tag)
    {
        if (tag == null)
        {
            return "Unknown Tag";
        }

        if (dir == tif.DirectoryIdentifier.IFD_DIRECTORY_IFD1)
        {
            if (tag == tif.tagspecs.TagIFD_Baseline.IFD_JPEG_INTERCHANGE_FORMAT)
            {
                return "ThumbnailOffset";
            }

            if (tag == tif.tagspecs.TagIFD_Baseline.IFD_JPEG_INTERCHANGE_FORMAT_LENGTH)
            {
                return "ThumbnailLength";
            }
        }

        return tag.getDescription();
    }
}
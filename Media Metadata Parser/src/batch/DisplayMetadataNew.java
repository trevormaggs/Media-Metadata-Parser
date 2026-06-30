package batch;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
 * @author Trevor Maggs
 * @version 1.3
 * @since 30 June 2026
 */
public final class DisplayMetadataNew
{
    // Exact ExifTool metrics: 15 chars for group, 32 chars for short tag name key
    private static final String COLUMN_FORMAT = "%-15s%-32s: %s%n";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private final MetadataScanner scanner;

    public DisplayMetadataNew(BatchConfiguration config)
    {
        this.scanner = new MetadataScanner(config);
    }

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

    private String formatTimestamp(long millis)
    {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(DTF);
    }

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

                if (value.contains("\n"))
                {
                    System.out.print(value + System.lineSeparator());
                }
                
                else
                {
                    System.out.printf(COLUMN_FORMAT, groupName, name, value);
                }
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

                // Sanitize XMP names and values to mirror short tag code keys and clean floating-point formats
                displayName = cleanTagName(displayName);
                translatedValue = cleanNumericValue(translatedValue);

                String groupName = (!prefix.isEmpty() ? "[XMP-" + prefix + "]" : "[XMP]");
                System.out.printf(COLUMN_FORMAT, groupName, displayName, translatedValue);
            }
        }
    }

    private void displayPngMetadata(PngMetadataProvider pngMeta)
    {
        // Placeholder for native text chunk printing
    }

    private String getDisplayName(tif.DirectoryIdentifier dir, Taggable tag)
    {
        if (tag == null)
        {
            return "UnknownTag";
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

        return cleanTagName(tag.getDescription());
    }

    /**
     * Reconstitutes description labels into strict ExifTool programmatic short keys
     * by normalizing casing adjustments and stripping invalid delimiter characters.
     */
    private String cleanTagName(String desc)
    {
        if (desc == null) return "";
        // Keep slash symbols safe when identifying nested arrays, otherwise wipe non-word entities
        String step1 = desc.replaceAll("[\\s\\-_\\(\\)]+", "");
        return step1.replace("/", "");
    }

    /**
     * Formats trailing scientific decimals passed up by structural XMP parsers to align
     * with standard presentation.
     */
    private String cleanNumericValue(String value)
    {
        if (value == null) return "";
        try
        {
            // If it behaves like a standard trailing decimal, strip redundant formatting
            if (value.contains(".") && value.matches("^-?\\d*\\.\\d+$"))
            {
                double d = Double.parseDouble(value);
                if (d == (long) d)
                {
                    return String.format(Locale.ROOT, "%d", (long) d);
                }
                return String.format(Locale.ROOT, "%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
            }
        }
        catch (NumberFormatException ignored) {}
        return value;
    }
}
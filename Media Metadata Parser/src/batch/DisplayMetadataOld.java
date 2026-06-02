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
import tif.TifMetadata;
import tif.TifMetadataProvider;

/**
 * Utility class to print media metadata in a format emulating ExifTool's -G1 -a -s -u output style.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 5 May 2026
 */
public final class DisplayMetadataOld
{
    private static final String COLUMN_FORMAT = "%-16s%-32s: %s%n";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private final MetadataScanner scanner;

    public DisplayMetadataOld(BatchConfiguration config)
    {
        this.scanner = new MetadataScanner(config);
    }

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

                    else if (record.isJPG() && meta instanceof TifMetadata)
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
     * Converts a long (milliseconds) to the ExifTool readable date format.
     */
    private String formatTimestamp(long millis)
    {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(DTF);
    }

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

        // 3. Handle XMP separately if it exists
        if (tif.getXmpDirectory() != null)
        {
            // Logic to iterate XMP key/value pairs
        }
    }
}
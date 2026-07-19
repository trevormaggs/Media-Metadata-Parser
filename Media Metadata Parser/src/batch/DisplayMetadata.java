package batch;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import common.AbstractImageParser;
import common.ImageParserFactory;
import common.Metadata;
import common.PropertyDisplay;
import filesystem.AbstractFileNode;
import filesystem.FileInspector;
import png.ChunkType;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import png.PngParser;
import tif.DirectoryIFD;
import tif.TifMetadataProvider;
import tif.tagspecs.PhotoshopManager;
import tif.tagspecs.TagIFD_Private;
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
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private final MetadataScanner scanner;
    private static final EnumSet<ChunkType> DISPLAY_CHUNK_FILTER = EnumSet.of(
            ChunkType.IHDR, ChunkType.gAMA, ChunkType.sRGB, ChunkType.pHYs,
            ChunkType.tEXt, ChunkType.zTXt, ChunkType.iTXt, ChunkType.eXIf,
            ChunkType.tIME);

    // TODO: investigate whether it is worthwhile adding these
    // 3 more chunks to display more information on metadata.
    // ChunkType.iCCP, ChunkType.cHRM, ChunkType.sBIT);

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
     * Executes the metadata extraction pipeline for all media records discovered by the scanner.
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

                if (parser instanceof PngParser)
                {
                    PngParser png = (PngParser) parser;
                    png.setChunkFilter(DISPLAY_CHUNK_FILTER);
                }

                parser.readMetadata();
                Metadata<?> meta = parser.getMetadata();

                // System.out.printf("%s%n", parser.formatDiagnosticString());

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

            catch (IOException exc)
            {
                System.err.printf("Warning: Skipping metadata display for [%s] due to error: %s%n", fpath.getFileName(), exc.getMessage());
            }
        }
    }

    /**
     * Displays file system attributes for the specified file. The attributes are grouped under the
     * {@code [System]} heading in the output.
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

        sb.append(String.format(Taggable.COLUMN_FORMAT, group, "FileName", node.getName()));
        sb.append(String.format(Taggable.COLUMN_FORMAT, group, "Directory", path.getParent() != null ? path.getParent().toString() : "."));
        sb.append(String.format(Taggable.COLUMN_FORMAT, group, "FileSize", (node.size() / 1024) + " KB"));
        sb.append(String.format(Taggable.COLUMN_FORMAT, group, "FileModifyDate", formatTimestamp(node.lastModifiedTime())));
        sb.append(String.format(Taggable.COLUMN_FORMAT, group, "FileAccessDate", formatTimestamp(node.lastAccessTime())));
        sb.append(String.format(Taggable.COLUMN_FORMAT, group, "FileCreateDate", formatTimestamp(node.creationTime())));
        sb.append(String.format(Taggable.COLUMN_FORMAT, group, "FilePermissions", node.getPermissionsString()));

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
        List<String> photoshopMeta = new ArrayList<>();

        for (DirectoryIFD ifd : tif)
        {
            tif.DirectoryIdentifier dirType = ifd.getDirectoryType();
            String groupName = "[" + dirType.getDescription() + "]";

            for (DirectoryIFD.EntryIFD entry : ifd)
            {
                Taggable tag = entry.getTag();
                Object rawData = entry.getData();

                if (tag == TagIFD_Private.IFD_PHOTOSHOP_SETTINGS)
                {
                    PhotoshopManager.decodePhotoshopProperties(rawData, new PropertyDisplay()
                    {
                        @Override
                        public void accept(String key, Object value)
                        {
                            photoshopMeta.add(String.format(Taggable.COLUMN_FORMAT, "[Photoshop]", key, value));
                        }
                    });

                    continue;
                }

                String name = getDisplayName(dirType, tag);
                String value = tag.translate(rawData);

                if (!value.isEmpty())
                {
                    System.out.printf(Taggable.COLUMN_FORMAT, groupName, name, value);
                }
            }
        }

        // Defer Photoshop listing until after last main IFD listing
        for (int i = 0; i < photoshopMeta.size(); i++)
        {
            System.out.print(photoshopMeta.get(i));
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
                XmpProperty xmpProp = XmpProperty.fromQualifiedPath(record.getQualifiedPath());

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

                System.out.printf(Taggable.COLUMN_FORMAT, groupName, displayName, translatedValue);
            }
        }
    }

    /**
     * Displays metadata contained within a PNG metadata structure.
     *
     * @param png
     *        the metadata provider supplying PNG directories and chunks
     */
    private void displayPngMetadata(PngMetadataProvider png)
    {
        String groupName = "[PNG]";

        PropertyDisplay disp = new PropertyDisplay()
        {
            @Override
            public void accept(String key, Object value)
            {
                System.out.printf(Taggable.COLUMN_FORMAT, groupName, key, value);
            }
        };

        for (PngDirectory dir : png)
        {
            for (PngChunk chunk : dir)
            {
                chunk.printProperties(disp);
            }
        }
    }

    /**
     * Returns the display name for a TIFF tag, applying compatibility adjustments where required to
     * match ExifTool output.
     *
     * @param dir
     *        the directory containing the tag
     * @param tag
     *        the tag whose display name is required
     * @return the display name
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
package batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import common.AbstractImageParser;
import common.DetectedFormatResult;
import common.ImageParserFactory;
import common.Metadata;
import common.PropertyConsumer;
import filesystem.AbstractFileNode;
import filesystem.FileInspector;
import gui.FileMetadataRecord;
import logger.LogFactory;
import png.ChunkType;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import png.PngParser;
import progressbar.ProgressListener;
import tif.DirectoryIFD;
import tif.TifMetadataProvider;
import tif.tagspecs.PhotoshopManager;
import tif.tagspecs.TagIFD_Private;
import tif.tagspecs.Taggable;
import util.SystemInfo;
import xmp.XmpDirectory;
import xmp.XmpDirectory.XmpRecord;
import xmp.XmpProperty;

/**
 * Extracts and displays media metadata in a format emulating the output style of
 * {@code exiftool -G1 -a -s -u}.
 *
 * <p>
 * This class coordinates file discovery through a {@link MetadataScanner}, displays file system
 * attributes under the standard {@code [System]} group, and renders metadata from supported image
 * formats in a column-aligned view.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 29 June 2026
 */
public final class DisplayMetadataFix
{
    private static final LogFactory LOGGER = LogFactory.getLogger(DisplayMetadataFix.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private static final EnumSet<ChunkType> DISPLAY_CHUNK_FILTER = EnumSet.of(
            ChunkType.IHDR, ChunkType.gAMA, ChunkType.sRGB, ChunkType.pHYs,
            ChunkType.tEXt, ChunkType.zTXt, ChunkType.iTXt, ChunkType.eXIf,
            ChunkType.tIME);

    // TODO: investigate whether it is worthwhile adding these
    // 3 more chunks to display more information on metadata.
    // ChunkType.iCCP, ChunkType.cHRM, ChunkType.sBIT);

    private final BatchConfiguration config;
    private final MetadataScanner scanner;
    private final List<ProgressListener> progressListeners;
    private Consumer<String> metadataReceivedListener;
    private Consumer<FileMetadataRecord> recordExtractedListener;

    /**
     * Creates an instance for displaying metadata name/value attributes, similar to the output
     * format produced by {@code exiftool -G1 -a -s -u}.
     *
     * @param config
     *        the configuration containing the validated source parameters and filters supplied on
     *        the command line
     */
    public DisplayMetadataFix(BatchConfiguration config)
    {
        this.config = config;
        this.progressListeners = new ArrayList<>();
        this.scanner = new MetadataScanner(config);
    }

    /**
     * Registers a progress listener to receive updates during file scanning and metadata
     * extraction. These updates can be used to display overall execution progress, such as in a
     * progress bar. You may add multiple listeners.
     *
     * @param listener
     *        the progress listener to register
     */
    public void addProgressListener(ProgressListener listener)
    {
        if (listener != null)
        {
            progressListeners.add(listener);
            scanner.addProgressListener(listener);
        }
    }

    /**
     * Sets the listener to receive formatted metadata text generated during metadata inspection.
     * The text is formatted to emulate the output style of {@code exiftool -G1 -a -s -u}.
     * 
     * @param listener
     *        the listener to receive the formatted metadata text
     */
    public void setOnMetadataReceived(Consumer<String> listener)
    {
        metadataReceivedListener = listener;
    }

    /**
     * Sets the listener to receive the structured metadata record extracted for each media file.
     *
     * @param listener
     *        the listener to receive the extracted {@link FileMetadataRecord}
     */
    public void setOnRecordExtracted(Consumer<FileMetadataRecord> listener)
    {
        recordExtractedListener = listener;
    }

    /**
     * Executes the metadata extraction pipeline for all media records discovered by the scanner.
     *
     * @return metrics containing the total number of source files scanned and their cumulative size
     */
    public BatchMetrics execute()
    {
        int count = 1;
        int totalSourceFiles = 0;
        long totalBytes = 0L;

        try
        {
            startLogging();
            scanner.start();
            resetListeners();// Reset progress bar state after scanning completes

            totalSourceFiles = scanner.getRecordCount();

            if (totalSourceFiles > 0)
            {
                for (MediaRecord record : scanner)
                {
                    Path fpath = record.getPath();
                    DetectedFormatResult result = ImageParserFactory.inspect(fpath);

                    totalBytes += record.getFileSize();

                    if (result.hasParser())
                    {
                        AbstractImageParser<?> parser = result.getParser();

                        if (parser instanceof PngParser)
                        {
                            PngParser png = (PngParser) parser;
                            png.setChunkFilter(DISPLAY_CHUNK_FILTER);
                        }

                        parser.readMetadata();
                        Metadata<?> meta = parser.getMetadata();
                        FileMetadataRecord fileRecord = new FileMetadataRecord(fpath);

                        readSystemMetadata(fpath, fileRecord);

                        if (meta != null && meta.hasMetadata())
                        {
                            if (meta instanceof TifMetadataProvider)
                            {
                                readTifMetadata((TifMetadataProvider) meta, fileRecord);
                            }

                            else if (meta instanceof PngMetadataProvider)
                            {
                                readPngMetadata((PngMetadataProvider) meta, fileRecord);
                            }
                        }

                        print(fileRecord.toRawText());

                        if (recordExtractedListener != null)
                        {
                            recordExtractedListener.accept(fileRecord);
                        }
                    }

                    /* Notify progress listeners based on overall loop count */
                    for (ProgressListener listener : progressListeners)
                    {
                        listener.onProgressUpdate(count, totalSourceFiles);
                    }

                    count++;
                }

                /* Signal completion to listeners so onCompleted triggers */
                for (ProgressListener listener : progressListeners)
                {
                    listener.onCompleted(totalSourceFiles);
                }
            }

            return new BatchMetrics(totalSourceFiles, 0, totalBytes);
        }

        catch (Exception exc)
        {
            // TODO: maybe change to Logger as error or re-throw an exception?
            System.err.println("Unable to initialise due to an error: " + exc.getMessage());
            return new BatchMetrics(0, 0, 0L);
        }

        finally
        {
            LogFactory.close();
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
    private void readSystemMetadata(Path path, FileMetadataRecord record) throws IOException
    {
        String group = "System";
        AbstractFileNode node = FileInspector.inspect(path, true);

        record.addItem(group, "FileName", node.getName());
        record.addItem(group, "Directory", path.getParent() != null ? path.getParent().toString() : ".");
        record.addItem(group, "FileSize", (node.size() / 1024) + " KB");
        record.addItem(group, "FileModifyDate", formatTimestamp(node.lastModifiedTime()));
        record.addItem(group, "FileAccessDate", formatTimestamp(node.lastAccessTime()));
        record.addItem(group, "FileCreateDate", formatTimestamp(node.creationTime()));
        record.addItem(group, "FilePermissions", node.getPermissionsString());
    }

    /**
     * Displays metadata contained within the supplied TIFF metadata provider.
     *
     * @param tif
     *        the metadata provider supplying TIFF directories and associated data
     * @param record
     *        the metadata record to which the extracted properties are added
     */
    private void readTifMetadata(TifMetadataProvider tif, FileMetadataRecord record)
    {
        for (DirectoryIFD ifd : tif)
        {
            tif.DirectoryIdentifier dirType = ifd.getDirectoryType();
            String groupName = dirType.getDescription();

            for (DirectoryIFD.EntryIFD entry : ifd)
            {
                Taggable tag = entry.getTag();
                Object rawData = entry.getData();

                if (tag == TagIFD_Private.IFD_PHOTOSHOP_SETTINGS)
                {
                    PhotoshopManager.decodePhotoshopProperties(rawData, new PropertyConsumer()
                    {
                        @Override
                        public void accept(String key, Object value)
                        {
                            record.addItem("Photoshop", key, String.valueOf(value));
                        }
                    });

                    continue;
                }

                String name = getDisplayName(dirType, tag);
                String value = tag.translate(rawData);

                if (!value.isEmpty())
                {
                    record.addItem(groupName, name, value);
                }
            }
        }

        if (tif.hasXmpData())
        {
            XmpDirectory xmp = tif.getXmpDirectory();

            for (XmpRecord xRecord : xmp)
            {
                String displayName;
                String translatedValue;
                String prefix = xRecord.getPrefix();
                String rawName = xRecord.getName();
                XmpProperty xmpProp = XmpProperty.fromQualifiedPath(xRecord.getQualifiedPath());

                if (rawName == null || rawName.contains("/xml:lang") || rawName.contains("exif:Fired") || rawName.contains("exif:Mode"))
                {
                    continue;
                }

                if (xmpProp == XmpProperty.UNKNOWN)
                {
                    displayName = XmpProperty.format(rawName);
                    translatedValue = XmpProperty.UNKNOWN.translate(xRecord.getValue());
                }

                else
                {
                    displayName = xmpProp.getDescription();
                    translatedValue = xmpProp.translate(xRecord.getValue());
                }

                String groupName = (!prefix.isEmpty() ? "XMP-" + prefix : "XMP");
                record.addItem(groupName, displayName, translatedValue);
            }
        }
    }

    /**
     * Displays metadata contained within the supplied PNG metadata provider.
     *
     * @param png
     *        the metadata provider supplying PNG directories and chunks
     * @param record
     *        the metadata record to which the extracted properties are added
     */
    private void readPngMetadata(PngMetadataProvider png, FileMetadataRecord record)
    {
        PropertyConsumer disp = new PropertyConsumer()
        {
            @Override
            public void accept(String key, Object value)
            {
                record.addItem("PNG", key, String.valueOf(value));
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

    private void print(String text)
    {
        if (metadataReceivedListener != null)
        {
            metadataReceivedListener.accept(text);
        }

        else
        {
            System.out.print(text);
        }
    }

    /**
     * Resets internal progress state across all registered listeners.
     */
    private void resetListeners()
    {
        for (ProgressListener listener : progressListeners)
        {
            listener.reset();
        }
    }

    /**
     * Formats an epoch timestamp as an exiftool-style date/time string.
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
     * Returns the display name for a TIFF tag, applying compatibility adjustments where required to
     * match ExifTool output.
     *
     * @param dir
     *        the directory containing the tag
     * @param tag
     *        the tag whose display name is required
     * @return the display name for the tag, or {@code "Unknown Tag"} if the tag is {@code null}
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

    /**
     * Initialises the logging system and records the active configuration.
     *
     * @throws BatchErrorException
     *         if the logging service cannot be established
     */
    private void startLogging() throws BatchErrorException
    {
        try
        {
            String logName = "metadata_" + SystemInfo.getHostname() + ".log";
            Path logPath = Paths.get(logName);

            if (Files.exists(logPath))
            {
                Files.deleteIfExists(logPath);
            }

            LogFactory.configure(logPath.toString());
            LogFactory.setDebug(config.isDebug());

            LOGGER.info(this.getClass().getSimpleName() + " loaded");
            LOGGER.info("Source: " + config.getSource().toAbsolutePath());
        }

        catch (IOException exc)
        {
            throw new BatchErrorException(exc);
        }
    }
}
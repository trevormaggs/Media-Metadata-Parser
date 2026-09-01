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
import gui.MediaFileMetadata;
import logger.LogFactory;
import png.ChunkType;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import png.PngParser;
import progressbar.ProgressListener;
import tif.DirectoryIFD;
import tif.TifMetadataProvider;
import tif.tagspecs.Taggable;
import util.SystemInfo;

/**
 * Extracts and displays media metadata using a pure POJO {@link MediaFileMetadata}.
 *
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
    private static final LogFactory LOGGER = LogFactory.getLogger(DisplayMetadata.class);
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
    private Consumer<MediaFileMetadata> recordExtractedListener;

    /**
     * Creates an instance for displaying metadata name/value attributes, similar to the output
     * format produced by {@code exiftool -G1 -a -s -u}.
     *
     * @param config
     *        the configuration containing the validated source parameters and filters supplied on
     *        the command line
     */
    public DisplayMetadata(BatchConfiguration config)
    {
        this.config = config;
        this.progressListeners = new ArrayList<>();
        this.scanner = new MetadataScanner(config);
    }

    /**
     * Registers a progress listener to receive updates during both scanning and processing
     * execution phases. You may add multiple listeners.
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
     * Sets the callback listener that receives formatted text output as metadata is processed.
     *
     * @param listener
     *        the string consumer to process raw text output
     */
    public void setOnMetadataReceived(Consumer<String> listener)
    {
        metadataReceivedListener = listener;
    }

    /**
     * Sets the callback listener that receives each parsed {@link MediaFileMetadata}.
     *
     * @param listener
     *        the consumer to process extracted metadata records
     */
    public void setOnRecordExtracted(Consumer<MediaFileMetadata> listener)
    {
        recordExtractedListener = listener;
    }

    /**
     * Executes the metadata extraction pipeline for all media records discovered by the scanner.
     *
     * @return metrics containing total source files scanned and size
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
            resetListeners(); // Reset progress bar state after scanning completes

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
                        MediaFileMetadata fileRecord = new MediaFileMetadata(fpath, meta);
                        StringBuilder sb = new StringBuilder().append("======== ").append(fpath).append(" ========");

                        appendSystemMetadata(fpath, sb);

                        if (meta.hasMetadata())
                        {
                            appendMetadataText(meta, sb);
                        }

                        sb.append(System.lineSeparator());

                        /*
                         * Dispatches the output string to the registered
                         * listener or standard output stream.
                         */
                        if (metadataReceivedListener != null)
                        {
                            metadataReceivedListener.accept(sb.toString());
                        }

                        else
                        {
                            System.out.print(sb.toString());
                        }

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
            // TODO: change to Logger as error or re-throw an exception?
            System.err.println("Unable to initialise due to an error: " + exc.getMessage());
            return new BatchMetrics(0, 0, 0L);
        }

        finally
        {
            LogFactory.close();
        }
    }

    /**
     * Appends file system attributes for the specified file to the provided string buffer.
     * The attributes are grouped under the {@code [System]} heading.
     *
     * @param path
     *        the file whose attributes are to be displayed
     * @param sb
     *        the buffer to append formatted attributes to
     * @throws IOException
     *         if the file system attributes cannot be read
     */
    private void appendSystemMetadata(Path path, StringBuilder sb) throws IOException
    {
        String group = "[System]";
        String fmt = Taggable.COLUMN_FORMAT;
        AbstractFileNode node = FileInspector.inspect(path, true);

        sb.append(System.lineSeparator());
        sb.append(String.format(fmt, group, "FileName", node.getName()));
        sb.append(String.format(fmt, group, "Directory", path.getParent() != null ? path.getParent().toString() : "."));
        sb.append(String.format(fmt, group, "FileSize", (node.size() / 1024) + " KB"));
        sb.append(String.format(fmt, group, "FileModifyDate", formatTimestamp(node.lastModifiedTime())));
        sb.append(String.format(fmt, group, "FileAccessDate", formatTimestamp(node.lastAccessTime())));
        sb.append(String.format(fmt, group, "FileCreateDate", formatTimestamp(node.creationTime())));
        sb.append(String.format(fmt, group, "FilePermissions", node.getPermissionsString()));
    }

    /**
     * Appends image format metadata (TIFF, PNG, etc.) to the provided string buffer.
     *
     * @param meta
     *        the metadata container to extract output strings from
     * @param sb
     *        the buffer to append formatted metadata entries to
     */
    private void appendMetadataText(Metadata<?> meta, StringBuilder sb)
    {
        if (meta instanceof TifMetadataProvider)
        {
            TifMetadataProvider tif = (TifMetadataProvider) meta;

            for (DirectoryIFD ifd : tif)
            {
                String groupName = "[" + ifd.getDirectoryType().getDescription() + "]";

                for (DirectoryIFD.EntryIFD entry : ifd)
                {
                    Taggable tag = entry.getTag();

                    if (tag != null)
                    {
                        String name = tag.getDescription();
                        String value = tag.translate(entry.getData());

                        if (!value.isEmpty())
                        {
                            sb.append(String.format(Taggable.COLUMN_FORMAT, groupName, name, value));
                        }
                    }
                }
            }
        }

        else if (meta instanceof PngMetadataProvider)
        {
            PngMetadataProvider png = (PngMetadataProvider) meta;

            PropertyConsumer consumer = new PropertyConsumer()
            {
                @Override
                public void accept(String key, Object value)
                {
                    sb.append(String.format(Taggable.COLUMN_FORMAT, "[PNG]", key, String.valueOf(value)));
                }
            };

            for (PngDirectory dir : png)
            {
                for (PngChunk chunk : dir)
                {
                    chunk.printProperties(consumer);
                }
            }
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
            LogFactory.setTrace(config.isTrace());

            LOGGER.info(this.getClass().getSimpleName() + " loaded");
            LOGGER.info("Source: " + config.getSource().toAbsolutePath());

            if (config.isDebug())
            {
                LOGGER.info("Debugging is enabled");
            }

            if (config.isTrace())
            {
                LOGGER.info("Trace logging is enabled");
            }
        }

        catch (IOException exc)
        {
            throw new BatchErrorException(exc);
        }
    }
}
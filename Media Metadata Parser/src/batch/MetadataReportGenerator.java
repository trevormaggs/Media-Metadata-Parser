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
import common.PropertyBiConsumer;
import common.Utils;
import filesystem.AbstractFileNode;
import filesystem.FileInspector;
import gui.CollectedMetadata;
import logger.LogFactory;
import png.ChunkType;
import png.ChunkType.Category;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import png.PngParser;
import progressbar.ProgressListener;
import tif.DirectoryIFD;
import tif.TifMetadata;
import tif.TifMetadataProvider;
import tif.TifParser;
import tif.tagspecs.Taggable;
import util.SystemInfo;
import xmp.XmpDirectory;
import xmp.XmpDirectory.XmpRecord;

/**
 * Coordinates media metadata inspection pipelines and emits structured key-value property entries
 * via {@link MetadataInspectionEvent} notifications.
 *
 * <p>
 * This class coordinates file discovery through a {@link MetadataScanner}, extracts standard file
 * system attributes under the {@code [System]} group, and parses metadata from supported image
 * formats (EXIF/TIFF, PNG, and XMP) for display or reporting targets.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 29 June 2026
 */
public final class MetadataReportGenerator
{
    private static final LogFactory LOGGER = LogFactory.getLogger(MetadataReportGenerator.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private static final EnumSet<ChunkType> DISPLAY_CHUNK_FILTER = EnumSet.of(
            ChunkType.IHDR, ChunkType.gAMA, ChunkType.sRGB, ChunkType.pHYs,
            ChunkType.tEXt, ChunkType.zTXt, ChunkType.iTXt, ChunkType.eXIf,
            ChunkType.tIME, ChunkType.iCCP, ChunkType.cHRM, ChunkType.sBIT);

    private final BatchConfiguration config;
    private final MetadataScanner scanner;
    private final List<ProgressListener> progressListeners;
    private Consumer<MetadataInspectionEvent> metadataInspectedListener;
    private Consumer<CollectedMetadata> recordExtractedListener;

    /**
     * Creates an instance for inspecting and generating metadata records, configured via
     * command-line parameters and filters.
     *
     * @param config
     *        the configuration containing validated source parameters and execution flags
     */
    public MetadataReportGenerator(BatchConfiguration config)
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
     * Registers an event callback listener that receives structured metadata inspection events as
     * each tag or attribute is extracted.
     * 
     * <p>
     * This method implements the Observer/Callback pattern, decoupling metadata extraction from
     * downstream targets, such as CLI loggers, GUI table models, or export writers.
     * </p>
     *
     * @param listener
     *        the callback consumer triggered when a {@link MetadataInspectionEvent} is emitted
     */
    public void setOnMetadataInspected(Consumer<MetadataInspectionEvent> listener)
    {
        metadataInspectedListener = listener;
    }

    /**
     * Sets the callback listener that receives each fully parsed {@link CollectedMetadata} object.
     *
     * @param listener
     *        the consumer to process extracted metadata records
     */
    public void setOnRecordExtracted(Consumer<CollectedMetadata> listener)
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

                        formatSystemProperties(record);

                        if (meta.hasMetadata())
                        {
                            extractMetadataEXIF(meta, record);
                        }

                        if (meta.hasXmpData())
                        {
                            extractMetadataXMP(meta, record);
                        }

                        emitMetadataEvent(new MetadataInspectionEvent(System.lineSeparator()));

                        /*
                         * Dispatches the output of metadata values
                         * to the registered listener.
                         */
                        if (recordExtractedListener != null)
                        {
                            recordExtractedListener.accept(new CollectedMetadata(fpath, meta));
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
            LOGGER.error("Unable to execute metadata inspection due to an error: " + exc.getMessage());
            return new BatchMetrics(0, 0, 0L);
        }

        finally
        {
            LogFactory.close();
        }
    }

    /**
     * Inspects file system level attributes for the specified record and emits them
     * under the {@code [System]} metadata group.
     *
     * @param record
     *        the media record whose file system attributes are to be inspected
     * @throws IOException
     *         if file system metadata cannot be accessed
     */
    private void formatSystemProperties(MediaRecord record) throws IOException
    {
        String group = "[System]";
        Path fpath = record.getPath();
        AbstractFileNode node = FileInspector.inspect(fpath, true);

        emitMetadataEvent(new MetadataInspectionEvent(String.format("======== %s ========%n", fpath)));
        emitMetadataEvent(new MetadataInspectionEvent(record, group, "FileName", node.getName()));
        emitMetadataEvent(new MetadataInspectionEvent(record, group, "Directory", fpath.getParent() != null ? fpath.getParent().toString() : "."));
        emitMetadataEvent(new MetadataInspectionEvent(record, group, "FileSize", (node.size() / 1024) + " KB"));
        emitMetadataEvent(new MetadataInspectionEvent(record, group, "FileModifyDate", formatTimestamp(node.lastModifiedTime())));
        emitMetadataEvent(new MetadataInspectionEvent(record, group, "FileAccessDate", formatTimestamp(node.lastAccessTime())));
        emitMetadataEvent(new MetadataInspectionEvent(record, group, "FileCreateDate", formatTimestamp(node.creationTime())));
        emitMetadataEvent(new MetadataInspectionEvent(record, group, "FilePermissions", node.getPermissionsString()));
    }

    /**
     * Extracts format-specific EXIF and chunk metadata properties from the provided metadata
     * container
     * and dispatches them as inspection events.
     *
     * @param meta
     *        the metadata container extracted from the parsed image file
     * @param record
     *        the media record currently being processed
     */
    private void extractMetadataEXIF(Metadata<?> meta, MediaRecord record)
    {
        if (meta instanceof TifMetadataProvider)
        {
            TifMetadataProvider tif = (TifMetadataProvider) meta;
            processIfdDirectories(tif, record);
        }

        else if (meta instanceof PngMetadataProvider)
        {
            PngMetadataProvider png = (PngMetadataProvider) meta;

            PropertyBiConsumer consumer = new PropertyBiConsumer()
            {
                @Override
                public void accept(String key, Object value)
                {
                    MetadataInspectionEvent event = new MetadataInspectionEvent(record, "[PNG]", key, String.valueOf(value));
                    emitMetadataEvent(event);
                }
            };

            for (PngDirectory dir : png)
            {
                for (PngChunk chunk : dir)
                {
                    chunk.exportProperties(consumer);
                }
            }

            PngDirectory dir = png.getDirectory(Category.MISC);
            PngChunk chunk = (dir != null ? dir.getFirstChunk(ChunkType.eXIf) : null);

            if (chunk != null)
            {
                TifMetadata exif = TifParser.parseTiffMetadataFromBytes(chunk.getPayloadArray());

                if (exif.hasExifData())
                {
                    processIfdDirectories(exif, record);
                }
            }
        }
    }

    /**
     * Iterates through a collection of IFD directories and emits events for all valid metadata
     * tags.
     *
     * @param directories
     *        an iterable collection of IFD directories containing metadata tag entries
     * @param record
     *        the media record associated with the IFD entries
     */
    private void processIfdDirectories(Iterable<DirectoryIFD> directories, MediaRecord record)
    {
        for (DirectoryIFD ifd : directories)
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
                        MetadataInspectionEvent event = new MetadataInspectionEvent(record, groupName, name, value);
                        emitMetadataEvent(event);
                    }
                }
            }
        }
    }

    /**
     * Extracts XMP metadata records from the provided metadata container and emits them
     * with prefixed namespace groups (e.g., {@code [XMP-dc]}).
     *
     * @param meta
     *        the metadata container holding XMP directory data
     * @param record
     *        the media record currently being processed
     */
    private void extractMetadataXMP(Metadata<?> meta, MediaRecord record)
    {
        if (meta instanceof TifMetadataProvider)
        {
            XmpDirectory xml = ((TifMetadataProvider) meta).getXmpDirectory();

            if (xml != null)
            {
                for (XmpRecord xmp : xml)
                {
                    MetadataInspectionEvent event = new MetadataInspectionEvent(record, "[XMP-" + xmp.getPrefix() + "]", Utils.capitalize(xmp.getName()), xmp.getValue());
                    emitMetadataEvent(event);
                }
            }
        }
    }

    /**
     * Safely dispatches an inspection event to the registered metadata listener.
     *
     * @param event
     *        the {@link MetadataInspectionEvent} containing extracted attribute details
     *        or structural delimiters to emit
     */
    private void emitMetadataEvent(MetadataInspectionEvent event)
    {
        if (metadataInspectedListener != null)
        {
            metadataInspectedListener.accept(event);
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
     * @return a string in the format {@code yyyy:MM:dd HH:mm:ss±HH:mm}, using the system default
     *         time zone
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
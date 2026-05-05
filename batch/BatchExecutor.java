package batch;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import common.AbstractImageParser;
import common.ImageParserFactory;
import common.Metadata;
import logger.LogFactory;
import util.SystemInfo;

/**
 * Automates the batch processing of image files by copying, renaming, and chronologically sorting
 * them based on their EXIF metadata, such as {@code DateTimeOriginal}. Includes a built-in
 * 10-second offset increment for user-defined dates to prevent metadata collisions and ensure
 * stable sorting in downstream applications.
 *
 * <p>
 * This class supports a range of image formats, including JPEG, TIFF, PNG, WebP, and HEIF. If EXIF
 * metadata is not available, it defaults to using the file's last modified time-stamp.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 * @see MediaFile
 */
public abstract class BatchExecutor implements Iterable<MediaFile>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(BatchExecutor.class);
    private static final long TEN_SECOND_OFFSET_MS = 10_000L;
    private static final FileVisitor<Path> DELETE_VISITOR;
    private final Set<MediaFile> imageSet;
    private final BatchConfiguration config;
    private long dateOffsetUpdate;

    public static final String DEFAULT_SOURCE_DIRECTORY = ".";
    public static final String DEFAULT_TARGET_DIRECTORY = "IMAGEDIR";
    public static final String DEFAULT_IMAGE_PREFIX = "image";

    static
    {
        DELETE_VISITOR = new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                if (exc == null)
                {
                    Files.delete(dir);
                }

                else
                {
                    throw exc;
                }

                return FileVisitResult.CONTINUE;
            }
        };
    }

    /**
     * Constructs a new Executor using the specified configuration.
     *
     * @param settings
     *        the configuration settings used to initialise the executor
     */
    protected BatchExecutor(BatchConfiguration settings)
    {
        this.config = settings;

        if (config.isDescending())
        {
            // Sorts the copied images in descending order
            imageSet = new TreeSet<>(new DescendingTimestampComparator());
            LOGGER.info("Sorted copied images in descending order");
        }

        else
        {
            // Sorts the copied images in ascending order
            imageSet = new TreeSet<>(new DefaultTimestampComparator());
            LOGGER.info("Sorted copied images in ascending order");
        }
    }

    /**
     * Returns an iterator over the internal sorted set of {@code MediaFile} objects.
     *
     * @return an Iterator for navigating the media file set
     */
    @Override
    public Iterator<MediaFile> iterator()
    {
        return imageSet.iterator();
    }

    /**
     * Begins the batch processing workflow by cleaning the target directory, setting up logging,
     * and processing the specified source files or directory. Note, this operation is destructive,
     * as the code uses DELETE_VISITOR to wipe the folder.
     * 
     * <p>
     * Note, this method is final to ensure no subclass accidentally overrides the defined critical
     * logic.
     * </p>
     *
     * @throws BatchErrorException
     *         if an I/O error has occurred
     */
    public final void start() throws BatchErrorException
    {
        FileVisitor<Path> visitor = createImageVisitor();

        try
        {
            if (Files.exists(config.getTarget()))
            {
                /*
                 * Prevents the user from accidentally pointing targetDir as the source directory.
                 */
                if (Files.isSameFile(config.getSource().toAbsolutePath(), config.getTarget().toAbsolutePath()))
                {
                    throw new BatchErrorException("Target directory [" + config.getTarget() + "] cannot be the same location as source directory [" + config.getSource() + "]");
                }

                /*
                 * Permanently deletes the target directory and all of its contents.
                 * This operation is destructive and cannot be undone.
                 */
                Files.walkFileTree(config.getTarget(), DELETE_VISITOR);

                LOGGER.warn("Old files within directory [" + config.getTarget() + "] deleted");
            }

            Files.createDirectories(config.getTarget());

            startLogging();

            if (config.getFileSet().size() > 0)
            {
                for (String fileName : config.getFileSet())
                {
                    Path fpath = config.getSource().resolve(fileName);

                    if (Files.exists(fpath) && Files.isRegularFile(fpath))
                    {
                        visitor.visitFile(fpath, Files.readAttributes(fpath, BasicFileAttributes.class));
                    }

                    else
                    {
                        LOGGER.info("Skipping non-regular file [" + fpath + "]");
                    }
                }
            }

            else
            {
                Files.walkFileTree(config.getSource(), visitor);
            }

            processBatchCopy();
        }

        catch (IOException exc)
        {
            throw new BatchErrorException("An I/O error has occurred", exc);
        }
    }

    /**
     * Retrieves the source directory where the original files are located.
     *
     * @return the Path instance of the source directory
     */
    protected Path getSourceDirectory()
    {
        return config.getSource();
    }

    /**
     * Retrieves the target directory where the processed files will be saved.
     *
     * @return the Path instance of the target directory
     */
    protected Path getTargetDirectory()
    {
        return config.getTarget();
    }

    /**
     * Returns the prefix used for renaming each copied image file.
     *
     * @return the filename prefix
     */
    protected String getPrefix()
    {
        return config.getPrefix();
    }

    /**
     * Returns the total number of image files identified.
     *
     * @return the count of processed images
     */
    protected int getImageCount()
    {
        return imageSet.size();
    }

    /**
     * Indicates whether the {@code Date Taken} attribute should be prepended to the copied file
     * name.
     *
     * @return true if the date-time should be prepended, otherwise false
     */
    protected boolean embedDateTime()
    {
        return config.isEmbedDateTime();
    }

    /**
     * Returns whether video files in the source directory should be skipped during processing.
     *
     * @return true if video files should be ignored, otherwise false
     */
    protected boolean skipVideoFiles()
    {
        return config.isSkipVideo();
    }

    /**
     * Returns whether the use of a user-provided date is forced, overriding existing file metadata.
     *
     * <p>
     * If true, the system will prioritise the date string provided by the user over any
     * {@code DateTimeOriginal} or other EXIF tags found within the media files.
     * </p>
     *
     * @return {@code true} if the user-defined date override is enabled;
     *         {@code false} otherwise
     */
    protected boolean isDateChangeForced()
    {
        return config.isForceDateChange();
    }

    /**
     * Returns a {@link FileVisitor} instance to traverse the source directory.
     *
     * <p>
     * The visitor analyses each file, extracts metadata segments, and determines the
     * {@code Date Taken} time-stamp. Each file is then wrapped in a {@link MediaFile} object and
     * added to the internal set for later processing.
     * </p>
     *
     * @return a configured {@link FileVisitor} for processing image files
     *
     * @throws BatchErrorException
     *         if the source directory is not a valid directory
     */
    private FileVisitor<Path> createImageVisitor() throws BatchErrorException
    {
        if (!Files.isDirectory(config.getSource()))
        {
            throw new BatchErrorException("The source directory [" + config.getSource() + "] is not a valid directory. Please verify that the path exists and is a directory");
        }

        return new SimpleFileVisitor<Path>()
        {
            /**
             * Determines the {@code Date Taken} time-stamp for a file based on a priority
             * hierarchy.
             *
             * <ol>
             * <li>User-provided date (if {@code force} is true or metadata is missing)</li>
             * <li>Metadata date (if available)</li>
             * <li>File's last modified time-stamp (final fallback)</li>
             * </ol>
             *
             * If the user-provided date is utilised, the time-stamp is incremented by a 10-second
             * offset for subsequent files to avoid collisions.
             *
             * @param fpath
             *        the image file path, used only for logging context
             * @param dateTaken
             *        the date obtained from the image's metadata, or null if unavailable
             * @param modifiedTime
             *        the file's last modified time-stamp, used as the final fallback
             * @return a {@link FileTime} representing the resolved "Date Taken" value
             */
            private FileTime selectDateTaken(Path fpath, ZonedDateTime dateTaken, FileTime modifiedTime)
            {
                if (config.getUserDate() != null && (config.isForceDateChange() || dateTaken == null))
                {
                    long newTime = config.getUserDate().toInstant().toEpochMilli() + (dateOffsetUpdate * TEN_SECOND_OFFSET_MS);
                    dateOffsetUpdate++;

                    return FileTime.fromMillis(newTime);
                }

                if (dateTaken != null)
                {
                    LOGGER.info("Date Taken found in Exif metadata: [" + fpath + "]");

                    return FileTime.fromMillis(dateTaken.toInstant().toEpochMilli());
                }

                return modifiedTime;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
                if (!dir.equals(config.getSource()))
                {
                    LOGGER.info("Sub-directory [" + dir + "] is being read");
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path fpath, BasicFileAttributes attr) throws IOException
            {
                LOGGER.info("Original file [" + fpath + "] registered for processing");

                try
                {
                    AbstractImageParser parser = ImageParserFactory.getParser(fpath);

                    parser.readMetadata();

                    Metadata<?> meta = parser.getMetadata();
                    ZonedDateTime dateTaken = meta.extractZonedDateTime();
                    FileTime modifiedTime = selectDateTaken(fpath, dateTaken, attr.lastModifiedTime());
                    MediaFile media = new MediaFile(fpath, modifiedTime, parser.getImageFormat(), (dateTaken == null));

                    // System.out.printf("Date/Time: %s\n", dateTaken);
                    System.out.printf("%s%n", parser.formatDiagnosticString());

                    imageSet.add(media);
                }

                /*
                 * IOException
                 * ImageReadErrorException
                 * NoSuchFileException
                 * UnsupportedOperationException (RuntimeException)
                 * IndexOutOfBoundsException (RuntimeException)
                 * IllegalStateException (RuntimeException)
                 * NullPointerException (RuntimeException)
                 * IllegalArgumentException (RuntimeException)
                 */
                catch (Exception exc)
                {
                    LOGGER.error(exc.getMessage(), exc);
                }

                return FileVisitResult.CONTINUE;
            }
        };
    }

    /**
     * Begins the logging system and writes configuration details to a log file. This method is
     * for internal setup and is not intended for external use.
     *
     * @throws BatchErrorException
     *         if the logging service cannot be established
     */
    private void startLogging() throws BatchErrorException
    {
        try
        {
            String logFilePath = Paths.get(config.getTarget().toAbsolutePath().toString(), "batchlog_" + SystemInfo.getHostname() + ".log").toString();

            LOGGER.configure(logFilePath);
            LOGGER.setDebug(config.isDebug());
            LOGGER.setTrace(false);

            // Log some information about the logging setup
            LOGGER.info("Source directory set to [" + getSourceDirectory().toAbsolutePath() + "] with original images");
            LOGGER.info("Target directory set to [" + getTargetDirectory().toAbsolutePath() + "] for copied images");
        }

        catch (SecurityException | IOException exc)
        {
            throw new BatchErrorException("Unable to enable logging. Program terminated", exc);
        }
    }

    /**
     * To be implemented by subclasses to provide the logic for iterating through the internal
     * sorted set of {@link MediaFile} objects for batching.
     */
    protected abstract void processBatchCopy();
}
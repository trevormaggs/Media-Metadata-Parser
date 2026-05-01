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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import common.AbstractImageParser;
import common.ImageParserFactory;
import common.Metadata;
import logger.LogFactory;
import util.SystemInfo;

public class MetadataScanner implements Iterable<MediaRecord>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(MetadataScanner.class);
    private static final FileVisitor<Path> DELETE_VISITOR;
    private final Set<MediaRecord> imageSet;
    private final BatchConfiguration config;

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
    protected MetadataScanner(BatchConfiguration settings)
    {
        this.config = settings;

        this.imageSet = new TreeSet<>(new Comparator<MediaRecord>()
        {
            @Override
            public int compare(MediaRecord o1, MediaRecord o2)
            {
                int cmp;
                FileTime d1 = o1.getNaturalDate();
                FileTime d2 = o2.getNaturalDate();

                if (config.isDescending())
                {
                    cmp = d2.compareTo(d1);
                }

                else
                {
                    cmp = d1.compareTo(d2);
                }

                /*
                 * If both timestamps equal, then compare the Path.
                 * It is a Tie-breaker.
                 */
                if (cmp == 0)
                {
                    cmp = o1.getPath().compareTo(o2.getPath());
                }

                return cmp;
            }
        });

        if (config.isDescending())
        {
            LOGGER.info("Sorted scanned images in descending order");
        }

        else
        {
            LOGGER.info("Sorted scanned images in ascending order");
        }
    }

    /**
     * Returns an iterator over the internal sorted set of {@code MediaFile} objects.
     *
     * @return an Iterator for navigating the media file set
     */
    @Override
    public Iterator<MediaRecord> iterator()
    {
        return imageSet.iterator();
    }

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
        }

        catch (IOException exc)
        {
            throw new BatchErrorException("An I/O error has occurred", exc);
        }
    }

    /**
     * Returns the total number of image files identified.
     *
     * @return the count of processed images
     */
    protected int getRecordCount()
    {
        return imageSet.size();
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

                // TEST IT FIRST
                if (!config.getFileSet().isEmpty() && !config.getFileSet().contains(fpath.getFileName().toString()))
                {
                    return FileVisitResult.CONTINUE;
                }

                try
                {
                    AbstractImageParser parser = ImageParserFactory.getParser(fpath);

                    parser.readMetadata();

                    Metadata<?> meta = parser.getMetadata();
                    MediaRecord media = new MediaRecord(fpath, meta, parser.getImageFormat(), attr.lastModifiedTime());

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
            LOGGER.info("Source directory set to [" + config.getSource().toAbsolutePath() + "] with original images");
            LOGGER.info("Target directory set to [" + config.getTarget().toAbsolutePath() + "] for copied images");
        }

        catch (SecurityException | IOException exc)
        {
            throw new BatchErrorException("Unable to enable logging. Program terminated", exc);
        }
    }
}
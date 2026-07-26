package batch;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import common.AbstractImageParser;
import common.ImageParserFactory;
import common.Metadata;
import progressbar.ProgressListener;

/**
 * Discovers media files within a directory tree and extracts their metadata.
 *
 * <p>
 * Progress updates are broadcast to registered {@link ProgressListener} instances during scanning,
 * providing real-time feedback without coupling the scanner to a specific user interface framework.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.3
 * @since 1 May 2026
 */
public class MetadataScanner implements Iterable<MediaRecord>
{
    private volatile boolean cancelled;
    private final BatchConfiguration config;
    private final Set<MediaRecord> imageSet;
    private final List<ProgressListener> listeners;
    private int fileCount;

    /**
     * Constructs a scanner using the specified batch configuration.
     * 
     * @param settings
     *        the validated configuration containing source and sorting preferences
     */
    protected MetadataScanner(BatchConfiguration settings)
    {
        this.cancelled = false;
        this.config = settings;
        this.listeners = new ArrayList<>();
        this.imageSet = new TreeSet<>(new Comparator<MediaRecord>()
        {
            @Override
            public int compare(MediaRecord o1, MediaRecord o2)
            {
                int cmp;
                FileTime d1 = o1.getNaturalDate();
                FileTime d2 = o2.getNaturalDate();

                cmp = (config.isDescending() ? d2.compareTo(d1) : d1.compareTo(d2));

                /* Use the path as a tie-breaker when timestamps are equal. */
                if (cmp == 0)
                {
                    cmp = o1.getPath().compareTo(o2.getPath());
                }

                return cmp;
            }
        });
    }

    /**
     * Returns the total number of media records discovered during scanning.
     *
     * @return the number of discovered media records
     */
    public int getRecordCount()
    {
        return imageSet.size();
    }

    /**
     * Registers a progress listener to receive updates during file discovery.
     *
     * @param listener
     *        the listener to notify during the scanning process
     */
    public void addProgressListener(ProgressListener listener)
    {
        if (listener != null)
        {
            this.listeners.add(listener);
        }
    }

    /**
     * Signals the scanner to abort execution at the earliest opportunity.
     */
    public void cancel()
    {
        cancelled = true;
    }

    /**
     * Returns whether execution cancellation was requested.
     *
     * @return {@code true} if cancellation has been requested or the current thread has been
     *         interrupted, otherwise {@code false}
     */
    public boolean isCancelled()
    {
        return (cancelled || Thread.currentThread().isInterrupted());
    }

    /**
     * Initiates the file system traversal to discover media files and extract their metadata. If
     * cancellation is requested, scanning terminates as soon as practical.
     * 
     * @throws BatchErrorException
     *         if a critical I/O error occurs or the source directory is inaccessible
     */
    public final void start() throws BatchErrorException
    {
        if (!isCancelled())
        {
            FileVisitor<Path> visitor = createImageVisitor();

            try
            {
                fileCount = config.getFileSet().size();

                if (fileCount > 0)
                {
                    int count = 1;

                    for (String fileName : config.getFileSet())
                    {
                        Path fpath = config.getSource().resolve(fileName);

                        if (Files.exists(fpath) && Files.isRegularFile(fpath))
                        {
                            FileVisitResult result = visitor.visitFile(fpath, Files.readAttributes(fpath, BasicFileAttributes.class));

                            if (result == FileVisitResult.TERMINATE)
                            {
                                break;
                            }
                        }

                        notifyListeners(count++, fileCount);
                    }
                }

                else
                {
                    fileCount = (int) countRegularFiles();
                    Files.walkFileTree(config.getSource(), visitor);
                }
            }

            catch (Exception exc)
            {
                throw new BatchErrorException(exc.getMessage(), exc);
            }
        }

        else
        {
            // TODO: consider adding a Logger?
            // LOGGER.warn("Batch process was cancelled by user after processing " + (count - 1) + " files.");
        }
    }

    /**
     * Counts the number of regular files within the configured source directory.
     *
     * <p>
     * The count is used to assist progress reporting prior to the metadata scan. The operation may
     * terminate early if cancellation is detected.
     * </p>
     *
     * @return the number of regular files discovered
     *
     * @throws IOException
     *         if an I/O error occurs while traversing the directory tree
     */
    protected long countRegularFiles() throws IOException
    {
        long regularFilesCount = 0;

        try (Stream<Path> stream = Files.walk(config.getSource()))
        {
            Iterator<Path> iterator = stream.iterator();

            while (iterator.hasNext())
            {
                if (isCancelled())
                {
                    break;
                }

                Path path = iterator.next();

                if (Files.isRegularFile(path))
                {
                    regularFilesCount++;
                }
            }
        }

        return regularFilesCount;
    }

    /**
     * Creates the {@link FileVisitor} used to traverse the source directory and extract metadata
     * from supported media files.
     *
     * @return the configured file visitor
     *
     * @throws BatchErrorException
     *         if the configured source path is not a valid directory
     */
    private FileVisitor<Path> createImageVisitor() throws BatchErrorException
    {
        if (!Files.isDirectory(config.getSource()))
        {
            throw new BatchErrorException("The source directory [" + config.getSource() + "] is not a valid directory");
        }

        return new SimpleFileVisitor<Path>()
        {
            private int scannedCount = 0;

            @Override
            public FileVisitResult visitFile(Path fpath, BasicFileAttributes attr) throws IOException
            {
                if (isCancelled())
                {
                    return FileVisitResult.TERMINATE;
                }

                if (config.getFileSet().size() > 0 && !config.getFileSet().contains(fpath.getFileName().toString()))
                {
                    return FileVisitResult.CONTINUE;
                }

                scannedCount++;

                try
                {
                    AbstractImageParser<?> parser = ImageParserFactory.getParser(fpath);

                    parser.readMetadata();
                    Metadata<?> meta = parser.getMetadata();
                    imageSet.add(new MediaRecord(fpath, meta, meta.getImageFormat(), attr.lastModifiedTime()));
                }

                catch (UnsupportedOperationException exc)
                {
                    // Gracefully skip unsupported file formats
                }

                /* Notify listeners across both directory walk and file set modes */
                notifyListeners(scannedCount, fileCount);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                return (isCancelled() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE);
            }
        };
    }

    /**
     * Notifies all registered progress listeners of the current scan progress.
     *
     * @param current
     *        the current progress position
     * @param total
     *        the total number of files to process
     */
    private void notifyListeners(int current, int total)
    {
        for (ProgressListener listener : listeners)
        {
            listener.onProgressUpdate(current, total);
        }
    }

    /**
     * Returns an iterator over the discovered media records. Records are returned in the sort order
     * defined by the current {@link BatchConfiguration}.
     * 
     * @return an iterator over the discovered media records
     */
    @Override
    public Iterator<MediaRecord> iterator()
    {
        return imageSet.iterator();
    }
}
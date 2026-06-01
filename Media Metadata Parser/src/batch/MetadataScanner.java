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
import common.AbstractImageParser;
import common.ImageParserFactory;
import common.Metadata;
import progressbar.ProgressListener;

/**
 * Facilitates the discovery and metadata extraction of media files within a directory tree.
 * 
 * <p>
 * This class performs a deep scan of the file system using a {@link FileVisitor}. It validates
 * media formats, extracts metadata via the {@link ImageParserFactory}, and maintains a sorted
 * collection of {@link MediaRecord} objects based on chronological criteria.
 * </p>
 * 
 * <p>
 * Progress updates are broadcast to registered {@link ProgressListener} instances during the
 * scanning phase to allow for real-time UI feedback.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 1 May 2026
 */
public class MetadataScanner implements Iterable<MediaRecord>
{
    private final Set<MediaRecord> imageSet;
    private final BatchConfiguration config;
    private final List<ProgressListener> listeners;

    /**
     * Constructs a new scanner using the specified batch configuration.
     * 
     * <p>
     * Initialises the internal {@code imageSet} with a custom comparator that handles chronological
     * sorting (ascending or descending) and uses file paths as tie-breakers to ensure a stable sort
     * order.
     * </p>
     *
     * @param settings
     *        the validated configuration containing source paths and sort preferences
     */
    protected MetadataScanner(BatchConfiguration settings)
    {
        this.config = settings;
        this.listeners = new ArrayList<ProgressListener>();
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
                 * If both timestamps equal, then compare
                 * the Path. It is a tie-breaker.
                 */
                if (cmp == 0)
                {
                    cmp = o1.getPath().compareTo(o2.getPath());
                }

                return cmp;
            }
        });
    }

    /**
     * Returns an iterator over the discovered media records, ordered according to the
     * configuration's sort preferences.
     *
     * @return an Iterator for the sorted media record set
     */
    @Override
    public Iterator<MediaRecord> iterator()
    {
        return imageSet.iterator();
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
     * Initiates the file system traversal to discover media and extract metadata.
     * 
     * <p>
     * If a specific file set is defined in the configuration, only those files are targeted.
     * Otherwise, the scanner performs a full recursive walk of the source directory.
     * </p>
     *
     * @throws BatchErrorException
     *         if a critical I/O error occurs or the source directory is inaccessible
     */
    public final void start() throws BatchErrorException
    {
        FileVisitor<Path> visitor = createImageVisitor();

        try
        {
            if (config.getFileSet().size() > 0)
            {
                for (String fileName : config.getFileSet())
                {
                    Path fpath = config.getSource().resolve(fileName);

                    if (Files.exists(fpath) && Files.isRegularFile(fpath))
                    {
                        visitor.visitFile(fpath, Files.readAttributes(fpath, BasicFileAttributes.class));
                    }
                }
            }

            else
            {
                Files.walkFileTree(config.getSource(), visitor);
            }
        }

        catch (Exception exc)
        {
            throw new BatchErrorException(exc.getMessage(), exc);
        }
    }

    /**
     * Returns the total number of valid media records identified during the scan.
     *
     * @return the size of the discovered media set
     */
    protected int getRecordCount()
    {
        return imageSet.size();
    }

    /**
     * Broadcasts the current progress count to all registered listeners.
     *
     * @param current
     *        the current number of successfully identified media records
     */
    private void notifyListeners(int current)
    {
        for (ProgressListener listener : listeners)
        {
            listener.onProgressUpdate(current);
        }
    }

    /**
     * Creates a {@link FileVisitor} instance designed to traverse the source directory for media
     * discovery and metadata extraction.
     * 
     * <p>
     * The returned visitor handles:
     * </p>
     * 
     * <ul>
     * <li>Filtering files based on user-defined file sets.</li>
     * <li>Detecting media formats and selecting the appropriate parser.</li>
     * <li>Building {@link MediaRecord} objects from extracted metadata and filesystem
     * attributes.</li>
     * <li>Notifying progress listeners upon each successful record creation.</li>
     * </ul>
     *
     * @return a SimpleFileVisitor configured for the current batch scan
     * 
     * @throws BatchErrorException
     *         if the source path in the configuration is not a valid directory
     */
    private FileVisitor<Path> createImageVisitor() throws BatchErrorException
    {
        if (!Files.isDirectory(config.getSource()))
        {
            throw new BatchErrorException("The source directory [" + config.getSource() + "] is not a valid directory.");
        }

        return new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path fpath, BasicFileAttributes attr) throws IOException
            {
                if (config.getFileSet().size() > 0 && !config.getFileSet().contains(fpath.getFileName().toString()))
                {
                    return FileVisitResult.CONTINUE;
                }

                try
                {
                    AbstractImageParser<?> parser = ImageParserFactory.getParser(fpath);

                    parser.readMetadata();
                    Metadata<?> meta = parser.getMetadata();
                    MediaRecord media = new MediaRecord(fpath, meta, parser.getImageFormat(), attr.lastModifiedTime());

                    imageSet.add(media);
                    notifyListeners(imageSet.size());

                    System.out.printf("%s%n", parser.formatDiagnosticString());
                    // System.out.printf("%s", meta);
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

                    throw exc;
                }

                return FileVisitResult.CONTINUE;
            }
        };
    }
}
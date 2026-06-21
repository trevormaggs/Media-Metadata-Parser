package batch;

import java.io.IOException;
import java.nio.file.DirectoryStream;
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
import progressbar.ConsoleProgressBar;
import progressbar.ProgressListener;

/**
 * Facilitates the discovery and metadata extraction of media files within a directory tree.
 * 
 * <p>
 * This class recursively traverses the file system using a {@link FileVisitor}. It identifies
 * supported media formats, extracts metadata via the {@link ImageParserFactory}, and maintains a
 * sorted collection of {@link MediaRecord} objects based on chronological criteria.
 * </p>
 * 
 * <p>
 * Progress updates are broadcast to registered {@link ProgressListener} instances during scanning
 * to provide real-time user interface feedback.
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
    private int fileCount;

    /**
     * Constructs a scanner using the specified batch configuration.
     * 
     * <p>
     * Initialises the internal {@code imageSet} with a custom comparator that handles chronological
     * sorting (ascending or descending) and uses file paths as tie-breakers to ensure a stable sort
     * order.
     * </p>
     *
     * @param settings
     *        the validated configuration containing source and sorting preferences
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

                cmp = (config.isDescending() ? d2.compareTo(d1) : d1.compareTo(d2));

                /*
                 * Use the path as a tie-breaker when timestamps are equal.
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
     * @return an iterator over the sorted media record set
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
     * If a specific file set is defined in the configuration, only those files are processed.
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
            fileCount = config.getFileSet().size();

            if (fileCount > 0)
            {
                // System.out.print("Scanning [" + fileCount + "] files");

                registerProgressBar(fileCount);

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
                fileCount = (int) countRegularFiles();

                // System.out.println("Scanning files in [" + config.getSource() + "]");

                registerProgressBar(fileCount);

                Files.walkFileTree(config.getSource(), visitor);
            }
        }

        catch (Exception exc)
        {
            throw new BatchErrorException(exc.getMessage(), exc);
        }
    }

    /**
     * Broadcasts the current progress count to all registered listeners.
     *
     * @param current
     *        the current number of discovered media records
     */
    private void notifyListeners(int current, int total)
    {
        for (ProgressListener listener : listeners)
        {
            listener.onProgressUpdate(current, total);
        }
    }

    /**
     * Conditionally registers the console progress bar if metadata display mode is inactive.
     *
     * @param totalFiles
     *        the total number of files to process
     */
    private void registerProgressBar(int totalFiles)
    {
        if (!config.isShowMetadata())
        {
            System.out.print("Processing [" + fileCount + "] files");

            addProgressListener(new ConsoleProgressBar(0, totalFiles));
        }
    }

    protected long countRegularFiles() throws IOException
    {
        long immediateFilesCount = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getSource()))
        {
            for (Path entry : stream)
            {
                if (Files.isRegularFile(entry))
                {
                    immediateFilesCount++;
                }
            }
        }

        catch (IOException exc)
        {
            // Handle or log your exception here
        }

        return immediateFilesCount;
    }

    /**
     * Returns the total number of media records discovered during scanning.
     *
     * @return the number of discovered media records
     */
    protected int getRecordCount()
    {
        return imageSet.size();
    }

    /**
     * Creates a file visitor for media discovery and metadata extraction.
     * 
     * <p>
     * The returned visitor handles:
     * </p>
     * 
     * <ul>
     * <li>Filtering files against the configured file set, when specified.</li>
     * <li>Detecting media formats and selecting the appropriate parser.</li>
     * <li>Building {@link MediaRecord} objects from extracted metadata and file system
     * attributes.</li>
     * <li>Notifying progress listeners upon each successful record creation.</li>
     * </ul>
     *
     * @return a file visitor configured for the current batch scan
     * 
     * @throws BatchErrorException
     *         if the source path in the configuration is not a valid directory
     */
    private FileVisitor<Path> createImageVisitor() throws BatchErrorException
    {
        if (!Files.isDirectory(config.getSource()))
        {
            throw new BatchErrorException("The source directory [" + config.getSource() + "] is not a valid directory");
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
                    imageSet.add(new MediaRecord(fpath, meta, meta.getImageFormat(), attr.lastModifiedTime()));

                    System.out.printf("%s%n", parser.formatDiagnosticString());
                    // System.out.printf("%s", meta);

                    notifyListeners(imageSet.size(), fileCount);
                }

                catch (UnsupportedOperationException exc)
                {
                    // Gracefully skip files with unsupported image signatures
                    fileCount--;
                }

                return FileVisitResult.CONTINUE;
            }
        };
    }
}
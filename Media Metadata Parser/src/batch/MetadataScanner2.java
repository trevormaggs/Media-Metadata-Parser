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
 * Facilitates the discovery and metadata extraction of media files within a directory tree.
 * 
 * <p>
 * Progress updates are broadcast to registered {@link ProgressListener} instances during scanning
 * to provide real-time UI feedback without coupling to specific frameworks.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 1 May 2026
 */
public class MetadataScanner2 implements Iterable<MediaRecord>
{
    private final Set<MediaRecord> imageSet;
    private final BatchConfiguration config;
    private final List<ProgressListener> listeners;
    private int fileCount;

    /**
     * Constructs a scanner using the specified batch configuration.
     * 
     * @param settings
     *        the validated configuration containing source and sorting preferences
     */
    protected MetadataScanner2(BatchConfiguration settings)
    {
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
                int count = 1;

                for (String fileName : config.getFileSet())
                {
                    Path fpath = config.getSource().resolve(fileName);

                    if (Files.exists(fpath) && Files.isRegularFile(fpath))
                    {
                        visitor.visitFile(fpath, Files.readAttributes(fpath, BasicFileAttributes.class));
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

    /**
     * Notifies all registered listeners of current scanning progress.
     */
    private void notifyListeners(int current, int total)
    {
        for (ProgressListener listener : listeners)
        {
            listener.onProgressUpdate(current, total);
        }
    }

    protected long countRegularFiles() throws IOException
    {
        long regularFilesCount = 0;

        try (Stream<Path> stream = Files.walk(config.getSource()))
        {
            Iterator<Path> iterator = stream.iterator();

            while (iterator.hasNext())
            {
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
     * Returns the total number of media records discovered during scanning.
     */
    public int getRecordCount()
    {
        return imageSet.size();
    }

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
        };
    }
}
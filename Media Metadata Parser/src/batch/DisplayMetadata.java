package batch;

import java.nio.file.Path;
import java.util.Iterator;
import common.AbstractImageParser;
import common.ImageParserFactory;
import common.Metadata;

/**
 * Utility class to print media metadata in a format emulating ExifTool's -G1 -a -s -u output style.
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 5 May 2026
 */
public final class DisplayMetadata
{
    @SuppressWarnings("unused")
    private static final String COLUMN_FORMAT = "[%-13s] %-31s : %s%n";
    private final MetadataScanner scanner;

    public DisplayMetadata(MetadataScanner scanner)
    {
        this.scanner = scanner;

        Iterator<MediaRecord> iter = scanner.iterator();

        try
        {
            while (iter.hasNext())
            {
                Path fpath = iter.next().getPath();

                System.out.printf("%s\n", fpath.getFileName());

                AbstractImageParser parser = ImageParserFactory.getParser(fpath);
                parser.readMetadata();
                Metadata<?> meta = parser.getMetadata();

                System.out.printf("%s\n", meta.hasExifData());
                // System.out.printf("%s\n", parser.formatDiagnosticString());
            }
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
        }
    }
}
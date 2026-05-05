package batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;

/**
 * Utility class to print media metadata in a format emulating ExifTool's -G1 -a -s -u output style.
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 5 May 2026
 */
public final class DisplayMetadata
{
    private static final String COLUMN_FORMAT = "[%-13s] %-31s : %s%n";

    /**
     * Prevents direct instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that direct instantiation is not supported
     */
    private DisplayMetadata()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    /**
     * Prints the metadata for all records within a scanner.
     * 
     * @param scanner
     *        the scanner containing media records
     */
    public static void print(MetadataScanner scanner)
    {
        for (MediaRecord record : scanner)
        {
            printRecord(record);
        }
    }

    private static void printRecord(MediaRecord record)
    {
        System.out.println("---- " + record.getPath().getFileName() + " ----");

        // 1. Emulated ExifTool Group
        printLine("ExifTool", "ExifToolVersion", "13.29");

        // 2. System Group (I/O related)
        printSystemMetadata(record);

        // 3. File Group (Format related)
        printLine("File", "FileType", record.getMediaFormat().name());
        printLine("File", "FileTypeExtension", record.getMediaFormat().getFileExtensionName());
        printLine("File", "MIMEType", record.getMediaFormat().getMimeType());

        // 4. Actual Embedded Metadata (IFD, Exif, XMP, IPTC)
        // Replacing lambdas with traditional entrySet iteration
        Map<String, Map<String, String>> groups = record.getMetadataGroups();

        for (Map.Entry<String, Map<String, String>> groupEntry : groups.entrySet())
        {
            String groupName = groupEntry.getKey();
            Map<String, String> tags = groupEntry.getValue();

            for (Map.Entry<String, String> tagEntry : tags.entrySet())
            {
                printLine(groupName, tagEntry.getKey(), tagEntry.getValue());
            }
        }

        System.out.println();
    }

    private static void printSystemMetadata(MediaRecord record)
    {
        try
        {
            BasicFileAttributes attrs = Files.readAttributes(record.getPath(), BasicFileAttributes.class);

            printLine("System", "FileName", record.getPath().getFileName().toString());
            printLine("System", "Directory", record.getPath().getParent().toString());
            printLine("System", "FileSize", formatFileSize(attrs.size()));
            printLine("System", "FileModifyDate", attrs.lastModifiedTime().toString());

            try
            {
                String perms = PosixFilePermissions.toString(Files.getPosixFilePermissions(record.getPath()));
                printLine("System", "FilePermissions", perms);
            }

            catch (UnsupportedOperationException e)
            {
                printLine("System", "FilePermissions", "read-write");
            }

            catch (IOException e)
            {
                printLine("System", "FilePermissions", "unknown");
            }
        }

        catch (IOException e)
        {
            printLine("System", "Error", "Could not read system attributes");
        }
    }

    private static void printLine(String group, String tag, Object value)
    {
        System.out.printf(COLUMN_FORMAT, group, tag, value != null ? value.toString() : "");
    }

    private static String formatFileSize(long bytes)
    {
        if (bytes < 1024)
        {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);

        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
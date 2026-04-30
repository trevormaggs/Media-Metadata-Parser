package tif;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import common.AbstractImageParser;
import common.Metadata;
import common.MetadataConstants;
import tif.DirectoryIFD.EntryIFD;
import xmp.XmpDirectory.XmpRecord;

public final class MetadataFormatter
{
    /**
     * Emulates: exiftool -G1 -a -s -u
     */
    /**
     * Formats an XMP record to match the exiftool -G1 -s style.
     */


    /**
     * Complete Emulation: exiftool -G1 -a -s -u
     */
    public static String toExifToolReport(TifMetadata metadata)
    {
        StringBuilder sb = new StringBuilder();

        // 1. Process TIFF IFDs (EXIF, GPS, etc.)
        for (DirectoryIFD ifd : metadata)
        {
            for (EntryIFD entry : ifd)
            {
                //sb.append(toExifToolString(entry)).append(System.lineSeparator());
            }
        }

        // 2. Process XMP (Mirroring ExifTool's unified view)
        if (metadata.hasXmpData())
        {
            for (XmpRecord record : metadata.getXmpDirectory())
            {
                //sb.append(toExifToolString(record)).append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    /**
     * Handles the top-level diagnostic summary formerly in AbstractImageParser.
     */
    public static String toFileAttributeString(AbstractImageParser parser) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

        sb.append("File Attributes").append(System.lineSeparator());
        sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());

        BasicFileAttributes attr = Files.readAttributes(parser.getImageFile(), BasicFileAttributes.class);

        sb.append(String.format(MetadataConstants.FORMATTER, "File", parser.getImageFile()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Creation Time", df.format(attr.creationTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Last Access Time", df.format(attr.lastAccessTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Last Modified Time", df.format(attr.lastModifiedTime().toInstant())));
        sb.append(String.format(MetadataConstants.FORMATTER, "Image Format Type", parser.getImageFormat().getFileExtensionName()));

        // Ensure metadata isn't null before checking Byte Order
        if (parser.getMetadata() != null)
        {
            sb.append(String.format(MetadataConstants.FORMATTER, "Byte Order", parser.getMetadata().getByteOrder()));
        }

        sb.append(System.lineSeparator());

        return sb.toString();
    }

    public String toDiagnosticString(AbstractImageParser parser)
    {
        StringBuilder sb = new StringBuilder();
        Metadata<?> meta = parser.getMetadata();

        try
        {
            sb.append("\t\t\tTIF Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
            sb.append(toFileAttributeString(parser));

            if (meta instanceof TifMetadata)
            {
                TifMetadata tif = (TifMetadata) meta;

                if (tif.hasMetadata())
                {
                    for (DirectoryIFD ifd : tif)
                    {
                        sb.append(ifd);
                    }
                }

                else
                {
                    sb.append("No TIFF metadata found").append(System.lineSeparator());
                }

                if (tif.hasXmpData())
                {
                    sb.append(tif.getXmpDirectory());
                }

                else
                {
                    sb.append("No XMP metadata found").append(System.lineSeparator());
                }

                sb.append(MetadataConstants.DIVIDER);
            }
        }

        catch (Exception exc)
        {
            // LOGGER.error("Diagnostics failed for file [" + getImageFile() + "]", exc);

            sb.append("Error generating diagnostics [")
                    .append(exc.getClass().getSimpleName())
                    .append("]: ")
                    .append(exc.getMessage())
                    .append(System.lineSeparator());
        }

        return sb.toString();
    }
}
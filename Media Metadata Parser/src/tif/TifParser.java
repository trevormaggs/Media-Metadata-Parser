package tif;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import com.adobe.internal.xmp.XMPException;
import common.AbstractImageParser;
import common.DigitalSignature;
import common.MetadataConstants;
import common.Utils;
import logger.LogFactory;
import tif.tagspecs.TagIFD_Extension;
import xmp.XmpDirectory;
import xmp.XmpHandler;

/**
 * A concrete implementation of {@link AbstractImageParser} for extracting metadata from TIFF files.
 *
 * <p>
 * This parser interprets the 8-byte TIFF header, including byte order (either big-endian {@code MM}
 * or little-endian {@code II}), magic number 0x002A, and initial IFD offset and traverses the
 * linked list of Image File Directories (IFDs).
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 28 May 2026
 */
public class TifParser extends AbstractImageParser<TifMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TifParser.class);
    private final TifMetadata metadata;
    private boolean dataLoaded;

    /**
     * Creates an instance intended for parsing the specified TIFF image file.
     *
     * @param fpath
     *        the path to the TIFF file
     */
    public TifParser(Path fpath)
    {
        super(fpath);

        this.dataLoaded = false;
        this.metadata = new TifMetadata();

        String ext = Utils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("tif") && !ext.equalsIgnoreCase("tiff") && !ext.equalsIgnoreCase("dng"))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }
    }

    /**
     * Creates an instance for parsing the specified TIFF image file.
     *
     * @param file
     *        the path to the TIFF file
     */
    public TifParser(String file)
    {
        this(Paths.get(file));
    }

    /**
     * Parses TIFF metadata from a byte array, assuming it is a valid TIFF or EXIF payload,
     * including the 8-byte header length.
     * 
     * <p>
     * Optimised for cases where TIFF data, particularly an embedded EXIF segment, is already
     * present in memory. This avoids redundant disk I/O.
     * </p>
     *
     * @param payload
     *        the byte array containing TIFF-formatted data
     * @return a {@link TifMetadata} object, populated if successful, or empty if corrupt
     */
    public static TifMetadata parseTiffMetadataFromBytes(byte[] payload)
    {
        TifMetadata tif = new TifMetadata();

        try (IFDHandler handler = new IFDHandler(payload))
        {
            if (handler.parseMetadata())
            {
                populateMetadata(tif, handler);
                return tif;
            }

            else
            {
                LOGGER.warn("Unable to parse standalone TIFF/EXIF payload byte array successfully");
            }
        }

        catch (IOException exc)
        {
            LOGGER.error("Unable to handle inline byte stream array due to an I/O error", exc);
        }

        tif.setImageFormat(DigitalSignature.UNKNOWN);

        return tif;
    }

    /**
     * Parses TIFF and XMP metadata from the image file on disk.
     */
    @Override
    public void readMetadata()
    {
        if (!dataLoaded)
        {
            try
            {
                validateFileState();

                try (IFDHandler handler = new IFDHandler(getImageFile()))
                {
                    if (!handler.parseMetadata())
                    {
                        LOGGER.info("No compatible TIFF metadata directories found in [" + getImageFile() + "]");
                        metadata.setImageFormat(DigitalSignature.UNKNOWN);
                        return;
                    }

                    populateMetadata(metadata, handler);

                    if (!metadata.hasXmpData())
                    {
                        LOGGER.debug("No XMP payload found inside [" + getImageFile() + "]");
                    }
                }
            }

            catch (IOException exc)
            {
                LOGGER.error("File [" + getImageFile() + "] encountered an I/O error", exc);
                metadata.setImageFormat(DigitalSignature.UNKNOWN);
            }

            finally
            {
                dataLoaded = true;
            }
        }
    }

    /**
     * Retrieves the extracted metadata container, automatically executing the parsing operation
     * safely if it has not yet run. Guaranteed never to throw an UncheckedIOException.
     *
     * @return the TIFF metadata container (may be empty if parsing failed)
     */
    @Override
    public TifMetadata getMetadata()
    {
        readMetadata();
        return metadata;
    }

    /**
     * Generates a human-readable diagnostic string containing metadata segment details.
     *
     * @return a formatted string suitable for diagnostics, logging, or inspection
     */
    @Override
    public String formatDiagnosticString()
    {
        TifMetadata tif = getMetadata();
        StringBuilder sb = new StringBuilder();

        try
        {
            sb.append("\t\t\tTIF Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
            sb.append(super.formatDiagnosticString());
            sb.append(String.format(MetadataConstants.FORMATTER, "Byte Order", tif.getByteOrder()));
            sb.append(System.lineSeparator());

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

        catch (Exception exc)
        {
            LOGGER.error("Diagnostics failed for file [" + getImageFile() + "]", exc);

            sb.append("Error generating diagnostics [")
                    .append(exc.getClass().getSimpleName())
                    .append("]: ")
                    .append(exc.getMessage())
                    .append(System.lineSeparator());
        }

        return sb.toString();
    }

    /**
     * Populates a metadata container with directories and tags extracted by the TIF handler.
     *
     * @param target
     *        the {@link TifMetadata} instance to populate
     * @param handler
     *        the {@link IFDHandler} containing the successfully parsed structures
     */
    private static void populateMetadata(TifMetadata target, IFDHandler handler)
    {
        List<DirectoryIFD> directories = handler.getDirectories();
        target.setByteOrder(handler.getTifByteOrder());

        for (DirectoryIFD dir : directories)
        {
            target.addDirectory(dir);
        }

        // Traverse in reverse to honor the "last-one-wins" XMP strategy
        for (int i = directories.size() - 1; i >= 0; i--)
        {
            DirectoryIFD dir = directories.get(i);

            if (dir.hasTag(TagIFD_Extension.IFD_XML_PACKET))
            {
                byte[] rawXmp = dir.getRawByteArray(TagIFD_Extension.IFD_XML_PACKET);

                try
                {
                    XmpDirectory xmpDir = XmpHandler.addXmpDirectory(rawXmp);
                    target.addXmpDirectory(xmpDir);
                    break;
                }

                catch (XMPException exc)
                {
                    LOGGER.error("Unable to parse XMP directory payload structural XML data strings", exc);
                }
            }
        }

        if (handler.isDngVersion())
        {
            target.setImageFormat(DigitalSignature.DNG);
        }
    }
}
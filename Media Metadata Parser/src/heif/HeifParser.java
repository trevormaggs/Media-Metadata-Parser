package heif;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import com.adobe.internal.xmp.XMPException;
import common.AbstractImageParser;
import common.DigitalSignature;
import common.MetadataConstants;
import common.Utils;
import heif.boxes.Box;
import logger.LogFactory;
import tif.DirectoryIFD;
import tif.TifMetadata;
import tif.TifParser;
import xmp.XmpDirectory;
import xmp.XmpHandler;

/**
 * Parses HEIF/HEIC image files and extracts embedded metadata.
 *
 * HEIF files are based on the ISO Base Media File Format (ISOBMFF). This parser extracts Exif
 * metadata by navigating the box structure defined in {@code ISO/IEC 14496-12} and
 * {@code ISO/IEC 23008-12} documents.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class HeifParser extends AbstractImageParser<TifMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(HeifParser.class);
    private final TifMetadata metadata;
    private boolean dataLoaded;

    /**
     * Constructs an instance to parse a HEIC/HEIF file.
     *
     * @param fpath
     *        the image file path
     */
    public HeifParser(Path fpath)
    {
        super(fpath);

        String ext = Utils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("heic") && !ext.equalsIgnoreCase("heif") && !ext.equalsIgnoreCase("hif"))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }

        this.dataLoaded = false;
        this.metadata = new TifMetadata(BoxHandler.HEIF_BYTE_ORDER);
    }

    /**
     * Constructs an instance to parse a HEIC/HEIF file.
     *
     * @param file
     *        the image file path as a string
     */
    public HeifParser(String file)
    {
        this(Paths.get(file));
    }

    /**
     * Reads the HEIC/HEIF image file to extract all supported raw metadata segments, specifically
     * EXIF and XMP, if present, and uses the extracted data to initialise the necessary metadata
     * objects for later data retrieval.
     *
     * <p>
     * This method extracts only the Exif and/or XMP segment from the file. While other HEIF boxes
     * are parsed internally, they are not returned or exposed.
     * </p>
     *
     * @throws IOException
     *         if a file reading error occurs during the parsing
     */
    @Override
    public void readMetadata() throws IOException
    {
        if (!dataLoaded)
        {
            validateFileState();

            try (BoxHandler handler = new BoxHandler(getImageFile()))
            {
                if (handler.parseMetadata())
                {
                    Optional<byte[]> exif = handler.getExifData();

                    if (exif.isPresent())
                    {
                        TifMetadata tif = TifParser.parseTiffMetadataFromBytes(exif.get());
                        metadata.setByteOrder(tif.getByteOrder());

                        for (DirectoryIFD ifd : tif)
                        {
                            metadata.addDirectory(ifd);
                        }
                    }

                    else
                    {
                        LOGGER.info("No EXIF metadata present in file [" + getImageFile() + "]");
                    }

                    Optional<byte[]> xmp = handler.getXmpData();

                    if (xmp.isPresent())
                    {
                        processXmpData(xmp.get());
                    }

                    else
                    {
                        LOGGER.info("No XMP metadata present in file [" + getImageFile() + "]");
                    }

                    dataLoaded = true;
                }

                else
                {
                    throw new IOException("Invalid or corrupt ISOBMFF structural headers detected");
                }
            }
        }
    }

    /**
     * Retrieves the extracted metadata container from the HEIF image file. If the container has not
     * been filled, it triggers the lazy-loading operation to ensure availability.
     *
     * @return a {@link TifMetadata} object
     */
    @Override
    public TifMetadata getMetadata()
    {
        try
        {
            readMetadata();
        }

        catch (IOException exc)
        {
            throw new UncheckedIOException("Unable to parse file [" + getImageFile() + "] due to an error downstream", exc);
        }

        return metadata;
    }

    /**
     * Returns the detected {@code HEIF} format.
     *
     * @return a {@link DigitalSignature} enum constant representing this image format
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.HEIF;
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
            sb.append("\t\t\tHEIF Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
            sb.append(super.formatDiagnosticString());
            sb.append(String.format(MetadataConstants.FORMATTER, "Byte Order", metadata.getByteOrder()));
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
                sb.append("No EXIF metadata found").append(System.lineSeparator());
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
     * Logs the hierarchy of boxes at the debug level for diagnostic purposes.
     *
     * @param handler
     *        an active BoxHandler object
     */
    public void logDebugBoxHierarchy(BoxHandler handler)
    {
        LOGGER.debug("Box hierarchy:");

        for (Box box : handler)
        {
            box.logBoxInfo();
        }
    }

    /**
     * Parses raw XMP data and adds it to the metadata collection.
     *
     * @param rawXmp
     *        the XML packet bytes to be processed
     */
    private void processXmpData(byte[] rawXmp)
    {
        try
        {
            XmpDirectory xmpDir = XmpHandler.addXmpDirectory(rawXmp);

            if (xmpDir != null)
            {
                metadata.addXmpDirectory(xmpDir);
            }
        }

        catch (XMPException exc)
        {
            LOGGER.error("XMP parsing failed for file [" + getImageFile() + "]", exc);
        }
    }
}
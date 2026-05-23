package tif;

import java.io.IOException;
import java.io.UncheckedIOException;
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
 * linked list of Image File Directories (IFDs). It supports standard tags, custom extensions, and
 * nested sub-directories such as EXIF.
 * </p>
 * 
 * @see <a href="https://www.itu.int/itudoc/itu-t/com16/tiff-fx/docs/tiff6.pdf">TIFF 6.0
 *      Specification</a>
 * 
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2025
 */
public class TifParser extends AbstractImageParser<TifMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TifParser.class);

    private final TifMetadata metadata;
    private boolean dataLoaded = false;

    /**
     * Creates an instance intended for parsing the specified TIFF image file.
     *
     * @param fpath
     *        the path to the TIFF file
     */
    public TifParser(Path fpath)
    {
        super(fpath);

        String ext = Utils.getFileExtension(getImageFile());

        if (!"tif".equalsIgnoreCase(ext) && !"tiff".equalsIgnoreCase(ext))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }

        this.metadata = new TifMetadata();
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
     * @param payload
     *        the byte array containing TIFF-formatted data
     * @return a {@link TifMetadata} object, populated with parsed segments
     *
     * @throws IOException
     *         if data corruption or parsing exception detected
     */
    public static TifMetadata parseTiffMetadataFromBytes(byte[] payload) throws IOException
    {
        TifMetadata tif = new TifMetadata();

        try (IFDHandler handler = new IFDHandler(payload))
        {
            if (handler.parseMetadata())
            {
                populateMetadata(tif, handler);
            }

            else
            {
                throw new IOException("Invalid or corrupt Image File Directory (IFD) headers detected");
            }
        }

        catch (IOException exc)
        {
            LOGGER.error("Data corruption or access error detected", exc);
            throw exc;
        }

        return tif;
    }

    /**
     * Parses TIFF and XMP metadata from the image file.
     * 
     * @throws IOException
     *         if data corruption or I/O errors occur during traversal
     */
    @Override
    public void readMetadata() throws IOException
    {
        if (!dataLoaded)
        {
            validateFileState();

            try (IFDHandler handler = new IFDHandler(getImageFile()))
            {
                if (handler.parseMetadata())
                {
                    populateMetadata(metadata, handler);

                    if (!metadata.hasXmpData())
                    {
                        LOGGER.debug("No XMP payload found");
                    }

                    dataLoaded = true;
                }

                else
                {
                    throw new IOException("Invalid or corrupt Image File Directory (IFD) headers detected");
                }
            }

            catch (IOException exc)
            {
                LOGGER.error("Data corruption or access error detected", exc);
                throw exc;
            }
        }
    }

    /**
     * Retrieves the extracted metadata container, automatically triggering the parsing operation if
     * it has not yet been executed.
     *
     * @return the TIFF metadata container
     *
     * @throws UncheckedIOException
     *         if an unrecoverable I/O or corruption failure occurs during lazy parsing
     */
    @Override
    public TifMetadata getMetadata()
    {
        if (!dataLoaded)
        {
            try
            {
                readMetadata();
            }

            catch (IOException exc)
            {
                throw new UncheckedIOException("Lazy execution of readMetadata() failed downstream", exc);
            }
        }

        return metadata;
    }

    /**
     * Returns the detected TIFF format.
     *
     * @return a {@link DigitalSignature} enum class
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.TIF;
    }

    /**
     * Generates a human-readable diagnostic string containing metadata segment details.
     *
     * @return a formatted string suitable for diagnostics, logging, or inspection
     * 
     * @throws IOException
     *         if there is a problem reading the file
     */
    @Override
    public String formatDiagnosticString() throws IOException
    {
        if (!dataLoaded)
        {
            readMetadata();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\t\t\tTIF Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
        sb.append(super.formatDiagnosticString());

        sb.append(String.format(MetadataConstants.FORMATTER, "Byte Order", metadata.getByteOrder()));
        sb.append(System.lineSeparator());

        if (metadata.hasMetadata())
        {
            for (DirectoryIFD ifd : metadata)
            {
                sb.append(ifd);
            }
        }

        else
        {
            sb.append("No TIFF metadata found").append(System.lineSeparator());
        }

        if (metadata.hasXmpData())
        {
            sb.append(metadata.getXmpDirectory());
        }

        else
        {
            sb.append("No XMP metadata found").append(System.lineSeparator());
        }

        sb.append(MetadataConstants.DIVIDER);

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

        // Traverse in reverse to honour the "last-one-wins" XMP strategy
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
                    LOGGER.error("Unable to parse XMP directory payload", exc);
                }
            }
        }
    }
}
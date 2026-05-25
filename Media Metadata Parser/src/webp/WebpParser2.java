package webp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Optional;
import com.adobe.internal.xmp.XMPException;
import common.AbstractImageParser;
import common.DigitalSignature;
import common.MetadataConstants;
import common.Utils;
import jpg.JpgParser;
import logger.LogFactory;
import tif.DirectoryIFD;
import tif.TifMetadata;
import tif.TifParser;
import xmp.XmpHandler;

/**
 * A concrete implementation of {@link AbstractImageParser} for extracting metadata from WebP files
 * using a RIFF container chunk parsing architecture.
 *
 * <p>
 * This parser lazy-loads metadata by sequentially inspecting RIFF chunks. It extracts EXIF blocks
 * (parsing them as embedded TIFF structures) and XMP metadata packets, ensuring robust cross-format
 * extraction.
 * </p>
 *
 * @see <a href="https://developers.google.com/speed/webp/docs/riff_container">WebP RIFF Container
 *      Specification</a>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2025
 */
public class WebpParser2 extends AbstractImageParser<TifMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(WebpParser2.class);
    private static final EnumSet<WebPChunkType> DEFAULT_METADATA_CHUNKS = EnumSet.of(WebPChunkType.EXIF, WebPChunkType.XMP);
    private final TifMetadata metadata;
    private boolean dataLoaded;

    /**
     * Creates an instance intended for parsing the specified WebP image file.
     *
     * @param fpath
     *        the path to the WebP file
     */
    public WebpParser2(Path fpath)
    {
        super(fpath);

        String ext = Utils.getFileExtension(getImageFile());

        if (!"webp".equalsIgnoreCase(ext))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }

        this.dataLoaded = false;
        this.metadata = new TifMetadata();
    }

    /**
     * Creates an instance for parsing the specified WebP image file.
     *
     * @param file
     *        the path to the WebP file
     */
    public WebpParser2(String file)
    {
        this(Paths.get(file));
    }

    /**
     * Parses WebP structural chunks to load EXIF and XMP metadata segments.
     * 
     * @throws IOException
     *         if data corruption or I/O errors occur during chunk traversal
     */
    @Override
    public void readMetadata() throws IOException
    {
        if (!dataLoaded)
        {
            validateFileState();

            try (WebpHandler handler = new WebpHandler(getImageFile(), DEFAULT_METADATA_CHUNKS))
            {
                if (handler.parseMetadata())
                {
                    // Processing embedded EXIF chunk
                    Optional<WebpChunk> optExif = handler.getFirstChunk(WebPChunkType.EXIF);

                    if (optExif.isPresent())
                    {
                        byte[] strippedPayload = JpgParser.stripExifPreamble(optExif.get().getPayloadArray());
                        TifMetadata tif = TifParser.parseTiffMetadataFromBytes(strippedPayload);

                        if (tif != null)
                        {
                            for (DirectoryIFD ifd : tif)
                            {
                                metadata.addDirectory(ifd);
                            }
                        }
                    }

                    else
                    {
                        LOGGER.debug("No Exif segment found in file [" + getImageFile() + "]");
                    }

                    // Processing embedded XMP chunk
                    Optional<WebpChunk> optXmp = handler.getLastChunk(WebPChunkType.XMP);

                    if (optXmp.isPresent())
                    {
                        try
                        {
                            metadata.addXmpDirectory(XmpHandler.addXmpDirectory(optXmp.get().getPayloadArray()));
                        }

                        catch (XMPException exc)
                        {
                            LOGGER.error("Unable to parse XMP payload via Adobe XMPCore", exc);
                        }
                    }

                    else
                    {
                        LOGGER.debug("No XMP payload found in file [" + getImageFile() + "]");
                    }

                    dataLoaded = true;
                }

                else
                {
                    throw new IOException("Invalid or corrupt WebP structural headers detected");
                }
            }

            catch (IOException exc)
            {
                LOGGER.error("Data corruption or access error detected within WebP structural container", exc);
                throw exc;
            }
        }
    }

    /**
     * Retrieves the extracted metadata container, automatically triggering the parsing operation if
     * it has not yet been executed.
     *
     * @return the TIFF metadata container holding WebP payload fields
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
     * Returns the detected WebP format.
     *
     * @return a {@link DigitalSignature} enum class
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.WEBP;
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
            sb.append("\t\t\tWebP Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
            sb.append(super.formatDiagnosticString());

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
}
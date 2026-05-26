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
 * A concrete implementation of {@link AbstractImageParser} for extracting metadata from WebP image
 * files using a RIFF container chunk parsing architecture.
 *
 * <p>
 * This parser loads metadata by sequentially inspecting RIFF chunks. It extracts EXIF blocks
 * embedded within TIFF structures and XMP metadata packets.
 * </p>
 * 
 * <p>
 * <b>WebP Data Stream</b>
 * </p>
 *
 * <p>
 * The WebP file format is built on the RIFF container, beginning with a fixed signature:
 * </p>
 *
 * <pre>
 * <code>RIFF</code> + FileSize (4 bytes) + <code>WEBP</code>
 * </pre>
 *
 * <p>
 * This is followed by a sequence of chunks. Each chunk includes:
 * </p>
 *
 * <ul>
 * <li>4 bytes: Chunk FourCC (ASCII, for example: {@code VP8 }, {@code VP8X}, {@code EXIF})</li>
 * <li>4 bytes: Chunk payload size (unsigned, little-endian)</li>
 * <li>Payload: Variable-length data</li>
 * <li>Padding: If size is odd, 1 padding byte follows (not counted in the size field)</li>
 * </ul>
 *
 * <p>
 * There are both mandatory and optional chunk types.
 * </p>
 *
 * <p>
 * <b>Core Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>{@code VP8 } – Lossy bitstream (standard)</li>
 * <li>{@code VP8L} – Lossless bitstream</li>
 * <li>{@code VP8X} – Extended format chunk (required for metadata and animation)</li>
 * </ul>
 *
 * <p>
 * <b>Optional Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>{@code EXIF} – Embedded EXIF metadata</li>
 * <li>{@code ICCP} – Embedded ICC color profile</li>
 * <li>{@code XMP } – XMP metadata</li>
 * <li>{@code ANIM} / {@code ANMF} – Animation control and frames</li>
 * </ul>
 *
 * <p>
 * <b>Chunk Processing</b>
 * </p>
 *
 * <ul>
 * <li>Only chunks specified in the {@code requiredChunks} list are read</li>
 * <li>An empty {@code requiredChunks} list disables chunk extraction</li>
 * <li>A null list results in all chunks being extracted</li>
 * </ul>
 *
 * <p>
 * When the {@code EXIF} chunk is found, its payload is parsed as TIFF/EXIF-formatted metadata. This
 * is commonly used to extract orientation, date, and camera information.
 * </p>
 * 
 * @see <a href="https://developers.google.com/speed/webp/docs/riff_container">WebP RIFF Container
 *      Specification</a>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2025
 */
public class WebpParser extends AbstractImageParser<TifMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(WebpParser.class);
    private static final EnumSet<WebPChunkType> DEFAULT_CHUNK_FILTER = EnumSet.of(WebPChunkType.EXIF, WebPChunkType.XMP);
    private final TifMetadata metadata;
    private boolean dataLoaded;

    /**
     * Creates an instance intended for parsing the specified WebP image file.
     *
     * @param fpath
     *        the path to the WebP file
     */
    public WebpParser(Path fpath)
    {
        super(fpath);

        String ext = Utils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("webp"))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }

        this.dataLoaded = false;
        this.metadata = new TifMetadata(RiffHandler.WEBP_BYTE_ORDER);
    }

    /**
     * Creates an instance for parsing the specified WebP image file.
     *
     * @param file
     *        the path to the WebP file
     */
    public WebpParser(String file)
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

            try (RiffHandler handler = new RiffHandler(getImageFile(), DEFAULT_CHUNK_FILTER))
            {
                if (handler.parseMetadata())
                {
                    // Processing embedded EXIF chunk
                    Optional<WebpChunk> optExif = handler.getFirstChunk(WebPChunkType.EXIF);

                    if (optExif.isPresent())
                    {
                        byte[] strippedPayload = JpgParser.stripExifPreamble(optExif.get().getPayloadArray());

                        // tif is guaranteed non-null
                        TifMetadata tif = TifParser.parseTiffMetadataFromBytes(strippedPayload);
                        metadata.setByteOrder(tif.getByteOrder());

                        for (DirectoryIFD dir : tif)
                        {
                            metadata.addDirectory(dir);
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
     * Retrieves the extracted metadata container. If the container has not been filled, it triggers
     * the lazy-loading operation to ensure availability.
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
}
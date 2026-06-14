package webp;

import java.io.IOException;
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
 * This concrete class implements {@link AbstractImageParser} for extracting metadata from WebP
 * image files using a RIFF container chunk parsing architecture.
 *
 * <p>
 * This parser loads metadata by inspecting RIFF chunks sequentially. It then extracts Exif blocks
 * embedded within TIFF structures and XMP metadata packets.
 * </p>
 *
 * <h3>WebP Data Stream Layout</h3>
 * <p>
 * The WebP file format is built on the RIFF container, beginning with a fixed signature:
 * </p>
 * 
 * <pre>
 * {@code RIFF + FileSize (4 bytes) + WEBP}
 * </pre>
 *
 * <p>
 * This header is followed by a sequence of chunks. Each chunk includes:
 * </p>
 * 
 * <ul>
 * <li><b>4 bytes:</b> Chunk FourCC (ASCII, case-sensitive, e.g., {@code VP8 }, {@code VP8X},
 * {@code Exif})</li>
 * <li><b>4 bytes:</b> Chunk payload size (unsigned, little-endian, 32-bit integer)</li>
 * <li><b>Payload:</b> Variable-length data bytes</li>
 * <li><b>Padding:</b> If the size is odd, a single zero padding byte follows (not counted in the
 * size field)</li>
 * </ul>
 *
 * <h3>Core Chunk Types (Mandatory)</h3>
 * 
 * <ul>
 * <li>{@code VP8 } – Lossy bitstream (standard framework)</li>
 * <li>{@code VP8L} – Lossless bitstream</li>
 * <li>{@code VP8X} – Extended format chunk (required for metadata and animation attributes)</li>
 * </ul>
 *
 * <h3>Optional Chunk Types</h3>
 * 
 * <ul>
 * <li>{@code Exif} – Embedded EXIF metadata (Note: Mixed case)</li>
 * <li>{@code ICCP} – Embedded ICC color profile</li>
 * <li>{@code Xmp } – Embedded XMP metadata (Note: Mixed case with a trailing space)</li>
 * <li>{@code ANIM} / {@code ANMF} – Animation control and frame headers</li>
 * </ul>
 *
 * <h3>Chunk Processing Rules</h3>
 * 
 * <ul>
 * <li>Only chunks specified in the filtering criteria are read into memory.</li>
 * <li>An empty filter list disables chunk extraction entirely.</li>
 * <li>A {@code null} configuration results in all chunk segments being extracted.</li>
 * </ul>
 *
 * <p>
 * When the {@code Exif} chunk is isolated, its payload array is parsed as TIFF/EXIF-formatted
 * metadata to extract image attributes like orientation, timestamps, and camera profiles.
 * </p>
 *
 * @see <a href="https://developers.google.com/speed/webp/docs/riff_container">WebP RIFF Container
 *      Specification</a>
 * @author Trevor Maggs
 * @version 1.4
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
     * <p>
     * This method implements a soft-landing strategy. If a chunk payload is corrupted and throws an
     * unhandled runtime exception, the failure is caught upstream. However, this method guarantees
     * that the parser's state flag is updated via a {@code finally} block to prevent redundant disk
     * read actions on subsequent queries.
     * </p>
     */
    @Override
    public void readMetadata()
    {
        if (!dataLoaded)
        {
            try
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
                    }

                    else
                    {
                        LOGGER.info("No credible metadata payload detected in file [" + getImageFile() + "]");
                    }
                }
            }

            catch (IOException exc)
            {
                LOGGER.error("File [" + getImageFile() + "] encountered an unrecoverable structural I/O error", exc);
            }

            finally
            {
                dataLoaded = true;
            }
        }
    }

    /**
     * Retrieves the extracted metadata from the WebP image file. If the metadata has not been
     * explicitly loaded yet, it triggers lazy execution to read data.
     *
     * @return the TIFF metadata container holding WebP data payload
     */
    @Override
    public TifMetadata getMetadata()
    {
        readMetadata();
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
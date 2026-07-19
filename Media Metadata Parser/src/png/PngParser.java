package png;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import com.adobe.internal.xmp.XMPException;
import common.AbstractImageParser;
import common.MetadataConstants;
import common.Utils;
import logger.LogFactory;
import png.ChunkType.Category;
import tif.DirectoryIFD;
import tif.TifMetadata;
import tif.TifParser;
import xmp.XmpDirectory;
import xmp.XmpHandler;

/**
 * Parses PNG image files using a configurable set of chunk filters.
 *
 * <p>
 * Metadata is extracted only from the configured chunk types. Embedded EXIF and XMP metadata are
 * parsed automatically when present.
 * </p>
 *
 * <p>
 * <b>PNG Data Stream Structure</b>
 * </p>
 *
 * <p>
 * The PNG data stream begins with an 8-byte PNG SIGNATURE (0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A)
 * followed by a series of chunks. Each individual chunk consists of:
 * </p>
 *
 * <ul>
 * <li>4 bytes: Data field length (unsigned 31-bit integer)</li>
 * <li>4 bytes: Chunk type identifier (restricted to [65-90] and [97-122] ASCII codes)</li>
 * <li>Variable bytes: The actual chunk data payload</li>
 * <li>4 bytes: CRC-32 checksum computed from the chunk type and data fields only</li>
 * </ul>
 *
 * <p>
 * There are two categories of chunks: Critical (required for rendering) and Ancillary (metadata).
 * </p>
 *
 * <p>
 * <b>Critical Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>{@code IHDR} - Image header; must be the first chunk in the data stream.</li>
 * <li>{@code PLTE} - Palette table; required for indexed-colour PNG images.</li>
 * <li>{@code IDAT} - Image data chunk; multiple sequential instances are common.</li>
 * <li>{@code IEND} - Image trailer; marks the end of the PNG stream.</li>
 * </ul>
 *
 * <p>
 * <b>Ancillary Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>Transparency info: {@code tRNS}</li>
 * <li>Colour space info: {@code cHRM}, {@code gAMA}, {@code iCCP}, {@code sBIT}, {@code sRGB},
 * {@code cICP}, {@code mDCV}</li>
 * <li>Textual info: {@code iTXt}, {@code tEXt}, {@code zTXt}</li>
 * <li>Miscellaneous info: {@code bKGD}, {@code hIST}, {@code pHYs}, {@code sPLT}, {@code eXIf}</li>
 * <li>Time info: {@code tIME}</li>
 * <li>Animation information: {@code acTL}, {@code fcTL}, {@code fdAT}</li>
 * </ul>
 *
 * <p>
 * <b>Note:</b> In Windows Explorer, the {@code Date Taken} attribute is often resolved
 * preferentially from the {@code Creation Time} textual keyword rather than an embedded EXIF block.
 * This behaviour can alter the chronological ordering of PNG files when processed or sorted on
 * Windows filesystems.
 * </p>
 *
 * <pre>
 * -- For developmental testing --
 *
 * Examples of helpful exiftool diagnostic recipes:
 *
 * # View all time blocks categorized by group
 * exiftool -time:all -a -G0:1 -s testPNGimage.png
 *
 * # Explicitly overwrite all standard timestamps
 * exiftool.exe -overwrite_original -alldates="2012:10:07 11:15:45" testPNGimage.png
 *
 * # Synchronise the filesystem modification date with the internal PNG metadata value
 * exiftool.exe "-FileModifyDate&lt;PNG:CreationTime" testPNGimage.png
 *
 * # Set explicit internal textual creation timestamp
 * exiftool "-PNG:CreationTime=2015:07:14 01:15:27" testPNGimage.png
 * </pre>
 *
 * @see <a href="https://www.w3.org/TR/png">W3C Portable Network Graphics (PNG) Specification</a>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 08 July 2026
 */
public class PngParser extends AbstractImageParser<PngMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngParser.class);
    private static final EnumSet<ChunkType> DEFAULT_CHUNK_FILTER = EnumSet.of(ChunkType.tEXt, ChunkType.zTXt, ChunkType.iTXt, ChunkType.eXIf, ChunkType.tIME);
    private final PngMetadata metadata;
    private EnumSet<ChunkType> chunkFilter;
    private boolean dataLoaded;

    /**
     * Creates a PNG parser configured with the specified chunk filter.
     *
     * @param fpath
     *        the path to the PNG file
     * @param customFilter
     *        the chunk types to parse, or {@code null} to parse all supported chunk types
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public PngParser(Path fpath, EnumSet<ChunkType> customFilter) throws IOException
    {
        super(fpath);

        this.dataLoaded = false;
        this.metadata = new PngMetadata();
        this.chunkFilter = (customFilter == null) ? EnumSet.allOf(ChunkType.class) : customFilter;

        String ext = Utils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("png"))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }
    }

    /**
     * Creates a PNG parser configured with the specified chunk filter.
     *
     * @param file
     *        the path to the PNG file as a string
     * @param customFilter
     *        the chunk types to parse, or {@code null} to parse all supported chunk types
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public PngParser(String file, EnumSet<ChunkType> customFilter) throws IOException
    {
        this(Paths.get(file), customFilter);
    }

    /**
     * Creates a PNG parser with the default metadata chunk filter parameters.
     *
     * @param fpath
     *        the path to the PNG file
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public PngParser(Path fpath) throws IOException
    {
        this(fpath, DEFAULT_CHUNK_FILTER);
    }

    /**
     * Creates a PNG parser with the default metadata chunk filter parameters.
     *
     * @param file
     *        the path to the PNG file as a string
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public PngParser(String file) throws IOException
    {
        this(Paths.get(file), DEFAULT_CHUNK_FILTER);
    }

    /**
     * Updates the chunk filter used during metadata parsing.
     *
     * <p>
     * Passing {@code null} removes the filter and causes all supported chunk types to be parsed.
     * This method should be called before metadata is read. Changing the filter after parsing has
     * completed has no effect unless the metadata is reloaded.
     * </p>
     *
     * @param customFilter
     *        the chunk types to parse, or {@code null} to parse all supported chunk types
     */
    public void setChunkFilter(EnumSet<ChunkType> customFilter)
    {
        dataLoaded = false;
        chunkFilter = customFilter;
        metadata.clear();
    }

    /**
     * Reads metadata from the PNG file.
     *
     * <p>
     * Only the configured chunk types are processed. Embedded EXIF and XMP metadata are parsed
     * automatically when present.
     * </p>
     */
    @Override
    public void readMetadata()
    {
        if (!dataLoaded)
        {
            try (ChunkHandler handler = new ChunkHandler(getImageFile(), chunkFilter))
            {
                if (handler.parseMetadata())
                {
                    PngChunkITXT xmpChunkFound = null;
                    List<PngChunk> chunks = handler.getChunks().orElse(Collections.emptyList());

                    for (PngChunk chunk : chunks)
                    {
                        Category cat = chunk.getType().getCategory();
                        PngDirectory dir = metadata.getDirectory(cat);

                        if (dir == null)
                        {
                            dir = new PngDirectory(cat);
                            metadata.addDirectory(dir);
                        }

                        dir.add(chunk);

                        /*
                         * XMP data should come from the last iTXt chunk, so keep going
                         * until the last matching XMP segment is found.
                         */
                        if (chunk.getType() == ChunkType.iTXt && chunk instanceof PngChunkITXT)
                        {
                            PngChunkITXT itxt = (PngChunkITXT) chunk;

                            if (itxt.hasKeyword(TextKeyword.XMP))
                            {
                                xmpChunkFound = itxt;
                            }
                        }
                    }

                    if (xmpChunkFound != null)
                    {
                        try
                        {
                            String xmlString = xmpChunkFound.getText();
                            XmpDirectory xmpDir = XmpHandler.addXmpDirectory(xmlString.getBytes(StandardCharsets.UTF_8));
                            metadata.addXmpDirectory(xmpDir);
                        }

                        catch (XMPException exc)
                        {
                            LOGGER.error("Unable to parse XMP directory payload in file [" + getImageFile() + "]", exc);
                        }
                    }
                }

                else
                {
                    LOGGER.info("No credible metadata payload detected in file [" + getImageFile() + "]");
                }

                dataLoaded = true;
            }

            catch (IOException exc)
            {
                LOGGER.error("File [" + getImageFile() + "] encountered an unrecoverable structural I/O error", exc);
            }
        }
    }

    /**
     * Retrieves the extracted metadata from the PNG image file. If the metadata has not been
     * explicitly loaded yet, it triggers lazy execution to read data.
     *
     * @return the extracted PNG metadata
     */
    @Override
    public PngMetadata getMetadata()
    {
        readMetadata();
        return metadata;
    }

    /**
     * Returns a formatted diagnostic summary of the extracted metadata.
     *
     * @return a formatted string suitable for diagnostics, logging, or inspection
     */
    @Override
    public String formatDiagnosticString()
    {
        PngMetadata png = getMetadata();
        StringBuilder sb = new StringBuilder();

        try
        {
            sb.append("\t\t\tPNG Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
            sb.append(super.formatDiagnosticString());
            sb.append(String.format(MetadataConstants.FORMATTER, "Byte Order", png.getByteOrder()));
            sb.append(System.lineSeparator());

            for (PngDirectory dir : png)
            {
                sb.append(dir.getCategory().getDescription()).append(System.lineSeparator());
                sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());
                sb.append(dir);
                sb.append(System.lineSeparator());
            }

            if (png.hasExifData())
            {
                PngDirectory dir = png.getDirectory(Category.MISC);
                PngChunk chunk = (dir != null) ? dir.getFirstChunk(ChunkType.eXIf) : null;

                if (chunk != null)
                {
                    sb.append("Embedded EXIF Metadata").append(System.lineSeparator());
                    sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());

                    TifMetadata exif = TifParser.parseTiffMetadataFromBytes(chunk.getPayloadArray());

                    if (exif.hasExifData())
                    {
                        for (DirectoryIFD ifd : exif)
                        {
                            sb.append(ifd);
                        }
                    }

                    else
                    {
                        sb.append("Embedded EXIF binary block is corrupt or unreadable").append(System.lineSeparator());
                    }

                    sb.append(System.lineSeparator());
                }
            }

            if (png.hasXmpData())
            {
                sb.append("Embedded XMP Payload").append(System.lineSeparator());
                sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());
                sb.append(png.getXmpDirectory());
                sb.append(System.lineSeparator());
            }

            sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());
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
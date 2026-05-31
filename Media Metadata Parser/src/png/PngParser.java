package png;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import com.adobe.internal.xmp.XMPException;
import common.AbstractImageParser;
import common.DigitalSignature;
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
 * Parses PNG image files to extract metadata structured within sequential chunks. Processes textual
 * chunks (tEXt, iTXt, zTXt), modification time (tIME), and embedded EXIF (eXIf) data blocks using a
 * structured internal data holder.
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
 * preferentially
 * from the {@code Creation Time} textual keyword rather than an embedded EXIF block. This behaviour
 * can
 * alter the chronological ordering of PNG files when processed or sorted on Windows filesystems.
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
 * exiftool.exe "-FileModifyDate<PNG:CreationTime" testPNGimage.png
 *
 * # Set explicit internal textual creation timestamp
 * exiftool "-PNG:CreationTime=2015:07:14 01:15:27" testPNGimage.png
 * </pre>
 *
 * @see <a href="https://www.w3.org/TR/png">W3C Portable Network Graphics (PNG) Specification</a>
 * @author Trevor Maggs
 * @version 1.1
 * @since 13 August 2025
 */
public class PngParser extends AbstractImageParser<PngMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngParser.class);
    private static final EnumSet<ChunkType> DEFAULT_CHUNK_FILTER = EnumSet.of(ChunkType.tEXt, ChunkType.zTXt, ChunkType.iTXt, ChunkType.eXIf, ChunkType.tIME);
    private final PngMetadata metadata;
    private boolean dataLoaded;

    private static class PngChunkData
    {
        private final List<PngChunk> textualChunks;
        private final PngChunk exif;
        private final PngChunk time;
        private final PngChunkITXT xmp;

        private PngChunkData()
        {
            this.textualChunks = Collections.emptyList();
            this.exif = null;
            this.time = null;
            this.xmp = null;
        }

        private PngChunkData(List<PngChunk> textualChunks, PngChunk exifChunk, PngChunk timeChunk, PngChunkITXT xmpChunk)
        {
            if (textualChunks != null && !textualChunks.isEmpty())
            {
                this.textualChunks = Collections.unmodifiableList(new ArrayList<>(textualChunks));
            }

            else
            {
                this.textualChunks = Collections.emptyList();
            }

            this.exif = exifChunk;
            this.time = timeChunk;
            this.xmp = xmpChunk;
        }

        private Optional<List<PngChunk>> getTextualChunks()
        {
            return textualChunks.isEmpty() ? Optional.empty() : Optional.of(textualChunks);
        }

        private Optional<PngChunk> getExifChunk()
        {
            return Optional.ofNullable(exif);
        }

        private Optional<PngChunk> getTimeChunk()
        {
            return Optional.ofNullable(time);
        }

        private Optional<PngChunkITXT> getXmpChunk()
        {
            return Optional.ofNullable(xmp);
        }

        private boolean isEmpty()
        {
            return (exif == null && xmp == null && time == null && textualChunks.isEmpty());
        }
    }

    /**
     * Constructs a new instance with the specified file path.
     *
     * @param fpath
     *        the path to the PNG file as an encapsulated object
     * 
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public PngParser(Path fpath) throws IOException
    {
        super(fpath);

        String ext = Utils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("png"))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }

        this.dataLoaded = false;
        this.metadata = new PngMetadata();
    }

    /**
     * Constructs a new instance from a file path string.
     *
     * @param file
     *        the path to the PNG file as a string
     * 
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public PngParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Parses the PNG file to extract filtered metadata chunks, such as EXIF, textual properties,
     * and embedded XMP data and populates the internal directories.
     */
    @Override
    public void readMetadata()
    {
        if (!dataLoaded)
        {
            try
            {
                validateFileState();

                PngChunkData chunkData = loadMetadata(DEFAULT_CHUNK_FILTER);

                if (chunkData.isEmpty())
                {
                    LOGGER.info("No credible metadata payload detected in file [" + getImageFile() + "]");
                }

                else
                {
                    if (chunkData.getTextualChunks().isPresent())
                    {
                        PngDirectory textualDir = new PngDirectory(Category.TEXTUAL);

                        textualDir.addChunkList(chunkData.getTextualChunks().get());
                        metadata.addDirectory(textualDir);
                    }

                    if (chunkData.getExifChunk().isPresent())
                    {
                        PngDirectory exifDir = new PngDirectory(ChunkType.eXIf.getCategory());

                        exifDir.add(chunkData.getExifChunk().get());
                        metadata.addDirectory(exifDir);
                    }

                    if (chunkData.getTimeChunk().isPresent())
                    {
                        PngDirectory timeDir = new PngDirectory(ChunkType.tIME.getCategory());

                        timeDir.add(chunkData.getTimeChunk().get());
                        metadata.addDirectory(timeDir);
                    }

                    if (chunkData.getXmpChunk().isPresent())
                    {
                        try
                        {
                            String xmlString = chunkData.getXmpChunk().get().getText();
                            XmpDirectory xmpDir = XmpHandler.addXmpDirectory(xmlString.getBytes(StandardCharsets.UTF_8));

                            metadata.addXmpDirectory(xmpDir);
                        }

                        catch (XMPException exc)
                        {
                            LOGGER.error("Unable to parse XMP directory payload in file [" + getImageFile() + "]", exc);
                        }
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
     * Retrieves the extracted metadata from the PNG image file. If the metadata has not been
     * explicitly loaded yet, it triggers lazy execution to read data.
     *
     * @return a PngMetadata object populated with extracted directories, or empty
     */
    @Override
    public PngMetadata getMetadata()
    {
        readMetadata();
        return metadata;
    }

    /**
     * Returns the detected {@code PNG} format.
     *
     * @return DigitalSignature.PNG
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.PNG;
    }

    /**
     * Generates a human-readable diagnostic string containing metadata details.
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

            if (png.hasTextualData())
            {
                for (PngDirectory cd : png)
                {
                    if (cd.getCategory() == Category.TEXTUAL)
                    {
                        sb.append("Textual Chunks").append(System.lineSeparator());
                        sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());
                        sb.append(cd);
                        break;
                    }
                }
            }

            else
            {
                sb.append("No textual metadata found").append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());

            PngDirectory timeCD = png.getDirectory(Category.TIME);

            if (timeCD != null)
            {
                sb.append("Time Modification Chunk").append(System.lineSeparator());
                sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());
                sb.append(timeCD);
            }

            else
            {
                sb.append("No time modification metadata found").append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());

            if (png.hasExifData())
            {
                PngDirectory cd = png.getDirectory(Category.MISC);
                PngChunk chunk = (cd != null) ? cd.getFirstChunk(ChunkType.eXIf) : null;

                if (chunk != null)
                {
                    sb.append("EXIF Metadata").append(System.lineSeparator());
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
                        LOGGER.warn("Corrupt EXIF block skipped during diagnostic compilation for [" + getImageFile() + "]");
                    }
                }

                else
                {
                    sb.append("No EXIF metadata found. eXIf chunk missing from MISC directory").append(System.lineSeparator());
                }
            }

            else
            {
                sb.append("No EXIF metadata found").append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());

            if (png.hasXmpData())
            {
                sb.append(png.getXmpDirectory());
            }

            else
            {
                sb.append("No XMP metadata found").append(System.lineSeparator());
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

    /**
     * Loads the filtered metadata chunks from the PNG image file.
     *
     * @param chunkFilter
     *        a filter set of chunk types to allow specific chunks to be processed
     * @return a PngChunkData record containing the extracted chunks
     * 
     * @throws IOException
     *         if an unrecoverable issue occurs while accessing the filesystem or reading the stream
     */
    private PngChunkData loadMetadata(EnumSet<ChunkType> chunkFilter) throws IOException
    {
        PngChunkData chunkData;

        try (ChunkHandler handler = new ChunkHandler(getImageFile(), chunkFilter))
        {
            if (handler.parseMetadata())
            {
                List<PngChunk> textList = handler.getChunks(Category.TEXTUAL).orElse(null);
                PngChunk exif = handler.getFirstChunk(ChunkType.eXIf).orElse(null);
                PngChunk time = handler.getFirstChunk(ChunkType.tIME).orElse(null);
                PngChunkITXT xmp = handler.getXmpChunk().orElse(null);

                chunkData = new PngChunkData(textList, exif, time, xmp);
            }

            else
            {
                // Empty fallback object
                chunkData = new PngChunkData();
            }
        }

        return chunkData;
    }
}
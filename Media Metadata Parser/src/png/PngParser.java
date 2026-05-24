package png;

import java.io.IOException;
import java.io.UncheckedIOException;
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
 * Parses PNG image files to extract metadata structured within sequential chunks.
 * Processes textual chunks (tEXt, iTXt, zTXt), modification time (tIME), and embedded
 * EXIF (eXIf) data blocks using an asset payload isolation layer.
 *
 * <p>
 * <strong>References:</strong><br>
 * For full technical requirements on standard metadata profile handling within PNG structures,
 * see the section "XMP in PNG" inside the <strong>Adobe XMP Specification Part 3:
 * Storage in Files</strong> documentation provided by Adobe Systems Incorporated.
 * </p>
 *
 * <p>
 * <strong>Technical Summary:</strong><br>
 * Unlike offset-mapped TIFF structures or size-constrained JPEG APP markers, the PNG format
 * organizes data into sequentially ordered, self-contained chunks. Each chunk declares its
 * payload size via a 4-byte big-endian field, followed by a 4-byte ASCII chunk identifier tag,
 * the variable payload bytes, and a trailing CRC-32 token. This parser scans, validates, and
 * extracts metadata blocks synchronously via a transactional isolation record step.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.9.2
 * @since 22 May 2026
 */
public class PngParser extends AbstractImageParser<PngMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngParser.class);
    private PngChunkData chunkData;
    private boolean dataLoaded;
    private final PngMetadata metadata;

    /**
     * An immutable data carrier for the raw metadata chunk layouts extracted
     * from the PNG file stream before directory tree compilation.
     */
    private static class PngChunkData
    {
        private final List<PngChunk> textualChunks;
        private final PngChunk exifChunk;
        private final PngChunk timeChunk;
        private final PngChunk itxtXmpChunk;

        private PngChunkData(List<PngChunk> textualChunks, PngChunk exifChunk, PngChunk timeChunk, PngChunk itxtXmpChunk)
        {
            if (textualChunks != null && !textualChunks.isEmpty())
            {
                this.textualChunks = Collections.unmodifiableList(new ArrayList<PngChunk>(textualChunks));
            }
            else
            {
                this.textualChunks = Collections.emptyList();
            }

            this.exifChunk = exifChunk;
            this.timeChunk = timeChunk;
            this.itxtXmpChunk = itxtXmpChunk;
        }

        private Optional<List<PngChunk>> getTextualChunks()
        {
            return textualChunks.isEmpty() ? Optional.<List<PngChunk>> empty() : Optional.of(textualChunks);
        }

        private Optional<PngChunk> getExifChunk()
        {
            return Optional.ofNullable(exifChunk);
        }

        private Optional<PngChunk> getTimeChunk()
        {
            return Optional.ofNullable(timeChunk);
        }

        private Optional<PngChunk> getItxtXmpChunk()
        {
            return Optional.ofNullable(itxtXmpChunk);
        }
    }

    /**
     * Constructs a new instance from a file path string.
     *
     * @param file the path to the PNG file as a string
     * @throws IOException if the file cannot be opened or read
     */
    public PngParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Constructs a new instance with the specified file path.
     *
     * @param fpath the path to the PNG file as an encapsulated object
     * @throws IOException if the file cannot be opened or read
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
     * Reads the PNG file to extract supported metadata chunks, including EXIF, textual properties,
     * and embedded XMP data payloads. This method fully populates internal directories.
     *
     * @throws IOException if an error occurs while reading or parsing the file structure
     */
    @Override
    public void readMetadata() throws IOException
    {
        if (dataLoaded)
        {
            return;
        }

        validateFileState();

        EnumSet<ChunkType> chunkSet = EnumSet.of(ChunkType.tEXt, ChunkType.zTXt, ChunkType.iTXt, ChunkType.eXIf, ChunkType.tIME);

        try (ChunkHandler handler = new ChunkHandler(getImageFile(), chunkSet))
        {
            if (!handler.parseMetadata())
            {
                LOGGER.warn("No metadata information found in file [" + getImageFile() + "]");

                // Defensively initialise carrier empty to rule out downstream NullPointerExceptions
                chunkData = new PngChunkData(new ArrayList<PngChunk>(), null, null, null);
                dataLoaded = true;
                return;
            }

            // Stage 1: Isolate and collect the asset elements into the carrier record
            List<PngChunk> textList = null;
            PngChunk exif = null;
            PngChunk time = null;
            PngChunk xmpItxt = null;

            Optional<List<PngChunk>> optList = handler.getChunks(Category.TEXTUAL);
            if (optList.isPresent())
            {
                textList = optList.get();
            }

            Optional<PngChunk> optExif = handler.getFirstChunk(ChunkType.eXIf);
            if (optExif.isPresent())
            {
                exif = optExif.get();
            }

            Optional<PngChunk> optTime = handler.getFirstChunk(ChunkType.tIME);
            if (optTime.isPresent())
            {
                time = optTime.get();
            }

            // Targeted selective retrieval matching XMP keywords specifically
            Optional<PngChunk> optITxt = handler.getXmpItxtChunk();
            
            if (optITxt.isPresent())
            {
                xmpItxt = optITxt.get();
            }

            this.chunkData = new PngChunkData(textList, exif, time, xmpItxt);
        }

        // Stage 2: Compile internal structural directories from isolated raw chunks
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

        if (chunkData.getItxtXmpChunk().isPresent())
        {
            try
            {
                // Enforce platform default big endian constraints via shared Metadata contract
                String xmlString = ((PngChunkITXT) chunkData.getItxtXmpChunk().get()).getText();
                XmpDirectory xmpDir = XmpHandler.addXmpDirectory(xmlString.getBytes(StandardCharsets.UTF_8));
                metadata.addXmpDirectory(xmpDir);
            }
            catch (XMPException exc)
            {
                LOGGER.error("Unable to parse XMP directory payload in file [" + getImageFile() + "]", exc);
            }
        }

        dataLoaded = true;
    }

    /**
     * Retrieves the extracted metadata from the PNG image file. If the metadata has not
     * been explicitly loaded yet, it triggers lazy execution.
     *
     * @return a PngMetadata object populated with extracted directories, or empty
     * @throws UncheckedIOException if an unrecoverable issue occurs during lazy evaluation
     */
    @Override
    public PngMetadata getMetadata()
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
     * @throws IOException if lower-level filesystem attributes are inaccessible
     */
    @Override
    public String formatDiagnosticString() throws IOException
    {
        if (!dataLoaded)
        {
            readMetadata();
        }

        PngMetadata meta = getMetadata();
        StringBuilder sb = new StringBuilder();

        try
        {
            sb.append("\t\t\tPNG Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
            sb.append(super.formatDiagnosticString());

            if (!(meta instanceof PngMetadataProvider))
            {
                sb.append("No PNG metadata available").append(System.lineSeparator());
                return sb.toString();
            }

            PngMetadataProvider png = (PngMetadataProvider) meta;

            // 1. Diagnostic: Textual Data
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

            // 2. Diagnostic: Time Modification Data
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

            // 3. Diagnostic: EXIF Data
            if (png.hasExifData())
            {
                PngChunk chunk = null;
                PngDirectory cd = png.getDirectory(Category.MISC);

                if (cd != null)
                {
                    chunk = cd.getFirstChunk(ChunkType.eXIf);
                }

                if (chunk != null)
                {
                    TifMetadata exif = TifParser.parseTiffMetadataFromBytes(chunk.getPayloadArray());

                    sb.append("EXIF Metadata").append(System.lineSeparator());
                    sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());

                    for (DirectoryIFD ifd : exif)
                    {
                        sb.append(ifd);
                    }
                }
                else
                {
                    String reason = (cd == null) ? "MISC directory missing" : "eXIf chunk missing from MISC directory";
                    sb.append("No EXIF metadata found. ").append(reason).append(System.lineSeparator());
                }
            }
            else
            {
                sb.append("No EXIF metadata found").append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());

            // 4. Diagnostic: XMP Data
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
}
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
 * Parses PNG image files to extract metadata structured within sequential chunks. Processes textual
 * chunks (tEXt, iTXt, zTXt), modification time (tIME), and embedded EXIF (eXIf) data blocks using
 * an asset payload isolation layer.
 *
 * Normally, most PNG files do not contain the EXIF structure, however, it will attempt to search
 * for these 4 potential chunks: ChunkType.eXIf, ChunkType.tEXt, ChunkType.iTXt or ChunkType.zTXt.
 *
 * <p>
 * <b>PNG Data Stream</b>
 * </p>
 *
 * <p>
 * The PNG data stream begins with a PNG SIGNATURE (0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A)
 * followed by a series of chunks. Each chunk consists of:
 * </p>
 *
 * <ul>
 * <li>4 bytes for data field length (unsigned, usually &lt;= 31 bytes)</li>
 * <li>4 bytes for chunk type (only [65-90] and [97-122]) ASCII codes</li>
 * <li>Variable number of bytes for data field</li>
 * <li>4 bytes for CRC computed from chunk type and data only</li>
 * </ul>
 *
 * <p>
 * There are two categories of chunks: Critical and Ancillary.
 * </p>
 *
 * <p>
 * <b>Critical Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>IHDR - image header, always the first chunk in the data stream</li>
 * <li>PLTE - palette table, relevant for indexed PNG images</li>
 * <li>IDAT - image data chunk, multiple occurrences likely</li>
 * <li>IEND - image trailer, always the last chunk in the data stream</li>
 * </ul>
 *
 * <p>
 * <b>Ancillary Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>Transparency info: tRNS</li>
 * <li>Colour space info: cHRM, gAMA, iCCP, sBIT, sRGB, cICP, mDCV</li>
 * <li>Textual info: iTXt, tEXt, zTXt</li>
 * <li>Miscellaneous info: bKGD, hIST, pHYs, sPLT, eXIf</li>
 * <li>Time info: tIME</li>
 * <li>Animation information: acTL, fcTL, fdAT</li>
 * </ul>
 * 
 * <p>
 * <b>Note:</b> In Windows Explorer, the {@code Date Taken} attribute is often resolved from the
 * {@code Creation Time} textual keyword rather than the embedded EXIF block. This behaviour can
 * affect the chronological ordering of PNG files when viewed or processed on Windows systems.
 * </p>
 *
 * {@literal
 *  -- For developmental testing --
 *
 * <u>Some examples of exiftool usages</u>
 *
 * exiftool -time:all -a -G0:1 -s testPNGimage.png
 * exiftool.exe -overwrite_original -alldates="2012:10:07 11:15:45" testPNGimage.png
 * exiftool.exe "-FileModifyDate<PNG:CreationTime" testPNGimage.png
 *
 * exiftool "-PNG:CreationTime=2015:07:14 01:15:27" testPNGimage.png
 * exiftool -filemodifydate="2024:08:10 00:00:00" -createdate="2024:08:10 00:00:00"
 * "-PNG:CreationTime<FileModifyDate" testPNGimage.png
 * }
 *
 * @see <a href="https://www.w3.org/TR/png">See this link for more technical PNG background
 *      information.</a>
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
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
        private final PngChunk exif;
        private final PngChunk time;
        private final PngChunk xmp;

        private PngChunkData()
        {
            this.textualChunks = Collections.emptyList();
            this.exif = null;
            this.time = null;
            this.xmp = null;
        }

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

            this.exif = exifChunk;
            this.time = timeChunk;
            this.xmp = itxtXmpChunk;
        }

        private Optional<List<PngChunk>> getTextualChunks()
        {
            return textualChunks.isEmpty() ? Optional.<List<PngChunk>> empty() : Optional.of(textualChunks);
        }

        private Optional<PngChunk> getExifChunk()
        {
            return Optional.ofNullable(exif);
        }

        private Optional<PngChunk> getTimeChunk()
        {
            return Optional.ofNullable(time);
        }

        private Optional<PngChunk> getItxtXmpChunk()
        {
            return Optional.ofNullable(xmp);
        }
    }

    /**
     * Constructs a new instance from a file path string.
     *
     * @param file
     *        the path to the PNG file as a string
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public PngParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Constructs a new instance with the specified file path.
     *
     * @param fpath
     *        the path to the PNG file as an encapsulated object
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
     * Reads the PNG file to extract supported metadata chunks, including EXIF, textual properties,
     * and embedded XMP data payloads. This method fully populates internal directories.
     *
     * @throws IOException
     *         if an error occurs while reading or parsing the file structure
     */
    @Override
    public void readMetadata() throws IOException
    {
        if (!dataLoaded)
        {
            validateFileState();

            EnumSet<ChunkType> chunkSet = EnumSet.of(ChunkType.tEXt, ChunkType.zTXt, ChunkType.iTXt, ChunkType.eXIf, ChunkType.tIME);

            try (ChunkHandler handler = new ChunkHandler(getImageFile(), chunkSet))
            {
                if (handler.parseMetadata())
                {
                    List<PngChunk> textList = null;
                    PngChunk exif = null;
                    PngChunk time = null;
                    PngChunk xmp = null;

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
                        xmp = optITxt.get();
                    }

                    chunkData = new PngChunkData(textList, exif, time, xmp);
                }

                else
                {
                    dataLoaded = true;
                    chunkData = new PngChunkData(); // Empty object
                    LOGGER.warn("No metadata information found in file [" + getImageFile() + "]");

                    return;
                }
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
    }

    /**
     * Retrieves the extracted metadata from the PNG image file. If the metadata has not
     * been explicitly loaded yet, it triggers lazy execution.
     *
     * @return a PngMetadata object populated with extracted directories, or empty
     * @throws UncheckedIOException
     *         if an unrecoverable issue occurs during lazy evaluation
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
     * 
     * @throws IOException
     *         if lower-level filesystem attributes are inaccessible
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

            if (meta instanceof PngMetadataProvider)
            {
                PngMetadataProvider png = (PngMetadataProvider) meta;

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
                    PngChunk chunk = null;
                    PngDirectory cd = png.getDirectory(Category.MISC);

                    if (cd != null)
                    {
                        chunk = cd.getFirstChunk(ChunkType.eXIf);

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
                            sb.append("No EXIF metadata found. eXIf chunk missing from MISC directory").append(System.lineSeparator());
                        }
                    }

                    else
                    {
                        sb.append("No EXIF metadata found. MISC directory missing").append(System.lineSeparator());
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

            else
            {
                sb.append("No PNG metadata available").append(System.lineSeparator());
            }
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
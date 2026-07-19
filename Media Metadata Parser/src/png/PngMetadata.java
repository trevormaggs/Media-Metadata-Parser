package png;

import java.nio.ByteOrder;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import common.DigitalSignature;
import common.MetadataConstants;
import png.ChunkType.Category;
import tif.TifMetadata;
import tif.TifParser;
import util.SmartDateParser;
import xmp.XmpDirectory;
import xmp.XmpProperty;

/**
 * Implements the {@link PngMetadataProvider} interface and provides access to metadata extracted
 * from PNG files.
 * 
 * This class aggregates various PNG chunk directories, prioritising embedded metadata standards
 * like EXIF and XMP for accurate data extraction.
 *
 * <p>
 * It organises metadata into directories based on chunk category, for example: TEXTUAL, MISC, etc
 * and automatically parses embedded XMP data if an iTXt chunk is detected.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 12 November 2025
 */
public class PngMetadata implements PngMetadataProvider
{
    private final Map<Category, PngDirectory> pngMap;
    private XmpDirectory xmpDir;

    /**
     * Constructs an empty {@code PngMetadata} object, initialising the internal map for storing PNG
     * directories.
     */
    public PngMetadata()
    {
        this.pngMap = new HashMap<>();
    }

    /**
     * Adds a new directory to the PNG collection, organised by its category.
     *
     * @param directory
     *        the {@link PngDirectory} to add
     *
     * @throws NullPointerException
     *         if the directory parameter is null
     */
    @Override
    public void addDirectory(PngDirectory directory)
    {
        if (directory == null)
        {
            throw new NullPointerException("Directory cannot be null");
        }

        pngMap.putIfAbsent(directory.getCategory(), directory);
    }

    /**
     * Removes a directory from the collection based on its category.
     *
     * @param directory
     *        the {@link PngDirectory} whose category is used for removal. Must not be null
     * @return true if a directory was removed, otherwise false
     *
     * @throws NullPointerException
     *         if the directory parameter is null.
     */
    @Override
    public boolean removeDirectory(PngDirectory directory)
    {
        if (directory == null)
        {
            throw new NullPointerException("Directory cannot be null");
        }

        return (pngMap.remove(directory.getCategory()) != null);
    }

    /**
     * Resets the metadata container by purging all parsed directories and standalone metadata
     * profiles.
     */
    @Override
    public void clear()
    {
        this.pngMap.clear();
        this.xmpDir = null;
    }

    /**
     * Checks if the metadata collection is empty.
     *
     * @return true if the collection is empty, otherwise false
     */
    @Override
    public boolean isEmpty()
    {
        return !hasMetadata();
    }

    /**
     * Returns the byte order, indicating how data values are interpreted correctly.
     *
     * @return always {@link java.nio.ByteOrder#BIG_ENDIAN}
     */
    @Override
    public ByteOrder getByteOrder()
    {
        return ByteOrder.BIG_ENDIAN;
    }

    /**
     * Returns the {@code PNG} format.
     *
     * @return DigitalSignature.PNG
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.PNG;
    }

    /**
     * Checks if the PNG image contains any metadata directories.
     *
     * @return true if the collection is not empty, otherwise false
     */
    @Override
    public boolean hasMetadata()
    {
        return (!pngMap.isEmpty() || hasXmpData());
    }

    /**
     * Extracts the most authoritative creation date available, returning a {@link ZonedDateTime}
     * derived from a prioritised metadata hierarchy across PNG chunks and embedded blocks.
     *
     * <p>
     * This method uses the following priority order when searching for a creation timestamp:
     * </p>
     *
     * <ol>
     * <li>Embedded <b>EXIF Sub-IFD</b> payload: {@code DateTimeOriginal} (Tag 0x9003)</li>
     * <li>Embedded <b>EXIF Main IFD0</b> payload: {@code DateTimeOriginal} or {@code DateTime}</li>
     * <li>Embedded <b>XMP EXIF Schema</b> text payload: {@code DateTimeOriginal}</li>
     * <li>Embedded <b>XMP General Schema</b> text payload: {@code CreateDate}</li>
     * <li>Ancillary PNG <b>tEXt/iTXt</b> chunk: Keyword 'Creation Time'</li>
     * <li>Critical PNG <b>tIME</b> chunk: Last modification timestamp fallback</li>
     * </ol>
     *
     * @return the extracted {@link ZonedDateTime}, or {@code null} if no valid timestamp is
     *         detected across any metadata blocks
     */
    @Override
    public ZonedDateTime extractZonedDateTime()
    {
        if (hasExifData())
        {
            PngDirectory dir = getDirectory(Category.MISC);

            if (dir != null)
            {
                PngChunk chunk = dir.getFirstChunk(ChunkType.eXIf);

                if (chunk != null)
                {
                    TifMetadata exif = TifParser.parseTiffMetadataFromBytes(chunk.getPayloadArray());

                    if (exif != null)
                    {
                        ZonedDateTime exifDate = exif.extractZonedDateTime();

                        if (exifDate != null)
                        {
                            return exifDate;
                        }
                    }
                }
            }
        }

        if (hasXmpData())
        {
            Optional<String> optXmp = xmpDir.getValueByPath(XmpProperty.EXIF_DATE_TIME_ORIGINAL);

            if (optXmp.isPresent())
            {
                ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(optXmp.get());

                if (zdt != null)
                {
                    return zdt;
                }
            }

            optXmp = xmpDir.getValueByPath(XmpProperty.XMP_CREATEDATE);

            if (optXmp.isPresent())
            {
                ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(optXmp.get());

                if (zdt != null)
                {
                    return zdt;
                }
            }
        }

        if (hasTextualData())
        {
            PngDirectory dir = getDirectory(Category.TEXTUAL);

            if (dir != null)
            {
                for (PngChunk chunk : dir)
                {
                    if (chunk instanceof TextualChunk)
                    {
                        TextualChunk textualChunk = (TextualChunk) chunk;

                        if (textualChunk.hasKeyword(TextKeyword.CREATION_TIME) || textualChunk.hasKeyword(TextKeyword.CREATE_DATE))
                        {
                            String text = textualChunk.getText();

                            if (text != null && !text.isEmpty())
                            {
                                ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(text);

                                if (zdt != null)
                                {
                                    return zdt;
                                }
                            }
                        }
                    }
                }
            }
        }

        PngDirectory timeDir = getDirectory(Category.TIME);

        if (timeDir != null)
        {
            PngChunk chunk = timeDir.getFirstChunk(ChunkType.tIME);

            if (chunk instanceof PngChunkTIME)
            {
                return ((PngChunkTIME) chunk).getModificationTime();
            }
        }

        return null;
    }

    /**
     * Retrieves a {@link PngDirectory} associated with the specified chunk category.
     *
     * @param category
     *        the {@link ChunkType.Category} identifier
     * @return the corresponding {@link PngDirectory}, or {@code null} if none exists
     */
    @Override
    public PngDirectory getDirectory(ChunkType.Category category)
    {
        return pngMap.get(category);
    }

    /**
     * Adds a new {@link XmpDirectory} directory to this metadata container.
     *
     * @param dir
     *        the {@link XmpDirectory} to be added
     *
     * @throws NullPointerException
     *         if the specified directory is null
     */
    @Override
    public void addXmpDirectory(XmpDirectory dir)
    {
        if (dir == null)
        {
            throw new NullPointerException("XMP directory cannot be null");
        }

        xmpDir = dir;
    }

    /**
     * Returns the embedded {@link XmpDirectory}, if present.
     *
     * @return an instance of the XmpDirectory if present, otherwise null if none was decoded. To
     *         avoid processing null, checking with the {@link #hasXmpData()} method first is
     *         recommended
     */
    @Override
    public XmpDirectory getXmpDirectory()
    {
        return xmpDir;
    }

    /**
     * Checks if the metadata contains a directory for textual chunks (tEXt, zTXt, iTXt).
     *
     * @return true if textual data directory is present, otherwise false
     */
    @Override
    public boolean hasTextualData()
    {
        return pngMap.containsKey(Category.TEXTUAL);
    }

    /**
     * Checks if the metadata contains an embedded EXIF profile (eXIf chunk).
     *
     * @return true if EXIF metadata is present, otherwise false
     */
    @Override
    public boolean hasExifData()
    {
        PngDirectory directory = pngMap.get(Category.MISC);

        if (directory != null)
        {
            return (directory.getFirstChunk(ChunkType.eXIf) != null);
        }

        return false;
    }

    /**
     * Checks if the metadata contains an XMP directory. Note, XMP data is typically embedded in an
     * iTXt chunk.
     *
     * @return true if XMP metadata is present and non-empty, otherwise false
     */
    @Override
    public boolean hasXmpData()
    {
        return (xmpDir != null && xmpDir.size() > 0);
    }

    /**
     * Returns an iterator over the {@link PngDirectory} values in this metadata collection.
     *
     * @return an {@link Iterator} over the directories
     */
    @Override
    public Iterator<PngDirectory> iterator()
    {
        return pngMap.values().iterator();
    }

    /**
     * Generates a formatted representation of the stored PNG metadata.
     *
     * @return a formatted string containing all metadata details
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Category, PngDirectory> entry : pngMap.entrySet())
        {
            sb.append("Category: ").append(entry.getKey()).append(System.lineSeparator());
            sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());
            sb.append(entry.getValue()).append(System.lineSeparator());
        }

        if (hasXmpData())
        {
            sb.append(xmpDir).append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
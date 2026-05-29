package png;

import static tif.tagspecs.TagIFD_Exif.EXIF_DATE_TIME_ORIGINAL;
import java.nio.ByteOrder;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import png.ChunkType.Category;
import tif.DirectoryIFD;
import tif.DirectoryIdentifier;
import tif.TifMetadata;
import tif.TifParser;
import util.SmartDateParser;
import xmp.XmpDirectory;
import xmp.XmpProperty;

/**
 * Implements the {@link PngMetadataProvider} interface to provide a comprehensive view and
 * extraction capability for metadata embedded within a PNG file. This class aggregates various PNG
 * chunk directories, prioritising embedded metadata standards like EXIF and XMP for accurate data
 * extraction.
 *
 * <p>
 * It organises metadata into directories based on chunk category, for example: TEXTUAL, MISC, etc
 * and pro-actively parses embedded XMP data if an iTXt chunk is detected.
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
     * Retrieves a {@link PngDirectory} associated with the specified chunk category.
     *
     * @param category
     *        the {@link ChunkType.Category} identifier
     * @return the corresponding {@link PngDirectory}, or null if not present.
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
     * Returns an {@link XmpDirectory} only if there exists XMP metadata.
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
     * Extracts the most authoritative creation date available, returning a {@link ZonedDateTime}
     * derived from a prioritized metadata hierarchy across PNG chunks and embedded blocks.
     *
     * <p>
     * This method employs a "waterfall" strategy evaluating tags in the following order:
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
        // Steps 1 & 2: Process embedded EXIF segment payloads if present
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
                        // Delegate straight to the optimized TifMetadata waterfall method you just wrote
                        ZonedDateTime exifDate = exif.extractZonedDateTime();
                        
                        if (exifDate != null)
                        {
                            return exifDate;
                        }
                    }
                }
            }
        }

        // Steps 3 & 4: Process embedded Adobe XMP string payloads
        if (hasXmpData())
        {
            Optional<String> optExif = xmpDir.getValueByPath(XmpProperty.EXIF_DATE_TIME_ORIGINAL);
            
            if (optExif.isPresent())
            {
                ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(optExif.get());
                
                if (zdt != null)
                {
                    return zdt;
                }
            }

            Optional<String> optXmp = xmpDir.getValueByPath(XmpProperty.XMP_CREATEDATE);
            
            if (optXmp.isPresent())
            {
                ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(optXmp.get());
                
                if (zdt != null)
                {
                    return zdt;
                }
            }
        }

        // Step 5: Process ancillary legacy textual metadata chunks
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
                        
                        if (textualChunk.hasKeyword(TextKeyword.CREATION_TIME))
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

        // Step 6: Native structural tIME chunk modification fallback
        PngDirectory timeDir = getDirectory(Category.TIME);
        
        if (timeDir != null)
        {
            PngChunk chunk = timeDir.getFirstChunk(ChunkType.tIME);
            
            if (chunk instanceof PngChunkTIME)
            {
                return ((PngChunkTIME) chunk).getModificationZonedDateTime();
            }
        }

        return null;
    }

    /**
     * Replaced by {@link #extractZonedDateTime()} to leverage thread-safe java.time components.
     */
    @Deprecated
    @Override
    public Date extractDate()
    {
        ZonedDateTime zdt = extractZonedDateTime();
        
        return (zdt != null) ? Date.from(zdt.toInstant()) : null;
    }

    /**
     * <p>
     * Extracts the date from PNG metadata following a priority hierarchy:
     * </p>
     *
     * <ol>
     * <li>Embedded <b>EXIF</b> data (most accurate, from {@code DateTimeOriginal})</li>
     * <li>Embedded <b>XMP</b> data (reliable fallback, from {@code CreateDate} or
     * {@code DateTimeOriginal})</li>
     * <li>Generic <b>Textual</b> data with the 'Creation Time' keyword</li>
     * <li>Native PNG <b>tIME</b> chunk tracking the last modification marker</li>
     * </ol>
     *
     * @return a {@link Date} object extracted from one of the metadata segments, otherwise null if
     *         not found
     */
    public Date extractDate2()
    {
        if (hasExifData())
        {
            PngDirectory dir = getDirectory(Category.MISC);
            PngChunk chunk = dir.getFirstChunk(ChunkType.eXIf);

            TifMetadata exif = TifParser.parseTiffMetadataFromBytes(chunk.getPayloadArray());
            DirectoryIFD ifd = exif.getDirectory(DirectoryIdentifier.IFD_EXIF_SUBIFD_DIRECTORY);

            if (ifd != null && ifd.hasTag(EXIF_DATE_TIME_ORIGINAL))
            {
                return ifd.getDate(EXIF_DATE_TIME_ORIGINAL);
            }
        }

        if (hasXmpData())
        {
            Optional<String> opt = xmpDir.getValueByPath(XmpProperty.EXIF_DATE_TIME_ORIGINAL);

            if (opt.isPresent())
            {
                Date date = SmartDateParser.convertToDate(opt.get());

                if (date != null)
                {
                    return date;
                }
            }

            opt = xmpDir.getValueByPath(XmpProperty.XMP_CREATEDATE);

            if (opt.isPresent())
            {
                Date date = SmartDateParser.convertToDate(opt.get());

                if (date != null)
                {
                    return date;
                }
            }
        }

        if (hasTextualData())
        {
            PngDirectory dir = getDirectory(ChunkType.Category.TEXTUAL);

            for (PngChunk chunk : dir)
            {
                if (chunk instanceof TextualChunk)
                {
                    TextualChunk textualChunk = (TextualChunk) chunk;

                    if (textualChunk.hasKeyword(TextKeyword.CREATION_TIME))
                    {
                        String text = textualChunk.getText();

                        if (!text.isEmpty())
                        {
                            return SmartDateParser.convertToDate(text);
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
                return ((PngChunkTIME) chunk).getModificationDate();
            }
        }

        return null;
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
     * Generates a string representation of the PNG metadata, listing all categories and their
     * associated directory contents.
     *
     * @return a formatted string containing all metadata details
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Category, PngDirectory> entry : pngMap.entrySet())
        {
            sb.append(entry.getKey()).append(System.lineSeparator());
            sb.append(entry.getValue()).append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
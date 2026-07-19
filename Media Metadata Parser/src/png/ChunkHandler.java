package png;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import common.ByteValueConverter;
import common.DigitalSignature;
import common.ImageHandler;
import common.binary.BinaryInput;
import common.binary.RandomAccessReader;
import logger.LogFactory;
import png.ChunkType.Category;

/**
 * Parses a PNG image stream into {@link PngChunk} objects.
 *
 * <p>
 * This handler performs PNG chunk parsing, CRC validation, and instantiates type-specific
 * {@link PngChunk} subclasses. Parsed chunks can be retrieved by {@link ChunkType} or
 * {@link Category} during the parsing lifecycle, and optional filtering allows only selected chunk
 * types to be loaded into memory.
 * </p>
 *
 * <p>
 * Although capable of parsing any PNG chunk defined by the PNG specification, this class is
 * primarily intended for metadata extraction, such as textual, EXIF, and XMP chunks.
 * </p>
 * 
 * <p>
 * <strong>Important Note:</strong> Instantiation only constructs the handler, it does not
 * automatically execute parsing. You must explicitly invoke {@link #parseMetadata()} to load the
 * chunks into memory before querying the repository.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 4 February 2026
 */
public class ChunkHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(ChunkHandler.class);
    private static final byte[] PNG_SIGNATURE_BYTES = DigitalSignature.PNG.getMagicNumberBytes(0);
    private static final long MAX_SAFE_ALLOCATION_LIMIT = 64 * 1024 * 1024;
    public static final ByteOrder PNG_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    private final Path imageFile;
    private final boolean strictMode;
    private final BinaryInput reader;
    private final EnumSet<ChunkType> requiredChunks;
    private final List<PngChunk> chunks = new ArrayList<>();

    /**
     * Creates a handler to select and parse specific chunks based on the specified filter from a
     * PNG file stream.
     *
     * @param fpath
     *        the {@link Path} to the PNG image file
     * @param reader
     *        the byte reader providing access to the raw PNG stream
     * @param requiredChunks
     *        an optional set of chunk types to extract. If {@code null}, all chunk types are
     *        selected
     * @param strict
     *        {@code true} to enforce strict parsing validation, otherwise {@code false} for lenient
     *        operations
     */
    private ChunkHandler(Path fpath, BinaryInput reader, EnumSet<ChunkType> requiredChunks, boolean strict)
    {
        this.imageFile = fpath;
        this.reader = reader;
        this.requiredChunks = requiredChunks;
        this.strictMode = strict;
    }

    /**
     * Creates a handler to select and parse specific chunks based on the specified filter from a
     * PNG file stream.
     *
     * <p>
     * <strong>Please note:</strong> This constructor opens a file handle internally. The caller
     * <strong>must</strong> use the handler within a try-with-resources block or call
     * {@link #close()} to ensure the underlying file lock is released.
     * </p>
     *
     * @param fpath
     *        the {@link Path} to the PNG image file
     * @param requiredChunks
     *        an optional set of chunk types to extract. If {@code null}, filtering is disabled
     * @param strict
     *        {@code true} to enforce strict parsing validation and fail fast, or {@code false} to
     *        skip errors gracefully
     * @throws IOException
     *         if the file cannot be opened for reading
     */
    public ChunkHandler(Path fpath, EnumSet<ChunkType> requiredChunks, boolean strict) throws IOException
    {
        this(fpath, new RandomAccessReader(fpath, PNG_BYTE_ORDER), requiredChunks, strict);
    }

    /**
     * Creates a handler to select and parse specific chunks based on the specified filter from a
     * PNG file stream in lenient mode.
     *
     * @param fpath
     *        the {@link Path} to the PNG image file
     * @param requiredChunks
     *        the set of filtered {@link ChunkType}s to load into memory. If {@code null}, all
     *        chunks are extracted
     * @throws IOException
     *         if the file cannot be opened for reading
     */
    public ChunkHandler(Path fpath, EnumSet<ChunkType> requiredChunks) throws IOException
    {
        this(fpath, requiredChunks, false);
    }

    /**
     * Validates the PNG signature and initiates chunk parsing.
     *
     * <p>
     * Parsing errors are logged and reported by returning {@code false} rather than propagating
     * exceptions to the caller. A file with a valid structure that contains no matching chunks
     * based on the active filter will return {@code true}.
     * </p>
     *
     * @return {@code true} if parsing completed successfully, otherwise {@code false}
     */
    @Override
    public boolean parseMetadata()
    {
        boolean parsed = false;

        try
        {
            long maxSize = reader.length();

            if (maxSize < PNG_SIGNATURE_BYTES.length)
            {
                LOGGER.error("File [" + imageFile + "] is too small to contain a valid PNG signature. Parsing cancelled");
                return false;
            }

            byte[] signature = reader.readBytes(PNG_SIGNATURE_BYTES.length);

            /*
             * Note: PNG_SIGNATURE_BYTES (magic numbers) are mapped to
             * {0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}
             */
            if (signature.length == PNG_SIGNATURE_BYTES.length && Arrays.equals(signature, PNG_SIGNATURE_BYTES))
            {
                parseChunks();
                parsed = true;
            }

            else
            {
                String msg = (signature != null && signature.length > 0) ? ByteValueConverter.toHex(signature) : "EMPTY";
                LOGGER.error("Invalid PNG signature [" + msg + "] detected in file [" + imageFile + "]");
            }

            if (parsed && chunks.isEmpty())
            {
                LOGGER.info("No matching chunks extracted from PNG file [" + imageFile + "]");
            }
        }

        catch (IOException | IllegalStateException exc)
        {
            LOGGER.error(exc.getMessage(), exc);
        }

        return parsed;
    }

    /**
     * Checks whether at least one chunk of the specified type has been extracted.
     *
     * @param type
     *        the chunk type to search for
     * @return {@code true} if a matching chunk exists
     */
    public boolean existsChunkType(ChunkType type)
    {
        for (PngChunk chunk : chunks)
        {
            if (chunk.getType() == type)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves a list of chunks that have been extracted.
     *
     * @return an {@link Optional} containing an unmodifiable list of extracted chunks, or
     *         {@link Optional#empty()}
     */
    public Optional<List<PngChunk>> getChunks()
    {
        return (chunks.isEmpty() ? Optional.empty() : Optional.of(Collections.unmodifiableList(chunks)));
    }

    /**
     * Retrieves all extracted chunks matching a specific metadata category.
     *
     * @param cat
     *        the category to retrieve
     * @return an {@link Optional} containing an unmodifiable list of matching {@link PngChunk}
     *         objects
     */
    public Optional<List<PngChunk>> getChunks(Category cat)
    {
        if (cat == null || cat == Category.UNDEFINED)
        {
            LOGGER.warn("Category [" + cat + "] is undefined");
            return Optional.empty();
        }

        List<PngChunk> chunkList = new ArrayList<>();

        for (PngChunk chunk : chunks)
        {
            if (chunk.getType().getCategory() == cat)
            {
                chunkList.add(chunk);
            }
        }

        return chunkList.isEmpty() ? Optional.empty() : Optional.of(Collections.unmodifiableList(chunkList));
    }

    /**
     * Retrieves a list of specific chunks based on the specified type.
     *
     * @param type
     *        the type of the chunk
     * @return an {@link Optional} containing a list of {@link PngChunk} objects if found
     */
    public Optional<List<PngChunk>> getChunks(ChunkType type)
    {
        if (type == null || type == ChunkType.UNKNOWN)
        {
            LOGGER.warn("Chunk Type [" + type + "] is undefined");
            return Optional.empty();
        }

        List<PngChunk> chunkList = new ArrayList<>();

        for (PngChunk chunk : chunks)
        {
            if (chunk.getType() == type)
            {
                chunkList.add(chunk);
            }
        }

        return chunkList.isEmpty() ? Optional.empty() : Optional.of(Collections.unmodifiableList(chunkList));
    }

    /**
     * Returns the first extracted chunk of the specified type.
     *
     * @param type
     *        the type of the chunk
     * @return an {@link Optional} containing the discovered {@link PngChunk} object
     */
    public Optional<PngChunk> getFirstChunk(ChunkType type)
    {
        if (type == null || type == ChunkType.UNKNOWN)
        {
            LOGGER.warn("Chunk Type [" + type + "] is undefined");
            return Optional.empty();
        }

        for (PngChunk chunk : chunks)
        {
            if (chunk.getType() == type)
            {
                return Optional.of(chunk);
            }
        }

        return Optional.empty();
    }

    /**
     * Returns the last extracted chunk of the specified type.
     *
     * @param type
     *        the type of the chunk
     * @return an {@link Optional} containing the discovered {@link PngChunk} object
     */
    public Optional<PngChunk> getLastChunk(ChunkType type)
    {
        if (type == null || type == ChunkType.UNKNOWN)
        {
            LOGGER.warn("Chunk Type [" + type + "] is undefined");
            return Optional.empty();
        }

        for (int i = chunks.size() - 1; i >= 0; i--)
        {
            PngChunk chunk = chunks.get(i);

            if (chunk.getType() == type)
            {
                return Optional.of(chunk);
            }
        }

        return Optional.empty();
    }

    /**
     * Processes the PNG chunk stream sequentially.
     *
     * <p>
     * <b>Structural Compliance Requirements:</b>
     * </p>
     *
     * <ul>
     * <li>The first chunk must be an <b>IHDR</b> (Image Header) segment</li>
     * <li>The final chunk must be an <b>IEND</b> (Image End) trailer</li>
     * <li>Duplicate chunks are rejected if {@link ChunkType#isMultipleAllowed()} evaluates to
     * {@code false}</li>
     * </ul>
     *
     * @throws IOException
     *         if there is an I/O error detected in the underlying stream
     * @throws IllegalStateException
     *         if any structural validation rule is violated
     */
    private void parseChunks() throws IOException
    {
        int position = 0;
        byte[] typeBytes;
        ChunkType chunkType = null;
        long maxSize = reader.length();

        while (reader.getCurrentPosition() < maxSize && chunkType != ChunkType.IEND)
        {
            long offsetStart = reader.getCurrentPosition();

            /*
             * 12 bytes is the minimum structural size required for any PNG chunk:
             * Length (4 bytes) + Type (4 bytes) + CRC (4 bytes).
             */
            if (reader.getCurrentPosition() + 12 > maxSize)
            {
                throw new IllegalStateException("Unexpected end of PNG file before IEND chunk detected");
            }

            // Read LENGTH (4 bytes)
            long length = reader.readUnsignedInteger();

            // 64MB max payload threshold for safety
            if (length > MAX_SAFE_ALLOCATION_LIMIT)
            {
                throw new IllegalStateException("PNG stream layout too large to allocate. Actual length [" + length + "] detected");
            }

            // Read TYPE (4 bytes) and resolve its structural type
            typeBytes = reader.readBytes(4);
            chunkType = ChunkType.fromBytes(typeBytes);

            if (chunkType != ChunkType.UNKNOWN)
            {
                if (position == 0 && chunkType != ChunkType.IHDR)
                {
                    throw new IllegalStateException("First chunk in file [" + imageFile + "] must be [" + ChunkType.IHDR + "], but found [" + chunkType + "]");
                }

                if (!chunkType.isMultipleAllowed() && existsChunkType(chunkType))
                {
                    String msg = "Duplicate [" + chunkType + "] found in file [" + imageFile + "] is disallowed";

                    if (strictMode)
                    {
                        throw new IllegalStateException(msg);
                    }

                    else
                    {
                        reader.skip(length + 4);
                        LOGGER.warn(msg);
                        position++;
                        continue;
                    }
                }

                byte[] chunkData = null;
                boolean isRequired = (requiredChunks == null || requiredChunks.contains(chunkType) || strictMode);

                if (isRequired)
                {
                    chunkData = reader.readBytes((int) length);
                }

                else
                {
                    reader.skip(length);
                }

                // Read CRC (4 bytes) - always the next 4 bytes after the data payload
                int crc32 = (int) reader.readUnsignedInteger();

                if (chunkData != null)
                {
                    PngChunk newChunk = addChunk(chunkType, length, crc32, chunkData, offsetStart);
                    int expectedCrc = newChunk.calculateCrc();

                    if (expectedCrc != crc32)
                    {
                        String msg = String.format("CRC mismatch for chunk [%s] in file [%s]. Calculated: 0x%08X, Expected: 0x%08X. File may be corrupt", chunkType, imageFile, expectedCrc, crc32);

                        if (strictMode)
                        {
                            throw new IllegalStateException(msg);
                        }

                        else
                        {
                            LOGGER.warn(msg);
                        }
                    }

                    LOGGER.debug("Chunk type [" + chunkType + "] added for file [" + imageFile + "]");
                }
            }

            else
            {
                /*
                 * The extra 4 bytes allow for the length of the CRC value that was not yet read.
                 * Therefore skipping the included length is necessary.
                 */
                if (reader.getCurrentPosition() + length + 4 > maxSize)
                {
                    throw new IllegalStateException("Malformed unknown chunk layout [" + length + "] detected");
                }

                chunkType = null;
                reader.skip(length + 4);
                LOGGER.warn("Unknown chunk type [" + new String(typeBytes, StandardCharsets.US_ASCII) + "] skipped");
            }

            position++;
        }

        if (chunkType != ChunkType.IEND)
        {
            throw new IllegalStateException("Invalid PNG file layout [" + imageFile + "]. Missing trailing [" + ChunkType.IEND + "] chunk");
        }
    }

    /**
     * Creates and stores a PNG chunk for the specified type.
     *
     * @param chunkType
     *        the identified critical or ancillary type of the PNG chunk
     * @param length
     *        the length of the data portion of the chunk
     * @param crc32
     *        the CRC value
     * @param data
     *        the raw byte array containing the chunk's payload
     * @param offsetStart
     *        the absolute physical file position where the chunk begins
     * @return the created type-specific {@link PngChunk}
     */
    private PngChunk addChunk(ChunkType chunkType, long length, int crc32, byte[] data, long offsetStart)
    {
        PngChunk newChunk;
        byte[] typeBytes = chunkType.getFourCCBytes();

        // Simple Factory pattern
        switch (chunkType)
        {
            case IHDR:
                newChunk = new PngChunkIHDR(length, typeBytes, crc32, data, offsetStart);
            break;

            case sRGB:
                newChunk = new PngChunkSRGB(length, typeBytes, crc32, data, offsetStart);
            break;

            case gAMA:
                newChunk = new PngChunkGAMA(length, typeBytes, crc32, data, offsetStart);
            break;

            case pHYs:
                newChunk = new PngChunkPHYS(length, typeBytes, crc32, data, offsetStart);
            break;

            case tEXt:
                newChunk = new PngChunkTEXT(length, typeBytes, crc32, data, offsetStart);
            break;

            case iTXt:
                newChunk = new PngChunkITXT(length, typeBytes, crc32, data, offsetStart);
            break;

            case zTXt:
                newChunk = new PngChunkZTXT(length, typeBytes, crc32, data, offsetStart);
            break;

            case tIME:
                newChunk = new PngChunkTIME(length, typeBytes, crc32, data, offsetStart);
            break;

            default:
                newChunk = new PngChunk(length, typeBytes, crc32, data, offsetStart);
            break;
        }

        this.chunks.add(newChunk);

        return newChunk;
    }

    /**
     * Releases the file handle and closes the underlying {@code BinaryInput} resource.
     *
     * <p>
     * This is called automatically when using a {@code try-with-resources} block. Closing this
     * handler ensures that any system locks on the file are released and memory resources are
     * freed.
     * </p>
     *
     * @throws IOException
     *         if an I/O error occurs while closing the reader
     */
    @Override
    public void close() throws IOException
    {
        if (reader != null)
        {
            reader.close();
        }
    }

    /**
     * Returns a textual representation of all parsed PNG chunks in this file.
     *
     * @return formatted string of all parsed chunk entries
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (PngChunk chunk : chunks)
        {
            sb.append(chunk).append(System.lineSeparator());
        }

        return sb.toString();
    }
}
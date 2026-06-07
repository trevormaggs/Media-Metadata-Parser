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
 * A handler designed to perform a heavy-lifting process to extract information from
 * {@link PngChunk} elements from a PNG stream.
 *
 * <p>
 * This class provides a structured repository for chunks, facilitating efficient retrieval by
 * {@link ChunkType} or {@link Category}. It serves as the primary data store during the parsing
 * lifecycle, managing subclass instantiation for extended chunks, such as {@code iTXt},
 * {@code eXIf} or {@code tIME}, and performing integrity validation via CRC checks.
 * </p>
 *
 * <p>
 * This handler can manage any PNG chunk type defined in the PNG specification, though it is most
 * commonly used for metadata extraction (XMP, EXIF, Textual). It supports filtered extraction using
 * the {@link #requiredChunks} set to conserve memory.
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
     * Constructs a handler to parse filtered chunks from a PNG image file stream.
     *
     * @param fpath
     *        the path to the PNG file
     * @param reader
     *        the byte reader providing access to the raw PNG stream
     * @param requiredChunks
     *        an optional set of chunk types to be extracted. If {@code null}, all chunks are
     *        selected
     * @param strict
     *        {@code true} to enforce strict parsing validation, otherwise {@code false} for lenient
     *        operations without interrupting the parsing process
     */
    private ChunkHandler(Path fpath, BinaryInput reader, EnumSet<ChunkType> requiredChunks, boolean strict)
    {
        this.imageFile = fpath;
        this.reader = reader;
        this.requiredChunks = requiredChunks;
        this.strictMode = strict;
    }

    /**
     * Instantiates a handler to parse the specified set of filtered chunks from a PNG file stream.
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
     *        the set of filtered {@link ChunkType}s to load into memory. If {@code null}, all
     *        encountered chunks are extracted
     * @param strict
     *        {@code true} to enforce strict parsing validation, otherwise {@code false} for lenient
     *        operations without interrupting the parsing process
     *
     * @throws IOException
     *         if the file cannot be opened for reading
     */
    public ChunkHandler(Path fpath, EnumSet<ChunkType> requiredChunks, boolean strict) throws IOException
    {
        this(fpath, new RandomAccessReader(fpath, PNG_BYTE_ORDER), requiredChunks, strict);
    }

    /**
     * Instantiates a handler to parse the specified set of filtered chunks from a PNG file stream
     * in lenient mode.
     *
     * @param fpath
     *        the {@link Path} to the PNG image file
     * @param requiredChunks
     *        the set of filtered {@link ChunkType}s to load into memory. If {@code null}, all
     *        encountered chunks are extracted
     *
     * @throws IOException
     *         if the file cannot be opened for reading
     */
    public ChunkHandler(Path fpath, EnumSet<ChunkType> requiredChunks) throws IOException
    {
        this(fpath, requiredChunks, false);
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
     * Validates the PNG stream signature and initiates sequential chunk parsing. This method
     * implements a "soft landing" exception strategy, ensuring that any parsing disruptions do not
     * lead to crashing the entire file parser unnecessarily. In the event of disruptions, they will
     * be logged.
     *
     * @return {@code true} if the PNG stream signature was verified and chunk parsing operations
     *         were completed successfully, otherwise {@code false} on any errors, which will be
     *         logged
     */
    @Override
    public boolean parseMetadata()
    {
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
                return true;
            }

            else
            {
                String msg = (signature != null && signature.length > 0) ? ByteValueConverter.toHex(signature) : "EMPTY";
                LOGGER.error("Invalid PNG signature [" + msg + "] detected in file [" + imageFile + "]");
            }
        }

        catch (IOException | IllegalStateException exc)
        {
            LOGGER.error(exc.getMessage(), exc);
        }

        chunks.clear();

        return false;
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

    /**
     * Checks if a chunk with the specified type has already been set.
     *
     * @param type
     *        the type of the chunk
     * @return {@code true} if the chunk is present
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
     * Retrieves all extracted chunks matching a specific metadata category.
     *
     * @param cat
     *        the specific target category to filter by
     * @return an {@link Optional} containing an unmodifiable list of matching {@link PngChunk}
     *         objects, or {@link Optional#empty()} if no matching chunks were extracted
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
     * Retrieves a list of specific chunks based on the specified type, for example: {@code tEXt},
     * {@code iTXt}, or {@code eXIf} etc.
     *
     * @param type
     *        the type of the chunk
     * @return an {@link Optional} containing a list of {@link PngChunk} objects if found, or
     *         {@link Optional#empty()} if the specified chunk type cannot be found
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
     * Retrieves the first occurrence of the chunk matching the specified type. Any subsequent
     * chunks will be skipped.
     *
     * @param type
     *        the type of the chunk
     * @return an {@link Optional} containing the discovered {@link PngChunk} object, otherwise
     *         {@link Optional#empty()} if not found
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
     * Retrieves the last occurrence of the chunk matching the specified type. Any previous chunks
     * will not be processed.
     *
     * @param type
     *        the type of the chunk
     * @return an {@link Optional} containing the discovered {@link PngChunk} object, otherwise
     *         {@link Optional#empty()} if not found
     *
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
     * Searches for a specific iTXt chunk that contains embedded XMP data payloads.
     *
     * @return an {@link Optional} containing the discovered XMP iTXt chunk with embedded XMP data,
     *         otherwise {@link Optional#empty()}
     */
    public Optional<PngChunkITXT> getXmpChunk()
    {
        for (int i = chunks.size() - 1; i >= 0; i--)
        {
            PngChunk chunk = chunks.get(i);

            if (chunk.getType() == ChunkType.iTXt && chunk instanceof PngChunkITXT)
            {
                PngChunkITXT itxtChunk = (PngChunkITXT) chunk;

                if (itxtChunk.hasKeyword(TextKeyword.XMP))
                {
                    return Optional.of(itxtChunk);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Processes the PNG data stream sequentially.
     *
     * <p>
     * <strong>Structural Compliance Requirements:</strong>
     * </p>
     *
     * <ul>
     * <li>The first chunk must be an <b>IHDR</b> (Image Header) segment</li>
     * <li>The final chunk must be an <b>IEND</b> (Image End) trailer</li>
     * <li>Duplicate chunks are rejected if {@link ChunkType#isMultipleAllowed()} evaluates to
     * {@code false}</li>
     * </ul>
     *
     * @throws IllegalStateException
     *         if any structural violation has occurred
     * @throws IOException
     *         if there is an I/O error detected in the underlying stream
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
                boolean isRequired = requiredChunks == null || requiredChunks.contains(chunkType);

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

                reader.skip(length + 4);

                LOGGER.warn("Unknown chunk type [" + new String(typeBytes, StandardCharsets.US_ASCII) + "] skipped");
            }

            position++;
        }

        if (chunks.isEmpty())
        {
            LOGGER.info("No chunks extracted from PNG file [" + imageFile + "]");
        }
    }

    /**
     * Attaches a general or type-specific PNG chunk to the collection.
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
     * @return a fully populated, type-specific {@link PngChunk} subclass instance, or a base
     *         container fallback if the type is unrecognised
     */
    private PngChunk addChunk(ChunkType chunkType, long length, int crc32, byte[] data, long offsetStart)
    {
        PngChunk newChunk;
        byte[] typeBytes = chunkType.getFourCCBytes();

        // Simple Factory pattern
        switch (chunkType)
        {
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
     * Checks if a chunk is classified in the specified category, for example: Textual, Header or
     * Time etc.
     *
     * @param cat
     *        the type of ChunkType.Category enumeration
     * @return {@code true} if the chunk is present
     */
    @Deprecated
    public boolean existsChunkCategory(Category cat)
    {
        for (PngChunk chunk : chunks)
        {
            if (chunk.getType().getCategory() == cat)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves a list of chunks that have been extracted.
     *
     * @return an {@link Optional} containing an unmodifiable list of extracted {@link PngChunk}
     *         objects, or {@link Optional#empty()} if no chunks matched the extraction criteria
     * @deprecated As of version 1.3, internal collections should not be fetched wholesale without
     *             explicit type boundaries.
     */
    @Deprecated
    public Optional<List<PngChunk>> getChunks()
    {
        return chunks.isEmpty() ? Optional.empty() : Optional.of(Collections.unmodifiableList(chunks));
    }
}
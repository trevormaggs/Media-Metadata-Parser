package png;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import common.ByteStreamReader;
import common.ByteValueConverter;
import common.DigitalSignature;
import common.ImageHandler;
import common.ImageRandomAccessReader;
import logger.LogFactory;
import png.ChunkType.Category;

/**
 * A specialised container used for handling {@link PngChunk} elements extracted from a PNG stream.
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
 * commonly used for metadata extraction (XMP, EXIF, Textual). It supports filtered extraction via
 * the {@code requiredChunks} set to optimise memory.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 4 February 2026
 */
public class ChunkHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(ChunkHandler.class);
    private static final byte[] PNG_SIGNATURE_BYTES = DigitalSignature.PNG.getMagicNumbers(0);
    public static final ByteOrder PNG_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    private final Path imageFile;
    private final boolean strictMode;
    private final ByteStreamReader reader;
    private final EnumSet<ChunkType> requiredChunks;
    private final List<PngChunk> chunks = new ArrayList<>();

    /**
     * Constructs a handler to parse selected chunks from a PNG image file stream.
     *
     * @param fpath
     *        the path to the PNG file for logging purposes
     * @param reader
     *        the byte reader providing access to the raw PNG stream
     * @param requiredChunks
     *        an optional set of chunk types to be extracted; if {@code null}, all chunks are
     *        selected
     * @param strict
     *        {@code true} to enforce strict parsing validation, or {@code false} to log structural
     *        anomalies or CRC mismatches as warnings without interrupting the parsing process
     */
    public ChunkHandler(Path fpath, ByteStreamReader reader, EnumSet<ChunkType> requiredChunks, boolean strict)
    {
        this.imageFile = fpath;
        this.reader = reader;
        this.requiredChunks = requiredChunks;
        this.strictMode = strict;
    }

    /**
     * Constructs a {@code ChunkHandler} using a default {@link ImageRandomAccessReader} in lenient
     * mode.
     * 
     * <p>
     * <strong>Resource Management:</strong> This constructor opens a file handle internally. The
     * caller <b>must</b> use this handler within a try-with-resources block or call
     * {@link #close()} to ensure the underlying file lock is released.
     * </p>
     * 
     * @param fpath
     *        the {@link Path} to the PNG image file
     * @param requiredChunks
     *        the set of {@link ChunkType}s to load into memory. If {@code null}, all encountered
     *        chunks are extracted
     * 
     * @throws IOException
     *         if the file cannot be opened or the {@link ImageRandomAccessReader} fails to
     *         initialise
     */
    public ChunkHandler(Path fpath, EnumSet<ChunkType> requiredChunks) throws IOException
    {
        this(fpath, new ImageRandomAccessReader(fpath, PNG_BYTE_ORDER), requiredChunks, false);
    }

    /**
     * Releases the file handle and closes the underlying {@code ByteStreamReader} resource.
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
     * Validates the PNG stream signature and initiates sequential chunk parsing.
     *
     * @return {@code true} if the PNG stream layout was successfully parsed up to its terminal
     *         marker, or {@code false} if a malformed structure was encountered in lenient mode
     * 
     * @throws IOException
     *         if the stream signature is invalid, missing, or an I/O error occurs
     * @throws IllegalStateException
     *         if a structural integrity violation is detected while parsing in strict mode
     */
    @Override
    public boolean parseMetadata() throws IOException
    {
        byte[] signature = reader.readBytes(PNG_SIGNATURE_BYTES.length);

        /*
         * Note: PNG_SIGNATURE_BYTES (magic numbers) are mapped to
         * {0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}
         */
        if (signature.length == PNG_SIGNATURE_BYTES.length && Arrays.equals(signature, PNG_SIGNATURE_BYTES))
        {
            try
            {
                parseChunks();
                return true;
            }

            catch (IllegalStateException exc)
            {
                chunks.clear();
                LOGGER.error(exc.getMessage());
            }
        }

        else
        {
            throw new IOException("Invalid PNG signature [" + ByteValueConverter.toHex(signature) + "] detected in file [" + imageFile + "]");
        }

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
     * @return true if the chunk is present
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
     * @return an {@link Optional} containing the discovered {@link PngChunk} object if found, or
     *         {@link Optional#empty()} if the specified chunk type cannot be found
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
     * Retrieves the last instance of the chunk matching the specified type. Any previous chunks
     * will be overwritten.
     *
     * @param type
     *        the type of the chunk
     * @return an {@link Optional} containing the last {@link PngChunk} object if found, or
     *         {@link Optional#empty()} if the specified chunk type cannot be found
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
     * Searches for a specific iTXt chunk that contains embedded XMP data payloads and returns it.
     *
     * @return an {@link Optional} containing the discovered XMP iTXt chunk, or
     *         {@link Optional#empty()}
     */
    public Optional<PngChunkITXT> getXmpChunk()
    {
        for (int i = chunks.size() - 1; i >= 0; i--)
        {
            PngChunk chunk = chunks.get(i);

            if (chunk.getType() == ChunkType.iTXt)
            {
                if (chunk instanceof PngChunkITXT)
                {
                    PngChunkITXT itxtChunk = (PngChunkITXT) chunk;

                    if (itxtChunk.hasKeyword(TextKeyword.XMP))
                    {
                        return Optional.of(itxtChunk);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Processes the PNG data stream sequentially.
     * 
     * <p>
     * <strong>Strict Requirements:</strong>
     * </p>
     * 
     * <ul>
     * <li>The first chunk must be <b>IHDR</b> (Image Header).</li>
     * <li>The last chunk must be <b>IEND</b> (Image Trailer).</li>
     * <li>Duplicate chunks are rejected if {@link ChunkType#isMultipleAllowed()} is false.</li>
     * </ul>
     * 
     * @throws IllegalStateException
     *         if the PNG structure violates the IHDR/IEND sequence or if a CRC mismatch is detected
     *         in {@code strictMode}
     * @throws IOException
     *         if there is an I/O stream error
     */
    private void parseChunks() throws IOException
    {
        int position = 0;
        byte[] typeBytes;
        ChunkType chunkType;
        boolean foundIEND = false;
        long fileSize = Files.size(imageFile);

        while (!foundIEND)
        {
            long offsetStart = reader.getCurrentPosition();

            /*
             * 12 bytes = minimum chunk size: (Length (4) + Type (4) + CRC (4)
             */
            if (fileSize == 0 || reader.getCurrentPosition() + 12 > fileSize)
            {
                throw new IllegalStateException("Unexpected end of PNG file before IEND chunk detected");
            }

            // Read LENGTH (4 bytes)
            long length = reader.readUnsignedInteger();

            if (length > Integer.MAX_VALUE)
            {
                throw new IllegalStateException("Out of bounds chunk length [" + length + "] detected");
            }

            // Read TYPE (4 bytes)
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
                    throw new IllegalStateException("Duplicate [" + chunkType + "] found in file [" + imageFile + "]. This is disallowed");
                }

                if (chunkType == ChunkType.IEND)
                {
                    foundIEND = true;
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

                // Read CRC (4 bytes) - always the next 4 bytes after the data
                int crc32 = (int) reader.readUnsignedInteger();

                // Only proceed with chunk creation and CRC validation if the data was read
                if (chunkData != null)
                {
                    PngChunk newChunk = addChunk(chunkType, length, typeBytes, crc32, chunkData, offsetStart);
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
                 * Handle UNKNOWN chunk type by skipping the full length
                 * of data plus 4 bytes for the CRC.
                 */
                reader.skip(length + 4);

                LOGGER.warn("Unknown chunk type [" + new String(typeBytes, StandardCharsets.US_ASCII) + "] skipped");
                LOGGER.debug("Data skipped by length [" + (length + 4) + "] in file [" + imageFile + "] due to an unknown chunk");
            }

            position++;
        }

        if (chunks.isEmpty())
        {
            LOGGER.info("No chunks extracted from PNG file [" + imageFile + "]");
        }
    }

    /**
     * Instantiates the appropriate {@link PngChunk} instance and registers it within the internal
     * collection.
     *
     * @param chunkType
     *        the identified type of the PNG chunk
     * @param length
     *        the length of the data portion of the chunk
     * @param typeBytes
     *        the raw 4-byte chunk type identifier
     * @param crc32
     *        the CRC value read from the file stream
     * @param data
     *        the raw byte array of the chunk's data payload
     * @param offsetStart
     *        the absolute physical file position where the chunk begins
     * @return a populated chunk instance matching the requested type
     */
    private PngChunk addChunk(ChunkType chunkType, long length, byte[] typeBytes, int crc32, byte[] data, long offsetStart)
    {
        PngChunk newChunk;

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
        }

        chunks.add(newChunk);

        return newChunk;
    }

    /**
     * Checks if a chunk is classified in the specified category, for example: Textual, Header or
     * Time etc.
     *
     * @param cat
     *        the type of ChunkType.Category enumeration
     * @return true if the chunk is present
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
     */
    @Deprecated
    public Optional<List<PngChunk>> getChunks()
    {
        return chunks.isEmpty() ? Optional.empty() : Optional.of(Collections.unmodifiableList(chunks));
    }
}
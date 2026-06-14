package webp;

import static webp.WebPChunkType.*;
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
import java.util.Set;
import common.ImageHandler;
import common.binary.AbstractRandomAccessStream;
import common.binary.BinaryInput;
import common.binary.ByteArrayReader;
import common.binary.RandomAccessReader;
import logger.LogFactory;

/**
 * Handles the sequential parsing of WebP RIFF containers.
 * 
 * <p>
 * This handler manages the top-level RIFF header, the WEBP signature, and subsequent data chunks
 * such as VP8, EXIF, and XMP. It ensures data integrity by validating signatures and enforcing RIFF
 * padding rules.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 26 May 2026
 */
public class RiffHandler implements ImageHandler
{
    /*
     * Note, CHUNK_HEADER_SIZE represents the size of a RIFF chunk header
     * (4 bytes for FourCC + 4 bytes for payload length, counting the WEBP identifier).
     */
    private static final LogFactory LOGGER = LogFactory.getLogger(RiffHandler.class);
    private static final EnumSet<WebPChunkType> FIRST_CHUNK_TYPES = EnumSet.of(VP8, VP8L, VP8X);
    public static final ByteOrder WEBP_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final int BITSTREAM_HEADER_BUDGET = 10;
    private static final int CHUNK_HEADER_SIZE = 8;
    private final BinaryInput reader;
    private final List<WebpChunk> chunks = new ArrayList<>();
    private final Set<WebPChunkType> requiredChunks;
    private int extendedFormat;

    /**
     * Constructs a handler to parse selected chunks from a WebP image file stream.
     *
     * @param reader
     *        the {@link BinaryInput} associated with the target WebP stream
     * @param requiredChunks
     *        an optional set of chunk types to extract. If {@code null}, all encountered chunks
     *        will be processed
     */
    private RiffHandler(BinaryInput reader, EnumSet<WebPChunkType> requiredChunks)
    {
        this.reader = reader;

        if (requiredChunks == null)
        {
            this.requiredChunks = null;
        }

        else
        {
            EnumSet<WebPChunkType> chunkset = requiredChunks.clone();

            // Ensure core dimension chunks are always processed
            chunkset.addAll(FIRST_CHUNK_TYPES);
            this.requiredChunks = chunkset;
        }
    }

    /**
     * Constructs a handler for the specified WebP file path.
     * 
     * <p>
     * <strong>Note:</strong> Since this constructor opens the file, please use a try-with-resources
     * block or call {@link #close()} to release the file lock.
     * </p>
     *
     * @param fpath
     *        the filesystem path to the WebP file
     * @param requiredChunks
     *        optional set of chunk types to extract. If {@code null}, all encountered chunks are
     *        processed
     * 
     * @throws IOException
     *         if the file is inaccessible or cannot be opened
     */
    public RiffHandler(Path fpath, EnumSet<WebPChunkType> requiredChunks) throws IOException
    {
        this(new RandomAccessReader(fpath, WEBP_BYTE_ORDER), requiredChunks);
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
     * Parses the WebP file container and extracts filtered chunks into local memory arrays.
     * 
     * <p>
     * This method implements a localised soft-landing exception strategy, ensuring that structural
     * disruptions (such as missing headers or truncated streams) do not throw exceptions.
     * Interrupted states are logged as errors, memory objects are reset, and a status boolean is
     * returned.
     * </p>
     *
     * @return {@code true} if all encountered RIFF chunks were successfully extracted or safely
     *         bypassed, {@code false} if a fatal container validation error occurred
     */
    @Override
    public boolean parseMetadata()
    {
        try
        {
            long filesize = getRealFileSize();
            long totalChunkSize = readFileHeader(reader);

            if (totalChunkSize <= 0)
            {
                LOGGER.error("WebP header has invalid size. Found [" + totalChunkSize + "]. Parsing cancelled");
                return false;
            }

            if (filesize > 0 && filesize < totalChunkSize)
            {
                LOGGER.error("WebP header size exceeds physical file length. Parsing cancelled");
                return false;
            }

            parseChunks(reader, totalChunkSize);
            return true;
        }

        catch (IOException | IllegalStateException exc)
        {
            LOGGER.error(exc.getMessage(), exc);
        }

        chunks.clear();

        return false;
    }

    /**
     * Returns a string summary of all parsed chunks.
     *
     * @return a newline-delimited string of chunk descriptions
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (WebpChunk chunk : chunks)
        {
            sb.append(chunk).append(System.lineSeparator());
        }

        return sb.toString();
    }

    /**
     * Returns the length of the physical image file.
     *
     * @return the length in bytes, or 0L if the size cannot be determined
     */
    public long getRealFileSize()
    {
        return ((AbstractRandomAccessStream) reader).getPath().toFile().length();
    }

    /**
     * Checks for the existence of a specific chunk type in the parsed list.
     *
     * @param type
     *        the type to check
     * @return {@code true} if present
     */
    public boolean existsChunk(WebPChunkType type)
    {
        for (WebpChunk chunk : chunks)
        {
            if (chunk.getType() == type)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves a list of active chunks.
     * 
     * @return an unmodifiable view of all parsed {@link WebpChunk} objects
     */
    public List<WebpChunk> getChunks()
    {
        return Collections.unmodifiableList(chunks);
    }

    /**
     * Finds the first occurrence of a chunk by type.
     *
     * @param type
     *        the {@link WebPChunkType} to find
     * @return an {@link Optional} containing the first matching {@link WebpChunk}, or empty if not
     *         found
     */
    public Optional<WebpChunk> getFirstChunk(WebPChunkType type)
    {
        for (WebpChunk chunk : chunks)
        {
            if (chunk.getType() == type)
            {
                return Optional.of(chunk);
            }
        }

        return Optional.empty();
    }

    /**
     * Finds the last occurrence of a chunk by type.
     * 
     * <p>
     * This is useful for metadata chunks, such as {@code 'XMP '} where, in some implementation
     * scenarios, a secondary chunk might be appended to override or update previous data.
     * </p>
     * 
     * @param type
     *        the {@link WebPChunkType} to find
     * @return an {@link Optional} containing the last matching {@link WebpChunk}, or empty if not
     *         found
     */
    public Optional<WebpChunk> getLastChunk(WebPChunkType type)
    {
        for (int i = chunks.size() - 1; i >= 0; i--)
        {
            WebpChunk chunk = chunks.get(i);

            if (chunk.getType() == type)
            {
                return Optional.of(chunk);
            }
        }

        return Optional.empty();
    }

    /**
     * Reads and validates the RIFF/WEBP header and determines the exact container size.
     *
     * @param reader
     *        the stream reader
     * @return the total file size reported by the RIFF header
     * 
     * @throws IOException
     *         if an I/O reading error occurs
     * @throws IllegalStateException
     *         if signatures are missing or size is mathematically invalid
     */
    private long readFileHeader(BinaryInput reader) throws IOException
    {
        byte[] type = reader.readBytes(4);

        if (!Arrays.equals(RIFF.getChunkName().getBytes(StandardCharsets.US_ASCII), type))
        {
            throw new IllegalStateException("Header [RIFF] not found. Not a valid WebP file");
        }

        long riffDataSize = reader.readUnsignedInteger();
        long totalChunkSize = riffDataSize + CHUNK_HEADER_SIZE;

        if (totalChunkSize < 12)
        {
            throw new IllegalStateException("Malformed WebP header: container size is too small to be valid");
        }

        type = reader.readBytes(4);

        if (!Arrays.equals(WEBP.getChunkName().getBytes(StandardCharsets.US_ASCII), type))
        {
            throw new IllegalStateException("Signature [WEBP] not found. Not a valid WebP file");
        }

        return totalChunkSize;
    }

    /**
     * Iterates through RIFF chunks sequentially and parses bitstream headers.
     *
     * @param reader
     *        the reader positioned after the WEBP signature
     * @param totalChunkSize
     *        the total expected size of the container in bytes
     * 
     * @throws IOException
     *         if an I/O error occurs during parsing
     * @throws IllegalStateException
     *         if layout bounds are crossed or critical bitstream configurations are malformed
     */
    private void parseChunks(BinaryInput reader, long totalChunkSize) throws IOException
    {
        boolean firstChunk = true;

        chunks.clear();

        while (reader.getCurrentPosition() + CHUNK_HEADER_SIZE <= totalChunkSize)
        {
            int fourCC = reader.readInteger();
            long payloadLength = reader.readUnsignedInteger();
            WebPChunkType chunkType = WebPChunkType.findType(fourCC);

            if (payloadLength < 0 || (reader.getCurrentPosition() + payloadLength > totalChunkSize))
            {
                throw new IllegalStateException("Malformed chunk structural layout bounds detected on [" + WebPChunkType.getChunkName(fourCC) + "]");
            }

            if (firstChunk && !FIRST_CHUNK_TYPES.contains(chunkType))
            {
                throw new IllegalStateException("Invalid first chunk found [" + WebPChunkType.getChunkName(fourCC) + "]. It must be either VP8, VP8L or VP8X");
            }

            if (requiredChunks == null || requiredChunks.contains(chunkType))
            {
                byte[] data;
                long currentDataOffset = reader.getCurrentPosition();

                if (chunkType == VP8 || chunkType == VP8L || chunkType == VP8X)
                {
                    int bytesToRead = (int) Math.min(payloadLength, BITSTREAM_HEADER_BUDGET);
                    data = reader.readBytes(bytesToRead);

                    if (chunkType == WebPChunkType.VP8X)
                    {
                        parseVP8X(data);
                    }

                    else if (chunkType == WebPChunkType.VP8)
                    {
                        parseVP8(data);
                    }

                    else if (chunkType == WebPChunkType.VP8L)
                    {
                        parseVP8L(data);
                    }

                    if (payloadLength > bytesToRead)
                    {
                        reader.skip(payloadLength - bytesToRead);
                    }
                }

                else
                {
                    data = reader.readBytes((int) payloadLength);
                }

                addChunk(chunkType, fourCC, (int) payloadLength, data, currentDataOffset);
            }

            else
            {
                reader.skip(payloadLength);
            }

            if (payloadLength % 2 != 0)
            {
                reader.skip(1);
            }

            firstChunk = false;
        }
    }

    /**
     * Parses the VP8X extended header to determine image features and dimensions.
     *
     * @param payload
     *        the extracted configuration payload bytes containing VP8X features
     * @see <a href=
     *      "https://developers.google.com/speed/webp/docs/riff_container#extended_file_format">Extended
     *      File Format Specification</a>
     */
    private void parseVP8X(byte[] payload)
    {
        if (payload.length >= 10)
        {
            try (ByteArrayReader subReader = new ByteArrayReader(payload, WEBP_BYTE_ORDER))
            {
                extendedFormat = subReader.readUnsignedByte();

                boolean hasAnimation = (extendedFormat & 0x02) != 0;
                boolean hasXMP = (extendedFormat & 0x04) != 0;
                boolean hasEXIF = (extendedFormat & 0x08) != 0;
                boolean hasAlpha = (extendedFormat & 0x10) != 0;
                boolean hasICC = (extendedFormat & 0x20) != 0;

                subReader.skip(3);

                int canvasWidth = subReader.readUnsignedInteger24() + 1;
                int canvasHeight = subReader.readUnsignedInteger24() + 1;

                LOGGER.debug(String.format("Chunk VP8X detected. CanvasWidth x CanvasHeight: %dx%d, EXIF: %b, XMP: %b, Alpha: %b, Anim %b, ICCP: %b",
                        canvasWidth, canvasHeight, hasEXIF, hasXMP, hasAlpha, hasAnimation, hasICC));
            }

            catch (IOException exc)
            {
                // It is expected that it would never throw. This is only to satisfy the compiler.
                throw new IllegalStateException("Unexpected I/O error parsing VP8X payload", exc);
            }
        }

        else
        {
            throw new IllegalStateException("Corrupt bitstream structure. Chunk [VP8X] size is less than the required 10 bytes");
        }
    }

    /**
     * Parses the VP8 Lossy bitstream header to validate sync structures and extract image
     * dimensions.
     *
     * @param payload
     *        the extracted configuration payload bytes from the lossy bitstream
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6386">RFC 6386 - VP8 Data Format
     *      Specification</a>
     */
    private void parseVP8(byte[] payload)
    {
        if (payload.length >= 10)
        {
            try (ByteArrayReader subReader = new ByteArrayReader(payload, WEBP_BYTE_ORDER))
            {
                subReader.skip(3);

                byte[] syncCode = subReader.readBytes(3);

                if (syncCode[0] == (byte) 0x9D && syncCode[1] == (byte) 0x01 && syncCode[2] == (byte) 0x2A)
                {
                    int width = subReader.readUnsignedShort() & 0x3FFF;
                    int height = subReader.readUnsignedShort() & 0x3FFF;

                    LOGGER.debug(String.format("VP8 Lossy Bitstream [%dx%d]", width, height));
                }

                else
                {
                    throw new IllegalStateException("Corrupt bitstream structure. Invalid VP8 lossy sync frame signature code");
                }
            }

            catch (IOException exc)
            {
                // It is expected that it would never throw. This is only to satisfy the compiler.
                throw new IllegalStateException("Unexpected I/O error parsing VP8 payload", exc);
            }
        }

        else
        {
            throw new IllegalStateException("Corrupt bitstream structure. Chunk [VP8] size is less than the required 10 bytes");
        }
    }

    /**
     * Parses the VP8L Loss-less bitstream header to validate signatures and extract image
     * dimensions.
     *
     * @param payload
     *        the extracted configuration payload bytes from the lossless bitstream
     * @see <a href=
     *      "https://developers.google.com/speed/webp/docs/webp_lossless_bitstream_specification">WebP
     *      Lossless Bitstream Specification</a>
     */
    private void parseVP8L(byte[] payload)
    {
        if (payload.length >= 5)
        {
            try (ByteArrayReader subReader = new ByteArrayReader(payload, WEBP_BYTE_ORDER))
            {
                int signature = subReader.readUnsignedByte();

                if (signature == 0x2F)
                {
                    int data = subReader.readInteger();
                    int width = (data & 0x3FFF) + 1;
                    int height = ((data >>> 14) & 0x3FFF) + 1;

                    LOGGER.debug(String.format("VP8L Lossless Bitstream: [%dx%d]", width, height));
                }

                else
                {
                    throw new IllegalStateException("Corrupt bitstream structure. Invalid VP8L lossless signature byte");
                }
            }
        }

        else
        {
            throw new IllegalStateException("Corrupt bitstream structure. Chunk [VP8L] size is less than the required 5 bytes");
        }
    }

    /**
     * Adds a chunk to the list, preventing duplicates for unique chunk types.
     *
     * @param type
     *        the resolved {@link WebPChunkType} value
     * @param fourCC
     *        the 32-bit FourCC identifier
     * @param length
     *        payload length in bytes
     * @param data
     *        the raw payload bytes
     * @param dataOffset
     *        the absolute physical position where the payload starts
     */
    private void addChunk(WebPChunkType type, int fourCC, int length, byte[] data, long dataOffset)
    {
        if (!type.isMultipleAllowed() && existsChunk(type))
        {
            LOGGER.warn("Duplicate chunk detected [" + type + "]");
            return;
        }

        chunks.add(new WebpChunk(fourCC, length, data, dataOffset));
    }

    /**
     * Queries whether the Extended File Format (VP8X) chunk indicates the presence of EXIF
     * metadata.
     * 
     * @return {@code true} if VP8X flags and EXIF chunk exist
     */
    @Deprecated
    public boolean existsExifMetadata()
    {
        return (existsChunk(WebPChunkType.EXIF) && (extendedFormat & 0x08) != 0);
    }

    /**
     * Queries whether the Extended File Format (VP8X) chunk indicates the presence of XMP metadata.
     * 
     * @return {@code true} if VP8X flags and XMP chunk exist
     */
    @Deprecated
    public boolean existsXmpMetadata()
    {
        return (existsChunk(WebPChunkType.XMP) && (extendedFormat & 0x04) != 0);
    }
}
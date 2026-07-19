package png;

import java.nio.ByteBuffer;
import common.ByteValueConverter;
import common.MetadataConstants;
import common.PropertyDisplay;
import logger.LogFactory;

/**
 * Represents an {@code IHDR} (Image Header) chunk in a PNG file, which stores critical image header
 * information.
 *
 * <p>
 * This chunk must be the first segment in any valid PNG stream. It contains the image dimensions,
 * bit depth, colour type, compression method, filter method, and interlace method required to
 * decode the image data.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 07 July 2026
 */
public class PngChunkIHDR extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkIHDR.class);

    private final int width;
    private final int height;
    private final int bitDepth;
    private final int colorType;
    private final int compressionMethod;
    private final int filterMethod;
    private final int interlaceMethod;

    /**
     * Constructs a {@code PngChunkIHDR} instance with the specified metadata.
     *
     * <p>
     * According to the PNG specification, the IHDR chunk payload must contain exactly 13 bytes:
     * </p>
     *
     * <ul>
     * <li><strong>Width</strong>: 4-byte unsigned integer</li>
     * <li><strong>Height</strong>: 4-byte unsigned integer</li>
     * <li><strong>Bit depth</strong>: 1 byte</li>
     * <li><strong>Color type</strong>: 1 byte</li>
     * <li><strong>Compression method</strong>: 1 byte</li>
     * <li><strong>Filter method</strong>: 1 byte</li>
     * <li><strong>Interlace method</strong>: 1 byte</li>
     * </ul>
     *
     * @param length
     *        the length of the chunk's data field (must be exactly 13)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        the raw IHDR payload
     * @param offsetStart
     *        the absolute physical position in the file where the chunk begins
     */
    public PngChunkIHDR(long length, byte[] typeBytes, int crc32, byte[] data, long offsetStart)
    {
        super(length, typeBytes, crc32, data, offsetStart);

        byte[] payload = getPayloadArray();

        if (getPayloadArray() == null || payload.length < 13)
        {
            LOGGER.warn("IHDR chunk is malformed or contains insufficient payload data [" + (payload != null ? ByteValueConverter.toHex(payload) : "NULL") + "]");

            this.width = 0;
            this.height = 0;
            this.bitDepth = 0;
            this.colorType = -1;
            this.compressionMethod = -1;
            this.filterMethod = -1;
            this.interlaceMethod = -1;
        }

        else
        {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            this.width = buffer.getInt();
            this.height = buffer.getInt();
            this.bitDepth = buffer.get() & 0xFF;
            this.colorType = buffer.get() & 0xFF;
            this.compressionMethod = buffer.get() & 0xFF;
            this.filterMethod = buffer.get() & 0xFF;
            this.interlaceMethod = buffer.get() & 0xFF;
        }
    }

    /**
     * Returns the image width in pixels.
     *
     * @return the image width
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Returns the image height in pixels.
     *
     * @return the image height
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Returns the image bit depth.
     *
     * @return the bit depth
     */
    public int getBitDepth()
    {
        return bitDepth;
    }

    /**
     * Returns the raw PNG colour type value.
     *
     * @return the colour type
     */
    public int getColorType()
    {
        return colorType;
    }

    /**
     * Returns a human-readable description of the compression method.
     *
     * @return the translated compression method
     */
    public int getCompressionMethod()
    {
        return compressionMethod;
    }

    /**
     * Returns the raw filter method value.
     *
     * @return the filter method
     */
    public int getFilterMethod()
    {
        return filterMethod;
    }

    /**
     * Returns the raw interlace method value.
     *
     * @return the interlace method
     */
    public int getInterlaceMethod()
    {
        return interlaceMethod;
    }

    /**
     * Returns a human-readable description of the PNG colour type.
     *
     * @return the translated colour type
     */
    public String translateColorType()
    {
        switch (colorType)
        {
            case 0:
                return "Grayscale";

            case 2:
                return "RGB";

            case 3:
                return "Palette";

            case 4:
                return "Grayscale with Alpha";

            case 6:
                return "RGBA";

            default:
                return "Unknown (" + colorType + ")";
        }
    }

    /**
     * Returns a human-readable description of the compression method.
     *
     * @return the translated compression method
     */
    public String translateCompressionMethod()
    {
        return (compressionMethod == 0 ? "Deflate/Inflate" : "Unknown (" + compressionMethod + ")");
    }

    /**
     * Returns a human-readable description of the filter method.
     *
     * @return the translated filter method
     */
    public String translateFilterMethod()
    {
        return (filterMethod == 0 ? "Adaptive" : "Unknown (" + filterMethod + ")");
    }

    /**
     * Returns a human-readable description of the interlace method.
     *
     * @return the translated interlace method
     */
    public String translateInterlaceMethod()
    {
        switch (interlaceMethod)
        {
            case 0:
                return "Noninterlaced";

            case 1:
                return "Adam7 Interlace";

            default:
                return "Unknown (" + interlaceMethod + ")";
        }
    }

    /**
     * Prints the structural image header fields of this chunk to the specified display target.
     *
     * <p>
     * <strong>Integration Note:</strong> This method is intended primarily for use by
     * {@code PhotoshopManager}, which invokes it to collect and format chunk properties for display
     * and reporting.
     * </p>
     *
     * @param display
     *        the target that receives the formatted metadata properties
     */
    @Override
    public void printProperties(PropertyDisplay display)
    {
        display.accept("Image Width", getWidth());
        display.accept("Image Height", getHeight());
        display.accept("Bit Depth", getBitDepth());
        display.accept("Colour Type", translateColorType());
        display.accept("Compression", translateCompressionMethod());
        display.accept("Filter", translateFilterMethod());
        display.accept("Interlace", translateInterlaceMethod());
    }

    /**
     * Returns a string representation of the chunk's properties and structural metadata.
     *
     * @return a formatted string describing this chunk
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());
        sb.append(String.format(MetadataConstants.FORMATTER, "Image Width", getWidth()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Image Height", getHeight()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Bit Depth", getBitDepth()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Colour Type", translateColorType()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Compression", translateCompressionMethod()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Filter", translateFilterMethod()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Interlace", translateInterlaceMethod()));

        return sb.toString();
    }
}
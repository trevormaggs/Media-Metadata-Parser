package png;

import common.ByteValueConverter;
import common.MetadataConstants;
import common.PropertyDisplay;
import logger.LogFactory;

/**
 * Represents an {@code sRGB} (Standard RGB Colour Space) chunk in a PNG file.
 *
 * <p>
 * This chunk contains a single byte that specifies the rendering intent associated with the image's
 * sRGB colour space.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 06 July 2026
 */
public class PngChunkSRGB extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkSRGB.class);
    private final int rendering;

    /**
     * Constructs a {@code PngChunkSRGB} instance with the specified metadata.
     *
     * @param length
     *        the length of the chunk's data field (must be exactly 1)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     * @param offsetStart
     *        the absolute physical position in the file where the chunk begins
     */
    public PngChunkSRGB(long length, byte[] typeBytes, int crc32, byte[] data, long offsetStart)
    {
        super(length, typeBytes, crc32, data, offsetStart);

        byte[] payload = getPayloadArray();

        if (payload != null && payload.length == 1)
        {
            this.rendering = payload[0] & 0xFF;
        }

        else
        {
            LOGGER.warn("sRGB chunk missing structural data or malformed [" + (payload != null ? ByteValueConverter.toHex(payload) : "NULL") + "]");
            this.rendering = 0;
        }
    }

    /**
     * Returns the raw rendering intent value stored in the chunk.
     *
     * @return the rendering intent value
     */
    public int getRendering()
    {
        return rendering;
    }

    /**
     * Returns a human-readable description of the rendering intent.
     *
     * @return the rendering intent description
     */
    public String translateRendering()
    {
        switch (rendering)
        {
            case 0:
                return "Perceptual";

            case 1:
                return "Relative colorimetric";

            case 2:
                return "Saturation";

            case 3:
                return "Absolute colorimetric";

            default:
                return "Unknown (" + rendering + ")";
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
        display.accept("SRGB Rendering", translateRendering());
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
        sb.append(String.format(MetadataConstants.FORMATTER, "SRGB Rendering", translateRendering()));

        return sb.toString();
    }
}
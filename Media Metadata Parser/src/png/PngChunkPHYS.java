package png;

import java.nio.ByteOrder;
import common.ByteValueConverter;
import common.MetadataConstants;
import common.PropertyDisplay;

/**
 * Represents a PNG {@code pHYs} (Physical Pixel Dimensions) chunk.
 *
 * <p>
 * This chunk specifies the intended physical pixel density of the image, including the horizontal
 * and vertical pixels-per-unit values and their associated unit.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 08 July 2026
 */
public class PngChunkPHYS extends PngChunk
{
    private final long pixelsPerUnitX;
    private final long pixelsPerUnitY;
    private final int unitSpecifier;

    /**
     * Creates a PNG {@code pHYs} chunk.
     *
     * @param length
     *        the length of the chunk's data field (must be exactly 9)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     * @param offsetStart
     *        the absolute physical position in the file where the chunk begins
     */
    public PngChunkPHYS(long length, byte[] typeBytes, int crc32, byte[] data, long offsetStart)
    {
        super(length, typeBytes, crc32, data, offsetStart);

        byte[] payload = getPayloadArray();

        if (payload != null && payload.length == 9)
        {
            this.pixelsPerUnitX = ByteValueConverter.toUnsignedInteger(payload, 0, ByteOrder.BIG_ENDIAN);
            this.pixelsPerUnitY = ByteValueConverter.toUnsignedInteger(payload, 4, ByteOrder.BIG_ENDIAN);
            this.unitSpecifier = payload[8] & 0xFF;
        }

        else
        {
            this.pixelsPerUnitX = 0L;
            this.pixelsPerUnitY = 0L;
            this.unitSpecifier = 0;
        }
    }

    /**
     * Returns the horizontal pixel density.
     *
     * @return the number of pixels per unit along the X axis
     */
    public long getPixelsPerUnitX()
    {
        return pixelsPerUnitX;
    }

    /**
     * Returns the vertical pixel density.
     *
     * @return the number of pixels per unit along the Y axis
     */
    public long getPixelsPerUnitY()
    {
        return pixelsPerUnitY;
    }

    /**
     * Returns whether the pixel density is expressed in metres.
     *
     * @return {@code true} if the unit is metres; {@code false} if the unit is unknown
     */
    public boolean isMeters()
    {
        return unitSpecifier == 1;
    }

    /**
     * Returns the horizontal resolution in dots per inch (DPI).
     *
     * @return the rounded horizontal DPI, or {@code 0} if the unit is unknown
     */
    public int getXResolutionDPI()
    {
        return (isMeters() ? (int) Math.round(pixelsPerUnitX * 0.0254) : 0);
    }

    /**
     * Returns the vertical resolution in dots per inch (DPI).
     *
     * @return the rounded vertical DPI, or {@code 0} if the unit is unknown
     */
    public int getYResolutionDPI()
    {
        return (isMeters() ? (int) Math.round(pixelsPerUnitY * 0.0254) : 0);
    }

    /**
     * Returns the unit used for the pixel density values.
     *
     * @return {@code "meters"} if the unit is defined; otherwise {@code "unknown"}
     */
    public String translateUnit()
    {
        return (isMeters() ? "meters" : "unknown");
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
        display.accept("PixelsPerUnitX", getPixelsPerUnitX());
        display.accept("PixelsPerUnitY", getPixelsPerUnitY());
        display.accept("PixelUnits", translateUnit());
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
        sb.append(String.format(MetadataConstants.FORMATTER, "PixelsPerUnitX", pixelsPerUnitX));
        sb.append(String.format(MetadataConstants.FORMATTER, "PixelsPerUnitY", pixelsPerUnitY));
        sb.append(String.format(MetadataConstants.FORMATTER, "PixelUnits", translateUnit()));

        if (isMeters())
        {
            sb.append(String.format(MetadataConstants.FORMATTER, "X Resolution", getXResolutionDPI() + " dpi"));
            sb.append(String.format(MetadataConstants.FORMATTER, "Y Resolution", getYResolutionDPI() + " dpi"));
        }

        return sb.toString();
    }
}
package png;

import java.math.RoundingMode;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import common.ByteValueConverter;
import common.MetadataConstants;
import common.PropertyDisplay;
import logger.LogFactory;

/**
 * Represents a {@code gAMA} (Image Gamma) chunk in a PNG file.
 *
 * <p>
 * This chunk contains a 4-byte unsigned integer that specifies the relationship between the image
 * samples and the desired display output intensity, scaled by 100,000.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 07 July 2026
 */
public class PngChunkGAMA extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkGAMA.class);
    private final long gamma;

    /**
     * Constructs a {@code PngChunkGAMA} instance with the specified metadata.
     *
     * @param length
     *        the length of the chunk's data field (must be exactly 4)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     * @param offsetStart
     *        the absolute physical position in the file where the chunk begins
     */
    public PngChunkGAMA(long length, byte[] typeBytes, int crc32, byte[] data, long offsetStart)
    {
        super(length, typeBytes, crc32, data, offsetStart);

        byte[] payload = getPayloadArray();

        if (payload != null && payload.length == 4)
        {
            this.gamma = ByteValueConverter.toUnsignedInteger(payload, ByteOrder.BIG_ENDIAN);
        }

        else
        {
            this.gamma = 0L;
            LOGGER.warn("gAMA chunk has an invalid payload [" + (payload != null ? ByteValueConverter.toHex(payload) : "NULL") + "]");
        }
    }

    /**
     * Returns the raw scaled gamma value stored in the chunk.
     *
     * @return the unsigned 4-byte gamma value
     */
    public long getGamma()
    {
        return gamma;
    }

    /**
     * Converts the stored PNG gamma value into a human-readable display gamma, using the reciprocal
     * of the encoded value to match ExifTool's output.
     * 
     * @return the formatted gamma metric string (e.g., "2.2")
     */
    public String translateGamma()
    {
        if (gamma != 0L)
        {
            double realGamma = gamma / 100000.0;
            double reciprocal = 1.0 / realGamma;

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
            DecimalFormat df = new DecimalFormat("0.0###", symbols);

            df.setRoundingMode(RoundingMode.HALF_UP);

            return df.format(reciprocal);
        }

        return "0";
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
        display.accept("Gamma", translateGamma());
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
        sb.append(String.format(MetadataConstants.FORMATTER, "Gamma", translateGamma()));

        return sb.toString();
    }
}
package png;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import common.MetadataConstants;
import common.PropertyDisplay;
import common.Utils;

/**
 * Encapsulates the {@code tIME} ancillary chunk, which records the last modification time of the
 * image content (not the creation time).
 * *
 * <p>
 * According to the PNG specification, the time values must represent <b>Universal Coordinated Time
 * (UTC)</b> to ensure consistency across different time zones.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 31 May 2026
 */
public class PngChunkTIME extends PngChunk
{
    private final int year;
    private final int month;
    private final int day;
    private final int hour;
    private final int minute;
    private final int second;

    public PngChunkTIME(long length, byte[] typeBytes, int crc32, byte[] data, long offset)
    {
        super(length, typeBytes, crc32, data, offset);

        if (data.length != 7)
        {
            throw new IllegalArgumentException("Invalid tIME chunk length. Expected 7, found " + data.length);
        }

        this.year = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        this.month = data[2] & 0xFF;
        this.day = data[3] & 0xFF;
        this.hour = data[4] & 0xFF;
        this.minute = data[5] & 0xFF;

        /*
         * Important note: this PNG chunk allows the second to be
         * between 0 and 60 seconds to support leap seconds
         */
        int sec = data[6] & 0xFF;
        this.second = (sec == 60) ? 59 : sec;
    }

    /**
     * Converts the binary fields into a modern, immutable {@link ZonedDateTime} instance explicitly
     * localised to UTC.
     * 
     * @return a {@link ZonedDateTime} representing the image modification time in UTC
     */
    public ZonedDateTime getModificationTime()
    {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneOffset.UTC);
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
        display.accept("Modification Date", getModificationTime());
    }

    /**
     * Returns a string representation of the chunk's properties and contents.
     * 
     * @return a formatted string describing this chunk
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());

        String isoDateText = getModificationTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String formattedDate = Utils.formatDateString(isoDateText, Utils.LOCALE_AU);

        sb.append(String.format(MetadataConstants.FORMATTER, "Modification Date", formattedDate));

        return sb.toString();
    }
}
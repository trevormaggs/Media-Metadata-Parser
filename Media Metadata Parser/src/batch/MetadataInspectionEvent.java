package batch;

import java.util.Objects;
import tif.tagspecs.Taggable;

/**
 * Immutable domain event representing an inspected metadata property, tag entry, XMP record, or
 * structural output delimiter extracted from a media file.
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 13 August 2026
 */
public final class MetadataInspectionEvent
{
    private final MediaRecord record;
    private final String groupName;
    private final String propertyName;
    private final Object propertyValue;

    /**
     * Creates a metadata inspection event representing a raw structural delimiter or line-formatted
     * header without associated media property attributes.
     *
     * @param line
     *        the raw string content or line delimiter to emit
     */
    public MetadataInspectionEvent(String line)
    {
        this.record = null;
        this.groupName = "";
        this.propertyName = "";
        this.propertyValue = (line != null ? line : "");
    }

    /**
     * Creates a metadata inspection event containing an extracted attribute, EXIF tag, or XMP data
     * entry for a media record.
     *
     * @param record
     *        the source media record being inspected
     * @param group
     *        the metadata category, directory, or XMP group name, such as {@code "[EXIF]"},
     *        {@code "[System]"}, or {@code "[XMP-dc]"}
     * @param name
     *        the name of the metadata tag, property, or XMP element
     * @param value
     *        the formatted display value or raw object representation of the attribute
     */
    public MetadataInspectionEvent(MediaRecord record, String group, String name, Object value)
    {
        this.record = Objects.requireNonNull(record, "Record cannot be null");
        this.groupName = (group != null ? group : "");
        this.propertyName = (name != null ? name : "");
        this.propertyValue = (value != null ? value : "");
    }

    /**
     * Returns the underlying source media record.
     *
     * @return the media record, or {@code null} if this event represents a delimiter
     */
    public MediaRecord getRecord()
    {
        return record;
    }

    /**
     * Returns the simple file name of the source media record.
     *
     * @return the source file name, or an empty string if no media record is associated
     */
    public String getSourceName()
    {
        return (record != null ? record.getPath().getFileName().toString() : "");
    }

    /**
     * Returns the metadata group heading.
     *
     * @return the group name, such as {@code "[System]"} or {@code "[EXIF]"}
     */
    public String getGroupName()
    {
        return groupName;
    }

    /**
     * Returns the metadata tag or property name.
     *
     * @return the property name
     */
    public String getPropertyName()
    {
        return propertyName;
    }

    /**
     * Returns the raw unformatted property value object.
     *
     * @return the raw property value object
     */
    public Object getRawPropertyValue()
    {
        return propertyValue;
    }

    /**
     * Returns the formatted metadata tag or property value string.
     *
     * @return the property value as a string
     */
    public String getPropertyValue()
    {
        return propertyValue.toString();
    }

    /**
     * Indicates whether this event represents a structural line delimiter or formatted header
     * rather than a key-value metadata property tuple.
     *
     * @return {@code true} if this event is a raw text delimiter, otherwise {@code false}
     */
    public boolean isDelimiter()
    {
        return record == null;
    }

    @Override
    public String toString()
    {
        return (record == null ? propertyValue.toString() : String.format(Taggable.COLUMN_FORMAT, groupName, propertyName, propertyValue.toString()));
    }
}
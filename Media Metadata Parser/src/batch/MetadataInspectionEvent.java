package batch;

import java.util.Objects;
import common.Metadata;
import tif.tagspecs.Taggable;

/**
 * Immutable domain event representing an inspected metadata property, tag entry, XMP record,
 * raw parsed {@link Metadata} container object, or structural output delimiter extracted from a
 * media file.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 23 September 2026
 */
public final class MetadataInspectionEvent
{
    private final MediaRecord record;
    private final String groupName;
    private final String propertyName;
    private final Object propertyValue;
    private final Metadata<?> metadata;

    /**
     * Creates a metadata inspection event representing a raw structural delimiter or line-formatted
     * header without associated media property attributes.
     *
     * @param line
     *        the raw string content or line-formatted header to emit
     */
    public MetadataInspectionEvent(String line)
    {
        this.record = null;
        this.groupName = "";
        this.propertyName = "";
        this.propertyValue = (line != null ? line : "");
        this.metadata = null;
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
        this.metadata = null;
    }

    /**
     * Creates a metadata inspection event wrapping a complete parsed {@link Metadata} container
     * associated with the inspected media record.
     *
     * @param record
     *        the source media record being inspected
     * @param metadata
     *        the parsed metadata tree container object
     */
    public MetadataInspectionEvent(MediaRecord record, Metadata<?> metadata)
    {
        this.record = Objects.requireNonNull(record, "Record cannot be null");
        this.groupName = "";
        this.propertyName = "";
        this.propertyValue = "";
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
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
     * Returns the metadata tag or property value as a string.
     *
     * @return the property value converted to a string
     */
    public String getPropertyValue()
    {
        return propertyValue.toString();
    }

    /**
     * Returns the parsed {@link Metadata} container object if present.
     *
     * @return the metadata container object, or {@code null} if not a container event
     */
    public Metadata<?> getMetadata()
    {
        return metadata;
    }

    /**
     * Indicates whether this event carries a parsed {@link Metadata} container object.
     *
     * @return {@code true} if a metadata container object is available, otherwise {@code false}
     */
    public boolean hasMetadata()
    {
        return metadata != null;
    }

    /**
     * Indicates whether this event represents a structural line delimiter or formatted header
     * rather than a key-value metadata property tuple or metadata container.
     *
     * @return {@code true} if this event represents structural text, otherwise {@code false}
     */
    public boolean isDelimiter()
    {
        return record == null;
    }

    @Override
    public String toString()
    {
        if (isDelimiter() || groupName.isEmpty())
        {
            return propertyValue.toString();
        }

        String displayGroup = groupName;

        if (displayGroup.length() > 15)
        {
            displayGroup = displayGroup.substring(0, 9) + "...]";
        }

        return String.format(Taggable.COLUMN_FORMAT, displayGroup, propertyName, propertyValue.toString());
    }
}
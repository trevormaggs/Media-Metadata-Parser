package tif;

import java.nio.ByteOrder;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import common.Directory;
import common.MetadataConstants;
import tif.DirectoryIFD.EntryIFD;
import tif.tagspecs.Taggable;

/**
 * A collection-based representation of a TIFF Image File Directory (IFD).
 *
 * <p>
 * A {@code DirectoryIFD} serves as a specialised container for {@link EntryIFD} objects. It
 * provides a high-level API to retrieve metadata in native Java formats, such as {@link Date},
 * {@link String}, or {@link RationalNumber}, while handling the underlying TIFF type conversions
 * automatically.
 * </p>
 *
 * <p>
 * Each instance represents a single directory within the file. While some directories contain pixel
 * data, others may store only metadata structures, such as EXIF or GPS blocks.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 13 August 2025
 * @see EntryIFD
 */
public class DirectoryIFD implements Directory<EntryIFD>
{
    private final Map<Integer, EntryIFD> entryMap = new LinkedHashMap<>();
    private DirectoryIdentifier directoryType;

    /**
     * Represents a single metadata entry within an Image File Directory.
     *
     * <p>
     * Each {@code EntryIFD} acts as a bridge between the raw TIFF field structure and Java objects.
     * It encapsulates the tag identity, field type, value count, and the resulting parsed data
     * (such as a String, Integer, or RationalNumber).
     * </p>
     *
     * Instances of this class are immutable. Internal byte arrays are defensively copied to
     * maintain data integrity.
     * </p>
     */
    public final static class EntryIFD
    {
        private final Taggable tagEnum;
        private final TifFieldType fieldType;
        private final long count;
        private final long valueOffset;
        private final byte[] value;
        private final Object parsedData;

        /**
         * Constructs an immutable {@code EntryIFD} instance from raw bytes.
         *
         * @param tag
         *        the tag descriptor (Taggable enum)
         * @param ttype
         *        the TIFF field type
         * @param length
         *        the number of values (count)
         * @param offset
         *        the raw offset or immediate value field
         * @param bytes
         *        the raw value bytes; the constructor performs a defensive copy
         * @param order
         *        the byte order used to parse the bytes
         */
        public EntryIFD(Taggable tag, TifFieldType ttype, long length, long offset, byte[] bytes, ByteOrder order)
        {
            this.tagEnum = tag;
            this.fieldType = ttype;
            this.count = length;
            this.valueOffset = offset;
            this.value = (bytes != null ? Arrays.copyOf(bytes, bytes.length) : null);
            this.parsedData = fieldType.parse(value, count, order);
        }

        /**
         * @return the tag enum that identifies this entry
         */
        public Taggable getTag()
        {
            return tagEnum;
        }

        /**
         * @return the numeric ID of the tag
         */
        public int getTagID()
        {
            return tagEnum.getNumberID();
        }

        /**
         * @return the TIFF field type for this entry
         */
        public TifFieldType getFieldType()
        {
            return fieldType;
        }

        /**
         * @return the number of values represented by this entry
         */
        public long getCount()
        {
            return count;
        }

        /**
         * @return the absolute file offset or the immediate value for this entry
         */
        public long getOffset()
        {
            return valueOffset;
        }

        /**
         * @return this entry's raw byte array, or null if not set
         */
        public byte[] getByteArray()
        {
            return (value != null) ? Arrays.copyOf(value, value.length) : null;
        }

        /**
         * @return the total byte length of the data based on type and count
         */
        public long getByteLength()
        {
            return count * fieldType.getFieldSize();
        }

        /**
         * @return the parsed data object, or null if no value is available
         */
        public Object getData()
        {
            return parsedData;
        }

        /**
         * @return true if the parsed data is an array type
         */
        public boolean isArray()
        {
            return (parsedData != null && parsedData.getClass().isArray());
        }

        /**
         * Generates a human-readable summary of the entry, suitable for logging or metadata
         * inspection. Includes the tag name, numeric ID, field type, and a formatted representation
         * of the value.
         *
         * @return a multi-line formatted string
         */
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            // Tag, Type, and Count Information
            sb.append(String.format(MetadataConstants.FORMATTER, "Tag Name", getTag() + " (Tag ID: " + String.format("0x%04X", getTagID()) + ")"));
            sb.append(String.format(MetadataConstants.FORMATTER, "Field Type", getFieldType() + " (count: " + getCount() + ")"));
            sb.append(String.format(MetadataConstants.FORMATTER, "Value", TagValueConverter.toStringValue(this)));
            sb.append(String.format(MetadataConstants.FORMATTER, "Hint", getTag().getHint()));

            if (getByteLength() > IFDHandler.ENTRY_MAX_VALUE_LENGTH)
            {
                sb.append(String.format(MetadataConstants.FORMATTER, "Jump Offset", String.format("0x%08X", valueOffset)));
            }

            else
            {
                String hexVal = String.format("0x%08X", valueOffset);
                sb.append(String.format(MetadataConstants.FORMATTER, "Inline Value", valueOffset + " (" + hexVal + ")"));
            }

            return sb.toString();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            EntryIFD entryIFD = (EntryIFD) o;

            if ((count != entryIFD.count) || (valueOffset != entryIFD.valueOffset) || (fieldType != entryIFD.fieldType))
            {
                return false;
            }

            if (!Objects.equals(tagEnum, entryIFD.tagEnum))
            {
                return false;
            }

            if (!Arrays.equals(value, entryIFD.value))
            {
                return false;
            }

            return Objects.deepEquals(parsedData, entryIFD.parsedData);
        }

        @Override
        public int hashCode()
        {
            int result = tagEnum != null ? tagEnum.hashCode() : 0;

            result = 31 * result + (fieldType != null ? fieldType.hashCode() : 0);
            result = 31 * result + (int) (count ^ (count >>> 32));
            result = 31 * result + (int) (valueOffset ^ (valueOffset >>> 32));
            result = 31 * result + Arrays.hashCode(value);
            result = 31 * result + (parsedData != null ? parsedData.hashCode() : 0);

            return result;
        }
    }

    /**
     * Constructs a new directory instance for a specific directory type.
     *
     * @param dirType
     *        the directory type identifier, for example: IFD0, EXIF, etc
     */
    public DirectoryIFD(DirectoryIdentifier dirType)
    {
        this.directoryType = dirType;
    }

    /**
     * Adds a new {@code EntryIFD} entry to the collection.
     *
     * @param entry
     *        {@code EntryIFD} object
     */
    @Override
    public void add(EntryIFD entry)
    {
        entryMap.put(entry.getTagID(), entry);
    }

    /**
     * Removes a {@code EntryIFD} entry from this directory.
     *
     * @param entry
     *        {@code EntryIFD} object to remove
     */
    @Override
    public boolean remove(EntryIFD entry)
    {
        return entryMap.remove(entry.getTagID(), entry);
    }

    /**
     * Checks if the specified {@code EntryIFD} entry has been added to this directory.
     *
     * @param entry
     *        {@code EntryIFD} object to check for
     * @return true if the specified entry is contained in the map
     */
    @Override
    public boolean contains(EntryIFD entry)
    {
        return entryMap.containsValue(entry);
    }

    /**
     * Returns the count of IFD entries present in this directory.
     *
     * @return the total number of entries
     */
    @Override
    public int size()
    {
        return entryMap.size();
    }

    /**
     * Returns true if the entry map is empty.
     *
     * @return true if empty, otherwise false
     */
    @Override
    public boolean isEmpty()
    {
        return entryMap.isEmpty();
    }

    /**
     * Retrieves an iterator to navigate through a collection of {@code EntryIFD} objects.
     *
     * @return an Iterator object
     */
    @Override
    public Iterator<EntryIFD> iterator()
    {
        return entryMap.values().iterator();
    }

    /**
     * Updates the directory type identifier. Used when a directory is promoted, for example: from
     * ROOT to IFD1, during the parsing phase.
     *
     * @param dirType
     *        the new directory type
     */
    public void setDirectoryType(DirectoryIdentifier dirType)
    {
        directoryType = dirType;
    }

    /**
     * Gets the current Image File Directory type.
     *
     * @return an enumeration value of DirectoryIdentifier
     */
    public DirectoryIdentifier getDirectoryType()
    {
        return directoryType;
    }

    /**
     * Checks if the specified tag is present in this directory.
     *
     * @param tag
     *        the enumeration tag to look for
     * @return true if an entry for the specified tag exists in this directory, otherwise false
     */
    public boolean hasTag(Taggable tag)
    {
        return entryMap.containsKey(tag.getNumberID());
    }

    /**
     * Returns a copy of the raw bytes associated with a tag.
     *
     * <p>
     * Use this method for tags containing "black box" data, such as embedded XMP packets, ICC
     * profiles, or private maker notes.
     * </p>
     *
     * @param tag
     *        the tag to obtain raw bytes for
     * @return a raw byte array, or an empty array if the tag is missing
     */
    public byte[] getRawByteArray(Taggable tag)
    {
        EntryIFD entry = getTagEntry(tag);

        if (entry != null)
        {
            byte[] b = entry.getByteArray();

            if (b != null && b.length > 0)
            {
                return b;
            }
        }

        return new byte[0];
    }

    /**
     * Returns the integer value associated with the specified tag.
     *
     * <p>
     * If the tag is missing or if the entry is not convertible to an int, this method throws an
     * exception, since numeric values are considered required when calling this method.
     * </p>
     *
     * @param tag
     *        the enumeration tag to retrieve
     * @return the tag's value as an int
     */
    public int getIntValue(Taggable tag)
    {
        return TagValueConverter.getIntValue(getTagEntry(tag));
    }

    /**
     * Retrieves the value of the specified tag as an array of integers.
     * 
     * @param tag
     *        the enumeration tag identifying the metadata entry
     * @return an array of integers, or an empty array if the tag cannot be determined
     */
    public int[] getIntArray(Taggable tag)
    {
        return TagValueConverter.getIntArray(getTagEntry(tag));
    }

    /**
     * Returns the long value associated with the specified tag.
     *
     * @param tag
     *        the enumeration tag to retrieve
     * @return the tag's value as a long
     */
    public long getLongValue(Taggable tag)
    {
        return TagValueConverter.getLongValue(getTagEntry(tag));
    }

    /**
     * Retrieves the value of the specified tag as an array of longs.
     * 
     * @param tag
     *        the enumeration tag identifying the metadata entry
     * @return an array of longs, or an empty array if the tag cannot be determined
     */
    public long[] getLongArray(Taggable tag)
    {
        return TagValueConverter.getLongArray(getTagEntry(tag));
    }

    /**
     * Returns the float value associated with the specified tag.
     *
     * @param tag
     *        the enumeration tag to retrieve
     * @return the tag's value as a float
     */
    public float getFloatValue(Taggable tag)
    {
        return TagValueConverter.getFloatValue(getTagEntry(tag));
    }

    /**
     * Retrieves the value of the specified tag as an array of floats.
     * 
     * @param tag
     *        the enumeration tag identifying the metadata entry
     * @return an array of floats, or an empty array if the tag cannot be determined
     */
    public float[] getFloatArray(Taggable tag)
    {
        return TagValueConverter.getFloatArray(getTagEntry(tag));
    }

    /**
     * Returns the double value associated with the specified tag.
     *
     * @param tag
     *        the enumeration tag to retrieve
     * @return the tag's value as a double
     */
    public double getDoubleValue(Taggable tag)
    {
        return TagValueConverter.getDoubleValue(getTagEntry(tag));
    }

    /**
     * Retrieves the value of the specified tag as an array of doubles.
     * 
     * @param tag
     *        the enumeration tag identifying the metadata entry
     * @return an array of doubles, or an empty array if the tag cannot be determined
     */
    public double[] getDoubleArray(Taggable tag)
    {
        return TagValueConverter.getDoubleArray(getTagEntry(tag));
    }

    /**
     * Returns the rational number value associated with the specified tag.
     *
     * @param tag
     *        the enumeration tag to fetch
     * @return the tag's rational value
     *
     * @throws IllegalArgumentException
     *         if the tag is missing or the entry's data is not an instance of RationalNumber
     */
    public RationalNumber getRationalValue(Taggable tag)
    {
        return TagValueConverter.getRationalValue(getTagEntry(tag));
    }

    /**
     * Returns an array of rational numbers associated with the specified tag.
     *
     * @param tag
     *        the enumeration tag to fetch
     * @return the tag's rational array value
     */
    public RationalNumber[] getRationalArrayValue(Taggable tag)
    {
        return TagValueConverter.getRationalArray(getTagEntry(tag));
    }

    /**
     * Returns the string value associated with the specified tag.
     *
     * @param tag
     *        the enumeration tag to obtain the value for
     * @return a string representing the tag's value
     *
     * @throws IllegalArgumentException
     *         if the tag is missing or information cannot be obtained
     */
    public String getString(Taggable tag)
    {
        EntryIFD entry = getTagEntry(tag);

        if (entry == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] not found in directory [%s]",
                    tag,
                    tag.getNumberID(),
                    getDirectoryType().getDescription()));
        }

        return TagValueConverter.toStringValue(entry);
    }

    /**
     * Returns a Date object associated with the specified tag, delegating parsing and validation to
     * the {@code TagValueConverter} utility.
     *
     * @param tag
     *        the enumeration tag to obtain the value for
     * @return a Date object if present and successfully parsed
     *
     * @throws IllegalArgumentException
     *         if the tag is missing or its value cannot be parsed as a valid Date
     */
    public Date getDate(Taggable tag)
    {
        EntryIFD entry = getTagEntry(tag);

        if (entry == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] not found in directory [%s]",
                    tag,
                    tag.getNumberID(),
                    getDirectoryType().getDescription()));
        }

        return TagValueConverter.getDate(entry);
    }

    /**
     * Returns the value of the specified tag as a {@link ZonedDateTime} object.
     * 
     * <p>
     * This method validates that the tag exists in the directory and that its content adheres to a
     * recognised date-time format (including ISO-8601 and regional variations).
     * </p>
     * 
     * @param tag
     *        the enumeration tag identifying the metadata entry
     * @return a {@link ZonedDateTime} representing the tag's value
     * 
     * @throws IllegalArgumentException
     *         if the tag is missing, or if the value cannot be parsed as a valid date format
     */
    public ZonedDateTime getZonedDateTime(Taggable tag)
    {
        EntryIFD entry = getTagEntry(tag);

        if (entry == null)
        {
            throw new IllegalArgumentException(String.format("Tag [%s (0x%04X)] not found in directory [%s]",
                    tag,
                    tag.getNumberID(),
                    getDirectoryType().getDescription()));
        }

        return TagValueConverter.getZonedDateTime(entry);
    }

    /**
     * Retrieves the {@code EntryIFD} associated with the numerical ID of the specified tag.
     *
     * <p>
     * This allows access to the raw directory entry metadata, including its TIFF field type, the
     * number of values (count), and the raw byte data or offset.
     * </p>
     *
     * @param tag
     *        the {@code Taggable} constant whose ID will be used for the lookup
     * @return the matched {@code EntryIFD}, or {@code null} if no entry with this ID exists in the
     *         current directory
     */
    public EntryIFD getTagEntry(Taggable tag)
    {
        return getEntry(tag.getNumberID());
    }

    /**
     * Retrieves an entry by its raw numeric ID.
     *
     * @param tagID
     *        the tag ID number to fetch
     * @return the matched {@code EntryIFD}, or {@code null} if no entry with this ID exists in the
     *         current directory
     */
    public EntryIFD getEntry(int tagID)
    {
        return entryMap.get(tagID);
    }

    /**
     * Generates a formatted string showing current values of every IFD entry in the collection.
     *
     * @return a comprehensive, string-based representation of each IFD entry
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Directory Type - ");
        sb.append(getDirectoryType().getDescription());
        sb.append(String.format(" (%d entries)%n", size()));
        sb.append(MetadataConstants.DIVIDER);
        sb.append(System.lineSeparator());

        for (EntryIFD entry : this)
        {
            sb.append(entry);
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
package xmp;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import common.Directory;
import common.MetadataConstants;
import xmp.XmpDirectory.XmpRecord;

/**
 * Stores a collection of {@link XmpRecord} objects representing XMP metadata.
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 10 November 2025
 */
public final class XmpDirectory implements Directory<XmpRecord>
{
    private final Map<String, XmpRecord> propertyMap;

    /**
     * Represents a single and immutable XMP property record.
     * 
     * <p>
     * Each {@code XmpRecord} encapsulates the namespace URI, cleaned property path, and the
     * property value. It is immutable and self-contained.
     * </p>
     */
    public static final class XmpRecord
    {
        private static final Pattern REGEX_PATH = Pattern.compile("^\\s*(\\w+):(.+)$");
        private final String namespace;
        private final String path;
        private final String value;
        private final String prefix;
        private final String name;

        /**
         * Constructs an immutable {@code XmpRecord} instance to hold a single record.
         *
         * @param namespace
         *        the namespace URI of the property
         * @param path
         *        the path of the property (e.g., dc:creator)
         * @param value
         *        the value of the property
         */
        public XmpRecord(String namespace, String path, String value)
        {
            this.namespace = namespace;
            this.path = path;
            this.value = value;

            Matcher matcher = REGEX_PATH.matcher(path);

            if (matcher.matches())
            {
                this.prefix = matcher.group(1);
                this.name = matcher.group(2);
            }

            else
            {
                this.prefix = "";
                this.name = path;
            }
        }

        /** @return the namespace URI of the property */
        public String getNamespace()
        {
            return namespace;
        }

        /** @return the path of the property */
        public String getPath()
        {
            return path;
        }

        /** @return the short identifier of the path */
        public String getPrefix()
        {
            return prefix;
        }

        /** @return the property name of the path */
        public String getName()
        {
            return name;
        }

        /** @return the value of the property */
        public String getValue()
        {
            return value;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (!(obj instanceof XmpRecord))
            {
                return false;
            }

            XmpRecord other = (XmpRecord) obj;

            return Objects.equals(namespace, other.namespace) && Objects.equals(path, other.path) && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(namespace, path, value);
        }

        /**
         * Returns a string representation of this {@link XmpRecord} object.
         *
         * @return formatted string describing the entry’s key characteristics
         */
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(MetadataConstants.FORMATTER, "Namespace", getNamespace()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Prefix", getPrefix()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Name", getName()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Full Path", getPath()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Value", getValue()));

            return sb.toString();
        }
    }

    /**
     * Constructs an empty XMP directory.
     */
    public XmpDirectory()
    {
        this.propertyMap = new LinkedHashMap<>();
    }

    /**
     * Retrieves the string value corresponding to the specified property path descriptor.
     *
     * @param prop
     *        an XmpProperty instance containing a fully qualified path key
     * @return an Optional containing the value found; {@link Optional#empty()} otherwise
     */
    public Optional<String> getValueByPath(XmpProperty prop)
    {
        XmpRecord xmp = propertyMap.get(prop.getQualifiedPath());

        return (xmp != null) ? Optional.of(xmp.getValue()) : Optional.empty();
    }

    /**
     * Adds a single {@link XmpRecord} to this directory.
     *
     * @param prop
     *        the XmpRecord to be added
     */
    @Override
    public void add(XmpRecord prop)
    {
        Objects.requireNonNull(prop, "Property cannot be null");

        propertyMap.put(prop.getPath(), prop);
    }

    /**
     * Removes a {@code XmpRecord} property from this directory.
     *
     * @param prop
     *        {@code XmpRecord} object to remove
     * @return {@code true} if an element was removed as a result of this invocation
     * 
     * @throws NullPointerException
     *         if {@code prop} is null
     */
    @Override
    public boolean remove(XmpRecord prop)
    {
        Objects.requireNonNull(prop, "Property cannot be null");

        return (propertyMap.remove(prop.getPath()) != null);
    }

    /**
     * Checks if a specific {@link XmpRecord} property is present in this directory.
     *
     * @param prop
     *        the XmpRecord to check for
     * @return {@code true} if the property is found, otherwise {@code false}
     */
    @Override
    public boolean contains(XmpRecord prop)
    {
        return (prop != null && propertyMap.containsKey(prop.getPath()));
    }

    /**
     * Returns the number of {@link XmpRecord} objects in this directory.
     *
     * @return the size of the directory
     */
    @Override
    public int size()
    {
        return propertyMap.size();
    }

    /**
     * Checks whether this directory contains any {@link XmpRecord} objects.
     *
     * @return {@code true} if this directory contains no records, otherwise {@code false}
     */
    @Override
    public boolean isEmpty()
    {
        return propertyMap.isEmpty();
    }

    /**
     * Returns an iterator over the extracted XMP properties. The properties are returned in the
     * order they appeared in the original XMP payload.
     * 
     * @return an iterator of {@link XmpRecord} objects
     */
    @Override
    public Iterator<XmpRecord> iterator()
    {
        return propertyMap.values().iterator();
    }

    /**
     * Returns a string representation of this directory, which is the concatenation of the string
     * representations of all contained {@link XmpRecord} objects, each on a new line.
     *
     * @return a multi-line string representing the properties in the directory
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("XMP Metadata (").append(size()).append(" entries)").append(System.lineSeparator());
        sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());

        for (XmpRecord record : propertyMap.values())
        {
            sb.append(record).append(System.lineSeparator());
        }

        return sb.toString();
    }
}
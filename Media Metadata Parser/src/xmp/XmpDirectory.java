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

/**
 * Stores a collection of {@link XmpRecord} objects representing XMP metadata properties.
 *
 * <p>
 * Records are indexed by their qualified property paths (for example, {@code dc:creator}) to
 * support efficient lookup, value translation, and flexible metadata queries.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 28 June 2026
 */
public final class XmpDirectory implements Directory<XmpDirectory.XmpRecord>
{
    private final Map<String, XmpRecord> propertyMap;

    /**
     * Represents a single immutable XMP metadata property.
     *
     * <p>
     * Each record stores the property's namespace URI, qualified path, local name, namespace
     * prefix, and associated value.
     * </p>
     */
    public static final class XmpRecord
    {
        private static final Pattern QUALIFIED_PATH_PATTERN = Pattern.compile("^\\s*(\\w+):(.+)$");
        private final String namespace;
        private final String path;
        private final String value;
        private final String prefix;
        private final String name;

        /**
         * Creates an immutable XMP property record.
         *
         * @param namespace
         *        the namespace URI associated with the property
         * @param path
         *        the qualified property path (for example, {@code dc:creator})
         * @param value
         *        the property value
         */
        public XmpRecord(String namespace, String path, String value)
        {
            this.path = Objects.requireNonNull(path, "Path cannot be null").trim();
            this.value = (value != null) ? value.trim() : "";
            this.namespace = (namespace != null) ? namespace.trim() : "";

            Matcher matcher = QUALIFIED_PATH_PATTERN.matcher(this.path);

            if (matcher.matches())
            {
                this.prefix = matcher.group(1);
                this.name = matcher.group(2);
            }

            else
            {
                this.prefix = "";
                this.name = this.path;
            }
        }

        /**
         * Returns the namespace URI associated with this property.
         *
         * @return the namespace URI
         */
        public String getNamespace()
        {
            return namespace;
        }

        /**
         * Returns the qualified property path.
         *
         * <p>
         * The path includes the namespace prefix, for example {@code dc:creator}.
         * </p>
         *
         * @return the qualified property path
         */
        public String getQualifiedPath()
        {
            return path;
        }

        /**
         * Returns the namespace prefix extracted from the qualified path.
         *
         * @return the namespace prefix, or an empty string if none exists
         */
        public String getPrefix()
        {
            return prefix;
        }

        /**
         * Returns the local property name.
         *
         * @return the local property name
         */
        public String getName()
        {
            return name;
        }

        /**
         * Returns the raw property value.
         *
         * @return the property value
         */
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

            return Objects.equals(namespace, other.namespace)
                    && Objects.equals(path, other.path)
                    && Objects.equals(value, other.value);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(namespace, path, value);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(MetadataConstants.FORMATTER, "Namespace", getNamespace()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Qualifier Path", getQualifiedPath()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Prefix", getPrefix()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Name", getName()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Raw Value", getValue()));

            return sb.toString();
        }
    }

    /**
     * Creates an empty XMP directory.
     */
    public XmpDirectory()
    {
        this.propertyMap = new LinkedHashMap<>();
    }

    /**
     * Retrieves an XMP property using its qualified path.
     *
     * <p>
     * Qualified paths include the namespace prefix, for example {@code dc:creator}.
     * </p>
     *
     * @param qualifiedPath
     *        the qualified property path
     * @return an {@link Optional} containing the matching property, or {@link Optional#empty()} if
     *         no matching property exists
     */
    public Optional<XmpRecord> getRecord(String qualifiedPath)
    {
        if (qualifiedPath == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(propertyMap.get(qualifiedPath.trim()));
    }

    /**
     * Retrieves the value associated with the specified XMP property.
     *
     * @param prop
     *        the XMP property identifier
     * @return an {@link Optional} containing the property value, or {@link Optional#empty()} if the
     *         property is unavailable
     */
    public Optional<String> getValueByPath(XmpProperty prop)
    {
        if (prop == null)
        {
            return Optional.empty();
        }

        return getRecord(prop.getQualifiedPath()).map(XmpRecord::getValue);
    }

    /**
     * Adds or replaces an XMP property.
     *
     * <p>
     * If another property with the same qualified path already exists, it is replaced.
     * </p>
     *
     * @param prop
     *        the property to store
     */
    @Override
    public void add(XmpRecord prop)
    {
        Objects.requireNonNull(prop, "Property cannot be null");
        propertyMap.put(prop.getQualifiedPath(), prop);
    }

    /**
     * Removes the specified property.
     *
     * @param prop
     *        the property to remove
     * @return {@code true} if the property existed and was removed, otherwise {@code false}
     */
    @Override
    public boolean remove(XmpRecord prop)
    {
        Objects.requireNonNull(prop, "Property cannot be null");
        return (propertyMap.remove(prop.getQualifiedPath()) != null);
    }

    /**
     * Determines whether this directory contains the specified property.
     *
     * @param prop
     *        the property to locate
     * @return {@code true} if the property exists, otherwise {@code false}
     */
    @Override
    public boolean contains(XmpRecord prop)
    {
        return (prop != null && propertyMap.containsKey(prop.getQualifiedPath()));
    }

    /**
     * Returns the number of stored XMP properties.
     *
     * @return the property count
     */
    @Override
    public int size()
    {
        return propertyMap.size();
    }

    /**
     * Determines whether this directory is empty.
     *
     * @return {@code true} if no properties are stored, otherwise {@code false}
     */
    @Override
    public boolean isEmpty()
    {
        return propertyMap.isEmpty();
    }

    /**
     * Returns an iterator over the stored XMP properties.
     *
     * @return an iterator in insertion order
     */
    @Override
    public Iterator<XmpRecord> iterator()
    {
        return propertyMap.values().iterator();
    }

    /**
     * Returns a formatted textual representation of all stored XMP properties.
     *
     * @return a formatted metadata listing
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
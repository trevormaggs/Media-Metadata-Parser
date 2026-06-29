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
 * Stores a collection of {@link XmpRecord} objects representing XMP metadata.
 * Refactored to support metadata value translation and flexible path query profiles.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 28 June 2026
 */
public final class XmpDirectory implements Directory<XmpDirectory.XmpRecord>
{
    private final Map<String, XmpRecord> propertyMap;

    /**
     * Represents a single and immutable XMP property record.
     */
    public static final class XmpRecord
    {
        private static final Pattern REGEX_PATH = Pattern.compile("^\\s*(\\w+):(.+)$");
        private final String namespace;
        private final String path;
        private final String value;
        private final String prefix;
        private final String name;

        public XmpRecord(String namespace, String path, String value)
        {
            this.path = Objects.requireNonNull(path, "Path cannot be null").trim();
            this.value = (value != null) ? value.trim() : "";
            this.namespace = (namespace != null) ? namespace.trim() : "";

            Matcher matcher = REGEX_PATH.matcher(this.path);

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

        /** @return the namespace URI of the property */
        public String getNamespace()
        {
            return namespace;
        }

        /** @return the path of the property (e.g., dc:creator) */
        public String getQualifierPath()
        {
            return path;
        }

        /** @return the short namespace identifier identifier string */
        public String getPrefix()
        {
            return prefix;
        }

        /** @return the isolated local property tag identifier name */
        public String getName()
        {
            return name;
        }

        /** @return the raw string value of the property */
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
            sb.append(String.format(MetadataConstants.FORMATTER, "Qualifier Path", getQualifierPath()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Prefix", getPrefix()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Name", getName()));
            sb.append(String.format(MetadataConstants.FORMATTER, "Raw Value", getValue()));

            return sb.toString();
        }
    }

    public XmpDirectory()
    {
        this.propertyMap = new LinkedHashMap<>();
    }

    /**
     * Looks up an XMP property by its raw qualified path string (e.g., "dc:creator").
     *
     * @param qualifiedPath
     *        The path string key
     * @return An Optional containing the matching record
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
     * Retrieves the raw string value corresponding to the specified property wrapper.
     *
     * @param prop
     *        an XmpProperty instance containing a fully qualified path key
     * @return an Optional containing the value found; {@link Optional#empty()} otherwise
     */
    public Optional<String> getValueByPath(XmpProperty prop)
    {
        if (prop == null)
        {
            return Optional.empty();
        }

        return getRecord(prop.getQualifiedPath()).map(XmpRecord::getValue);
    }



    @Override
    public void add(XmpRecord prop)
    {
        Objects.requireNonNull(prop, "Property cannot be null");
        propertyMap.put(prop.getQualifierPath(), prop);
    }

    @Override
    public boolean remove(XmpRecord prop)
    {
        Objects.requireNonNull(prop, "Property cannot be null");
        return (propertyMap.remove(prop.getQualifierPath()) != null);
    }

    @Override
    public boolean contains(XmpRecord prop)
    {
        return (prop != null && propertyMap.containsKey(prop.getQualifierPath()));
    }

    @Override
    public int size()
    {
        return propertyMap.size();
    }

    @Override
    public boolean isEmpty()
    {
        return propertyMap.isEmpty();
    }

    @Override
    public Iterator<XmpRecord> iterator()
    {
        return propertyMap.values().iterator();
    }

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
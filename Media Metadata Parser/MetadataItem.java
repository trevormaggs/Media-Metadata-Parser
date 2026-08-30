package gui;

/**
 * Represents a generic key-value metadata item for GUI display.
 */
public class MetadataItem
{
    private final String key;
    private final String value;

    public MetadataItem(String key, String value)
    {
        this.key = key;
        this.value = value;
    }

    public String getKey()
    {
        return key;
    }

    public String getValue()
    {
        return value;
    }

    public String getTagName()
    {
        return key;
    }

    public String getTagValue()
    {
        return value;
    }
}
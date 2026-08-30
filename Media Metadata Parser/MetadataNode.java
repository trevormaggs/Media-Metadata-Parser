package gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a single node entry in the Metadata TreeTableView. Can represent either a category
 * group header or a key-value tag entry.
 */
public class MetadataNode
{
    private final StringProperty name;
    private final StringProperty value;
    private final StringProperty group;

    /**
     * Constructor for Category/Group header nodes (e.g., "[System]", "[Exif SubIFD]").
     *
     * @param groupName
     *        the display name of the group
     */
    public MetadataNode(String groupName)
    {
        this.name = new SimpleStringProperty(groupName);
        this.value = new SimpleStringProperty("");
        this.group = new SimpleStringProperty("Group Header");
    }

    /**
     * Constructor for individual metadata tag entries.
     *
     * @param name
     *        the metadata tag name (e.g., "FileName", "Exposure Time")
     * @param value
     *        the extracted metadata value (e.g., "babygemma.tif", "1/180")
     * @param group
     *        the metadata group category it belongs to (e.g., "System", "Exif SubIFD")
     */
    public MetadataNode(String name, String value, String group)
    {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.group = new SimpleStringProperty(group);
    }

    /* --- Property Getters --- */

    public StringProperty nameProperty()
    {
        return name;
    }

    public StringProperty valueProperty()
    {
        return value;
    }

    public StringProperty groupProperty()
    {
        return group;
    }

    /* --- Standard Value Getters & Setters --- */
    public void setName(String name)
    {
        this.name.set(name);
    }

    public String getName()
    {
        return name.get();
    }

    public void setValue(String value)
    {
        this.value.set(value);
    }

    public String getValue()
    {
        return value.get();
    }

    public void setGroup(String group)
    {
        this.group.set(group);
    }

    public String getGroup()
    {
        return group.get();
    }
}
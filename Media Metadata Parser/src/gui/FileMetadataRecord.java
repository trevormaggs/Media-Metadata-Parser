package gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileMetadataRecord
{
    private Path filePath;
    private String fileName;
    private final List<MetadataDirectory> directories = new ArrayList<>();

    public FileMetadataRecord()
    {
    }

    public FileMetadataRecord(Path filePath)
    {
        this.filePath = filePath;
        if (filePath != null && filePath.getFileName() != null)
        {
            this.fileName = filePath.getFileName().toString();
        }
    }

    public Path getFilePath()
    {
        return filePath;
    }

    public void setFilePath(Path filePath)
    {
        this.filePath = filePath;
        if (filePath != null && filePath.getFileName() != null)
        {
            this.fileName = filePath.getFileName().toString();
        }
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public List<MetadataDirectory> getDirectories()
    {
        return directories;
    }

    public void addDirectory(MetadataDirectory directory)
    {
        directories.add(directory);
    }

    public List<String> getGroups()
    {
        List<String> groupNames = new ArrayList<>();
        for (MetadataDirectory dir : directories)
        {
            if (dir.getName() != null && !groupNames.contains(dir.getName()))
            {
                groupNames.add(dir.getName());
            }
        }
        return groupNames;
    }

    /**
     * Returns List of the top-level MetadataItem objects for the requested group.
     */
    public List<MetadataItem> getItemsForGroup(String groupName)
    {
        if (groupName == null)
        {
            return Collections.emptyList();
        }

        for (MetadataDirectory dir : directories)
        {
            if (groupName.equalsIgnoreCase(dir.getName()))
            {
                List<MetadataItem> items = new ArrayList<>();
                for (MetadataTag tag : dir.getTags())
                {
                    items.add(new MetadataItem(tag.getTagName(), tag.getTagValue()));
                }
                return items;
            }
        }
        return Collections.emptyList();
    }

    public void addItem(String category, String tagName, String value)
    {
        MetadataDirectory dir = findOrCreateDirectory(category);
        dir.addTag(new MetadataTag(tagName, value));
    }

    private MetadataDirectory findOrCreateDirectory(String categoryName)
    {
        String name = (categoryName != null && !categoryName.trim().isEmpty()) ? categoryName : "General";
        for (MetadataDirectory dir : directories)
        {
            if (dir.getName().equalsIgnoreCase(name))
            {
                return dir;
            }
        }
        MetadataDirectory newDir = new MetadataDirectory(name);
        directories.add(newDir);
        return newDir;
    }

    public String toRawText()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(fileName != null ? fileName : "Unknown").append("\n");

        for (MetadataDirectory dir : directories)
        {
            sb.append("  [").append(dir.getName()).append("]\n");
            for (MetadataTag tag : dir.getTags())
            {
                sb.append("    ").append(tag.getTagName())
                  .append(" : ").append(tag.getTagValue()).append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public static class MetadataDirectory
    {
        private String name;
        private final List<MetadataTag> tags = new ArrayList<>();

        public MetadataDirectory(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public List<MetadataTag> getTags()
        {
            return tags;
        }

        public void addTag(MetadataTag tag)
        {
            tags.add(tag);
        }
    }

    public static class MetadataTag
    {
        private String tagName;
        private String tagValue;

        public MetadataTag(String tagName, String tagValue)
        {
            this.tagName = tagName;
            this.tagValue = tagValue;
        }

        public String getTagName()
        {
            return tagName;
        }

        public String getTagValue()
        {
            return tagValue;
        }
    }
}
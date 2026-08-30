package gui;

import common.Directory;
import common.Metadata;
import java.nio.file.Path;

/**
 * Pure POJO representing a media file and its extracted metadata container.
 */
public class FileMetadataRecord
{
    private String fileName;
    private Metadata<? extends Directory<?>> metadata;

    public FileMetadataRecord(Path fpath)
    {
        setFilePath(fpath);
    }

    public FileMetadataRecord(Path fpath, Metadata<? extends Directory<?>> metadata)
    {
        this(fpath);
        this.metadata = metadata;
    }

    public void setFilePath(Path fpath)
    {
        if (fpath != null && fpath.getFileName() != null)
        {
            fileName = fpath.getFileName().toString();
        }

        else
        {
            fileName = null;
        }
    }

    public String getFileName()
    {
        return fileName;
    }

    public Metadata<? extends Directory<?>> getMetadata()
    {
        return metadata;
    }
}
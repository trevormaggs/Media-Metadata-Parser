package gui.oldgui;

import common.PropertyListener;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Table model for displaying batch processing status per file.
 */
public class FileRecord implements PropertyListener
{
    private final SimpleStringProperty sourceName;
    private final SimpleStringProperty targetName;
    private final SimpleStringProperty status;
    private final SimpleLongProperty fileSize;

    public FileRecord(String sourceName, String targetName, String status, long fileSize)
    {
        this.sourceName = new SimpleStringProperty(sourceName);
        this.targetName = new SimpleStringProperty(targetName);
        this.status = new SimpleStringProperty(status);
        this.fileSize = new SimpleLongProperty(fileSize);
    }

    @Override
    public void accept(String key, Object value)
    {
        if (key == null || value == null)
        {
            return;
        }

        switch (key.toUpperCase())
        {
            case MediaMetadataApp.KEY_SOURCE:
                sourceName.set(String.valueOf(value));
                break;

            case MediaMetadataApp.KEY_TARGET:
                targetName.set(String.valueOf(value));
                break;

            case MediaMetadataApp.KEY_STATUS:
                status.set(String.valueOf(value));
                break;

            case MediaMetadataApp.KEY_SIZE:
                
                if (value instanceof Number)
                {
                    fileSize.set(((Number) value).longValue());
                }
                
                else
                {
                    try
                    {
                        fileSize.set(Long.parseLong(String.valueOf(value)));
                    }
                    
                    catch (NumberFormatException exc)
                    {
                        // Ignore invalid format
                    }
                }
                
                break;
        }
    }

    // --- Property Getters for JavaFX TableView ---

    public SimpleStringProperty sourceNameProperty()
    {
        return sourceName;
    }

    public SimpleStringProperty targetNameProperty()
    {
        return targetName;
    }

    public SimpleStringProperty statusProperty()
    {
        return status;
    }

    SimpleLongProperty fileSizeProperty()
    {
        return fileSize;
    }

    // --- Standard Value Getters & Setters ---

    String getSourceName()
    {
        return sourceName.get();
    }

    void setSourceName(String src)
    {
        sourceName.set(src);
    }

    String getTargetName()
    {
        return targetName.get();
    }

    void setTargetName(String tgt)
    {
        targetName.set(tgt);
    }

    String getStatus()
    {
        return status.get();
    }

    void setStatus(String sts)
    {
        status.set(sts);
    }

    long getFileSize()
    {
        return fileSize.get();
    }

    void setFileSize(long fsize)
    {
        fileSize.set(fsize);
    }
}
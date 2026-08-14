package gui;

import common.PropertyListener;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Table model for displaying batch processing status per file.
 */
class FileSummaryRecord implements PropertyListener
{
    static final String KEY_SOURCE = "SOURCE";
    static final String KEY_TARGET = "TARGET";
    static final String KEY_STATUS = "STATUS";
    static final String KEY_SIZE = "SIZE";
    private final SimpleStringProperty sourceName;
    private final SimpleStringProperty targetName;
    private final SimpleStringProperty status;
    private final SimpleLongProperty fileSize;

    FileSummaryRecord()
    {
        this("", "", "", 0L);
    }

    FileSummaryRecord(String sourceName, String targetName, String status, long fileSize)
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
            case KEY_SOURCE:
                sourceName.set(String.valueOf(value));
            break;

            case KEY_TARGET:
                targetName.set(String.valueOf(value));
            break;

            case KEY_STATUS:
                status.set(String.valueOf(value));
            break;

            case KEY_SIZE:
                
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

    SimpleStringProperty sourceNameProperty()
    {
        return sourceName;
    }

    SimpleStringProperty targetNameProperty()
    {
        return targetName;
    }

    SimpleStringProperty statusProperty()
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
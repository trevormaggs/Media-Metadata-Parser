package gui;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import common.DigitalSignature;
import common.PropertyConsumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Table model for displaying batch processing status per file.
 */
class FileProcessingRecord implements PropertyConsumer
{
    static final String KEY_SOURCE = "SOURCE";
    static final String KEY_TARGET = "TARGET";
    static final String KEY_MAGIC = "MAGIC";
    static final String KEY_STATUS = "STATUS";
    static final String KEY_SIZE = "SIZE";

    private final SimpleStringProperty sourceName;
    private final SimpleStringProperty targetName;
    private final ObjectProperty<DigitalSignature> digitalSignature;
    private final SimpleStringProperty status;
    private final SimpleLongProperty fileSize;

    FileProcessingRecord()
    {
        this("", "", DigitalSignature.UNKNOWN, "", 0L);
    }

    FileProcessingRecord(String sourceName, String targetName, DigitalSignature magic, String status, long fileSize)
    {
        this.sourceName = new SimpleStringProperty(sourceName);
        this.targetName = new SimpleStringProperty(targetName);
        this.digitalSignature = new SimpleObjectProperty<>(magic != null ? magic : DigitalSignature.UNKNOWN);
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

            case KEY_MAGIC:
                if (value instanceof DigitalSignature)
                {
                    digitalSignature.set((DigitalSignature) value);
                }
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

    ObjectProperty<DigitalSignature> digitalSignatureProperty()
    {
        return digitalSignature;
    }

    SimpleStringProperty statusProperty()
    {
        return status;
    }

    SimpleLongProperty fileSizeProperty()
    {
        return fileSize;
    }

    // POJO Setters

    void setSourceName(String src)
    {
        sourceName.set(src);
    }

    void setTargetName(String tgt)
    {
        targetName.set(tgt);
    }

    void setDigitalSignature(DigitalSignature signature)
    {
        digitalSignature.set(signature != null ? signature : DigitalSignature.UNKNOWN);
    }

    void setStatus(String sts)
    {
        status.set(sts);
    }

    void setFileSize(long fsize)
    {
        fileSize.set(fsize);
    }

    // POJO Getters

    String getSourceName()
    {
        return sourceName.get();
    }

    String getTargetName()
    {
        return targetName.get();
    }

    DigitalSignature getDigitalSignature()
    {
        return digitalSignature.get();
    }

    String getStatus()
    {
        return status.get();
    }

    long getFileSize()
    {
        return fileSize.get();
    }

    /**
     * Resolves and returns the source string as a {@link Path} resource.
     *
     * @return the resolved source {@link Path}, or {@code null} if empty or invalid
     */
    Path getSourcePath()
    {
        return toPath(getSourceName());
    }

    /**
     * Resolves and returns the target string as a {@link Path} resource.
     *
     * @return the resolved target {@link Path}, or {@code null} if empty or invalid
     */
    Path getTargetPath()
    {
        return toPath(getTargetName());
    }

    /**
     * Helper method to safely convert path strings into {@link Path} objects.
     */
    private Path toPath(String rawPath)
    {
        if (rawPath == null || rawPath.trim().isEmpty())
        {
            return null;
        }

        try
        {
            return Paths.get(rawPath);
        }

        catch (InvalidPathException exc)
        {
            return null;
        }
    }
}
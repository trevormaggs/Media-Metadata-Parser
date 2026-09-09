package gui;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import common.DigitalSignature;
import common.PropertyConsumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Table model representing the processing status of an individual file.
 */
class ProcessedFileRecord implements PropertyConsumer
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

    /**
     * Creates an empty file processing record with default values.
     */
    ProcessedFileRecord()
    {
        this("", "", DigitalSignature.UNKNOWN, "", 0L);
    }

    /**
     * Creates a file processing record with the specified values.
     *
     * @param sourceName
     *        the source file name or path
     * @param targetName
     *        the target file name or path
     * @param magic
     *        the digital signature identifying the file type
     * @param status
     *        the current processing status
     * @param fileSize
     *        the file size in bytes
     */
    ProcessedFileRecord(String sourceName, String targetName, DigitalSignature magic, String status,
            long fileSize)
    {
        this.sourceName = new SimpleStringProperty(sourceName);
        this.targetName = new SimpleStringProperty(targetName);
        this.digitalSignature = new SimpleObjectProperty<>(
                magic != null ? magic : DigitalSignature.UNKNOWN);
        this.status = new SimpleStringProperty(status);
        this.fileSize = new SimpleLongProperty(fileSize);
    }

    /**
     * Updates the property identified by the specified key.
     *
     * <p>
     * Values that are {@code null} or cannot be converted to the expected property type are
     * ignored.
     * </p>
     *
     * @param key
     *        the property key identifying the value to update
     * @param value
     *        the new property value
     */
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

    /**
     * Returns the JavaFX property containing the source file name or path.
     *
     * @return the source name property
     */
    SimpleStringProperty sourceNameProperty()
    {
        return sourceName;
    }

    /**
     * Returns the JavaFX property containing the target file name or path.
     *
     * @return the target name property
     */
    SimpleStringProperty targetNameProperty()
    {
        return targetName;
    }

    /**
     * Returns the JavaFX property containing the file's digital signature.
     *
     * @return the digital signature property
     */
    ObjectProperty<DigitalSignature> digitalSignatureProperty()
    {
        return digitalSignature;
    }

    /**
     * Returns the JavaFX property containing the current processing status.
     *
     * @return the status property
     */
    SimpleStringProperty statusProperty()
    {
        return status;
    }

    /**
     * Returns the JavaFX property containing the file size.
     *
     * @return the file size property
     */
    SimpleLongProperty fileSizeProperty()
    {
        return fileSize;
    }

    /**
     * Sets the source file name or path.
     *
     * @param src
     *        the source file name or path
     */
    void setSourceName(String src)
    {
        sourceName.set(src);
    }

    /**
     * Sets the target file name or path.
     *
     * @param tgt
     *        the target file name or path
     */
    void setTargetName(String tgt)
    {
        targetName.set(tgt);
    }

    /**
     * Sets the file's digital signature.
     *
     * <p>
     * A {@code null} signature is replaced with {@link DigitalSignature#UNKNOWN}.
     * </p>
     *
     * @param sig
     *        the digital signature to set
     */
    void setDigitalSignature(DigitalSignature sig)
    {
        digitalSignature.set(sig != null ? sig : DigitalSignature.UNKNOWN);
    }

    /**
     * Sets the current processing status.
     *
     * @param sts
     *        the processing status
     */
    void setStatus(String sts)
    {
        status.set(sts);
    }

    /**
     * Sets the file size.
     *
     * @param fsize
     *        the file size in bytes
     */
    void setFileSize(long fsize)
    {
        fileSize.set(fsize);
    }

    /**
     * Returns the source file name or path.
     *
     * @return the source file name or path
     */
    String getSourceName()
    {
        return sourceName.get();
    }

    /**
     * Returns the target file name or path.
     *
     * @return the target file name or path
     */
    String getTargetName()
    {
        return targetName.get();
    }

    /**
     * Returns the file's digital signature.
     *
     * @return the digital signature
     */
    DigitalSignature getDigitalSignature()
    {
        return digitalSignature.get();
    }

    /**
     * Returns the current processing status.
     *
     * @return the processing status
     */
    String getStatus()
    {
        return status.get();
    }

    /**
     * Returns the file size.
     *
     * @return the file size in bytes
     */
    long getFileSize()
    {
        return fileSize.get();
    }

    /**
     * Returns the source string as a {@link Path}.
     *
     * @return the source {@link Path}, or {@code null} if empty or invalid
     */
    Path getSourcePath()
    {
        return toPath(getSourceName());
    }

    /**
     * Returns the target string as a {@link Path}.
     *
     * @return the target {@link Path}, or {@code null} if empty or invalid
     */
    Path getTargetPath()
    {
        return toPath(getTargetName());
    }

    /**
     * Converts a path string into a {@link Path} object.
     *
     * @param rawPath
     *        the path string to convert
     * @return the resulting {@link Path}, or {@code null} if the string is empty or contains an
     *         invalid path
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
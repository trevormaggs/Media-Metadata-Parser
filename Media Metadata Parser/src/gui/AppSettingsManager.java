package gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/**
 * Manages persistent user configuration settings across application sessions using a simple
 * key-value configuration file.
 */
public final class AppSettingsManager
{
    private static final String CONFIG_FILE_NAME = "app_settings.properties";
    private static final String KEY_SOURCE_PATH = "last.source.path";
    private static final String KEY_TARGET_PATH = "last.target.path";

    private AppSettingsManager()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    private static Path getSettingsPath()
    {
        return Paths.get(System.getProperty("user.home"), CONFIG_FILE_NAME);
    }

    /**
     * Saves the specified source and target text paths to persistent storage.
     *
     * @param sourcePath
     *        the current source text path string
     * @param targetPath
     *        the current target text path string
     */
    public static void saveSettings(String sourcePath, String targetPath)
    {
        Properties props = new Properties();

        if (sourcePath != null && !sourcePath.trim().isEmpty())
        {
            props.setProperty(KEY_SOURCE_PATH, sourcePath.trim());
        }

        if (targetPath != null && !targetPath.trim().isEmpty())
        {
            props.setProperty(KEY_TARGET_PATH, targetPath.trim());
        }

        Path settingsFile = getSettingsPath();

        try (OutputStream os = Files.newOutputStream(settingsFile))
        {
            props.store(os, "Media Metadata App User Settings");
        }

        catch (IOException e)
        {
            System.err.println("Failed to save application settings: " + e.getMessage());
        }
    }

    /**
     * Loads saved settings from persistent storage and updates the specified UI controls.
     *
     * @param sourceText
     *        the source path text field
     * @param targetText
     *        the target path text field
     */
    public static void loadSettings(TextField sourceText, TextField targetText)
    {
        Path settingsFile = getSettingsPath();

        if (!Files.exists(settingsFile))
        {
            return;
        }

        Properties props = new Properties();

        try (InputStream is = Files.newInputStream(settingsFile))
        {
            props.load(is);

            String savedSource = props.getProperty(KEY_SOURCE_PATH);
            String savedTarget = props.getProperty(KEY_TARGET_PATH);

            if (sourceText != null && savedSource != null && !savedSource.isEmpty())
            {
                sourceText.setText(savedSource);
                sourceText.setTooltip(new Tooltip(savedSource));

                File sourceFile = new java.io.File(savedSource);

                if (sourceFile.exists())
                {
                    String parentDir = sourceFile.isDirectory() ? sourceFile.getAbsolutePath() : sourceFile.getParent();
                    sourceText.setUserData(parentDir);
                }
            }

            if (targetText != null && savedTarget != null && !savedTarget.isEmpty())
            {
                targetText.setText(savedTarget);
                targetText.setTooltip(new Tooltip(savedTarget));
            }
        }
        catch (IOException e)
        {
            System.err.println("Failed to load application settings: " + e.getMessage());
        }
    }
}
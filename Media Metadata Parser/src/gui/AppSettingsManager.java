package gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
    private static final String KEY_SOURCE_PARENT_PATH = "last.source.parent.path";
    private static final String KEY_TARGET_PATH = "last.target.path";
    private static final String KEY_RECENT_PREFIX = "recent.source.path.";
    private static final int MAX_RECENT_ENTRIES = 5;

    private AppSettingsManager()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    private static Path getSettingsPath()
    {
        return Paths.get(System.getProperty("user.home"), CONFIG_FILE_NAME);
    }

    /**
     * Pushes a new source path into the recent paths collection, placing it at the front and
     * truncating old entries past the maximum limit. If the path points to a file, its parent
     * directory is extracted and stored instead.
     *
     * @param newPath
     *        the newly selected or executed source path
     */
    public static void addRecentSourcePath(String newPath)
    {
        if (newPath == null || newPath.trim().isEmpty())
        {
            return;
        }

        String cleanPath = newPath.trim();

        /* If the path points to a file, resolve its parent directory for history */
        try
        {
            Path path = Paths.get(cleanPath);

            if (Files.exists(path))
            {
                if (Files.isRegularFile(path))
                {
                    Path parent = path.getParent();
                    if (parent != null)
                    {
                        cleanPath = parent.toAbsolutePath().toString();
                    }
                }
                else if (Files.isDirectory(path))
                {
                    cleanPath = path.toAbsolutePath().toString();
                }
            }
        }
        catch (InvalidPathException exc)
        {
            // Leave cleanPath unchanged if string parsing fails
        }

        List<String> currentHistory = loadRecentSourcePaths();
        List<String> updatedHistory = new ArrayList<>();

        updatedHistory.add(cleanPath);

        for (int i = 0; i < currentHistory.size(); i++)
        {
            String existing = currentHistory.get(i);

            if (!existing.equalsIgnoreCase(cleanPath) && updatedHistory.size() < MAX_RECENT_ENTRIES)
            {
                updatedHistory.add(existing);
            }
        }

        saveRecentHistory(updatedHistory);
    }

    /**
     * Loads the list of recent source paths using indexed property keys.
     *
     * @return a list of recent source path strings
     */
    public static List<String> loadRecentSourcePaths()
    {
        List<String> history = new ArrayList<>();
        Path settingsFile = getSettingsPath();

        if (!Files.exists(settingsFile))
        {
            return history;
        }

        Properties props = new Properties();

        try (InputStream is = Files.newInputStream(settingsFile))
        {
            props.load(is);

            for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
            {
                String path = props.getProperty(KEY_RECENT_PREFIX + i);

                if (path != null && !path.trim().isEmpty())
                {
                    history.add(path.trim());
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("Failed to load recent history: " + e.getMessage());
        }

        return history;
    }

    private static void saveRecentHistory(List<String> history)
    {
        Path settingsFile = getSettingsPath();
        Properties props = new Properties();

        if (Files.exists(settingsFile))
        {
            try (InputStream is = Files.newInputStream(settingsFile))
            {
                props.load(is);
            }

            catch (IOException e)
            {
                // Continue on load failure
            }
        }

        /* Clear old indexed keys to prevent lingering stale entries */
        for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
        {
            props.remove(KEY_RECENT_PREFIX + i);
        }

        /* Write current history list with index markers */
        for (int i = 0; i < history.size(); i++)
        {
            props.setProperty(KEY_RECENT_PREFIX + i, history.get(i));
        }

        try (OutputStream os = Files.newOutputStream(settingsFile))
        {
            props.store(os, "Media Metadata App User Settings");
        }
        catch (IOException e)
        {
            System.err.println("Failed to save recent history: " + e.getMessage());
        }
    }

    /**
     * Saves the source, target, and parent directory metadata paths to maintain persistent storage.
     *
     * @param sourceText
     *        the source path text field
     * @param targetText
     *        the target path text field
     * 
     * @throws IOException
     *         if writing to storage fails
     */
    public static void saveSettings(TextField sourceText, TextField targetText) throws IOException
    {
        String sourceParentPath = null;
        Properties props = new Properties();
        Path historyFile = getSettingsPath();
        Object userData = sourceText.getUserData();
        String sourcePath = sourceText.getText().trim();
        String targetPath = targetText.getText().trim();

        if (userData instanceof String && ((String) userData).trim().length() > 0)
        {
            sourceParentPath = ((String) userData).trim();
        }
        
        else if (!sourcePath.isEmpty())
        {
            try
            {
                /* Derive parent folder on the fly for single file entries */
                Path path = Paths.get(sourcePath);

                if (Files.exists(path))
                {
                    Path parent = Files.isDirectory(path) ? path : path.getParent();

                    if (parent != null)
                    {
                        sourceParentPath = parent.toAbsolutePath().toString();
                    }
                }
            }
            
            catch (InvalidPathException exc)
            {
                // Just pass through
            }
        }

        if (Files.exists(historyFile))
        {
            try (InputStream is = Files.newInputStream(historyFile))
            {
                props.load(is);
            }
        }

        if (sourceParentPath != null && !sourceParentPath.isEmpty())
        {
            props.setProperty(KEY_SOURCE_PARENT_PATH, sourceParentPath);
        }
        
        else
        {
            props.remove(KEY_SOURCE_PARENT_PATH);
        }

        if (!sourcePath.isEmpty())
        {
            props.setProperty(KEY_SOURCE_PATH, sourcePath);
        }
        
        else
        {
            props.remove(KEY_SOURCE_PATH);
        }

        if (!targetPath.isEmpty())
        {
            props.setProperty(KEY_TARGET_PATH, targetPath);
        }
        
        else
        {
            props.remove(KEY_TARGET_PATH);
        }

        try (OutputStream os = Files.newOutputStream(historyFile))
        {
            props.store(os, "Media Metadata App User Settings");
        }
    }

    /**
     * Loads saved settings from persistent storage and updates the specified text fields.
     *
     * @param sourceText
     *        the text field for the source path
     * @param targetText
     *        the text field for the target path
     * 
     * @throws IOException
     *         if reading from storage fails
     */
    public static void loadSettings(TextField sourceText, TextField targetText) throws IOException
    {
        Properties props = new Properties();
        Path historyFile = getSettingsPath();

        if (Files.exists(historyFile))
        {
            try (InputStream is = Files.newInputStream(historyFile))
            {
                props.load(is);

                String savedSourceParent = props.getProperty(KEY_SOURCE_PARENT_PATH);
                String savedSource = props.getProperty(KEY_SOURCE_PATH);
                String savedTarget = props.getProperty(KEY_TARGET_PATH);

                if (savedSource != null && !savedSource.isEmpty())
                {
                    sourceText.setText(savedSource);
                    sourceText.setTooltip(new Tooltip(savedSource));

                    if (savedSourceParent != null && !savedSourceParent.isEmpty())
                    {
                        sourceText.setUserData(savedSourceParent);
                    }

                    else
                    {
                        try
                        {
                            Path sourceFile = Paths.get(savedSource).toAbsolutePath();

                            if (Files.exists(sourceFile))
                            {
                                Path parentDir = Files.isDirectory(sourceFile) ? sourceFile : sourceFile.getParent();

                                if (parentDir != null)
                                {
                                    sourceText.setUserData(parentDir.toString());
                                }
                            }
                        }

                        catch (InvalidPathException exc)
                        {
                            // Just pass through
                        }
                    }
                }

                if (savedTarget != null && !savedTarget.isEmpty())
                {
                    targetText.setText(savedTarget);
                    targetText.setTooltip(new Tooltip(savedTarget));
                }
            }
        }
    }
}
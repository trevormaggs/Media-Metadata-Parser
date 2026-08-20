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
final class PathHistoryStore
{
    private static final String CONFIG_FILE_NAME = "app_settings.properties";
    private static final String KEY_SOURCE_PATH = "last.source.path";
    private static final String KEY_SOURCE_PARENT_PATH = "last.source.parent.path";
    private static final String KEY_TARGET_PATH = "last.target.path";
    private static final String KEY_RECENT_PREFIX = "recent.source.path.";
    private static final int MAX_RECENT_ENTRIES = 5;

    /**
     * Private constructor to prevent direct instantiation of this utility class.
     */
    private PathHistoryStore()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    /**
     * Loads the list of recent source paths using indexed property keys.
     *
     * @return a {@link List} of recent source path strings ordered from most to least recent
     */
    static List<String> loadRecentSourcePaths()
    {
        Path history = getSettingsPath();
        List<String> historyConfig = new ArrayList<>();

        if (Files.exists(history))
        {
            Properties props = new Properties();

            try (InputStream is = Files.newInputStream(history))
            {
                props.load(is);

                for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
                {
                    String path = props.getProperty(KEY_RECENT_PREFIX + i, "").trim();

                    if (!path.isEmpty())
                    {
                        historyConfig.add(path);
                    }
                }
            }

            catch (IOException e)
            {
                System.err.println("Failed to load recent history: " + e.getMessage());
            }
        }

        return historyConfig;
    }

    /**
     * Saves the current source and target paths and updates the recent source path history.
     *
     * @param sourceText
     *        the text field containing the current source path
     * @param targetText
     *        the text field containing the current target path
     *
     * @throws IOException
     *         if an I/O error occurs while loading or saving the settings
     */
    static void saveSettings(TextField sourceText, TextField targetText) throws IOException
    {
        Path sourceParentPath = null;
        Properties props = new Properties();
        Path history = getSettingsPath();
        Object userData = sourceText.getUserData();
        String sourcePath = sourceText.getText().trim();
        String targetPath = targetText.getText().trim();

        if (userData instanceof Path)
        {
            sourceParentPath = ((Path) userData);
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
                        sourceParentPath = parent.toAbsolutePath();
                    }
                }
            }

            catch (InvalidPathException exc)
            {
                // Just pass through
            }
        }

        String entry = (sourceParentPath != null && !sourceParentPath.toString().isEmpty() ? sourceParentPath.toString() : sourcePath);

        if (Files.exists(history))
        {
            try (InputStream is = Files.newInputStream(history))
            {
                props.load(is);
            }
        }

        if (sourceParentPath == null)
        {
            props.remove(KEY_SOURCE_PARENT_PATH);
        }

        else
        {
            props.setProperty(KEY_SOURCE_PARENT_PATH, sourceParentPath.toString());
        }

        if (sourcePath.isEmpty())
        {
            props.remove(KEY_SOURCE_PATH);
        }

        else
        {
            props.setProperty(KEY_SOURCE_PATH, sourcePath);
        }

        if (targetPath.isEmpty())
        {
            props.remove(KEY_TARGET_PATH);
        }

        else
        {
            props.setProperty(KEY_TARGET_PATH, targetPath);
        }

        if (!entry.isEmpty())
        {
            updateRecentHistoryInProps(props, entry);
        }

        /* Commit all changes to disk in a single write operation */
        try (OutputStream os = Files.newOutputStream(history))
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
    static void loadSettings(TextField sourceText, TextField targetText) throws IOException
    {
        Path history = getSettingsPath();
        Properties props = new Properties();

        if (Files.exists(history))
        {
            try (InputStream is = Files.newInputStream(history))
            {
                props.load(is);

                String savedSourceParent = props.getProperty(KEY_SOURCE_PARENT_PATH, "").trim();
                String savedSource = props.getProperty(KEY_SOURCE_PATH);
                String savedTarget = props.getProperty(KEY_TARGET_PATH);

                if (savedSource != null && !savedSource.isEmpty())
                {
                    sourceText.setText(savedSource);
                    sourceText.setTooltip(new Tooltip(savedSource));

                    if (!savedSourceParent.isEmpty())
                    {
                        try
                        {
                            sourceText.setUserData(Paths.get(savedSourceParent).toAbsolutePath());
                        }

                        catch (InvalidPathException exc)
                        {
                            // Just pass through and fallback execution below handles path creation
                        }
                    }

                    if (sourceText.getUserData() == null)
                    {
                        try
                        {
                            Path sourceFile = Paths.get(savedSource).toAbsolutePath();

                            if (Files.exists(sourceFile))
                            {
                                Path parentDir = Files.isDirectory(sourceFile) ? sourceFile : sourceFile.getParent();

                                if (parentDir != null)
                                {
                                    sourceText.setUserData(parentDir);
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

    /**
     * Resolves the absolute path to the persistent settings configuration file within the user's
     * home directory.
     *
     * @return the {@link Path} pointing to the configuration properties file
     */
    private static Path getSettingsPath()
    {
        return Paths.get(System.getProperty("user.home"), CONFIG_FILE_NAME);
    }

    /**
     * Prepends a source path to the recent history, removes duplicates, and limits the history to
     * the maximum number of entries.
     *
     * @param props
     *        the properties containing the application settings
     * @param newPath
     *        the source path to add
     */
    private static void updateRecentHistoryInProps(Properties props, String newPath)
    {
        List<String> oldHistory = new ArrayList<>();
        List<String> newHistory = new ArrayList<>();

        for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
        {
            String entry = props.getProperty(KEY_RECENT_PREFIX + i, "").trim();

            if (!entry.isEmpty())
            {
                oldHistory.add(entry);
            }
        }

        newHistory.add(newPath);

        for (String entry : oldHistory)
        {
            if (!entry.equalsIgnoreCase(newPath) && newHistory.size() < MAX_RECENT_ENTRIES)
            {
                newHistory.add(entry);
            }
        }

        // Refresh properties
        for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
        {
            props.remove(KEY_RECENT_PREFIX + i);
        }

        for (int i = 0; i < newHistory.size(); i++)
        {
            props.setProperty(KEY_RECENT_PREFIX + i, newHistory.get(i));
        }
    }
}
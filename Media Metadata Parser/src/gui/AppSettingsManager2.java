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
public final class AppSettingsManager2
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
    private AppSettingsManager2()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
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
     * Loads the list of recent source paths using indexed property keys.
     *
     * @return a {@link List} of recent source path strings ordered from most to least recent
     */
    public static List<String> loadRecentSourcePaths()
    {
        Path historyConfig = getSettingsPath();
        List<String> history = new ArrayList<>();

        if (Files.exists(historyConfig))
        {
            Properties props = new Properties();

            try (InputStream is = Files.newInputStream(historyConfig))
            {
                props.load(is);

                for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
                {
                    String path = props.getProperty(KEY_RECENT_PREFIX + i, "").trim();

                    if (!path.isEmpty())
                    {
                        history.add(path);
                    }
                }
            }

            catch (IOException e)
            {
                System.err.println("Failed to load recent history: " + e.getMessage());
            }
        }

        return history;
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
    public static void saveSettings(TextField sourceText, TextField targetText) throws IOException
    {
        String sourceParentPath = null;
        Properties props = new Properties();
        Path historyConfig = getSettingsPath();
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

        if (Files.exists(historyConfig))
        {
            try (InputStream is = Files.newInputStream(historyConfig))
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

        String entry = (sourceParentPath != null && !sourceParentPath.isEmpty()) ? sourceParentPath : sourcePath;

        if (!entry.isEmpty())
        {
            updateRecentHistoryInProps(props, entry);
        }

        /* Commit all changes to disk in a single write operation */
        try (OutputStream os = Files.newOutputStream(historyConfig))
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
        Path historyConfig = getSettingsPath();

        if (Files.exists(historyConfig))
        {
            try (InputStream is = Files.newInputStream(historyConfig))
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
        List<String> currentHistory = new ArrayList<>();
        List<String> updatedHistory = new ArrayList<>();

        for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
        {
            String entry = props.getProperty(KEY_RECENT_PREFIX + i, "").trim();

            if (!entry.isEmpty())
            {
                currentHistory.add(entry);
            }
        }

        updatedHistory.add(newPath);

        for (String entry : currentHistory)
        {
            if (!entry.equalsIgnoreCase(newPath) && updatedHistory.size() < MAX_RECENT_ENTRIES)
            {
                updatedHistory.add(entry);
            }
        }

        // Refresh properties
        for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
        {
            props.remove(KEY_RECENT_PREFIX + i);
        }

        for (int i = 0; i < updatedHistory.size(); i++)
        {
            props.setProperty(KEY_RECENT_PREFIX + i, updatedHistory.get(i));
        }
    }
}
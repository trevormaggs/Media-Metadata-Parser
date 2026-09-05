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

import batch.BatchErrorException;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/**
 * Manages persistent user configuration settings across application sessions using a simple
 * key-value configuration file.
 * 
 * The settings file is stored in the user's home directory and maintains the most recently used
 * source and target paths, together with a limited history of recent source entries.
 */
final class PathHistoryStore2
{
    private static final String CONFIG_FILE_NAME = "app_settings.properties";
    private static final String KEY_SOURCE_PATH = "last.source.path";
    private static final String KEY_SOURCE_PARENT_PATH = "last.source.parent.path";
    private static final String KEY_TARGET_PATH = "last.target.path";
    private static final String KEY_RECENT_PREFIX = "recent.source.path.";
    private static final int MAX_RECENT_ENTRIES = 5;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException
     *         always thrown when an instance is created
     */
    private PathHistoryStore2()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    /**
     * Loads the recent source path history, up to 5 recent entries, from the persistent settings
     * file.
     *
     * @return a list of recent source path entries, ordered from most recent to oldest
     * 
     * @throws BatchErrorException
     *         if the settings file cannot be read
     */
    static List<String> loadRecentSourcePaths() throws BatchErrorException
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
                    String entry = props.getProperty(KEY_RECENT_PREFIX + i, "");

                    if (!entry.isEmpty())
                    {
                        historyConfig.add(entry);
                    }
                }
            }

            catch (IOException exc)
            {
                throw new BatchErrorException("Failed to read settings file:\n" + exc.getMessage(), exc);
            }
        }

        return historyConfig;
    }

    /**
     * Saves the current source and target paths to the persistent settings file and updates the
     * recent history.
     *
     * When possible, the source's absolute parent directory is determined from the source field's
     * tooltip or from one of its absolute paths. For multiple source files, the parent directory is
     * stored together with the source list using a pipe delimiter.
     * 
     * @param sourceText
     *        the text field containing the source path or paths
     * @param targetText
     *        the text field containing the target path
     * @throws IOException
     *         if the settings file cannot be read or written
     */
    static void saveSettings(TextField sourceText, TextField targetText) throws IOException
    {
        Path sourceParentPath = null;
        Path history = getSettingsPath();
        Properties props = new Properties();
        String sourcePath = sourceText.getText().trim();
        String targetPath = targetText.getText().trim();
        Tooltip sourceTooltip = sourceText.getTooltip();

        if (Files.exists(history))
        {
            try (InputStream is = Files.newInputStream(history))
            {
                props.load(is);
            }
        }

        if (sourceTooltip != null)
        {
            String tooltipText = sourceTooltip.getText();

            if (tooltipText != null && !tooltipText.isEmpty())
            {
                try
                {
                    Path fpath = Paths.get(tooltipText);

                    if (fpath.isAbsolute())
                    {
                        sourceParentPath = (Files.isDirectory(fpath) ? fpath : (fpath.getParent() == null ? fpath.getRoot() : fpath.getParent()));
                    }
                }

                catch (InvalidPathException exc)
                {
                    // Fall back to path token parsing
                }
            }
        }

        if (sourceParentPath == null && !sourcePath.isEmpty())
        {
            String[] parts = sourcePath.split("\\s*,\\s*");

            for (String token : parts)
            {
                try
                {
                    Path fpath = Paths.get(token);

                    if (fpath.isAbsolute())
                    {
                        Path parent = fpath.getParent();
                        sourceParentPath = (Files.isDirectory(fpath) ? fpath : (parent == null ? fpath.getRoot() : parent));
                        break;
                    }
                }

                catch (InvalidPathException exc)
                {
                    // Continue checking subsequent tokens
                }
            }
        }

        if (sourceParentPath == null)
        {
            props.remove(KEY_SOURCE_PARENT_PATH);
        }

        else
        {
            props.setProperty(KEY_SOURCE_PARENT_PATH, sourceParentPath.toAbsolutePath().toString());
        }

        if (targetPath.isEmpty())
        {
            props.remove(KEY_TARGET_PATH);
        }

        else
        {
            props.setProperty(KEY_TARGET_PATH, targetPath);
        }

        if (sourcePath.isEmpty())
        {
            props.remove(KEY_SOURCE_PATH);
        }

        else
        {
            String entry;

            if (sourceParentPath != null && sourcePath.contains(","))
            {
                entry = String.format("%s|%s", sourceParentPath.toAbsolutePath().toString(), sourcePath);
            }

            else
            {
                entry = sourcePath;
            }

            props.setProperty(KEY_SOURCE_PATH, entry);
            updateRecentHistory(props, entry);
        }

        try (OutputStream os = Files.newOutputStream(history))
        {
            props.store(os, "Media Metadata App User Settings");
        }
    }

    /**
     * Loads the previously saved source and target paths into the supplied text fields.
     * 
     * A source entry stored in pipe-delimited form is unpacked so that the source text and its
     * parent directory can be restored separately. The restored parent directory is stored in the
     * source field's tooltip.
     *
     * @param sourceText
     *        the text field into which the saved source path or paths are loaded
     * @param targetText
     *        the text field into which the saved target path is loaded
     *
     * @throws IOException
     *         if the settings file cannot be read
     */
    static void loadSettings(TextField sourceText, TextField targetText) throws IOException
    {
        Path settingsPath = getSettingsPath();

        if (Files.exists(settingsPath))
        {
            Properties props = new Properties();

            try (InputStream is = Files.newInputStream(settingsPath))
            {
                props.load(is);

                String savedSource = props.getProperty(KEY_SOURCE_PATH, "");
                String savedTarget = props.getProperty(KEY_TARGET_PATH, "");
                String savedParent = props.getProperty(KEY_SOURCE_PARENT_PATH, "");

                if (!savedTarget.isEmpty())
                {
                    targetText.setText(savedTarget);
                }

                if (!savedSource.isEmpty())
                {
                    int pos = savedSource.indexOf('|');

                    if (pos >= 0)
                    {
                        savedParent = savedSource.substring(0, pos);
                        savedSource = savedSource.substring(pos + 1);
                    }

                    sourceText.setText(savedSource);

                    if (!savedParent.isEmpty())
                    {
                        sourceText.setTooltip(new Tooltip(savedParent));
                    }
                }
            }
        }
    }

    /**
     * Returns the path of the persistent application settings file.
     *
     * @return the settings file path in the current user's home directory
     */
    private static Path getSettingsPath()
    {
        return Paths.get(System.getProperty("user.home"), CONFIG_FILE_NAME);
    }

    /**
     * Updates the recent source path history by placing the supplied entry at the front and
     * retaining unique existing entries up to the configured maximum.
     *
     * @param props
     *        the properties containing the existing history
     * @param newEntry
     *        the source path entry to place at the front of the history
     */
    private static void updateRecentHistory(Properties props, String newEntry)
    {
        List<String> oldHistory = new ArrayList<>();
        List<String> newHistory = new ArrayList<>();

        for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
        {
            String entry = props.getProperty(KEY_RECENT_PREFIX + i, "");

            if (!entry.isEmpty())
            {
                oldHistory.add(entry);
            }
        }

        newHistory.add(newEntry);

        for (String entry : oldHistory)
        {
            if (!getDisplayText(entry).equalsIgnoreCase(getDisplayText(newEntry)) && newHistory.size() < MAX_RECENT_ENTRIES)
            {
                newHistory.add(entry);
            }
        }

        for (int i = 0; i < MAX_RECENT_ENTRIES; i++)
        {
            props.remove(KEY_RECENT_PREFIX + i);
        }

        for (int i = 0; i < newHistory.size(); i++)
        {
            props.setProperty(KEY_RECENT_PREFIX + i, newHistory.get(i));
        }
    }

    /**
     * Extracts the source display text from a stored history entry.
     * 
     * @param rawEntry
     *        the stored source history entry
     * @return the source text portion of the entry
     */
    private static String getDisplayText(String rawEntry)
    {
        int pos = rawEntry.indexOf('|');
        return (pos != -1 ? rawEntry.substring(pos + 1) : rawEntry);
    }
}
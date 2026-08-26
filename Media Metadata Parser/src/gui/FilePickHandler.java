package gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/**
 * Event handler for launching a JavaFX {@link DirectoryChooser} and populating the selected
 * directory path into a source or target {@link TextField}. Automatically resolves the most
 * appropriate initial directory when opened.
 *
 * <p>
 * <b>Access Restriction:</b> By careful design, this class is intentionally package-private and
 * intended strictly for internal use within the {@code gui} package.
 * </p>
 *
 * @PackagePrivate
 * @author Trevor Maggs
 * @version 1.2
 * @since 6 August 2026
 */
class FilePickHandler implements EventHandler<ActionEvent>
{
    private final String dialogTitle;
    private final TextField targetField;

    /**
     * Constructs a directory popup handler for the specified text field.
     *
     * @param targetField
     *        the text field to populate with the chosen folder path
     * @param dialogTitle
     *        the title for the file chooser dialog
     *
     * @throws NullPointerException
     *         if {@code targetField} is {@code null}
     */
    FilePickHandler(TextField targetField, String dialogTitle)
    {
        this.targetField = Objects.requireNonNull(targetField, "Target text field must not be null");
        this.dialogTitle = (dialogTitle != null ? dialogTitle : "Select Directory");
    }

    /**
     * Handles the action event by displaying a {@link DirectoryChooser} modal dialog. Automatically
     * pre-populates the initial directory based on existing field contents or defaults to the user
     * home directory.
     *
     * @param event
     *        the triggered action event
     */
    @Override
    public void handle(ActionEvent event)
    {
        File openDir = resolveOpeningDirectory();
        DirectoryChooser chooser = new DirectoryChooser();

        chooser.setTitle(dialogTitle);

        if (openDir != null && openDir.isDirectory())
        {
            chooser.setInitialDirectory(openDir);
        }

        Window window = (targetField.getScene() != null ? targetField.getScene().getWindow() : null);
        File selectedFolder = chooser.showDialog(window);

        if (selectedFolder != null)
        {
            String fullpath = selectedFolder.getAbsolutePath();

            targetField.setText(fullpath);
            targetField.setTooltip(new Tooltip(fullpath));
        }
    }

    /**
     * Resolves the directory to open the popup window using the following priority:
     *
     * <ol>
     * <li>Parent path stored in the field's tooltip</li>
     * <li>First valid absolute path token parsed from the text field</li>
     * <li>User's home directory</li>
     * </ol>
     *
     * @return an existing directory {@link File}, or {@code null} if none is found
     */
    private File resolveOpeningDirectory()
    {
        Path parentDir = null;

        if (targetField.getTooltip() != null)
        {
            String tooltipText = targetField.getTooltip().getText();

            if (!tooltipText.isEmpty())
            {
                try
                {
                    Path fpath = Paths.get(tooltipText);
                    parentDir = (Files.isDirectory(fpath) ? fpath : (fpath.getParent() == null ? fpath.getRoot() : fpath.getParent()));
                }

                catch (InvalidPathException exc)
                {
                    // Fall back to field content parsing on invalid path format
                }
            }
        }

        if (parentDir == null || !Files.isDirectory(parentDir))
        {
            String currentPath = targetField.getText().trim();

            if (!currentPath.isEmpty())
            {
                String[] parts = currentPath.split("\\s*,\\s*");

                for (String token : parts)
                {
                    try
                    {
                        Path fpath = Paths.get(token);

                        if (fpath.isAbsolute())
                        {
                            Path parent = (Files.isDirectory(fpath) ? fpath : (fpath.getParent() == null ? fpath.getRoot() : fpath.getParent()));

                            if (parent != null && Files.isDirectory(parent))
                            {
                                parentDir = parent;
                                break;
                            }
                        }
                    }

                    catch (InvalidPathException exc)
                    {
                        // Skip malformed token and check next
                    }
                }
            }
        }

        if (parentDir == null || !Files.isDirectory(parentDir))
        {
            parentDir = Paths.get(System.getProperty("user.home"));
        }

        return (parentDir != null && Files.isDirectory(parentDir) ? parentDir.toFile() : null);
    }
}
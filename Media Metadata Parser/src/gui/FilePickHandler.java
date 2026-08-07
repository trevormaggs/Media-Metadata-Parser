package gui;

import java.io.File;
import java.util.Objects;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/**
 * Event handler for opening a JavaFX {@link DirectoryChooser} and populating the selected directory
 * path into a target {@link TextField}. Automatically resolves the most appropriate initial
 * directory when opened.
 * 
 * <p>
 * <b>Access Restriction:</b> By careful design, this class is intentionally package-private and
 * intended strictly for internal use within the {@code gui} package.
 * </p>
 * 
 * @PackagePrivate
 * @author Trevor Maggs
 * @version 1.1
 * @since 6 August 2026
 */
class FilePickHandler implements EventHandler<ActionEvent>
{
    private final String dialogTitle;
    private final TextField targetField;

    /**
     * Constructs a directory popup handler for a given target text field.
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
     * Handles the action event by displaying a {@link DirectoryChooser} modal dialog.
     * Automatically pre-populates the initial directory based on existing field contents
     * or defaults to the user home directory.
     *
     * @param event
     *        the triggered action event
     */
    @Override
    public void handle(ActionEvent event)
    {
        File openDir = resolveInitialDirectory();
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
            targetField.setText(selectedFolder.getAbsolutePath());
            targetField.setUserData(null);
        }
    }

    /**
     * Resolves the best starting directory for the popup window. The hierarchical priority is based
     * on the order: 1) stored directory in user data, 2) text field path (or its parent),
     * and 3) default user home path.
     *
     * @return a valid existing directory {@link File}, or {@code null}
     */
    private File resolveInitialDirectory()
    {
        File parentDir = null;
        Object userData = targetField.getUserData();

        if (userData instanceof String)
        {
            parentDir = new File((String) userData);
        }
        
        else if (userData instanceof File)
        {
            parentDir = (File) userData;
        }

        if (parentDir != null && parentDir.isDirectory())
        {
            return parentDir;
        }

        String currentPath = targetField.getText().trim();

        if (!currentPath.isEmpty())
        {
            File currentFile = new File(currentPath);

            if (currentFile.isDirectory())
            {
                return currentFile;
            }

            File parent = currentFile.getParentFile();

            if (parent != null && parent.isDirectory())
            {
                return parent;
            }
        }

        File homeDir = new File(System.getProperty("user.home"));

        return homeDir.isDirectory() ? homeDir : null;
    }
}
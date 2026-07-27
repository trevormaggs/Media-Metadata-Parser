package gui;

import java.io.File;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/**
 * Event handler for opening a JavaFX {@link DirectoryChooser} and populating the selected directory
 * path into a target {@link TextField}.
 */
public class DirectoryPopupHandler implements EventHandler<ActionEvent>
{
    private final TextField targetField;
    private final String dialogTitle;

    /**
     * Constructs a directory popup handler for a given target text field.
     *
     * @param targetField
     *        the text field to populate with the chosen folder path
     * @param dialogTitle
     *        the title for the file chooser dialog
     */
    public DirectoryPopupHandler(TextField targetField, String dialogTitle)
    {
        this.targetField = targetField;
        this.dialogTitle = dialogTitle;
    }

    @Override
    public void handle(ActionEvent event)
    {
        if (targetField != null)
        {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(dialogTitle);

            String currentPath = targetField.getText();

            if (currentPath != null && !currentPath.trim().isEmpty())
            {
                File currentFile = new File(currentPath.trim());

                if (currentFile.exists() && currentFile.isDirectory())
                {
                    chooser.setInitialDirectory(currentFile);
                }
            }

            Window window = targetField.getScene() != null ? targetField.getScene().getWindow() : null;
            File selectedFolder = chooser.showDialog(window);

            if (selectedFolder != null)
            {
                targetField.setText(selectedFolder.getAbsolutePath());
            }
        }
    }
}
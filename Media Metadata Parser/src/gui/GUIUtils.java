package gui;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * Provides utility methods for JavaFX user-interface operations, node traversal, and popup dialogs.
 */
final class GUIUtils
{
    private GUIUtils()
    {
        // Private constructor to prevent instantiation
    }

    /**
     * Displays a modal alert dialog to the user.
     *
     * @param owner
     *        the owner {@link Window} for the dialog. It may be {@code null}
     * @param title
     *        the title string for the alert window
     * @param msg
     *        the message content string
     * @param type
     *        the {@link AlertType} defining the severity level
     */
    static void launchPopup(Window owner, String title, String msg, AlertType type)
    {
        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);

        if (owner != null)
        {
            alert.initOwner(owner);
        }

        alert.showAndWait();
    }

    /**
     * Displays a modal alert dialog without specifying an owner window.
     *
     * @param title
     *        the title string for the alert window
     * @param msg
     *        the message content string
     * @param type
     *        the {@link AlertType} defining the severity level
     */
    static void launchPopup(String title, String msg, AlertType type)
    {
        launchPopup(null, title, msg, type);
    }

    /**
     * Recursively searches a JavaFX node hierarchy for a node with the specified ID.
     *
     * @param <T>
     *        the expected node type
     * @param root
     *        the root node from which to begin the search
     * @param id
     *        the target JavaFX ID string
     * @return the matching node cast to type {@code T}, or {@code null} if no matching node is found
     */
    @SuppressWarnings("unchecked")
    static <T extends Node> T getById(Node root, String id)
    {
        if (root != null && id != null)
        {
            if (id.equals(root.getId()))
            {
                return (T) root;
            }

            else if (root instanceof Parent)
            {
                ObservableList<Node> nodes = ((Parent) root).getChildrenUnmodifiable();

                for (int i = 0; i < nodes.size(); i++)
                {
                    T result = getById(nodes.get(i), id);

                    if (result != null)
                    {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Creates a horizontal spacer that expands to fill available space within an {@link HBox}.
     *
     * @return a {@link Region} configured to grow horizontally and fill available space
     */
    static Region fillRow()
    {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
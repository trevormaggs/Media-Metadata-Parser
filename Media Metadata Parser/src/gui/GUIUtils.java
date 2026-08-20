package gui;

import java.util.NoSuchElementException;
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
     * Retrieves a node by ID, verifies its type, and casts it to the expected type.
     *
     * @param <T>
     *        the expected node type
     * @param root
     *        the root node from which to begin the search
     * @param id
     *        the target JavaFX node ID
     * @param type
     *        the expected node type token, for example {@code TextField.class}
     * @return the matching node cast to {@code T}
     *
     * @throws NoSuchElementException
     *         if no node with the specified ID exists
     * @throws IllegalArgumentException
     *         if a node with the specified ID exists but is not an instance of {@code type}
     */
    static <T extends Node> T getById(Node root, String id, Class<T> type)
    {
        Node node = GUIUtils.getById(root, id);

        if (node == null)
        {
            throw new NoSuchElementException("Node ID [" + id + "] not found in the layout hierarchy");
        }

        if (!type.isInstance(node))
        {
            throw new IllegalArgumentException("Node ID [" + id + "] is of type " + node.getClass().getName() + ", but expected " + type.getName());
        }

        return type.cast(node);
    }

    /**
     * Recursively searches the specified JavaFX node hierarchy for a node with the given ID.
     *
     * @param root
     *        the root node from which to begin the search
     * @param id
     *        the JavaFX ID to search for
     * @return the first node whose ID matches {@code id}, or {@code null} if no matching node is
     *         found
     */
    static Node getById(Node root, String id)
    {
        if (root != null && id != null)
        {
            if (id.equals(root.getId()))
            {
                return root;
            }

            if (root instanceof Parent)
            {
                ObservableList<Node> children = ((Parent) root).getChildrenUnmodifiable();

                for (int i = 0; i < children.size(); i++)
                {
                    Node result = getById(children.get(i), id);

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
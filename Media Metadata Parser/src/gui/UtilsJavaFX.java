package gui;

import java.util.NoSuchElementException;
import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Provides utility methods for JavaFX user-interface operations, node traversal, and popup dialogs.
 */
final class UtilsJavaFX
{
    private UtilsJavaFX()
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
        Node node = UtilsJavaFX.getById(root, id);

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

    /**
     * Copy text area contents to system clipboard and trigger visual flash feedback, applying  soft
     * green background highlight visual flash feedback
     *
     * @param logArea
     *        target text field component
     */
    static void doFlashCopyTextArea(final TextArea logArea)
    {
        if (logArea != null && !logArea.getText().isEmpty())
        {
            ClipboardContent content = new ClipboardContent();
            content.putString(logArea.getText());
            Clipboard.getSystemClipboard().setContent(content);

            final String originalStyle = logArea.getStyle();
            logArea.setStyle(originalStyle + " -fx-highlight-fill: #a8e6cf; -fx-highlight-text-fill: #000000;");
            logArea.selectAll();

            PauseTransition flash = new PauseTransition(Duration.millis(550));

            flash.setOnFinished(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    logArea.deselect();
                    logArea.setStyle(originalStyle);
                }
            });

            flash.play();
        }
    }
    
    /**
     * Checks if the given tag description represents a linkable GPS coordinate property.
     *
     * @param name
     *        the metadata tag description
     * @return true if the tag contains latitude or longitude keywords
     */
    static boolean isGpsLocationTag(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lower = name.toLowerCase();

        return lower.contains("latitude") || lower.contains("longitude");
    }
}
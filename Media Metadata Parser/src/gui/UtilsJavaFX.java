package gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Provides utility methods for JavaFX user interface operations, node traversal, and popup dialogs.
 */
final class UtilsJavaFX
{
    private UtilsJavaFX()
    {
        // Private constructor to prevent instantiation
    }

    /**
     * Displays a modal alert dialog to the user, applying the stylesheets from the given owner
     * window to the dialog.
     *
     * @param owner
     *        the owner {@link Window} for the dialog; may be {@code null}
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

            if (owner.getScene() != null)
            {
                ObservableList<String> stylesheets = owner.getScene().getStylesheets();

                for (int i = 0; i < stylesheets.size(); i++)
                {
                    alert.getDialogPane().getStylesheets().add(stylesheets.get(i));
                }
            }
        }

        alert.showAndWait();
    }

    /**
     * Displays a modal alert dialog to the user, using the specified UI node to determine the owner
     * window and theme stylesheets when available.
     *
     * @param targetNode
     *        a UI {@link Node} whose scene and window are used to style and own the dialog when
     *        available
     * @param title
     *        the title string for the alert window
     * @param msg
     *        the message content string
     * @param type
     *        the {@link AlertType} defining the severity level
     */
    static void launchPopup(Node targetNode, String title, String msg, AlertType type)
    {
        Window owner = null;

        if (targetNode != null && targetNode.getScene() != null)
        {
            owner = targetNode.getScene().getWindow();
        }

        launchPopup(owner, title, msg, type);
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
     *        the expected node type token, for example {@code TextField.class}. It must not be
     *        {@code null}
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
     *        the JavaFX node ID to search for
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
     * Processes paste hotkey shortcuts in the source location text field.
     *
     * @param owner
     *        the parent {@link Window} owning the active scene
     * @param event
     *        the triggered key event
     * @param sourceText
     *        the source path text field component
     */
    static void handleSourcePaste(Window owner, KeyEvent event, TextField sourceText)
    {
        KeyCodeCombination shortcut = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);

        if (shortcut.match(event))
        {
            Clipboard clipboard = Clipboard.getSystemClipboard();

            if (clipboard.hasString())
            {
                String pastedText = clipboard.getString().trim();

                if (pastedText.contains(","))
                {
                    // Evaluate multi-file comma-separated list path validity
                    Path parentDir = null;
                    String[] parts = pastedText.split("\\s*,\\s*");

                    for (String token : parts)
                    {
                        try
                        {
                            Path fpath = Paths.get(token).toAbsolutePath();

                            if (Files.isRegularFile(fpath))
                            {
                                parentDir = fpath.getParent();
                                break;
                            }
                        }

                        catch (InvalidPathException exc)
                        {
                            // Ignore invalid path components during initial root discovery
                        }
                    }

                    boolean valid = (parentDir != null);

                    if (valid)
                    {
                        for (String token : parts)
                        {
                            try
                            {
                                Path fpath = parentDir.resolve(token);

                                if (!Files.isRegularFile(fpath) || !parentDir.equals(fpath.getParent()))
                                {
                                    valid = false;
                                    break;
                                }
                            }

                            catch (InvalidPathException exc)
                            {
                                valid = false;
                                break;
                            }
                        }
                    }

                    if (valid)
                    {
                        sourceText.setText(pastedText);
                        sourceText.setTooltip(new Tooltip(pastedText));
                    }

                    else
                    {
                        String msg = "One or more pasted files is unknown or not in the same directory:\n\n" + pastedText;
                        UtilsJavaFX.launchPopup(owner, "Invalid File Set", msg, AlertType.WARNING);
                    }
                }

                else
                {
                    // Evaluate single folder or file target path
                    try
                    {
                        Path fpath = Paths.get(pastedText);

                        if (Files.exists(fpath))
                        {
                            sourceText.setText(pastedText);
                            sourceText.setTooltip(new Tooltip(pastedText));
                        }

                        else
                        {
                            String msg = "The pasted path does not exist:\n\n" + pastedText;
                            UtilsJavaFX.launchPopup(owner, "Invalid Path", msg, AlertType.WARNING);
                        }
                    }

                    catch (InvalidPathException exc)
                    {
                        String msg = "The pasted content is not a valid file path:\n\n" + pastedText;
                        UtilsJavaFX.launchPopup(owner, "Invalid Path", msg, AlertType.WARNING);
                    }
                }
            }

            event.consume();
        }
    }

    /**
     * Prompts a file open selection dialog to capture explicit media files and sets the
     * formatted file list into the source path input component.
     *
     * @param owner
     *        the parent {@link Window} hosting the file chooser dialog
     */
    static void handleFileSelection(Window owner)
    {
        if (owner == null || owner.getScene() == null)
        {
            return;
        }

        TextField sourceText = UtilsJavaFX.getById(owner.getScene().getRoot(), MainViewPane.SRCID, TextField.class);
        if (sourceText == null)
        {
            return;
        }

        String actualText = sourceText.getText().trim();
        File sourceDir = new File(actualText.isEmpty() ? System.getProperty("user.home") : actualText);
        FileChooser chooser = new FileChooser();

        chooser.setTitle("Select Source Files");

        if (sourceDir.isDirectory())
        {
            chooser.setInitialDirectory(sourceDir);
        }

        List<File> files = chooser.showOpenMultipleDialog(owner);

        if (files != null && !files.isEmpty())
        {
            StringJoiner joiner = new StringJoiner(",");

            for (File file : files)
            {
                joiner.add(file.getName());
            }

            String joined = joiner.toString();
            Path parent = files.get(0).toPath().getParent();
            Path commonDir = (parent == null ? files.get(0).toPath().getRoot() : parent);

            sourceText.setText(joined);
            sourceText.setTooltip(new Tooltip(commonDir.toAbsolutePath().toString()));
        }
    }

    /**
     * Copies the text area's contents to the system clipboard and provides temporary visual
     * feedback by highlighting the selected text with a soft green background.
     *
     * @param logArea
     *        the target {@link TextArea}
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
     * Determines whether the specified tag description contains a latitude or longitude keyword.
     *
     * @param name
     *        the metadata tag description
     * @return {@code true} if the description contains {@code "latitude"} or {@code "longitude"},
     *         {@code false} otherwise
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

    /**
     * Diagnostic utility that prints registered ImageIO file extensions and inspects
     * available image reader implementations for TIFF and WebP formats to standard output.
     */
    static void verifyImageIOSupport()
    {
        // Check registered file extensions
        String[] suffixes = ImageIO.getReaderFileSuffixes();

        System.out.println("Registered Suffixes: " + Arrays.toString(suffixes));

        Iterator<ImageReader> tiffReaders = ImageIO.getImageReadersByFormatName("TIFF");

        while (tiffReaders.hasNext())
        {
            ImageReader reader = tiffReaders.next();
            System.out.println("TIFF Reader Class: " + reader.getClass().getName());
        }

        Iterator<ImageReader> webpReaders = ImageIO.getImageReadersByFormatName("WebP");

        while (webpReaders.hasNext())
        {
            ImageReader reader = webpReaders.next();
            System.out.println("WebP Reader Class: " + reader.getClass().getName());
        }
    }
}
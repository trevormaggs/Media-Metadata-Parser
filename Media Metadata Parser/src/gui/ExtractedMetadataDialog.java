package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Modal dialog that presents extracted metadata using both a structured TreeTableView
 * and a flat raw text representation within the original toolbar layout.
 */
public class ExtractedMetadataDialog extends Stage
{
    private final TreeTableView<MetadataNode> treeTableView;
    private final TextArea flatTextArea;
    private final StackPane containerStack;

    public ExtractedMetadataDialog(Stage owner)
    {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Extracted Media Metadata");

        // --- View 1: Structured TreeTableView ---
        treeTableView = new TreeTableView<MetadataNode>();
        treeTableView.setShowRoot(false);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        TreeTableColumn<MetadataNode, String> nameCol = new TreeTableColumn<MetadataNode, String>("Property / Group");
        nameCol.setPrefWidth(250);
        nameCol.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<MetadataNode, String> param)
            {
                if (param != null && param.getValue() != null && param.getValue().getValue() != null)
                {
                    String name = param.getValue().getValue().getName();
                    return new ReadOnlyStringWrapper(name != null ? name : "");
                }
                return new ReadOnlyStringWrapper("");
            }
        });

        TreeTableColumn<MetadataNode, String> valueCol = new TreeTableColumn<MetadataNode, String>("Value");
        valueCol.setPrefWidth(350);
        valueCol.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<MetadataNode, String> param)
            {
                if (param != null && param.getValue() != null && param.getValue().getValue() != null)
                {
                    String value = param.getValue().getValue().getValue();
                    return new ReadOnlyStringWrapper(value != null ? value : "");
                }
                return new ReadOnlyStringWrapper("");
            }
        });

        treeTableView.getColumns().add(nameCol);
        treeTableView.getColumns().add(valueCol);

        // --- View 2: Flat Text Display ---
        flatTextArea = new TextArea();
        flatTextArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        flatTextArea.setEditable(false);

        // StackPane container
        containerStack = new StackPane(treeTableView, flatTextArea);

        // Toggle View Group (Radio Buttons for Flat vs Tree)
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton rbFlat = new RadioButton("Raw Flat Text");
        RadioButton rbTree = new RadioButton("Structured Tree");

        rbFlat.setToggleGroup(toggleGroup);
        rbTree.setToggleGroup(toggleGroup);
        rbTree.setSelected(true);

        // Single Dynamic Expansion Toggle Button
        final boolean[] isExpanded = new boolean[]{true};
        final Button btnToggleExpand = new Button("Collapse All");

        btnToggleExpand.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (isExpanded[0])
                {
                    if (treeTableView.getRoot() != null)
                    {
                        setExpandedRecursive(treeTableView.getRoot(), false);
                    }
                    btnToggleExpand.setText("Expand All");
                    isExpanded[0] = false;
                }
                else
                {
                    if (treeTableView.getRoot() != null)
                    {
                        setExpandedRecursive(treeTableView.getRoot(), true);
                    }
                    btnToggleExpand.setText("Collapse All");
                    isExpanded[0] = true;
                }
            }
        });

        // Initial Visibility
        flatTextArea.setVisible(false);
        treeTableView.setVisible(true);

        // View Switch Listener (hides/shows the toggle button based on active view)
        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue)
            {
                boolean showTree = (newValue == rbTree);

                treeTableView.setVisible(showTree);
                flatTextArea.setVisible(!showTree);
                btnToggleExpand.setVisible(showTree);
            }
        });

        // Top Toolbar Assembly
        HBox toolbarPane = new HBox(12, new Label("View Mode:"), rbTree, rbFlat, new Region(), btnToggleExpand);
        toolbarPane.setAlignment(Pos.CENTER_LEFT);
        toolbarPane.setPadding(new Insets(5, 10, 5, 10));

        // Push expand button to the far right
        HBox.setHgrow(toolbarPane.getChildren().get(3), Priority.ALWAYS);

        // Bottom Action Buttons
        Button btnCopy = new Button("Copy to Clipboard");
        btnCopy.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();

                content.putString(flatTextArea.getText());
                clipboard.setContent(content);
            }
        });

        Button btnExport = new Button("Export to File");
        btnExport.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                exportToFile();
            }
        });

        Button btnClose = new Button("Close");
        btnClose.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                close();
            }
        });

        HBox actionPane = new HBox(10, btnCopy, btnExport, new Region(), btnClose);
        HBox.setHgrow(actionPane.getChildren().get(2), Priority.ALWAYS);
        actionPane.setPadding(new Insets(10, 0, 0, 0));

        VBox rootLayout = new VBox(10, toolbarPane, containerStack, actionPane);
        rootLayout.setPadding(new Insets(10));

        VBox.setVgrow(containerStack, Priority.ALWAYS);
        setScene(new Scene(rootLayout, 800, 550));
    }

    /**
     * Sets raw flat output text into the preview component.
     */
    public void setMetadataText(String rawOutput)
    {
        flatTextArea.setText(rawOutput != null ? rawOutput : "");
    }

    /**
     * Converts a list of FileMetadataRecord POJOs into a hierarchical TreeItem root structure.
     */
    public void setMetadataRecords(List<FileMetadataRecord> records)
    {
        TreeItem<MetadataNode> rootItem = new TreeItem<MetadataNode>(new MetadataNode("Root", ""));

        if (records != null)
        {
            for (FileMetadataRecord record : records)
            {
                // Level 1: File Root Node
                String fileName = record.getFilePath() != null ? record.getFilePath().getFileName().toString() : "Unknown File";
                TreeItem<MetadataNode> fileNode = new TreeItem<MetadataNode>(new MetadataNode(fileName, ""));
                fileNode.setExpanded(true);

                // Populate categories and key/value items from POJO
                for (String group : record.getGroups())
                {
                    // Level 2: Metadata Group (e.g., [System], [EXIF], [Photoshop])
                    TreeItem<MetadataNode> groupNode = new TreeItem<MetadataNode>(new MetadataNode("[" + group + "]", ""));
                    groupNode.setExpanded(true);

                    // Referenced directly as top-level MetadataItem
                    List<MetadataItem> items = record.getItemsForGroup(group);
                    if (items != null)
                    {
                        for (MetadataItem item : items)
                        {
                            // Level 3: Individual Tag Name / Value Pairs
                            TreeItem<MetadataNode> leaf = new TreeItem<MetadataNode>(
                                new MetadataNode(item.getTagName(), item.getValue())
                            );
                            groupNode.getChildren().add(leaf);
                        }
                    }

                    fileNode.getChildren().add(groupNode);
                }

                rootItem.getChildren().add(fileNode);
            }
        }

        treeTableView.setRoot(rootItem);
    }

    /**
     * Recursively updates the expanded state of TreeItems without collapsing the hidden root.
     *
     * @param item the target TreeItem
     * @param expanded true to expand, false to collapse
     */
    private void setExpandedRecursive(TreeItem<?> item, boolean expanded)
    {
        if (item == null)
        {
            return;
        }

        // Keep the hidden root expanded so child rows remain visible when collapsed
        if (item != treeTableView.getRoot())
        {
            item.setExpanded(expanded);
        }
 
        for (TreeItem<?> child : item.getChildren())
        {
            setExpandedRecursive(child, expanded);
        }
    }

    private void exportToFile()
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Metadata File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));

        File file = chooser.showSaveDialog(this);

        if (file != null)
        {
            try (FileWriter writer = new FileWriter(file))
            {
                writer.write(flatTextArea.getText());
            }
            catch (IOException exc)
            {
                System.err.println("Failed to export metadata: " + exc.getMessage());
            }
        }
    }

    /**
     * Inner POJO representing a single row in the TreeTableView.
     */
    public static class MetadataNode
    {
        private final String name;
        private final String value;

        public MetadataNode(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        public String getName()
        {
            return name;
        }

        public String getValue()
        {
            return value;
        }
    }
}
package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import common.Metadata;
import common.PropertyConsumer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Callback;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import tif.DirectoryIFD;
import tif.TifMetadataProvider;
import tif.tagspecs.Taggable;

/**
 * Modal dialog that presents extracted metadata using both a structured TreeTableView
 * and a flat raw text representation within the original toolbar layout.
 */
public class ExtractedMetadataDialog2 extends Stage
{
    private final TextArea flatTextArea;
    private final StackPane containerStack;
    private final TreeTableView<MetadataNode> treeTableView;

    public ExtractedMetadataDialog2(Stage owner)
    {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Extracted Media Metadata");

        // --- View 1: Structured TreeTableView ---
        treeTableView = new TreeTableView<MetadataNode>();
        treeTableView.setShowRoot(false);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        TreeTableColumn<MetadataNode, String> nameCol = new TreeTableColumn<MetadataNode, String>("File / Metadata Group / Property");
        TreeTableColumn<MetadataNode, String> valueCol = new TreeTableColumn<MetadataNode, String>("Value");

        nameCol.setPrefWidth(300);
        nameCol.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<MetadataNode, String> param)
            {
                if (param != null && param.getValue() != null && param.getValue().getValue() != null)
                {
                    TreeItem<MetadataNode> item = param.getValue();
                    MetadataNode node = item.getValue();
                    String name = node.getName();

                    return new ReadOnlyStringWrapper(name != null ? name : "");
                }

                return new ReadOnlyStringWrapper("");
            }
        });

        valueCol.setPrefWidth(300);
        valueCol.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<MetadataNode, String> param)
            {
                if (param != null && param.getValue() != null && param.getValue().getValue() != null)
                {
                    TreeItem<MetadataNode> item = param.getValue();
                    MetadataNode node = item.getValue();
                    String value = node.getValue();

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

        containerStack = new StackPane(treeTableView, flatTextArea);

        // Toggle View Group (Radio Buttons for Flat vs Tree)
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton rbFlat = new RadioButton("Raw Flat Text");
        RadioButton rbTree = new RadioButton("Structured Tree");

        rbFlat.setToggleGroup(toggleGroup);
        rbTree.setToggleGroup(toggleGroup);
        rbTree.setSelected(true);

        // Single Dynamic Expansion Toggle Button
        final Button btnToggleExpand = new Button("Collapse All");

        btnToggleExpand.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                boolean isExpanded = btnToggleExpand.getText().equals("Collapse All");

                if (treeTableView.getRoot() != null)
                {
                    setExpandedRecursive(treeTableView.getRoot(), !isExpanded);
                }

                btnToggleExpand.setText(isExpanded ? "Expand All" : "Collapse All");
            }
        });

        treeTableView.setVisible(true);
        flatTextArea.setVisible(false);
        
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
                String fileName = record.getFileName() != null ? record.getFileName() : "Unknown File";
                TreeItem<MetadataNode> fileNode = new TreeItem<MetadataNode>(new MetadataNode(fileName, ""));
                fileNode.setExpanded(true);

                Metadata<?> meta = record.getMetadata();

                if (meta instanceof TifMetadataProvider)
                {
                    TifMetadataProvider tif = (TifMetadataProvider) meta;

                    for (DirectoryIFD ifd : tif)
                    {
                        // Level 2: Metadata Group (e.g. [IFD0], [ExifIFD])
                        String groupName = "[" + ifd.getDirectoryType().getDescription() + "]";
                        TreeItem<MetadataNode> groupNode = new TreeItem<MetadataNode>(new MetadataNode(groupName, ""));

                        groupNode.setExpanded(true);

                        for (DirectoryIFD.EntryIFD entry : ifd)
                        {
                            Taggable tag = entry.getTag();

                            if (tag != null)
                            {
                                String value = tag.translate(entry.getData());

                                if (!value.isEmpty())
                                {
                                    // Level 3: Key / Value Pairs
                                    groupNode.getChildren().add(new TreeItem<MetadataNode>(new MetadataNode(tag.getDescription(), value)));
                                }
                            }
                        }

                        if (!groupNode.getChildren().isEmpty())
                        {
                            fileNode.getChildren().add(groupNode);
                        }
                    }
                }
                else if (meta instanceof PngMetadataProvider)
                {
                    PngMetadataProvider png = (PngMetadataProvider) meta;
                    final TreeItem<MetadataNode> groupNode = new TreeItem<MetadataNode>(new MetadataNode("[PNG]", ""));

                    groupNode.setExpanded(true);

                    PropertyConsumer consumer = new PropertyConsumer()
                    {
                        @Override
                        public void accept(String key, Object value)
                        {
                            groupNode.getChildren().add(new TreeItem<MetadataNode>(new MetadataNode(key, String.valueOf(value))));
                        }
                    };

                    for (PngDirectory dir : png)
                    {
                        for (PngChunk chunk : dir)
                        {
                            chunk.printProperties(consumer);
                        }
                    }

                    if (!groupNode.getChildren().isEmpty())
                    {
                        fileNode.getChildren().add(groupNode);
                    }
                }

                rootItem.getChildren().add(fileNode);
            }
        }

        treeTableView.setRoot(rootItem);
    }

    /**
     * Recursively updates the expanded state of TreeItems without collapsing the hidden root.
     *
     * @param item
     *        the target TreeItem
     * @param expanded
     *        true to expand, false to collapse
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
    private static class MetadataNode
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
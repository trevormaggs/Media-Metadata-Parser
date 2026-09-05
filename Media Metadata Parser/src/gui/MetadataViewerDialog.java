package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import common.Metadata;
import common.PropertyConsumer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.*;
import javafx.util.Callback;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import tif.DirectoryIFD;
import tif.TifMetadataProvider;
import tif.tagspecs.Taggable;
import util.SystemInfo;

/**
 * Modal dialog for displaying extracted metadata in either a structured tree or flat text format,
 * with optional GPS map visualisation.
 */
class MetadataViewerDialog extends Stage
{
    private final TextArea flatTextArea;
    private final WebView mapView;
    private final RadioButton rbMap;
    private final ComboBox<String> cbGpsFiles;
    private final StackPane containerStack;
    private final ViewManagerGPS gpsMapManager;
    private final TreeTableView<MetadataNode> treeTableView;
    private final TextField txtSearch;
    private TreeItem<MetadataNode> masterRootNode;
    private boolean allItemsExpanded;
    private List<MediaFileMetadata> currentRecords;
    
    /**
     * Constructs a new metadata viewer dialog owned by the specified stage.
     * 
     * @param owner
     *        the parent {@link Stage} for this modal dialog
     */
    MetadataViewerDialog(Stage owner)
    {
        final Button btnExpand = new Button("Collapse All");
        final Button btnCopy = new Button("Copy to Clipboard");
        final Button btnExport = new Button("Export to File");
        final Button btnClose = new Button("Close");
        final RadioButton rbFlat = new RadioButton("Raw Flat Text");
        final RadioButton rbTree = new RadioButton("Structured Tree");

        txtSearch = new TextField();
        txtSearch.setPromptText("Search tags or values...");
        txtSearch.setPrefWidth(180);
        txtSearch.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                filterTree(newValue);
            }
        });

        txtSearch.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                txtSearch.clear();
            }
        });

        mapView = new WebView();
        cbGpsFiles = new ComboBox<>();
        cbGpsFiles.setPromptText("Select GPS File...");
        cbGpsFiles.setVisible(false);
        cbGpsFiles.setManaged(false);
        cbGpsFiles.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                String selectedFile = cbGpsFiles.getValue();

                if (selectedFile != null)
                {
                    gpsMapManager.renderMap(selectedFile);
                }
            }
        });

        // Delegate UI integration and events to ViewManagerGPS
        gpsMapManager = new ViewManagerGPS(mapView);

        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Media Metadata Viewer");

        TreeTableColumn<MetadataNode, String> nameCol = new TreeTableColumn<>("File / Metadata Group / Property");

        nameCol.setPrefWidth(300);
        nameCol.setCellValueFactory(new Callback<CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(CellDataFeatures<MetadataNode, String> param)
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

        TreeTableColumn<MetadataNode, String> valueCol = new TreeTableColumn<>("Value");

        valueCol.setPrefWidth(300);
        valueCol.setCellValueFactory(new Callback<CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(CellDataFeatures<MetadataNode, String> param)
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

        valueCol.setCellFactory(new Callback<TreeTableColumn<MetadataNode, String>, TreeTableCell<MetadataNode, String>>()
        {
            @Override
            public TreeTableCell<MetadataNode, String> call(TreeTableColumn<MetadataNode, String> param)
            {
                return new TreeTableCell<MetadataNode, String>()
                {
                    @Override
                    protected void updateItem(String value, boolean empty)
                    {
                        super.updateItem(value, empty);

                        if (empty || value == null)
                        {
                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        final TreeItem<MetadataNode> item = getTreeTableRow() != null ? getTreeTableRow().getTreeItem() : null;

                        if (item != null && item.getValue() != null && UtilsJavaFX.isGpsLocationTag(item.getValue().getName()))
                        {
                            Hyperlink link = new Hyperlink(value);

                            link.setOnAction(new EventHandler<ActionEvent>()
                            {
                                @Override
                                public void handle(ActionEvent event)
                                {
                                    String targetFileName = traverseToRootName(item);

                                    if (targetFileName != null && gpsMapManager.hasDataGPS())
                                    {
                                        rbMap.setSelected(true);
                                        cbGpsFiles.getSelectionModel().select(targetFileName);
                                        gpsMapManager.renderMap(targetFileName);
                                    }
                                }
                            });

                            setText(null);
                            setGraphic(link);
                        }
                        else
                        {
                            setText(value);
                            setGraphic(null);
                        }
                    }
                };
            }
        });

        treeTableView = new TreeTableView<>();
        treeTableView.setShowRoot(false);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        treeTableView.getColumns().add(nameCol);
        treeTableView.getColumns().add(valueCol);

        flatTextArea = new TextArea();
        flatTextArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        flatTextArea.setEditable(false);

        containerStack = new StackPane(treeTableView, flatTextArea, mapView);

        ToggleGroup toggleGroup = new ToggleGroup();
        rbMap = new RadioButton("GPS Map 📍");
        rbMap.setToggleGroup(toggleGroup);
        rbMap.setDisable(true);
        rbFlat.setToggleGroup(toggleGroup);
        rbTree.setToggleGroup(toggleGroup);
        rbTree.setSelected(true);
        allItemsExpanded = true;

        btnExpand.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                allItemsExpanded = !allItemsExpanded;

                if (treeTableView.getRoot() != null)
                {
                    setExpandedRecursive(treeTableView.getRoot(), allItemsExpanded);
                }

                btnExpand.setText(allItemsExpanded ? "Collapse All" : "Expand All");
            }
        });

        treeTableView.setVisible(true);
        flatTextArea.setVisible(false);
        mapView.setVisible(false);

        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue)
            {
                boolean showTree = (newValue == rbTree);
                boolean showFlat = (newValue == rbFlat);
                boolean showMap = (newValue == rbMap);

                treeTableView.setVisible(showTree);
                flatTextArea.setVisible(showFlat);
                mapView.setVisible(showMap);

                txtSearch.setVisible(showTree);
                txtSearch.setManaged(showTree);
                btnExpand.setVisible(showTree);

                cbGpsFiles.setVisible(showMap);
                cbGpsFiles.setManaged(showMap);
            }
        });

        HBox toolbarPane = new HBox(12, new Label("View Mode:"), rbTree, rbFlat, rbMap, cbGpsFiles, new Region(), txtSearch, btnExpand);
        toolbarPane.setAlignment(Pos.CENTER_LEFT);
        toolbarPane.setPadding(new Insets(5, 10, 5, 10));
        HBox.setHgrow(toolbarPane.getChildren().get(5), Priority.ALWAYS);

        btnCopy.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                rbFlat.setSelected(true);
                UtilsJavaFX.doFlashCopyTextArea(flatTextArea);
            }
        });

        btnExport.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                rbFlat.setSelected(true);
                exportToFile();
            }
        });

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
        setScene(new Scene(rootLayout, 850, 550));
    }

    /**
     * Filters the metadata tree using the specified search query. Matching nodes and their parent
     * branches are retained. The complete tree is restored when the query is empty.
     * 
     * @param query
     *        the search query to apply to metadata names and values
     */
    private void filterTree(String query)
    {
        if (masterRootNode == null)
        {
            return;
        }

        if (query == null || query.trim().isEmpty())
        {
            treeTableView.setRoot(masterRootNode);
            return;
        }

        String filter = query.toLowerCase().trim();
        TreeItem<MetadataNode> filteredRoot = buildFilteredSubtree(masterRootNode, filter);

        treeTableView.setRoot(filteredRoot);
    }

    /**
     * Recursively builds a filtered copy of the metadata tree, retaining nodes that match the query
     * or contain matching descendants. Structural pointer tags are excluded from direct matches.
     * 
     * @param current
     *        the current tree item to evaluate
     * @param query
     *        the lower-case search query
     * @return a filtered subtree, or {@code null} if the node and all its descendants do not match
     */
    private TreeItem<MetadataNode> buildFilteredSubtree(TreeItem<MetadataNode> current, String query)
    {
        MetadataNode nodeData = current.getValue();
        boolean matches = false;

        if (nodeData != null)
        {
            String name = nodeData.getName() != null ? nodeData.getName().toLowerCase() : "";
            String val = nodeData.getValue() != null ? nodeData.getValue().toLowerCase() : "";

            // Ignore structural IFD offset pointers from direct search hits
            boolean isPointerTag = name.endsWith("pointer");

            if (!isPointerTag)
            {
                matches = name.contains(query) || val.contains(query);
            }
        }

        TreeItem<MetadataNode> copyNode = new TreeItem<>(nodeData);

        for (TreeItem<MetadataNode> child : current.getChildren())
        {
            TreeItem<MetadataNode> filteredChild = buildFilteredSubtree(child, query);

            if (filteredChild != null)
            {
                copyNode.getChildren().add(filteredChild);
            }
        }

        if (matches || !copyNode.getChildren().isEmpty())
        {
            copyNode.setExpanded(true);
            return copyNode;
        }

        return null;
    }

    /**
     * Traverses the tree hierarchy from the specified item to determine the associated root file
     * name.
     * 
     * @param item
     *        the tree item whose associated file name is required
     * @return the associated root file name, or {@code null} if it cannot be determined
     */
    private String traverseToRootName(TreeItem<MetadataNode> item)
    {
        TreeItem<MetadataNode> node = item;

        while (node != null && node.getParent() != null && node.getParent() != treeTableView.getRoot())
        {
            node = node.getParent();
        }

        return (node != null && node.getValue() != null) ? node.getValue().getName() : null;
    }

    /**
     * Sets the raw metadata output displayed in the flat text view.
     * 
     * @param rawOutput
     *        the raw metadata text to display
     */
    void setMetadataText(String rawOutput)
    {
        flatTextArea.setText(rawOutput == null ? "" : rawOutput);
    }

    /**
     * Populates the metadata tree from the specified records and updates the available GPS
     * locations.
     * 
     * @param records
     *        the extracted metadata records to display
     */
    void setMetadataRecords(List<MediaFileMetadata> records)
    {
        this.currentRecords = records; 
        
        masterRootNode = new TreeItem<>(new MetadataNode("Root", ""));
        txtSearch.clear();

        gpsMapManager.reset();

        if (records != null)
        {
            for (MediaFileMetadata record : records)
            {
                Metadata<?> meta = record.getMetadata();
                String fileName = record.getFileName() != null ? record.getFileName() : "Unknown File";
                TreeItem<MetadataNode> fileNode = new TreeItem<>(new MetadataNode(fileName, ""));

                fileNode.setExpanded(true);

                if (meta instanceof TifMetadataProvider)
                {
                    TifMetadataProvider tif = (TifMetadataProvider) meta;

                    for (DirectoryIFD ifd : tif)
                    {
                        gpsMapManager.addLocationGPS(fileName, ifd);

                        String groupName = "[" + ifd.getDirectoryType().getDescription() + "]";
                        TreeItem<MetadataNode> groupNode = new TreeItem<>(new MetadataNode(groupName, ""));

                        groupNode.setExpanded(true);

                        for (DirectoryIFD.EntryIFD entry : ifd)
                        {
                            Taggable tag = entry.getTag();

                            if (tag != null)
                            {
                                String value = tag.translate(entry.getData());

                                if (!value.isEmpty())
                                {
                                    TreeItem<MetadataNode> valueNode = new TreeItem<>(new MetadataNode(tag.getDescription(), value));
                                    groupNode.getChildren().add(valueNode);
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
                    final TreeItem<MetadataNode> groupNode = new TreeItem<>(new MetadataNode("[PNG]", ""));

                    groupNode.setExpanded(true);

                    PropertyConsumer consumer = new PropertyConsumer()
                    {
                        @Override
                        public void accept(String key, Object value)
                        {
                            groupNode.getChildren().add(new TreeItem<>(new MetadataNode(key, String.valueOf(value))));
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

                masterRootNode.getChildren().add(fileNode);
            }
        }

        List<String> gpsFiles = gpsMapManager.update();

        cbGpsFiles.getItems().setAll(gpsFiles);

        if (!gpsFiles.isEmpty())
        {
            cbGpsFiles.getSelectionModel().selectFirst();
        }

        rbMap.setDisable(!gpsMapManager.hasDataGPS());

        treeTableView.setRoot(masterRootNode);
    }

    /**
     * Recursively updates the expanded state of the specified tree item and its descendants without
     * changing the expanded state of the hidden root item.
     * 
     * @param item
     *        the tree item whose descendants are to be updated
     * @param expanded
     *        {@code true} to expand the items, {@code false} to collapse them
     */
    private void setExpandedRecursive(TreeItem<?> item, boolean expanded)
    {
        if (item != null)
        {
            if (item != treeTableView.getRoot())
            {
                item.setExpanded(expanded);
            }

            for (TreeItem<?> child : item.getChildren())
            {
                setExpandedRecursive(child, expanded);
            }
        }
    }

    /**
     * Prompts the user with a {@link FileChooser} supporting multiple export formats (JSON, CSV,
     * TXT)
     * and exports the stored metadata records accordingly.
     */
    private void exportToFile()
    {
        FileChooser chooser = new FileChooser();
        File userHome = new File(System.getProperty("user.home"));

        chooser.setTitle("Export Metadata Records");

        if (userHome.exists() && userHome.isDirectory())
        {
            chooser.setInitialDirectory(userHome);
        }

        FileChooser.ExtensionFilter jsonFilter = new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json");
        FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv");
        FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");

        chooser.getExtensionFilters().addAll(jsonFilter, csvFilter, txtFilter);
        chooser.setInitialFileName(SystemInfo.getHostname() + "_metadata");

        File file = chooser.showSaveDialog(this);

        if (file != null)
        {
            String format = "TXT";
            FileChooser.ExtensionFilter selectedFilter = chooser.getSelectedExtensionFilter();

            if (selectedFilter == jsonFilter)
            {
                format = "JSON";
            }

            else if (selectedFilter == csvFilter)
            {
                format = "CSV";
            }

            try
            {
                MetadataExporter.export(file, currentRecords, format);
                UtilsJavaFX.launchPopup(this, "Export Successful", "Metadata exported successfully to:\n" + file.getAbsolutePath(), AlertType.INFORMATION);
            }

            catch (IOException exc)
            {
                String errorMsg = (exc.getMessage() != null && !exc.getMessage().isEmpty()) ? exc.getMessage() : exc.toString();
                UtilsJavaFX.launchPopup(this, "Export Error", "Failed to export metadata:\n" + errorMsg, AlertType.ERROR);
            }
        }
    }

    /**
     * Prompts the user to save the flat metadata text to a file. The user's home directory is used
     * as the initial directory when available, and the default file name includes the system
     * hostname. An error dialog is displayed if the file cannot be written.
     */
    private void exportToFile2()
    {
        FileChooser chooser = new FileChooser();
        File userHome = new File(System.getProperty("user.home"));

        chooser.setTitle("Save Metadata File");

        if (userHome.exists() && userHome.isDirectory())
        {
            chooser.setInitialDirectory(userHome);
        }

        chooser.setInitialFileName(SystemInfo.getHostname() + "_metadata.txt");
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
                String errorMsg = (exc.getMessage() != null && !exc.getMessage().isEmpty()) ? exc.getMessage() : exc.toString();
                UtilsJavaFX.launchPopup(this, "Export Error", "Failed to export metadata to file:\n" + errorMsg, AlertType.ERROR);
            }
        }
    }

    /**
     * Represents the name and value displayed for a metadata tree item.
     */
    private static class MetadataNode
    {
        private final String name;
        private final String value;

        /**
         * Constructs a new {@code MetadataNode} with the specified name and value.
         * 
         * @param name
         *        the metadata item name
         * @param value
         *        the metadata item value
         */
        MetadataNode(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns the name of this metadata node.
         * 
         * @return the metadata node name
         */
        String getName()
        {
            return name;
        }

        /**
         * Returns the value of this metadata node.
         * 
         * @return the metadata node value
         */
        String getValue()
        {
            return value;
        }
    }
}
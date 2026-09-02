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
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import tif.DirectoryIFD;
import tif.TifMetadataProvider;
import tif.tagspecs.Taggable;
import util.SystemInfo;

/**
 * Modal dialog that presents extracted general metadata using a structured TreeTableView
 * and flat raw text, delegating dedicated GPS map rendering to an external manager.
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
    private boolean allItemsExpanded;

    /**
     * Constructs a new dialog box initialised with layout components, cell value factories, view
     * toggle listeners, and action handlers for viewing metadata items in either tree or flat text
     * mode.
     *
     * @param owner
     *        the parent {@link Stage} owning this modal dialog
     */
    MetadataViewerDialog(Stage owner)
    {
        final Button btnExpand = new Button("Collapse All");
        final Button btnCopy = new Button("Copy to Clipboard");
        final Button btnExport = new Button("Export to File");
        final Button btnClose = new Button("Close");
        final RadioButton rbFlat = new RadioButton("Raw Flat Text");
        final RadioButton rbTree = new RadioButton("Structured Tree");

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

        // Delegate UI integration and events to GpsMapHtmlManager
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
                btnExpand.setVisible(showTree);

                cbGpsFiles.setVisible(showMap);
                cbGpsFiles.setManaged(showMap);
            }
        });

        HBox toolbarPane = new HBox(12, new Label("View Mode:"), rbTree, rbFlat, rbMap, cbGpsFiles, new Region(), btnExpand);
        toolbarPane.setAlignment(Pos.CENTER_LEFT);
        toolbarPane.setPadding(new Insets(5, 10, 5, 10));
        HBox.setHgrow(toolbarPane.getChildren().get(5), Priority.ALWAYS);

        btnCopy.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                rbFlat.setSelected(true);
                UtilsJavaFX.copyTextAreaWithFlash(flatTextArea);
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
        setScene(new Scene(rootLayout, 800, 550));
    }

    /**
     * Sets raw flat output text into the preview component.
     *
     * @param rawOutput
     *        the unformatted raw text to display
     */
    void setMetadataText(String rawOutput)
    {
        flatTextArea.setText(rawOutput == null ? "" : rawOutput);
    }

    /**
     * Populates the {@link TreeTableView} with general metadata records and delegates
     * GPS metadata processing to {@link GpsMapHtmlManager}.
     *
     * @param records
     *        the extracted file metadata records to display
     */
    void setMetadataRecords(List<MediaFileMetadata> records)
    {
        TreeItem<MetadataNode> rootNode = new TreeItem<>(new MetadataNode("Root", ""));

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
                        gpsMapManager.processIfd(fileName, ifd);

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

                rootNode.getChildren().add(fileNode);
            }
        }

        List<String> gpsFiles = gpsMapManager.update();

        cbGpsFiles.getItems().setAll(gpsFiles);

        if (!gpsFiles.isEmpty())
        {
            cbGpsFiles.getSelectionModel().selectFirst();
        }

        rbMap.setDisable(!gpsMapManager.hasLocations());

        treeTableView.setRoot(rootNode);
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
     * Prompts the user with a {@link FileChooser} dialog to save the flat text metadata to a file.
     * Defaults to the user's home directory with a pre-populated file name containing the system
     * hostname. Launches an error popup via {@link UtilsJavaFX#launchPopup} if writing fails or is
     * interrupted.
     */
    private void exportToFile()
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
     * Represents the name and value displayed for a single metadata tree item.
     */
    private static class MetadataNode
    {
        private final String name;
        private final String value;

        /**
         * Constructs a new {@code MetadataNode} with the specified key name and value.
         *
         * @param name
         *        the node description or key
         * @param value
         *        the value representation of the metadata item
         */
        MetadataNode(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns the name or property title of this node.
         *
         * @return the node name string
         */
        String getName()
        {
            return name;
        }

        /**
         * Returns the metadata property value of this node.
         *
         * @return the node value string
         */
        String getValue()
        {
            return value;
        }
    }
}
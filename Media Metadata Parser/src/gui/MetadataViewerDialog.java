package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import png.PngChunk;
import png.PngDirectory;
import png.PngMetadataProvider;
import tif.DirectoryIFD;
import tif.RationalNumber;
import tif.TagValueTranslator;
import tif.TifMetadataProvider;
import tif.tagspecs.TagIFD_GPS;
import tif.tagspecs.Taggable;
import util.SystemInfo;

/**
 * Modal dialog that presents extracted metadata using a structured TreeTableView, flat
 * raw text, and an integrated interactive GPS map view.
 */
class MetadataViewerDialog extends Stage
{
    private boolean allItemsExpanded;
    private final TextArea flatTextArea;
    private final WebView mapView;
    private final RadioButton rbMap;
    private final ComboBox<String> cbGpsFiles;
    private final StackPane containerStack;
    private final TreeTableView<MetadataNode> treeTableView;
    private final Map<String, double[]> gpsCoordsMap = new HashMap<>();

    /**
     * Constructs a new dialog box initialized with layout components, cell value factories, view
     * toggle listeners, and action handlers for viewing metadata items.
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
        final TreeTableColumn<MetadataNode, String> nameCol = new TreeTableColumn<>("File / Metadata Group / Property");
        final TreeTableColumn<MetadataNode, String> valueCol = new TreeTableColumn<>("Value");

        mapView = new WebView();
        cbGpsFiles = new ComboBox<>();
        cbGpsFiles.setPromptText("Select GPS File...");
        cbGpsFiles.setVisible(false);
        cbGpsFiles.setManaged(false); // Prevents layout spacing when hidden

        cbGpsFiles.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                String selectedFile = cbGpsFiles.getValue();

                if (selectedFile != null && gpsCoordsMap.containsKey(selectedFile))
                {
                    double[] coords = gpsCoordsMap.get(selectedFile);
                    mapView.getEngine().loadContent(buildMapHtml(selectedFile, coords[0], coords[1]));
                }
            }
        });

        rbMap = new RadioButton("GPS Map 📍");
        rbMap.setDisable(true); // Enabled dynamically when GPS data is present

        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Media Metadata Viewer");

        treeTableView = new TreeTableView<>();
        treeTableView.setShowRoot(false);
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

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

        treeTableView.getColumns().add(nameCol);
        treeTableView.getColumns().add(valueCol);

        flatTextArea = new TextArea();
        flatTextArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        flatTextArea.setEditable(false);

        containerStack = new StackPane(treeTableView, flatTextArea, mapView);

        ToggleGroup toggleGroup = new ToggleGroup();

        rbFlat.setToggleGroup(toggleGroup);
        rbTree.setToggleGroup(toggleGroup);
        rbMap.setToggleGroup(toggleGroup);
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

                // Show dropdown only when map mode is active
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
                GUIUtils.copyTextAreaWithFlash(flatTextArea);
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
     * Populates the {@link TreeTableView} with a hierarchical representation of the specified
     * metadata records, while scanning for GPS attributes to populate the ComboBox and map view.
     *
     * @param records
     *        the extracted file metadata records to display
     */
    void setMetadataRecords(List<MediaFileMetadata> records)
    {
        TreeItem<MetadataNode> rootItem = new TreeItem<>(new MetadataNode("Root", ""));

           gpsCoordsMap.clear();
        cbGpsFiles.getItems().clear();

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
                    String latRef = "N";
                    String lonRef = "E";
                    RationalNumber[] latDms = null;
                    RationalNumber[] lonDms = null;

                    for (DirectoryIFD ifd : tif)
                    {
                        String groupName = "[" + ifd.getDirectoryType().getDescription() + "]";
                        TreeItem<MetadataNode> groupNode = new TreeItem<>(new MetadataNode(groupName, ""));

                        groupNode.setExpanded(true);

                        for (DirectoryIFD.EntryIFD entry : ifd)
                        {
                            Taggable tag = entry.getTag();

                            if (tag != null)
                            {
                                String value = tag.translate(entry.getData());

                                // Capture GPS Coordinate attributes for Map rendering
                                if (tag == TagIFD_GPS.GPS_LATITUDE)
                                {
                                    latDms = TagValueTranslator.toRationalArray(entry.getData());
                                }

                                else if (tag == TagIFD_GPS.GPS_LATITUDE_REF)
                                {
                                    latRef = String.valueOf(entry.getData()).trim();
                                }

                                else if (tag == TagIFD_GPS.GPS_LONGITUDE)
                                {
                                    lonDms = TagValueTranslator.toRationalArray(entry.getData());
                                }

                                else if (tag == TagIFD_GPS.GPS_LONGITUDE_REF)
                                {
                                    lonRef = String.valueOf(entry.getData()).trim();
                                }

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

                    // Store coordinates and add file name to ComboBox list
                    if (latDms != null && lonDms != null)
                    {
                        double lat = convertDmsToDecimal(latDms, latRef);
                        double lon = convertDmsToDecimal(lonDms, lonRef);

                        if (!Double.isNaN(lat) && !Double.isNaN(lon))
                        {
                            gpsCoordsMap.put(fileName, new double[]{lat, lon});
                            cbGpsFiles.getItems().add(fileName);
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

                rootItem.getChildren().add(fileNode);
            }
        }

        boolean gpsFound = !cbGpsFiles.getItems().isEmpty();
        rbMap.setDisable(!gpsFound);

        // Pre-select the first file and load its map coordinates
        if (gpsFound)
        {
            cbGpsFiles.getSelectionModel().selectFirst();
            String firstFile = cbGpsFiles.getValue();
            double[] coords = gpsCoordsMap.get(firstFile);
            
            mapView.getEngine().loadContent(buildMapHtml(firstFile, coords[0], coords[1]));
        }

        treeTableView.setRoot(rootItem);
    }

    /**
     * Converts a Degrees, Minutes, and Seconds (DMS) rational array into decimal degrees.
     */
    private double convertDmsToDecimal(RationalNumber[] dms, String ref)
    {
        if (dms == null || dms.length < 3 || dms[0] == null || dms[1] == null || dms[2] == null)
        {
            return Double.NaN;
        }

        double degrees = dms[0].doubleValue();
        double minutes = dms[1].doubleValue();
        double seconds = dms[2].doubleValue();
        double decimal = degrees + (minutes / 60.0) + (seconds / 3600.0);

        if ("S".equalsIgnoreCase(ref) || "W".equalsIgnoreCase(ref))
        {
            decimal = -decimal;
        }

        return decimal;
    }

    /**
     * Constructs HTML string embedding OpenStreetMap via LeafletJS.
     */
    private String buildMapHtml(String title, double lat, double lon)
    {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "  <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "  <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "  <style>html, body, #map { height: 100%; margin: 0; padding: 0; }</style>"
                + "</head>"
                + "<body>"
                + "  <div id='map'></div>"
                + "  <script>"
                + "    var map = L.map('map').setView([" + lat + ", " + lon + "], 15);"
                + "    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {"
                + "      maxZoom: 19,"
                + "      attribution: '© OpenStreetMap'"
                + "    }).addTo(map);"
                + "    L.marker([" + lat + ", " + lon + "]).addTo(map)"
                + "      .bindPopup('<b>" + title.replace("'", "\\'") + "</b><br>Lat: " + lat + "<br>Lon: " + lon + "')"
                + "      .openPopup();"
                + "  </script>"
                + "</body>"
                + "</html>";
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
     * hostname. Launches an error popup via {@link GUIUtils#launchPopup} if writing fails or is
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
                GUIUtils.launchPopup(this, "Export Error", "Failed to export metadata to file:\n" + errorMsg, AlertType.ERROR);
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
package gui;

import java.io.File;
import java.util.List;
import batch.MediaBatchProcessor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import progressbar.JavaFXProgressAdapter;
import progressbar.ProgressListener;

public class MediaMetadataApp extends Application
{
    private static final String ID_SHOW_METADATA_CHECK = "showMetadataCheck";
    private static final String ID_ACTION_BTN = "actionBtn";
    private static final String ID_PREFIX_FIELD = "prefixField";
    private static final String ID_MODIFY_DATE_PICKER = "modifyDatePicker";
    private static final String ID_LOG_AREA = "logArea";
    private static final String ID_CANCEL_BTN = "cancelBtn";
    private Stage stage;

    @Override
    public void start(Stage userstage)
    {
        this.stage = userstage;
        stage.setTitle("Image Metadata Structure Viewer");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(15));

        // Configure Row Constraints:
        // Row 0 (Top Pane): ALWAYS
        // Row 1 (Middle Pane): ALWAYS
        // Row 2 (Log Pane): ALWAYS (Grows dynamically when window expands/resizes)
        // Row 3 (Bottom Pane): ALWAYS

        javafx.scene.layout.RowConstraints staticRow = new javafx.scene.layout.RowConstraints();
        staticRow.setVgrow(Priority.NEVER); // Keep controls at natural height

        javafx.scene.layout.RowConstraints growingRow = new javafx.scene.layout.RowConstraints();
        growingRow.setVgrow(Priority.ALWAYS); // Expand log pane fill space

        formGrid.getRowConstraints().addAll(staticRow, staticRow, growingRow, staticRow, staticRow);

        addTopPane(formGrid);
        addMiddlePane(formGrid);
        addLogPane(formGrid);
        addBottomPane(formGrid);
        addControlPane(formGrid);

        Scene scene = new Scene(formGrid, 620, 650);
        stage.setScene(scene);
        stage.show();

        configureDynamicNodes(scene);
    }

    private void configureDynamicNodes(Scene scene)
    {
        CheckBox showMetadataCheck = (CheckBox) scene.lookup("#" + ID_SHOW_METADATA_CHECK);
        Button actionBtn = (Button) scene.lookup("#" + ID_ACTION_BTN);
        TextField prefixField = (TextField) scene.lookup("#" + ID_PREFIX_FIELD);
        DatePicker modifyDatePicker = (DatePicker) scene.lookup("#" + ID_MODIFY_DATE_PICKER);

        if (showMetadataCheck != null)
        {
            if (actionBtn != null)
            {
                showMetadataCheck.selectedProperty().addListener(new ChangeListener<Boolean>()
                {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
                    {
                        if (newValue != null && newValue.booleanValue())
                        {
                            actionBtn.setText("Display Metadata");
                        }
                        else
                        {
                            actionBtn.setText("Run Batch Process");
                        }
                    }
                });
            }

            if (prefixField != null)
            {
                prefixField.disableProperty().bind(showMetadataCheck.selectedProperty());
            }

            if (modifyDatePicker != null)
            {
                modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());
            }
        }
    }

    private void addTopPane(GridPane pane)
    {
        double labelWidth = 140;

        Label sourceLabel = new Label("Source Directory: ");
        sourceLabel.setPrefWidth(labelWidth);

        TextField sourceField = new TextField();
        sourceField.setPromptText("Directory or files...");
        sourceField.setPrefWidth(300);
        sourceField.setMaxWidth(300);

        Button browseSourceBtn = new Button("Browse...");
        ContextMenu sourceMenu = new ContextMenu();
        MenuItem selectFolderItem = new MenuItem("Select Folder...");
        MenuItem selectFilesItem = new MenuItem("Select Specific Files...");

        selectFolderItem.setOnAction(new DirectoryPopupHandler(sourceField, "Select Source Directory"));

        selectFilesItem.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Select Source Files");

                List<File> files = chooser.showOpenMultipleDialog(pane.getScene().getWindow());

                if (files != null && !files.isEmpty())
                {
                    StringBuilder names = new StringBuilder();

                    for (int i = 0; i < files.size(); i++)
                    {
                        names.append(files.get(i).getName());

                        if (i < files.size() - 1)
                        {
                            names.append(", ");
                        }
                    }

                    sourceField.setText(names.toString());
                }
            }
        });

        sourceMenu.getItems().addAll(selectFolderItem, selectFilesItem);

        browseSourceBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                sourceMenu.show(browseSourceBtn, Side.BOTTOM, 0, 0);
            }
        });

        HBox sourceRowLayout = new HBox(10);
        sourceRowLayout.getChildren().addAll(sourceLabel, sourceField, createSpacer(), browseSourceBtn);

        Label targetLabel = new Label("Target Directory: ");
        targetLabel.setPrefWidth(labelWidth);

        TextField targetField = new TextField();
        targetField.setText(MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY);
        targetField.setPrefWidth(300);
        targetField.setMaxWidth(300);

        Button browseTargetBtn = new Button("Browse...");
        browseTargetBtn.setOnAction(new DirectoryPopupHandler(targetField, "Select Target Directory"));

        HBox targetRowLayout = new HBox(10);
        targetRowLayout.getChildren().addAll(targetLabel, targetField, createSpacer(), browseTargetBtn);

        Label prefixLabel = new Label("File Prefix Name: ");
        prefixLabel.setPrefWidth(labelWidth);

        TextField prefixField = new TextField();
        prefixField.setId(ID_PREFIX_FIELD);
        prefixField.setText(MediaBatchProcessor.DEFAULT_IMAGE_PREFIX);
        prefixField.setPromptText("Example: Holiday_Trip_");
        prefixField.setPrefWidth(300);
        prefixField.setMaxWidth(300);

        HBox prefixRowLayout = new HBox(10);
        prefixRowLayout.getChildren().addAll(prefixLabel, prefixField, createSpacer());

        Label dateLabel = new Label("Modify Date Taken:");
        dateLabel.setPrefWidth(labelWidth);

        DatePicker modifyDatePicker = new DatePicker();
        modifyDatePicker.setId(ID_MODIFY_DATE_PICKER);
        modifyDatePicker.setPromptText("Select date...");
        modifyDatePicker.setPrefWidth(300);
        modifyDatePicker.setMaxWidth(300);

        HBox modifyDateRowLayout = new HBox(10);
        modifyDateRowLayout.getChildren().addAll(dateLabel, modifyDatePicker, createSpacer());

        VBox optionsGroup = new VBox(12);
        BorderStroke stroke = new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1));
        optionsGroup.setPadding(new Insets(10));
        optionsGroup.setBorder(new Border(stroke));
        optionsGroup.getChildren().addAll(sourceRowLayout, targetRowLayout, prefixRowLayout, modifyDateRowLayout);

        GridPane.setHgrow(optionsGroup, Priority.ALWAYS);

        pane.add(optionsGroup, 0, 0);
    }

    private void addMiddlePane(GridPane pane)
    {
        CheckBox embedDateTimeCheck = new CheckBox("Embed Date/Time");
        CheckBox forceDateChangeCheck = new CheckBox("Force Date Change");
        CheckBox skipVideoCheck = new CheckBox("Skip Video Files");
        CheckBox descendingCheck = new CheckBox("Sort Descending");
        CheckBox debugCheck = new CheckBox("Enable Debugging");

        CheckBox showMetadataCheck = new CheckBox("Display Exif Metadata");
        showMetadataCheck.setId(ID_SHOW_METADATA_CHECK);

        embedDateTimeCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
        forceDateChangeCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
        skipVideoCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
        descendingCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
        debugCheck.disableProperty().bind(showMetadataCheck.selectedProperty());

        VBox column1 = new VBox(10, embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck);
        VBox column2 = new VBox(10, showMetadataCheck, descendingCheck, debugCheck);

        HBox optionsGroup = new HBox(40, column1, column2);
        optionsGroup.setPadding(new Insets(10));
        optionsGroup.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));

        GridPane.setHgrow(optionsGroup, Priority.ALWAYS);
        skipVideoCheck.setSelected(true);

        pane.add(optionsGroup, 0, 1);
    }

    private void addLogPane(GridPane pane)
    {
        TextArea logArea = new TextArea();
        logArea.setId(ID_LOG_AREA);
        logArea.setEditable(false);
        logArea.setFocusTraversable(false);
        logArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
        logArea.setPromptText("Console output...");

        // Make the TextArea grow vertically to fill its VBox
        VBox.setVgrow(logArea, Priority.ALWAYS);
        logArea.setMaxHeight(Double.MAX_VALUE);

        VBox logGroup = new VBox(5);
        logGroup.getChildren().addAll(new Label("Execution Log:"), logArea);

        // Make the VBox fill the entire available width and height in the GridPane
        GridPane.setHgrow(logGroup, Priority.ALWAYS);
        GridPane.setVgrow(logGroup, Priority.ALWAYS);

        pane.add(logGroup, 0, 2);
    }

    private void addBottomPane(GridPane pane)
    {
        Button actionBtn = new Button("Run Batch Process");
        actionBtn.setId(ID_ACTION_BTN);

        ProgressBar progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(220);
        progressBar.prefHeightProperty().bind(actionBtn.heightProperty());

        // NEW: Summary Button
        Button viewSummaryBtn = new Button("View Summary...");
        viewSummaryBtn.prefHeightProperty().bind(actionBtn.heightProperty());
        viewSummaryBtn.setOnAction(e -> showSummaryDialog(pane.getScene().getWindow()));

        final ProgressListener progressAdapter = new JavaFXProgressAdapter(progressBar);

        actionBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                actionBtn.setDisable(true);
                TextArea logArea = (TextArea) pane.getScene().lookup("#" + ID_LOG_AREA);

                if (logArea != null)
                {
                    logArea.clear();
                    logArea.appendText("[INFO] Initializing batch process...\n");
                }

                Button cancelBtn = (Button) pane.getScene().lookup("#" + ID_CANCEL_BTN);

                if (cancelBtn != null)
                {
                    cancelBtn.setDisable(false);
                }

                Thread workerThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            CheckBox showMetadataCheck = (CheckBox) pane.getScene().lookup("#" + ID_SHOW_METADATA_CHECK);

                            // Simulate workload and real-time logging
                            for (int i = 1; i <= 5; i++)
                            {
                                Thread.sleep(500); // Simulated delay

                                String msg = "[PROCESS] Handled Image_" + i + ".jpg\n";

                                if (logArea != null)
                                {
                                    Platform.runLater(() -> logArea.appendText(msg));
                                }
                            }

                            if (showMetadataCheck != null && showMetadataCheck.isSelected())
                            {
                                Platform.runLater(() -> logArea.appendText("\n[SUCCESS] Exif data retrieved successfully.\n"));
                            }

                            else
                            {
                                Platform.runLater(() -> logArea.appendText("\n[SUCCESS] Batch processing complete.\n"));
                            }
                        }

                        catch (Exception exc)
                        {
                            exc.printStackTrace();
                            Platform.runLater(() -> logArea.appendText("[ERROR] " + exc.getMessage() + "\n"));
                        }

                        finally
                        {
                            Platform.runLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (cancelBtn != null)
                                    {
                                        cancelBtn.setDisable(true);
                                    }

                                    actionBtn.setDisable(false);
                                }
                            });
                        }
                    }
                });

                workerThread.setDaemon(true);
                workerThread.start();
            }
        });

        HBox bottomLayout = new HBox(15, actionBtn, progressBar, createSpacer(), viewSummaryBtn);
        bottomLayout.setPadding(new Insets(10));
        bottomLayout.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));

        GridPane.setHgrow(bottomLayout, Priority.ALWAYS);

        // Moved down to row 3
        pane.add(bottomLayout, 0, 3);
    }

    private void showSummaryDialog(Window ownerWindow)
    {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch Processing Summary");
        dialog.setHeaderText("Detailed Processing Results");
        dialog.initOwner(ownerWindow);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<FileRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Auto-fits columns to
                                                                          // width

        // Column 1: Source File
        TableColumn<FileRecord, String> sourceCol = new TableColumn<>("Source File");
        sourceCol.setCellValueFactory(cellData -> cellData.getValue().sourceNameProperty());
        sourceCol.setPrefWidth(200);

        // Column 2: Target File
        TableColumn<FileRecord, String> targetCol = new TableColumn<>("Target File");
        targetCol.setCellValueFactory(cellData -> cellData.getValue().targetNameProperty());
        targetCol.setPrefWidth(200);

        // Column 3: Status
        TableColumn<FileRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(120);

        table.getColumns().add(sourceCol);
        table.getColumns().add(targetCol);
        table.getColumns().add(statusCol);

        // Sample data illustrating the mapping flow
        table.getItems().addAll(
                new FileRecord("IMG_1020.JPG", "Holiday_Trip_001.JPG", "Success"),
                new FileRecord("IMG_1021.JPG", "Holiday_Trip_002.JPG", "Success"),
                new FileRecord("VID_0045.MP4", "-", "Skipped (Video)"));

        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefSize(550, 320); // Widened slightly to fit 3 columns
                                                      // comfortably
        dialog.showAndWait();
    }

    private void addControlPane(GridPane pane)
    {
        Button cancelBtn = new Button("Cancel Process");
        cancelBtn.setId(ID_CANCEL_BTN);
        cancelBtn.setDisable(true); // Disabled by default, enabled only during execution

        Button clearLogBtn = new Button("Clear Log");
        clearLogBtn.setOnAction(e ->
        {
            TextArea logArea = (TextArea) pane.getScene().lookup("#" + ID_LOG_AREA);
            if (logArea != null)
            {
                logArea.clear();
            }
        });

        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(e -> Platform.exit());

        HBox controlLayout = new HBox(10, cancelBtn, clearLogBtn, createSpacer(), exitBtn);
        controlLayout.setPadding(new Insets(5, 0, 0, 0));

        GridPane.setHgrow(controlLayout, Priority.ALWAYS);

        // Add to Row 4 below the bottom progress pane
        pane.add(controlLayout, 0, 4);
    }

    private Region createSpacer()
    {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private class DirectoryPopupHandler implements EventHandler<ActionEvent>
    {
        private final TextField targetField;
        private final String dialogTitle;

        public DirectoryPopupHandler(TextField targetField, String dialogTitle)
        {
            this.targetField = targetField;
            this.dialogTitle = dialogTitle;
        }

        @Override
        public void handle(ActionEvent event)
        {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(dialogTitle);

            Window window = targetField.getScene().getWindow();
            File folder = chooser.showDialog(window);

            if (folder != null)
            {
                targetField.setText(folder.getAbsolutePath());
            }
        }
    }

    public static class FileRecord
    {
        private final SimpleStringProperty sourceName;
        private final SimpleStringProperty targetName;
        private final SimpleStringProperty status;

        public FileRecord(String sourceName, String targetName, String status)
        {
            this.sourceName = new SimpleStringProperty(sourceName);
            this.targetName = new SimpleStringProperty(targetName);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleStringProperty sourceNameProperty()
        {
            return sourceName;
        }

        public SimpleStringProperty targetNameProperty()
        {
            return targetName;
        }

        public SimpleStringProperty statusProperty()
        {
            return status;
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
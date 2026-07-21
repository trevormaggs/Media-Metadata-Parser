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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import progressbar.JavaFXProgressAdapter;
import progressbar.ProgressListener;

public class MediaMetadataApp extends Application
{
    private final TextField sourceText;
    private final Button sourceBtn;
    private final MenuItem selectFiles;
    private final CheckBox showMetadataCheck;
    private final TextField prefixText;
    private final DatePicker modifyDatePicker;
    private final Button actionBtn;
    private final Button cancelBtn;
    private final TextArea logArea;
    private final Button clearLogBtn;
    private final Button exitBtn;
    private final Button viewBtn;

    private Stage stage;

    public MediaMetadataApp()
    {
        this.sourceBtn = new Button("Browse...");
        this.sourceText = new TextField();
        this.selectFiles = new MenuItem("Select Specific Files...");
        this.showMetadataCheck = new CheckBox("Display Exif Metadata");
        this.prefixText = new TextField();
        this.modifyDatePicker = new DatePicker();
        this.actionBtn = new Button("Run Batch Process");
        this.cancelBtn = new Button("Cancel Process");
        this.logArea = new TextArea();
        this.clearLogBtn = new Button("Clear Log");
        this.exitBtn = new Button("Exit");
        this.viewBtn = new Button("View Summary...");
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

    /**
     * Initialises the primary JavaFX stage and builds the GUI components for this application.
     *
     * @param stage
     *        the primary stage used for populating this JavaFX application
     */
    @Override
    public void start(Stage stage)
    {
        RowConstraints staticRow = new RowConstraints();
        staticRow.setVgrow(Priority.NEVER); // Keep controls at natural height

        RowConstraints growingRow = new RowConstraints();
        growingRow.setVgrow(Priority.ALWAYS); // Expand log pane fill space

        this.stage = stage;
        stage.setTitle("Image Metadata Structure Viewer");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(15));
        formGrid.getRowConstraints().addAll(staticRow, staticRow, growingRow, staticRow, staticRow);

        addTopPane(formGrid);
        addMiddlePane(formGrid);
        addLogPane(formGrid);
        addBottomPane(formGrid);
        addControlPane(formGrid);

        Scene scene = new Scene(formGrid, 620, 650);
        stage.setScene(scene);
        stage.show();

        configureDynamicNodes();
    }

    /**
     * Builds and adds the application's top configuration panel to the specified
     * root grid pane.
     *
     * @param pane
     *        the root {@link GridPane} to which the configuration panel is added
     */
    private void addTopPane(GridPane pane)
    {
        double labelWidth = 140;
        // Row 1
        Label sourceLabel = new Label("Source Directory");
        sourceLabel.setPrefWidth(labelWidth);

        sourceText.setPromptText("Directory or files...");
        sourceText.setPrefWidth(300);
        sourceText.setMaxWidth(300);

        MenuItem selectFolder = new MenuItem("Select Folder...");
        selectFolder.setOnAction(new DirectoryPopupHandler(sourceText, "Select Source Directory"));

        ContextMenu sourceMenu = new ContextMenu();
        ActionHandler actionHandler = new ActionHandler(sourceMenu);

        sourceMenu.getItems().addAll(selectFolder, selectFiles);

        selectFiles.setOnAction(actionHandler);
        sourceBtn.setOnAction(actionHandler);

        HBox sourceHbox = new HBox(10);
        sourceHbox.getChildren().addAll(sourceLabel, sourceText, createSpacer(), sourceBtn);

        // Row 2
        Label targetLabel = new Label("Target Directory");
        targetLabel.setPrefWidth(labelWidth);

        TextField targetText = new TextField();
        targetText.setText(MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY);
        targetText.setPrefWidth(300);
        targetText.setMaxWidth(300);

        Button targetBtn = new Button("Browse...");
        targetBtn.setOnAction(new DirectoryPopupHandler(targetText, "Select Target Directory"));

        HBox targetHbox = new HBox(10);
        targetHbox.getChildren().addAll(targetLabel, targetText, createSpacer(), targetBtn);

        // Row 3
        Label prefixLabel = new Label("File Prefix Name");
        prefixLabel.setPrefWidth(labelWidth);

        prefixText.setText(MediaBatchProcessor.DEFAULT_IMAGE_PREFIX);
        prefixText.setPromptText("Example: Holiday_Trip_");
        prefixText.setPrefWidth(300);
        prefixText.setMaxWidth(300);

        HBox prefixHbox = new HBox(10);
        prefixHbox.getChildren().addAll(prefixLabel, prefixText, createSpacer());

        // Row 4
        Label dateLabel = new Label("Modify Date Taken");
        dateLabel.setPrefWidth(labelWidth);

        modifyDatePicker.setPromptText("Select date...");
        modifyDatePicker.setPrefWidth(300);
        modifyDatePicker.setMaxWidth(300);

        HBox modifyDateHbox = new HBox(10);
        modifyDateHbox.getChildren().addAll(dateLabel, modifyDatePicker, createSpacer());

        VBox optionsGroup = new VBox(12);
        BorderStroke stroke = new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1));
        optionsGroup.setPadding(new Insets(10));
        optionsGroup.setBorder(new Border(stroke));
        optionsGroup.getChildren().addAll(sourceHbox, targetHbox, prefixHbox, modifyDateHbox);

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

        embedDateTimeCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
        forceDateChangeCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
        skipVideoCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
        descendingCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
        debugCheck.disableProperty().bind(showMetadataCheck.selectedProperty());

        VBox col1 = new VBox(10, embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck);
        VBox col2 = new VBox(10, showMetadataCheck, descendingCheck, debugCheck);

        HBox optionsGroup = new HBox(40, col1, col2);
        optionsGroup.setPadding(new Insets(10));
        optionsGroup.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));

        GridPane.setHgrow(optionsGroup, Priority.ALWAYS);
        skipVideoCheck.setSelected(true);

        pane.add(optionsGroup, 0, 1);
    }

    private void addLogPane(GridPane pane)
    {
        logArea.setEditable(false);
        logArea.setFocusTraversable(false);
        logArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
        logArea.setPromptText("Console output...");
        logArea.setMaxHeight(Double.MAX_VALUE);

        VBox.setVgrow(logArea, Priority.ALWAYS);

        VBox logGroup = new VBox(5);
        logGroup.getChildren().addAll(new Label("Execution Log:"), logArea);

        GridPane.setHgrow(logGroup, Priority.ALWAYS);
        GridPane.setVgrow(logGroup, Priority.ALWAYS);

        pane.add(logGroup, 0, 2);
    }

    private void addBottomPane(GridPane pane)
    {
        ActionHandler actionHandler = new ActionHandler();

        actionBtn.setOnAction(actionHandler);

        ProgressBar progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(220);
        progressBar.prefHeightProperty().bind(actionBtn.heightProperty());

        viewBtn.prefHeightProperty().bind(actionBtn.heightProperty());
        viewBtn.setOnAction(actionHandler);

        ProgressListener progressAdapter = new JavaFXProgressAdapter(progressBar);

        HBox bottomLayout = new HBox(15, actionBtn, progressBar, createSpacer(), viewBtn);
        bottomLayout.setPadding(new Insets(10));
        bottomLayout.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));

        GridPane.setHgrow(bottomLayout, Priority.ALWAYS);

        pane.add(bottomLayout, 0, 3);
    }

    /**
     * Configures the dynamic behaviour of the application's user interface by attaching event
     * listeners and binding control properties.
     */
    private void configureDynamicNodes()
    {
        showMetadataCheck.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                if (newValue != null && newValue)
                {
                    actionBtn.setText("Display Metadata");
                }
                else
                {
                    actionBtn.setText("Run Batch Process");
                }
            }
        });

        prefixText.disableProperty().bind(showMetadataCheck.selectedProperty());
        modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());
    }

    private void executeBatchProcess()
    {
        logArea.clear();
        logArea.appendText("[INFO] Initializing batch process...\n");
        actionBtn.setDisable(true);
        cancelBtn.setDisable(false);

        Thread workerThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    for (int i = 1; i <= 5; i++)
                    {
                        Thread.sleep(500);

                        final String msg = "[PROCESS] Handled Image_" + i + ".jpg\n";

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                logArea.appendText(msg);
                            }
                        });
                    }

                    if (showMetadataCheck.isSelected())
                    {
                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                logArea.appendText("\n[SUCCESS] Exif data retrieved successfully.\n");
                            }
                        });
                    }
                    else
                    {
                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                logArea.appendText("\n[SUCCESS] Batch processing complete.\n");
                            }
                        });
                    }
                }
                catch (Exception exc)
                {
                    exc.printStackTrace();

                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            logArea.appendText("[ERROR] " + exc.getMessage() + "\n");
                        }
                    });
                }
                finally
                {
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            cancelBtn.setDisable(true);
                            actionBtn.setDisable(false);
                        }
                    });
                }
            }
        });

        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void cancelBatchProcess()
    {
        logArea.appendText("[WARNING] Batch process cancellation requested.\n");
    }

    private void handleFileSelection()
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Source Files");

        List<File> files = chooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty())
        {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < files.size(); i++)
            {
                sb.append(files.get(i).getName());

                if (i < files.size() - 1)
                {
                    sb.append(", ");
                }
            }

            sourceText.setText(sb.toString());
            sourceText.setTooltip(new Tooltip(sb.toString()));
        }
    }

    private void showSummaryDialog(Window ownerWindow)
    {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch Processing Summary");
        dialog.setHeaderText("Detailed Processing Results");
        dialog.initOwner(ownerWindow);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<FileRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FileRecord, String> sourceCol = new TableColumn<>("Source File");
        sourceCol.setCellValueFactory(new javafx.util.Callback<TableColumn.CellDataFeatures<FileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileRecord, String> cellData)
            {
                return cellData.getValue().sourceNameProperty();
            }
        });
        sourceCol.setPrefWidth(200);

        TableColumn<FileRecord, String> targetCol = new TableColumn<>("Target File");
        targetCol.setCellValueFactory(new javafx.util.Callback<TableColumn.CellDataFeatures<FileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileRecord, String> cellData)
            {
                return cellData.getValue().targetNameProperty();
            }
        });
        targetCol.setPrefWidth(200);

        TableColumn<FileRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new javafx.util.Callback<TableColumn.CellDataFeatures<FileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileRecord, String> cellData)
            {
                return cellData.getValue().statusProperty();
            }
        });
        statusCol.setPrefWidth(120);

        table.getColumns().add(sourceCol);
        table.getColumns().add(targetCol);
        table.getColumns().add(statusCol);

        table.getItems().addAll(
                new FileRecord("IMG_1020.JPG", "Holiday_Trip_001.JPG", "Success"),
                new FileRecord("IMG_1021.JPG", "Holiday_Trip_002.JPG", "Success"),
                new FileRecord("VID_0045.MP4", "-", "Skipped (Video)"));

        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefSize(550, 320);
        dialog.showAndWait();
    }

    private void addControlPane(GridPane pane)
    {
        ActionHandler actionHandler = new ActionHandler();

        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(actionHandler);
        clearLogBtn.setOnAction(actionHandler);
        exitBtn.setOnAction(actionHandler);

        HBox controlLayout = new HBox(10, cancelBtn, clearLogBtn, createSpacer(), exitBtn);
        controlLayout.setPadding(new Insets(5, 0, 0, 0));

        GridPane.setHgrow(controlLayout, Priority.ALWAYS);

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

        public DirectoryPopupHandler(TextField targetText, String title)
        {
            this.targetField = targetText;
            this.dialogTitle = title;
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

    /**
     * Handles action events generated by the application's user interface controls.
     */
    private class ActionHandler implements EventHandler<ActionEvent>
    {
        private final ContextMenu sourceMenu;

        public ActionHandler()
        {
            this(null);
        }

        public ActionHandler(ContextMenu sourceMenu)
        {
            this.sourceMenu = sourceMenu;
        }

        @Override
        public void handle(ActionEvent event)
        {
            Object source = event.getSource();

            if (source == sourceBtn)
            {
                sourceMenu.show(sourceBtn, Side.BOTTOM, 0, 0);
            }

            else if (source == clearLogBtn)
            {
                logArea.clear();
            }

            else if (source == exitBtn)
            {
                Platform.exit();
            }

            else if (source == viewBtn)
            {
                showSummaryDialog(stage);
            }

            else if (source == cancelBtn)
            {
                cancelBatchProcess();
            }

            else if (source == actionBtn)
            {
                executeBatchProcess();
            }

            else if (source == selectFiles)
            {
                handleFileSelection();
            }
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
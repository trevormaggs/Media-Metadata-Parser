package gui;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import batch.BatchBuilder;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.DisplayMetadata;
import batch.MediaBatchProcessor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import progressbar.JavaFXProgressAdapter;

public class MediaMetadataApp3 extends Application
{
    private Stage stage;

    // Control IDs for dynamic scene lookup
    private static final String SRCID = "srcId";
    private static final String TGTID = "tgtId";
    private static final String PFXID = "pfxId";
    private static final String DTMID = "dtmId";
    private static final String EMBID = "embId";
    private static final String FORID = "forId";
    private static final String SKPID = "skpId";
    private static final String SRTID = "srtId";
    private static final String DBGID = "dbgId";
    private static final String METAID = "metaId";
    private static final String LOGID = "logId";
    private static final String ACTID = "actId";
    private static final String CANID = "canId";
    private static final String PRGID = "prgId";

    public MediaMetadataApp3()
    {
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

    @Override
    public void start(Stage root)
    {
        this.stage = root;
        stage.setTitle("Image Metadata Structure Viewer");

        RowConstraints fixedRow = new RowConstraints();
        fixedRow.setVgrow(Priority.NEVER);

        RowConstraints fillRow = new RowConstraints();
        fillRow.setVgrow(Priority.ALWAYS);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(15));
        formGrid.getRowConstraints().addAll(fixedRow, fixedRow, fillRow, fixedRow, fixedRow);

        addTopPane(formGrid);
        addMiddlePane(formGrid);
        addLogPane(formGrid);
        addControlPane(formGrid);
        addBottomPane(formGrid);

        Scene scene = new Scene(formGrid, 620, 650);
        stage.setScene(scene);
        stage.show();

        configureDynamicNodes(formGrid);
    }

    private void addTopPane(GridPane pane)
    {
        double labelWidth = 140;

        // Row 1: Source
        HBox sourceHbox = new HBox(10);
        Label sourceLabel = new Label("Source Directory");
        sourceLabel.setPrefWidth(labelWidth);

        final TextField sourceText = new TextField("E:\\ImageBatchDir");
        sourceText.setId(SRCID);
        sourceText.setPromptText("Directory or files...");
        sourceText.setPrefWidth(300);
        sourceText.setMaxWidth(300);

        final ContextMenu sourceMenu = new ContextMenu();
        MenuItem selectFolder = new MenuItem("Select Folder...");
        selectFolder.setOnAction(new DirectoryPopupHandler(sourceText, "Select Source Directory"));

        MenuItem selectFiles = new MenuItem("Select Specific Files...");
        selectFiles.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                handleFileSelection(sourceText);
            }
        });
        sourceMenu.getItems().addAll(selectFolder, selectFiles);

        final Button sourceBtn = new Button("Browse...");
        sourceBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                sourceMenu.show(sourceBtn, Side.BOTTOM, 0, 0);
            }
        });

        sourceHbox.getChildren().addAll(sourceLabel, sourceText, fillRow(), sourceBtn);

        // Row 2: Target
        HBox targetHbox = new HBox(10);
        Label targetLabel = new Label("Target Directory");
        targetLabel.setPrefWidth(labelWidth);

        TextField targetText = new TextField(MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY);
        targetText.setId(TGTID);
        targetText.setPrefWidth(300);
        targetText.setMaxWidth(300);

        Button targetBtn = new Button("Browse...");
        targetBtn.setOnAction(new DirectoryPopupHandler(targetText, "Select Target Directory"));

        targetHbox.getChildren().addAll(targetLabel, targetText, fillRow(), targetBtn);

        // Row 3: Prefix
        HBox prefixHbox = new HBox(10);
        Label prefixLabel = new Label("File Prefix Name");
        prefixLabel.setPrefWidth(labelWidth);

        TextField prefixText = new TextField(MediaBatchProcessor.DEFAULT_IMAGE_PREFIX);
        prefixText.setId(PFXID);
        prefixText.setPromptText("Example: Holiday_Trip_");
        prefixText.setPrefWidth(300);
        prefixText.setMaxWidth(300);

        prefixHbox.getChildren().addAll(prefixLabel, prefixText, fillRow());

        // Row 4: Date Picker
        HBox modifyDateHbox = new HBox(10);
        Label dateLabel = new Label("Modify Date Taken");
        dateLabel.setPrefWidth(labelWidth);

        DatePicker modifyDatePicker = new DatePicker();
        modifyDatePicker.setId(DTMID);
        modifyDatePicker.setPromptText("Select date...");
        modifyDatePicker.setPrefWidth(300);
        modifyDatePicker.setMaxWidth(300);

        modifyDateHbox.getChildren().addAll(dateLabel, modifyDatePicker, fillRow());

        // Assembly
        VBox contentPane = new VBox(12);
        contentPane.setPadding(new Insets(10));
        contentPane.getChildren().addAll(sourceHbox, targetHbox, prefixHbox, modifyDateHbox);

        TitledPane titledPane = new TitledPane("Input Options", contentPane);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.setFocusTraversable(false);
        GridPane.setHgrow(titledPane, Priority.ALWAYS);

        pane.add(titledPane, 0, 0);
    }

    private void addMiddlePane(GridPane pane)
    {
        TitledPane optionsTitledPane = new TitledPane();
        optionsTitledPane.setText("Processing Options");

        CheckBox embedDateTimeCheck = new CheckBox("Embed Date/Time");
        embedDateTimeCheck.setId(EMBID);

        CheckBox forceDateChangeCheck = new CheckBox("Force Date Change");
        forceDateChangeCheck.setId(FORID);

        CheckBox skipVideoCheck = new CheckBox("Skip Video Files");
        skipVideoCheck.setId(SKPID);
        skipVideoCheck.setSelected(true);

        CheckBox showMetadataCheck = new CheckBox("Display Metadata");
        showMetadataCheck.setId(METAID);

        CheckBox descendingCheck = new CheckBox("Sort Descending");
        descendingCheck.setId(SRTID);

        CheckBox debugCheck = new CheckBox("Enable Debugging");
        debugCheck.setId(DBGID);

        CheckBox[] processingChecks = new CheckBox[] {embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck, descendingCheck, debugCheck
        };

        for (int i = 0; i < processingChecks.length; i++)
        {
            processingChecks[i].disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        VBox leftCol = new VBox(10, embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck);
        VBox rightCol = new VBox(10, showMetadataCheck, descendingCheck, debugCheck);

        HBox checkBoxPane = new HBox(15, leftCol, rightCol);
        checkBoxPane.setPadding(new Insets(10, 5, 10, 5));

        optionsTitledPane.setContent(checkBoxPane);
        optionsTitledPane.setCollapsible(false);
        optionsTitledPane.setFocusTraversable(false);
        optionsTitledPane.setMaxWidth(Double.MAX_VALUE);
        optionsTitledPane.setMaxHeight(Double.MAX_VALUE);

        // Statistics Pane
        TitledPane statsTitledPane = new TitledPane();
        statsTitledPane.setText("Statistics");

        VBox statPane = new VBox(8);
        statPane.setPadding(new Insets(10));

        statsTitledPane.setContent(statPane);
        statsTitledPane.setCollapsible(false);
        statsTitledPane.setFocusTraversable(false);
        statsTitledPane.setMaxWidth(Double.MAX_VALUE);
        statsTitledPane.setMaxHeight(Double.MAX_VALUE);

        HBox middleRow = new HBox(15, optionsTitledPane, statsTitledPane);
        GridPane.setHgrow(middleRow, Priority.ALWAYS);

        optionsTitledPane.prefWidthProperty().bind(middleRow.widthProperty().subtract(15).divide(2));
        statsTitledPane.prefWidthProperty().bind(middleRow.widthProperty().subtract(15).divide(2));

        pane.add(middleRow, 0, 1);
    }

    private void addLogPane(GridPane pane)
    {
        TextArea logArea = new TextArea();
        logArea.setId(LOGID);
        logArea.setEditable(false);
        logArea.setFocusTraversable(false);
        logArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
        logArea.setPromptText("Console output...");
        logArea.setMaxWidth(Double.MAX_VALUE);
        logArea.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        VBox logContent = new VBox(logArea);

        TitledPane titledPane = new TitledPane("Execution Log", logContent);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.setFocusTraversable(false);

        GridPane.setHgrow(titledPane, Priority.ALWAYS);
        GridPane.setVgrow(titledPane, Priority.ALWAYS);

        pane.add(titledPane, 0, 2);
    }

    private void addControlPane(GridPane pane)
    {
        Button actionBtn = new Button("Run Batch Process");
        actionBtn.setId(ACTID);
        actionBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                executeBatchProcess();
            }
        });

        ProgressBar progressBar = new ProgressBar(0.0);
        progressBar.setId(PRGID);
        progressBar.setPrefWidth(220);
        progressBar.prefHeightProperty().bind(actionBtn.heightProperty());

        Button viewBtn = new Button("View Summary...");
        viewBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                showSummaryDialog(stage);
            }
        });
        viewBtn.prefHeightProperty().bind(actionBtn.heightProperty());

        HBox buttonBox = new HBox(15, actionBtn, progressBar, fillRow(), viewBtn);
        buttonBox.setPadding(new Insets(10));

        TitledPane titledPane = new TitledPane("Actions", buttonBox);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.setFocusTraversable(false);

        GridPane.setHgrow(titledPane, Priority.ALWAYS);

        pane.add(titledPane, 0, 3);
    }

    private void addBottomPane(GridPane pane)
    {
        Button cancelBtn = new Button("Cancel Process");
        cancelBtn.setId(CANID);
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                cancelBatchProcess();
            }
        });

        Button clearLogBtn = new Button("Clear Log");
        clearLogBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                TextArea logArea = getById(stage.getScene().getRoot(), LOGID);
                if (logArea != null)
                {
                    logArea.clear();
                }
            }
        });

        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                Platform.exit();
            }
        });

        HBox controlLayout = new HBox(10, cancelBtn, clearLogBtn, fillRow(), exitBtn);
        controlLayout.setPadding(new Insets(5, 0, 0, 0));

        GridPane.setHgrow(controlLayout, Priority.ALWAYS);

        pane.add(controlLayout, 0, 4);
    }

    private void configureDynamicNodes(Parent root)
    {
        TextField prefixText = getById(root, PFXID);
        DatePicker modifyDatePicker = getById(root, DTMID);
        CheckBox showMetadataCheck = getById(root, METAID);
        final Button actionBtn = getById(root, ACTID);

        if (prefixText != null && showMetadataCheck != null)
        {
            prefixText.disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        if (modifyDatePicker != null && showMetadataCheck != null)
        {
            modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        if (showMetadataCheck != null && actionBtn != null)
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
        }
    }

    private void executeBatchProcess()
    {
        Parent root = stage.getScene().getRoot();
        final TextArea logArea = getById(root, LOGID);
        final Button actionBtn = getById(root, ACTID);
        final Button cancelBtn = getById(root, CANID);
        CheckBox showMetadataCheck = getById(root, METAID);
        ProgressBar progressBar = getById(root, PRGID);

        final boolean metaDisplay = (showMetadataCheck != null && showMetadataCheck.isSelected());

        if (logArea != null)
        {
            logArea.clear();
            logArea.appendText("[INFO] Initializing batch process...\n");
        }

        if (actionBtn != null) actionBtn.setDisable(true);
        if (cancelBtn != null) cancelBtn.setDisable(false);

        final BatchConfiguration config;
        try
        {
            config = buildConfiguration();
        }
        catch (BatchErrorException exc)
        {
            if (logArea != null) logArea.appendText("[ERROR] Configuration error: " + exc.getMessage() + "\n");
            if (actionBtn != null) actionBtn.setDisable(false);
            if (cancelBtn != null) cancelBtn.setDisable(true);
            return;
        }

        final ProgressBar finalProgressBar = progressBar;

        Thread workerThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (metaDisplay)
                    {
                        DisplayMetadata display = new DisplayMetadata(config);
                        display.execute();

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (logArea != null) logArea.appendText("\n[SUCCESS] Exif data retrieved successfully.\n");
                            }
                        });
                    }
                    else
                    {
                        MediaBatchProcessor processor = new MediaBatchProcessor(config);
                        if (finalProgressBar != null)
                        {
                            processor.addProgressListener(new JavaFXProgressAdapter(finalProgressBar));
                        }
                        processor.execute();

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (logArea != null) logArea.appendText("\n[SUCCESS] Batch processing complete.\n");
                            }
                        });
                    }
                }
                catch (final BatchErrorException exc)
                {
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (logArea != null) logArea.appendText("[ERROR] " + exc.getMessage() + "\n");
                        }
                    });
                }
                catch (final Exception exc)
                {
                    exc.printStackTrace();
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (logArea != null) logArea.appendText("[ERROR] Unexpected error: " + exc.getMessage() + "\n");
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
                            if (cancelBtn != null) cancelBtn.setDisable(true);
                            if (actionBtn != null) actionBtn.setDisable(false);
                        }
                    });
                }
            }
        });

        workerThread.setDaemon(true);
        workerThread.start();
    }

    private BatchConfiguration buildConfiguration() throws BatchErrorException
    {
        Parent root = stage.getScene().getRoot();

        TextField sourceText = getById(root, SRCID);
        TextField targetText = getById(root, TGTID);
        TextField prefixText = getById(root, PFXID);
        DatePicker modifyDatePicker = getById(root, DTMID);

        CheckBox embedDateTime = getById(root, EMBID);
        CheckBox forceDateChange = getById(root, FORID);
        CheckBox skipVideo = getById(root, SKPID);
        CheckBox showMetadata = getById(root, METAID);
        CheckBox descending = getById(root, SRTID);
        CheckBox debug = getById(root, DBGID);

        String sourcePath = (sourceText != null ? sourceText.getText() : null);
        String targetPath = (targetText != null ? targetText.getText() : null);
        String prefixValue = (prefixText != null ? prefixText.getText() : null);

        LocalDate dateValue = (modifyDatePicker != null ? modifyDatePicker.getValue() : null);
        String userDateStr = (dateValue != null ? dateValue.toString() : null);

        BatchBuilder builder = new BatchBuilder()
                .source(sourcePath)
                .target(targetPath)
                .prefix(prefixValue)
                .userDate(userDateStr)
                .showMetadata(showMetadata != null && showMetadata.isSelected());

        if (embedDateTime != null) builder.embedDateTime(embedDateTime.isSelected());
        if (forceDateChange != null) builder.forceDateChange(forceDateChange.isSelected());
        if (skipVideo != null) builder.skipVideo(skipVideo.isSelected());
        if (descending != null) builder.descending(descending.isSelected());
        if (debug != null) builder.debug(debug.isSelected());

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> T getById(Node root, String id)
    {
        if (root != null && id != null)
        {
            if (id.equals(root.getId()))
            {
                return (T) root;
            }
            else if (root instanceof Parent)
            {
                ObservableList<Node> nodes = ((Parent) root).getChildrenUnmodifiable();
                for (Node child : nodes)
                {
                    T result = getById(child, id);
                    if (result != null)
                    {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private void cancelBatchProcess()
    {
        TextArea logArea = getById(stage.getScene().getRoot(), LOGID);
        if (logArea != null)
        {
            logArea.appendText("[WARNING] Batch process cancellation requested.\n");
        }
    }

    private void handleFileSelection(TextField sourceText)
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Source Files");

        File defaultDir = new File("E:\\ImageBatchDir");
        if (defaultDir.exists() && defaultDir.isDirectory())
        {
            chooser.setInitialDirectory(defaultDir);
        }

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
        Dialog<Void> dialog = new Dialog<Void>();
        dialog.setTitle("Batch Processing Summary");
        dialog.setHeaderText("Detailed Processing Results");
        dialog.initOwner(ownerWindow);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<FileRecord> table = new TableView<FileRecord>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FileRecord, String> sourceCol = new TableColumn<FileRecord, String>("Source File");
        sourceCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileRecord, String> cellData)
            {
                return cellData.getValue().sourceNameProperty();
            }
        });
        sourceCol.setPrefWidth(200);

        TableColumn<FileRecord, String> targetCol = new TableColumn<FileRecord, String>("Target File");
        targetCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileRecord, String> cellData)
            {
                return cellData.getValue().targetNameProperty();
            }
        });
        targetCol.setPrefWidth(200);

        TableColumn<FileRecord, String> statusCol = new TableColumn<FileRecord, String>("Status");
        statusCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileRecord, String>, ObservableValue<String>>()
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

    private Region fillRow()
    {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private class DirectoryPopupHandler implements EventHandler<ActionEvent>
    {
        private final TextField targetField;
        private final String dialogTitle;

        public DirectoryPopupHandler(TextField text, String title)
        {
            this.targetField = text;
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

    public static void main(String[] args)
    {
        launch(args);
    }
}
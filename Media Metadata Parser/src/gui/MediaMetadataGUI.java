package gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.BatchMetrics;
import batch.BatchProcessEvent;
import common.DigitalSignature;
import common.PropertyConsumer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.*;

/**
 * Provides the JavaFX graphical user interface for configuring and running batch media metadata
 * processing operations.
 */
public class MediaMetadataGUI extends Application implements EventHandler<ActionEvent>
{
    private GridPane rootPane;
    private BatchTask workerTask;
    private MainViewPane viewPane;
    private StringBuilder flatExtractedRecords;
    private ObservableList<FileProcessingRecord> fileRecords;
    private ObservableList<FileMetadataRecord> treeExtractedRecords;

    /**
     * Initialises state components prior to scene setup.
     */
    @Override
    public void init()
    {
        viewPane = new MainViewPane();
        flatExtractedRecords = new StringBuilder();
        fileRecords = FXCollections.observableArrayList();
        treeExtractedRecords = FXCollections.observableArrayList();
    }

    /**
     * Constructs and displays the main application window.
     *
     * @param primaryStage
     *        the primary application window stage
     */
    @Override
    public void start(Stage primaryStage)
    {
        // Define explicit row layout behavior for the main grid container
        RowConstraints fixedRow = new RowConstraints();
        fixedRow.setVgrow(Priority.NEVER);

        RowConstraints fillRow = new RowConstraints();
        fillRow.setVgrow(Priority.ALWAYS);

        rootPane = new GridPane();
        rootPane.setHgap(10);
        rootPane.setVgap(10);
        rootPane.requestFocus();
        rootPane.setPadding(new Insets(15));
        rootPane.getRowConstraints().addAll(fixedRow, fixedRow, fillRow, fixedRow, fixedRow);

        viewPane.buildLayout(rootPane);

        Scene scene = new Scene(rootPane, 620, 650);
        scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());

        primaryStage.setTitle("Image Metadata Structure Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();

        configureDynamicNodes();
    }

    /**
     * Handles cleanup and configuration saving when shutting down the stage.
     */
    @Override
    public void stop()
    {
        TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);
        TextField targetText = GUIUtils.getById(rootPane, MainViewPane.TGTID, TextField.class);

        try
        {
            PathHistoryStore.saveSettings(sourceText, targetText);
        }

        catch (IOException exc)
        {
            System.err.println("Unable to save path history information: " + exc.getMessage());
        }
    }

    /**
     * Handles action events from user interface buttons.
     *
     * @param event
     *        the triggered event to react to
     */
    @Override
    public void handle(ActionEvent event)
    {
        Object source = event.getSource();

        if (source == viewPane.sourceBtn)
        {
            ContextMenu menu = (ContextMenu) viewPane.sourceBtn.getUserData();

            if (menu != null)
            {
                menu.show(viewPane.sourceBtn, Side.BOTTOM, 0, 0);
            }
        }

        else if (source == viewPane.actionBtn)
        {
            CheckBox showMetadata = GUIUtils.getById(rootPane, MainViewPane.SHWID, CheckBox.class);

            if (showMetadata.isSelected())
            {
                executeMetadataInspection();
            }

            else
            {
                executeBatchProcess();
            }
        }

        else if (source == viewPane.copyLogBtn)
        {
            copyTextAreaWithFlash((TextArea) viewPane.clearLogBtn.getUserData());
        }

        else if (source == viewPane.abortBtn)
        {
            if (workerTask != null)
            {
                workerTask.cancel(true);
            }
        }

        else if (source == viewPane.viewBtn)
        {
            CheckBox showMetadata = GUIUtils.getById(rootPane, MainViewPane.SHWID, CheckBox.class);

            if (showMetadata.isSelected())
            {
                showMetadataInspectorTree();
            }

            else
            {
                showSummaryDialog();
            }
        }

        else if (source == viewPane.clearLogBtn)
        {
            TextArea logArea = (TextArea) viewPane.clearLogBtn.getUserData();

            if (logArea != null)
            {
                logArea.clear();
            }
        }

        else if (source == viewPane.exitBtn)
        {
            Platform.exit();
        }
    }

    /**
     * Triggers non-destructive background metadata structure extraction task.
     */
    private void executeMetadataInspection()
    {
        BatchConfiguration config;
        ProgressBar progressBar = viewPane.progressBar;
        final Label progressLabel = (Label) progressBar.getUserData();
        TextArea logArea = (TextArea) viewPane.clearLogBtn.getUserData();

        logArea.clear();
        treeExtractedRecords.clear();
        flatExtractedRecords.setLength(0);

        try
        {
            config = new ConfigurationBuilder(rootPane).build();
        }

        catch (BatchErrorException exc)
        {
            progressLabel.setText("Configuration error");
            GUIUtils.launchPopup("Configuration Error", exc.getMessage(), AlertType.ERROR);
            return;
        }

        workerTask = new BatchTask(config, logArea, progressBar, true);

        // Stream raw metadata text directly into flatExtractedRecords as DisplayMetadata emits it
        workerTask.setOnMetadataReceived(new Consumer<String>()
        {
            @Override
            public void accept(String text)
            {
                flatExtractedRecords.append(text);
            }
        });

        // Populate POJO records directly useful for GUI display
        workerTask.setOnRecordExtracted(new Consumer<FileMetadataRecord>()
        {
            @Override
            public void accept(FileMetadataRecord record)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        treeExtractedRecords.add(record);
                    }
                });
            }
        });

        workerTask.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                showMetadataInspectorTree();
                resetControlStates(progressLabel);
            }
        });

        workerTask.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                String msg;
                Throwable exc = workerTask.getException();

                if (exc != null && exc.getMessage() == null && exc.getCause() != null)
                {
                    exc = exc.getCause();
                }

                if (exc != null && exc.getMessage() != null && !exc.getMessage().trim().isEmpty())
                {
                    msg = exc.getMessage();
                }

                else
                {
                    msg = "An unexpected error occurred during metadata extraction.";
                }

                resetControlStates(progressLabel);
                GUIUtils.launchPopup("Metadata Extraction Error", msg, AlertType.ERROR);
            }
        });

        workerTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                resetControlStates(progressLabel);
            }
        });

        viewPane.actionBtn.setDisable(true);
        viewPane.abortBtn.setDisable(false);
        viewPane.copyLogBtn.setDisable(true);
        progressLabel.textProperty().bind(workerTask.messageProperty());

        Thread worker = new Thread(workerTask);
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Initiates the standard asynchronous batch modification process task.
     */
    private void executeBatchProcess()
    {
        final BatchConfiguration config;
        final ProgressBar progressBar = viewPane.progressBar;
        final TextArea logArea = (TextArea) viewPane.clearLogBtn.getUserData();
        final Label progressLabel = (Label) progressBar.getUserData();

        logArea.clear();
        fileRecords.clear();
        StatRecord.resetAll();

        try
        {
            config = new ConfigurationBuilder(rootPane).build();
        }

        catch (BatchErrorException exc)
        {
            progressLabel.setText("Configuration error");
            GUIUtils.launchPopup("Invalid File Selection", exc.getMessage(), AlertType.ERROR);
            return;
        }

        workerTask = new BatchTask(config, logArea, progressBar, false);

        // Receive file execution output records for tabular summary reporting
        workerTask.setOnFileSummaryListener(new PropertyConsumer()
        {
            @Override
            public void accept(String key, Object value)
            {
                if (value instanceof BatchProcessEvent)
                {
                    BatchProcessEvent event = (BatchProcessEvent) value;

                    final String source = event.getSourceName();
                    final String target = event.getTargetName();
                    final DigitalSignature magic = event.getDigitalSignature();
                    final String status = event.isSuccess() ? "Completed" : "Failed";
                    final long size = event.getTargetSize();

                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            fileRecords.add(new FileProcessingRecord(source, target, magic, status, size));
                        }
                    });
                }
            }
        });

        // Update scanned source file count in the metrics table
        workerTask.setOnFileScanned(new Consumer<Integer>()
        {
            @Override
            public void accept(Integer count)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        StatRecord.SOURCE_FILES.setValue(count);
                    }
                });
            }
        });

        // Update processed target file count in the metrics table
        workerTask.setOnFileProcessed(new Consumer<Integer>()
        {
            @Override
            public void accept(Integer count)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        StatRecord.TARGET_FILES.setValue(count);
                    }
                });
            }
        });

        // Update final metrics when processing completes
        workerTask.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                BatchMetrics stats = workerTask.getValue();

                if (stats != null)
                {
                    StatRecord.SOURCE_FILES.setValue(stats.getScanned());
                    StatRecord.TARGET_FILES.setValue(stats.getProcessed());
                    StatRecord.FILES_SKIPPED.setValue(stats.getFilesSkippedCount());
                    StatRecord.TOTAL_SIZE.setValue(String.format("%.2f MB", stats.getTotalTargetSizeMB()));
                }

                resetControlStates(progressLabel);
                viewPane.viewBtn.fire();
                GUIUtils.launchPopup("Process Complete", "Batch processing completed", AlertType.INFORMATION);
            }
        });

        workerTask.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                Throwable exc = workerTask.getException();
                String msg = (exc != null && exc.getMessage() != null ? exc.getMessage() : "An unknown error occurred.");

                resetControlStates(progressLabel);
                GUIUtils.launchPopup("Processing Error", msg, AlertType.ERROR);
            }
        });

        workerTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                resetControlStates(progressLabel);
            }
        });

        viewPane.actionBtn.setDisable(true);
        viewPane.abortBtn.setDisable(false);
        viewPane.copyLogBtn.setDisable(true);
        progressLabel.textProperty().bind(workerTask.messageProperty());

        Thread worker = new Thread(workerTask);
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Renders detailed non-modal popup dialog summarising execution record entries.
     */
    private void showSummaryDialog()
    {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch Processing Summary");
        dialog.setHeaderText("Detailed Processing Results");
        dialog.initModality(Modality.NONE);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<FileProcessingRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Define dynamic numerical row index column
        TableColumn<FileProcessingRecord, Void> indexCol = new TableColumn<>("#");

        indexCol.setCellFactory(new Callback<TableColumn<FileProcessingRecord, Void>, TableCell<FileProcessingRecord, Void>>()
        {
            @Override
            public TableCell<FileProcessingRecord, Void> call(TableColumn<FileProcessingRecord, Void> param)
            {
                return new TableCell<FileProcessingRecord, Void>()
                {
                    @Override
                    protected void updateItem(Void item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        if (empty || getTableRow() == null || getTableRow().getItem() == null)
                        {
                            setText(null);
                        }

                        else
                        {
                            setText(String.valueOf(getIndex() + 1));
                        }
                    }
                };
            }
        });

        indexCol.setMinWidth(30);
        indexCol.setMaxWidth(30);
        indexCol.setPrefWidth(30);
        indexCol.setResizable(false);
        indexCol.setStyle("-fx-alignment: CENTER;");

        // Source filename column
        TableColumn<FileProcessingRecord, String> sourceCol = new TableColumn<>("Source File");

        sourceCol.setPrefWidth(190);
        sourceCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileProcessingRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileProcessingRecord, String> cellData)
            {
                return cellData.getValue().sourceNameProperty();
            }
        });

        // Target filename column
        TableColumn<FileProcessingRecord, String> targetCol = new TableColumn<>("Target File");

        targetCol.setPrefWidth(190);
        targetCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileProcessingRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileProcessingRecord, String> cellData)
            {
                return cellData.getValue().targetNameProperty();
            }
        });

        // Target file byte size column with formatted units display
        TableColumn<FileProcessingRecord, Long> sizeCol = new TableColumn<>("File Size");

        sizeCol.setPrefWidth(100);
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileProcessingRecord, Long>, ObservableValue<Long>>()
        {
            @Override
            public ObservableValue<Long> call(TableColumn.CellDataFeatures<FileProcessingRecord, Long> cellData)
            {
                return new ReadOnlyObjectWrapper<>(cellData.getValue().getFileSize());
            }
        });

        sizeCol.setCellFactory(new Callback<TableColumn<FileProcessingRecord, Long>, TableCell<FileProcessingRecord, Long>>()
        {
            @Override
            public TableCell<FileProcessingRecord, Long> call(TableColumn<FileProcessingRecord, Long> param)
            {
                return new TableCell<FileProcessingRecord, Long>()
                {
                    @Override
                    protected void updateItem(Long item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        if (empty || item == null)
                        {
                            setText(null);
                        }

                        else if (item <= 0)
                        {
                            setText("0 B");
                        }

                        else
                        {
                            String[] units = {"B", "KB", "MB", "GB", "TB"};
                            int digitGroups = (int) (Math.log10(item) / Math.log10(1024));

                            digitGroups = Math.min(digitGroups, units.length - 1);
                            setText(new DecimalFormat("#,##0.#").format(item / Math.pow(1024, digitGroups)) + " " + units[digitGroups]);
                        }
                    }
                };
            }
        });

        table.getColumns().add(indexCol);
        table.getColumns().add(sourceCol);
        table.getColumns().add(targetCol);
        table.getColumns().add(sizeCol);
        table.setItems(fileRecords);

        Path targetDir = null;
        TextField targetText = GUIUtils.getById(rootPane, MainViewPane.TGTID, TextField.class);

        if (targetText != null && !targetText.getText().trim().isEmpty())
        {
            try
            {
                targetDir = Paths.get(targetText.getText().trim()).toAbsolutePath();
            }

            catch (InvalidPathException exc)
            {
                // Fall back to null if target path string cannot be parsed
            }
        }

        final ImagePreviewPopup thumbnail = new ImagePreviewPopup(dialog.getDialogPane().getScene().getWindow(), targetDir);

        // Attach hover image thumb-nail listeners on rows
        table.setRowFactory(new Callback<TableView<FileProcessingRecord>, TableRow<FileProcessingRecord>>()
        {
            @Override
            public TableRow<FileProcessingRecord> call(TableView<FileProcessingRecord> param)
            {
                final TableRow<FileProcessingRecord> row = new TableRow<>();

                row.setOnMouseEntered(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        if (!row.isEmpty())
                        {
                            row.setStyle("-fx-background-color: #0078d7; -fx-text-background-color: white;");
                            thumbnail.showPreview(row.getItem(), event.getScreenX(), event.getScreenY());
                        }
                    }
                });

                row.setOnMouseExited(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        row.setStyle("");
                        thumbnail.hide();
                    }
                });

                return row;
            }
        });

        // Ensure newly appending rows pull scrolling view downward automatically
        fileRecords.addListener(new ListChangeListener<FileProcessingRecord>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends FileProcessingRecord> change)
            {
                while (change.next())
                {
                    if (change.wasAdded() && !fileRecords.isEmpty())
                    {
                        table.scrollTo(fileRecords.size() - 1);
                    }
                }
            }
        });

        dialog.setOnCloseRequest(new EventHandler<DialogEvent>()
        {
            @Override
            public void handle(DialogEvent event)
            {
                thumbnail.hide();
            }
        });

        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefSize(550, 320);
        dialog.show();
    }

    /**
     * Binds control events, property listeners, and state dependencies.
     */
    private void configureDynamicNodes()
    {
        final TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);
        final TextField targetText = GUIUtils.getById(rootPane, MainViewPane.TGTID, TextField.class);
        final TextField prefixText = GUIUtils.getById(rootPane, MainViewPane.PFXID, TextField.class);
        final CheckBox embedDateTimeCheck = GUIUtils.getById(rootPane, MainViewPane.EMBID, CheckBox.class);
        final DatePicker modifyDatePicker = GUIUtils.getById(rootPane, MainViewPane.DTMID, DatePicker.class);
        final CheckBox showMetadataCheck = GUIUtils.getById(rootPane, MainViewPane.SHWID, CheckBox.class);

        try
        {
            PathHistoryStore.loadSettings(sourceText, targetText);
        }

        catch (IOException exc)
        {
            String errmsg = "Unable to load path history information from properties due to an error.\n\n" + exc.getMessage();
            GUIUtils.launchPopup("Configuration Error", errmsg, AlertType.ERROR);
        }

        // Primary mouse click opens folder picker menu directly
        sourceText.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                if (event.getButton() == MouseButton.PRIMARY)
                {
                    viewPane.sourceBtn.fire();
                }
            }
        });

        // Auto-trim white spaces when focus leaves the path input
        sourceText.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal)
            {
                if (!newVal)
                {
                    sourceText.setText(sourceText.getText().trim());
                }
            }
        });

        // Custom path validation intercept for system clipboard paste events
        sourceText.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                handleSourcePaste(event, sourceText);
            }
        });

        // Dynamic output filename target preview updates
        InvalidationListener previewListener = new InvalidationListener()
        {
            @Override
            public void invalidated(Observable observable)
            {
                viewPane.updatePreview(rootPane);
            }
        };

        prefixText.disableProperty().bind(showMetadataCheck.selectedProperty());
        modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());

        prefixText.textProperty().addListener(previewListener);
        embedDateTimeCheck.selectedProperty().addListener(previewListener);
        modifyDatePicker.valueProperty().addListener(previewListener);

        // Adjust display button names according to execution mode toggle
        showMetadataCheck.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldVal, Boolean newVal)
            {
                boolean isMetadata = newVal;

                viewPane.viewBtn.setText(isMetadata ? "List Metadata" : "View Summary");
                viewPane.actionBtn.setText(isMetadata ? "Display Metadata" : "Run Batch Process");
            }
        });

        // Disable summary output triggering until meaningful data structures are ready
        BooleanBinding isBatchRecordsEmpty = Bindings.isEmpty(fileRecords);
        BooleanBinding isMetadataEmpty = Bindings.isEmpty(treeExtractedRecords);
        BooleanBinding isViewDisabled = Bindings.when(showMetadataCheck.selectedProperty()).then(isMetadataEmpty).otherwise(isBatchRecordsEmpty);

        viewPane.viewBtn.disableProperty().bind(isViewDisabled);
        viewPane.updatePreview(rootPane);

        viewPane.sourceBtn.setOnAction(this);
        viewPane.actionBtn.setOnAction(this);
        viewPane.exitBtn.setOnAction(this);
        viewPane.copyLogBtn.setOnAction(this);
        viewPane.clearLogBtn.setOnAction(this);
        viewPane.abortBtn.setOnAction(this);
        viewPane.viewBtn.setOnAction(this);

        populateRecentHistoryMenu();
    }

    /**
     * Processes paste hotkey shortcuts in the source location text field.
     *
     * @param event
     *        the triggered key event
     * @param sourceText
     *        source path text component
     */
    public void handleSourcePaste(KeyEvent event, TextField sourceText)
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
                        GUIUtils.launchPopup("Invalid File Set", msg, AlertType.WARNING);
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
                            GUIUtils.launchPopup("Invalid Path", msg, AlertType.WARNING);
                        }
                    }

                    catch (InvalidPathException exc)
                    {
                        String msg = "The pasted content is not a valid file path:\n\n" + pastedText;
                        GUIUtils.launchPopup("Invalid Path", msg, AlertType.WARNING);
                    }
                }
            }

            event.consume();
        }
    }

    /**
     * Builds and populates the recent paths context menu.
     */
    private void populateRecentHistoryMenu()
    {
        ContextMenu menu = new ContextMenu();
        Button sourceBtn = viewPane.sourceBtn;
        final TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);

        MenuItem selectFolder = new MenuItem("Select Folder...");
        selectFolder.setOnAction(new FilePickHandler(sourceText, "Select Source Directory"));

        MenuItem selectFiles = new MenuItem("Select Specific Files...");
        selectFiles.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                handleFileSelection();
            }
        });

        menu.getItems().addAll(selectFolder, selectFiles, new SeparatorMenuItem());

        try
        {
            List<String> history = PathHistoryStore.loadRecentSourcePaths();

            if (history.isEmpty())
            {
                MenuItem blankItem = new MenuItem("No recent paths");
                blankItem.setDisable(true);
                menu.getItems().add(blankItem);
            }

            else
            {
                for (String entry : history)
                {
                    if (entry == null || entry.isEmpty())
                    {
                        continue;
                    }

                    int pos = entry.indexOf('|');
                    String parentHistory = null;
                    String textHistory;

                    if (pos >= 0)
                    {
                        parentHistory = entry.substring(0, pos);
                        textHistory = entry.substring(pos + 1);
                    }

                    else
                    {
                        textHistory = entry;
                    }

                    final String targetText = textHistory;
                    final String targetParent = parentHistory;
                    MenuItem item = new MenuItem(textHistory);

                    item.setOnAction(new EventHandler<ActionEvent>()
                    {
                        @Override
                        public void handle(ActionEvent event)
                        {
                            sourceText.setText(targetText);

                            if (targetParent != null && !targetParent.isEmpty())
                            {
                                sourceText.setTooltip(new Tooltip(targetParent));
                            }

                            else
                            {
                                sourceText.setTooltip(null);
                            }
                        }
                    });

                    menu.getItems().add(item);
                }
            }

            sourceBtn.setUserData(menu);
        }

        catch (BatchErrorException exc)
        {
            MenuItem blankItem = new MenuItem("Recent paths unknown");
            blankItem.setDisable(true);
            menu.getItems().add(blankItem);
        }
    }

    /**
     * Prompts a file open selection dialog to capture explicit media files.
     */
    private void handleFileSelection()
    {
        TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);
        String actualText = sourceText.getText().trim();
        File sourceDir = new File(actualText.isEmpty() ? System.getProperty("user.home") : actualText);
        FileChooser chooser = new FileChooser();

        chooser.setTitle("Select Source Files");

        if (sourceDir.isDirectory())
        {
            chooser.setInitialDirectory(sourceDir);
        }

        List<File> files = chooser.showOpenMultipleDialog(rootPane.getScene().getWindow());

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
     * Restores main interactive controls from active execution state.
     *
     * @param progressLabel
     *        progress status display text label
     */
    private void resetControlStates(final Label progressLabel)
    {
        final Button actionBtn = viewPane.actionBtn;
        final Button cancelBtn = viewPane.abortBtn;
        final Button copyLogBtn = viewPane.copyLogBtn;
        final ProgressBar progressBar = viewPane.progressBar;

        workerTask = null;
        actionBtn.setDisable(false);
        actionBtn.getScene().getRoot().requestFocus();
        cancelBtn.setDisable(true);
        copyLogBtn.setDisable(false);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));

        delay.setOnFinished(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (progressLabel != null)
                {
                    progressLabel.textProperty().unbind();
                    progressLabel.setText("");
                }

                progressBar.progressProperty().unbind();
                progressBar.setProgress(0.0);
            }
        });

        delay.play();
    }

    /**
     * Shared helper to copy text area contents to system clipboard and trigger visual flash
     * feedback.
     *
     * @param logArea
     *        target text field component
     */
    private void copyTextAreaWithFlash(final TextArea logArea)
    {
        if (logArea != null && !logArea.getText().isEmpty())
        {
            ClipboardContent content = new ClipboardContent();
            content.putString(logArea.getText());
            Clipboard.getSystemClipboard().setContent(content);

            // Apply soft green background highlight visual flash feedback
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
     * Opens modal dialog window displaying structural metadata contents using the interactive
     * TreeTableView inspector.
     */
    private void showMetadataInspectorTree()
    {
        ExtractedMetadataDialog dialog = new ExtractedMetadataDialog((Stage) rootPane.getScene().getWindow());

        dialog.setMetadataRecords(treeExtractedRecords);
        dialog.setMetadataText(flatExtractedRecords.toString());

        flatExtractedRecords.setLength(0);
        flatExtractedRecords.trimToSize();

        dialog.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args
     *        command-line arguments supplied to the application
     */
    public static void main(String[] args)
    {
        launch(args);
    }
}
package gui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

/**
 * Provides the JavaFX graphical user interface for configuring and running batch media metadata
 * processing operations.
 */
public class MediaMetadataGUI extends Application implements EventHandler<ActionEvent>
{
    private GridPane rootPane;
    private BatchTask workerTask;
    private MainViewPane viewPane;
    private StringBuilder flatMetadataText;
    private ObservableList<MediaFileMetadata> extractedMetadata;
    private ObservableList<ProcessedFileRecord> completedFileRecords;

    /**
     * Initialises state components prior to scene setup.
     */
    @Override
    public void init()
    {
        viewPane = new MainViewPane();
        flatMetadataText = new StringBuilder();
        completedFileRecords = FXCollections.observableArrayList();
        extractedMetadata = FXCollections.observableArrayList();
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

        primaryStage.setTitle("Image Metadata Structure Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();

        configureDynamicNodes();
        populateRecentHistoryMenu();
    }

    /**
     * Handles cleanup and configuration saving when shutting down the stage.
     */
    @Override
    public void stop()
    {
        TextField sourceText = UtilsJavaFX.getById(rootPane, MainViewPane.SRCID, TextField.class);
        TextField targetText = UtilsJavaFX.getById(rootPane, MainViewPane.TGTID, TextField.class);
        CheckBox themeBox = UtilsJavaFX.getById(rootPane, MainViewPane.THMID, CheckBox.class);

        try
        {
            PathHistoryStore.saveSettings(sourceText, targetText, themeBox.isSelected());
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
            CheckBox showMetadata = UtilsJavaFX.getById(rootPane, MainViewPane.SHWID, CheckBox.class);

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
            UtilsJavaFX.doFlashCopyTextArea((TextArea) viewPane.clearLogBtn.getUserData());
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
            CheckBox showMetadata = UtilsJavaFX.getById(rootPane, MainViewPane.SHWID, CheckBox.class);

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
        final BatchConfiguration config;
        final ProgressBar progressBar = viewPane.progressBar;
        final Label progressLabel = (Label) progressBar.getUserData();
        final TextArea logArea = (TextArea) viewPane.clearLogBtn.getUserData();

        logArea.clear();
        StatRecord.resetAll();
        extractedMetadata.clear();
        flatMetadataText.setLength(0);

        try
        {
            config = new ConfigurationBuilder(rootPane).build();
        }

        catch (BatchErrorException exc)
        {
            progressLabel.setText("Configuration error");
            UtilsJavaFX.launchPopup(rootPane, "Configuration Error", exc.getMessage(), AlertType.ERROR);
            return;
        }

        workerTask = new BatchTask(config, logArea, progressBar, true);

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

        // Stream metadata text directly into Text Area while DisplayMetadata emits it
        workerTask.setOnMetadataReceived(new Consumer<String>()
        {
            @Override
            public void accept(String text)
            {
                flatMetadataText.append(text);
            }
        });

        // Populate metadata directly into List
        workerTask.setOnRecordExtracted(new Consumer<MediaFileMetadata>()
        {
            @Override
            public void accept(MediaFileMetadata record)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        extractedMetadata.add(record);
                    }
                });
            }
        });

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
                UtilsJavaFX.launchPopup(rootPane, "Metadata Extraction Error", msg, AlertType.ERROR);
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
        completedFileRecords.clear();
        StatRecord.resetAll();

        try
        {
            config = new ConfigurationBuilder(rootPane).build();
        }

        catch (BatchErrorException exc)
        {
            progressLabel.setText("Configuration error");
            UtilsJavaFX.launchPopup(rootPane, "Invalid File Selection", exc.getMessage(), AlertType.ERROR);
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
                            completedFileRecords.add(new ProcessedFileRecord(source, target, magic, status, size));
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
                UtilsJavaFX.launchPopup(rootPane, "Processing Error", msg, AlertType.ERROR);
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

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().addAll(rootPane.getScene().getStylesheets());
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        TableView<ProcessedFileRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Fixed-width Row Index Column (#)
        TableColumn<ProcessedFileRecord, Void> indexCol = new TableColumn<>("#");
        indexCol.setMinWidth(35);
        indexCol.setMaxWidth(35);
        indexCol.setPrefWidth(35);
        indexCol.setResizable(false);
        indexCol.setStyle("-fx-alignment: CENTER;");

        indexCol.setCellFactory(new Callback<TableColumn<ProcessedFileRecord, Void>, TableCell<ProcessedFileRecord, Void>>()
        {
            @Override
            public TableCell<ProcessedFileRecord, Void> call(TableColumn<ProcessedFileRecord, Void> param)
            {
                return new TableCell<ProcessedFileRecord, Void>()
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

        // Dynamic Source Filename Column
        TableColumn<ProcessedFileRecord, String> sourceCol = new TableColumn<>("Source File");
        sourceCol.setMinWidth(150);
        sourceCol.setPrefWidth(210);
        sourceCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ProcessedFileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ProcessedFileRecord, String> cellData)
            {
                return cellData.getValue().sourceNameProperty();
            }
        });

        // Dynamic Target Filename Column
        TableColumn<ProcessedFileRecord, String> targetCol = new TableColumn<>("Target File");
        targetCol.setMinWidth(150);
        targetCol.setPrefWidth(210);
        targetCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ProcessedFileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ProcessedFileRecord, String> cellData)
            {
                return cellData.getValue().targetNameProperty();
            }
        });

        // Fixed-width File Size Column
        TableColumn<ProcessedFileRecord, Long> sizeCol = new TableColumn<>("File Size");
        sizeCol.setMinWidth(90);
        sizeCol.setMaxWidth(90);
        sizeCol.setPrefWidth(90);
        sizeCol.setResizable(false);
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ProcessedFileRecord, Long>, ObservableValue<Long>>()
        {
            @Override
            public ObservableValue<Long> call(TableColumn.CellDataFeatures<ProcessedFileRecord, Long> cellData)
            {
                return new ReadOnlyObjectWrapper<>(cellData.getValue().getFileSize());
            }
        });

        sizeCol.setCellFactory(new Callback<TableColumn<ProcessedFileRecord, Long>, TableCell<ProcessedFileRecord, Long>>()
        {
            @Override
            public TableCell<ProcessedFileRecord, Long> call(TableColumn<ProcessedFileRecord, Long> param)
            {
                return new TableCell<ProcessedFileRecord, Long>()
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
        table.setItems(completedFileRecords);

        Path targetDir = null;
        TextField targetText = UtilsJavaFX.getById(rootPane, MainViewPane.TGTID, TextField.class);

        if (!targetText.getText().trim().isEmpty())
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

        final ImagePreviewPopup thumbnail = new ImagePreviewPopup(dialogPane.getScene().getWindow(), targetDir);

        // Attach hover image thumbnail listeners on rows
        table.setRowFactory(new Callback<TableView<ProcessedFileRecord>, TableRow<ProcessedFileRecord>>()
        {
            @Override
            public TableRow<ProcessedFileRecord> call(TableView<ProcessedFileRecord> param)
            {
                final TableRow<ProcessedFileRecord> row = new TableRow<>();

                row.setOnMouseEntered(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        if (!row.isEmpty())
                        {
                            thumbnail.showPreview(row.getItem(), event.getScreenX(), event.getScreenY());
                        }
                    }
                });

                row.setOnMouseExited(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        thumbnail.hide();
                    }
                });

                return row;
            }
        });

        // Ensure newly appending rows pull scrolling view downward automatically
        completedFileRecords.addListener(new ListChangeListener<ProcessedFileRecord>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends ProcessedFileRecord> change)
            {
                while (change.next())
                {
                    if (change.wasAdded() && !completedFileRecords.isEmpty())
                    {
                        table.scrollTo(completedFileRecords.size() - 1);
                    }
                }
            }
        });

        dialog.setOnCloseRequest(new EventHandler<DialogEvent>()
        {
            @Override
            public void handle(DialogEvent event)
            {
                thumbnail.dispose();
                thumbnail.clearCache();
            }
        });

        dialogPane.setContent(table);
        dialogPane.setPrefSize(570, 320);
        dialog.show();
    }

    /**
     * Binds control events, property listeners, and state dependencies.
     */
    private void configureDynamicNodes()
    {
        final TextField sourceText = UtilsJavaFX.getById(rootPane, MainViewPane.SRCID, TextField.class);
        final TextField targetText = UtilsJavaFX.getById(rootPane, MainViewPane.TGTID, TextField.class);
        final TextField prefixText = UtilsJavaFX.getById(rootPane, MainViewPane.PFXID, TextField.class);
        final CheckBox embedDateTimeCheck = UtilsJavaFX.getById(rootPane, MainViewPane.EMBID, CheckBox.class);
        final DatePicker modifyDatePicker = UtilsJavaFX.getById(rootPane, MainViewPane.DTMID, DatePicker.class);
        final CheckBox showMetadataCheck = UtilsJavaFX.getById(rootPane, MainViewPane.SHWID, CheckBox.class);
        final CheckBox themeCheck = UtilsJavaFX.getById(rootPane, MainViewPane.THMID, CheckBox.class);

        themeCheck.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal)
            {
                switchTheme(newVal.booleanValue() ? "dark.css" : "light.css");
            }
        });

        try
        {
            boolean isDark = PathHistoryStore.loadSettings(sourceText, targetText);

            if (isDark)
            {
                themeCheck.setSelected(true);
            }

            else
            {
                switchTheme("light.css");
            }
        }

        catch (IOException exc)
        {
            String errmsg = "Unable to load path history information from properties due to an error.\n\n" + exc.getMessage();
            UtilsJavaFX.launchPopup(rootPane, "Configuration Error", errmsg, AlertType.ERROR);
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
        BooleanBinding isBatchRecordsEmpty = Bindings.isEmpty(completedFileRecords);
        BooleanBinding isMetadataEmpty = Bindings.isEmpty(extractedMetadata);
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
                        UtilsJavaFX.launchPopup(rootPane, "Invalid File Set", msg, AlertType.WARNING);
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
                            UtilsJavaFX.launchPopup(rootPane, "Invalid Path", msg, AlertType.WARNING);
                        }
                    }

                    catch (InvalidPathException exc)
                    {
                        String msg = "The pasted content is not a valid file path:\n\n" + pastedText;
                        UtilsJavaFX.launchPopup(rootPane, "Invalid Path", msg, AlertType.WARNING);
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
        final ContextMenu menu = new ContextMenu();
        final Button sourceBtn = viewPane.sourceBtn;
        final TextField sourceText = UtilsJavaFX.getById(rootPane, MainViewPane.SRCID, TextField.class);

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
            String[] history = PathHistoryStore.loadRecentSourcePaths();

            if (history.length == 0)
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
        TextField sourceText = UtilsJavaFX.getById(rootPane, MainViewPane.SRCID, TextField.class);
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
        final ProgressBar progressBar = viewPane.progressBar;

        workerTask = null;
        viewPane.actionBtn.setDisable(false);
        viewPane.actionBtn.getScene().getRoot().requestFocus();
        viewPane.abortBtn.setDisable(true);
        viewPane.copyLogBtn.setDisable(false);

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
     * Opens modal dialog window displaying structural metadata contents using the interactive
     * TreeTableView inspector.
     */
    private void showMetadataInspectorTree()
    {
        MetadataViewerDialog dialog = new MetadataViewerDialog((Stage) rootPane.getScene().getWindow());

        dialog.setMetadataRecords(extractedMetadata);
        dialog.setMetadataText(flatMetadataText.toString());

        flatMetadataText.setLength(0);
        flatMetadataText.trimToSize();

        dialog.show();
    }

    /**
     * Switches the active application UI theme by replacing the stylesheets applied to the scene
     * containing the root pane.
     * 
     * <p>
     * The specified theme is loaded and applied to the application. If {@code themeFileName} is
     * {@code null}, no change is made. If the specified theme cannot be found, the current theme
     * remains unchanged and an error is reported.
     * </p>
     *
     * @param themeFileName
     *        the file name of the theme to apply (e.g., {@code "dark-theme.css"}), or {@code null}
     *        to leave the current theme unchanged
     */
    private void switchTheme(String themeFileName)
    {
        Scene scene = rootPane.getScene();

        if (themeFileName != null)
        {
            URL resource = getClass().getResource("/gui/" + themeFileName);

            if (resource != null)
            {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(resource.toExternalForm());
            }

            else
            {
                System.err.println("Theme stylesheet not found: /gui/" + themeFileName);
            }
        }
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
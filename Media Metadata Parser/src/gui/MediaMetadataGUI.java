package gui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import javafx.scene.input.ClipboardContent;
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
 *
 * <p>
 * This class manages the main application window, user input controls, batch-processing tasks,
 * progress updates, processing results, and summary dialogs.
 * </p>
 */
public class MediaMetadataGUI extends Application implements EventHandler<ActionEvent>
{
    private GridPane rootPane;
    private BatchTask workerTask;
    private MainViewPane viewPane;
    private StringProperty extractedMetadata;
    private ObservableList<FileProcessingRecord> fileRecords;

    /**
     * Initialises the application state before the JavaFX application window is launched.
     */
    @Override
    public void init()
    {
        viewPane = new MainViewPane();
        extractedMetadata = new SimpleStringProperty("");
        fileRecords = FXCollections.observableArrayList();
    }

    /**
     * Creates and launches the primary application window.
     *
     * <p>
     * Builds the main layout, configures the scene and stylesheet, attaches action handlers, and
     * configures dynamic user-interface controls.
     * </p>
     *
     * @param primaryStage
     *        the primary stage provided by the JavaFX runtime
     */
    @Override
    public void start(Stage primaryStage)
    {
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
     * Called when the application is stopping. Saves persistent path settings.
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
     * Handles user actions generated by the main application controls. Processes an action event
     * and delegates it to the appropriate application operation.
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
            copyLogText();
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
                showMetadataInspector();
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
     * Processes clipboard content pasted into the source field and updates the associated source
     * path information.
     *
     * @param event
     *        the key event triggered by the paste combination
     * @param sourceText
     *        the target text field control receiving input
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
                    Path parentDir = null;
                    String[] parts = pastedText.split("\\s*,\\s*");

                    for (String part : parts)
                    {
                        try
                        {
                            Path fpath = Paths.get(part).toAbsolutePath();

                            if (Files.isRegularFile(fpath))
                            {
                                parentDir = fpath.getParent();
                                break;
                            }
                        }

                        catch (InvalidPathException exc)
                        {
                            // Ignore invalid paths
                        }
                    }

                    boolean valid = (parentDir != null);

                    if (valid)
                    {
                        for (String part : parts)
                        {
                            try
                            {
                                Path fpath = parentDir.resolve(part);

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
                        sourceText.setUserData(parentDir == null ? null : parentDir.toAbsolutePath());
                    }

                    else
                    {
                        String msg = "One or more pasted files is unknown or not in the same directory:\n\n" + pastedText;
                        GUIUtils.launchPopup("Invalid File Set", msg, AlertType.WARNING);
                    }
                }

                else
                {
                    try
                    {
                        Path fpath = Paths.get(pastedText);

                        if (Files.exists(fpath))
                        {
                            Path fullPath = fpath.toAbsolutePath();
                            Path parent = Files.isDirectory(fullPath) ? fullPath : fullPath.getParent();

                            sourceText.setText(pastedText);
                            sourceText.setTooltip(new Tooltip(pastedText));
                            sourceText.setUserData(parent == null ? null : parent.toAbsolutePath());
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
     * Configures dynamic behaviour and event handlers for the UI controls contained within the
     * main root pane.
     *
     * <p>
     * Configures source-field interactions, file chooser operations, clipboard handling, control
     * bindings, dynamic metadata updates, and populates recent location context menus.
     * </p>
     *
     * <b>Important note for developers:</b>
     * When files are pasted into the source text field, their common parent directory is stored in
     * the field's {@link TextField#getUserData() user data}. This directory is used as the base
     * directory for downstream processing. If a common parent directory cannot be determined,
     * {@code null} is stored instead.
     * </p>
     */
    private void configureDynamicNodes()
    {
        TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);
        TextField targetText = GUIUtils.getById(rootPane, MainViewPane.TGTID, TextField.class);
        TextField prefixText = GUIUtils.getById(rootPane, MainViewPane.PFXID, TextField.class);
        CheckBox embedDateTimeCheck = GUIUtils.getById(rootPane, MainViewPane.EMBID, CheckBox.class);
        DatePicker modifyDatePicker = GUIUtils.getById(rootPane, MainViewPane.DTMID, DatePicker.class);
        CheckBox showMetadataCheck = GUIUtils.getById(rootPane, MainViewPane.SHWID, CheckBox.class);

        try
        {
            PathHistoryStore.loadSettings(sourceText, targetText);
        }

        catch (IOException exc)
        {
            String errmsg = "Unable to load path history information from properties due to an error.\n\n" + exc.getMessage();
            GUIUtils.launchPopup("Configuration Error", errmsg, AlertType.ERROR);
        }

        populateRecentHistoryMenu();

        // Handle context menu attached to source button
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

        // Handle refreshing source text field when it is out of focus
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

        // Handle source parent directory via sourceText.setUserData()
        sourceText.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal)
            {
                if (sourceText.getUserData() != null)
                {
                    Path currentPath = (Path) sourceText.getUserData();

                    if (!currentPath.toString().equals(newVal))
                    {
                        sourceText.setUserData(null);
                    }
                }
            }
        });

        // Handle text paste in source text field, ie file1.jpg, file2.png...
        sourceText.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                handleSourcePaste(event, sourceText);
            }
        });

        // Handle target preview label
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

        // Toggle buttons between batch processing and metadata inspection modes
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

        BooleanBinding isBatchRecordsEmpty = Bindings.isEmpty(fileRecords);
        BooleanBinding isMetadataEmpty = extractedMetadata.isEmpty();
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
     * Populates the Source button's context menu with default selection options
     * (Select Folder, Select Specific Files) and recent paths retrieved from persistent storage.
     */
    private void populateRecentHistoryMenu()
    {
        ContextMenu menu = new ContextMenu();
        Button sourceBtn = viewPane.sourceBtn;
        List<String> recentPaths = PathHistoryStore.loadRecentSourcePaths();
        TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);

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

        if (recentPaths.isEmpty())
        {
            MenuItem emptyItem = new MenuItem("No recent folders");
            emptyItem.setDisable(true);
            menu.getItems().add(emptyItem);
        }

        else
        {
            for (String entry : recentPaths)
            {
                MenuItem item = new MenuItem(entry);

                item.setOnAction(new EventHandler<ActionEvent>()
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        sourceText.setText(entry);
                        sourceText.setTooltip(new Tooltip(entry));

                        try
                        {
                            Path fpath = Paths.get(entry);

                            if (Files.exists(fpath))
                            {
                                Path parent = Files.isDirectory(fpath) ? fpath : fpath.getParent();
                                sourceText.setUserData(parent != null ? parent.toAbsolutePath() : null);
                            }

                            else
                            {
                                sourceText.setUserData(null);
                            }
                        }

                        catch (InvalidPathException exc)
                        {
                            sourceText.setUserData(null);
                        }
                    }
                });

                menu.getItems().add(item);
            }
        }

        sourceBtn.setUserData(menu);
    }

    /**
     * Opens a system {@link FileChooser} dialog allowing users to select multiple input files.
     *
     * <p>
     * When at least one file is selected, the file names are formatted into a comma-separated
     * string and populated into the source text field alongside an updated tooltip.
     * </p>
     */
    private void handleFileSelection()
    {
        TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);
        FileChooser chooser = new FileChooser();
        String actualText = sourceText.getText().trim();

        // Use Path?
        File sourceDir = new File(actualText.isEmpty() ? System.getProperty("user.home") : actualText);

        chooser.setTitle("Select Source Files");

        if (sourceDir.isDirectory())
        {
            chooser.setInitialDirectory(sourceDir);
        }

        List<File> files = chooser.showOpenMultipleDialog(rootPane.getScene().getWindow());

        if (files != null && !files.isEmpty())
        {
            StringJoiner joiner = new StringJoiner(", ");

            for (File file : files)
            {
                joiner.add(file.getName());
            }

            String joined = joiner.toString();

            sourceText.setText(joined);
            sourceText.setTooltip(new Tooltip(joined));
            sourceText.setUserData(files.get(0).toPath().getParent().toAbsolutePath());
        }
    }

    /**
     * Creates, configures, and starts a background batch-processing task.
     *
     * <p>
     * Resets the current processing results, builds the batch configuration from the user-interface
     * settings, establishes listeners for processing progress and results, and starts the task on
     * a background thread.
     * </p>
     *
     * <p>
     * Updates to JavaFX user-interface controls are performed on the JavaFX application thread.
     * </p>
     */
    private void executeBatchProcess()
    {
        BatchConfiguration config;
        Button actionBtn = viewPane.actionBtn;
        Button cancelBtn = viewPane.abortBtn;
        Button copyLogBtn = viewPane.copyLogBtn;
        Button clearLogBtn = viewPane.clearLogBtn;
        ProgressBar progressBar = viewPane.progressBar;
        TextArea logArea = (TextArea) clearLogBtn.getUserData();
        Label progressLabel = (Label) progressBar.getUserData();
        CheckBox showMetadata = GUIUtils.getById(rootPane, MainViewPane.SHWID, CheckBox.class);

        if (logArea != null)
        {
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

            workerTask = new BatchTask(config, logArea, progressBar, showMetadata.isSelected());

            workerTask.setFileSummaryListener(new PropertyConsumer()
            {
                @Override
                public void accept(String key, Object value)
                {
                    // Received from BatchTask
                    if (value instanceof BatchProcessEvent)
                    {
                        BatchProcessEvent event = (BatchProcessEvent) value;

                        long size = event.getTargetSize();
                        String source = event.getSourceName();
                        String target = event.getTargetName();
                        DigitalSignature magic = event.getDigitalSignature();
                        String status = event.isSuccess() ? "Completed" : "Failed";

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

            actionBtn.setDisable(true);
            cancelBtn.setDisable(false);
            copyLogBtn.setDisable(true);
            progressLabel.textProperty().bind(workerTask.messageProperty());

            Thread worker = new Thread(workerTask);
            worker.setDaemon(true);
            worker.start();
        }
    }

    /**
     * Copies the content of the log text area to the system clipboard and briefly highlights the
     * text to provide visual feedback to the user.
     *
     * <p>
     * If the log area contains text, its entire contents are transferred to the system clipboard. A
     * temporary visual flash effect is applied using a {@link PauseTransition} before restoring the
     * original control styling and clearing the selection.
     * </p>
     */
    private void copyLogText()
    {
        TextArea logArea = (TextArea) viewPane.clearLogBtn.getUserData();

        if (logArea != null && !logArea.getText().isEmpty())
        {
            ClipboardContent content = new ClipboardContent();
            content.putString(logArea.getText());
            Clipboard.getSystemClipboard().setContent(content);

            String originalStyle = logArea.getStyle();
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
     * Resets action buttons, progress bars, and status labels after a task finishes, fails, or
     * cancels.
     *
     * @param progressLabel
     *        the UI label displaying task progress text
     */
    private void resetControlStates(Label progressLabel)
    {
        Button actionBtn = viewPane.actionBtn;
        Button cancelBtn = viewPane.abortBtn;
        Button copyLogBtn = viewPane.copyLogBtn;
        ProgressBar progressBar = viewPane.progressBar;

        actionBtn.setDisable(false);
        actionBtn.getScene().getRoot().requestFocus();
        cancelBtn.setDisable(true);
        copyLogBtn.setDisable(false);
        workerTask = null;

        PauseTransition delay = new PauseTransition(Duration.seconds(3));

        delay.setOnFinished(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                progressLabel.textProperty().unbind();
                progressBar.progressProperty().unbind();
                progressLabel.setText("");
                progressBar.setProgress(0.0);
            }
        });

        delay.play();
    }

    /**
     * Opens a summary dialog containing details and processing statuses for all processed files.
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
                        
                        else
                        {
                            setText(formatFileSize(item));
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

        // Retrieve current target path from UI
        TextField targetText = GUIUtils.getById(rootPane, MainViewPane.TGTID, TextField.class);
        Path targetDir = null;

        if (targetText != null && !targetText.getText().trim().isEmpty())
        {
            try
            {
                targetDir = Paths.get(targetText.getText().trim()).toAbsolutePath();
            }

            catch (InvalidPathException exc)
            {
                // Fall back to null if invalid
            }
        }

        ImagePreviewPopup previewPopup = new ImagePreviewPopup(dialog.getDialogPane().getScene().getWindow(), targetDir);

        table.setRowFactory(new Callback<TableView<FileProcessingRecord>, TableRow<FileProcessingRecord>>()
        {
            @Override
            public TableRow<FileProcessingRecord> call(TableView<FileProcessingRecord> param)
            {
                final TableRow<FileProcessingRecord> row = new TableRow<>();

                // Trigger popup when mouse moves over row
                row.setOnMouseEntered(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        if (!row.isEmpty())
                        {
                            previewPopup.showPreview(row.getItem(), event.getScreenX(), event.getScreenY());
                        }
                    }
                });

                // Hide popup when mouse exits row
                row.setOnMouseExited(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        previewPopup.hide();
                    }
                });

                return row;
            }
        });

        // Auto-scroll listener to follow live updates
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

        // Ensure popup closes when summary dialog closes
        dialog.setOnCloseRequest(new EventHandler<DialogEvent>()
        {
            @Override
            public void handle(DialogEvent event)
            {
                previewPopup.hide();
            }
        });

        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefSize(550, 320);
        dialog.show();
    }

    /**
     * Helper method to convert byte counts into readable string formats.
     */
    private String formatFileSize(long bytes)
    {
        if (bytes <= 0)
        {
            return "0 B";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        return new java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /* List Metadata to emulate exiftool.exe -G1 -s -u */

    /**
     * Configures and launches an asynchronous background task to extract media metadata.
     *
     * <p>
     * Aggregates emitted metadata snippets in a background buffer and updates
     * {@link #extractedMetadata} upon completion before opening the inspector window.
     * </p>
     */
    private void executeMetadataInspection()
    {
        BatchConfiguration config;
        StringBuilder sb = new StringBuilder();
        ProgressBar progressBar = viewPane.progressBar;
        Label progressLabel = (Label) progressBar.getUserData();
        TextArea logArea = (TextArea) viewPane.clearLogBtn.getUserData();

        if (logArea != null)
        {
            logArea.clear();

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

            workerTask.setOnMetadataReceived(new Consumer<String>()
            {
                @Override
                public void accept(final String text)
                {
                    sb.append(text);
                }
            });

            workerTask.setOnSucceeded(new EventHandler<WorkerStateEvent>()
            {
                @Override
                public void handle(WorkerStateEvent event)
                {
                    extractedMetadata.set(sb.toString());
                    showMetadataInspector();
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
    }

    /**
     * Constructs and displays the Metadata Inspector dialog window.
     *
     * <p>
     * The inspector's text display is bound directly to {@link #extractedMetadata},
     * ensuring it dynamically updates whenever background extraction tasks complete.
     * </p>
     */
    private void showMetadataInspector()
    {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.NONE);
        dialog.setTitle("Extracted Media Metadata");
        dialog.setHeaderText("Discovered Structure Attributes");

        ButtonType exportBtnType = new ButtonType("Export to File");
        dialog.getDialogPane().getButtonTypes().addAll(exportBtnType, ButtonType.CLOSE);

        TextArea textDisplay = new TextArea();
        textDisplay.setEditable(false);
        textDisplay.setWrapText(false);
        textDisplay.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");

        // Bind text directly to the observable property
        textDisplay.textProperty().bind(extractedMetadata);

        GridPane content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);
        content.setPadding(new Insets(10));
        content.add(textDisplay, 0, 0);

        GridPane.setHgrow(textDisplay, Priority.ALWAYS);
        GridPane.setVgrow(textDisplay, Priority.ALWAYS);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(700, 500);

        Button exportBtn = (Button) dialog.getDialogPane().lookupButton(exportBtnType);

        exportBtn.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                exportMetadataToFile(textDisplay.getText());
                event.consume();
            }
        });

        dialog.show();
    }

    /**
     * Opens a system {@link FileChooser} dialog to save extracted metadata to disk.
     *
     * @param content
     *        the formatted text payload to write to the file
     */
    private void exportMetadataToFile(String content)
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Metadata to File");
        chooser.setInitialFileName("metadata_output.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));

        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null)
        {
            try
            {
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

                Files.write(file.toPath(), bytes);
                GUIUtils.launchPopup("Success", "Metadata exported successfully to:\n" + file.getAbsolutePath(), AlertType.INFORMATION);
            }

            catch (IOException exc)
            {
                GUIUtils.launchPopup("Error", "Failed to save metadata file: " + exc.getMessage(), AlertType.ERROR);
            }
        }
    }
}
package gui;

import java.io.IOException;
import java.util.function.Consumer;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.BatchMetrics;
import batch.BatchProcessEvent;
import batch.MetadataInspectionEvent;
import common.DigitalSignature;
import common.PropertyBiConsumer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
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
    private ObservableList<MetadataInspectionEvent> inspectionEvents;
    private ObservableList<ProcessedFileRecord> completedFileRecords;

    /**
     * Initialises state components prior to scene setup.
     */
    @Override
    public void init()
    {
        viewPane = new MainViewPane();
        flatMetadataText = new StringBuilder();
        inspectionEvents = FXCollections.observableArrayList();
        completedFileRecords = FXCollections.observableArrayList();
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
        createSourceContextMenu();
        restoreSavedSettings();
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

        if (workerTask != null && workerTask.isRunning())
        {
            workerTask.cancel(true);
        }

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
                TextField targetText = UtilsJavaFX.getById(rootPane, MainViewPane.TGTID, TextField.class);
                SummaryDialogFactory.show(rootPane.getScene().getWindow(), targetText, completedFileRecords);
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
     * Binds control events, property listeners, and state dependencies.
     */
    private void configureDynamicNodes()
    {
        final TextField sourceText = UtilsJavaFX.getById(rootPane, MainViewPane.SRCID, TextField.class);
        final TextField prefixText = UtilsJavaFX.getById(rootPane, MainViewPane.PFXID, TextField.class);
        final CheckBox embedDateTimeCheck = UtilsJavaFX.getById(rootPane, MainViewPane.EMBID, CheckBox.class);
        final DatePicker modifyDatePicker = UtilsJavaFX.getById(rootPane, MainViewPane.DTMID, DatePicker.class);
        final CheckBox showMetadataCheck = UtilsJavaFX.getById(rootPane, MainViewPane.SHWID, CheckBox.class);
        final CheckBox themeCheck = UtilsJavaFX.getById(rootPane, MainViewPane.THMID, CheckBox.class);

        // Toggle between dark and light theme
        themeCheck.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal)
            {
                UtilsJavaFX.switchTheme(rootPane, newVal.booleanValue() ? "dark.css" : "light.css");
            }
        });

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
                UtilsJavaFX.handleSourcePaste(rootPane.getScene().getWindow(), event, sourceText);
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
        BooleanBinding isMetadataEmpty = Bindings.isEmpty(inspectionEvents);
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
     * Constructs the source selection context menu containing fixed pick options and recent
     * history.
     */
    private void createSourceContextMenu()
    {
        final ContextMenu menu = new ContextMenu();
        MenuItem selectFolder = new MenuItem("Select Folder...");
        MenuItem selectFiles = new MenuItem("Select Specific Files...");
        TextField sourceText = UtilsJavaFX.getById(rootPane, MainViewPane.SRCID, TextField.class);

        selectFolder.setOnAction(new FilePickHandler(sourceText, "Select Source Directory"));

        selectFiles.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                UtilsJavaFX.handleFileSelection(rootPane.getScene().getWindow());
            }
        });

        menu.getItems().addAll(selectFolder, selectFiles, new SeparatorMenuItem());
        viewPane.sourceBtn.setUserData(menu);
        populateRecentHistoryMenu(menu, sourceText);
    }

    /**
     * Reads recent source path history from storage and appends the entries to the menu.
     *
     * @param menu
     *        the target {@link ContextMenu} instance
     * @param sourceText
     *        the source path {@link TextField} control
     */
    private void populateRecentHistoryMenu(ContextMenu menu, final TextField sourceText)
    {
        try
        {
            String[] history = PathHistoryStore.loadRecentSourcePaths();

            if (history.length > 0)
            {
                for (String entry : history)
                {
                    if (entry == null || entry.isEmpty())
                    {
                        continue;
                    }

                    String fileHistory;
                    String parentHistory = null;

                    int pos = entry.indexOf('|');

                    if (pos >= 0)
                    {
                        parentHistory = entry.substring(0, pos);
                        fileHistory = entry.substring(pos + 1);
                    }

                    else
                    {
                        fileHistory = entry;
                    }

                    final String targetFile = fileHistory;
                    final String targetParent = parentHistory;
                    MenuItem item = new MenuItem(fileHistory);

                    item.setOnAction(new EventHandler<ActionEvent>()
                    {
                        @Override
                        public void handle(ActionEvent event)
                        {
                            sourceText.setText(targetFile);

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

            else
            {
                MenuItem blankItem = new MenuItem("No recent paths");
                blankItem.setDisable(true);
                menu.getItems().add(blankItem);
            }
        }

        catch (BatchErrorException exc)
        {
            MenuItem blankItem = new MenuItem("Recent paths unknown");
            blankItem.setDisable(true);
            menu.getItems().add(blankItem);
        }
    }

    /**
     * Restores user settings and path history from persistent storage.
     */
    private void restoreSavedSettings()
    {
        TextField sourceText = UtilsJavaFX.getById(rootPane, MainViewPane.SRCID, TextField.class);
        TextField targetText = UtilsJavaFX.getById(rootPane, MainViewPane.TGTID, TextField.class);
        CheckBox themeCheck = UtilsJavaFX.getById(rootPane, MainViewPane.THMID, CheckBox.class);

        try
        {
            boolean isDark = PathHistoryStore.loadSettings(sourceText, targetText);

            if (isDark)
            {
                themeCheck.setSelected(true);
                UtilsJavaFX.switchTheme(rootPane, "dark.css");
            }

            else
            {
                UtilsJavaFX.switchTheme(rootPane, "light.css");
            }
        }

        catch (IOException exc)
        {
            String errmsg = "Unable to load path history information from properties due to an error.\n\n" + exc.getMessage();
            UtilsJavaFX.launchPopup(rootPane, "Configuration Error", errmsg, AlertType.ERROR);
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
        inspectionEvents.clear();
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

        workerTask = new BatchTask(config, progressBar, true);

        workerTask.setOnFileScanned(new Consumer<Integer>()
        {
            @Override
            public void accept(final Integer count)
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

        // Consolidated metadata event handling for flat text and structured tree processing
        workerTask.setOnMetadataInspected(new Consumer<MetadataInspectionEvent>()
        {
            @Override
            public void accept(final MetadataInspectionEvent event)
            {
                flatMetadataText.append(event.toString());

                if (!event.isDelimiter())
                {
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            inspectionEvents.add(event);
                        }
                    });
                }
            }
        });

        workerTask.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(final WorkerStateEvent event)
            {
                BatchMetrics stats = workerTask.getValue();

                if (stats != null)
                {
                    StatRecord.SOURCE_FILES.setValue(stats.getScanned());
                    StatRecord.TARGET_FILES.setValue(stats.getProcessed());
                    StatRecord.FILES_SKIPPED.setValue(stats.getFilesSkippedCount());
                    StatRecord.TOTAL_SIZE.setValue(String.format("%.2f MB", stats.getTotalTargetSizeMB()));
                }

                logArea.appendText("\n[SUCCESS] Exif data retrieved successfully.\n");

                showMetadataInspectorTree();
                resetControlStates(progressLabel);
            }
        });

        workerTask.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(final WorkerStateEvent event)
            {
                Throwable exc = workerTask.getException();
                String msg = (exc != null && exc.getMessage() != null
                        ? exc.getMessage()
                        : "An unexpected error occurred during metadata extraction.");

                logArea.appendText("[ERROR] " + msg + "\n");
                resetControlStates(progressLabel);
                UtilsJavaFX.launchPopup(rootPane, "Metadata Extraction Error", msg, AlertType.ERROR);
            }
        });

        workerTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(final WorkerStateEvent event)
            {
                logArea.appendText("[WARNING] Batch process was cancelled.\n");
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
     * Opens modal dialog window displaying structural metadata contents using the interactive
     * TreeTableView inspector.
     */
    private void showMetadataInspectorTree()
    {
        MetadataViewerDialog dialog = new MetadataViewerDialog((Stage) rootPane.getScene().getWindow());

        dialog.setMetadataEvents(inspectionEvents);
        dialog.setMetadataText(flatMetadataText.toString());

        flatMetadataText.setLength(0);
        flatMetadataText.trimToSize();

        dialog.show();
    }

    /**
     * Initiates the standard asynchronous batch modification process task.
     */
    private void executeBatchProcess()
    {
        final BatchConfiguration config;
        final ProgressBar progressBar = viewPane.progressBar;
        final Label progressLabel = (Label) progressBar.getUserData();
        final TextArea logArea = (TextArea) viewPane.clearLogBtn.getUserData();

        logArea.clear();
        StatRecord.resetAll();
        completedFileRecords.clear();

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

        workerTask = new BatchTask(config, progressBar, false);

        // Receive file execution output records for tabular summary reporting
        workerTask.setOnFileSummaryListener(new PropertyBiConsumer()
        {
            @Override
            public void accept(final String key, final Object value)
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
            public void accept(final Integer count)
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
            public void accept(final Integer count)
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

        // Update final metrics
        workerTask.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(final WorkerStateEvent event)
            {
                BatchMetrics stats = workerTask.getValue();

                if (stats != null)
                {
                    StatRecord.SOURCE_FILES.setValue(stats.getScanned());
                    StatRecord.TARGET_FILES.setValue(stats.getProcessed());
                    StatRecord.FILES_SKIPPED.setValue(stats.getFilesSkippedCount());
                    StatRecord.TOTAL_SIZE.setValue(String.format("%.2f MB", stats.getTotalTargetSizeMB()));
                }

                logArea.appendText("\n[SUCCESS] Batch processing complete.\n");

                resetControlStates(progressLabel);
                viewPane.viewBtn.fire();
            }
        });

        workerTask.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(final WorkerStateEvent event)
            {
                Throwable exc = workerTask.getException();
                String msg = (exc != null && exc.getMessage() != null) ? exc.getMessage() : "An unexpected error occurred during batch processing.";

                logArea.appendText("[ERROR] " + msg + "\n");
                resetControlStates(progressLabel);
                UtilsJavaFX.launchPopup(rootPane, "Processing Error", msg, AlertType.ERROR);
            }
        });

        workerTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(final WorkerStateEvent event)
            {
                logArea.appendText("[WARNING] Batch process was cancelled.\n");
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
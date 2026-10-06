package gui;

import java.io.IOException;
import java.util.function.Consumer;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.BatchMetrics;
import batch.BatchProcessEvent;
import batch.MetadataInspectionEvent;
import common.DigitalSignature;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.TextFlow;
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
    private PauseTransition progressResetDelay;
    private ObservableList<MetadataInspectionEvent> inspectionEvents;
    private ObservableList<ProcessedFileRecord> completedFileRecords;

    /**
     * Initialises state components prior to scene setup.
     */
    @Override
    public void init()
    {
        viewPane = new MainViewPane();
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

        ImageView icon = UtilsJavaFX.createIcon("lens.png");

        if (icon != null && icon.getImage() != null)
        {
            primaryStage.getIcons().add(icon.getImage());
        }

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
            UtilsJavaFX.copyTextFlowToClipboard(viewPane.logFlow);
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
            TextFlow logFlow = viewPane.logFlow;

            logFlow.getChildren().clear();
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
                boolean isDark = newVal.booleanValue();

                UtilsJavaFX.switchTheme(rootPane, isDark ? "dark.css" : "light.css");
                viewPane.applyThemeIcons(isDark);
                createSourceContextMenu();
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
        viewPane.copyLogBtn.setOnAction(this);
        viewPane.clearLogBtn.setOnAction(this);
        viewPane.viewBtn.setOnAction(this);
    }

    /**
     * Constructs or updates the source selection context menu with theme-appropriate graphics and
     * recent history.
     *
     */
    private void createSourceContextMenu()
    {
        ContextMenu menu;
        Object node = viewPane.sourceBtn.getUserData();

        if (!(node instanceof ContextMenu))
        {
            menu = new ContextMenu();
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

        else
        {
            menu = (ContextMenu) node;
        }

        MenuItem selectFolder = menu.getItems().get(0);
        MenuItem selectFiles = menu.getItems().get(1);
        CheckBox themeCheck = UtilsJavaFX.getById(rootPane, MainViewPane.THMID, CheckBox.class);
        ImageView folderIcon = UtilsJavaFX.createIcon("folder.png", 16, themeCheck.isSelected());
        ImageView fileIcon = UtilsJavaFX.createIcon("files.png", 16, themeCheck.isSelected());

        if (folderIcon != null)
        {
            selectFolder.setGraphic(folderIcon);
        }

        if (fileIcon != null)
        {
            selectFiles.setGraphic(fileIcon);
        }
    }

    /**
     * Reads recent source path history from storage and appends the entries to the menu.
     *
     * @param menu
     *        the target {@link ContextMenu} instance
     * @param sourceText
     *        the source path {@link TextField} control
     */
    private void populateRecentHistoryMenu(ContextMenu menu, TextField sourceText)
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
            }

            else
            {
                UtilsJavaFX.switchTheme(rootPane, "light.css");
                viewPane.applyThemeIcons(false);
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
        final TextFlow logFlow = viewPane.logFlow;

        if (progressResetDelay != null)
        {
            progressResetDelay.stop();
        }

        logFlow.getChildren().clear();
        StatRecord.resetAll();
        inspectionEvents.clear();

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

        workerTask = new BatchTask(config, true);

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

        workerTask.setOnMetadataInspected(new Consumer<MetadataInspectionEvent>()
        {
            @Override
            public void accept(MetadataInspectionEvent event)
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

                UtilsJavaFX.writeToTextFlow(logFlow, "[SUCCESS] Exif data retrieved successfully", "log-success");
                showMetadataInspectorTree();
                resetControlStates();
            }
        });

        workerTask.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                Throwable exc = workerTask.getException();
                String msg = (exc != null && exc.getMessage() != null
                        ? exc.getMessage()
                        : "An unexpected error occurred during metadata extraction.");

                resetControlStates();
                UtilsJavaFX.writeToTextFlow(logFlow, "[ERROR] " + msg, "log-success");
                UtilsJavaFX.launchPopup(rootPane, "Metadata Extraction Error", msg, AlertType.ERROR);
            }
        });

        workerTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                resetControlStates();
                UtilsJavaFX.writeToTextFlow(logFlow, "[WARNING] Batch process was cancelled", "log-success");

                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        progressBar.progressProperty().unbind();

                        if (progressLabel != null)
                        {
                            progressLabel.textProperty().unbind();
                            progressLabel.setText("Cancelled");
                        }

                        progressBar.setProgress(0.0);
                    }
                });
            }
        });

        viewPane.actionBtn.setDisable(true);
        viewPane.copyLogBtn.setDisable(true);
        progressLabel.textProperty().bind(workerTask.messageProperty());
        progressBar.progressProperty().bind(workerTask.progressProperty());

        TaskProgressDialog.show(rootPane.getScene().getWindow(), "Retrieving Metadata", workerTask);
    }

    /**
     * Opens modal dialog window displaying structural metadata contents using the interactive
     * TreeTableView inspector.
     */
    private void showMetadataInspectorTree()
    {
        MetadataViewerDialog dialog = new MetadataViewerDialog((Stage) rootPane.getScene().getWindow());

        dialog.setMetadataEvents(inspectionEvents);
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
        final TextFlow logFlow = viewPane.logFlow;

        if (progressResetDelay != null)
        {
            progressResetDelay.stop();
        }

        logFlow.getChildren().clear();
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

        workerTask = new BatchTask(config, false);

        // Receive file execution output records for tabular summary reporting
        workerTask.setOnBatchSummaryListener(new Consumer<BatchProcessEvent>()
        {
            @Override
            public void accept(BatchProcessEvent event)
            {
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

        // Update final metrics
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

                resetControlStates();
                UtilsJavaFX.writeToTextFlow(logFlow, "[SUCCESS] Batch processing complete", "log-success");
                viewPane.viewBtn.fire();
            }
        });

        workerTask.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                Throwable exc = workerTask.getException();
                String msg = (exc != null && exc.getMessage() != null) ? exc.getMessage() : "An unexpected error occurred during batch processing.";

                resetControlStates();
                UtilsJavaFX.writeToTextFlow(logFlow, "[ERROR] " + msg, "log-success");
                UtilsJavaFX.launchPopup(rootPane, "Processing Error", msg, AlertType.ERROR);
            }
        });

        workerTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                resetControlStates();
                UtilsJavaFX.writeToTextFlow(logFlow, "[WARNING] Batch process was cancelled", "log-success");

                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        progressBar.progressProperty().unbind();

                        if (progressLabel != null)
                        {
                            progressLabel.textProperty().unbind();
                            progressLabel.setText("Cancelled");
                        }

                        progressBar.setProgress(0.0);
                    }
                });
            }
        });

        viewPane.actionBtn.setDisable(true);
        viewPane.copyLogBtn.setDisable(true);
        progressLabel.textProperty().bind(workerTask.messageProperty());
        progressBar.progressProperty().bind(workerTask.progressProperty());

        TaskProgressDialog.show(rootPane.getScene().getWindow(), "Processing Batch", workerTask);
    }

    /**
     * Restores main interactive controls from active execution state.
     *
     * <p>
     * Promptly unbinds UI properties to prevent lingering task updates from distorting
     * the GUI, followed by a 3-second delay before progress display controls are cleared.
     * </p>
     */
    private void resetControlStates()
    {
        final ProgressBar progressBar = viewPane.progressBar;
        final Label progressLabel = (Label) progressBar.getUserData();

        workerTask = null;
        progressLabel.textProperty().unbind();
        progressBar.progressProperty().unbind();
        viewPane.actionBtn.setDisable(false);

        if (viewPane.actionBtn.getScene() != null && viewPane.actionBtn.getScene().getRoot() != null)
        {
            viewPane.actionBtn.getScene().getRoot().requestFocus();
        }

        viewPane.copyLogBtn.setDisable(false);

        if (progressResetDelay != null)
        {
            progressResetDelay.stop();
        }

        progressResetDelay = new PauseTransition(Duration.seconds(3));

        progressResetDelay.setOnFinished(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (progressLabel != null)
                {
                    progressLabel.setText("");
                }

                progressBar.setProgress(0.0);
            }
        });

        progressResetDelay.play();
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
package gui;

import java.io.File;
import java.io.IOException;
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
import common.PropertyListener;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
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
import javafx.stage.Window;
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
    private ObservableList<FileSummaryRecord> fileRecords;

    /**
     * Initialises the application state before the JavaFX application window is launched.
     */
    @Override
    public void init()
    {
        viewPane = new MainViewPane();
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

        configureDynamicNodes(rootPane);
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
            System.err.println("Unable to save path history information due to an error: " + exc.getMessage());
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
            executeBatchProcess();
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
            showSummaryDialog();
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
     * Configures dynamic behaviour and event handlers for the UI controls contained within the
     * specified parent pane.
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
     *
     * @param pane
     *        the parent container holding the UI elements to configure
     */
    private void configureDynamicNodes(Parent pane)
    {
        TextField sourceText = GUIUtils.getById(pane, MainViewPane.SRCID, TextField.class);
        TextField targetText = GUIUtils.getById(pane, MainViewPane.TGTID, TextField.class);
        TextField prefixText = GUIUtils.getById(pane, MainViewPane.PFXID, TextField.class);
        CheckBox embedDateTimeCheck = GUIUtils.getById(pane, MainViewPane.EMBID, CheckBox.class);
        DatePicker modifyDatePicker = GUIUtils.getById(pane, MainViewPane.DTMID, DatePicker.class);
        CheckBox showMetadataCheck = GUIUtils.getById(pane, MainViewPane.SHWID, CheckBox.class);

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

        sourceText.focusedProperty().addListener(new InvalidationListener()
        {
            @Override
            public void invalidated(Observable observable)
            {
                if (!sourceText.isFocused())
                {
                    sourceText.setText(sourceText.getText().trim());
                }
            }
        });

        sourceText.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                // If text was manually edited, ensure userData doesn't point to an outdated Path
                if (sourceText.getUserData() != null)
                {
                    Path currentPath = (Path) sourceText.getUserData();

                    if (!currentPath.toString().equals(newValue))
                    {
                        sourceText.setUserData(null);
                    }
                }
            }
        });

        sourceText.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
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

                            /*
                             * Locate the parent directory from the first valid path entry
                             */
                            for (String part : parts)
                            {
                                try
                                {
                                    Path file = Paths.get(part).toAbsolutePath();

                                    if (Files.isRegularFile(file))
                                    {
                                        parentDir = file.getParent();
                                        break;
                                    }
                                }

                                catch (InvalidPathException exc)
                                {
                                    // Ignore invalid individual path tokens during discovery
                                }
                            }

                            boolean valid = (parentDir != null);

                            if (valid)
                            {
                                /*
                                 * Verify all files exist and share the exact same directory
                                 */
                                for (String part : parts)
                                {
                                    try
                                    {
                                        Path file = parentDir.resolve(part);

                                        if (!Files.isRegularFile(file) || !parentDir.equals(file.getParent()))
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
                                Path rawPath = Paths.get(pastedText);

                                if (Files.exists(rawPath))
                                {
                                    Path fullPath = rawPath.toAbsolutePath();
                                    Path parent = Files.isDirectory(fullPath) ? fullPath : fullPath.getParent();

                                    sourceText.setText(pastedText);
                                    sourceText.setTooltip(new Tooltip(pastedText));
                                    sourceText.setUserData(parent == null ? null : parent.toAbsolutePath());
                                }

                                else
                                {
                                    String msg = "The pasted path does not exist on disk:\n\n" + pastedText;
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
        });

        // Preview listener for live preview generation
        InvalidationListener previewListener = new InvalidationListener()
        {
            @Override
            public void invalidated(Observable observable)
            {
                viewPane.updatePreview((GridPane) pane);
            }
        };

        prefixText.disableProperty().bind(showMetadataCheck.selectedProperty());
        modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());

        prefixText.textProperty().addListener(previewListener);
        embedDateTimeCheck.selectedProperty().addListener(previewListener);
        modifyDatePicker.valueProperty().addListener(previewListener);
        showMetadataCheck.selectedProperty().addListener(new InvalidationListener()
        {
            @Override
            public void invalidated(Observable observable)
            {
                boolean isMetadata = showMetadataCheck.isSelected();

                viewPane.actionBtn.setText(isMetadata ? "Display Metadata" : "Run Batch Process");
                viewPane.viewBtn.setText(isMetadata ? "List Metadata" : "View Summary");
            }
        });

        // Action Handlers
        viewPane.sourceBtn.setOnAction(this);
        viewPane.actionBtn.setOnAction(this);
        viewPane.exitBtn.setOnAction(this);
        viewPane.copyLogBtn.setOnAction(this);
        viewPane.clearLogBtn.setOnAction(this);
        viewPane.abortBtn.setOnAction(this);
        viewPane.viewBtn.setOnAction(this);

        viewPane.updatePreview((GridPane) pane);
    }

    /**
     * Populates the Source button's context menu with default selection options
     * (Select Folder, Select Specific Files) and recent paths retrieved from persistent storage.
     */
    private void populateRecentHistoryMenu()
    {
        ContextMenu menu = new ContextMenu();
        Button sourceBtn = viewPane.sourceBtn;
        TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);
        List<String> recentPaths = PathHistoryStore.loadRecentSourcePaths();

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
                            Path path = Paths.get(entry);

                            if (Files.exists(path))
                            {
                                Path parent = Files.isDirectory(path) ? path : path.getParent();
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

            workerTask.setFileSummaryListener(new PropertyListener()
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
                        String status = event.isSuccess() ? "Completed" : "Failed";

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                fileRecords.add(new FileSummaryRecord(source, target, status, size));
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

                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            GUIUtils.launchPopup("Process Complete", "Batch processing completed", AlertType.INFORMATION);
                        }
                    });
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

        TableView<FileSummaryRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FileSummaryRecord, String> sourceCol = new TableColumn<>("Source File");
        sourceCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileSummaryRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileSummaryRecord, String> cellData)
            {
                return cellData.getValue().sourceNameProperty();
            }
        });

        sourceCol.setPrefWidth(200);

        TableColumn<FileSummaryRecord, String> targetCol = new TableColumn<>("Target File");
        targetCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileSummaryRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileSummaryRecord, String> cellData)
            {
                return cellData.getValue().targetNameProperty();
            }
        });

        targetCol.setPrefWidth(200);

        TableColumn<FileSummaryRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileSummaryRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileSummaryRecord, String> cellData)
            {
                return cellData.getValue().statusProperty();
            }
        });

        statusCol.setPrefWidth(120);

        table.getColumns().add(sourceCol);
        table.getColumns().add(targetCol);
        table.getColumns().add(statusCol);
        table.setItems(fileRecords);

        // Row factory to highlight the latest processed record in green
        table.setRowFactory(new Callback<TableView<FileSummaryRecord>, TableRow<FileSummaryRecord>>()
        {
            @Override
            public TableRow<FileSummaryRecord> call(TableView<FileSummaryRecord> param)
            {
                return new TableRow<FileSummaryRecord>()
                {
                    @Override
                    protected void updateItem(FileSummaryRecord item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        if (empty || item == null)
                        {
                            setStyle("");
                        }

                        else if (getIndex() == fileRecords.size() - 1)
                        {
                            setStyle("-fx-background-color: #c8e6c9; -fx-text-fill: #1b5e20;");
                        }

                        else
                        {
                            setStyle("");
                        }
                    }
                };
            }
        });

        // Auto-scroll listener to follow live updates
        fileRecords.addListener(new ListChangeListener<FileSummaryRecord>()
        {
            @Override
            public void onChanged(Change<? extends FileSummaryRecord> change)
            {
                while (change.next())
                {
                    if (change.wasAdded() && !fileRecords.isEmpty())
                    {
                        table.scrollTo(fileRecords.size() - 1);
                        table.refresh();
                    }
                }
            }
        });

        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefSize(550, 320);
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
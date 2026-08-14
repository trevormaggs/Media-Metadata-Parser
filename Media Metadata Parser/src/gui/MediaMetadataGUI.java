package gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
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
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;

public class MediaMetadataGUI extends Application
{
    private Stage stage;
    private BatchTask workerTask;
    private MainViewPane viewPane;
    private ObservableList<FileSummaryRecord> fileRecords;

    @Override
    public void init()
    {
        viewPane = new MainViewPane();
        fileRecords = FXCollections.observableArrayList();
    }

    @Override
    public void start(Stage primaryStage)
    {
        RowConstraints fixedRow = new RowConstraints();
        fixedRow.setVgrow(Priority.NEVER);

        RowConstraints fillRow = new RowConstraints();
        fillRow.setVgrow(Priority.ALWAYS);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(15));
        gridPane.getRowConstraints().addAll(fixedRow, fixedRow, fillRow, fixedRow, fixedRow);
        gridPane.requestFocus();

        viewPane.buildLayout(gridPane);

        Scene scene = new Scene(gridPane, 620, 650);
        scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());

        stage = primaryStage;
        stage.setTitle("Image Metadata Structure Viewer");
        stage.setScene(scene);
        stage.show();

        attachActionHandlers(new ActionHandler());
        configureDynamicNodes(gridPane);
    }

    private void configureDynamicNodes(Parent pane)
    {
        TextField sourceText = MainViewPane.getById(pane, MainViewPane.SRCID);
        TextField prefixText = MainViewPane.getById(pane, MainViewPane.PFXID);
        DatePicker modifyDatePicker = MainViewPane.getById(pane, MainViewPane.DTMID);
        CheckBox showMetadataCheck = MainViewPane.getById(pane, MainViewPane.SHWID);

        if (sourceText != null)
        {
            sourceText.setOnMouseClicked(new EventHandler<MouseEvent>()
            {
                @Override
                public void handle(MouseEvent event)
                {
                    if (event.getButton() == MouseButton.PRIMARY)
                    {
                        viewPane.getSourceBtn().fire();
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
                            File path = new File(pastedText);

                            if (path.exists())
                            {
                                sourceText.setText(pastedText);
                                sourceText.setUserData(path.isDirectory() ? null : path.getParent());
                                sourceText.setTooltip(new Tooltip(pastedText));
                            }

                            else if (pastedText.contains(","))
                            {
                                Path parentDir = null;
                                String[] parts = pastedText.split("\\s*,\\s*");

                                /* Find the first valid parent directory first */
                                for (String part : parts)
                                {
                                    Path file = Paths.get(part).normalize();

                                    if (file.getParent() != null && Files.isRegularFile(file))
                                    {
                                        parentDir = file.getParent();
                                        break;
                                    }
                                }

                                boolean valid = (parentDir != null);

                                /*
                                 * Then, check if all files are resolved in
                                 * the first discovered directory.
                                 */
                                if (valid)
                                {
                                    for (String part : parts)
                                    {
                                        Path file = Paths.get(part);

                                        if (!file.isAbsolute())
                                        {
                                            file = parentDir.resolve(file);
                                        }

                                        file = file.normalize();

                                        if (!Files.isRegularFile(file) || !parentDir.equals(file.getParent()))
                                        {
                                            valid = false;
                                            break;
                                        }
                                    }
                                }

                                sourceText.setText(pastedText);
                                sourceText.setTooltip(new Tooltip(pastedText));
                                sourceText.setUserData(valid ? parentDir.toAbsolutePath().toString() : null);
                            }

                            else
                            {
                                // TODO: convert to Alert popup
                                System.out.println("Pasted path does not exist [" + pastedText + "]");
                            }
                        }

                        event.consume();
                    }
                }
            });
        }

        if (prefixText != null && showMetadataCheck != null)
        {
            prefixText.disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        if (modifyDatePicker != null && showMetadataCheck != null)
        {
            modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        if (showMetadataCheck != null)
        {
            showMetadataCheck.selectedProperty().addListener(new InvalidationListener()
            {
                @Override
                public void invalidated(Observable observable)
                {
                    viewPane.getActionBtn().setText(showMetadataCheck.isSelected() ? "Display Metadata" : "Run Batch Process");
                }
            });
        }
    }

    private void attachActionHandlers(ActionHandler handler)
    {
        viewPane.getSourceBtn().setOnAction(handler);
        viewPane.getActionBtn().setOnAction(handler);
        viewPane.getExitBtn().setOnAction(handler);
        viewPane.getSelectFiles().setOnAction(handler);
        viewPane.getCopyLogBtn().setOnAction(handler);
        viewPane.getClearLogBtn().setOnAction(handler);
        viewPane.getCancelBtn().setOnAction(handler);
        viewPane.getViewBtn().setOnAction(handler);
    }

    private class ActionHandler implements EventHandler<ActionEvent>
    {
        @Override
        public void handle(ActionEvent event)
        {
            Object source = event.getSource();

            if (source == viewPane.getSourceBtn())
            {
                ContextMenu menu = (ContextMenu) viewPane.getSourceBtn().getUserData();

                if (menu != null)
                {
                    menu.show(viewPane.getSourceBtn(), Side.BOTTOM, 0, 0);
                }
            }

            else if (source == viewPane.getSelectFiles())
            {
                handleFileSelection();
            }

            else if (source == viewPane.getActionBtn())
            {
                executeBatchProcess();
            }

            else if (source == viewPane.getCopyLogBtn())
            {
                TextArea logArea = (TextArea) viewPane.getClearLogBtn().getUserData();

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

            else if (source == viewPane.getCancelBtn())
            {
                if (workerTask != null)
                {
                    if (workerTask != null)
                    {
                        workerTask.cancel(true);
                    }
                }
            }

            else if (source == viewPane.getViewBtn())
            {
                showSummaryDialog(stage);
            }

            else if (source == viewPane.getClearLogBtn())
            {
                TextArea logArea = (TextArea) viewPane.getClearLogBtn().getUserData();

                if (logArea != null)
                {
                    logArea.clear();
                }
            }

            else if (source == viewPane.getExitBtn())
            {
                Platform.exit();
            }
        }
    }

    private void executeBatchProcess()
    {
        BatchConfiguration config;
        Button actionBtn = viewPane.getActionBtn();
        Button cancelBtn = viewPane.getCancelBtn();
        Button copyLogBtn = viewPane.getCopyLogBtn();
        Button clearLogBtn = viewPane.getClearLogBtn();
        ProgressBar progressBar = viewPane.getProgressBar();
        TextArea logArea = (TextArea) clearLogBtn.getUserData();
        Label progressLabel = (Label) progressBar.getUserData();
        CheckBox showMetadata = getById(MainViewPane.SHWID);
        boolean metaDisplay = (showMetadata != null && showMetadata.isSelected());

        if (logArea != null)
        {
            logArea.clear();
            fileRecords.clear();
            StatRecord.resetAll();

            try
            {
                Parent root = stage.getScene().getRoot();
                config = new ConfigurationBuilder(root).build();
            }

            catch (BatchErrorException exc)
            {
                progressLabel.setText("Configuration error");
                GUIUtils.launchPopup("Invalid File Selection", exc.getMessage(), AlertType.ERROR);
                return;
            }

            workerTask = new BatchTask(config, logArea, progressBar, metaDisplay);

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
     * Opens a system native {@link FileChooser} dialog allowing users to select multiple input
     * files.
     */
    private void handleFileSelection()
    {
        TextField sourceText = getById(MainViewPane.SRCID);

        if (sourceText != null)
        {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Source Files");

            File sourceDir = new File(sourceText.getText().isEmpty() ? System.getProperty("user.home") : sourceText.getText());

            if (sourceDir.isDirectory())
            {
                chooser.setInitialDirectory(sourceDir);
            }

            List<File> files = chooser.showOpenMultipleDialog(stage);

            if (files != null && !files.isEmpty())
            {
                File parentDir = files.get(0).getParentFile();

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

                if (parentDir != null)
                {
                    sourceText.setUserData(parentDir.getAbsolutePath());
                }
            }
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
        Button actionBtn = viewPane.getActionBtn();
        Button cancelBtn = viewPane.getCancelBtn();
        Button copyLogBtn = viewPane.getCopyLogBtn();
        ProgressBar progressBar = viewPane.getProgressBar();

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
                if (progressLabel.textProperty().isBound())
                {
                    progressLabel.textProperty().unbind();
                }

                progressLabel.setText("");

                if (progressBar.progressProperty().isBound())
                {
                    progressBar.progressProperty().unbind();
                }

                progressBar.setProgress(0.0);
            }
        });

        delay.play();
    }

    /**
     * Opens a summary dialog containing details and processing statuses for all processed files.
     *
     * @param ownerWindow
     *        the owning window stage for this modal dialog
     */
    private void showSummaryDialog(Window ownerWindow)
    {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch Processing Summary");
        dialog.setHeaderText("Detailed Processing Results");
        dialog.initOwner(ownerWindow);

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

        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefSize(550, 320);
        dialog.showAndWait();
    }

    /**
     * Convenience method to retrieve an FX control by its ID starting from the root scene node.
     *
     * @param <T>
     *        the expected node type
     * @param id
     *        the target JavaFX FXID string
     * @return the matching node cast to type {@code T}, or {@code null} if not found
     */
    private <T extends Node> T getById(String id)
    {
        if (stage != null && stage.getScene() != null)
        {
            return GUIUtils.getById(stage.getScene().getRoot(), id);
        }

        return null;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
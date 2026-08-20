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
import common.PropertyListener;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Provides the JavaFX graphical user interface for configuring and running batch media metadata
 * processing operations.
 */
public class MediaMetadataGUI_Test extends Application implements EventHandler<ActionEvent>
{
    private GridPane rootPane;
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

        sourceText.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY)
            {
                viewPane.sourceBtn.fire();
            }
        });

        sourceText.focusedProperty().addListener(observable -> {
            if (!sourceText.isFocused())
            {
                sourceText.setText(sourceText.getText().trim());
            }
        });

        sourceText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (sourceText.getUserData() != null)
            {
                Path currentPath = (Path) sourceText.getUserData();
                if (!currentPath.toString().equals(newValue))
                {
                    sourceText.setUserData(null);
                }
            }
        });

        sourceText.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
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
                                Path file = Paths.get(part).toAbsolutePath();
                                if (Files.isRegularFile(file))
                                {
                                    parentDir = file.getParent();
                                    break;
                                }
                            }
                            catch (InvalidPathException exc)
                            {
                                // Ignore invalid path tokens during discovery
                            }
                        }

                        boolean valid = (parentDir != null);

                        if (valid)
                        {
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
        });

        InvalidationListener previewListener = observable -> viewPane.updatePreview((GridPane) pane);

        prefixText.disableProperty().bind(showMetadataCheck.selectedProperty());
        modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());

        prefixText.textProperty().addListener(previewListener);
        embedDateTimeCheck.selectedProperty().addListener(previewListener);
        modifyDatePicker.valueProperty().addListener(previewListener);

        showMetadataCheck.selectedProperty().addListener(observable -> {
            boolean isMetadata = showMetadataCheck.isSelected();
            viewPane.actionBtn.setText(isMetadata ? "Display Metadata" : "Run Batch Process");
            viewPane.viewBtn.setText(isMetadata ? "List Metadata" : "View Summary");
        });

        viewPane.sourceBtn.setOnAction(this);
        viewPane.actionBtn.setOnAction(this);
        viewPane.exitBtn.setOnAction(this);
        viewPane.copyLogBtn.setOnAction(this);
        viewPane.clearLogBtn.setOnAction(this);
        viewPane.abortBtn.setOnAction(this);
        viewPane.viewBtn.setOnAction(this);
        viewPane.updatePreview((GridPane) pane);
    }

    private void populateRecentHistoryMenu()
    {
        ContextMenu menu = new ContextMenu();
        Button sourceBtn = viewPane.sourceBtn;
        TextField sourceText = GUIUtils.getById(rootPane, MainViewPane.SRCID, TextField.class);
        List<String> recentPaths = PathHistoryStore.loadRecentSourcePaths();

        MenuItem selectFolder = new MenuItem("Select Folder...");
        selectFolder.setOnAction(new FilePickHandler(sourceText, "Select Source Directory"));

        MenuItem selectFiles = new MenuItem("Select Specific Files...");
        selectFiles.setOnAction(event -> handleFileSelection());

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
                item.setOnAction(event -> {
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
                });
                menu.getItems().add(item);
            }
        }

        sourceBtn.setUserData(menu);
    }

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

            workerTask.setFileSummaryListener((key, value) -> {
                if (value instanceof BatchProcessEvent)
                {
                    BatchProcessEvent event = (BatchProcessEvent) value;
                    long size = event.getTargetSize();
                    String source = event.getSourceName();
                    String target = event.getTargetName();
                    String status = event.isSuccess() ? "Completed" : "Failed";

                    Platform.runLater(() -> fileRecords.add(new FileSummaryRecord(source, target, status, size)));
                }
            });

            workerTask.setOnFileScanned((count) -> 
                Platform.runLater(() -> StatRecord.SOURCE_FILES.setValue(count))
            );

            workerTask.setOnFileProcessed((count) -> 
                Platform.runLater(() -> StatRecord.TARGET_FILES.setValue(count))
            );

            workerTask.setOnSucceeded(event -> {
                BatchMetrics stats = workerTask.getValue();

                if (stats != null)
                {
                    StatRecord.SOURCE_FILES.setValue(stats.getScanned());
                    StatRecord.TARGET_FILES.setValue(stats.getProcessed());
                    StatRecord.FILES_SKIPPED.setValue(stats.getFilesSkippedCount());
                    StatRecord.TOTAL_SIZE.setValue(String.format("%.2f MB", stats.getTotalTargetSizeMB()));
                }

                resetControlStates(progressLabel);
                Platform.runLater(() -> GUIUtils.launchPopup("Process Complete", "Batch processing completed", AlertType.INFORMATION));
            });

            workerTask.setOnFailed(event -> {
                Throwable exc = workerTask.getException();
                String msg = (exc != null && exc.getMessage() != null ? exc.getMessage() : "An unknown error occurred.");

                resetControlStates(progressLabel);
                GUIUtils.launchPopup("Processing Error", msg, AlertType.ERROR);
            });

            workerTask.setOnCancelled(event -> resetControlStates(progressLabel));

            actionBtn.setDisable(true);
            cancelBtn.setDisable(false);
            copyLogBtn.setDisable(true);
            progressLabel.textProperty().bind(workerTask.messageProperty());

            Thread worker = new Thread(workerTask);
            worker.setDaemon(true);
            worker.start();

            if (showMetadata.isSelected())
            {
                showMetadataList();
            }
        }
    }

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
            flash.setOnFinished(event -> {
                logArea.deselect();
                logArea.setStyle(originalStyle);
            });

            flash.play();
        }
    }

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
        delay.setOnFinished(event -> {
            progressLabel.textProperty().unbind();
            progressBar.progressProperty().unbind();
            progressLabel.setText("");
            progressBar.setProgress(0.0);
        });

        delay.play();
    }

    private void showSummaryDialog()
    {
        showSummaryDialog2();
    }

    private void showSummaryDialog2()
    {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.NONE);
        dialog.setTitle("Batch Processing Summary");
        dialog.setHeaderText("Detailed Processing Results");

        ButtonType exportBtnType = new ButtonType("Export to File");
        dialog.getDialogPane().getButtonTypes().addAll(exportBtnType, ButtonType.CLOSE);

        TableView<FileSummaryRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FileSummaryRecord, String> sourceCol = new TableColumn<>("Source File");
        sourceCol.setCellValueFactory(cellData -> cellData.getValue().sourceNameProperty());
        sourceCol.setPrefWidth(200);

        TableColumn<FileSummaryRecord, String> targetCol = new TableColumn<>("Target File");
        targetCol.setCellValueFactory(cellData -> cellData.getValue().targetNameProperty());
        targetCol.setPrefWidth(200);

        TableColumn<FileSummaryRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(120);

        table.getColumns().add(sourceCol);
        table.getColumns().add(targetCol);
        table.getColumns().add(statusCol);
        table.setItems(fileRecords);

        table.setRowFactory(param -> new TableRow<FileSummaryRecord>() {
            @Override
            protected void updateItem(FileSummaryRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (getIndex() == fileRecords.size() - 1) {
                    setStyle("-fx-background-color: #e8f5e9;");
                } else {
                    setStyle("");
                }
            }
        });

        fileRecords.addListener((ListChangeListener<FileSummaryRecord>) change -> {
            if (!fileRecords.isEmpty()) {
                table.scrollTo(fileRecords.size() - 1);
            }
        });

        GridPane content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);
        content.setPadding(new Insets(10));
        content.add(table, 0, 0);

        GridPane.setHgrow(table, Priority.ALWAYS);
        GridPane.setVgrow(table, Priority.ALWAYS);

        dialog.getDialogPane().setContent(content);

        Button exportBtn = (Button) dialog.getDialogPane().lookupButton(exportBtnType);
        exportBtn.addEventFilter(ActionEvent.ACTION, event -> {
            exportSummaryToFile();
            event.consume();
        });

        dialog.show();
    }

    private void exportSummaryToFile()
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Summary to File");
        chooser.setInitialFileName("metadata_summary.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));

        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null)
        {
            try
            {
                byte[] bytes = buildSummaryText().getBytes(StandardCharsets.UTF_8);
                Files.write(file.toPath(), bytes);
                GUIUtils.launchPopup("Success", "Summary exported successfully to:\n" + file.getAbsolutePath(), AlertType.INFORMATION);
            }
            catch (IOException exc)
            {
                GUIUtils.launchPopup("Error", "Failed to save file: " + exc.getMessage(), AlertType.ERROR);
            }
        }
    }

    private String buildSummaryText()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Batch Processing Summary ===\n\n");

        for (FileSummaryRecord record : fileRecords)
        {
            sb.append(String.format("Source : %s\nTarget : %s\nStatus : %s\nSize   : %d bytes\n---\n",
                record.getSourceName(),
                record.getTargetName(),
                record.getStatus(),
                record.getFileSize()));
        }

        return sb.toString();
    }

    private void showMetadataList()
    {
        showSummaryDialog2();
    }
}
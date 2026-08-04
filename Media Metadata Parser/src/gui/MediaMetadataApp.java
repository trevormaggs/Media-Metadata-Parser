package gui;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import batch.BatchBuilder;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.BatchStatistics;
import batch.MediaBatchProcessor;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;
import logger.LogFactory;

public class MediaMetadataApp extends Application
{
    private final Button sourceBtn;
    private final MenuItem selectFiles;
    private final Button actionBtn;
    private final Button clearLogBtn;
    private final Button copyLogBtn;

    private final Button exitBtn;
    private final ProgressBar progressBar;

    private final Button cancelBtn;
    private final Button viewBtn;

    private Stage stage;
    private BatchTask activeTask;

    private static final String SRCID = "srcId";
    private static final String TGTID = "tgtId";
    private static final String PFXID = "pfxId";
    private static final String DTMID = "dtmId";
    private static final String EMBID = "embId";
    private static final String FORID = "forId";
    private static final String SKPID = "skpId";
    private static final String SHWID = "shwId";
    private static final String SRTID = "srtId";
    private static final String DBGID = "dbgId";

    public MediaMetadataApp()
    {
        this.sourceBtn = new Button();
        this.selectFiles = new MenuItem();
        this.actionBtn = new Button();
        this.clearLogBtn = new Button();
        this.copyLogBtn = new Button();

        this.exitBtn = new Button();
        this.progressBar = new ProgressBar(0.0);

        this.cancelBtn = new Button();
        this.viewBtn = new Button();
    }

    /**
     * Initialises the primary JavaFX stage and builds the GUI components for this application.
     *
     * @param root
     *        the primary stage used for populating this JavaFX application
     */
    @Override
    public void start(Stage root)
    {
        RowConstraints fixedRow = new RowConstraints();
        fixedRow.setVgrow(Priority.NEVER); // Keep controls at natural height

        RowConstraints fillRow = new RowConstraints();
        fillRow.setVgrow(Priority.ALWAYS); // Expand log pane fill space

        this.stage = root;
        stage.setTitle("Image Metadata Structure Viewer");

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

    /**
     * Builds and adds the application's top configuration panel to the specified root grid pane.
     *
     * @param pane
     *        the root {@link GridPane} to which the configuration panel is added
     */
    private void addTopPane(GridPane pane)
    {
        double labelWidth = 140;
        ActionHandler actionHandler = new ActionHandler();

        // Row 1
        Label sourceLabel = new Label("Source Directory");
        sourceLabel.setPrefWidth(labelWidth);
        TextField sourceText = new TextField();
        sourceText.setId(SRCID);
        sourceText.setPromptText("Directory or files...");
        sourceText.setPrefWidth(300);
        sourceText.setMaxWidth(300);
        sourceText.setText("E:\\ImageBatchDir");
        // sourceText.setEditable(false);
        MenuItem selectFolder = new MenuItem("Select Folder...");
        selectFolder.setOnAction(new FilePickHandler(sourceText, "Select Source Directory"));
        selectFiles.setText("Select Specific Files...");
        selectFiles.setOnAction(actionHandler);
        ContextMenu sourceMenu = new ContextMenu();
        sourceMenu.getItems().addAll(selectFolder, selectFiles);
        sourceBtn.setText("Browse...");
        sourceBtn.setUserData(sourceMenu);
        sourceBtn.setOnAction(actionHandler);
        HBox sourceHbox = new HBox(10);
        sourceHbox.getChildren().addAll(sourceLabel, sourceText, fillRow(), sourceBtn);

        // Row 2
        Label targetLabel = new Label("Target Directory");
        targetLabel.setPrefWidth(labelWidth);
        TextField targetText = new TextField();
        targetText.setId(TGTID);
        targetText.setText(MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY);
        targetText.setPrefWidth(300);
        targetText.setMaxWidth(300);
        Button targetBtn = new Button("Browse...");
        targetBtn.setOnAction(new FilePickHandler(targetText, "Select Target Directory"));
        HBox targetHbox = new HBox(10);
        targetHbox.getChildren().addAll(targetLabel, targetText, fillRow(), targetBtn);

        // Row 3
        Label prefixLabel = new Label("File Prefix Name");
        prefixLabel.setPrefWidth(labelWidth);
        TextField prefixText = new TextField();
        prefixText.setId(PFXID);
        prefixText.setText(MediaBatchProcessor.DEFAULT_IMAGE_PREFIX);
        prefixText.setPromptText("Example: Holiday_Trip_");
        prefixText.setPrefWidth(300);
        prefixText.setMaxWidth(300);
        HBox prefixHbox = new HBox(10);
        prefixHbox.getChildren().addAll(prefixLabel, prefixText, fillRow());

        // Row 4
        Label dateLabel = new Label("Modify Date Taken");
        dateLabel.setPrefWidth(labelWidth);
        DatePicker modifyDatePicker = new DatePicker();
        modifyDatePicker.setId(DTMID);
        modifyDatePicker.setPromptText("Select date...");
        modifyDatePicker.setPrefWidth(300);
        modifyDatePicker.setMaxWidth(300);
        HBox modifyDateHbox = new HBox(10);
        modifyDateHbox.getChildren().addAll(dateLabel, modifyDatePicker, fillRow());

        // Combine boxes
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

    /**
     * Creates and attaches the application's processing options and statistics panels to the
     * specified root {@link GridPane}.
     *
     * <p>
     * Both panels are contained within side-by-side {@link TitledPane} instances that expand
     * equally to fill the available width.
     * </p>
     *
     * @param pane
     *        the root {@link GridPane} to which the panels are added
     */
    /**
     * Creates and attaches the application's processing options and statistics panels to the
     * specified root {@link GridPane}.
     *
     * @param pane
     *        the root {@link GridPane} to which the panels are added
     */
    private void addMiddlePane(GridPane pane)
    {
        // Left Titled Pane - Processing Options
        CheckBox embedDateTimeCheck = new CheckBox("Embed Date/Time");
        embedDateTimeCheck.setId(EMBID);

        CheckBox forceDateChangeCheck = new CheckBox("Force Date Change");
        forceDateChangeCheck.setId(FORID);

        CheckBox skipVideoCheck = new CheckBox("Skip Video Files");
        skipVideoCheck.setId(SKPID);
        skipVideoCheck.setSelected(true);

        CheckBox descendingCheck = new CheckBox("Sort Descending");
        descendingCheck.setId(SRTID);

        CheckBox showMetadataCheck = new CheckBox("Display Metadata");
        showMetadataCheck.setId(SHWID);

        CheckBox debugCheck = new CheckBox("Enable Debugging");
        debugCheck.setId(DBGID);

        CheckBox[] processingChecks = {embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck, descendingCheck, debugCheck};
        VBox leftCol = new VBox(10, embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck);
        VBox rightCol = new VBox(10, showMetadataCheck, descendingCheck, debugCheck);

        for (CheckBox check : processingChecks)
        {
            check.disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        HBox checkBoxPane = new HBox(15, leftCol, rightCol);
        checkBoxPane.setPadding(new Insets(10, 5, 10, 5));

        TitledPane optionsTitledPane = new TitledPane();
        optionsTitledPane.setText("Processing Options");
        optionsTitledPane.setContent(checkBoxPane);
        optionsTitledPane.setCollapsible(false);
        optionsTitledPane.setFocusTraversable(false);
        optionsTitledPane.setMaxWidth(Double.MAX_VALUE);
        optionsTitledPane.setMaxHeight(Double.MAX_VALUE);

        // Right Titled Pane - Statistics Table
        TableView<StatRecord> statsTable = new TableView<>();
        statsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        statsTable.setFocusTraversable(false);

        statsTable.setFixedCellSize(24.0);
        statsTable.setPrefHeight(100.0);
        statsTable.setMinHeight(Region.USE_PREF_SIZE);
        statsTable.setMaxHeight(Region.USE_PREF_SIZE);

        TableColumn<StatRecord, String> metricCol = new TableColumn<>("Metric");

        metricCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<StatRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<StatRecord, String> cellData)
            {
                return cellData.getValue().metricProperty();
            }
        });

        TableColumn<StatRecord, String> valueCol = new TableColumn<>("Value");

        valueCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<StatRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<StatRecord, String> cellData)
            {
                return cellData.getValue().valueProperty();
            }
        });

        statsTable.getColumns().add(metricCol);
        statsTable.getColumns().add(valueCol);
        statsTable.getItems().addAll(StatRecord.SOURCE_FILES, StatRecord.TARGET_FILES, StatRecord.TOTAL_SIZE);

        VBox statPane = new VBox(statsTable);
        statPane.setPadding(new Insets(5));

        TitledPane statsTitledPane = new TitledPane();
        statsTitledPane.setText("Statistics");
        statsTitledPane.setContent(statPane);
        statsTitledPane.setCollapsible(false);
        statsTitledPane.setFocusTraversable(false);
        statsTitledPane.setMaxWidth(Double.MAX_VALUE);
        statsTitledPane.setMaxHeight(Double.MAX_VALUE);

        // Arrange both titled panes side by side
        HBox middleRow = new HBox(15, optionsTitledPane, statsTitledPane);
        GridPane.setHgrow(middleRow, Priority.ALWAYS);

        // Forces both inner panes to have equal 50/50 width
        optionsTitledPane.prefWidthProperty().bind(middleRow.widthProperty().subtract(15).divide(2));
        statsTitledPane.prefWidthProperty().bind(optionsTitledPane.prefWidthProperty());

        pane.add(middleRow, 0, 1);
    }

    /**
     * Builds and adds the application's log panel to the specified root grid pane.
     *
     * <p>
     * The panel contains a read-only text area used to display execution messages and status
     * information.
     * </p>
     *
     * @param pane
     *        the root {@link GridPane} to which the log panel is added
     */
    private void addLogPane(GridPane pane)
    {
        TextArea logArea = new TextArea();
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

        clearLogBtn.setUserData(logArea);
        GridPane.setHgrow(titledPane, Priority.ALWAYS);
        GridPane.setVgrow(titledPane, Priority.ALWAYS);

        pane.add(titledPane, 0, 2);

        LogFactory.addLogListener(new JavaFXLogListener(logArea));
    }

    /**
     * Builds and adds the application's actions panel to the specified root grid pane.
     *
     * <p>
     * The panel contains controls used to execute the batch process, monitor its progress, and
     * display the processing summary.
     * </p>
     *
     * @param pane
     *        the root {@link GridPane} to which the actions panel is added
     */
    private void addControlPane(GridPane pane)
    {
        ActionHandler actionHandler = new ActionHandler();

        actionBtn.setText("Run Batch Process");
        actionBtn.setOnAction(actionHandler);

        progressBar.setPrefWidth(180);
        progressBar.setMaxWidth(180);

        Label progressLabel = new Label("");
        progressLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #555555;");
        progressLabel.setMaxWidth(180);
        progressBar.setUserData(progressLabel);

        // Stack bar and label, set alignment to TOP_LEFT
        VBox progressBox = new VBox(4, progressBar, progressLabel);
        progressBox.setAlignment(Pos.TOP_LEFT);

        copyLogBtn.setText("Copy Log");
        copyLogBtn.setOnAction(actionHandler);

        cancelBtn.setDisable(true);
        cancelBtn.setText("Cancel");
        cancelBtn.setOnAction(actionHandler);

        // Align row children to TOP_LEFT so the top edges of actionBtn and progressBox match up
        HBox buttonBox = new HBox(12, actionBtn, progressBox, fillRow(), copyLogBtn, cancelBtn);
        buttonBox.setAlignment(Pos.TOP_LEFT);
        buttonBox.setPadding(new Insets(10));

        TitledPane titledPane = new TitledPane("Actions", buttonBox);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.setFocusTraversable(false);

        GridPane.setHgrow(titledPane, Priority.ALWAYS);

        pane.add(titledPane, 0, 3);
    }

    /**
     * Builds and adds the application's bottom control panel to the specified root grid pane.
     *
     * @param pane
     *        the root {@link GridPane} to which the control panel is added
     */
    /**
     * Builds and adds the application's bottom control panel.
     */
    private void addBottomPane(GridPane pane)
    {
        ActionHandler actionHandler = new ActionHandler();

        viewBtn.setText("View Summary...");
        viewBtn.setOnAction(actionHandler);
        viewBtn.prefHeightProperty().bind(actionBtn.heightProperty());

        clearLogBtn.setText("Clear Log");
        clearLogBtn.setOnAction(actionHandler);

        exitBtn.setText("Exit");
        exitBtn.setOnAction(actionHandler);

        HBox controlLayout = new HBox(10, viewBtn, clearLogBtn, fillRow(), exitBtn);
        controlLayout.setPadding(new Insets(5, 0, 0, 0));

        GridPane.setHgrow(controlLayout, Priority.ALWAYS);

        pane.add(controlLayout, 0, 4);
    }

    /**
     * Configures the dynamic behaviour of the application's user interface by attaching event
     * listeners and binding control properties.
     */
    private void configureDynamicNodes(Parent pane)
    {
        TextField sourceText = getById(SRCID);
        TextField prefixText = getById(pane, PFXID);
        DatePicker modifyDatePicker = getById(pane, DTMID);
        CheckBox showMetadataCheck = getById(pane, SHWID);

        if (prefixText != null && showMetadataCheck != null)
        {
            prefixText.disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        if (modifyDatePicker != null && showMetadataCheck != null)
        {
            modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        showMetadataCheck.selectedProperty().addListener(new InvalidationListener()
        {
            @Override
            public void invalidated(Observable observable)
            {
                actionBtn.setText(showMetadataCheck.isSelected() ? "Display Metadata" : "Run Batch Process");
            }
        });

        if (sourceText != null)
        {
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
                                sourceText.setUserData(path.isDirectory() ? pastedText : path.getParent());
                                sourceText.setTooltip(new Tooltip(pastedText));
                            }

                            else if (pastedText.contains(","))
                            {
                                String[] parts = pastedText.split(",");
                                File firstFile = new File(parts[0].trim());

                                if (firstFile.exists())
                                {
                                    sourceText.setText(pastedText);
                                    sourceText.setUserData(firstFile.getParent());
                                    sourceText.setTooltip(new Tooltip(pastedText));
                                }

                                else
                                {
                                    sourceText.setText(pastedText);
                                    sourceText.setTooltip(new Tooltip(pastedText));
                                }
                            }

                            else
                            {
                                System.out.println("Pasted path does not exist [" + pastedText + "]");
                            }
                        }

                        event.consume(); // Prevent default JavaFX handling
                    }
                }
            });
        }
    }

    private <T extends Node> T getById(String id)
    {
        return getById(stage.getScene().getRoot(), id);
    }

    /**
     * Traverses the scene graph and uses getId() to match the target ID.
     * (Java 8 Compatible)
     */
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

    /**
     * Handles action events generated by the application's user interface controls.
     */
    private class ActionHandler implements EventHandler<ActionEvent>
    {
        @Override
        public void handle(ActionEvent event)
        {
            Object source = event.getSource();

            if (source == sourceBtn)
            {
                ContextMenu menu = (ContextMenu) sourceBtn.getUserData();

                if (menu != null)
                {
                    menu.show(sourceBtn, Side.BOTTOM, 0, 0);
                }
            }

            else if (source == actionBtn)
            {
                executeBatchProcess();
            }

            else if (source == exitBtn)
            {
                Platform.exit();
            }

            else if (source == selectFiles)
            {
                handleFileSelection();
            }

            else if (source == copyLogBtn)
            {
                TextArea logArea = (TextArea) clearLogBtn.getUserData();

                if (logArea != null && !logArea.getText().isEmpty())
                {
                    // 1. Copy text to clipboard
                    ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(logArea.getText());
                    Clipboard.getSystemClipboard().setContent(content);

                    // 2. Temporarily style the selection highlight color (e.g., soft green or
                    // bright blue)
                    String originalStyle = logArea.getStyle();
                    logArea.setStyle(originalStyle + " -fx-highlight-fill: #a8e6cf; -fx-highlight-text-fill: #000000;");

                    // 3. Highlight/Select all text in logArea
                    logArea.selectAll();

                    // 4. Remove highlight after 250 milliseconds
                    PauseTransition flash = new PauseTransition(Duration.millis(550));

                    flash.setOnFinished(e ->
                    {
                        logArea.deselect();
                        logArea.setStyle(originalStyle); // Restore original style
                    });

                    flash.play();
                }
            }

            else if (source == clearLogBtn)
            {
                TextArea logArea = (TextArea) clearLogBtn.getUserData();

                if (logArea != null)
                {
                    logArea.clear();
                }
            }

            else if (source == cancelBtn)
            {
                if (activeTask != null)
                {
                    activeTask.cancelProcessor();
                }
            }

            else if (source == viewBtn)
            {
                showSummaryDialog(stage);
            }
        }
    }

    private void executeBatchProcess()
    {
        BatchConfiguration config;
        CheckBox showMetadata = getById(SHWID);
        TextArea logArea = (TextArea) clearLogBtn.getUserData();
        boolean metaDisplay = (showMetadata != null && showMetadata.isSelected());
        Label progressLabel = (Label) progressBar.getUserData();

        if (logArea != null)
        {
            logArea.clear();
            StatRecord.resetAll();

            try
            {
                config = buildConfiguration();
            }

            catch (BatchErrorException exc)
            {
                progressLabel.setText("Configuration error");
                return;
            }

            actionBtn.setDisable(true);
            cancelBtn.setDisable(false);
            activeTask = new BatchTask(config, logArea, progressBar, metaDisplay);
            progressLabel.textProperty().bind(activeTask.messageProperty());

            activeTask.stateProperty().addListener(new ChangeListener<Worker.State>()
            {
                @Override
                public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldState, Worker.State newState)
                {
                    if (newState == Worker.State.SUCCEEDED || newState == Worker.State.FAILED || newState == Worker.State.CANCELLED)
                    {
                        actionBtn.getScene().getRoot().requestFocus();
                        cancelBtn.setDisable(true);
                        actionBtn.setDisable(false);

                        BatchStatistics stats = activeTask.getValue();
                        activeTask = null; // Force GC

                        if (newState == Worker.State.SUCCEEDED)
                        {
                            Platform.runLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);

                                    if (stats != null)
                                    {
                                        StatRecord.SOURCE_FILES.setValue(stats.getSourceFilesCount());
                                        StatRecord.TARGET_FILES.setValue(stats.getTargetFilesCount());
                                        StatRecord.TOTAL_SIZE.setValue(String.format("%.2f MB", stats.getTotalTargetSizeMB()));
                                    }

                                    alert.setTitle("Process Complete");
                                    alert.setHeaderText(null);
                                    alert.setContentText("Batch processing completed");
                                    alert.initOwner(stage);
                                    alert.showAndWait();
                                }
                            });
                        }

                        new Thread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    Thread.sleep(3000);

                                    Platform.runLater(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            // Clean up UI status indicators
                                            progressLabel.textProperty().unbind();
                                            progressLabel.setText("");
                                            progressBar.progressProperty().unbind();
                                            progressBar.setProgress(0.0);
                                        }
                                    });
                                }

                                catch (InterruptedException exc)
                                {
                                    // Just pass through
                                }
                            }
                        }).start();
                    }
                }
            });

            Thread workerThread = new Thread(activeTask);

            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    /**
     * Builds a BatchConfiguration directly from the JavaFX UI controls using getId().
     */
    private BatchConfiguration buildConfiguration() throws BatchErrorException
    {
        TextField sourceText = getById(SRCID);
        String filename = sourceText.getText().trim();
        TextField targetText = getById(TGTID);
        TextField prefixText = getById(PFXID);
        DatePicker modifyDatePicker = getById(DTMID);
        LocalDate dateValue = (modifyDatePicker != null) ? modifyDatePicker.getValue() : null;
        CheckBox embedDateTime = getById(EMBID);
        CheckBox forceDateChange = getById(FORID);
        CheckBox skipVideo = getById(SKPID);
        CheckBox showMetadata = getById(SHWID);
        CheckBox descending = getById(SRTID);
        CheckBox debug = getById(DBGID);

        BatchBuilder builder = new BatchBuilder();

        if (!filename.isEmpty())
        {
            String parentDir = (String) sourceText.getUserData();

            if (parentDir != null || filename.contains(","))
            {
                String[] parts = filename.split(",");
                String[] files = new String[parts.length];

                for (int i = 0; i < parts.length; i++)
                {
                    files[i] = parts[i].trim();
                }

                builder.source(parentDir == null ? new File(filename).getParent() : parentDir).fileSet(files);
            }

            else
            {
                builder.source(filename);
            }
        }

        return builder
                .target(targetText == null ? null : targetText.getText())
                .prefix(prefixText == null ? null : prefixText.getText())
                .userDate(dateValue == null ? null : dateValue.toString())
                .embedDateTime(embedDateTime != null && embedDateTime.isSelected())
                .forceDateChange(forceDateChange != null && forceDateChange.isSelected())
                .skipVideo(skipVideo != null && skipVideo.isSelected())
                .descending(descending != null && descending.isSelected())
                .debug(debug != null && debug.isSelected())
                .showMetadata(showMetadata != null && showMetadata.isSelected())
                .build();
    }

    /**
     * Opens a file chooser to allow the user to select one or more source files.
     *
     * <p>
     * The names of the selected files are displayed in the source text field.
     * </p>
     */
    private void handleFileSelection()
    {
        TextField sourceText = getById(stage.getScene().getRoot(), SRCID);

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

    private static class StatRecord
    {
        private final SimpleStringProperty metric;
        private final SimpleStringProperty value;
        private final String defaultValue;
        static final StatRecord SOURCE_FILES = new StatRecord("Source Files", "0");
        static final StatRecord TARGET_FILES = new StatRecord("Target Files", "0");
        static final StatRecord TOTAL_SIZE = new StatRecord("Total Size", "0.00 MB");

        private StatRecord(String metric, String defaultValue)
        {
            this.metric = new SimpleStringProperty(metric);
            this.value = new SimpleStringProperty(defaultValue);
            this.defaultValue = defaultValue;
        }

        public SimpleStringProperty metricProperty()
        {
            return metric;
        }

        public SimpleStringProperty valueProperty()
        {
            return value;
        }

        @SuppressWarnings("unused")
        public String getValue()
        {
            return value.get();
        }

        public void setValue(Object ref)
        {
            value.set(String.valueOf(ref));
        }

        public void reset()
        {
            value.set(defaultValue);
        }

        public static void resetAll()
        {
            SOURCE_FILES.reset();
            TARGET_FILES.reset();
            TOTAL_SIZE.reset();
        }
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

    private Region fillRow()
    {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return spacer;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
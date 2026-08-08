package gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
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
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.scene.control.TableCell;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;
import logger.LogFactory;

/**
 * Main JavaFX application entry point for the Media Metadata Structure Viewer.
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 1 May 2026
 */
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
    private static final String FRCID = "forId";
    private static final String SKPID = "skpId";
    private static final String SHWID = "shwId";
    private static final String SRTID = "srtId";
    private static final String DBGID = "dbgId";
    private static final String TRCID = "trcId";

    /**
     * Public default constructor required by JavaFX reflection runtime.
     */
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
        fixedRow.setVgrow(Priority.NEVER);

        RowConstraints fillRow = new RowConstraints();
        fillRow.setVgrow(Priority.ALWAYS);

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

        stage.setOnShown(new EventHandler<WindowEvent>()
        {
            @Override
            public void handle(WindowEvent event)
            {
                for (Node node : formGrid.lookupAll(".titled-pane .title .text"))
                {
                    node.setStyle("-fx-font-weight: bold;");
                }
            }
        });

        stage.setScene(scene);
        stage.show();
        formGrid.requestFocus();

        configureDynamicNodes(formGrid);
    }

    /**
     * Constructs and populates the top pane containing source, target, prefix, and date input
     * fields.
     *
     * @param pane
     *        the parent {@link GridPane} container
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
        sourceText.setEditable(false);
        sourceText.setStyle("-fx-background-color: #EEEEEE; -fx-text-fill: #555555; -fx-border-color: #999999; "
                + "-fx-border-radius: 3px; -fx-background-radius: 3px; -fx-cursor: hand;");
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
     * Constructs and populates the middle pane containing processing options and execution
     * statistics.
     *
     * @param pane
     *        the parent {@link GridPane} container
     */
    private void addMiddlePane(GridPane pane)
    {
        CheckBox embedDateTimeCheck = new CheckBox("Embed Date/Time");
        embedDateTimeCheck.setId(EMBID);

        CheckBox forceDateChangeCheck = new CheckBox("Force Date Change");
        forceDateChangeCheck.setId(FRCID);

        CheckBox debugCheck = new CheckBox("Enable Debugging");
        debugCheck.setId(DBGID);

        CheckBox traceCheck = new CheckBox("Enable Trace Logging");
        traceCheck.setId(TRCID);

        CheckBox descendingCheck = new CheckBox("Sort Descending");
        descendingCheck.setId(SRTID);

        CheckBox skipVideoCheck = new CheckBox("Skip Video Files");
        skipVideoCheck.setId(SKPID);
        skipVideoCheck.setSelected(true);

        CheckBox showMetadataCheck = new CheckBox("Display Metadata");
        showMetadataCheck.setId(SHWID);

        CheckBox[] processingChecks = {embedDateTimeCheck, forceDateChangeCheck, debugCheck, traceCheck, descendingCheck, skipVideoCheck};
        VBox leftCol = new VBox(10, embedDateTimeCheck, forceDateChangeCheck, debugCheck, traceCheck);
        VBox rightCol = new VBox(10, descendingCheck, skipVideoCheck, showMetadataCheck);

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

        metricCol.setCellFactory(new Callback<TableColumn<StatRecord, String>, TableCell<StatRecord, String>>()
        {
            @Override
            public TableCell<StatRecord, String> call(TableColumn<StatRecord, String> param)
            {
                return new TableCell<StatRecord, String>()
                {
                    @Override
                    protected void updateItem(String item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        setText(empty ? null : item);
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #666666;");
                    }
                };
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

        HBox middleRow = new HBox(15, optionsTitledPane, statsTitledPane);
        GridPane.setHgrow(middleRow, Priority.ALWAYS);

        optionsTitledPane.prefWidthProperty().bind(middleRow.widthProperty().subtract(15).divide(2));
        statsTitledPane.prefWidthProperty().bind(optionsTitledPane.prefWidthProperty());

        pane.add(middleRow, 0, 1);
    }

    /**
     * Constructs and populates the log pane containing the live application execution console
     * output.
     *
     * @param pane
     *        the parent {@link GridPane} container
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
     * Constructs and populates the action control pane containing execution, progress, and cancel
     * buttons.
     *
     * @param pane
     *        the parent {@link GridPane} container
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

        VBox progressBox = new VBox(4, progressBar, progressLabel);
        progressBox.setAlignment(Pos.TOP_LEFT);

        copyLogBtn.setText("Copy Log");
        copyLogBtn.setOnAction(actionHandler);

        cancelBtn.setDisable(true);
        cancelBtn.setText("Cancel");
        cancelBtn.setOnAction(actionHandler);

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
     * Constructs and populates the bottom toolbar containing summary viewing, log clearing, and
     * exit controls.
     *
     * @param pane
     *        the parent {@link GridPane} container
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
     * Binds dynamic behaviours, listeners, and clip-board paste handling to the active UI controls.
     *
     * @param pane
     *        the primary parent container holding scene elements
     */
    private void configureDynamicNodes(Parent pane)
    {
        TextField sourceText = getById(SRCID);
        TextField prefixText = getById(pane, PFXID);
        DatePicker modifyDatePicker = getById(pane, DTMID);
        CheckBox showMetadataCheck = getById(pane, SHWID);

        if (sourceText != null)
        {
            sourceText.setOnMouseClicked(new EventHandler<MouseEvent>()
            {
                @Override
                public void handle(MouseEvent event)
                {
                    if (event.getButton() == MouseButton.PRIMARY)
                    {
                        sourceBtn.fire();
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

                                /* Firstly, find the first valid parent directory */
                                for (int i = 0; i < parts.length; i++)
                                {
                                    Path file = Paths.get(parts[i]).normalize();

                                    if (file.getParent() != null && Files.isRegularFile(file))
                                    {
                                        parentDir = file.getParent();
                                        break;
                                    }
                                }

                                boolean valid = (parentDir != null);

                                /*
                                 * Secondly, check if all files are resolved
                                 * in the first discovered directory.
                                 */
                                if (valid)
                                {
                                    for (int i = 0; i < parts.length; i++)
                                    {
                                        Path file = Paths.get(parts[i]);

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
                                sourceText.setUserData(valid ? parentDir.toFile().getAbsolutePath() : null);
                            }

                            else
                            {
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

        showMetadataCheck.selectedProperty().addListener(new InvalidationListener()
        {

            @Override
            public void invalidated(Observable observable)
            {
                actionBtn.setText(showMetadataCheck.isSelected() ? "Display Metadata" : "Run Batch Process");
            }

        });
    }

    /**
     * Convenience method to retrieve a FX control by its ID starting from the root scene node.
     *
     * @param <T>
     *        the expected node type
     * @param id
     *        the target JavaFX FXID string
     * @return the matching node casting to type {@code T}, or {@code null} if not found
     */
    private <T extends Node> T getById(String id)
    {
        return getById(stage.getScene().getRoot(), id);
    }

    /**
     * Recursively traverses a node hierarchy to locate a node with the given ID string.
     *
     * @param <T>
     *        the expected node type
     * @param root
     *        the parent root node from which traversal begins
     * @param id
     *        the target JavaFX FXID string
     * @return the matching node casting to type {@code T}, or {@code null} if not found
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
     * Event handler implementation that processes user UI button clicks and control selections.
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

    /**
     * Instantiates and runs the asynchronous background batch processing task.
     */
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
                launchPopup("Invalid File Selection", exc.getMessage(), AlertType.ERROR);
                return;
            }

            activeTask = new BatchTask(config, logArea, progressBar, metaDisplay);

            activeTask.setOnScanCompleted(new Consumer<Integer>()
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

            activeTask.setOnFileProcessed(new Consumer<Integer>()
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

            progressLabel.textProperty().bind(activeTask.messageProperty());

            actionBtn.setDisable(true);
            cancelBtn.setDisable(false);
            copyLogBtn.setDisable(true);

            activeTask.setOnSucceeded(new EventHandler<WorkerStateEvent>()
            {
                @Override
                public void handle(WorkerStateEvent event)
                {
                    BatchStatistics stats = activeTask.getValue();

                    if (stats != null)
                    {
                        StatRecord.TOTAL_SIZE.setValue(String.format("%.2f MB", stats.getTotalTargetSizeMB()));
                    }

                    resetControlStates(progressLabel);
                    launchPopup("Process Complete", "Batch processing completed", AlertType.INFORMATION);
                }
            });

            activeTask.setOnFailed(new EventHandler<WorkerStateEvent>()
            {
                @Override
                public void handle(WorkerStateEvent event)
                {
                    Throwable exc = activeTask.getException();
                    String msg = (exc != null && exc.getMessage() != null ? exc.getMessage() : "An unknown error occurred.");

                    resetControlStates(progressLabel);
                    launchPopup("Processing Error", msg, AlertType.ERROR);
                }
            });

            activeTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
            {
                @Override
                public void handle(WorkerStateEvent event)
                {
                    resetControlStates(progressLabel);
                }
            });

            Thread worker = new Thread(activeTask);

            worker.setDaemon(true);
            worker.start();
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
        actionBtn.getScene().getRoot().requestFocus();
        cancelBtn.setDisable(true);
        actionBtn.setDisable(false);
        copyLogBtn.setDisable(false);
        activeTask = null;

        PauseTransition delay = new PauseTransition(Duration.seconds(3));

        delay.setOnFinished(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                progressLabel.textProperty().unbind();
                progressLabel.setText("");
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0.0);
            }
        });

        delay.play();
    }

    /**
     * Helper method to display modal dialog alerts to the user.
     *
     * @param title
     *        the title string for the alert window
     * @param msg
     *        the message content string
     * @param type
     *        the {@link AlertType} defining the severity level
     */
    private void launchPopup(String title, String msg, AlertType type)
    {
        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    /**
     * Constructs a {@link BatchConfiguration} instance from the active UI controls.
     * 
     * <p>
     * Extracts values from input fields, pickers, and check boxes across the interface. Validates
     * that a source directory or file set has been selected before delegating to the
     * {@link BatchBuilder}. Supports single directories, explicit file sets, and comma-separated
     * lists.
     * </p>
     *
     * @return a fully populated {@link BatchConfiguration} ready for execution
     * 
     * @throws BatchErrorException
     *         if the source input is empty or if comma-separated files are specified without an
     *         associated parent directory context
     */
    private BatchConfiguration buildConfiguration() throws BatchErrorException
    {
        BatchBuilder builder = new BatchBuilder();
        TextField sourceText = getById(SRCID);
        TextField targetText = getById(TGTID);
        TextField prefixText = getById(PFXID);
        DatePicker modifyDatePicker = getById(DTMID);
        LocalDate dateValue = (modifyDatePicker != null ? modifyDatePicker.getValue() : null);
        CheckBox embedDateTime = getById(EMBID);
        CheckBox forceDateChange = getById(FRCID);
        CheckBox skipVideo = getById(SKPID);
        CheckBox showMetadata = getById(SHWID);
        CheckBox descending = getById(SRTID);
        CheckBox debug = getById(DBGID);
        CheckBox trace = getById(TRCID);
        String filename = (sourceText != null ? sourceText.getText().trim() : "");

        if (filename.isEmpty())
        {
            throw new BatchErrorException("No source directory or files specified.\n\nPlease select a source folder or specific files before running the batch process.");
        }

        String parentDir = (String) sourceText.getUserData();

        if (filename.contains(","))
        {
            if (parentDir == null || parentDir.trim().isEmpty())
            {
                throw new BatchErrorException("Individual files detected without a parent folder context.\n\nPlease use the 'Select Specific Files' menu option to select files.");
            }

            String[] parts = filename.split("\\s*,\\s*");
            String[] files = new String[parts.length];

            for (int i = 0; i < parts.length; i++)
            {
                // Only basic file names are accepted
                files[i] = Paths.get(parts[i]).getFileName().toString();
            }

            builder.source(parentDir).fileSet(files);
        }

        else
        {
            builder.source(filename);
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
                .trace(trace != null && trace.isSelected())
                .build();
    }

    /**
     * Opens a system native {@link FileChooser} dialog allowing users to select multiple input
     * files.
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

    /**
     * Model representing a key-value metric pair inside the processing statistics table view.
     */
    private static class StatRecord
    {
        private static final StatRecord SOURCE_FILES = new StatRecord("Source Files", "0");
        private static final StatRecord TARGET_FILES = new StatRecord("Target Files", "0");
        private static final StatRecord TOTAL_SIZE = new StatRecord("Total Size", "0.00 MB");
        private final SimpleStringProperty metric;
        private final SimpleStringProperty value;
        private final String defaultValue;

        /**
         * Constructs a new statistic metric record.
         *
         * @param metric
         *        the metric label
         * @param defaultValue
         *        the default value string
         */
        private StatRecord(String metric, String defaultValue)
        {
            this.metric = new SimpleStringProperty(metric);
            this.value = new SimpleStringProperty(defaultValue);
            this.defaultValue = defaultValue;
        }

        /**
         * @return the metric property
         */
        public SimpleStringProperty metricProperty()
        {
            return metric;
        }

        /**
         * @return the value property
         */
        public SimpleStringProperty valueProperty()
        {
            return value;
        }

        /**
         * @return the current metric value string
         */
        @SuppressWarnings("unused")
        public String getValue()
        {
            return value.get();
        }

        /**
         * Updates the metric value.
         *
         * @param ref
         *        the object whose string representation will be set
         */
        public void setValue(Object ref)
        {
            value.set(String.valueOf(ref));
        }

        /**
         * Resets the value back to its default state.
         */
        public void reset()
        {
            value.set(defaultValue);
        }

        /**
         * Resets all static metric records to default baseline values.
         */
        public static void resetAll()
        {
            SOURCE_FILES.reset();
            TARGET_FILES.reset();
            TOTAL_SIZE.reset();
        }
    }

    /**
     * Model representing an individual file processing result entry in the summary dialog table
     * view.
     */
    public static class FileRecord
    {
        private final SimpleStringProperty sourceName;
        private final SimpleStringProperty targetName;
        private final SimpleStringProperty status;

        /**
         * Constructs a file execution record.
         *
         * @param sourceName
         *        the original source filename
         * @param targetName
         *        the output target filename
         * @param status
         *        the processing status outcome string
         */
        public FileRecord(String sourceName, String targetName, String status)
        {
            this.sourceName = new SimpleStringProperty(sourceName);
            this.targetName = new SimpleStringProperty(targetName);
            this.status = new SimpleStringProperty(status);
        }

        /**
         * @return the source name property
         */
        public SimpleStringProperty sourceNameProperty()
        {
            return sourceName;
        }

        /**
         * @return the target name property
         */
        public SimpleStringProperty targetNameProperty()
        {
            return targetName;
        }

        /**
         * @return the status property
         */
        public SimpleStringProperty statusProperty()
        {
            return status;
        }
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

        TableView<FileRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FileRecord, String> sourceCol = new TableColumn<>("Source File");
        sourceCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileRecord, String> cellData)
            {
                return cellData.getValue().sourceNameProperty();
            }
        });
        sourceCol.setPrefWidth(200);

        TableColumn<FileRecord, String> targetCol = new TableColumn<>("Target File");
        targetCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<FileRecord, String> cellData)
            {
                return cellData.getValue().targetNameProperty();
            }
        });
        targetCol.setPrefWidth(200);

        TableColumn<FileRecord, String> statusCol = new TableColumn<>("Status");
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

    /**
     * Generates a dynamic horizontal expansion spacer region for UI layouts.
     *
     * @return a configured {@link Region} spacer
     */
    private Region fillRow()
    {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return spacer;
    }

    /**
     * Standard Java application entry point.
     *
     * @param args
     *        command-line arguments passed to the application
     */
    public static void main(String[] args)
    {
        launch(args);
    }
}
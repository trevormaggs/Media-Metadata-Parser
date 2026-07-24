package gui;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import batch.BatchBuilder;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.DisplayMetadata;
import batch.MediaBatchProcessor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import progressbar.JavaFXProgressAdapter;

public class MediaMetadataApp2 extends Application
{
    private final TextField sourceText;
    private final Button sourceBtn;
    private final MenuItem selectFiles;
    private final CheckBox showMetadataCheck;
    private final TextField prefixText;
    private final DatePicker modifyDatePicker;
    private final Button actionBtn;
    private final Button cancelBtn;
    private final TextArea logArea;
    private final Button clearLogBtn;
    private final Button exitBtn;
    private final Button viewBtn;
    private final ProgressBar progressBar;
    private Stage stage;

    private final ContextMenu sourceMenu; 
    
    private static final String SRCID = "srcId";
    private static final String TGTID = "tgtId";
    private static final String PFXID = "pfxId";
    private static final String DTMID = "dtmId";
    private static final String EMBID = "embId";
    private static final String FORID = "forId";
    private static final String SKPID = "skpId";
    private static final String SRTID = "srtId";
    private static final String DBGID = "dbgId";

    public MediaMetadataApp2()
    {
        this.sourceMenu = new ContextMenu();
        
        this.sourceBtn = new Button();
        this.sourceText = new TextField();
        this.selectFiles = new MenuItem();
        this.showMetadataCheck = new CheckBox();
        this.prefixText = new TextField();
        this.modifyDatePicker = new DatePicker();
        this.actionBtn = new Button();
        this.cancelBtn = new Button();
        this.logArea = new TextArea();
        this.clearLogBtn = new Button();
        this.exitBtn = new Button();
        this.viewBtn = new Button();
        this.progressBar = new ProgressBar(0.0);
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

        configureDynamicNodes();
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
        HBox sourceHbox = new HBox(10);
        Label sourceLabel = new Label("Source Directory");
        sourceLabel.setPrefWidth(labelWidth);
        sourceText.setPromptText("Directory or files...");
        sourceText.setPrefWidth(300);
        sourceText.setMaxWidth(300);
        sourceText.setText("E:\\ImageBatchDir");
        MenuItem selectFolder = new MenuItem("Select Folder...");
        selectFolder.setOnAction(new DirectoryPopupHandler(sourceText, "Select Source Directory"));
        selectFiles.setText("Select Specific Files...");
        selectFiles.setOnAction(actionHandler);
        sourceMenu.getItems().addAll(selectFolder, selectFiles);
        sourceBtn.setText("Browse...");
        sourceBtn.setOnAction(actionHandler);
        sourceHbox.getChildren().addAll(sourceLabel, sourceText, fillRow(), sourceBtn);

        // Row 2
        HBox targetHbox = new HBox(10);
        Label targetLabel = new Label("Target Directory");
        targetLabel.setPrefWidth(labelWidth);
        TextField targetText = new TextField();
        targetText.setId("targetText");
        targetText.setText(MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY);
        targetText.setPrefWidth(300);
        targetText.setMaxWidth(300);
        Button targetBtn = new Button("Browse...");
        targetBtn.setOnAction(new DirectoryPopupHandler(targetText, "Select Target Directory"));
        targetHbox.getChildren().addAll(targetLabel, targetText, fillRow(), targetBtn);

        // Row 3
        HBox prefixHbox = new HBox(10);
        Label prefixLabel = new Label("File Prefix Name");
        prefixLabel.setPrefWidth(labelWidth);
        prefixText.setText(MediaBatchProcessor.DEFAULT_IMAGE_PREFIX);
        prefixText.setPromptText("Example: Holiday_Trip_");
        prefixText.setPrefWidth(300);
        prefixText.setMaxWidth(300);
        prefixHbox.getChildren().addAll(prefixLabel, prefixText, fillRow());

        // Row 4
        HBox modifyDateHbox = new HBox(10);
        Label dateLabel = new Label("Modify Date Taken");
        dateLabel.setPrefWidth(labelWidth);
        modifyDatePicker.setPromptText("Select date...");
        modifyDatePicker.setPrefWidth(300);
        modifyDatePicker.setMaxWidth(300);
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
    private void addMiddlePane(GridPane pane)
    {
        showMetadataCheck.setText("Display Metadata");

        // Left Titled Pane - Processing Options 
        TitledPane optionsTitledPane = new TitledPane();
        optionsTitledPane.setText("Processing Options");

        CheckBox embedDateTimeCheck = new CheckBox("Embed Date/Time");
        embedDateTimeCheck.setId("embedDateTimeCheck");

        CheckBox forceDateChangeCheck = new CheckBox("Force Date Change");
        forceDateChangeCheck.setId("forceDateChangeCheck");

        CheckBox skipVideoCheck = new CheckBox("Skip Video Files");
        skipVideoCheck.setId("skipVideoCheck");
        skipVideoCheck.setSelected(true);

        CheckBox descendingCheck = new CheckBox("Sort Descending");
        descendingCheck.setId("descendingCheck");

        CheckBox debugCheck = new CheckBox("Enable Debugging");
        debugCheck.setId("debugCheck");

        CheckBox[] processingChecks = {embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck, descendingCheck, debugCheck};
        VBox leftCol = new VBox(10, embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck);
        VBox rightCol = new VBox(10, showMetadataCheck, descendingCheck, debugCheck);

        for (CheckBox check : processingChecks)
        {
            check.disableProperty().bind(showMetadataCheck.selectedProperty());
        }

        HBox checkBoxPane = new HBox(15, leftCol, rightCol);
        checkBoxPane.setPadding(new Insets(10, 5, 10, 5));

        optionsTitledPane.setContent(checkBoxPane);
        optionsTitledPane.setCollapsible(false);
        optionsTitledPane.setFocusTraversable(false);
        optionsTitledPane.setMaxWidth(Double.MAX_VALUE);
        optionsTitledPane.setMaxHeight(Double.MAX_VALUE);

        // Right Titled Pane - Statistics
        TitledPane statsTitledPane = new TitledPane();
        statsTitledPane.setText("Statistics");

        VBox statPane = new VBox(8);
        statPane.setPadding(new Insets(10));

        // Label statLabel = new Label("Statistics");
        // statLabel.setStyle("-fx-font-weight: bold;");
        // statPane.getChildren().add(statLabel);

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
        statsTitledPane.prefWidthProperty().bind(middleRow.widthProperty().subtract(15).divide(2));

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
        VBox.setVgrow(logArea, Priority.ALWAYS);

        logArea.setEditable(false);
        logArea.setFocusTraversable(false);
        logArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
        logArea.setPromptText("Console output...");
        logArea.setMaxWidth(Double.MAX_VALUE);
        logArea.setMaxHeight(Double.MAX_VALUE);

        VBox logContent = new VBox(logArea);

        TitledPane titledPane = new TitledPane("Execution Log", logContent);
        titledPane.setCollapsible(false);
        titledPane.setMaxWidth(Double.MAX_VALUE);
        titledPane.setFocusTraversable(false);

        GridPane.setHgrow(titledPane, Priority.ALWAYS);
        GridPane.setVgrow(titledPane, Priority.ALWAYS);

        pane.add(titledPane, 0, 2);
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

        progressBar.setPrefWidth(220);
        progressBar.prefHeightProperty().bind(actionBtn.heightProperty());

        viewBtn.setText("View Summary...");
        viewBtn.setOnAction(actionHandler);
        viewBtn.prefHeightProperty().bind(actionBtn.heightProperty());

        HBox buttonBox = new HBox(15, actionBtn, progressBar, fillRow(), viewBtn);
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
    private void addBottomPane(GridPane pane)
    {
        ActionHandler actionHandler = new ActionHandler();

        cancelBtn.setDisable(true);
        cancelBtn.setText("Cancel Process");
        cancelBtn.setOnAction(actionHandler);
        clearLogBtn.setText("Clear Log");
        clearLogBtn.setOnAction(actionHandler);
        exitBtn.setText("Exit");
        exitBtn.setOnAction(actionHandler);

        HBox controlLayout = new HBox(10, cancelBtn, clearLogBtn, fillRow(), exitBtn);
        controlLayout.setPadding(new Insets(5, 0, 0, 0));

        GridPane.setHgrow(controlLayout, Priority.ALWAYS);

        pane.add(controlLayout, 0, 4);
    }

    /**
     * Configures the dynamic behaviour of the application's user interface by attaching event
     * listeners and binding control properties.
     */
    private void configureDynamicNodes()
    {
        prefixText.disableProperty().bind(showMetadataCheck.selectedProperty());
        modifyDatePicker.disableProperty().bind(showMetadataCheck.selectedProperty());

        showMetadataCheck.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                if (newValue != null && newValue)
                {
                    actionBtn.setText("Display Metadata");
                }

                else
                {
                    actionBtn.setText("Run Batch Process");
                }
            }
        });
    }

    /**
     * Executes the operation on a background thread using UI input parameters.
     */
    private void executeBatchProcess()
    {
        BatchConfiguration config;
        boolean metaDisplay = showMetadataCheck.isSelected();

        logArea.clear();
        logArea.appendText("[INFO] Initializing batch process...\n");

        actionBtn.setDisable(true);
        cancelBtn.setDisable(false);

        try
        {
            config = buildConfiguration();
        }

        catch (BatchErrorException exc)
        {
            logArea.appendText("[ERROR] Configuration error: " + exc.getMessage() + "\n");
            actionBtn.setDisable(false);
            cancelBtn.setDisable(true);
            return;
        }

        // Dispatch job to background thread
        Thread workerThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (metaDisplay)
                    {
                        // Metadata display path
                        DisplayMetadata display = new DisplayMetadata(config);
                        display.execute();

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                logArea.appendText("\n[SUCCESS] Exif data retrieved successfully.\n");
                            }
                        });
                    }

                    else
                    {
                        MediaBatchProcessor processor = new MediaBatchProcessor(config);

                        processor.addProgressListener(new JavaFXProgressAdapter(progressBar));
                        processor.execute();

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                logArea.appendText("\n[SUCCESS] Batch processing complete.\n");
                            }
                        });
                    }
                }

                catch (BatchErrorException exc)
                {
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            logArea.appendText("[ERROR] " + exc.getMessage() + "\n");
                        }
                    });
                }

                catch (Exception exc)
                {
                    exc.printStackTrace();
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            logArea.appendText("[ERROR] Unexpected error: " + exc.getMessage() + "\n");
                        }
                    });
                }

                finally
                {
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            cancelBtn.setDisable(true);
                            actionBtn.setDisable(false);
                        }
                    });
                }
            }
        });

        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Builds a BatchConfiguration directly from the JavaFX UI controls using getId().
     */
    private BatchConfiguration buildConfiguration() throws BatchErrorException
    {
        Parent root = stage.getScene().getRoot();

        // Retrieve fields dynamically by ID
        TextField targetText = getById(root, "targetText");
        CheckBox embedDateTime = getById(root, "embedDateTimeCheck");
        CheckBox forceDateChange = getById(root, "forceDateChangeCheck");
        CheckBox skipVideo = getById(root, "skipVideoCheck");
        CheckBox descending = getById(root, "descendingCheck");
        CheckBox debug = getById(root, "debugCheck");

        String targetPath = (targetText != null ? targetText.getText() : null);
        LocalDate dateValue = modifyDatePicker.getValue();
        String userDateStr = (dateValue != null ? dateValue.toString() : null);

        BatchBuilder builder = new BatchBuilder()
                .source(sourceText.getText())
                .target(targetPath)
                .prefix(prefixText.getText())
                .userDate(userDateStr)
                .showMetadata(showMetadataCheck.isSelected());

        if (embedDateTime != null) builder.embedDateTime(embedDateTime.isSelected());
        if (forceDateChange != null) builder.forceDateChange(forceDateChange.isSelected());
        if (skipVideo != null) builder.skipVideo(skipVideo.isSelected());
        if (descending != null) builder.descending(descending.isSelected());
        if (debug != null) builder.debug(debug.isSelected());

        return builder.build();
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

    private void cancelBatchProcess()
    {
        logArea.appendText("[WARNING] Batch process cancellation requested.\n");
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
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Source Files");

        File defaultDir = new File("E:\\ImageBatchDir");

        if (defaultDir.exists() && defaultDir.isDirectory())
        {
            chooser.setInitialDirectory(defaultDir);
        }

        List<File> files = chooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty())
        {
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

    private class DirectoryPopupHandler implements EventHandler<ActionEvent>
    {
        private final TextField targetField;
        private final String dialogTitle;

        public DirectoryPopupHandler(TextField text, String title)
        {
            this.targetField = text;
            this.dialogTitle = title;
        }

        @Override
        public void handle(ActionEvent event)
        {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(dialogTitle);

            File defaultDir = new File("E:\\ImageBatchDir");

            if (defaultDir.exists() && defaultDir.isDirectory())
            {
                chooser.setInitialDirectory(defaultDir);
            }

            Window window = targetField.getScene().getWindow();
            File folder = chooser.showDialog(window);

            if (folder != null)
            {
                targetField.setText(folder.getAbsolutePath());
            }
        }
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

            if (source == sourceBtn && sourceMenu != null)
            {
                sourceMenu.show(sourceBtn, Side.BOTTOM, 0, 0);
            }

            else if (source == clearLogBtn)
            {
                logArea.clear();
            }

            else if (source == exitBtn)
            {
                Platform.exit();
            }

            else if (source == viewBtn)
            {
                showSummaryDialog(stage);
            }

            else if (source == cancelBtn)
            {
                cancelBatchProcess();
            }

            else if (source == actionBtn)
            {
                executeBatchProcess();
            }

            else if (source == selectFiles)
            {
                handleFileSelection();
            }
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
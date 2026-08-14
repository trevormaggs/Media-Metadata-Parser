package gui;

import batch.MediaBatchProcessor;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import logger.LogFactory;

/**
 * View construction pane for the Media Metadata Structure Viewer interface. Builds layout panels
 * and assigns dynamic FXIDs to UI controls for configuration building.
 */
final class MainViewPane
{
    public static final String SRCID = "srcId";
    public static final String TGTID = "tgtId";
    public static final String PFXID = "pfxId";
    public static final String DTMID = "dtmId";
    public static final String EMBID = "embId";
    public static final String FRCID = "forId";
    public static final String SKPID = "skpId";
    public static final String SHWID = "shwId";
    public static final String SRTID = "srtId";
    public static final String DBGID = "dbgId";
    public static final String TRCID = "trcId";

    private final ProgressBar progressBar;
    private final Button sourceBtn;
    private final MenuItem selectFiles;
    private final Button actionBtn;
    private final Button copyLogBtn;
    private final Button cancelBtn;
    private final Button viewBtn;
    private final Button clearLogBtn;
    private final Button exitBtn;

    MainViewPane()
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
     * Populates the provided GridPane container with all sub-panes.
     *
     * @param pane
     *        the target container managed by MediaMetadataGUI
     */
    void buildLayout(GridPane pane)
    {
        addTopPane(pane);
        addMiddlePane(pane);
        addLogPane(pane);
        addControlPane(pane);
        addBottomPane(pane);
    }

    /**
     * Constructs and populates the top pane containing source, target, prefix, and date input
     * fields.
     */
    private void addTopPane(GridPane pane)
    {
        double labelWidth = 140;

        // Row 1
        Label sourceLabel = new Label("Source Directory");
        sourceLabel.setPrefWidth(labelWidth);
        TextField sourceText = new TextField();
        sourceText.setId(SRCID);
        sourceText.setPromptText("Directory or files...");
        sourceText.setPrefWidth(300);
        sourceText.setMaxWidth(300);
        sourceText.setEditable(false);
        sourceText.getStyleClass().add("read-only-path-field");
        sourceText.setText("E:\\ImageBatchDir\\babygemma.tif");

        MenuItem selectFolder = new MenuItem("Select Folder...");
        selectFolder.setOnAction(new FilePickHandler(sourceText, "Select Source Directory"));
        selectFiles.setText("Select Specific Files...");

        ContextMenu sourceMenu = new ContextMenu();
        sourceMenu.getItems().addAll(selectFolder, selectFiles);
        sourceBtn.setText("Browse...");
        sourceBtn.setUserData(sourceMenu);

        HBox sourceHbox = new HBox(10);
        sourceHbox.getChildren().addAll(sourceLabel, sourceText, GUIUtils.fillRow(), sourceBtn);

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
        targetHbox.getChildren().addAll(targetLabel, targetText, GUIUtils.fillRow(), targetBtn);

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
        prefixHbox.getChildren().addAll(prefixLabel, prefixText, GUIUtils.fillRow());

        // Row 4
        Label dateLabel = new Label("Modify Date Taken");
        dateLabel.setPrefWidth(labelWidth);
        DatePicker modifyDatePicker = new DatePicker();
        modifyDatePicker.setId(DTMID);
        modifyDatePicker.setPromptText("Select date...");
        modifyDatePicker.setPrefWidth(300);
        modifyDatePicker.setMaxWidth(300);

        HBox modifyDateHbox = new HBox(10);
        modifyDateHbox.getChildren().addAll(dateLabel, modifyDatePicker, GUIUtils.fillRow());

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
        skipVideoCheck.setSelected(true);
        skipVideoCheck.setId(SKPID);

        CheckBox showMetadataCheck = new CheckBox("Display Metadata");
        showMetadataCheck.setId(SHWID);

        CheckBox[] processingChecks = new CheckBox[]{
                embedDateTimeCheck, forceDateChangeCheck, debugCheck, traceCheck, descendingCheck, skipVideoCheck
        };

        VBox leftCol = new VBox(10, embedDateTimeCheck, forceDateChangeCheck, debugCheck, traceCheck);
        VBox rightCol = new VBox(10, descendingCheck, skipVideoCheck, showMetadataCheck);

        for (CheckBox processingCheck : processingChecks)
        {
            processingCheck.disableProperty().bind(showMetadataCheck.selectedProperty());
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
        statsTable.prefHeightProperty().bind(optionsTitledPane.heightProperty());

        TableColumn<StatRecord, String> metricCol = new TableColumn<>("Metric");
        metricCol.getStyleClass().add("metric-column");
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
        statsTable.getItems().addAll(StatRecord.SOURCE_FILES, StatRecord.TARGET_FILES, StatRecord.FILES_SKIPPED, StatRecord.TOTAL_SIZE);

        HBox middleRow = new HBox(15, optionsTitledPane, statsTable);
        GridPane.setHgrow(middleRow, Priority.ALWAYS);

        optionsTitledPane.prefWidthProperty().bind(middleRow.widthProperty().subtract(15).divide(2));
        statsTable.prefWidthProperty().bind(optionsTitledPane.prefWidthProperty());

        pane.add(middleRow, 0, 1);
    }

    /**
     * Constructs and populates the log pane containing the execution console output.
     */
    private void addLogPane(GridPane pane)
    {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFocusTraversable(false);
        logArea.getStyleClass().add("log-area");
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
     */
    private void addControlPane(GridPane pane)
    {
        actionBtn.setText("Run Batch Process");

        progressBar.setPrefWidth(180);
        progressBar.setMaxWidth(180);

        Label progressLabel = new Label("");
        progressLabel.getStyleClass().add("progress-label");
        progressLabel.setMaxWidth(180);
        progressBar.setUserData(progressLabel);

        VBox progressBox = new VBox(4, progressBar, progressLabel);
        progressBox.setAlignment(Pos.TOP_LEFT);

        copyLogBtn.setText("Copy Log");
        cancelBtn.setDisable(true);
        cancelBtn.setText("Cancel");

        HBox buttonBox = new HBox(12, actionBtn, progressBox, GUIUtils.fillRow(), copyLogBtn, cancelBtn);
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
     */
    private void addBottomPane(GridPane pane)
    {
        viewBtn.setText("View Summary...");
        viewBtn.prefHeightProperty().bind(actionBtn.heightProperty());

        clearLogBtn.setText("Clear Log");
        exitBtn.setText("Exit");

        HBox controlLayout = new HBox(10, viewBtn, clearLogBtn, GUIUtils.fillRow(), exitBtn);
        controlLayout.setPadding(new Insets(5, 0, 0, 0));

        GridPane.setHgrow(controlLayout, Priority.ALWAYS);

        pane.add(controlLayout, 0, 4);
    }

    /**
     * Utility method to dynamically search for controls by FXID inside a node hierarchy.
     */
    static <T extends Node> T getById(Node root, String id)
    {
        return GUIUtils.getById(root, id);
    }

    Button getSourceBtn()
    {
        return sourceBtn;
    }

    MenuItem getSelectFiles()
    {
        return selectFiles;
    }

    Button getActionBtn()
    {
        return actionBtn;
    }

    Button getClearLogBtn()
    {
        return clearLogBtn;
    }

    Button getCopyLogBtn()
    {
        return copyLogBtn;
    }

    Button getExitBtn()
    {
        return exitBtn;
    }

    ProgressBar getProgressBar()
    {
        return progressBar;
    }

    Button getCancelBtn()
    {
        return cancelBtn;
    }

    Button getViewBtn()
    {
        return viewBtn;
    }
}
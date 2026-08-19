package gui;

import batch.MediaBatchProcessor;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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
    static final String SRCID = "srcId";
    static final String TGTID = "tgtId";
    static final String PFXID = "pfxId";
    static final String DTMID = "dtmId";
    static final String EMBID = "embId";
    static final String FRCID = "forId";
    static final String SKPID = "skpId";
    static final String SHWID = "shwId";
    static final String SRTID = "srtId";
    static final String DBGID = "dbgId";
    static final String TRCID = "trcId";

    final ProgressBar progressBar;
    final Button sourceBtn;
    final Button actionBtn;
    final Button copyLogBtn;
    final Button abortBtn;
    final Button viewBtn;
    final Button clearLogBtn;
    final Button exitBtn;
    private Label previewValueLabel;

    MainViewPane()
    {
        this.progressBar = new ProgressBar(0.0);
        this.sourceBtn = new Button();
        this.actionBtn = new Button();
        this.clearLogBtn = new Button();
        this.copyLogBtn = new Button();
        this.exitBtn = new Button();
        this.abortBtn = new Button();
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
     * Retrieves a node by ID and safely casts it to the expected type.
     *
     * @param <T>
     *        the expected node type
     * @param root
     *        the root node to begin searching from
     * @param id
     *        the target JavaFX ID
     * @param type
     *        the expected Class token, for example: TextField.class
     * @return the matching node cast to {@code T}
     *
     * @throws NoSuchElementException
     *         if no node with the ID exists
     * @throws IllegalArgumentException
     *         if the node exists but is not an instance of {@code type}
     */
    static <T extends Node> T getById(Node root, String id, Class<T> type)
    {
        Node node = GUIUtils.getById(root, id);

        if (node == null)
        {
            throw new NoSuchElementException("Node ID [" + id + "] not found in the layout hierarchy");
        }

        if (!type.isInstance(node))
        {
            throw new IllegalArgumentException("Node ID [" + id + "] is of type " + node.getClass().getName() + ", but expected " + type.getName());
        }

        return type.cast(node);
    }

    /**
     * Recalculates the preview text based on current input field values retrieved by FXID.
     */
    void updatePreview(GridPane pane)
    {
        if (previewValueLabel != null)
        {
            TextField prefixText = getById(pane, PFXID, TextField.class);
            CheckBox embedDateTimeCheck = getById(pane, EMBID, CheckBox.class);
            DatePicker modifyDatePicker = getById(pane, DTMID, DatePicker.class);
            StringBuilder sb = new StringBuilder();
            String prefix = prefixText.getText().trim();

            if (!prefix.isEmpty())
            {
                sb.append(prefix).append("_");
            }

            if (embedDateTimeCheck.isSelected())
            {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMMyyyy");

                if (modifyDatePicker.getValue() != null)
                {
                    sb.append(modifyDatePicker.getValue().format(formatter)).append("_");
                }

                else
                {
                    sb.append(LocalDate.now().format(formatter)).append("_");
                }
            }

            sb.append(String.format("%04d", 1)).append(".jpg");

            previewValueLabel.setText(sb.toString());
        }
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

        sourceBtn.setText("Browse...");

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

        // Row 5: Dynamic Filename Preview
        Label previewTitleLabel = new Label("Target Preview");
        previewTitleLabel.setPrefWidth(labelWidth);

        previewValueLabel = new Label();
        previewValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #005A9E;");

        HBox previewHbox = new HBox(10);
        previewHbox.getChildren().addAll(previewTitleLabel, previewValueLabel, GUIUtils.fillRow());

        VBox contentPane = new VBox(12);
        contentPane.setPadding(new Insets(10));
        contentPane.getChildren().addAll(sourceHbox, targetHbox, prefixHbox, modifyDateHbox, previewHbox);

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
                embedDateTimeCheck,
                forceDateChangeCheck,
                debugCheck,
                traceCheck,
                descendingCheck,
                skipVideoCheck
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
        abortBtn.setDisable(true);
        abortBtn.setText("Abort");

        HBox buttonBox = new HBox(12, actionBtn, progressBox, GUIUtils.fillRow(), copyLogBtn, abortBtn);
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
}
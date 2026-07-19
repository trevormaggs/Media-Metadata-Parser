package gui;

import batch.BatchBuilder;
import batch.BatchErrorException;
import batch.MediaBatchProcessor;
import batch.MediaMetadataConsole;
import java.io.File;
import java.io.PrintStream;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class MediaMetadataApp extends Application
{
    private Stage stage;

    // UI Input Widgets
    private TextField sourceField;
    private TextField targetField;
    private TextField prefixField;
    private TextField userDateField;

    // UI Toggle Switches
    private CheckBox embedDateTimeCheck;
    private CheckBox forceDateChangeCheck;
    private CheckBox skipVideoCheck;
    private CheckBox showMetadataCheck;
    private CheckBox descendingCheck;
    private CheckBox debugCheck;

    // UI Output Log Console
    private TextArea logArea;

    @Override
    public void start(Stage stage)
    {
        this.stage = stage;
        stage.setTitle("Image Metadata Structure Viewer");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(15));

        addTopPane(formGrid);
        addMiddlePane(formGrid);

        Scene scene = new Scene(formGrid, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    private void addTopPane(GridPane pane)
    {
        // ROW 1
        Label sourceLabel = new Label("Source Directory: ");
        sourceLabel.setPrefWidth(100);

        TextField sourceField = new TextField();
        sourceField.setPromptText("Directory containing original media...");
        sourceField.setPrefWidth(300);
        sourceField.setMaxWidth(300);

        Button browseSourceBtn = new Button("Browse...");
        browseSourceBtn.setOnAction(new BrowseSourceHandler());

        Region sourceSpacer = new Region();
        HBox.setHgrow(sourceSpacer, Priority.ALWAYS);

        HBox sourceRowLayout = new HBox(10);
        sourceRowLayout.getChildren().addAll(sourceLabel, sourceField, sourceSpacer, browseSourceBtn);

        // ROW 2
        Label targetLabel = new Label("Target Directory: ");
        targetLabel.setPrefWidth(100);

        TextField targetField = new TextField();
        targetField.setText(MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY);
        targetField.setPrefWidth(300);
        targetField.setMaxWidth(300);

        Button browseTargetBtn = new Button("Browse...");
        browseTargetBtn.setOnAction(new BrowseTargetHandler());

        Region targetSpacer = new Region();
        HBox.setHgrow(targetSpacer, Priority.ALWAYS);

        HBox targetRowLayout = new HBox(10);
        targetRowLayout.getChildren().addAll(targetLabel, targetField, targetSpacer, browseTargetBtn);

        // ROW 3
        Label prefixLabel = new Label("File Prefix Name: ");
        prefixLabel.setPrefWidth(100);

        TextField prefixField = new TextField();
        prefixField.setText(MediaBatchProcessor.DEFAULT_IMAGE_PREFIX);
        prefixField.setPromptText("Example: Holiday_Trip_");
        prefixField.setPrefWidth(300);
        prefixField.setMaxWidth(300);

        Region prefixSpacer = new Region();
        HBox.setHgrow(prefixSpacer, Priority.ALWAYS);

        HBox prefixRowLayout = new HBox(10);
        prefixRowLayout.getChildren().addAll(prefixLabel, prefixField, prefixSpacer);

        // Master Border Frame Box
        VBox optionsGroup = new VBox(12);
        BorderStroke stroke = new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1));
        optionsGroup.setPadding(new Insets(10));
        optionsGroup.setBorder(new Border(stroke));
        optionsGroup.getChildren().addAll(sourceRowLayout, targetRowLayout, prefixRowLayout);

        // Mount onto the grid layout frame and isolate growth to just this widget cell
        pane.add(optionsGroup, 0, 0);
        GridPane.setHgrow(optionsGroup, Priority.ALWAYS);
    }

    private void addMiddlePane(GridPane pane)
    {
        CheckBox embedDateTimeCheck = new CheckBox("Embed Date/Time Metadata");
        CheckBox forceDateChangeCheck = new CheckBox("Force Date Change on Edit");

        BorderStroke stroke = new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1));

        VBox optionsGroup = new VBox(10);
        optionsGroup.setPadding(new Insets(10));
        optionsGroup.setBorder(new Border(stroke));
        optionsGroup.getChildren().addAll(embedDateTimeCheck, forceDateChangeCheck);

        GridPane.setHgrow(optionsGroup, Priority.ALWAYS);

        pane.add(optionsGroup, 0, 1);
    }

    public void start2(Stage stage)
    {
        this.stage = stage;
        stage.setTitle("Media Metadata Automation Studio");

        // --- 1. Form Layout Area (GridPane for clean label/field layout) ---
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(12);

        // Source Directory Selector
        Label sourceLabel = new Label("Source Directory:");
        sourceField = new TextField();
        sourceField.setPromptText("Directory containing original media...");
        sourceField.setPrefWidth(300);
        Button browseSourceBtn = new Button("Browse...");
        browseSourceBtn.setOnAction(new BrowseSourceHandler());
        formGrid.add(sourceLabel, 0, 0);
        formGrid.add(sourceField, 1, 0);
        formGrid.add(browseSourceBtn, 2, 0);

        // Target Directory Selector
        Label targetLabel = new Label("Target Directory:");
        targetField = new TextField();
        targetField.setText(MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY);
        Button browseTargetBtn = new Button("Browse...");
        browseTargetBtn.setOnAction(new BrowseTargetHandler());
        formGrid.add(targetLabel, 0, 1);
        formGrid.add(targetField, 1, 1);
        formGrid.add(browseTargetBtn, 2, 1);

        // User Custom Prefix Text Input
        Label prefixLabel = new Label("File Name Prefix (-p):");
        prefixField = new TextField();
        prefixField.setPromptText("e.g. Summer_Trip_");
        formGrid.add(prefixLabel, 0, 2);
        formGrid.add(prefixField, 1, 2);

        // Custom Date String Entry
        Label dateLabel = new Label("Modify Date Taken (-m):");
        userDateField = new TextField();
        userDateField.setPromptText("yyyy-MM-dd HH:mm:ss");
        formGrid.add(dateLabel, 0, 3);
        formGrid.add(userDateField, 1, 3);

        // --- 2. Option Checkboxes (Arranged in simple horizontal layout rows) ---
        HBox optionsRow1 = new HBox(20);
        embedDateTimeCheck = new CheckBox("Embed Date/Time (-e)");
        forceDateChangeCheck = new CheckBox("Force Date Change (-f)");
        skipVideoCheck = new CheckBox("Skip Video Files (-S)");
        optionsRow1.getChildren().addAll(embedDateTimeCheck, forceDateChangeCheck, skipVideoCheck);

        HBox optionsRow2 = new HBox(20);
        showMetadataCheck = new CheckBox("Display Exif Metadata (-X)");
        descendingCheck = new CheckBox("Sort Descending (--desc)");
        debugCheck = new CheckBox("Enable Debugging (-d)");
        optionsRow2.getChildren().addAll(showMetadataCheck, descendingCheck, debugCheck);

        // --- 3. Run Controls Execution Row ---
        HBox controlRow = new HBox();
        controlRow.setAlignment(Pos.CENTER_RIGHT);
        Button runButton = new Button("Execute Batch Operation");
        runButton.setPrefWidth(200);
        runButton.setOnAction(new ExecutionHandler());
        controlRow.getChildren().add(runButton);

        // --- 4. Logging Panel (Replaces standard System.out terminal console) ---
        Label logLabel = new Label("Execution Logs & Output Console:");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(250);
        logArea.setPromptText("System output will be redirected here when execution begins...");

        // Redirect System.out streams directly inside our JavaFX UI TextArea component
        System.setOut(new PrintStream(new TextAreaOutputStream(logArea)));
        System.setErr(new PrintStream(new TextAreaOutputStream(logArea)));

        // --- 5. Main Root Arrangement Assembly ---
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.getChildren().addAll(
                formGrid,
                optionsRow1,
                optionsRow2,
                controlRow,
                logLabel,
                logArea);

        Scene scene = new Scene(mainLayout, 640, 650);
        stage.setScene(scene);
        stage.show();
    }

    // --- NON-LAMBDA ACTION EVENT HANDLERS ---

    private class BrowseSourceHandler implements EventHandler<ActionEvent>
    {
        @Override
        public void handle(ActionEvent event)
        {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Media Source Folder");

            File selection = chooser.showDialog(stage);

            if (selection != null)
            {
                sourceField.setText(selection.getAbsolutePath());
            }
        }
    }

    private class BrowseTargetHandler implements EventHandler<ActionEvent>
    {
        @Override
        public void handle(ActionEvent event)
        {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Destination Output Folder");

            File selection = chooser.showDialog(stage);

            if (selection != null)
            {
                targetField.setText(selection.getAbsolutePath());
            }
        }
    }

    private class ExecutionHandler implements EventHandler<ActionEvent>
    {
        @Override
        public void handle(ActionEvent event)
        {
            logArea.clear();

            String srcDir = sourceField.getText().trim();
            if (srcDir.isEmpty())
            {
                System.err.println("Execution Error: You must select a valid Source Directory before processing.");
                return;
            }

            System.out.println("Initializing operational configuration architecture...");

            // Plugs visual widget properties straight into your pristine BatchBuilder system
            BatchBuilder builder = new BatchBuilder()
                    .source(srcDir)
                    .prefix(prefixField.getText().trim().isEmpty() ? null : prefixField.getText().trim())
                    .target(targetField.getText().trim().isEmpty() ? null : targetField.getText().trim())
                    .embedDateTime(embedDateTimeCheck.isSelected())
                    .userDate(userDateField.getText().trim().isEmpty() ? null : userDateField.getText().trim())
                    .skipVideo(skipVideoCheck.isSelected())
                    .showMetadata(showMetadataCheck.isSelected())
                    .descending(descendingCheck.isSelected())
                    .forceDateChange(forceDateChangeCheck.isSelected())
                    .debug(debugCheck.isSelected());

            try
            {
                // Instantiates and passes execution directly to your original business engine logic
                // safely
                MediaMetadataConsole consoleEngine = builder.build();
                consoleEngine.run();
            }
            catch (BatchErrorException exc)
            {
                System.err.println("Fatal Batch Operation Process Interruption: " + exc.getMessage());
            }
        }
    }

    /**
     * Helper stream utility helper class targeting background buffer appending adjustments.
     */
    private static class TextAreaOutputStream extends java.io.OutputStream
    {
        private final TextArea destination;

        public TextAreaOutputStream(TextArea destination)
        {
            this.destination = destination;
        }

        @Override
        public void write(int value)
        {
            // Redirect character byte adjustments cleanly inside UI platform event stream update
            // safety paths
            javafx.application.Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    destination.appendText(String.valueOf((char) value));
                }
            });
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
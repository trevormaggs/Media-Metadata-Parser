package gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExtractedMetadataDialog extends Stage
{
    private final TextArea flatTextArea;
    private final StackPane containerStack;
    private final MetadataInspectorPane treeInspectorPane;

    public ExtractedMetadataDialog(Stage owner)
    {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Extracted Media Metadata");

        // View 1: Structured TreeTableView
        treeInspectorPane = new MetadataInspectorPane();

        // View 2: Flat Text Display
        flatTextArea = new TextArea();
        flatTextArea.setStyle("-fx-font-family: monospace;");
        flatTextArea.setEditable(false);
        
        // StackPane container
        containerStack = new StackPane(treeInspectorPane, flatTextArea);

        // Toggle View Group (Radio Buttons for Flat vs Tree)
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton rbFlat = new RadioButton("Raw Flat Text");
        RadioButton rbTree = new RadioButton("Structured Tree");

        rbFlat.setToggleGroup(toggleGroup);
        rbTree.setToggleGroup(toggleGroup);
        rbTree.setSelected(true);

        // Single Dynamic Expansion Toggle Button
        boolean[] isExpanded = new boolean[]{true};
        Button btnToggleExpand = new Button("Collapse All");

        btnToggleExpand.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (isExpanded[0])
                {
                    treeInspectorPane.setAllNodesExpanded(false);
                    btnToggleExpand.setText("Expand All");
                    isExpanded[0] = false;
                }

                else
                {
                    treeInspectorPane.setAllNodesExpanded(true);
                    btnToggleExpand.setText("Collapse All");
                    isExpanded[0] = true;
                }
            }
        });

        // Initial Visibility
        flatTextArea.setVisible(false);
        treeInspectorPane.setVisible(true);

        // View Switch Listener (hides/shows the toggle button based on active view)
        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue)
            {
                boolean showTree = (newValue == rbTree);
                
                treeInspectorPane.setVisible(showTree);
                flatTextArea.setVisible(!showTree);
                btnToggleExpand.setVisible(showTree);
            }
        });

        // Top Toolbar Assembly
        HBox toolbarPane = new HBox(12, new Label("View Mode:"), rbTree, rbFlat, new Region(), btnToggleExpand);
        
        toolbarPane.setAlignment(Pos.CENTER_LEFT);
        toolbarPane.setPadding(new Insets(5, 10, 5, 10));

        // Push button to the right
        HBox.setHgrow(toolbarPane.getChildren().get(3), Priority.ALWAYS);

        // Dialog Action Buttons
        Button btnCopy = new Button("Copy to Clipboard");
        
        btnCopy.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                
                content.putString(flatTextArea.getText());
                clipboard.setContent(content);
            }
        });

        Button btnExport = new Button("Export to File");
        
        btnExport.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                exportToFile();
            }
        });

        Button btnClose = new Button("Close");
        
        btnClose.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                close();
            }
        });

        HBox actionPane = new HBox(10, btnCopy, btnExport, new Region(), btnClose);
        HBox.setHgrow(actionPane.getChildren().get(2), Priority.ALWAYS);
        
        actionPane.setPadding(new Insets(10, 0, 0, 0));

        VBox rootLayout = new VBox(10, toolbarPane, containerStack, actionPane);
        rootLayout.setPadding(new Insets(10));
        
        VBox.setVgrow(containerStack, Priority.ALWAYS);
        setScene(new Scene(rootLayout, 800, 550));
    }

    public void setMetadataText(String rawOutput)
    {
        flatTextArea.setText(rawOutput);
        treeInspectorPane.populateFromRawOutput(rawOutput);
    }

    private void exportToFile()
    {
        FileChooser chooser = new FileChooser();
        File file = chooser.showSaveDialog(this);
        
        chooser.setTitle("Save Metadata File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));

        if (file != null)
        {
            try (FileWriter writer = new FileWriter(file))
            {
                writer.write(flatTextArea.getText());
            }
            
            catch (IOException exc)
            {
                System.err.println("Failed to export metadata: " + exc.getMessage());
            }
        }
    }
}
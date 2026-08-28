package gui;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class MetadataInspectorPane extends VBox
{
    private final TextField searchField;
    private final TreeTableView<MetadataNode> treeTableView;
    private TreeItem<MetadataNode> rootNode;

    public MetadataInspectorPane()
    {
        this.searchField = new TextField();
        this.searchField.setPromptText("Filter metadata by tag or value (e.g., ISO, Exposure, Date)...");

        this.treeTableView = new TreeTableView<>();
        this.treeTableView.setShowRoot(false);

        // Auto-fit columns to fill 100% of horizontal space (eliminates empty trailing column area)
        this.treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        // Styling rows to visually distinguish File headers from Group headers and regular tags
        this.treeTableView.setRowFactory(new Callback<TreeTableView<MetadataNode>, TreeTableRow<MetadataNode>>()
        {
            @Override
            public TreeTableRow<MetadataNode> call(TreeTableView<MetadataNode> param)
            {
                return new TreeTableRow<MetadataNode>()
                {
                    @Override
                    protected void updateItem(MetadataNode item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        if (empty || item == null)
                        {
                            setStyle("");
                        }
                        
                        else if ("File Node".equals(item.getGroup()))
                        {
                            setStyle("-fx-font-weight: bold; -fx-background-color: #e6f2ff; -fx-font-size: 13px;");
                        }
                        
                        else if (item.getName().startsWith("["))
                        {
                            setStyle("-fx-font-weight: bold; -fx-font-style: italic; -fx-background-color: #f9f9f9;");
                        }
                        
                        else
                        {
                            setStyle("");
                        }
                    }
                };
            }
        });

        // Column 1: Multi-level Tree Column (File -> Group -> Tag Name)
        TreeTableColumn<MetadataNode, String> nameCol = new TreeTableColumn<>("File / Tag Name");
        
        nameCol.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<MetadataNode, String> param)
            {
                return param.getValue().getValue().nameProperty();
            }
        });

        // Column 2: Value
        TreeTableColumn<MetadataNode, String> valueCol = new TreeTableColumn<>("Value");
        
        valueCol.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<MetadataNode, String> param)
            {
                return param.getValue().getValue().valueProperty();
            }
        });

        // Column 3: Group Category
        TreeTableColumn<MetadataNode, String> groupCol = new TreeTableColumn<>("Group Category");
        
        groupCol.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<MetadataNode, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<MetadataNode, String> param)
            {
                return param.getValue().getValue().groupProperty();
            }
        });

        treeTableView.getColumns().add(nameCol);
        treeTableView.getColumns().add(valueCol);
        treeTableView.getColumns().add(groupCol);

        VBox.setVgrow(treeTableView, Priority.ALWAYS);
        this.getChildren().addAll(searchField, treeTableView);

        setupSearchFilter();
    }

    public void populateFromRawOutput(String rawOutput)
    {
        rootNode = new TreeItem<>(new MetadataNode("Root"));

        TreeItem<MetadataNode> currentFileNode = null;
        Map<String, TreeItem<MetadataNode>> groupMap = null;

        String[] lines = rawOutput.split("\\r?\\n");
        for (String line : lines)
        {
            String trimmed = line.trim();

            if (trimmed.isEmpty())
            {
                continue;
            }

            // 1. Detect Header Line for individual files (e.g. "========
            // E:\ImageBatchDir\babygemma.tif ========")
            if (trimmed.startsWith("========") && trimmed.endsWith("========"))
            {
                String filePath = trimmed.replace("=", "").trim();

                // Extract filename from path for clear node label
                String fileName = filePath;
                int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
                if (lastSep >= 0 && lastSep < filePath.length() - 1)
                {
                    fileName = filePath.substring(lastSep + 1);
                }

                // Create Top-Level File Node
                currentFileNode = new TreeItem<>(new MetadataNode(fileName, filePath, "File Node"));
                currentFileNode.setExpanded(true);
                rootNode.getChildren().add(currentFileNode);

                // Reset group tracking for the new file
                groupMap = new HashMap<>();
                continue;
            }

            // 2. Fallback if metadata output starts without a header banner line
            if (currentFileNode == null)
            {
                currentFileNode = new TreeItem<>(new MetadataNode("Default File Entry", "", "File Node"));
                currentFileNode.setExpanded(true);
                rootNode.getChildren().add(currentFileNode);
                groupMap = new HashMap<>();
            }

            // 3. Parse Metadata Key-Value Tag Rows (e.g. "[System] Directory : E:\ImageBatchDir")
            int groupEnd = line.indexOf(']');
            int colonIdx = line.indexOf(':');

            if (line.startsWith("[") && groupEnd > 0 && colonIdx > groupEnd)
            {
                String groupName = line.substring(1, groupEnd).trim();
                String tagName = line.substring(groupEnd + 1, colonIdx).trim();
                String tagValue = line.substring(colonIdx + 1).trim();

                // Find or create category folder node under the current file
                TreeItem<MetadataNode> groupItem = groupMap.get(groupName);
                
                if (groupItem == null)
                {
                    groupItem = new TreeItem<>(new MetadataNode("[" + groupName + "]"));
                    groupItem.setExpanded(true);
                    currentFileNode.getChildren().add(groupItem);
                    groupMap.put(groupName, groupItem);
                }

                // Add key-value child node under its respective category group
                groupItem.getChildren().add(new TreeItem<>(new MetadataNode(tagName, tagValue, groupName)));
            }
        }

        treeTableView.setRoot(rootNode);
    }

    private void setupSearchFilter()
    {
        searchField.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                if (rootNode != null)
                {
                    filterTree(rootNode, newValue.toLowerCase().trim());
                }
            }
        });
    }

    private boolean filterTree(TreeItem<MetadataNode> item, String filter)
    {
        if (filter.isEmpty())
        {
            item.setExpanded(true);
            
            for (TreeItem<MetadataNode> child : item.getChildren())
            {
                filterTree(child, filter);
            }
            
            return true;
        }

        boolean match = false;
        MetadataNode data = item.getValue();

        if (data != null && (data.nameProperty().get().toLowerCase().contains(filter) || data.valueProperty().get().toLowerCase().contains(filter)))
        {
            match = true;
        }

        boolean childMatch = false;
        
        for (TreeItem<MetadataNode> child : item.getChildren())
        {
            if (filterTree(child, filter))
            {
                childMatch = true;
            }
        }

        boolean visible = match || childMatch;
        
        if (visible)
        {
            item.setExpanded(true);
        }
        
        return visible;
    }

    /**
     * Recursively expands or collapses all nodes within the TreeTableView.
     *
     * @param expanded
     *        true to expand all nodes; false to collapse all.
     */
    public void setAllNodesExpanded(boolean expanded)
    {
        if (rootNode != null)
        {
            setNodeExpandedRecursive(rootNode, expanded);
        }
    }

    private void setNodeExpandedRecursive(TreeItem<MetadataNode> item, boolean expanded)
    {
        if (item != null)
        {
            // Keep root node children operations clean while toggling
            if (item != rootNode)
            {
                item.setExpanded(expanded);
            }

            for (TreeItem<MetadataNode> child : item.getChildren())
            {
                setNodeExpandedRecursive(child, expanded);
            }
        }
    }
}
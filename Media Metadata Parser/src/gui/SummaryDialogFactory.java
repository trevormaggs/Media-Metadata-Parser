package gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;

/**
 * Factory class responsible for constructing and displaying the batch processing summary dialog.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 16 September 2026
 */
final class SummaryDialogFactory
{
    private SummaryDialogFactory()
    {
        // Private constructor to prevent instantiation. This is a utility class.
    }

    /**
     * Constructs and shows the batch processing summary dialog.
     *
     * @param owner
     *        the parent window owning this dialog
     * @param targetText
     *        the target directory text field to resolve relative file paths
     * @param completedFileRecords
     *        the list of records to populate inside the summary table
     */
    static void show(Window owner, TextField targetText, ObservableList<ProcessedFileRecord> completedFileRecords)
    {
        Path resolvedTargetDir = null;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch Processing Summary");
        dialog.setHeaderText("Detailed Processing Results");
        dialog.initModality(Modality.NONE);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().addAll(owner.getScene().getStylesheets());
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        // Fixed-width Row Index Column (#)
        TableColumn<ProcessedFileRecord, Void> indexCol = new TableColumn<>("#");
        indexCol.setMinWidth(35);
        indexCol.setMaxWidth(35);
        indexCol.setPrefWidth(35);
        indexCol.setResizable(false);
        indexCol.setStyle("-fx-alignment: CENTER;");

        indexCol.setCellFactory(new Callback<TableColumn<ProcessedFileRecord, Void>, TableCell<ProcessedFileRecord, Void>>()
        {
            @Override
            public TableCell<ProcessedFileRecord, Void> call(TableColumn<ProcessedFileRecord, Void> param)
            {
                return new TableCell<ProcessedFileRecord, Void>()
                {
                    @Override
                    protected void updateItem(Void item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        if (empty || getTableRow() == null || getTableRow().getItem() == null)
                        {
                            setText(null);
                        }

                        else
                        {
                            setText(String.valueOf(getIndex() + 1));
                        }
                    }
                };
            }
        });

        // Dynamic Source Filename Column
        TableColumn<ProcessedFileRecord, String> sourceCol = new TableColumn<>("Source File");
        sourceCol.setMinWidth(130);
        sourceCol.setPrefWidth(180);
        sourceCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ProcessedFileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ProcessedFileRecord, String> cellData)
            {
                return cellData.getValue().sourceNameProperty();
            }
        });

        // Dynamic Target Filename Column
        TableColumn<ProcessedFileRecord, String> targetCol = new TableColumn<>("Target File");
        targetCol.setMinWidth(130);
        targetCol.setPrefWidth(180);
        targetCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ProcessedFileRecord, String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<ProcessedFileRecord, String> cellData)
            {
                return cellData.getValue().targetNameProperty();
            }
        });

        // Fixed-width File Size Column
        TableColumn<ProcessedFileRecord, Long> sizeCol = new TableColumn<>("File Size");
        sizeCol.setMinWidth(90);
        sizeCol.setMaxWidth(90);
        sizeCol.setPrefWidth(90);
        sizeCol.setResizable(false);
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        sizeCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ProcessedFileRecord, Long>, ObservableValue<Long>>()
        {
            @Override
            public ObservableValue<Long> call(TableColumn.CellDataFeatures<ProcessedFileRecord, Long> cellData)
            {
                return new ReadOnlyObjectWrapper<>(cellData.getValue().getFileSize());
            }
        });

        sizeCol.setCellFactory(new Callback<TableColumn<ProcessedFileRecord, Long>, TableCell<ProcessedFileRecord, Long>>()
        {
            @Override
            public TableCell<ProcessedFileRecord, Long> call(TableColumn<ProcessedFileRecord, Long> param)
            {
                return new TableCell<ProcessedFileRecord, Long>()
                {
                    @Override
                    protected void updateItem(Long item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        setText(UtilsJavaFX.formatFileSize(item));
                    }
                };
            }
        });

        if (!targetText.getText().trim().isEmpty())
        {
            try
            {
                resolvedTargetDir = Paths.get(targetText.getText().trim()).toAbsolutePath();
            }

            catch (InvalidPathException exc)
            {
                // Fall back to null if target path string cannot be parsed
            }
        }

        final Path targetDir = resolvedTargetDir;
        final HoverDebouncer debouncer = new HoverDebouncer(120);
        final ImagePreviewPopup thumbnail = new ImagePreviewPopup(dialogPane.getScene().getWindow(), targetDir);

        final TableView<ProcessedFileRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Enable multi-row selection mode
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        table.getColumns().add(indexCol);
        table.getColumns().add(sourceCol);
        table.getColumns().add(targetCol);
        table.getColumns().add(sizeCol);
        table.setItems(completedFileRecords);

        // Row factory handling hover preview popups, double-click open, and context menu
        table.setRowFactory(new Callback<TableView<ProcessedFileRecord>, TableRow<ProcessedFileRecord>>()
        {
            @Override
            public TableRow<ProcessedFileRecord> call(TableView<ProcessedFileRecord> param)
            {
                final ContextMenu contextMenu = new ContextMenu();
                final TableRow<ProcessedFileRecord> row = new TableRow<>();

                MenuItem openFolderItem = new MenuItem("Open Target Location");
                MenuItem copyPathItem = new MenuItem("Copy Target Path(s)");
                MenuItem copyNameItem = new MenuItem("Copy Target Name(s)");
                MenuItem copyCsvItem = new MenuItem("Copy Selected (CSV)");

                openFolderItem.setOnAction(new EventHandler<ActionEvent>()
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        ProcessedFileRecord record = row.getItem();

                        if (record != null && targetDir != null)
                        {
                            File file = targetDir.resolve(record.getTargetName()).toFile();
                            File folderToOpen = file.exists() ? file.getParentFile() : targetDir.toFile();

                            try
                            {
                                if (Desktop.isDesktopSupported() && folderToOpen.exists())
                                {
                                    Desktop.getDesktop().open(folderToOpen);
                                }
                            }

                            catch (IOException exc)
                            {
                                UtilsJavaFX.launchPopup(dialogPane.getScene().getWindow(), "File Error", "Unable to open directory location:\n" + exc.getMessage(), AlertType.ERROR);
                            }
                        }
                    }
                });

                copyPathItem.setOnAction(new EventHandler<ActionEvent>()
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        ObservableList<ProcessedFileRecord> selected = table.getSelectionModel().getSelectedItems();

                        if (selected != null && !selected.isEmpty())
                        {
                            StringBuilder sb = new StringBuilder();

                            for (ProcessedFileRecord record : selected)
                            {
                                if (record != null)
                                {
                                    String fullPath = (targetDir != null ? targetDir.resolve(record.getTargetName()).toString() : record.getTargetName());
                                    sb.append(fullPath).append(System.lineSeparator());
                                }
                            }

                            copyToClipboard(sb.toString().trim());
                        }
                    }
                });

                copyNameItem.setOnAction(new EventHandler<ActionEvent>()
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        ObservableList<ProcessedFileRecord> selected = table.getSelectionModel().getSelectedItems();

                        if (selected != null && !selected.isEmpty())
                        {
                            StringBuilder sb = new StringBuilder();

                            for (ProcessedFileRecord record : selected)
                            {
                                if (record != null)
                                {
                                    sb.append(record.getTargetName()).append(System.lineSeparator());
                                }
                            }

                            copyToClipboard(sb.toString().trim());
                        }
                    }
                });

                copyCsvItem.setOnAction(new EventHandler<ActionEvent>()
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        copySelectedRowsToCsv(table);
                    }
                });

                copyCsvItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN));

                contextMenu.getItems().addAll(openFolderItem, copyPathItem, copyNameItem, copyCsvItem);

                row.emptyProperty().addListener(new ChangeListener<Boolean>()
                {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> obs, Boolean wasEmpty, Boolean isEmpty)
                    {
                        if (isEmpty)
                        {
                            row.setContextMenu(null);
                        }

                        else
                        {
                            row.setContextMenu(contextMenu);
                        }
                    }
                });

                // Double-click row shortcut to open output file
                row.setOnMouseClicked(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        if (event.getClickCount() == 2 && !row.isEmpty())
                        {
                            ProcessedFileRecord record = row.getItem();

                            if (record != null && targetDir != null)
                            {
                                File file = targetDir.resolve(record.getTargetName()).toFile();

                                try
                                {
                                    if (Desktop.isDesktopSupported() && file.exists())
                                    {
                                        Desktop.getDesktop().open(file);
                                    }

                                    else if (Desktop.isDesktopSupported() && file.getParentFile() != null && file.getParentFile().exists())
                                    {
                                        Desktop.getDesktop().open(file.getParentFile());
                                    }
                                }

                                catch (IOException exc)
                                {
                                    UtilsJavaFX.launchPopup(dialogPane.getScene().getWindow(), "File Error", "Unable to open target file:\n" + exc.getMessage(), AlertType.ERROR);
                                }
                            }
                        }
                    }
                });

                row.setOnMouseEntered(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        if (!row.isEmpty())
                        {
                            final ProcessedFileRecord record = row.getItem();
                            final double screenX = event.getScreenX();
                            final double screenY = event.getScreenY();

                            debouncer.request(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    thumbnail.showPreview(record, screenX, screenY);
                                }
                            });
                        }
                    }
                });

                row.setOnMouseExited(new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent event)
                    {
                        debouncer.cancel();
                        thumbnail.hide();
                    }
                });

                return row;
            }
        });

        // Cancel and hide if cursor leaves the entire table area
        table.setOnMouseExited(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                debouncer.cancel();
                thumbnail.hide();
            }
        });

        // Keyboard arrow navigation event handling with debouncing
        table.setOnKeyReleased(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN)
                {
                    ProcessedFileRecord selectedRecord = table.getSelectionModel().getSelectedItem();

                    if (selectedRecord != null)
                    {
                        Point2D point = table.localToScreen(0, 0);

                        if (point != null)
                        {
                            double screenX = point.getX() + table.getWidth() / 2;
                            double screenY = point.getY() + 50;

                            debouncer.request(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    thumbnail.showPreview(selectedRecord, screenX, screenY);
                                }
                            });

                        }
                    }
                }

                else if (event.getCode() == KeyCode.ESCAPE)
                {
                    debouncer.cancel();
                    thumbnail.hide();
                }
            }
        });

        // SHORTCUT_DOWN handles Ctrl on Windows/Linux and Cmd on macOS
        table.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                if (new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN).match(event))
                {
                    copySelectedRowsToCsv(table);
                    event.consume();
                }
            }
        });

        // Ensure newly appending rows pull scrolling view downward automatically
        completedFileRecords.addListener(new ListChangeListener<ProcessedFileRecord>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends ProcessedFileRecord> change)
            {
                while (change.next())
                {
                    if (change.wasAdded() && !completedFileRecords.isEmpty())
                    {
                        table.scrollTo(completedFileRecords.size() - 1);
                    }
                }
            }
        });

        // Shut down background thread executor and clear thumbnail memory references
        dialog.setOnCloseRequest(new EventHandler<DialogEvent>()
        {
            @Override
            public void handle(DialogEvent event)
            {
                debouncer.cancel();
                thumbnail.dispose();
            }
        });

        dialogPane.setContent(table);
        dialogPane.setPrefSize(570, 320);
        dialog.show();

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                table.applyCss();
                table.requestLayout();
            }
        });
    }

    /**
     * Helper utility for copying string text to the system clipboard.
     * 
     * @param text
     *        the text string to place on clipboard
     */
    private static void copyToClipboard(String text)
    {
        if (text != null && !text.trim().isEmpty())
        {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    /**
     * Helper method to format selected table rows as CSV and copy to clipboard.
     * 
     * @param table
     *        the target TableView instance
     */
    private static void copySelectedRowsToCsv(TableView<ProcessedFileRecord> table)
    {
        ObservableList<ProcessedFileRecord> selected = table.getSelectionModel().getSelectedItems();

        if (selected != null && !selected.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Source File,Target File,File Size").append(System.lineSeparator());

            for (ProcessedFileRecord record : selected)
            {
                if (record != null)
                {
                    sb.append("\"").append(record.getSourceName()).append("\",")
                            .append("\"").append(record.getTargetName()).append("\",")
                            .append("\"").append(UtilsJavaFX.formatFileSize(record.getFileSize())).append("\"")
                            .append(System.lineSeparator());
                }
            }

            copyToClipboard(sb.toString().trim());
        }
    }
}
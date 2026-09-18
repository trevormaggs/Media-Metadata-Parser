package gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import common.Utils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
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
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;

/**
 * Factory class responsible for constructing and displaying the batch processing summary dialog.
 * 
 * <p>
 * Provides interactive features such as dynamic file size formatting, contextual menus,
 * double-click file opening, multi-selection row dragging, CSV clipboard copying, and debounced
 * hover image previews.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 17 September 2026
 */
final class SummaryDialogFactory
{
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SummaryDialogFactory()
    {
        // Private constructor to prevent instantiation.
    }

    /**
     * Constructs and displays the non-modal batch processing summary dialog.
     *
     * @param owner
     *        the parent {@link Window} owning this dialog instance
     * @param targetText
     *        the {@link TextField} containing the output target directory string
     * @param completedFileRecords
     *        the observable list of {@link ProcessedFileRecord} entries to populate in the summary
     *        table
     */
    static void show(Window owner, TextField targetText, ObservableList<ProcessedFileRecord> completedFileRecords)
    {
        Path targetDir = null;

        if (!Utils.isBlank(targetText.getText()))
        {
            try
            {
                targetDir = Paths.get(targetText.getText().trim()).toAbsolutePath();
            }

            catch (InvalidPathException exc)
            {
                // Fall back to null if target path string cannot be parsed
            }
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch Processing Summary");
        dialog.setHeaderText("Detailed Processing Results");
        dialog.initModality(Modality.NONE);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().addAll(owner.getScene().getStylesheets());
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        final HoverDebouncer debouncer = new HoverDebouncer(120);
        final TableView<ProcessedFileRecord> table = addSummaryTable();
        final ImagePreviewPopup thumbnail = new ImagePreviewPopup(dialog.getDialogPane().getScene().getWindow(), targetDir);

        table.setItems(completedFileRecords);
        attachListeners(table, targetDir, thumbnail, debouncer);

        // Ensure newly appending rows pull the scrolling view downward automatically
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
        
        // Cancel pending preview requests and release thumbnail resources upon dialog close
        dialog.setOnCloseRequest(new EventHandler<DialogEvent>()
        {
            @Override
            public void handle(DialogEvent event)
            {
                debouncer.cancel();
                thumbnail.dispose();
            }
        });

        dialogPane.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                debouncer.cancel();
                thumbnail.hide();
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
     * Prepares the {@link TableView} instance displaying the processing records.
     *
     * @return a fully configured {@link TableView} instance
     */
    private static TableView<ProcessedFileRecord> addSummaryTable()
    {
        TableView<ProcessedFileRecord> table = new TableView<>();
        TableColumn<ProcessedFileRecord, Void> indexCol = new TableColumn<>("#");
        TableColumn<ProcessedFileRecord, String> sourceCol = new TableColumn<>("Source File");
        TableColumn<ProcessedFileRecord, String> targetCol = new TableColumn<>("Target File");
        TableColumn<ProcessedFileRecord, Long> sizeCol = new TableColumn<>("File Size");

        // Fixed-width Row Index Column (#)
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

                        if (empty || item == null)
                        {
                            setText(null);
                        }

                        else
                        {
                            setText(UtilsJavaFX.formatFileSize(item));
                        }
                    }
                };
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getColumns().add(indexCol);
        table.getColumns().add(sourceCol);
        table.getColumns().add(targetCol);
        table.getColumns().add(sizeCol);

        return table;
    }

    /**
     * Attaches row factories, mouse, keyboard, context menu, and hover preview listeners to the
     * summary table.
     *
     * @param table
     *        the target {@link TableView} instance
     * @param targetDir
     *        the resolved base target directory path
     * @param thumbnail
     *        the {@link ImagePreviewPopup} handler for hovering over image files
     * @param debouncer
     *        the {@link HoverDebouncer} for pacing image preview tasks
     */
    private static void attachListeners(TableView<ProcessedFileRecord> table, Path targetDir, ImagePreviewPopup thumbnail, HoverDebouncer debouncer)
    {
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
                                UtilsJavaFX.launchPopup(row.getScene().getWindow(), "File Error", "Unable to open directory location:\n" + exc.getMessage(), AlertType.ERROR);
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

                        if (!selected.isEmpty())
                        {
                            StringBuilder sb = new StringBuilder();

                            for (ProcessedFileRecord record : selected)
                            {
                                String fullPath = (targetDir != null ? targetDir.resolve(record.getTargetName()).toString() : record.getTargetName());
                                sb.append(fullPath).append(System.lineSeparator());
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

                        if (!selected.isEmpty())
                        {
                            StringBuilder sb = new StringBuilder();

                            for (ProcessedFileRecord record : selected)
                            {
                                sb.append(record.getTargetName()).append(System.lineSeparator());
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

                copyCsvItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
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
                                    UtilsJavaFX.launchPopup(row.getScene().getWindow(), "File Error", "Unable to open target file:\n" + exc.getMessage(), AlertType.ERROR);
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

                            debouncer.request(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Platform.runLater(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Window window = row.getScene().getWindow();
                                            double previewX = window.getX() + window.getWidth() + 10;
                                            double previewY = window.getY();

                                            thumbnail.showPreview(record, previewX, previewY);
                                        }
                                    });
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
                    final ProcessedFileRecord selectedRecord = table.getSelectionModel().getSelectedItem();

                    if (selectedRecord != null)
                    {
                        debouncer.request(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Platform.runLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        Window window = table.getScene().getWindow();
                                        double screenX = window.getX() + window.getWidth() + 10;
                                        double screenY = window.getY();

                                        thumbnail.showPreview(selectedRecord, screenX, screenY);
                                    }
                                });
                            }
                        });
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
                if (new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN).match(event))
                {
                    copySelectedRowsToCsv(table);
                    event.consume();
                }
            }
        });

        // Enable standard mouse drag multi-row selection
        table.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                if (event.isPrimaryButtonDown() && !event.isShiftDown() && !event.isShortcutDown())
                {
                    // Clear prior selections on a fresh click unless modifier keys are held
                    table.getSelectionModel().clearSelection();
                }
            }
        });

        table.setOnMouseDragged(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                if (event.isPrimaryButtonDown())
                {
                    // Pick row under the current cursor position
                    Node node = event.getPickResult().getIntersectedNode();

                    while (node != null && !(node instanceof TableRow))
                    {
                        node = node.getParent();
                    }

                    if (node instanceof TableRow)
                    {
                        TableRow<?> row = (TableRow<?>) node;

                        if (!row.isEmpty())
                        {
                            table.getSelectionModel().select(row.getIndex());
                        }
                    }
                }
            }
        });
    }

    /**
     * Helper utility for placing formatted string text directly onto the system clipboard.
     *
     * @param text
     *        the text string to copy to the system clipboard
     */
    private static void copyToClipboard(String text)
    {
        if (!Utils.isBlank(text))
        {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);

            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    /**
     * Helper utility to format selected table rows into CSV format and copy them to the system
     * clipboard.
     *
     * @param table
     *        the target {@link TableView} instance containing selected records
     */
    private static void copySelectedRowsToCsv(TableView<ProcessedFileRecord> table)
    {
        ObservableList<ProcessedFileRecord> selected = table.getSelectionModel().getSelectedItems();

        if (!selected.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Source File,Target File,File Size").append(System.lineSeparator());

            for (ProcessedFileRecord record : selected)
            {
                sb.append(Utils.csvEscape(record.getSourceName())).append(',')
                        .append(Utils.csvEscape(record.getTargetName())).append(',')
                        .append(Utils.csvEscape(UtilsJavaFX.formatFileSize(record.getFileSize())))
                        .append(System.lineSeparator());
            }

            copyToClipboard(sb.toString().trim());
        }
    }
}
package gui;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;

/**
 * Factory class responsible for constructing and displaying the batch processing summary dialog.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 7 September 2026
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
        Path targetDir = null;

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
        sourceCol.setMinWidth(150);
        sourceCol.setPrefWidth(210);
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
        targetCol.setMinWidth(150);
        targetCol.setPrefWidth(210);
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

                        if (empty || item == null)
                        {
                            setText(null);
                        }

                        else if (item <= 0)
                        {
                            setText("0 B");
                        }

                        else
                        {
                            String[] units = {"B", "KB", "MB", "GB", "TB"};
                            int digitGroups = (int) (Math.log10(item) / Math.log10(1024));

                            digitGroups = Math.min(digitGroups, units.length - 1);
                            setText(new DecimalFormat("#,##0.#").format(item / Math.pow(1024, digitGroups)) + " " + units[digitGroups]);
                        }
                    }
                };
            }
        });

        if (!targetText.getText().trim().isEmpty())
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

        final HoverDebouncer debouncer = new HoverDebouncer(120);
        final ImagePreviewPopup thumbnail = new ImagePreviewPopup(dialogPane.getScene().getWindow(), targetDir);

        TableView<ProcessedFileRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(indexCol);
        table.getColumns().add(sourceCol);
        table.getColumns().add(targetCol);
        table.getColumns().add(sizeCol);
        table.setItems(completedFileRecords);

        // Attach hover image thumbnail listeners on rows, debounced to reduce redundant loader tasks
        table.setRowFactory(new Callback<TableView<ProcessedFileRecord>, TableRow<ProcessedFileRecord>>()
        {
            @Override
            public TableRow<ProcessedFileRecord> call(TableView<ProcessedFileRecord> param)
            {
                final TableRow<ProcessedFileRecord> row = new TableRow<>();

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
    }
}
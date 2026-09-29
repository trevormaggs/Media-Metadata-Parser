package gui;

import javafx.beans.value.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.concurrent.Worker;

/**
 * Modal progress dialog displaying an overlaid percentage counter.
 */
final class TaskProgressDialog
{
    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException
     *         always thrown when an instance is created
     */
    private TaskProgressDialog()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    /**
     * Displays a modal progress dialog bound to the specified {@link BatchTask}.
     *
     * @param owner
     *        the parent window owning this modal dialog
     * @param title
     *        the window title for the dialog
     * @param task
     *        the task whose progress and status are displayed
     */
    static void show(Window owner, String title, BatchTask task)
    {
        final Stage dialog = new Stage();
        final Label percentLabel = new Label("0%");
        final Label statusLabel = new Label("");
        final Button abortButton = new Button("Abort");
        final ProgressBar progressBar = new ProgressBar();

        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setTitle(title);
        dialog.setResizable(false);

        if (owner instanceof Stage)
        {
            dialog.getIcons().addAll(((Stage) owner).getIcons());
        }
        
        // Bindings
        statusLabel.textProperty().bind(task.messageProperty());

        percentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        percentLabel.setBlendMode(BlendMode.DIFFERENCE);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(22);
        progressBar.progressProperty().bind(task.progressProperty());

        final ChangeListener<Number> progressChangeListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                if (newValue != null && newValue.doubleValue() >= 0.0 && newValue.doubleValue() <= 1.0)
                {
                    int percent = (int) Math.round(newValue.doubleValue() * 100);

                    percentLabel.setText(percent + "%");
                }

                else
                {
                    percentLabel.setText("");
                }
            }
        };

        task.progressProperty().addListener(progressChangeListener);

        final Runnable closeProgressTask = new Runnable()
        {
            @Override
            public void run()
            {
                task.progressProperty().removeListener(progressChangeListener);

                statusLabel.textProperty().unbind();
                progressBar.progressProperty().unbind();

                if (dialog.isShowing())
                {
                    dialog.close();
                }
            }
        };

        abortButton.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                task.cancel(true);
                closeProgressTask.run();
            }
        });

        dialog.setOnCloseRequest(new EventHandler<WindowEvent>()
        {
            @Override
            public void handle(WindowEvent event)
            {
                task.cancel(true);
                closeProgressTask.run();
            }
        });

        // Close dialog automatically when the task stops running.
        task.runningProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean wasRunning, Boolean isRunning)
            {
                if (wasRunning && !isRunning)
                {
                    closeProgressTask.run();
                }
            }
        });

        // Layout construction
        StackPane progressOverlayPane = new StackPane();
        progressOverlayPane.getChildren().addAll(progressBar, percentLabel);

        StackPane.setAlignment(percentLabel, Pos.CENTER);

        HBox buttonBox = new HBox(abortButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, statusLabel, progressOverlayPane, buttonBox);

        root.setPadding(new Insets(16));
        root.setPrefWidth(400);

        Scene dialogScene = new Scene(root);

        if (owner != null && owner.getScene() != null)
        {
            dialogScene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        dialog.setScene(dialogScene);

        dialog.show();

        if (task.getState() == Worker.State.READY)
        {
            Thread backgroundThread = new Thread(task, "BatchTask-Worker-Thread");

            backgroundThread.setDaemon(true);
            backgroundThread.start();
        }
    }
}
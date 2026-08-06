package gui;

import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.BatchStatistics;
import batch.DisplayMetadata;
import batch.MediaBatchProcessor;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import progressbar.JavaFXProgressAdapter;

/**
 * Executes batch media processing or metadata extraction on a background thread.
 *
 * <p>
 * This task coordinates long-running batch operations while reporting progress and completion
 * status to the supplied user interface components without blocking the JavaFX Application Thread.
 * </p>
 */
class BatchTask2 extends Task<BatchStatistics>
{
    private final BatchConfiguration config;
    private final TextArea logArea;
    private final ProgressBar progressBar;
    private final boolean displayMetadata;
    private volatile MediaBatchProcessor processor;

    /**
     * Constructs a background task for executing batch processing or metadata extraction.
     *
     * @param config
     *        the validated batch configuration
     * @param logArea
     *        the destination for status messages, or {@code null}
     * @param progressBar
     *        the progress bar to update during processing, or {@code null}
     * @param displayMetadata
     *        {@code true} to display metadata instead of processing files
     */
    BatchTask2(BatchConfiguration config, TextArea logArea, ProgressBar progressBar, boolean displayMetadata)
    {
        this.config = config;
        this.logArea = logArea;
        this.progressBar = progressBar;
        this.displayMetadata = displayMetadata;
    }

    /**
     * Returns the underlying batch processor instance.
     *
     * @return the {@link MediaBatchProcessor}, or {@code null} if it is not yet initialised
     */
    public MediaBatchProcessor getProcessor()
    {
        return processor;
    }

    /**
     * Cancels the active processing engine if currently running.
     */
    void cancelProcessor()
    {
        super.cancel();

        if (processor != null)
        {
            processor.cancel();
        }
    }

    @Override
    protected BatchStatistics call() throws Exception
    {
        if (displayMetadata)
        {
            updateMessage("Retrieving metadata...");
            DisplayMetadata display = new DisplayMetadata(config);
            display.execute();
        }

        else
        {
            processor = new MediaBatchProcessor(config);

            processor.addProgressListener(new JavaFXProgressAdapter(progressBar)
            {
                private boolean isScanning = true;

                @Override
                public void onProgressUpdate(int current)
                {
                    if (!isCancelled())
                    {
                        super.onProgressUpdate(current);

                        if (isScanning)
                        {
                            updateMessage(String.format("Scanning files (%d found)...", current));
                        }

                        else
                        {
                            updateMessage(String.format("Processing batch (%d files)...", current));
                        }
                    }
                }

                @Override
                public void onProgressUpdate(int current, int total)
                {
                    if (!isCancelled())
                    {
                        super.onProgressUpdate(current, total);

                        if (isScanning)
                        {
                            if (total > 0)
                            {
                                updateMessage(String.format("Scanning files: %d of %d", current, total));
                            }

                            else
                            {
                                updateMessage(String.format("Scanning files (%d)...", current));
                            }
                        }

                        else
                        {
                            if (total > 0)
                            {
                                updateMessage(String.format("Processing batch: %d of %d", current, total));
                            }

                            else
                            {
                                updateMessage(String.format("Processing batch (%d)...", current));
                            }
                        }
                    }
                }

                @Override
                public void reset()
                {
                    if (!isCancelled())
                    {
                        super.reset();
                        isScanning = false;
                        updateMessage("Preparing batch processing...");
                    }
                }
            });

            processor.execute();

            return (processor.getStatistics() != null ? processor.getStatistics() : new BatchStatistics(0, 0, 0L));
        }

        return null;
    }

    @Override
    protected void succeeded()
    {
        updateMessage("Batch completed");

        if (logArea != null)
        {
            if (displayMetadata)
            {
                logArea.appendText("\n[SUCCESS] Exif data retrieved successfully.\n");
            }

            else
            {
                logArea.appendText("\n[SUCCESS] Batch processing complete.\n");
            }
        }
    }

    @Override
    protected void failed()
    {
        updateMessage("Process failed");

        Throwable exc = getException();
        String msg = (exc != null && exc.getMessage() != null ? exc.getMessage() : "An unknown error occurred.");

        if (exc instanceof BatchErrorException)
        {
            logArea.appendText("[ERROR] " + msg + "\n");
        }

        else if (exc == null)
        {
            logArea.appendText("[ERROR] " + msg + "\n");
        }

        else
        {
            logArea.appendText("[ERROR] Unexpected error: " + msg + "\n");
            exc.printStackTrace();
        }

        showErrorDialog(msg);
    }

    @Override
    protected void cancelled()
    {
        updateMessage("Process cancelled");

        if (logArea != null)
        {
            logArea.appendText("[WARNING] Batch process was cancelled.\n");
        }
    }

    @Override
    protected void done()
    {
        processor = null;
    }

    private void showErrorDialog(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Processing Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
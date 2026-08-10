package gui;

import java.util.function.Consumer;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.BatchStatistics;
import batch.DisplayMetadata;
import batch.MediaBatchProcessor;
import javafx.concurrent.Task;
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
class BatchTask extends Task<BatchStatistics>
{
    private final BatchConfiguration config;
    private final TextArea logArea;
    private final ProgressBar progressBar;
    private final boolean displayMetadata;
    private Consumer<Integer> scanCompleteListener;
    private Consumer<Integer> fileProcessedListener;
    private volatile MediaBatchProcessor processor;

    /**
     * Constructs a background task for executing batch processing or metadata extraction.
     *
     * @param config
     *        the validated batch configuration
     * @param logArea
     *        the destination for status messages
     * @param progressBar
     *        the progress bar to update during processing, or {@code null}
     * @param displayMetadata
     *        {@code true} to display metadata instead of processing files
     */
    BatchTask(BatchConfiguration config, TextArea logArea, ProgressBar progressBar, boolean displayMetadata)
    {
        this.config = config;
        this.logArea = logArea;
        this.progressBar = progressBar;
        this.displayMetadata = displayMetadata;
    }

    /**
     * Registers a listener to be notified when the initial file scan is completed.
     * 
     * @param listener
     *        the listener to receive the number of files found during the scan
     */
    void setOnScanCompleted(Consumer<Integer> listener)
    {
        this.scanCompleteListener = listener;
    }

    /**
     * Registers a listener to be notified after each file is processed.
     * 
     * @param listener
     *        the listener to receive the number of files processed
     */
    void setOnFileProcessed(Consumer<Integer> listener)
    {
        this.fileProcessedListener = listener;
    }

    /**
     * Returns the underlying batch processor instance.
     *
     * @return the {@link MediaBatchProcessor}, or {@code null} if it is not yet initialised
     */
    MediaBatchProcessor getProcessor()
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

    /**
     * Executes the batch operation on the background thread.
     * 
     * <p>
     * When metadata display is enabled, metadata is retrieved instead of processing the batch.
     * Otherwise, a {@link MediaBatchProcessor} is created and executed while progress is reported
     * to the associated JavaFX controls.
     * </p>
     * 
     * @return the statistics produced by the batch processor, or {@code null} when metadata is
     *         displayed
     * 
     * @throws Exception
     *         if the batch operation fails
     */
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

            // Extends JavaFXProgressAdapter anonymously
            processor.addProgressListener(new JavaFXProgressAdapter(progressBar)
            {
                private boolean scanMode = true;

                @Override
                public void onProgressUpdate(int current)
                {
                    if (!isCancelled())
                    {
                        super.onProgressUpdate(current);

                        if (scanMode)
                        {
                            updateMessage(String.format("Scanning files (%d found)...", current));
                            
                            if (scanCompleteListener != null)
                            {
                                scanCompleteListener.accept(current);
                            }
                        }

                        else
                        {
                            updateMessage(String.format("Processing batch (%d files)...", current));

                            if (fileProcessedListener != null)
                            {
                                fileProcessedListener.accept(current);
                            }
                        }
                    }
                }

                @Override
                public void onProgressUpdate(int current, int total)
                {
                    if (!isCancelled())
                    {
                        super.onProgressUpdate(current, total);

                        if (scanMode)
                        {
                            if (total > 0)
                            {
                                updateMessage(String.format("Scanning files: %d of %d", current, total));                     
                            }

                            else
                            {
                                updateMessage(String.format("Scanning files (%d)...", current));
                            }
                            
                            if (scanCompleteListener != null)
                            {
                                scanCompleteListener.accept(current);
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

                            if (fileProcessedListener != null)
                            {
                                fileProcessedListener.accept(current);
                            }
                        }
                    }
                }

                @Override
                public void onCompleted(int total)
                {
                    if (scanMode)
                    {
                        scanMode = false;
                    }
                }

                @Override
                public void reset()
                {
                    if (!isCancelled())
                    {
                        super.reset();
                    }
                }
            });

            processor.execute();

            return (processor.getStatistics() != null ? processor.getStatistics() : new BatchStatistics(0, 0, 0L));
        }

        return null;
    }

    /**
     * Handles successful completion of the background task.
     * 
     * <p>
     * Updates the task status message and records a success message in the log area.
     * </p>
     */
    @Override
    protected void succeeded()
    {
        updateMessage("Batch completed");

        if (displayMetadata)
        {
            logArea.appendText("\n[SUCCESS] Exif data retrieved successfully.\n");
        }

        else
        {
            logArea.appendText("\n[SUCCESS] Batch processing complete.\n");
        }
    }

    /**
     * Handles failure of the background task.
     * 
     * <p>
     * Records the exception message in the log area and distinguishes expected
     * {@link BatchErrorException} failures from unexpected errors.
     * </p>
     */
    @Override
    protected void failed()
    {
        updateMessage("Process failed");

        Throwable exc = getException();
        String msg = (exc != null && exc.getMessage() != null ? exc.getMessage() : "An unknown error occurred.");

        if (exc instanceof BatchErrorException || exc == null)
        {
            logArea.appendText("[ERROR] " + msg + "\n");
        }

        else
        {
            logArea.appendText("[ERROR] Unexpected error: " + msg + "\n");
        }
    }

    /**
     * Handles cancellation of the background task.
     * 
     * <p>
     * Updates the task status message and records a cancellation warning in the log area.
     * </p>
     */
    @Override
    protected void cancelled()
    {
        updateMessage("Process cancelled");
        logArea.appendText("[WARNING] Batch process was cancelled.\n");
    }

    /**
     * Performs cleanup after the task reaches a terminal state.
     * 
     * <p>
     * Releases the reference to the active {@link MediaBatchProcessor}.
     * </p>
     */
    @Override
    protected void done()
    {
        processor = null;
    }
}
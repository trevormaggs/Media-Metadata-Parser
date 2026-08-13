package gui;

import java.util.function.Consumer;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.BatchEventType;
import batch.BatchMetrics;
import batch.BatchProcessEvent;
import batch.DisplayMetadata;
import batch.MediaBatchProcessor;
import common.PropertyListener;
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
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 5 May 2026
 */
class BatchTask extends Task<BatchMetrics>
{
    private final BatchConfiguration config;
    private final TextArea logArea;
    private final ProgressBar progressBar;
    private final boolean displayMetadata;
    private PropertyListener fileSummaryListener;
    private Consumer<Integer> fileScannedListener;
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
     * Sets the listener to be notified while a file scan is in progress.
     *
     * @param listener
     *        the listener to receive the progressive count of scanned files
     */
    void setOnFileScanned(Consumer<Integer> listener)
    {
        fileScannedListener = listener;
    }

    /**
     * Sets the listener to be notified after each file is processed.
     *
     * @param listener
     *        the listener to receive the progressive count of processed files
     */
    void setOnFileProcessed(Consumer<Integer> listener)
    {
        fileProcessedListener = listener;
    }

    /**
     * Sets the listener to receive individual file records used for summary reporting.
     *
     * @param listener
     *        the property listener to receive file record updates
     */
    void setFileSummaryListener(PropertyListener listener)
    {
        fileSummaryListener = listener;
    }

    /**
     * Cancels the task and signals the underlying batch processor to abort execution.
     *
     * @param interrupt
     *        {@code true} if the thread executing this task should be interrupted, otherwise,
     *        in-flight calls are allowed to complete
     * @return {@code true} if the task was cancelled
     */
    @Override
    public boolean cancel(boolean interrupt)
    {
        if (processor != null)
        {
            processor.cancel();
        }

        return super.cancel(interrupt);
    }

    /**
     * Executes the batch operation on the background thread.
     *
     * <p>
     * When metadata display is enabled, metadata is retrieved instead of executing a full batch.
     * Otherwise, a {@link MediaBatchProcessor} is created and executed while progress is reported
     * to associated JavaFX controls.
     * </p>
     *
     * @return the {@link BatchMetrics} produced by the batch processor, or zeroed metrics when
     *         cancelled or displaying metadata
     *
     * @throws Exception
     *         if an unrecoverable error occurs during processing
     */
    @Override
    protected BatchMetrics call() throws Exception
    {
        if (isCancelled())
        {
            if (processor != null)
            {
                processor.cancel();
            }
        }

        else if (displayMetadata)
        {
            updateMessage("Retrieving metadata...");
            DisplayMetadata display = new DisplayMetadata(config);
            display.execute();
        }

        else
        {
            processor = new MediaBatchProcessor(config);

            if (fileSummaryListener != null)
            {
                // Handles file processing metrics
                processor.setPropertyListener(new PropertyListener()
                {
                    @Override
                    public void accept(String key, Object value)
                    {
                        if (BatchEventType.FILE_PROCESSED.getKey().equals(key) && value instanceof BatchProcessEvent)
                        {
                            BatchProcessEvent event = (BatchProcessEvent) value;
                            String status = event.isSuccess() ? "Completed" : "Failed";

                            fileSummaryListener.accept(MediaMetadataApp.KEY_SOURCE, event.getSourceName());
                            fileSummaryListener.accept(MediaMetadataApp.KEY_TARGET, event.getTargetName());
                            fileSummaryListener.accept(MediaMetadataApp.KEY_STATUS, status);
                            fileSummaryListener.accept(MediaMetadataApp.KEY_SIZE, event.getTargetSize());
                        }
                    }
                });
            }

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

                            if (fileScannedListener != null)
                            {
                                fileScannedListener.accept(current);
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

                            if (fileScannedListener != null)
                            {
                                fileScannedListener.accept(current);
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

                        if (fileScannedListener != null)
                        {
                            fileScannedListener.accept(total);
                        }
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

            return processor.execute();
        }

        return new BatchMetrics(0, 0, 0L);
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
        super.succeeded();
        
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
        super.failed();
        
        updateMessage("Process failed");

        Throwable exc = getException();
        String msg = (exc != null && exc.getMessage() != null ? exc.getMessage() : "An unknown error occurred.");

        if (exc instanceof BatchErrorException)
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
        super.cancelled();
        
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
        super.done();
        
        processor = null;
    }
}
package gui;

import java.util.function.Consumer;
import batch.BatchConfiguration;
import batch.BatchErrorException;
import batch.BatchMetrics;
import batch.BatchProcessEvent;
import batch.MediaBatchProcessor;
import batch.MetadataInspectionEvent;
import batch.MetadataInspector;
import common.PropertyBiConsumer;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;
import progressbar.JavaFXProgressAdapter;

/**
 * Executes batch media processing or metadata extraction on a background thread.
 *
 * <p>
 * This task coordinates long-running batch operations while reporting progress and completion
 * status to the registered user interface components without blocking the JavaFX Application
 * Thread.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 29 June 2026
 */
class BatchTask extends Task<BatchMetrics>
{
    private final BatchConfiguration config;
    private final ProgressBar progressBar;
    private final boolean display;
    private PropertyBiConsumer fileSummaryListener;
    private Consumer<Integer> fileScannedListener;
    private Consumer<Integer> fileProcessedListener;
    private Consumer<MetadataInspectionEvent> metadataInspectedListener;
    private volatile MediaBatchProcessor processor;

    /**
     * Constructs a background task for executing batch processing or displaying metadata.
     *
     * @param config
     *        the validated batch configuration
     * @param progressBar
     *        the progress bar to update during execution
     * @param displayMetadata
     *        {@code true} to display metadata only, or {@code false} to perform standard batch
     *        processing
     */
    BatchTask(BatchConfiguration config, ProgressBar progressBar, boolean displayMetadata)
    {
        this.config = config;
        this.progressBar = progressBar;
        this.display = displayMetadata;
    }

    /**
     * Sets the listener to be notified while files are scanned.
     *
     * <p>
     * The listener receives the current count of scanned files, allowing the processing statistics
     * table view to be updated downstream while the scan is in progress.
     * </p>
     *
     * @param listener
     *        the listener to receive the current count of scanned files
     */
    void setOnFileScanned(Consumer<Integer> listener)
    {
        fileScannedListener = listener;
    }

    /**
     * Sets the listener to be notified while files are processed.
     *
     * <p>
     * The listener receives the current count of processed files, allowing the processing
     * statistics table view to be updated downstream while the processing is in progress.
     * </p>
     *
     * @param listener
     *        the listener to receive the current count of processed files
     */
    void setOnFileProcessed(Consumer<Integer> listener)
    {
        fileProcessedListener = listener;
    }

    /**
     * Sets the listener to receive batch process events for updating file summary metrics. The
     * listener is notified after each file has been processed, allowing the summary statistics to
     * be updated progressively during batch processing.
     *
     * @param listener
     *        the listener to receive batch process event updates
     */
    void setOnFileSummaryListener(PropertyBiConsumer listener)
    {
        fileSummaryListener = listener;
    }

    /**
     * Registers a listener to receive formatted output text lines extracted during metadata
     * inspection.
     *
     * <p>
     * This listener receives string representations of {@link MetadataInspectionEvent} instances
     * bridged from {@link MetadataInspector} for display in GUI components.
     * </p>
     *
     * @param listener
     *        the text consumer callback to receive formatted metadata lines
     */
    void setOnMetadataInspected(Consumer<MetadataInspectionEvent> listener)
    {
        metadataInspectedListener = listener;
    }

    /**
     * Cancels the task and signals the underlying batch processor to abort execution.
     *
     * @param interrupt
     *        {@code true} to interrupt the thread executing the task, otherwise {@code false}
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
     * When metadata inspection is enabled, metadata is retrieved via
     * {@link MetadataInspector} instead of executing a full batch. Otherwise, a
     * {@link MediaBatchProcessor} is created for execution. In both cases, progress is reported to
     * associated JavaFX controls.
     * </p>
     *
     * @return the {@link BatchMetrics} produced by the batch operation
     *
     * @throws Exception
     *         if an unrecoverable error occurs during processing
     */
    @Override
    protected BatchMetrics call() throws Exception
    {
        if (display)
        {
            MetadataInspector inspector = new MetadataInspector(config);

            inspector.addProgressListener(attachProgressAdapter("Retrieving metadata"));
            inspector.setOnMetadataInspected(new Consumer<MetadataInspectionEvent>()
            {
                /*
                 * Acts as an event adapter that receives MetadataInspectionEvent
                 * notifications from the inspector, converts them to formatted text,
                 * and forwards them to the GUI listener.
                 */
                @Override
                public void accept(final MetadataInspectionEvent event)
                {
                    if (metadataInspectedListener != null)
                    {
                        metadataInspectedListener.accept(event);
                    }
                }
            });

            return inspector.execute();
        }

        processor = new MediaBatchProcessor(config);
        processor.addProgressListener(attachProgressAdapter("Processing batch"));

        if (fileSummaryListener != null)
        {
            processor.setSummaryListener(new PropertyBiConsumer()
            {
                @Override
                public void accept(String key, Object value)
                {
                    if (value instanceof BatchProcessEvent)
                    {
                        // Receives and then forwards BatchProcessEvent updates to the GUI listener
                        fileSummaryListener.accept(key, value);
                    }
                }
            });
        }

        return processor.execute();
    }

    /**
     * Attaches a progress listener adapter for reporting scan and execution progress.
     *
     * @param actionLabel
     *        the descriptive label for the active execution phase, such as "Processing batch" or
     *        "Retrieving metadata"
     * @return the configured progress listener adapter
     */
    private JavaFXProgressAdapter attachProgressAdapter(String actionLabel)
    {
        return new JavaFXProgressAdapter(progressBar)
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
                        updateMessage(String.format("%s (%d files)...", actionLabel, current));

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
                            updateMessage(String.format("%s: %d of %d", actionLabel, current, total));
                        }

                        else
                        {
                            updateMessage(String.format("%s (%d)...", actionLabel, current));
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
        };
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
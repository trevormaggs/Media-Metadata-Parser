package gui;

import java.util.function.Consumer;
import batch.BatchConfiguration;
import batch.BatchMetrics;
import batch.BatchProcessEvent;
import batch.MediaBatchProcessor;
import batch.MetadataInspectionEvent;
import batch.MetadataInspector;
import common.PropertyBiConsumer;
import javafx.concurrent.Task;
import progressbar.ProgressListener;

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
    private final boolean display;
    private PropertyBiConsumer batchSummaryListener;
    private Consumer<Integer> fileScannedListener;
    private Consumer<Integer> fileProcessedListener;
    private Consumer<MetadataInspectionEvent> metadataInspectedListener;
    private volatile MediaBatchProcessor processor;

    /**
     * Constructs a background task for executing batch processing or displaying metadata.
     *
     * @param config
     *        the validated batch configuration
     * @param displayMetadata
     *        {@code true} to display metadata only, or {@code false} to perform standard batch
     *        processing
     */
    BatchTask(BatchConfiguration config, boolean displayMetadata)
    {
        this.config = config;
        this.display = displayMetadata;
    }

    /**
     * Sets the listener to be notified while files are scanned.
     *
     * <p>
     * The listener receives the current count of scanned files, allowing the processing statistics
     * view to be updated downstream while the scan is in progress.
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
     * statistics view to be updated downstream while the processing is in progress.
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
     * Sets the listener to receive batch process events for updating file summary metrics.
     *
     * <p>
     * Batch process events received from the {@link MediaBatchProcessor} are forwarded to the
     * registered listener as processing progresses.
     * </p>
     *
     * @param listener
     *        the listener to receive batch process event updates
     */
    void setOnBatchSummaryListener(PropertyBiConsumer listener)
    {
        batchSummaryListener = listener;
    }

    /**
     * Registers a listener to receive notifications generated during metadata inspection.
     *
     * <p>
     * The listener receives each {@link MetadataInspectionEvent} produced by the
     * {@link MetadataInspector} and forwards it for display or further processing by GUI
     * components.
     * </p>
     *
     * @param listener
     *        the listener to receive metadata inspection events
     */
    void setOnMetadataInspected(Consumer<MetadataInspectionEvent> listener)
    {
        metadataInspectedListener = listener;
    }

    /**
     * Cancels the task and requests cancellation of the underlying batch processor.
     *
     * @param interrupt
     *        {@code true} to interrupt the thread executing the task, otherwise {@code false}
     * @return {@code true} if the task was successfully cancelled
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
     * When metadata inspection is selected, metadata is retrieved via {@link MetadataInspector}
     * instead. Otherwise, a {@link MediaBatchProcessor} is created for execution. In both cases,
     * progress is reported to associated JavaFX controls.
     * </p>
     *
     * @return the {@link BatchMetrics} produced by the associated operation
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

            inspector.addProgressListener(createProgressListener("Retrieving metadata"));

            inspector.setOnMetadataInspected(new Consumer<MetadataInspectionEvent>()
            {
                /*
                 * Acts as an event adapter that receives MetadataInspectionEvent
                 * notifications from the inspector, and then forwards them to the
                 * GUI listener.
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
        processor.addProgressListener(createProgressListener("Processing batch"));

        if (batchSummaryListener != null)
        {
            processor.setSummaryListener(new PropertyBiConsumer()
            {
                @Override
                public void accept(String key, Object value)
                {
                    if (value instanceof BatchProcessEvent)
                    {
                        // Forward BatchProcessEvent updates to the GUI listener.
                        batchSummaryListener.accept(key, value);
                    }
                }
            });
        }

        return processor.execute();
    }

    /**
     * Creates a progress listener for reporting scan and execution progress.
     *
     * @param actionLabel
     *        the descriptive label for the active execution phase, such as "Processing batch" or
     *        "Retrieving metadata"
     * @return the configured progress listener
     */
    private ProgressListener createProgressListener(String actionLabel)
    {
        return new ProgressListener()
        {
            private boolean scanMode = true;

            @Override
            public void onProgressUpdate(int current, int total)
            {
                if (!isCancelled())
                {
                    /*
                     * Make sure Task.updateProgress is updated so
                     * task.progressProperty() fires as it should.
                     */
                    if (total > 0)
                    {
                        updateProgress(current, total);
                    }

                    else
                    {
                        updateProgress(-1, 1);
                    }

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
                    updateProgress(0, 1);
                }
            }
        };
    }

    /**
     * Handles successful completion of the background task.
     *
     * <p>
     * Updates the task message to indicate that the batch operation has completed successfully.
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
     * Updates the task message to indicate that the batch operation failed. The exception that
     * caused the failure remains available through the task's exception state.
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
     * Updates the task message to indicate that the batch operation was cancelled.
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
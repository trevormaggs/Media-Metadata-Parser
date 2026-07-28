package gui;

import batch.BatchConfiguration;
import batch.BatchErrorException;
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
class BatchTask extends Task<Void>
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
    BatchTask(BatchConfiguration config, TextArea logArea, ProgressBar progressBar, boolean displayMetadata)
    {
        this.config = config;
        this.logArea = logArea;
        this.progressBar = progressBar;
        this.displayMetadata = displayMetadata;
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
    protected Void call() throws Exception
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

            try (MediaBatchProcessor activeProc = processor)
            {
                if (progressBar != null)
                {
                    activeProc.addProgressListener(new JavaFXProgressAdapter(progressBar)
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
                }

                activeProc.execute();
            }
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

        if (logArea == null)
        {
            return;
        }

        Throwable exc = getException();
        Throwable cause = (exc != null && exc.getCause() != null) ? exc.getCause() : exc;

        if (cause instanceof BatchErrorException)
        {
            logArea.appendText("[ERROR] " + cause.getMessage() + "\n");
        }

        else if (cause != null)
        {
            cause.printStackTrace();
            logArea.appendText("[ERROR] Unexpected error: " + cause.getMessage() + "\n");
        }
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
}
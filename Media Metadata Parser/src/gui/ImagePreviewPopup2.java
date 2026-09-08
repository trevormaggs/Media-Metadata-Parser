package gui;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import common.DigitalSignature;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * A lightweight, frameless floating JavaFX popup window that displays dynamic image previews when
 * hovering over media records.
 *
 * <p>
 * Includes an LRU memory cache to serve previously decoded thumbnails quickly, avoiding unnecessary
 * re-decodes on repeated hovers.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 7 September 2026
 */
public class ImagePreviewPopup2
{
    private static final int MAX_CACHE_SIZE = 50;
    private final Path targetDir;
    private final Stage popupStage;
    private final ImageView imageView;
    private final Label unsupportedLabel;
    private Task<Image> currentThreadTask;
    private final Map<Path, Image> thumbnailCache;
    private final ExecutorService imageLoaderExecutor;

    /**
     * Constructs a new floating image preview popup associated with a parent window.
     *
     * @param ownerWindow
     *        the parent {@link Window} that owns the popup, or {@code null} if no owner is
     *        specified
     * @param targetDir
     *        the base {@link Path} directory used for resolving relative paths, or {@code null}
     */
    public ImagePreviewPopup2(Window ownerWindow, Path targetDir)
    {
        popupStage = new Stage();
        popupStage.initStyle(StageStyle.TRANSPARENT);
        popupStage.initOwner(ownerWindow);

        imageView = new ImageView();
        imageView.setFitWidth(250);
        imageView.setFitHeight(250);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        unsupportedLabel = new Label("Format currently not supported");
        unsupportedLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 15px;");
        unsupportedLabel.setAlignment(Pos.CENTER);

        StackPane container = new StackPane(imageView, unsupportedLabel);
        container.setPrefSize(250, 250);
        container.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 8px; -fx-background-radius: 6px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);");

        Scene popupScene = new Scene(container);
        popupScene.setFill(null);
        popupStage.setScene(popupScene);

        this.targetDir = targetDir;

        /*
         * We use a dedicated single-threaded background executor to ensure intensive
         * thumbnail decoding operations are handled sequentially. This prevents disk I/O
         * thrashing caused by reading multiple large files concurrently, while keeping
         * the main UI thread completely responsive.
         */
        this.imageLoaderExecutor = Executors.newSingleThreadExecutor(new ThreadFactory()
        {
            @Override
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread(r, "ImagePreview-Loader-Thread");
                t.setDaemon(true);
                return t;
            }
        });

        /*
         * Since storing multiple images in a Map could potentially cause an OutOfMemoryError, we
         * use a thread-safe LRU (Least Recently Used) cache to map file paths to scaled JavaFX
         * Image objects. The least recently accessed thumbnail is automatically evicted whenever
         * the cache exceeds MAX_CACHE_SIZE entries. This ensures fast, instant loading on repeated
         * hovers over them.
         */
        this.thumbnailCache = Collections.synchronizedMap(new LinkedHashMap<Path, Image>(MAX_CACHE_SIZE, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Path, Image> eldest)
            {
                return size() > MAX_CACHE_SIZE;
            }
        });
    }

    /**
     * Displays the preview thumbnail overlay for a specified file record at the given cursor
     * coordinates.
     *
     * <p>
     * Checks the LRU memory cache before reading from disk. On a cache miss, JPG and PNG images are
     * decoded using JavaFX, while other supported formats are decoded using an {@link ImageIO}
     * {@link ImageReader} with source subsampling, being implemented by TwelveMonkeys stream
     * subsampling.
     * </p>
     *
     * @param record
     *        the {@link FileProcessingRecord} containing target path and magic signature metadata
     * @param screenX
     *        the absolute horizontal cursor coordinate on screen
     * @param screenY
     *        the absolute vertical cursor coordinate on screen
     */
    public void showPreview(FileProcessingRecord record, double screenX, double screenY)
    {
        if (record == null || record.getTargetPath() == null)
        {
            hide();
            return;
        }

        final Path fpath = record.getTargetPath();
        final Path realPath = (fpath.isAbsolute() ? fpath : (targetDir != null ? targetDir.resolve(fpath) : fpath.toAbsolutePath()));

        if (!Files.exists(realPath))
        {
            hide();
            return;
        }

        Image cachedThumb = thumbnailCache.get(realPath);
        final DigitalSignature sig = record.getDigitalSignature();

        if (!isViewable(sig))
        {
            imageView.setImage(null);
            imageView.setVisible(false);
            unsupportedLabel.setVisible(true);
            showThumbnailPopup(screenX, screenY);

            return;
        }

        // 1. FAST PATH: Instant Cache Hit on FX Application Thread
        if (cachedThumb != null)
        {
            if (currentThreadTask != null && currentThreadTask.isRunning())
            {
                currentThreadTask.cancel();
            }

            unsupportedLabel.setVisible(false);
            imageView.setVisible(true);
            imageView.setImage(cachedThumb);
            showThumbnailPopup(screenX, screenY);

            return;
        }

        // 2. SLOW PATH: Cancel previous pending task & load asynchronously
        if (currentThreadTask != null && currentThreadTask.isRunning())
        {
            currentThreadTask.cancel();
        }

        // Temporarily clear current image while background thread decodes
        imageView.setImage(null);
        imageView.setVisible(false);
        unsupportedLabel.setVisible(false);

        Task<Image> task = new Task<Image>()
        {
            @Override
            protected Image call() throws Exception
            {
                if (sig == DigitalSignature.JPG || sig == DigitalSignature.PNG)
                {
                    try (InputStream is = Files.newInputStream(realPath))
                    {
                        return new Image(is, 250, 250, true, true);
                    }
                }

                return readThumbnail(realPath, 250, 250);
            }
        };

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                if (task != currentThreadTask)
                {
                    return;
                }

                Image loadedImage = task.getValue();

                if (loadedImage == null)
                {
                    imageView.setImage(null);
                    imageView.setVisible(false);
                    unsupportedLabel.setVisible(true);
                }

                else
                {
                    thumbnailCache.put(realPath, loadedImage);
                    imageView.setVisible(true);
                    unsupportedLabel.setVisible(false);
                    imageView.setImage(loadedImage);
                }
            }
        });

        task.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                if (task != currentThreadTask)
                {
                    return;
                }

                imageView.setImage(null);
                imageView.setVisible(false);
                unsupportedLabel.setVisible(true);
            }
        });

        currentThreadTask = task;
        showThumbnailPopup(screenX, screenY);
        imageLoaderExecutor.submit(task);
    }

    /**
     * Displays the thumbnail preview popup and positions it relative to the specified screen
     * coordinates.
     *
     * <p>
     * The popup is positioned to the lower-right of the cursor when space permits. If insufficient
     * space is available at the right or bottom edge of the screen, the popup is repositioned to
     * the opposite side of the cursor.
     * </p>
     *
     * @param screenX
     *        the absolute horizontal cursor coordinate on screen
     * @param screenY
     *        the absolute vertical cursor coordinate on screen
     */
    private void showThumbnailPopup(double screenX, double screenY)

    {
        if (!popupStage.isShowing())
        {
            popupStage.show();
        }

        Rectangle2D screenBounds = Screen.getScreensForRectangle(screenX, screenY, 1, 1).get(0).getVisualBounds();
        double popupWidth = popupStage.getWidth() > 0 ? popupStage.getWidth() : 266;
        double popupHeight = popupStage.getHeight() > 0 ? popupStage.getHeight() : 266;
        double targetX = screenX + 15;
        double targetY = screenY + 15;

        if (targetX + popupWidth > screenBounds.getMaxX())
        {
            targetX = screenX - popupWidth - 10;
        }

        if (targetY + popupHeight > screenBounds.getMaxY())
        {
            targetY = screenY - popupHeight - 10;
        }

        popupStage.setX(targetX);
        popupStage.setY(targetY);
    }

    /**
     * Reads and scales an image to create a lightweight thumbnail using a native ImageIO image
     * reader.
     *
     * <p>
     * Source sub-sampling is used to reduce the amount of image data decoded for large source
     * images.
     * </p>
     *
     * @param path
     *        the {@link Path} to the target image file
     * @param targetWidth
     *        the maximum desired thumbnail width in pixels
     * @param targetHeight
     *        the maximum desired thumbnail height in pixels
     * @return a scaled JavaFX {@link Image}, or {@code null} if decoding fails or no reader is
     *         registered
     */
    private Image readThumbnail(Path path, int targetWidth, int targetHeight)
    {
        try (ImageInputStream stream = ImageIO.createImageInputStream(path.toFile()))
        {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            if (readers.hasNext())
            {
                ImageReader reader = readers.next();

                try
                {
                    reader.setInput(stream);

                    int imageWidth = reader.getWidth(0);
                    int imageHeight = reader.getHeight(0);

                    int subsample = Math.max(1, Math.min(imageWidth / targetWidth, imageHeight / targetHeight));

                    ImageReadParam param = reader.getDefaultReadParam();
                    param.setSourceSubsampling(subsample, subsample, 0, 0);

                    BufferedImage bImg = reader.read(0, param);

                    return SwingFXUtils.toFXImage(bImg, null);
                }

                finally
                {
                    reader.dispose();
                }
            }
        }

        catch (Exception exc)
        {
            // Pass through to return null on stream read error
        }

        return null;
    }

    /**
     * Hides the preview popup stage and releases the displayed image reference. The image remains
     * cached in memory within {@link #thumbnailCache}.
     */
    public void hide()
    {
        if (currentThreadTask != null && currentThreadTask.isRunning())
        {
            currentThreadTask.cancel();
        }

        if (popupStage.isShowing())
        {
            popupStage.hide();
        }

        imageView.setImage(null);
    }

    /**
     * Clears all cached thumbnails from memory. Call this if the active workspace or target
     * directory changes.
     */
    public void clearCache()
    {
        thumbnailCache.clear();
    }

    /**
     * Cancels pending operations, hides the stage, and shuts down the background image loader.
     * Call this when shutting down the application.
     */
    public void dispose()
    {
        hide();
        imageLoaderExecutor.shutdownNow();
    }

    /**
     * Determines whether a digital signature corresponds to a supported preview format.
     *
     * @param type
     *        the {@link DigitalSignature} magic-number signature enum to check
     * @return {@code true} if the signature corresponds to a supported preview format,
     *         {@code false} otherwise
     */
    private boolean isViewable(DigitalSignature type)
    {
        return type == DigitalSignature.JPG || type == DigitalSignature.PNG ||
                type == DigitalSignature.TIF || type == DigitalSignature.WEBP ||
                type == DigitalSignature.DNG;
    }
}
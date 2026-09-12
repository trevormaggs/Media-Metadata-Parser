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
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import common.DigitalSignature;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;

/**
 * A lightweight, frameless JavaFX popup window that displays dynamic image previews when hovering
 * over media records.
 *
 * <p>
 * Previously loaded thumbnails are kept in memory so they can be displayed quickly when viewed
 * again.
 * </p>
 *
 * <p>
 * <b>Note:</b> Extended image format support (such as TIFF, WebP, and DNG) relies on external
 * <a href="https://github.com/haraldk/TwelveMonkeys">TwelveMonkeys ImageIO</a> plugins registered
 * in the application classpath.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.3
 * @since 7 September 2026
 */
public class ImagePreviewPopup
{
    private static final int MAX_CACHE_SIZE = 50;
    private final Path targetDir;
    private final Stage popupStage;
    private final ImageView imageView;
    private final Label unsupportedLabel;
    private final HBox overlayBar;
    private final Map<Path, Image> thumbnailCache;
    private final ExecutorService imageLoaderExecutor;
    private Task<Image> currentThreadTask;

    /**
     * Creates a new floating image preview popup associated with a parent window.
     *
     * @param owner
     *        the parent {@link Window}, or {@code null} if no owner is specified
     * @param targetDir
     *        the base {@link Path} used to resolve relative paths, or {@code null}
     */
    public ImagePreviewPopup(Window owner, Path targetDir)
    {
        popupStage = new Stage();
        popupStage.initStyle(StageStyle.TRANSPARENT);
        popupStage.initOwner(owner);

        imageView = new ImageView();
        imageView.setFitWidth(250);
        imageView.setFitHeight(250);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        unsupportedLabel = new Label("Format currently not supported");
        unsupportedLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 15px;");
        unsupportedLabel.setAlignment(Pos.CENTER);

        // Metadata Footer Bar Setup
        Label formatLabel = new Label();
        formatLabel.setUserData("FORMAT");
        formatLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label dimensionsLabel = new Label();
        dimensionsLabel.setUserData("DIMENSIONS");
        dimensionsLabel.setStyle("-fx-text-fill: #dcdcdc; -fx-font-size: 11px;");

        Label sizeLabel = new Label();
        sizeLabel.setUserData("SIZE");
        sizeLabel.setStyle("-fx-text-fill: #dcdcdc; -fx-font-size: 11px;");

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        overlayBar = new HBox(8, formatLabel, spacer1, dimensionsLabel, spacer2, sizeLabel);
        overlayBar.setAlignment(Pos.CENTER);
        overlayBar.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 6px 10px 6px 10px; -fx-background-radius: 0 0 6px 6px;");
        overlayBar.setMaxWidth(Double.MAX_VALUE);
        overlayBar.setMinHeight(Region.USE_PREF_SIZE);
        VBox.setVgrow(overlayBar, Priority.NEVER);

        // Image container with unsupported format overlay
        StackPane imageHolder = new StackPane(imageView, unsupportedLabel);
        imageHolder.setAlignment(Pos.CENTER);
        VBox.setVgrow(imageHolder, Priority.ALWAYS);

        // Dynamic VBox container that shrinks to fit the thumbnail height + footer
        VBox container = new VBox(imageHolder, overlayBar);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 6px; -fx-background-radius: 6px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);");

        Scene popupScene = new Scene(container);
        popupScene.setFill(null);
        popupStage.setScene(popupScene);

        // Prevent container from stealing focus from underlying control
        container.setFocusTraversable(false);

        this.targetDir = targetDir;

        /*
         * We use a dedicated single-threaded background executor to ensure intensive
         * thumbnail decoding operations are handled sequentially. This prevents disk I/O
         * thrashing caused by reading multiple large files concurrently, while keeping
         * the main UI thread completely responsive.
         */
        this.imageLoaderExecutor = Executors.newSingleThreadExecutor(r ->
        {
            Thread t = new Thread(r, "ImagePreview-Loader-Thread");
            t.setDaemon(true);
            return t;
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
     * Clears all cached thumbnails from memory.
     *
     * <p>
     * Call this when the active workspace or target directory changes so that thumbnails from the
     * previous location are no longer retained.
     * </p>
     */
    public void clearCache()
    {
        thumbnailCache.clear();
        imageView.setImage(null);
    }

    /**
     * Hides the preview popup, cancels background image loading, clears the thumbnail cache, and
     * shuts down the image loader.
     *
     * <p>
     * Call this when the application is shutting down.
     * </p>
     */
    public void dispose()
    {
        hide();
        imageLoaderExecutor.shutdownNow();
        clearCache();
    }

    /**
     * Hides the preview popup and removes the currently displayed image.
     *
     * <p>
     * The image remains available in the thumbnail cache for future previews.
     * </p>
     */
    public void hide()
    {
        if (currentThreadTask != null && !currentThreadTask.isDone())
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
     * Displays an image preview for the specified file record at the given screen coordinates.
     *
     * <p>
     * Previously loaded thumbnails are displayed immediately. Otherwise, the image is loaded in the
     * background and displayed when ready.
     * </p>
     *
     * @param record
     *        the {@link ProcessedFileRecord} containing the target path and file type information
     * @param screenX
     *        the horizontal cursor position on screen
     * @param screenY
     *        the vertical cursor position on screen
     */
    public void showPreview(ProcessedFileRecord record, double screenX, double screenY)
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
            overlayBar.setVisible(false);
            showThumbnailPopup(screenX, screenY);

            return;
        }

        // Instant Cache Hit on FX Application Thread
        if (cachedThumb != null)
        {
            if (currentThreadTask != null && currentThreadTask.isRunning())
            {
                currentThreadTask.cancel();
            }

            unsupportedLabel.setVisible(false);
            imageView.setVisible(true);
            imageView.setImage(cachedThumb);
            updateOverlay(record, cachedThumb);
            showThumbnailPopup(screenX, screenY);

            return;
        }

        if (currentThreadTask != null && currentThreadTask.isRunning())
        {
            currentThreadTask.cancel();
        }

        imageView.setImage(null);
        imageView.setVisible(false);
        unsupportedLabel.setVisible(false);
        overlayBar.setVisible(false);

        // Each Task instance is persistently bound to a single image loading request. If the user
        // hovers over a new image, 'currentThreadTask' is reassigned to a NEW Task instance.
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
                if (task == currentThreadTask)
                {
                    Image loadedImage = task.getValue();

                    if (loadedImage == null)
                    {
                        imageView.setImage(null);
                        imageView.setVisible(false);
                        unsupportedLabel.setVisible(true);
                        overlayBar.setVisible(false);
                    }

                    else
                    {
                        thumbnailCache.put(realPath, loadedImage);
                        imageView.setVisible(true);
                        unsupportedLabel.setVisible(false);
                        imageView.setImage(loadedImage);
                        updateOverlay(record, loadedImage);
                    }
                }
            }
        });

        task.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent event)
            {
                if (task == currentThreadTask)
                {
                    imageView.setImage(null);
                    imageView.setVisible(false);
                    unsupportedLabel.setVisible(true);
                    overlayBar.setVisible(false);
                }
            }
        });

        currentThreadTask = task;
        showThumbnailPopup(screenX, screenY);
        imageLoaderExecutor.submit(task);
    }

    /**
     * Finds an overlay label associated with the specified key.
     *
     * @param key
     *        the key used to identify the label
     * @return the matching {@link Label}, or {@code null} if no matching label is found
     */
    private Label getOverlayLabel(String key)
    {
        for (Node node : overlayBar.getChildren())
        {
            if (key.equals(node.getUserData()) && node instanceof Label)
            {
                return (Label) node;
            }
        }

        return null;
    }

    /**
     * Updates text labels on the translucent metadata bar.
     */
    private void updateOverlay(ProcessedFileRecord record, Image loadedImage)
    {
        if (record == null)
        {
            overlayBar.setVisible(false);
            return;
        }

        DigitalSignature sig = record.getDigitalSignature();
        Label formatLabel = getOverlayLabel("FORMAT");
        Label dimensionsLabel = getOverlayLabel("DIMENSIONS");
        Label sizeLabel = getOverlayLabel("SIZE");

        if (formatLabel != null)
        {
            formatLabel.setText(sig != null ? sig.name() : "FILE");
        }

        if (dimensionsLabel != null)
        {
            if (loadedImage != null && !loadedImage.isError())
            {
                int width = (int) loadedImage.getWidth();
                int height = (int) loadedImage.getHeight();
                dimensionsLabel.setText(width + "×" + height);
            }

            else
            {
                dimensionsLabel.setText("—");
            }
        }

        if (sizeLabel != null)
        {
            sizeLabel.setText(UtilsJavaFX.formatFileSize(record.getFileSize()));
        }

        overlayBar.setVisible(true);
    }

    /**
     * Displays the thumbnail preview popup and positions it near the specified screen coordinates.
     *
     * <p>
     * The popup is positioned to the lower-right of the cursor when space permits. If there is not
     * enough space, it is moved to the opposite side of the cursor.
     * </p>
     *
     * @param screenX
     *        the horizontal cursor position on screen
     * @param screenY
     *        the vertical cursor position on screen
     */
    private void showThumbnailPopup(double screenX, double screenY)
    {
        if (!popupStage.isShowing())
        {
            popupStage.show();

            if (popupStage.getOwner() != null)
            {
                popupStage.getOwner().requestFocus();
            }
        }

        popupStage.sizeToScene();

        Rectangle2D screenBounds = Screen.getScreensForRectangle(screenX, screenY, 1, 1).get(0).getVisualBounds();
        double popupWidth = popupStage.getWidth() > 0 ? popupStage.getWidth() : 262;
        double popupHeight = popupStage.getHeight() > 0 ? popupStage.getHeight() : 290;
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
     * Reads an image and creates a thumbnail of the requested size.
     *
     * <p>
     * Large images are reduced while being read to avoid loading more image data than necessary.
     * </p>
     *
     * @param path
     *        the {@link Path} to the image file
     * @param targetWidth
     *        the maximum desired thumbnail width in pixels
     * @param targetHeight
     *        the maximum desired thumbnail height in pixels
     * @return a scaled JavaFX {@link Image}, or {@code null} if the image cannot be read
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
                    BufferedImage bufImage = reader.read(0, param);

                    return SwingFXUtils.toFXImage(bufImage, null);
                }

                finally
                {
                    reader.dispose();
                }
            }
        }

        catch (Exception exc)
        {
            return null;
        }

        return null;
    }

    /**
     * Determines whether the file type is supported for image previews.
     *
     * @param type
     *        the file type to check
     * @return {@code true} if the file type can be previewed, {@code false} otherwise
     */
    private static boolean isViewable(DigitalSignature type)
    {
        return type == DigitalSignature.JPG || type == DigitalSignature.PNG ||
                type == DigitalSignature.TIF || type == DigitalSignature.WEBP ||
                type == DigitalSignature.DNG;
    }
}
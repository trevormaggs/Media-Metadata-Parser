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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
 * re-decodes on repeated hovers, as well as a sleek metadata footer bar.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.4
 * @since 11 September 2026
 */
public class ImagePreviewPopup
{
    private static final int MAX_CACHE_SIZE = 50;
    private final Path targetDir;
    private final Stage popupStage;
    private final ImageView imageView;
    private final Label unsupportedLabel;

    // Metadata Overlay Controls
    private final HBox overlayBar;
    private final Label formatLabel;
    private final Label dimensionsLabel;
    private final Label sizeLabel;

    private final Map<Path, Image> thumbnailCache;
    private final ExecutorService imageLoaderExecutor;
    private Task<Image> currentThreadTask;

    /**
     * Constructs a new floating image preview popup associated with a parent window.
     *
     * @param owner
     *        the parent {@link Window} that owns the popup, or {@code null} if no owner is
     *        specified
     * @param targetDir
     *        the base {@link Path} directory used for resolving relative paths, or {@code null}
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
        formatLabel = new Label();
        formatLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold;");

        dimensionsLabel = new Label();
        dimensionsLabel.setStyle("-fx-text-fill: #dcdcdc; -fx-font-size: 11px;");

        sizeLabel = new Label();
        sizeLabel.setStyle("-fx-text-fill: #dcdcdc; -fx-font-size: 11px;");

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        overlayBar = new HBox(8, formatLabel, spacer1, dimensionsLabel, spacer2, sizeLabel);
        overlayBar.setAlignment(Pos.CENTER);
        // Match bottom corner radiuses of outer container (6px)
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
     * Clears all cached thumbnails from memory. Call this if the active workspace or target
     * directory changes.
     */
    public void clearCache()
    {
        thumbnailCache.clear();
        imageView.setImage(null);
        System.gc();
    }

    /**
     * Cancels pending operations, hides the stage, and shuts down the background image loader.
     */
    public void dispose()
    {
        hide();
        imageLoaderExecutor.shutdownNow();
        clearCache();
    }

    /**
     * Hides the preview popup stage and releases the displayed image reference.
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
     * Displays the preview thumbnail overlay for a specified file record at the given cursor
     * coordinates.
     *
     * @param record
     *        the {@link ProcessedFileRecord} containing target path and magic signature metadata
     * @param screenX
     *        the absolute horizontal cursor coordinate on screen
     * @param screenY
     *        the absolute vertical cursor coordinate on screen
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

        // Cancel previous pending task & load asynchronously
        if (currentThreadTask != null && currentThreadTask.isRunning())
        {
            currentThreadTask.cancel();
        }

        // Temporarily clear current image while background thread decodes
        imageView.setImage(null);
        imageView.setVisible(false);
        unsupportedLabel.setVisible(false);
        overlayBar.setVisible(false);

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
        formatLabel.setText(sig != null ? sig.name() : "FILE");

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

        sizeLabel.setText(formatFileSize(record.getFileSize()));
        overlayBar.setVisible(true);
    }

    /**
     * Helper to format raw byte values into human-readable string units.
     */
    private String formatFileSize(long bytes)
    {
        if (bytes <= 0)
        {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);

        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

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
            // Pass through to return null on stream read error
        }

        return null;
    }

    private boolean isViewable(DigitalSignature type)
    {
        return type == DigitalSignature.JPG || type == DigitalSignature.PNG ||
                type == DigitalSignature.TIF || type == DigitalSignature.WEBP ||
                type == DigitalSignature.DNG;
    }
}
package gui;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import common.DigitalSignature;
import javafx.embed.swing.SwingFXUtils;
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
 * Supports native JavaFX formats (JPEG, PNG) directly via standard streams, and delegates
 * high-density formats (TIFF, WebP, DNG) to TwelveMonkeys ImageIO plug-ins using stream sub-sampling
 * to prevent full-raster memory allocations.
 * </p>
 *
 * Note, unfortunately, there is no TwelveMonkeys ImageIO plug-in to support the HEIC/HEIF format.
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 7 September 2026
 */
public class ImagePreviewPopup2
{
    private final Path targetDir;
    private final Stage popupStage;
    private final ImageView imageView;
    private final Label unsupportedLabel;

    /**
     * Constructs a new floating image preview popup associated with a parent window.
     *
     * @param ownerWindow
     *        the parent {@link Window} anchor for modal and z-index ordering
     * @param targetDir
     *        the base {@link Path} directory used for resolving relative paths, or {@code null}
     */
    public ImagePreviewPopup2(Window ownerWindow, Path targetDir)
    {
        this.targetDir = targetDir;

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
    }

    /**
     * Displays the preview thumbnail overlay for a specified file record at the given cursor
     * coordinates.
     *
     * <p>
     * Resolves relative paths against {@code targetDir}, checks viewability via magic-number
     * signatures, clamps window placement within screen visual bounds, and safely falls back to a
     * message label on failure.
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
        if (record == null)
        {
            hide();
            return;
        }

        Path fpath = record.getTargetPath();

        if (fpath == null)
        {
            hide();
            return;
        }

        Path realPath = (fpath.isAbsolute() ? fpath : (targetDir != null ? targetDir.resolve(fpath) : fpath.toAbsolutePath()));

        if (Files.exists(realPath))
        {
            DigitalSignature sig = record.getDigitalSignature();

            if (isViewable(sig))
            {
                imageView.setVisible(true);
                unsupportedLabel.setVisible(false);

                try (InputStream is = Files.newInputStream(realPath))
                {
                    Image thumb;

                    // Native JavaFX Formats
                    if (sig == DigitalSignature.JPG || sig == DigitalSignature.PNG)
                    {
                        thumb = new Image(is, 250, 250, true, true);
                    }

                    else
                    {
                        // Use TwelveMonkeys ImageIO decoders for TIFF, WebP, and DNG
                        thumb = readThumbnail(realPath, 250, 250);

                        if (thumb == null)
                        {
                            throw new Exception("Decoder returned null for format: " + sig);
                        }
                    }

                    imageView.setImage(thumb);
                }

                catch (Exception exc)
                {
                    imageView.setImage(null);  
                    imageView.setVisible(false);
                    unsupportedLabel.setVisible(true);
                }
            }

            else
            {
                imageView.setImage(null);
                imageView.setVisible(false);
                unsupportedLabel.setVisible(true);
            }

            // Show window FIRST so JavaFX measures stage dimensions accurately
            if (!popupStage.isShowing())
            {
                popupStage.show();
            }

            // Calculate screen bounds and clamp window position to monitor visual bounds
            Rectangle2D screenBounds = Screen.getScreensForRectangle(screenX, screenY, 1, 1).get(0).getVisualBounds();

            // Fall back to container size (250x250 + padding) if stage size hasn't reported yet
            double popupWidth = popupStage.getWidth() > 0 ? popupStage.getWidth() : 266;
            double popupHeight = popupStage.getHeight() > 0 ? popupStage.getHeight() : 266;

            double targetX = screenX + 15;
            double targetY = screenY + 15;

            // Flip to left side of cursor if extending past right boundary
            if (targetX + popupWidth > screenBounds.getMaxX())
            {
                targetX = screenX - popupWidth - 10;
            }

            // Flip above cursor if extending past bottom boundary
            if (targetY + popupHeight > screenBounds.getMaxY())
            {
                targetY = screenY - popupHeight - 10;
            }

            popupStage.setX(targetX);
            popupStage.setY(targetY);
        }
        else
        {
            hide();
        }
    }

    /**
     * Reads and scales an image to create a lightweight thumbnail, utilizing TwelveMonkeys ImageIO
     * decoders to support formats not natively handled by JavaFX, such as TIFF, DNG, and WebP.
     * 
     * <p>
     * Standard {@code ImageIO.read()} forces a full-raster decode into heap memory before scaling,
     * which causes significant latency on large files. This method uses stream subsampling to skip
     * intermediate pixel bytes during decoding. Header metadata is retrieved via
     * {@code reader.getWidth(0)} and {@code reader.getHeight(0)} without rendering full pixel
     * arrays, bypassing full-raster memory allocations.
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

                    // Calculate image dimensions without full decode
                    int imageWidth = reader.getWidth(0);
                    int imageHeight = reader.getHeight(0);

                    // Calculate subsampling ratio to decode a fraction of total pixels
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
     * Hides the preview popup stage and releases the rendered image reference to conserve RAM.
     */
    public void hide()
    {
        if (popupStage.isShowing())
        {
            popupStage.hide();
            imageView.setImage(null);
        }
    }

    /**
     * Evaluates whether a given file signature matches supported preview formats.
     *
     * @param type
     *        the {@link DigitalSignature} magic-number signature enum to check
     * @return {@code true} if format decoding is supported; {@code false} otherwise
     */
    private boolean isViewable(DigitalSignature type)
    {
        return type == DigitalSignature.JPG ||
                type == DigitalSignature.PNG ||
                type == DigitalSignature.TIF ||
                type == DigitalSignature.WEBP ||
                type == DigitalSignature.DNG;
    }
}
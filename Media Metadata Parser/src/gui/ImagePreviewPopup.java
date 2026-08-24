package gui;

import java.io.File;
import java.nio.file.Path;
import common.DigitalSignature;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class ImagePreviewPopup
{
    private final Stage popupStage;
    private final ImageView imageView;
    private final Label unsupportedLabel;
    private final Path targetDir;

    public ImagePreviewPopup(Window ownerWindow, Path targetDir)
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

        // If path is relative, resolve it against the target output directory
        Path resolvedPath = (fpath.isAbsolute() ? fpath : (targetDir != null ? targetDir.resolve(fpath) : fpath.toAbsolutePath()));
        File file = resolvedPath.toFile();

        if (file.exists())
        {
            DigitalSignature sig = record.getDigitalSignature();

            if (isViewable(sig))
            {
                unsupportedLabel.setVisible(false);
                imageView.setVisible(true);

                Image thumb = new Image(file.toURI().toString(), 250, 250, true, true, true);
                imageView.setImage(thumb);
            }
            
            else
            {
                imageView.setImage(null);
                imageView.setVisible(false);
                unsupportedLabel.setVisible(true);
            }

            popupStage.setX(screenX + 15);
            popupStage.setY(screenY + 15);

            if (!popupStage.isShowing())
            {
                popupStage.show();
            }
        }
        
        else
        {
            hide();
        }
    }

    public void hide()
    {
        if (popupStage.isShowing())
        {
            popupStage.hide();
            imageView.setImage(null);
        }
    }

    private boolean isViewable(DigitalSignature type)
    {
        return (type == DigitalSignature.JPG || type == DigitalSignature.PNG);
    }
}
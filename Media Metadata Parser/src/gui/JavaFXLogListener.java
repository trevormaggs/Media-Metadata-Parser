package gui;

import java.util.Objects;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import logger.LogListener;

public class JavaFXLogListener implements LogListener
{
    private final TextArea textArea;

    public JavaFXLogListener(TextArea textArea)
    {
        this.textArea = Objects.requireNonNull(textArea, "TextArea is undefined");
    }

    @Override
    public void onLog(Level level, String message)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                textArea.appendText(message + System.lineSeparator());
            }
        });
    }

    @Override
    public void reset()
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                textArea.clear();
            }
        });
    }
}
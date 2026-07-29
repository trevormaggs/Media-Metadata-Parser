package gui;

import java.util.logging.Level;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import logger.LogListener;

public class JavaFXLogListener implements LogListener
{
    private final TextArea textArea;

    public JavaFXLogListener(TextArea textArea)
    {
        if (textArea == null)
        {
            throw new IllegalArgumentException("TextArea cannot be null");
        }

        this.textArea = textArea;
    }

    @Override
    public void onLog(Level level, String message)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                textArea.appendText("[" + level + "] " + message + System.lineSeparator());
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
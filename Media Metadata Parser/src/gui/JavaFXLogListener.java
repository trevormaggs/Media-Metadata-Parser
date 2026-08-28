package gui;

import java.util.Objects;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import logger.LogListener;

/**
 * A {@link LogListener} implementation that displays log messages in a JavaFX {@link TextArea}.
 *
 * <p>
 * Log messages are appended on the JavaFX Application Thread using
 * {@link Platform#runLater(Runnable)} to ensure thread-safe updates to the user interface.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 4 August 2026
 */
public class JavaFXLogListener implements LogListener
{
    private final TextArea textArea;

    /**
     * Creates a new listener that writes log messages to the specified text area.
     *
     * @param textArea
     *        the text area used to display log messages
     * @throws NullPointerException
     *         if {@code textArea} is {@code null}
     */
    public JavaFXLogListener(TextArea textArea)
    {
        this.textArea = Objects.requireNonNull(textArea, "TextArea is undefined");
    }

    /**
     * Appends a log message to the text area.
     *
     * @param level
     *        the logging level
     * @param message
     *        the formatted log message
     */
    @Override
    public void onLog(Level level, String message)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                textArea.appendText(message);
            }
        });
    }

    /**
     * Clears all log messages from the text area.
     */
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
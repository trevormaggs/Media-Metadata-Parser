package gui;

import java.util.Objects;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import logger.LogListener;

/**
 * A {@link LogListener} implementation that displays log messages in a JavaFX {@link TextFlow}
 * container with severity-based style formatting and direct message injection support.
 *
 * <p>
 * Log messages are appended on the JavaFX Application Thread using
 * {@link Platform#runLater(Runnable)} to ensure thread-safe updates to the user interface.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 4 August 2026
 */
public class JavaFXLogListener implements LogListener
{
    private final TextFlow logFlow;

    /**
     * Creates a new listener that writes log messages to the specified TextFlow container.
     *
     * @param logFlow
     *        the target {@link TextFlow} used to display styled log messages
     *
     * @throws NullPointerException
     *         if {@code logFlow} is {@code null}
     */
    public JavaFXLogListener(final TextFlow logFlow)
    {
        this.logFlow = Objects.requireNonNull(logFlow, "TextFlow is undefined");
    }

    /**
     * Appends a log message to the TextFlow with appropriate severity styling based on logging
     * level.
     *
     * @param level
     *        the logging level
     * @param message
     *        the formatted log message
     */
    @Override
    public void onLog(Level level, String message)
    {
        if (message != null)
        {
            final Text textNode = new Text(message);

            if (Level.CONFIG.equals(level))
            {
                textNode.getStyleClass().add("log-debug");
            }

            else if (Level.WARNING.equals(level))
            {
                textNode.getStyleClass().add("log-warn");
            }

            else if (Level.SEVERE.equals(level))
            {
                textNode.getStyleClass().add("log-error");
            }

            else if (level.intValue() <= Level.FINE.intValue())
            {
                textNode.getStyleClass().add("log-trace");
            }

            else
            {
                textNode.getStyleClass().add("log-info");
            }

            Runnable appendTask = new Runnable()
            {
                @Override
                public void run()
                {
                    logFlow.getChildren().add(textNode);
                }
            };

            if (Platform.isFxApplicationThread())
            {
                appendTask.run();
            }

            else
            {
                Platform.runLater(appendTask);
            }
        }
    }

    /**
     * Clears all log messages from the TextFlow container.
     */
    @Override
    public void reset()
    {
        Runnable clearTask = new Runnable()
        {
            @Override
            public void run()
            {
                logFlow.getChildren().clear();
            }
        };

        if (Platform.isFxApplicationThread())
        {
            clearTask.run();
        }

        else
        {
            Platform.runLater(clearTask);
        }
    }
}
package gui;

import java.util.Objects;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import logger.LogListener;

/**
 * A {@link LogListener} implementation that displays log messages in a JavaFX {@link TextFlow}
 * container with severity-based style formatting.
 *
 * <p>
 * Log messages are appended on the JavaFX Application Thread using
 * {@link Platform#runLater(Runnable)} to ensure thread-safe updates to the user interface.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 4 August 2026
 */
public class JavaFXTextFlowLogListener implements LogListener
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
    public JavaFXTextFlowLogListener(TextFlow logFlow)
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
            final Text textNode = new Text(message.endsWith("\n") ? message : message + "\n");

            textNode.getStyleClass().add("log-text");

            if (level != null)
            {
                if (Level.SEVERE.equals(level))
                {
                    textNode.getStyleClass().add("log-error");
                }

                else if (Level.WARNING.equals(level))
                {
                    textNode.getStyleClass().add("log-warn");
                }

                else if (Level.CONFIG.equals(level))
                {
                    textNode.getStyleClass().add("log-debug");
                }

                else if (Level.FINE.equals(level) || Level.FINER.equals(level) || Level.FINEST.equals(level))
                {
                    textNode.getStyleClass().add("log-trace");
                }

                else
                {
                    textNode.getStyleClass().add("log-info");
                }
            }

            else
            {
                textNode.getStyleClass().add("log-info");
            }

            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    logFlow.getChildren().add(textNode);
                }
            });
        }
    }

    /**
     * Clears all log messages from the TextFlow container.
     */
    @Override
    public void reset()
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                logFlow.getChildren().clear();
            }
        });
    }

    /**
     * Appends a custom log line with a specific CSS style class directly to this listener's TextFlow container.
     *
     * @param message
     *        the message text to append
     * @param styleClass
     *        the CSS class name to apply (e.g., "log-success", "log-error", "log-warn")
     */
    void appendLogLine(final String message, final String styleClass)
    {
        appendLogLine(this.logFlow, message, styleClass);
    }

    /**
     * Appends a custom log line with a specific CSS style class directly to a target TextFlow container.
     *
     * @param targetFlow
     *        the target TextFlow container
     * @param message
     *        the message text to append
     * @param styleClass
     *        the CSS class name to apply (e.g., "log-success", "log-error", "log-warn")
     */
    static void appendLogLine(final TextFlow targetFlow, final String message, final String styleClass)
    {
        if (targetFlow != null && message != null)
        {
            final Text textNode = new Text(message.endsWith("\n") ? message : message + "\n");
            textNode.getStyleClass().add("log-text");

            if (styleClass != null && !styleClass.isEmpty())
            {
                textNode.getStyleClass().add(styleClass);
            }

            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    targetFlow.getChildren().add(textNode);
                }
            });
        }
    }
}
package gui;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/**
 * Provides smooth hover debouncing to optimise performance. It delays the execution of rapidly
 * triggered actions until a specified quiet period has elapsed.
 * 
 * <p>
 * This class optimises performance during rapid hover or keyboard navigation events by preventing
 * redundant background task creation. For example, moving a cursor across 20 table rows in a
 * fraction of a second would otherwise spawn and cancel 20 image-loading tasks. By introducing a
 * brief delay of 120ms, actions like {@code showPreview()} are only invoked when navigation pauses,
 * saving CPU and I/O cycles.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 7 September 2026
 */
class HoverDebouncer
{
    private final PauseTransition delay;
    private Runnable pending;

    /**
     * Constructs a debouncer with the specified execution delay.
     *
     * @param delayMillis
     *        the delay in milliseconds to wait after the last request before executing the pending
     *        action
     */
    HoverDebouncer(double delayMillis)
    {
        this.delay = new PauseTransition(Duration.millis(delayMillis));

        delay.setOnFinished(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (pending != null)
                {
                    pending.run();
                }
            }
        });
    }

    /**
     * Schedules an action to execute after the configured delay.
     *
     * <p>
     * If another action is requested before the delay expires, the previous action is replaced and
     * the timer restarts from zero.
     * </p>
     *
     * @param action
     *        the {@link Runnable} action to execute after the delay, or {@code null} to clear the
     *        pending action
     */
    void request(Runnable action)
    {
        pending = action;
        delay.playFromStart();
    }

    /**
     * Cancels any currently pending action and stops the execution timer.
     */
    void cancel()
    {
        delay.stop();
        pending = null;
    }
}
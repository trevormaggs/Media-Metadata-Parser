package common;

/**
 * A destination receiver used to display or format metadata properties.
 */
public interface PropertyDisplay
{
    /**
     * Accepts a property name and its formatted value for display.
     *
     * @param key
     *        the human-readable name of the property
     * @param value
     *        the formatted property value
     */
    void accept(String key, Object value);
}
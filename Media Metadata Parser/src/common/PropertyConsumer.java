package common;

/**
 * A functional consumer that receives metadata property names and their formatted values.
 */
public interface PropertyConsumer
{
    /**
     * Accepts a property name and its formatted value for display or processing.
     *
     * @param key
     *        the human-readable name of the property
     * @param value
     *        the formatted property value
     */
    void accept(String key, Object value);
}
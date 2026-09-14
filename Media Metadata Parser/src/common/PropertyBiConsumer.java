package common;

/**
 * Represents an operation that accepts a property key and a typed value for display or processing.
 *
 * @param <V>
 *        the type of the property value
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 7 September 2026
 */
public interface PropertyBiConsumer
{
    /**
     * Accepts a property key-value pair for display or processing.
     *
     * @param key
     *        the human-readable name of the property
     * @param value
     *        the property value
     */
    void accept(String key, Object value);
}
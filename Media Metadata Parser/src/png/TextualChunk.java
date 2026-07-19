package png;

import common.PropertyDisplay;

/**
 * Common interface for PNG textual metadata chunks (tEXt, zTXt, iTXt).
 */
public interface TextualChunk
{
    /**
     * Checks if this chunk matches a specific predefined keyword.
     *
     * @param keyword
     *        the keyword to search for
     * @return true if found, false otherwise
     */
    boolean hasKeyword(TextKeyword keyword);

    /**
     * Gets the raw keyword string.
     *
     * @return the keyword
     */
    String getKeyword();

    /**
     * Provides easy, type-safe retrieval of the resolved keyword enum token.
     *
     * @return the associated {@link TextKeyword} constant, or {@link TextKeyword#OTHER} if unknown
     */
    TextKeyword getTextKeyword();

    /**
     * Gets the decoded text content.
     *
     * @return the decoded text, or an empty string if missing or could not be decoded
     */
    String getText();

    /**
     * Gets the formatted keyword tag tailored for display output formats.
     *
     * @return the formatted keyword string
     */
    default String getFormattedKeyword()
    {
        return getKeyword();
    }

    /**
     * Prints the standard textual property key-value pair of this chunk to the provided display
     * target.
     *
     * <p>
     * This default implementation automatically pairs the display-ready keyword with its
     * corresponding uncompressed or decoded text content.
     * </p>
     *
     * @param display
     *        the target display destination for the printed metadata fields
     */
    default void printProperties(PropertyDisplay display)
    {
        display.accept(getFormattedKeyword(), getText());
    }
}